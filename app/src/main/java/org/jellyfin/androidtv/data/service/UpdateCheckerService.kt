package org.jellyfin.androidtv.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jellyfin.androidtv.BuildConfig
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Service for checking GitHub releases and downloading updates
 */
class UpdateCheckerService(private val context: Context) {
	private val httpClient = OkHttpClient.Builder()
		.connectTimeout(30, TimeUnit.SECONDS)
		.readTimeout(30, TimeUnit.SECONDS)
		.build()

	private val json = Json {
		ignoreUnknownKeys = true
		isLenient = true
	}

	companion object {
		private const val GITHUB_OWNER = "ToastyToast25"
		private const val GITHUB_REPO = "VoidStream-FireTV"
		private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
		private const val GITHUB_ALL_RELEASES_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases"
		private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
	}

	private var cachedUpdateInfo: UpdateInfo? = null
	private var cacheTimestamp: Long = 0L

	@Serializable
	data class GitHubRelease(
		@SerialName("tag_name") val tagName: String,
		@SerialName("name") val name: String,
		@SerialName("body") val body: String?,
		@SerialName("html_url") val htmlUrl: String,
		@SerialName("assets") val assets: List<GitHubAsset>,
		@SerialName("published_at") val publishedAt: String,
	)

	@Serializable
	data class GitHubAsset(
		@SerialName("name") val name: String,
		@SerialName("browser_download_url") val downloadUrl: String,
		@SerialName("size") val size: Long,
	)

	data class UpdateInfo(
		val version: String,
		val releaseNotes: String,
		val downloadUrl: String,
		val releaseUrl: String,
		val isNewer: Boolean,
		val apkSize: Long,
		val publishedAt: String,
	)

	/**
	 * Check if an update is available (uses 5-minute in-memory cache)
	 */
	suspend fun checkForUpdate(forceRefresh: Boolean = false): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
		// Return cached result if still valid
		if (!forceRefresh && cachedUpdateInfo != null && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS) {
			Timber.d("Returning cached update info (age: ${(System.currentTimeMillis() - cacheTimestamp) / 1000}s)")
			return@withContext Result.success(cachedUpdateInfo)
		}

		runCatching {
			val request = Request.Builder()
				.url(GITHUB_API_URL)
				.addHeader("Accept", "application/vnd.github.v3+json")
				.build()

			httpClient.newCall(request).execute().use { response ->
				if (!response.isSuccessful) {
					Timber.e("Failed to check for updates: ${response.code}")
					return@runCatching null
				}

				val body = response.body?.string() ?: return@runCatching null
				val release = json.decodeFromString<GitHubRelease>(body)

				// Find the APK asset
				val apkAsset = release.assets.firstOrNull { asset ->
					asset.name.endsWith(".apk", ignoreCase = true) &&
						(asset.name.contains("debug", ignoreCase = true) ||
							asset.name.contains("release", ignoreCase = true))
				}

				if (apkAsset == null) {
					Timber.w("No APK found in release")
					return@runCatching null
				}

				// Compare versions
				val currentVersion = BuildConfig.VERSION_NAME
				val latestVersion = release.tagName.removePrefix("v")
				val isNewer = compareVersions(latestVersion, currentVersion) > 0

				Timber.d("Current version: $currentVersion, Latest version: $latestVersion, Is newer: $isNewer")

				val updateInfo = UpdateInfo(
					version = latestVersion,
					releaseNotes = release.body ?: "No release notes available",
					downloadUrl = apkAsset.downloadUrl,
					releaseUrl = release.htmlUrl,
					isNewer = isNewer,
					apkSize = apkAsset.size,
					publishedAt = release.publishedAt,
				)

				// Cache the result
				cachedUpdateInfo = updateInfo
				cacheTimestamp = System.currentTimeMillis()

				updateInfo
			}
		}
	}

	/**
	 * Download the APK update with retry and resume support.
	 * @param downloadUrl The URL to download from
	 * @param maxRetries Maximum number of retry attempts (default 3)
	 * @param onProgress Callback for download progress (0-100)
	 * @return The file URI of the downloaded APK
	 */
	suspend fun downloadUpdate(
		downloadUrl: String,
		maxRetries: Int = 3,
		onProgress: (Int) -> Unit = {}
	): Result<Uri> = withContext(Dispatchers.IO) {
		runCatching {
			val downloadsDir = File(context.getExternalFilesDir(null), "downloads")
			downloadsDir.mkdirs()
			val apkFile = File(downloadsDir, "update.apk")

			var attempt = 0
			var lastException: Exception? = null

			while (attempt <= maxRetries) {
				try {
					// Check how much we already have for resume
					val existingBytes = if (apkFile.exists()) apkFile.length() else 0L

					val requestBuilder = Request.Builder().url(downloadUrl)
					if (existingBytes > 0) {
						requestBuilder.addHeader("Range", "bytes=$existingBytes-")
						Timber.d("Resuming download from byte $existingBytes")
					}

					httpClient.newCall(requestBuilder.build()).execute().use { response ->
						val isResume = response.code == 206
						if (!response.isSuccessful && !isResume) {
							throw Exception("Failed to download update: ${response.code}")
						}

						val body = response.body ?: throw Exception("Empty response body")
						val contentLength = if (isResume) {
							existingBytes + body.contentLength()
						} else {
							body.contentLength()
						}

						// If server doesn't support range, start fresh
						if (!isResume && existingBytes > 0) {
							apkFile.delete()
						}

						val append = isResume
						FileOutputStream(apkFile, append).use { output ->
							val buffer = ByteArray(8192)
							var bytesRead: Int
							var totalBytesRead = if (isResume) existingBytes else 0L

							body.byteStream().use { input ->
								while (input.read(buffer).also { bytesRead = it } != -1) {
									output.write(buffer, 0, bytesRead)
									totalBytesRead += bytesRead

									if (contentLength > 0) {
										val progress = (totalBytesRead * 100 / contentLength).toInt()
										withContext(Dispatchers.Main) {
											onProgress(progress)
										}
									}
								}
							}
						}

						Timber.d("Update downloaded to: ${apkFile.absolutePath}")

						return@runCatching FileProvider.getUriForFile(
							context,
							"${context.packageName}.fileprovider",
							apkFile
						)
					}
				} catch (e: Exception) {
					lastException = e
					attempt++
					if (attempt <= maxRetries) {
						val delayMs = (1000L * (1 shl (attempt - 1))).coerceAtMost(8000L) // 1s, 2s, 4s, max 8s
						Timber.w("Download attempt $attempt failed, retrying in ${delayMs}ms: ${e.message}")
						kotlinx.coroutines.delay(delayMs)
					}
				}
			}

			throw lastException ?: Exception("Download failed after $maxRetries retries")
		}
	}

	/**
	 * Install the downloaded APK
	 */
	fun installUpdate(apkUri: Uri) {
		val intent = Intent(Intent.ACTION_VIEW).apply {
			setDataAndType(apkUri, "application/vnd.android.package-archive")
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		}
		context.startActivity(intent)
	}

	/**
	 * Fetch combined release notes for all versions between current and latest.
	 * Returns combined markdown string, or null if only one version ahead.
	 */
	suspend fun getCombinedChangelog(): String? = withContext(Dispatchers.IO) {
		try {
			val currentVersion = BuildConfig.VERSION_NAME
			val request = Request.Builder()
				.url("$GITHUB_ALL_RELEASES_URL?per_page=20")
				.addHeader("Accept", "application/vnd.github.v3+json")
				.build()

			httpClient.newCall(request).execute().use { response ->
				if (!response.isSuccessful) return@withContext null
				val body = response.body?.string() ?: return@withContext null
				val releases = json.decodeFromString<List<GitHubRelease>>(body)

				// Filter to releases newer than current version
				val newerReleases = releases.filter { release ->
					val version = release.tagName.removePrefix("v")
					compareVersions(version, currentVersion) > 0
				}.sortedByDescending { release ->
					val version = release.tagName.removePrefix("v")
					version.split(".").map { it.toIntOrNull() ?: 0 }
						.fold(0L) { acc, part -> acc * 1000 + part }
				}

				if (newerReleases.size <= 1) return@withContext null

				buildString {
					for (release in newerReleases) {
						val version = release.tagName.removePrefix("v")
						appendLine("## Version $version")
						appendLine()
						appendLine(release.body ?: "No release notes")
						appendLine()
						appendLine("---")
						appendLine()
					}
				}.trimEnd()
			}
		} catch (e: Exception) {
			Timber.w(e, "Failed to fetch combined changelog")
			null
		}
	}

	/**
	 * Compare two semantic version strings
	 * Returns: negative if v1 < v2, zero if v1 == v2, positive if v1 > v2
	 */
	fun compareVersions(v1: String, v2: String): Int {
		val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
		val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

		val maxLength = maxOf(parts1.size, parts2.size)
		for (i in 0 until maxLength) {
			val p1 = parts1.getOrNull(i) ?: 0
			val p2 = parts2.getOrNull(i) ?: 0
			if (p1 != p2) return p1 - p2
		}
		return 0
	}
}
