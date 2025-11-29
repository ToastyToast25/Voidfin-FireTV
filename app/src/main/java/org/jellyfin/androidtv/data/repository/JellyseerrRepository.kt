package org.jellyfin.androidtv.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrBlacklistPageDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrCreateRequestDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrDiscoverItemDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrDiscoverPageDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrHttpClient
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrListResponse
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrMovieDetailsDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrPersonCombinedCreditsDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrPersonDetailsDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrRadarrSettingsDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrRequestDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrSonarrSettingsDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrTvDetailsDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrUserDto
import org.jellyfin.androidtv.data.service.jellyseerr.Seasons
import org.jellyfin.androidtv.preference.JellyseerrPreferences
import timber.log.Timber

/**
 * Repository for Jellyseerr operations
 * Manages requests, discover content, and connection state
 */
interface JellyseerrRepository {
	val isAvailable: StateFlow<Boolean>

	/**
	 * Ensure the repository is initialized from saved preferences
	 */
	suspend fun ensureInitialized()

	/**
	 * Initialize the repository with server configuration
	 */
	suspend fun initialize(serverUrl: String, apiKey: String): Result<Unit>

	/**
	 * Test the connection to Jellyseerr
	 */
	suspend fun testConnection(): Result<Boolean>

	/**
	 * Get movie details including cast
	 */
	suspend fun getMovieDetails(tmdbId: Int): Result<JellyseerrMovieDetailsDto>

	/**
	 * Get TV show details including cast
	 */
	suspend fun getTvDetails(tmdbId: Int): Result<JellyseerrTvDetailsDto>

	/**
	 * Authenticate with Jellyfin SSO (cookie-based, 30-day expiration)
	 */
	suspend fun loginWithJellyfin(username: String, password: String, jellyfinUrl: String, jellyseerrUrl: String): Result<JellyseerrUserDto>

	/**
	 * Authenticate with local Jellyseerr credentials (returns API key for permanent auth)
	 */
	suspend fun loginLocal(email: String, password: String, jellyseerrUrl: String): Result<JellyseerrUserDto>

	/**
	 * Regenerate API key for current user (requires active session)
	 * Use after Jellyfin login to get permanent API key instead of 30-day cookies
	 */
	suspend fun regenerateApiKey(): Result<String>

	/**
	 * Check if the current session is still valid (cookie-based auth)
	 * This verifies that the stored session cookie from a previous login is still active
	 */
	suspend fun isSessionValid(): Result<Boolean>

	/**
	 * Get current authenticated user
	 */
	suspend fun getCurrentUser(): Result<JellyseerrUserDto>

	/**
	 * Get all requests visible to the current user
	 */
	suspend fun getRequests(
		filter: String? = null,
		requestedBy: Int? = null,
		limit: Int = 50,
		offset: Int = 0,
	): Result<JellyseerrListResponse<JellyseerrRequestDto>>

	/**
	 * Create a new request for a movie or TV show
	 */
	suspend fun createRequest(
		mediaId: Int,
		mediaType: String,
		seasons: Seasons? = null,
		is4k: Boolean = false,
		profileId: Int? = null,
		rootFolderId: Int? = null,
		serverId: Int? = null,
	): Result<JellyseerrRequestDto>

	/**
	 * Delete an existing request
	 */
	suspend fun deleteRequest(requestId: Int): Result<Unit>

	/**
	 * Get trending movies
	 */
	suspend fun getTrendingMovies(
		limit: Int = 20,
		offset: Int = 0,
	): Result<JellyseerrDiscoverPageDto>

	/**
	 * Get trending TV shows
	 */
	suspend fun getTrendingTv(
		limit: Int = 20,
		offset: Int = 0,
	): Result<JellyseerrDiscoverPageDto>

	/**
	 * Get trending content (movies and TV combined)
	 */
	suspend fun getTrending(
		limit: Int = 20,
		offset: Int = 0,
	): Result<JellyseerrDiscoverPageDto>

	/**
	 * Get top-rated movies
	 */
	suspend fun getTopMovies(
		limit: Int = 20,
		offset: Int = 0,
	): Result<JellyseerrDiscoverPageDto>

	/**
	 * Get top-rated TV shows
	 */
	suspend fun getTopTv(
		limit: Int = 20,
		offset: Int = 0,
	): Result<JellyseerrDiscoverPageDto>

	/**
	 * Get upcoming movies
	 */
	suspend fun getUpcomingMovies(
		limit: Int = 20,
		offset: Int = 0,
	): Result<JellyseerrDiscoverPageDto>

	/**
	 * Get upcoming TV shows
	 */
	suspend fun getUpcomingTv(
		limit: Int = 20,
		offset: Int = 0,
	): Result<JellyseerrDiscoverPageDto>

	/**
	 * Search for movies or TV shows
	 */
	suspend fun search(
		query: String,
		mediaType: String? = null,
		limit: Int = 20,
		offset: Int = 0,
	): Result<JellyseerrDiscoverPageDto>

	/**
	 * Get similar movies for a given movie ID
	 */
	suspend fun getSimilarMovies(tmdbId: Int, page: Int = 1): Result<JellyseerrDiscoverPageDto>

	/**
	 * Get similar TV shows for a given TV show ID
	 */
	suspend fun getSimilarTv(tmdbId: Int, page: Int = 1): Result<JellyseerrDiscoverPageDto>

	/**
	 * Get person details by ID
	 */
	suspend fun getPersonDetails(personId: Int): Result<JellyseerrPersonDetailsDto>

	/**
	 * Get combined credits (movies and TV) for a person
	 */
	suspend fun getPersonCombinedCredits(personId: Int): Result<JellyseerrPersonCombinedCreditsDto>

	/**
	 * Get the blacklist
	 */
	suspend fun getBlacklist(): Result<JellyseerrBlacklistPageDto>

	/**
	 * Get all Radarr server configurations
	 */
	suspend fun getRadarrSettings(): Result<List<JellyseerrRadarrSettingsDto>>

	/**
	 * Get all Sonarr server configurations
	 */
	suspend fun getSonarrSettings(): Result<List<JellyseerrSonarrSettingsDto>>

	/**
	 * Cleanup resources
	 */
	fun close()
}

class JellyseerrRepositoryImpl(
	private val context: android.content.Context,
	private val preferences: JellyseerrPreferences,
	private val userRepository: UserRepository,
) : JellyseerrRepository {
	private var httpClient: JellyseerrHttpClient? = null
	private val _isAvailable = MutableStateFlow(false)
	override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()
	private var initialized = false

	/**
	 * Auto-initialize from saved preferences if available
	 * Waits for user to be set before initializing to ensure correct cookie storage is used
	 * Will attempt to re-authenticate if session is invalid and credentials are saved
	 */
	override suspend fun ensureInitialized() {
		// Reset initialization if client is no longer available
		if (initialized && httpClient == null) {
			initialized = false
		}

		if (initialized) return
		if (httpClient != null) {
			initialized = true
			return
		}

		withContext(Dispatchers.IO) {
			try {
				val serverUrl = preferences[JellyseerrPreferences.serverUrl]
				val enabled = preferences[JellyseerrPreferences.enabled]
				val storedApiKey = preferences[JellyseerrPreferences.apiKey] ?: ""
				val authMethod = preferences[JellyseerrPreferences.authMethod] ?: ""

				// For cookie-based auth, wait for user to be set so correct cookie storage is used
				if (enabled && !serverUrl.isNullOrEmpty() && storedApiKey.isEmpty()) {
					// Wait up to 5 seconds for user to be available
					val user = withTimeoutOrNull(5000L) {
						userRepository.currentUser.first { it != null }
					}
					if (user != null) {
						Timber.d("Jellyseerr: User '${user.name}' detected, switching cookie storage before init")
						JellyseerrHttpClient.switchCookieStorage(user.id.toString())
					} else {
						Timber.w("Jellyseerr: No user available after timeout, cookies may not be loaded correctly")
					}
				}

				// Initialize with stored API key or cookie-based auth
				if (enabled && !serverUrl.isNullOrEmpty()) {
					Timber.d("Jellyseerr: Auto-initializing from saved preferences (API key: ${if (storedApiKey.isNotEmpty()) "present" else "absent, using cookies"})")
					httpClient = JellyseerrHttpClient(context, serverUrl, storedApiKey)
				
					// Verify the session is actually valid by calling getCurrentUser
					val sessionValid = httpClient?.getCurrentUser()?.isSuccess == true
					
					if (!sessionValid && storedApiKey.isEmpty() && authMethod == "jellyfin") {
						// Cookie-based auth failed, try to re-authenticate with saved password
						val savedPassword = preferences[JellyseerrPreferences.password] ?: ""
						if (savedPassword.isNotEmpty()) {
							Timber.i("Jellyseerr: Session expired, attempting auto-re-login with saved credentials")
							val jellyfinUser = userRepository.currentUser.value
							if (jellyfinUser != null) {
								val username = jellyfinUser.name ?: ""
								// Get Jellyfin server URL from the API client
								val jellyfinUrl = org.koin.core.context.GlobalContext.get().get<org.jellyfin.sdk.api.client.ApiClient>().baseUrl ?: ""
								if (username.isNotEmpty() && jellyfinUrl.isNotEmpty()) {
									val reloginResult = httpClient?.loginJellyfin(username, savedPassword, jellyfinUrl)
									if (reloginResult?.isSuccess == true) {
										Timber.i("Jellyseerr: Auto-re-login successful")
										_isAvailable.emit(true)
										initialized = true
										return@withContext
									} else {
										Timber.w("Jellyseerr: Auto-re-login failed")
									}
								}
							}
						} else {
							Timber.w("Jellyseerr: Session expired but no saved password for auto-re-login")
						}
					}
					
					_isAvailable.emit(sessionValid)
					initialized = true
					Timber.d("Jellyseerr: Auto-initialized - Session valid: $sessionValid")
				} else {
					Timber.w("Jellyseerr: Jellyseerr is disabled or not configured")
					_isAvailable.emit(false)
					initialized = true
				}
			} catch (error: Exception) {
				Timber.w(error, "Jellyseerr: Failed to auto-initialize from preferences")
				_isAvailable.emit(false)
				initialized = true
			}
		}
	}

	override suspend fun initialize(serverUrl: String, apiKey: String): Result<Unit> =
		withContext(Dispatchers.IO) {
			try {
				// Clean up old client if it exists
				httpClient?.close()
				initialized = false

				// Create new client
				httpClient = JellyseerrHttpClient(context, serverUrl, apiKey)

				// Test connection
				val connectionTest = httpClient?.testConnection()?.getOrNull() == true
				_isAvailable.emit(connectionTest)

				// Save preferences
				preferences[JellyseerrPreferences.serverUrl] = serverUrl
				preferences[JellyseerrPreferences.enabled] = true
				initialized = true

				Timber.d("Jellyseerr: Initialized - Available: $connectionTest")
				Result.success(Unit)
			} catch (error: Exception) {
				Timber.e(error, "Jellyseerr: Failed to initialize")
				_isAvailable.emit(false)
				Result.failure(error)
			}
		}

	override suspend fun testConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			Timber.w("Jellyseerr: No client initialized and no saved preferences")
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		val result = client.testConnection()
		if (result.isSuccess) {
			_isAvailable.emit(true)
		}
		result
	}

	override suspend fun getMovieDetails(tmdbId: Int): Result<JellyseerrMovieDetailsDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getMovieDetails(tmdbId)
	}

	override suspend fun getTvDetails(tmdbId: Int): Result<JellyseerrTvDetailsDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getTvDetails(tmdbId)
	}

	override suspend fun loginWithJellyfin(username: String, password: String, jellyfinUrl: String, jellyseerrUrl: String): Result<JellyseerrUserDto> = withContext(Dispatchers.IO) {
		// Create temporary client without API key for authentication
		val tempClient = JellyseerrHttpClient(context, jellyseerrUrl, "")
		val result = tempClient.loginJellyfin(username, password, jellyfinUrl)
		
		result.onSuccess { user ->
			// Successfully logged in with Jellyfin (cookie-based auth)
			Timber.d("Jellyseerr: Jellyfin login successful, user: ${user.username}")
			
			// Save password for auto-re-login when cookies expire
			preferences[JellyseerrPreferences.password] = password
			
			// Check if auto API key generation is enabled
			val autoGenerateApiKey = preferences[JellyseerrPreferences.autoGenerateApiKey]
			
			if (autoGenerateApiKey) {
				Timber.d("Jellyseerr: Auto API key generation enabled, regenerating API key...")
				
				// Initialize with empty API key first (using cookies for auth)
				initialize(jellyseerrUrl, "")
				
				// Try to regenerate API key to get permanent auth
				val apiKeyResult = regenerateApiKey()
				apiKeyResult.onSuccess { apiKey ->
					Timber.d("Jellyseerr: Successfully generated permanent API key (length: ${apiKey.length})")
					// Save the API key to preferences
					preferences[JellyseerrPreferences.apiKey] = apiKey
					preferences[JellyseerrPreferences.authMethod] = "jellyfin-apikey"
					// Clear password since we have API key now
					preferences[JellyseerrPreferences.password] = ""
				}.onFailure { error ->
					Timber.w(error, "Jellyseerr: Failed to auto-generate API key - This requires ADMIN permissions")
					Timber.w("Jellyseerr: Falling back to cookie-based auth with saved password for auto-re-login")
					// Keep using cookie-based auth with saved password
					preferences[JellyseerrPreferences.authMethod] = "jellyfin"
				}
			} else {
				Timber.d("Jellyseerr: Auto API key generation disabled, using cookie-based auth")
				// Initialize client with empty API key (will use cookies)
				initialize(jellyseerrUrl, "")
				preferences[JellyseerrPreferences.authMethod] = "jellyfin"
			}
		}
		
		result
	}

	override suspend fun loginLocal(email: String, password: String, jellyseerrUrl: String): Result<JellyseerrUserDto> = withContext(Dispatchers.IO) {
		Timber.i("Jellyseerr Repository: Starting local login for email: $email")
		Timber.d("Jellyseerr Repository: Server URL: $jellyseerrUrl")
		
		// Create temporary client without API key for authentication
		val tempClient = JellyseerrHttpClient(context, jellyseerrUrl, "")
		Timber.d("Jellyseerr Repository: Created temporary HTTP client for authentication")
		
		val result = tempClient.loginLocal(email, password)
		
		result.onSuccess { user ->
			// Successfully logged in with local credentials
			val hasApiKey = !user.apiKey.isNullOrEmpty()
			val apiKeyLength = user.apiKey?.length ?: 0
			Timber.i("Jellyseerr Repository: Local login successful - User: ${user.username}, Has API key: $hasApiKey (length: $apiKeyLength)")
			
			// Initialize client with the API key from response
			val apiKey = user.apiKey ?: ""
			if (apiKey.isEmpty()) {
				Timber.w("Jellyseerr Repository: Login succeeded but no API key returned")
			}
			
			Timber.d("Jellyseerr Repository: Initializing client with returned API key")
			initialize(jellyseerrUrl, apiKey)
			
			// Save credentials and API key to preferences
			Timber.d("Jellyseerr Repository: Saving authentication data to preferences")
			preferences[JellyseerrPreferences.authMethod] = "local"
			preferences[JellyseerrPreferences.localEmail] = email
			preferences[JellyseerrPreferences.localPassword] = password
			preferences[JellyseerrPreferences.apiKey] = apiKey
			Timber.i("Jellyseerr Repository: Local login completed successfully")
		}.onFailure { error ->
			Timber.e(error, "Jellyseerr Repository: Local login failed")
		}
		
		result
	}

	override suspend fun regenerateApiKey(): Result<String> = withContext(Dispatchers.IO) {
		Timber.i("Jellyseerr Repository: Starting API key regeneration")
		ensureInitialized()

		val client = httpClient ?: run {
			Timber.e("Jellyseerr Repository: Cannot regenerate API key - client not initialized")
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		Timber.d("Jellyseerr Repository: Calling HTTP client regenerateApiKey()")
		val result = client.regenerateApiKey()
		
		result.onSuccess { apiKey ->
			Timber.i("Jellyseerr Repository: API key regenerated successfully (length: ${apiKey.length})")
			
			// Reinitialize the client with the new API key
			val serverUrl = preferences[JellyseerrPreferences.serverUrl]
			if (!serverUrl.isNullOrEmpty()) {
				Timber.d("Jellyseerr Repository: Reinitializing client with new API key")
				initialize(serverUrl, apiKey)
				Timber.d("Jellyseerr Repository: Client reinitialized, saving new API key to preferences")
				preferences[JellyseerrPreferences.apiKey] = apiKey
				preferences[JellyseerrPreferences.authMethod] = "jellyfin-apikey"
				Timber.i("Jellyseerr Repository: API key regeneration completed successfully")
			} else {
				Timber.w("Jellyseerr Repository: No server URL in preferences, cannot reinitialize client")
			}
		}.onFailure { error ->
			Timber.e(error, "Jellyseerr Repository: API key regeneration failed")
		}
		
		result
	}

	override suspend fun isSessionValid(): Result<Boolean> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.success(false)
		}

		// Try to get current user - if successful, session is valid
		val result = client.getCurrentUser()
		Result.success(result.isSuccess)
	}

	override suspend fun getCurrentUser(): Result<JellyseerrUserDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getCurrentUser()
	}

	override suspend fun getRequests(
		filter: String?,
		requestedBy: Int?,
		limit: Int,
		offset: Int,
	): Result<JellyseerrListResponse<JellyseerrRequestDto>> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getRequests(filter = filter, requestedBy = requestedBy, limit = limit, offset = offset)
	}

	override suspend fun createRequest(
		mediaId: Int,
		mediaType: String,
		seasons: Seasons?,
		is4k: Boolean,
		profileId: Int?,
		rootFolderId: Int?,
		serverId: Int?,
	): Result<JellyseerrRequestDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.createRequest(mediaId, mediaType, seasons, is4k, profileId, rootFolderId, serverId)
	}

	override suspend fun deleteRequest(requestId: Int): Result<Unit> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.deleteRequest(requestId)
	}

	override suspend fun getTrendingMovies(
		limit: Int,
		offset: Int,
	): Result<JellyseerrDiscoverPageDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getTrendingMovies(limit, offset)
	}

	override suspend fun getTrendingTv(
		limit: Int,
		offset: Int,
	): Result<JellyseerrDiscoverPageDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getTrendingTv(limit, offset)
	}

	override suspend fun getTrending(
		limit: Int,
		offset: Int,
	): Result<JellyseerrDiscoverPageDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getTrending(limit, offset)
	}

	override suspend fun getTopMovies(
		limit: Int,
		offset: Int,
	): Result<JellyseerrDiscoverPageDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getTopMovies(limit, offset)
	}

	override suspend fun getTopTv(
		limit: Int,
		offset: Int,
	): Result<JellyseerrDiscoverPageDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getTopTv(limit, offset)
	}

	override suspend fun getUpcomingMovies(
		limit: Int,
		offset: Int,
	): Result<JellyseerrDiscoverPageDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getUpcomingMovies(limit, offset)
	}

	override suspend fun getUpcomingTv(
		limit: Int,
		offset: Int,
	): Result<JellyseerrDiscoverPageDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getUpcomingTv(limit, offset)
	}

	override suspend fun search(
		query: String,
		mediaType: String?,
		limit: Int,
		offset: Int,
	): Result<JellyseerrDiscoverPageDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.search(query, mediaType, limit, offset)
	}

	override suspend fun getSimilarMovies(tmdbId: Int, page: Int): Result<JellyseerrDiscoverPageDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getSimilarMovies(tmdbId, page)
	}

	override suspend fun getSimilarTv(tmdbId: Int, page: Int): Result<JellyseerrDiscoverPageDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getSimilarTv(tmdbId, page)
	}

	override suspend fun getPersonDetails(personId: Int): Result<JellyseerrPersonDetailsDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getPersonDetails(personId)
	}

	override suspend fun getPersonCombinedCredits(personId: Int): Result<JellyseerrPersonCombinedCreditsDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getPersonCombinedCredits(personId)
	}

	override suspend fun getBlacklist(): Result<JellyseerrBlacklistPageDto> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getBlacklist()
	}

	override suspend fun getRadarrSettings(): Result<List<JellyseerrRadarrSettingsDto>> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getRadarrSettings()
	}

	override suspend fun getSonarrSettings(): Result<List<JellyseerrSonarrSettingsDto>> = withContext(Dispatchers.IO) {
		ensureInitialized()

		val client = httpClient ?: run {
			return@withContext Result.failure(IllegalStateException("Jellyseerr not initialized"))
		}

		client.getSonarrSettings()
	}

	override fun close() {
		httpClient?.close()
		httpClient = null
	}
}
