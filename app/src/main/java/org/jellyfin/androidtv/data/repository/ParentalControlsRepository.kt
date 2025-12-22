package org.jellyfin.androidtv.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.filterApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import timber.log.Timber
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Repository for managing parental controls based on content ratings.
 * Stores blocked ratings per-user and filters items accordingly.
 */
interface ParentalControlsRepository {
	/**
	 * Get all unique content ratings from the user's libraries across all servers.
	 */
	suspend fun getAvailableRatings(): List<String>

	/**
	 * Get the set of blocked ratings for the current user.
	 */
	fun getBlockedRatings(): Set<String>

	/**
	 * Set the blocked ratings for the current user.
	 */
	fun setBlockedRatings(ratings: Set<String>)

	/**
	 * Check if an item should be filtered (hidden) based on its rating.
	 */
	fun shouldFilterItem(item: BaseItemDto): Boolean

	/**
	 * Check if a rating is blocked.
	 */
	fun isRatingBlocked(rating: String?): Boolean

	/**
	 * Filter a list of items based on parental controls.
	 */
	fun <T> filterItems(items: List<T>, ratingExtractor: (T) -> String?): List<T>

	/**
	 * Observable flow of blocked ratings for the current user.
	 */
	val blockedRatingsFlow: StateFlow<Set<String>>

	/**
	 * Whether parental controls are enabled (any ratings blocked).
	 */
	fun isEnabled(): Boolean
}

class ParentalControlsRepositoryImpl(
	private val context: Context,
	private val sessionRepository: SessionRepository,
	private val multiServerRepository: MultiServerRepository,
) : ParentalControlsRepository {

	companion object {
		private const val PREFS_NAME_PREFIX = "parental_controls_"
		private const val KEY_BLOCKED_RATINGS = "blocked_ratings"
		private const val KEY_CACHED_RATINGS = "cached_ratings"
		private const val KEY_CACHE_TIMESTAMP = "cache_timestamp"
		private val SERVER_TIMEOUT = 10.seconds
		private val CACHE_DURATION = 24.hours
		private val json = Json { ignoreUnknownKeys = true }
	}

	private val _blockedRatingsFlow = MutableStateFlow<Set<String>>(emptySet())
	override val blockedRatingsFlow: StateFlow<Set<String>> = _blockedRatingsFlow

	// In-memory cache for faster access
	private var cachedRatings: List<String>? = null
	
	// Scope for observing session changes
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
	private var currentLoadedUserId: UUID? = null

	init {
		// Observe session changes and reload blocked ratings when user changes
		sessionRepository.currentSession
			.filterNotNull()
			.onEach { session ->
				if (session.userId != currentLoadedUserId) {
					Timber.d("ParentalControlsRepository: Session changed, reloading for user ${session.userId}")
					currentLoadedUserId = session.userId
					loadBlockedRatings()
				}
			}
			.launchIn(scope)
		
		// Also try to load immediately if session exists
		loadBlockedRatings()
	}

	private fun getCurrentUserId(): UUID? {
		return sessionRepository.currentSession.value?.userId
	}

	private fun getPrefsForUser(userId: UUID) =
		context.getSharedPreferences("$PREFS_NAME_PREFIX$userId", Context.MODE_PRIVATE)

	private fun loadBlockedRatings() {
		val userId = getCurrentUserId()
		if (userId == null) {
			Timber.d("ParentalControlsRepository: No current user, cannot load blocked ratings")
			return
		}
		val prefs = getPrefsForUser(userId)
		val jsonString = prefs.getString(KEY_BLOCKED_RATINGS, null)
		val ratings = if (jsonString != null) {
			try {
				json.decodeFromString<Set<String>>(jsonString)
			} catch (e: Exception) {
				Timber.e(e, "Failed to parse blocked ratings")
				emptySet()
			}
		} else {
			emptySet()
		}
		_blockedRatingsFlow.value = ratings
		Timber.d("ParentalControlsRepository: Loaded ${ratings.size} blocked ratings for user $userId: $ratings")
	}

	override suspend fun getAvailableRatings(): List<String> = withContext(Dispatchers.IO) {
		// Return in-memory cache if available
		cachedRatings?.let { return@withContext it }

		// Check disk cache
		val userId = getCurrentUserId()
		if (userId != null) {
			val cached = loadCachedRatings(userId)
			if (cached != null) {
				Timber.d("ParentalControlsRepository: Using cached ratings (${cached.size} ratings)")
				cachedRatings = cached
				return@withContext cached
			}
		}

		// Fetch from servers
		val allRatings = mutableSetOf<String>()

		val loggedInServers = multiServerRepository.getLoggedInServers()
		Timber.d("ParentalControlsRepository: Getting ratings from ${loggedInServers.size} servers")

		loggedInServers.forEach { session ->
			try {
				val ratings = getRatingsFromServer(session.apiClient)
				allRatings.addAll(ratings)
				Timber.d("ParentalControlsRepository: Got ${ratings.size} ratings from ${session.server.name}")
			} catch (e: Exception) {
				Timber.e(e, "ParentalControlsRepository: Error getting ratings from ${session.server.name}")
			}
		}

		// Sort ratings (common TV/Movie ratings first, then alphabetically)
		val sortedRatings = allRatings.toList().sortedWith(RatingComparator)

		// Cache the results
		if (userId != null && sortedRatings.isNotEmpty()) {
			saveCachedRatings(userId, sortedRatings)
		}
		cachedRatings = sortedRatings

		sortedRatings
	}

	private fun loadCachedRatings(userId: UUID): List<String>? {
		val prefs = getPrefsForUser(userId)
		val timestamp = prefs.getLong(KEY_CACHE_TIMESTAMP, 0)
		val age = System.currentTimeMillis() - timestamp

		// Check if cache is still valid
		if (age > CACHE_DURATION.inWholeMilliseconds) {
			Timber.d("ParentalControlsRepository: Cache expired (age: ${age}ms)")
			return null
		}

		val jsonString = prefs.getString(KEY_CACHED_RATINGS, null) ?: return null
		return try {
			json.decodeFromString<List<String>>(jsonString)
		} catch (e: Exception) {
			Timber.e(e, "Failed to parse cached ratings")
			null
		}
	}

	private fun saveCachedRatings(userId: UUID, ratings: List<String>) {
		val prefs = getPrefsForUser(userId)
		prefs.edit()
			.putString(KEY_CACHED_RATINGS, json.encodeToString(ratings))
			.putLong(KEY_CACHE_TIMESTAMP, System.currentTimeMillis())
			.apply()
		Timber.d("ParentalControlsRepository: Cached ${ratings.size} ratings")
	}

	private suspend fun getRatingsFromServer(apiClient: ApiClient): List<String> {
		return withTimeoutOrNull(SERVER_TIMEOUT) {
			try {
				// Get ratings for movies and TV shows
				val movieRatings = apiClient.filterApi.getQueryFiltersLegacy(
					includeItemTypes = listOf(BaseItemKind.MOVIE)
				).content.officialRatings ?: emptyList()

				val seriesRatings = apiClient.filterApi.getQueryFiltersLegacy(
					includeItemTypes = listOf(BaseItemKind.SERIES)
				).content.officialRatings ?: emptyList()

				val episodeRatings = apiClient.filterApi.getQueryFiltersLegacy(
					includeItemTypes = listOf(BaseItemKind.EPISODE)
				).content.officialRatings ?: emptyList()

				(movieRatings + seriesRatings + episodeRatings)
					.filter { it.isNotBlank() }
					.distinct()
			} catch (e: Exception) {
				Timber.e(e, "Error fetching ratings from server")
				emptyList()
			}
		} ?: emptyList()
	}

	override fun getBlockedRatings(): Set<String> = getBlockedRatingsInternal()

	/**
	 * Get blocked ratings, loading from SharedPreferences if not yet loaded.
	 * This ensures we never miss filtering due to race conditions at startup.
	 */
	private fun getBlockedRatingsInternal(): Set<String> {
		// Fast path - we already have loaded ratings
		val current = _blockedRatingsFlow.value
		if (current.isNotEmpty()) return current
		
		// Try to load from prefs directly if flow is empty (race condition at startup)
		val userId = getCurrentUserId()
		if (userId == null) {
			Timber.d("ParentalControlsRepository: getBlockedRatingsInternal - no userId available")
			return emptySet()
		}
		
		// Always try to load from prefs when flow is empty, regardless of currentLoadedUserId
		// This handles the case where init ran before session was available
		val prefs = getPrefsForUser(userId)
		val jsonString = prefs.getString(KEY_BLOCKED_RATINGS, null)
		if (jsonString != null) {
			try {
				val ratings = json.decodeFromString<Set<String>>(jsonString)
				if (ratings.isNotEmpty()) {
					Timber.d("ParentalControlsRepository: Loaded ${ratings.size} blocked ratings on-demand for user $userId: $ratings")
					_blockedRatingsFlow.value = ratings
					currentLoadedUserId = userId
					return ratings
				}
			} catch (e: Exception) {
				Timber.e(e, "ParentalControlsRepository: Failed to parse blocked ratings on-demand")
			}
		}
		
		return emptySet()
	}

	override fun setBlockedRatings(ratings: Set<String>) {
		val userId = getCurrentUserId() ?: return
		val prefs = getPrefsForUser(userId)
		val jsonString = json.encodeToString(ratings)
		prefs.edit().putString(KEY_BLOCKED_RATINGS, jsonString).apply()
		_blockedRatingsFlow.value = ratings
		Timber.d("ParentalControlsRepository: Set blocked ratings to $ratings for user $userId")
	}

	override fun shouldFilterItem(item: BaseItemDto): Boolean {
		val result = isRatingBlocked(item.officialRating)
		if (result) {
			Timber.d("ParentalControlsRepository: Filtering item '${item.name}' with rating '${item.officialRating}'")
		}
		return result
	}

	override fun isRatingBlocked(rating: String?): Boolean {
		if (rating.isNullOrBlank()) return false
		val blocked = getBlockedRatingsInternal()
		return blocked.contains(rating)
	}

	override fun <T> filterItems(items: List<T>, ratingExtractor: (T) -> String?): List<T> {
		val blocked = getBlockedRatingsInternal()
		if (blocked.isEmpty()) return items

		val filtered = items.filter { item ->
			val rating = ratingExtractor(item)
			rating == null || !blocked.contains(rating)
		}
		Timber.d("ParentalControlsRepository: Filtered ${items.size} -> ${filtered.size} items (blocked: $blocked)")
		return filtered
	}

	override fun isEnabled(): Boolean = getBlockedRatingsInternal().isNotEmpty()

	/**
	 * Comparator that sorts ratings in a logical order:
	 * - Common US ratings first (G, PG, PG-13, R, NC-17)
	 * - Common TV ratings (TV-Y, TV-Y7, TV-G, TV-PG, TV-14, TV-MA)
	 * - Other ratings alphabetically
	 */
	private object RatingComparator : Comparator<String> {
		private val ratingOrder = listOf(
			// Movie ratings
			"G", "PG", "PG-13", "R", "NC-17", "NR", "Unrated",
			// TV ratings
			"TV-Y", "TV-Y7", "TV-Y7-FV", "TV-G", "TV-PG", "TV-14", "TV-MA",
			// UK ratings
			"U", "PG", "12", "12A", "15", "18", "R18",
			// Common international
			"All", "6", "9", "12", "16", "18"
		)

		override fun compare(a: String, b: String): Int {
			val indexA = ratingOrder.indexOf(a)
			val indexB = ratingOrder.indexOf(b)

			return when {
				indexA >= 0 && indexB >= 0 -> indexA - indexB
				indexA >= 0 -> -1
				indexB >= 0 -> 1
				else -> a.compareTo(b, ignoreCase = true)
			}
		}
	}
}
