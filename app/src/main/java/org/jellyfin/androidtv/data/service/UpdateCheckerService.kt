package org.jellyfin.androidtv.data.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
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
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Service for checking GitHub releases and downloading updates
 */
class UpdateCheckerService(private val context: Context) {
	private val httpClient = OkHttpClient.Builder()
		.connectTimeout(30, TimeUnit.SECONDS)
		.readTimeout(30, TimeUnit.SECONDS)
		.build()

	private val downloadClient = OkHttpClient.Builder()
		.connectTimeout(30, TimeUnit.SECONDS)
		.readTimeout(60, TimeUnit.SECONDS)
		.followRedirects(true)
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
		private const val PREFS_NAME = "update_checker"
		private const val KEY_PENDING_WHATS_NEW_VERSION = "pending_whats_new_version"
		private const val KEY_PENDING_WHATS_NEW_NOTES = "pending_whats_new_notes"
		const val ACTION_INSTALL_STATUS = "org.voidstream.androidtv.INSTALL_STATUS"
	}

	private var cachedUpdateInfo: UpdateInfo? = null
	private var cacheTimestamp: Long = 0L

	private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

	@Serializable
	data class GitHubRelease(
		@SerialName("tag_name") val tagName: String,
		@SerialName("name") val name: String,
		@SerialName("body") val body: String?,
		@SerialName("html_url") val htmlUrl: String,
		@SerialName("assets") val assets: List<GitHubAsset>,
		@SerialName("published_at") val publishedAt: String,
		@SerialName("prerelease") val prerelease: Boolean = false,
	)

	@Serializable
	data class GitHubAsset(
		@SerialName("name") val name: String,
		@SerialName("browser_download_url") val downloadUrl: String,
		@SerialName("size") val size: Long,
		@SerialName("digest") val digest: String? = null,
	)

	data class UpdateInfo(
		val version: String,
		val releaseNotes: String,
		val downloadUrl: String,
		val releaseUrl: String,
		val isNewer: Boolean,
		val apkSize: Long,
		val publishedAt: String,
		val expectedSha256: String? = null,
		val isForced: Boolean = true,
		val isBeta: Boolean = false,
	)

	/**
	 * Check if the device has an active network connection.
	 */
	fun isNetworkAvailable(): Boolean {
		val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
		val network = cm.activeNetwork ?: return false
		val capabilities = cm.getNetworkCapabilities(network) ?: return false
		return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
	}

	/**
	 * Check if an update is available (uses 5-minute in-memory cache).
	 * @param forceRefresh Bypass cache
	 * @param includePrereleases Include beta/pre-release builds
	 */
	suspend fun checkForUpdate(
		forceRefresh: Boolean = false,
		includePrereleases: Boolean = false,
	): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
		// Return cached result if still valid
		if (!forceRefresh && cachedUpdateInfo != null && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS) {
			Timber.d("Returning cached update info (age: ${(System.currentTimeMillis() - cacheTimestamp) / 1000}s)")
			return@withContext Result.success(cachedUpdateInfo)
		}

		runCatching {
			val release = if (includePrereleases) {
				fetchNewestRelease(includePrereleases = true)
			} else {
				fetchLatestStableRelease()
			} ?: return@runCatching null

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
				expectedSha256 = apkAsset.digest?.removePrefix("sha256:"),
				isForced = release.body?.contains("[FORCE]") == true,
				isBeta = release.prerelease,
			)

			// Cache the result
			cachedUpdateInfo = updateInfo
			cacheTimestamp = System.currentTimeMillis()

			updateInfo
		}
	}

	/**
	 * Fetch the latest stable release (excludes pre-releases).
	 */
	private fun fetchLatestStableRelease(): GitHubRelease? {
		val request = Request.Builder()
			.url(GITHUB_API_URL)
			.addHeader("Accept", "application/vnd.github.v3+json")
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				Timber.e("Failed to check for updates: ${response.code}")
				return null
			}
			val body = response.body?.string() ?: return null
			return json.decodeFromString<GitHubRelease>(body)
		}
	}

	/**
	 * Fetch the newest release including pre-releases.
	 */
	private fun fetchNewestRelease(includePrereleases: Boolean): GitHubRelease? {
		val request = Request.Builder()
			.url("$GITHUB_ALL_RELEASES_URL?per_page=10")
			.addHeader("Accept", "application/vnd.github.v3+json")
			.build()

		httpClient.newCall(request).execute().use { response ->
			if (!response.isSuccessful) {
				Timber.e("Failed to fetch releases: ${response.code}")
				return null
			}
			val body = response.body?.string() ?: return null
			val releases = json.decodeFromString<List<GitHubRelease>>(body)

			// Find the newest release (including pre-releases if enabled)
			return releases
				.filter { includePrereleases || !it.prerelease }
				.maxByOrNull { release ->
					val version = release.tagName.removePrefix("v")
					version.split(".").map { it.toIntOrNull() ?: 0 }
						.fold(0L) { acc, part -> acc * 1000 + part }
				}
		}
	}

	/**
	 * Download the APK update with retry, resume, and optional checksum verification.
	 * @param downloadUrl The URL to download from
	 * @param expectedSha256 Expected SHA-256 hash for verification (null to skip)
	 * @param maxRetries Maximum number of retry attempts (default 3)
	 * @param onProgress Callback for download progress (downloadedBytes, totalBytes)
	 * @return The file URI of the downloaded APK
	 */
	suspend fun downloadUpdate(
		downloadUrl: String,
		expectedSha256: String? = null,
		maxRetries: Int = 3,
		onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
	): Result<Uri> = withContext(Dispatchers.IO) {
		runCatching {
			// Prefer external storage, fall back to internal if unavailable
			val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
			Timber.d("Download base dir: ${baseDir.absolutePath} (exists=${baseDir.exists()}, free=${baseDir.freeSpace / 1024 / 1024}MB)")
			val downloadsDir = File(baseDir, "downloads")
			downloadsDir.mkdirs()
			val apkFile = File(downloadsDir, "update.apk")

			// If a complete file already exists from a previous download, verify it
			// and skip re-downloading if valid (handles retry after install failure)
			if (apkFile.exists() && apkFile.length() > 0) {
				if (expectedSha256 != null) {
					Timber.d("Existing APK found (${apkFile.length()} bytes), verifying checksum...")
					if (verifyChecksum(apkFile, expectedSha256)) {
						Timber.i("Existing APK passes checksum, skipping download")
						withContext(Dispatchers.Main) { onProgress(apkFile.length(), apkFile.length()) }
						return@runCatching FileProvider.getUriForFile(
							context,
							"${context.packageName}.fileprovider",
							apkFile
						)
					}
					Timber.w("Existing APK failed checksum, re-downloading")
					apkFile.delete()
				} else {
					Timber.d("Existing APK found but no checksum to verify, re-downloading fresh")
					apkFile.delete()
				}
			}

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

					downloadClient.newCall(requestBuilder.build()).execute().use { response ->
						// Handle 416 Range Not Satisfiable — file may already be complete
						if (response.code == 416) {
							Timber.d("Got 416 Range Not Satisfiable, deleting partial file and retrying fresh")
							apkFile.delete()
							throw Exception("Range not satisfiable, retrying from start")
						}

						val isResume = response.code == 206
						if (!response.isSuccessful && !isResume) {
							throw Exception("Failed to download update: HTTP ${response.code}")
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
										withContext(Dispatchers.Main) {
											onProgress(totalBytesRead, contentLength)
										}
									}
								}
							}
						}

						Timber.d("Update downloaded to: ${apkFile.absolutePath} (${apkFile.length()} bytes)")

						// Verify checksum if provided
						if (expectedSha256 != null) {
							Timber.d("Verifying SHA-256 checksum...")
							if (!verifyChecksum(apkFile, expectedSha256)) {
								apkFile.delete()
								throw Exception("Checksum verification failed")
							}
							Timber.i("Checksum verification passed")
						}

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
	 * Verify the SHA-256 checksum of a downloaded file.
	 */
	private fun verifyChecksum(file: File, expectedSha256: String): Boolean {
		val digest = MessageDigest.getInstance("SHA-256")
		file.inputStream().use { input ->
			val buffer = ByteArray(8192)
			var bytesRead: Int
			while (input.read(buffer).also { bytesRead = it } != -1) {
				digest.update(buffer, 0, bytesRead)
			}
		}
		val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
		val matches = actualHash.equals(expectedSha256, ignoreCase = true)
		if (!matches) {
			Timber.e("Checksum mismatch: expected=$expectedSha256, actual=$actualHash")
		}
		return matches
	}

	/**
	 * Install the downloaded APK.
	 * Uses intent-based install first (directly launches the visible installer UI),
	 * falls back to PackageInstaller session API if the intent fails.
	 */
	fun installUpdate(apkUri: Uri): Boolean {
		val apkFile = getApkFile()
		if (apkFile == null || !apkFile.exists()) {
			Timber.e("APK file not found for installation")
			return false
		}
		Timber.d("Installing APK: ${apkFile.absolutePath} (size=${apkFile.length()} bytes)")

		// Try intent-based install first — this directly launches the system installer UI
		// which is more reliable than PackageInstaller sessions (which can silently fail
		// to show the confirmation dialog on some devices/emulators)
		return try {
			installViaIntent(apkUri)
		} catch (e: Exception) {
			Timber.e(e, "Intent-based install failed, trying PackageInstaller session")
			try {
				installViaPackageInstaller(apkFile)
			} catch (e2: Exception) {
				Timber.e(e2, "PackageInstaller also failed")
				false
			}
		}
	}

	private fun installViaPackageInstaller(apkFile: File): Boolean {
		val installer = context.packageManager.packageInstaller
		val params = PackageInstaller.SessionParams(
			PackageInstaller.SessionParams.MODE_FULL_INSTALL
		).apply {
			setSize(apkFile.length())
		}

		val sessionId = installer.createSession(params)
		val session = installer.openSession(sessionId)

		try {
			// Write APK into the session
			session.openWrite("update.apk", 0, apkFile.length()).use { output ->
				apkFile.inputStream().use { input ->
					input.copyTo(output)
				}
				session.fsync(output)
			}

			// Create a status receiver intent
			val intent = Intent(ACTION_INSTALL_STATUS).apply {
				setPackage(context.packageName)
			}
			val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
			} else {
				PendingIntent.FLAG_UPDATE_CURRENT
			}
			val pendingIntent = PendingIntent.getBroadcast(
				context, sessionId, intent, flags
			)

			// Commit the session — system will show install confirmation
			session.commit(pendingIntent.intentSender)
			Timber.i("PackageInstaller session $sessionId committed")
			return true
		} catch (e: Exception) {
			session.abandon()
			throw e
		}
	}

	@Suppress("DEPRECATION")
	private fun installViaIntent(apkUri: Uri): Boolean {
		return try {
			val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
				data = apkUri
				addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
			}
			context.startActivity(intent)
			true
		} catch (e: Exception) {
			Timber.e(e, "Intent-based install also failed")
			false
		}
	}

	private fun getApkFile(): File? {
		val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
		val apkFile = File(baseDir, "downloads/update.apk")
		return if (apkFile.exists()) apkFile else null
	}

	/**
	 * Save pending "What's New" info to show after the update installs.
	 */
	fun savePendingWhatsNew(version: String, notes: String) {
		Timber.i("Saving pending What's New for version $version")
		prefs.edit()
			.putString(KEY_PENDING_WHATS_NEW_VERSION, version)
			.putString(KEY_PENDING_WHATS_NEW_NOTES, notes)
			.commit() // Use commit() (synchronous) to ensure data is written before install starts
	}

	/**
	 * Get and clear pending "What's New" info. Returns (version, notes) or null.
	 */
	fun getPendingWhatsNew(): Pair<String, String>? {
		val version = prefs.getString(KEY_PENDING_WHATS_NEW_VERSION, null) ?: return null
		val notes = prefs.getString(KEY_PENDING_WHATS_NEW_NOTES, null) ?: return null
		// Clear after reading
		prefs.edit()
			.remove(KEY_PENDING_WHATS_NEW_VERSION)
			.remove(KEY_PENDING_WHATS_NEW_NOTES)
			.apply()
		return Pair(version, notes)
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
