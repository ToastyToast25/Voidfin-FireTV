package org.jellyfin.androidtv.data.service

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jellyfin.androidtv.BuildConfig
import timber.log.Timber
import java.util.concurrent.TimeUnit

class IssueReporterService(
	private val context: Context,
	private val logCollector: AppLogCollector,
) {
	companion object {
		private const val GITHUB_OWNER = "ToastyToast25"
		private const val GITHUB_REPO = "VoidStream-FireTV"
		private const val ISSUES_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/issues"
		private const val SEARCH_URL = "https://api.github.com/search/issues"
		private const val BASE_COOLDOWN_MS = 30 * 1000L // 30 seconds base
		private const val MAX_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes max cap
		private const val HISTORY_WINDOW_MS = 15 * 60 * 1000L // 15 minute tracking window
		private const val PREFS_NAME = "issue_reporter"
		private const val KEY_LAST_SUBMIT = "last_submit_time"
		private const val KEY_SUBMISSION_HISTORY = "submission_history"
	}

	private val httpClient = OkHttpClient.Builder()
		.connectTimeout(15, TimeUnit.SECONDS)
		.readTimeout(15, TimeUnit.SECONDS)
		.build()

	private val json = Json {
		ignoreUnknownKeys = true
		encodeDefaults = true
	}

	enum class IssueCategory(val label: String) {
		UPDATE_DOWNLOAD_FAILED("Update failed to download"),
		UPDATE_INSTALL_FAILED("Update failed to install"),
		APP_CRASH("App crash / freeze"),
		DISPLAY_ISSUE("Display or UI issue"),
		CONNECTION_ISSUE("Connection issue"),
		OTHER("Other"),
	}

	@Serializable
	private data class CreateIssueRequest(
		val title: String,
		val body: String,
		val labels: List<String> = listOf("user-report"),
	)

	@Serializable
	private data class CreateIssueResponse(
		@SerialName("number") val number: Int,
		@SerialName("html_url") val htmlUrl: String,
	)

	@Serializable
	private data class SearchResponse(
		@SerialName("total_count") val totalCount: Int,
		@SerialName("items") val items: List<SearchItem>,
	)

	@Serializable
	private data class SearchItem(
		@SerialName("number") val number: Int,
		@SerialName("title") val title: String,
		@SerialName("state") val state: String,
		@SerialName("html_url") val htmlUrl: String,
	)

	enum class TokenStatus {
		VALID,
		EXPIRED_OR_INVALID,
		NOT_CONFIGURED,
		UNKNOWN,
	}

	private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

	/**
	 * Check if the GitHub PAT is valid by hitting the rate_limit endpoint.
	 * Returns the token status. Caches result for 1 hour.
	 */
	suspend fun checkTokenStatus(): TokenStatus = withContext(Dispatchers.IO) {
		val token = BuildConfig.GITHUB_ISSUE_TOKEN
		if (token.isBlank()) return@withContext TokenStatus.NOT_CONFIGURED

		// Check cache
		val cachedStatus = prefs.getString("token_status", null)
		val cachedTime = prefs.getLong("token_status_time", 0)
		if (cachedStatus != null && System.currentTimeMillis() - cachedTime < 60 * 60 * 1000L) {
			return@withContext try { TokenStatus.valueOf(cachedStatus) } catch (_: Exception) { TokenStatus.UNKNOWN }
		}

		try {
			val request = Request.Builder()
				.url("https://api.github.com/rate_limit")
				.addHeader("Accept", "application/vnd.github.v3+json")
				.addHeader("Authorization", "Bearer $token")
				.build()

			val status = httpClient.newCall(request).execute().use { response ->
				when {
					response.isSuccessful -> TokenStatus.VALID
					response.code == 401 -> TokenStatus.EXPIRED_OR_INVALID
					else -> TokenStatus.UNKNOWN
				}
			}

			// Cache result
			prefs.edit()
				.putString("token_status", status.name)
				.putLong("token_status_time", System.currentTimeMillis())
				.apply()

			status
		} catch (e: Exception) {
			Timber.w(e, "Failed to check token status")
			TokenStatus.UNKNOWN
		}
	}

	/**
	 * Get submission history and clean up old entries (older than 15 minutes).
	 * Returns list of timestamps within the tracking window.
	 */
	private fun getSubmissionHistory(): List<Long> {
		val historyString = prefs.getString(KEY_SUBMISSION_HISTORY, "") ?: ""
		if (historyString.isBlank()) return emptyList()

		val currentTime = System.currentTimeMillis()
		val cutoffTime = currentTime - HISTORY_WINDOW_MS

		// Parse timestamps and filter out old ones
		val recentSubmissions = historyString.split(",")
			.mapNotNull { it.toLongOrNull() }
			.filter { it > cutoffTime }

		// Save cleaned history back to prefs
		if (recentSubmissions.size != historyString.split(",").size) {
			saveSubmissionHistory(recentSubmissions)
		}

		return recentSubmissions
	}

	/**
	 * Save submission history to SharedPreferences.
	 */
	private fun saveSubmissionHistory(timestamps: List<Long>) {
		val historyString = timestamps.joinToString(",")
		prefs.edit().putString(KEY_SUBMISSION_HISTORY, historyString).apply()
	}

	/**
	 * Calculate progressive cooldown based on recent submission count.
	 * - 1st submission: 30 seconds
	 * - 2nd within 15 min: 1 minute
	 * - 3rd within 15 min: 2 minutes
	 * - 4th+ within 15 min: 5 minutes (capped)
	 */
	private fun calculateProgressiveCooldown(submissionCount: Int): Long {
		return when (submissionCount) {
			0, 1 -> BASE_COOLDOWN_MS // 30 seconds
			2 -> 60 * 1000L // 1 minute
			3 -> 120 * 1000L // 2 minutes
			else -> MAX_COOLDOWN_MS // 5 minutes (cap)
		}
	}

	/**
	 * Returns remaining cooldown in seconds, or 0 if ready to submit.
	 * Uses progressive cooldown based on recent submission history.
	 */
	fun getCooldownRemaining(): Long {
		val lastSubmit = prefs.getLong(KEY_LAST_SUBMIT, 0)
		val elapsed = System.currentTimeMillis() - lastSubmit

		// Get recent submission history to determine cooldown period
		val history = getSubmissionHistory()
		val cooldownPeriod = calculateProgressiveCooldown(history.size)

		val remaining = cooldownPeriod - elapsed
		return if (remaining > 0) remaining / 1000 else 0
	}

	/**
	 * Get the current progressive cooldown duration in seconds.
	 * This is used by the UI to show the actual cooldown time.
	 */
	fun getProgressiveCooldownDuration(): Long {
		val history = getSubmissionHistory()
		return calculateProgressiveCooldown(history.size) / 1000
	}

	/**
	 * Check for existing open issues with similar category.
	 * Returns the issue number if a duplicate is found, null otherwise.
	 */
	suspend fun findDuplicate(category: IssueCategory): Int? = withContext(Dispatchers.IO) {
		try {
			val token = BuildConfig.GITHUB_ISSUE_TOKEN
			if (token.isBlank()) return@withContext null

			val query = "repo:$GITHUB_OWNER/$GITHUB_REPO is:issue is:open label:user-report \"${category.label}\" in:title"
			val url = "$SEARCH_URL?q=${java.net.URLEncoder.encode(query, "UTF-8")}&per_page=1"

			val request = Request.Builder()
				.url(url)
				.addHeader("Accept", "application/vnd.github.v3+json")
				.addHeader("Authorization", "Bearer $token")
				.build()

			httpClient.newCall(request).execute().use { response ->
				if (!response.isSuccessful) return@withContext null
				val body = response.body?.string() ?: return@withContext null
				val searchResult = json.decodeFromString<SearchResponse>(body)
				if (searchResult.totalCount > 0) searchResult.items.firstOrNull()?.number else null
			}
		} catch (e: Exception) {
			Timber.w(e, "Failed to check for duplicate issues")
			null
		}
	}

	suspend fun submitIssue(
		category: IssueCategory,
		description: String,
		updateVersion: String,
	): Result<Int> = withContext(Dispatchers.IO) {
		runCatching {
			// Check rate limit
			val cooldown = getCooldownRemaining()
			if (cooldown > 0) {
				throw Exception("Please wait $cooldown seconds before submitting another report")
			}

			val token = BuildConfig.GITHUB_ISSUE_TOKEN
			if (token.isBlank()) throw Exception("Issue reporting not configured")

			val title = "[User Report] ${category.label}"

			val body = buildString {
				appendLine("**Category:** ${category.label}")
				if (description.isNotBlank()) {
					appendLine("**Description:** $description")
				}
				appendLine()
				appendLine("---")
				appendLine("**Device Info**")
				appendLine("- App Version: ${BuildConfig.VERSION_NAME}")
				appendLine("- Update Version: $updateVersion")
				appendLine("- Device: ${Build.MANUFACTURER} ${Build.MODEL}")
				appendLine("- Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
				appendLine("- Build: ${Build.DISPLAY}")

				// Append logs (ring buffer + crash + logcat) in collapsed sections
				append(logCollector.buildLogSection())
			}

			val requestBody = json.encodeToString(CreateIssueRequest(
				title = title,
				body = body,
			))

			val request = Request.Builder()
				.url(ISSUES_URL)
				.addHeader("Accept", "application/vnd.github.v3+json")
				.addHeader("Authorization", "Bearer $token")
				.post(requestBody.toRequestBody("application/json".toMediaType()))
				.build()

			httpClient.newCall(request).execute().use { response ->
				if (!response.isSuccessful) {
					val errorBody = response.body?.string() ?: "No response body"
					Timber.e("Failed to create issue: ${response.code} - $errorBody")
					throw Exception("Failed to submit report (${response.code})")
				}

				val responseBody = response.body?.string() ?: throw Exception("Empty response")
				val issueResponse = json.decodeFromString<CreateIssueResponse>(responseBody)
				Timber.i("Issue created: #${issueResponse.number} - ${issueResponse.htmlUrl}")

				// Record submission time for rate limiting and progressive cooldown
				val currentTime = System.currentTimeMillis()
				prefs.edit().putLong(KEY_LAST_SUBMIT, currentTime).apply()

				// Add to submission history
				val history = getSubmissionHistory().toMutableList()
				history.add(currentTime)
				saveSubmissionHistory(history)

				issueResponse.number
			}
		}
	}
}
