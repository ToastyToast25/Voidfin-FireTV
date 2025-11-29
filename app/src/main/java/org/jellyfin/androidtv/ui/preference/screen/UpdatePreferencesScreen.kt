package org.jellyfin.androidtv.ui.preference.screen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.UpdateCheckerService
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.action
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.inject
import timber.log.Timber

class UpdatePreferencesScreen : OptionsFragment() {
	private val updateChecker by inject<UpdateCheckerService>()
	private var isChecking = false
	private var isDownloading = false

	override val screen by optionsScreen {
		setTitle(R.string.pref_updates_title)

		category {
			setTitle(R.string.pref_updates_info)

			action {
				setTitle(R.string.pref_current_version)
				content = BuildConfig.VERSION_NAME
			}
		}

		category {
			setTitle(R.string.pref_updates_check)

			action {
				setTitle(R.string.pref_check_for_updates)
				setContent(R.string.pref_check_for_updates_description)
				onActivate = {
					checkForUpdates()
				}
			}
		}
	}

	private fun checkForUpdates() {
		if (isChecking) return
		isChecking = true

		lifecycleScope.launch {
			try {
				toast(getString(R.string.msg_checking_for_updates))

				val result = updateChecker.checkForUpdate()
				result.fold(
					onSuccess = { updateInfo ->
						if (updateInfo == null) {
							toast(getString(R.string.msg_update_check_failed))
						} else if (!updateInfo.isNewer) {
							toast(getString(R.string.msg_no_updates_available))
						} else {
							showUpdateAvailableDialog(updateInfo)
						}
					},
					onFailure = { error ->
						Timber.e(error, "Failed to check for updates")
						toast(getString(R.string.msg_update_check_failed))
					}
				)
			} finally {
				isChecking = false
			}
		}
	}

	private fun showUpdateAvailableDialog(updateInfo: UpdateCheckerService.UpdateInfo) {
		val sizeMB = updateInfo.apkSize / (1024.0 * 1024.0)
		val message = "New version ${updateInfo.version} is available!\n\nSize: ${String.format("%.1f", sizeMB)} MB"

		androidx.appcompat.app.AlertDialog.Builder(requireContext())
			.setTitle(R.string.title_update_available)
			.setMessage(message)
			.setPositiveButton(R.string.btn_download) { _, _ ->
				downloadAndInstall(updateInfo)
			}
			.setNegativeButton(R.string.btn_later, null)
			.setNeutralButton(R.string.btn_view_release_notes) { _, _ ->
				showReleaseNotesDialog(updateInfo)
			}
			.show()
	}

	private fun showReleaseNotesDialog(updateInfo: UpdateCheckerService.UpdateInfo) {
		val sizeMB = updateInfo.apkSize / (1024.0 * 1024.0)
		
		// Create WebView for HTML content
		val webView = WebView(requireContext()).apply {
			layoutParams = LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				(resources.displayMetrics.heightPixels * 0.85).toInt()
			)
			settings.apply {
				javaScriptEnabled = false
				defaultTextEncodingName = "utf-8"
			}
			
			// Convert markdown to HTML with dark theme styling
			val htmlContent = buildString {
				append("<!DOCTYPE html><html><head>")
				append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
				append("<style>")
				append("body { font-family: sans-serif; padding: 16px; background-color: #1a1a1a; color: #e0e0e0; margin: 0; }")
				append("h1, h2, h3 { color: #ffffff; margin-top: 16px; margin-bottom: 8px; }")
				append("h1 { font-size: 1.5em; }")
				append("h2 { font-size: 1.3em; }")
				append("h3 { font-size: 1.1em; }")
				append("p { margin: 8px 0; line-height: 1.5; }")
				append("ul, ol { margin: 8px 0; padding-left: 24px; line-height: 1.6; }")
				append("li { margin: 4px 0; }")
				append("code { background-color: #2d2d2d; padding: 2px 6px; border-radius: 3px; font-family: monospace; color: #f0f0f0; }")
				append("pre { background-color: #2d2d2d; padding: 12px; border-radius: 4px; overflow-x: auto; }")
				append("pre code { background-color: transparent; padding: 0; }")
				append("a { color: #64b5f6; text-decoration: none; }")
				append("blockquote { border-left: 3px solid #64b5f6; margin: 8px 0; padding-left: 12px; color: #b0b0b0; }")
				append("strong { color: #ffffff; }")
				append("hr { border: none; border-top: 1px solid #404040; margin: 16px 0; }")
				append("</style></head><body>")
				append("<h2>Version ${updateInfo.version}</h2>")
				append("<p><strong>Size:</strong> ${String.format("%.1f", sizeMB)} MB</p>")
				append("<hr>")
				
				// Convert basic markdown to HTML
				val releaseNotes = updateInfo.releaseNotes
					.replace("### ", "<h3>")
					.replace("## ", "<h2>")
					.replace("# ", "<h1>")
					.replace(Regex("(?<!<h[1-3]>)(.+)"), "$1</p>")
					.replace(Regex("<h([1-3])>(.+?)</p>"), "<h$1>$2</h$1>")
					.replace(Regex("^- (.+)"), "<li>$1</li>")
					.replace(Regex("((?:<li>.*</li>\n?)+)"), "<ul>$1</ul>")
					.replace(Regex("^\\* (.+)"), "<li>$1</li>")
					.replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
					.replace(Regex("`(.+?)`"), "<code>$1</code>")
					.replace("\n\n", "</p><p>")
					.replace(Regex("^(?!<[uh]|<li|<p)(.+)"), "<p>$1")
				
				append(releaseNotes)
				append("</body></html>")
			}
			
			loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
		}
		
		// Create container with padding
		val container = LinearLayout(requireContext()).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(48, 24, 48, 24) // Add horizontal padding for wider appearance
			addView(webView)
		}

		androidx.appcompat.app.AlertDialog.Builder(requireContext())
			.setTitle(R.string.title_update_available)
			.setView(container)
			.setPositiveButton(R.string.btn_download) { _, _ ->
				downloadAndInstall(updateInfo)
			}
			.setNegativeButton(R.string.btn_later, null)
			.setNeutralButton(R.string.btn_view_on_github) { _, _ ->
				openUrl(updateInfo.releaseUrl)
			}
			.show()
			.apply {
				// Make dialog wider
				window?.setLayout(
					(resources.displayMetrics.widthPixels * 0.90).toInt(),
					ViewGroup.LayoutParams.WRAP_CONTENT
				)
			}
	}

	private fun downloadAndInstall(updateInfo: UpdateCheckerService.UpdateInfo) {
		// Check for install permission on Android 8.0+
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (!requireContext().packageManager.canRequestPackageInstalls()) {
				// Show dialog to take user to settings
				androidx.appcompat.app.AlertDialog.Builder(requireContext())
					.setTitle(R.string.msg_install_permission_required)
					.setMessage(R.string.msg_install_permission_required_description)
					.setPositiveButton(R.string.btn_open_settings) { _, _ ->
						openInstallPermissionSettings()
					}
					.setNegativeButton(R.string.btn_cancel, null)
					.show()
				return
			}
		}

		if (isDownloading) return
		isDownloading = true

		lifecycleScope.launch {
			try {
				toast(getString(R.string.msg_downloading_update))

				val result = updateChecker.downloadUpdate(updateInfo.downloadUrl) { progress ->
					// Could update a progress bar here
					Timber.d("Download progress: $progress%")
				}

				result.fold(
					onSuccess = { apkUri ->
						toast(getString(R.string.msg_update_downloaded))
						updateChecker.installUpdate(apkUri)
					},
					onFailure = { error ->
						Timber.e(error, "Failed to download update")
						toast(getString(R.string.msg_download_failed))
					}
				)
			} finally {
				isDownloading = false
			}
		}
	}

	private fun openUrl(url: String) {
		try {
			val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
			startActivity(intent)
		} catch (e: Exception) {
			Timber.e(e, "Failed to open URL")
			toast(getString(R.string.msg_failed_to_open_url))
		}
	}

	private fun openInstallPermissionSettings() {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
					data = Uri.parse("package:${requireContext().packageName}")
				}
				startActivity(intent)
			}
		} catch (e: Exception) {
			Timber.e(e, "Failed to open install permission settings")
			toast(getString(R.string.msg_failed_to_open_settings))
		}
	}

	private fun toast(message: String) {
		android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
	}
}
