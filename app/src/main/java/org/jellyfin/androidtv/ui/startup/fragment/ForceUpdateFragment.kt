package org.jellyfin.androidtv.ui.startup.fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.IssueReporterService
import org.jellyfin.androidtv.data.service.UpdateCheckerService
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.koin.android.ext.android.inject
import timber.log.Timber

class ForceUpdateFragment : Fragment() {
	companion object {
		const val ARG_VERSION = "version"
		const val ARG_DOWNLOAD_URL = "download_url"
		const val ARG_APK_SIZE = "apk_size"
		const val ARG_RELEASE_NOTES = "release_notes"
		const val ARG_RELEASE_URL = "release_url"
		const val ARG_PUBLISHED_AT = "published_at"
		const val ARG_EXPECTED_SHA256 = "expected_sha256"
		const val ARG_IS_BETA = "is_beta"

		fun newInstance(updateInfo: UpdateCheckerService.UpdateInfo) = ForceUpdateFragment().apply {
			arguments = Bundle().apply {
				putString(ARG_VERSION, updateInfo.version)
				putString(ARG_DOWNLOAD_URL, updateInfo.downloadUrl)
				putLong(ARG_APK_SIZE, updateInfo.apkSize)
				putString(ARG_RELEASE_NOTES, updateInfo.releaseNotes)
				putString(ARG_RELEASE_URL, updateInfo.releaseUrl)
				putString(ARG_PUBLISHED_AT, updateInfo.publishedAt)
				putString(ARG_EXPECTED_SHA256, updateInfo.expectedSha256)
				putBoolean(ARG_IS_BETA, updateInfo.isBeta)
			}
		}
	}

	private val updateCheckerService: UpdateCheckerService by inject()
	private val issueReporterService: IssueReporterService by inject()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	) = content {
		JellyfinTheme {
			ForceUpdateScreen(
				version = arguments?.getString(ARG_VERSION) ?: "",
				apkSize = arguments?.getLong(ARG_APK_SIZE) ?: 0L,
				releaseNotes = arguments?.getString(ARG_RELEASE_NOTES) ?: "",
				publishedAt = arguments?.getString(ARG_PUBLISHED_AT) ?: "",
				isBeta = arguments?.getBoolean(ARG_IS_BETA) ?: false,
				issueReporterService = issueReporterService,
				onDownloadAndInstall = { onProgress ->
					downloadAndInstall(
						downloadUrl = arguments?.getString(ARG_DOWNLOAD_URL) ?: "",
						expectedSha256 = arguments?.getString(ARG_EXPECTED_SHA256),
						version = arguments?.getString(ARG_VERSION) ?: "",
						releaseNotes = arguments?.getString(ARG_RELEASE_NOTES) ?: "",
						onProgress = onProgress,
					)
				}
			)
		}
	}

	private suspend fun downloadAndInstall(
		downloadUrl: String,
		expectedSha256: String?,
		version: String,
		releaseNotes: String,
		onProgress: (downloaded: Long, total: Long) -> Unit,
	): Boolean {
		// Network check
		if (!updateCheckerService.isNetworkAvailable()) {
			withContext(Dispatchers.Main) {
				Toast.makeText(requireContext(), getString(R.string.force_update_no_network), Toast.LENGTH_LONG).show()
			}
			return false
		}

		// Check install permission on Android 8+
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			if (!requireContext().packageManager.canRequestPackageInstalls()) {
				withContext(Dispatchers.Main) {
					Toast.makeText(requireContext(), getString(R.string.force_update_install_permission), Toast.LENGTH_LONG).show()
					val intent = Intent(
						Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
						android.net.Uri.parse("package:${requireContext().packageName}")
					)
					startActivity(intent)
				}
				return false
			}
		}

		return try {
			val result = updateCheckerService.downloadUpdate(
				downloadUrl = downloadUrl,
				expectedSha256 = expectedSha256,
				onProgress = onProgress,
			)
			result.fold(
				onSuccess = { apkUri ->
					// Save What's New for display after update installs
					updateCheckerService.savePendingWhatsNew(version, releaseNotes)
					updateCheckerService.installUpdate(apkUri)
					true
				},
				onFailure = { error ->
					Timber.e(error, "Failed to download update")
					false
				}
			)
		} catch (e: Exception) {
			Timber.e(e, "Error downloading update")
			false
		}
	}
}

private enum class UpdateState {
	READY,
	DOWNLOADING,
	VERIFYING,
	INSTALLING,
	FAILED
}

private fun formatPublishedDate(isoDate: String): String {
	return try {
		val instant = java.time.Instant.parse(isoDate)
		val zoned = instant.atZone(java.time.ZoneId.systemDefault())
		java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy").format(zoned)
	} catch (_: Exception) {
		isoDate
	}
}

@Composable
private fun ForceUpdateScreen(
	version: String,
	apkSize: Long,
	releaseNotes: String,
	publishedAt: String,
	isBeta: Boolean,
	issueReporterService: IssueReporterService,
	onDownloadAndInstall: suspend ((downloaded: Long, total: Long) -> Unit) -> Boolean,
) {
	var state by remember { mutableStateOf(UpdateState.READY) }
	var downloadedBytes by remember { mutableLongStateOf(0L) }
	var totalBytes by remember { mutableLongStateOf(0L) }
	var showReleaseNotes by remember { mutableStateOf(false) }
	var showReportIssue by remember { mutableStateOf(false) }
	val scope = rememberCoroutineScope()

	val sizeMB = String.format("%.1f", apkSize / (1024.0 * 1024.0))
	val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(colorResource(id = R.color.not_quite_black)),
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
			modifier = Modifier.fillMaxSize(),
		) {
			// Logo
			Image(
				painter = painterResource(R.drawable.app_logo),
				contentDescription = stringResource(R.string.app_name),
				modifier = Modifier
					.width(200.dp)
					.height(120.dp)
			)

			Spacer(modifier = Modifier.height(32.dp))

			// Update card
			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
				modifier = Modifier
					.width(480.dp)
					.background(
						color = Color(0xFF1A1212),
						shape = RoundedCornerShape(16.dp)
					)
					.border(
						width = 1.dp,
						color = Color(0xFF3A1A1A),
						shape = RoundedCornerShape(16.dp)
					)
					.padding(32.dp)
			) {
				// Title
				Text(
					text = stringResource(R.string.force_update_title),
					color = Color.White,
					fontSize = 24.sp,
					fontWeight = FontWeight.Bold,
				)

				Spacer(modifier = Modifier.height(16.dp))

				// Message
				Text(
					text = stringResource(R.string.force_update_message, version),
					color = Color(0xFFB0B0B0),
					fontSize = 16.sp,
					textAlign = TextAlign.Center,
					lineHeight = 22.sp,
				)

				Spacer(modifier = Modifier.height(16.dp))

				// Version info and details
				Column(
					horizontalAlignment = Alignment.CenterHorizontally,
					modifier = Modifier
						.fillMaxWidth()
						.background(
							color = Color(0xFF140E0E),
							shape = RoundedCornerShape(8.dp)
						)
						.padding(horizontal = 16.dp, vertical = 12.dp),
				) {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
					) {
						Text(
							text = stringResource(R.string.force_update_label_current_version),
							color = Color(0xFF808080),
							fontSize = 13.sp,
						)
						Text(
							text = BuildConfig.VERSION_NAME,
							color = Color(0xFFB0B0B0),
							fontSize = 13.sp,
							fontWeight = FontWeight.Medium,
						)
					}
					Spacer(modifier = Modifier.height(6.dp))
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
					) {
						Text(
							text = stringResource(R.string.force_update_label_new_version),
							color = Color(0xFF808080),
							fontSize = 13.sp,
						)
						Row(verticalAlignment = Alignment.CenterVertically) {
							Text(
								text = version,
								color = Color(0xFFCC3333),
								fontSize = 13.sp,
								fontWeight = FontWeight.Medium,
							)
							if (isBeta) {
								Spacer(modifier = Modifier.width(6.dp))
								Text(
									text = stringResource(R.string.force_update_beta_badge),
									color = Color.White,
									fontSize = 10.sp,
									fontWeight = FontWeight.Bold,
									modifier = Modifier
										.background(Color(0xFFCC3333), RoundedCornerShape(4.dp))
										.padding(horizontal = 6.dp, vertical = 2.dp),
								)
							}
						}
					}
					Spacer(modifier = Modifier.height(6.dp))
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
					) {
						Text(
							text = stringResource(R.string.force_update_label_released),
							color = Color(0xFF808080),
							fontSize = 13.sp,
						)
						Text(
							text = formatPublishedDate(publishedAt),
							color = Color(0xFFB0B0B0),
							fontSize = 13.sp,
						)
					}
					Spacer(modifier = Modifier.height(6.dp))
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
					) {
						Text(
							text = stringResource(R.string.force_update_label_size),
							color = Color(0xFF808080),
							fontSize = 13.sp,
						)
						Text(
							text = "$sizeMB MB",
							color = Color(0xFFB0B0B0),
							fontSize = 13.sp,
						)
					}
				}

				Spacer(modifier = Modifier.height(24.dp))

				// Progress bar (visible during download)
				if (state == UpdateState.DOWNLOADING) {
					Text(
						text = stringResource(R.string.force_update_downloading),
						color = Color(0xFFB0B0B0),
						fontSize = 14.sp,
					)
					Spacer(modifier = Modifier.height(8.dp))
					LinearProgressIndicator(
						progress = { progress },
						modifier = Modifier
							.fillMaxWidth()
							.height(8.dp),
						color = Color(0xFFCC3333),
						trackColor = Color(0xFF3A1A1A),
					)
					Spacer(modifier = Modifier.height(4.dp))
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
					) {
						Text(
							text = "${(progress * 100).toInt()}%",
							color = Color(0xFF808080),
							fontSize = 12.sp,
						)
						if (totalBytes > 0) {
							Text(
								text = stringResource(
									R.string.force_update_download_progress,
									String.format("%.1f", downloadedBytes / (1024.0 * 1024.0)),
									String.format("%.1f", totalBytes / (1024.0 * 1024.0)),
								),
								color = Color(0xFF808080),
								fontSize = 12.sp,
							)
						}
					}
					Spacer(modifier = Modifier.height(16.dp))
				}

				if (state == UpdateState.VERIFYING) {
					Text(
						text = stringResource(R.string.force_update_verifying),
						color = Color(0xFFB0B0B0),
						fontSize = 14.sp,
					)
					Spacer(modifier = Modifier.height(8.dp))
					LinearProgressIndicator(
						modifier = Modifier
							.fillMaxWidth()
							.height(8.dp),
						color = Color(0xFFCC3333),
						trackColor = Color(0xFF3A1A1A),
					)
					Spacer(modifier = Modifier.height(16.dp))
				}

				if (state == UpdateState.INSTALLING) {
					Text(
						text = stringResource(R.string.force_update_installing),
						color = Color(0xFFCC3333),
						fontSize = 16.sp,
						fontWeight = FontWeight.Medium,
					)
					Spacer(modifier = Modifier.height(16.dp))
				}

				// Update Now button
				var updateButtonFocused by remember { mutableStateOf(false) }
				Button(
					onClick = {
						if (state == UpdateState.READY || state == UpdateState.FAILED) {
							state = UpdateState.DOWNLOADING
							downloadedBytes = 0L
							totalBytes = 0L
							scope.launch(Dispatchers.IO) {
								val success = onDownloadAndInstall { downloaded, total ->
									downloadedBytes = downloaded
									totalBytes = total
									// When download completes (100%), switch to verifying
									if (total > 0 && downloaded >= total && state == UpdateState.DOWNLOADING) {
										state = UpdateState.VERIFYING
									}
								}
								withContext(Dispatchers.Main) {
									state = if (success) UpdateState.INSTALLING else UpdateState.FAILED
								}
							}
						}
					},
					enabled = state == UpdateState.READY || state == UpdateState.FAILED,
					colors = ButtonDefaults.buttonColors(
						containerColor = if (updateButtonFocused) Color(0xFFE04444) else Color(0xFFCC3333),
						contentColor = Color.White,
						disabledContainerColor = Color(0xFF3A2020),
						disabledContentColor = Color(0xFF808080),
					),
					shape = RoundedCornerShape(8.dp),
					modifier = Modifier
						.fillMaxWidth()
						.height(48.dp)
						.then(
							if (updateButtonFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
							else Modifier
						)
						.onFocusChanged { updateButtonFocused = it.isFocused },
				) {
					Text(
						text = when (state) {
							UpdateState.READY -> stringResource(R.string.force_update_button)
							UpdateState.DOWNLOADING -> stringResource(R.string.force_update_downloading)
							UpdateState.VERIFYING -> stringResource(R.string.force_update_verifying)
							UpdateState.INSTALLING -> stringResource(R.string.force_update_installing)
							UpdateState.FAILED -> stringResource(R.string.force_update_download_failed)
						},
						fontSize = 16.sp,
						fontWeight = FontWeight.Medium,
					)
				}

				// Release Notes & Report buttons (only when not downloading)
				if (state == UpdateState.READY || state == UpdateState.FAILED) {
					Spacer(modifier = Modifier.height(8.dp))
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
					) {
						var notesButtonFocused by remember { mutableStateOf(false) }
						TextButton(
							onClick = { showReleaseNotes = true },
							colors = ButtonDefaults.textButtonColors(
								contentColor = if (notesButtonFocused) Color.White else Color(0xFFCC3333),
							),
							shape = RoundedCornerShape(8.dp),
							modifier = Modifier
								.weight(1f)
								.height(40.dp)
								.then(
									if (notesButtonFocused) Modifier.border(2.dp, Color(0xFFCC3333), RoundedCornerShape(8.dp))
									else Modifier
								)
								.onFocusChanged { notesButtonFocused = it.isFocused },
						) {
							Text(
								text = stringResource(R.string.force_update_release_notes),
								fontSize = 14.sp,
							)
						}

						var reportButtonFocused by remember { mutableStateOf(false) }
						TextButton(
							onClick = { showReportIssue = true },
							colors = ButtonDefaults.textButtonColors(
								contentColor = if (reportButtonFocused) Color.White else Color(0xFF808080),
							),
							shape = RoundedCornerShape(8.dp),
							modifier = Modifier
								.weight(1f)
								.height(40.dp)
								.then(
									if (reportButtonFocused) Modifier.border(2.dp, Color(0xFF808080), RoundedCornerShape(8.dp))
									else Modifier
								)
								.onFocusChanged { reportButtonFocused = it.isFocused },
						) {
							Text(
								text = stringResource(R.string.force_update_button_report_issue),
								fontSize = 14.sp,
							)
						}
					}
				}
			}
		}

		// Release Notes overlay
		AnimatedVisibility(
			visible = showReleaseNotes,
			enter = fadeIn(),
			exit = fadeOut(),
		) {
			ReleaseNotesOverlay(
				version = version,
				publishedAt = publishedAt,
				releaseNotes = releaseNotes,
				onDismiss = { showReleaseNotes = false },
			)
		}

		// Report Issue overlay
		AnimatedVisibility(
			visible = showReportIssue,
			enter = fadeIn(),
			exit = fadeOut(),
		) {
			ReportIssueOverlay(
				updateVersion = version,
				issueReporterService = issueReporterService,
				onDismiss = { showReportIssue = false },
			)
		}
	}
}

@Composable
private fun ReleaseNotesOverlay(
	version: String,
	publishedAt: String,
	releaseNotes: String,
	onDismiss: () -> Unit,
) {
	val scrollState = rememberScrollState()
	val scope = rememberCoroutineScope()
	val closeFocusRequester = remember { FocusRequester() }

	// Give Close button focus immediately so user can always press OK to close
	LaunchedEffect(Unit) {
		closeFocusRequester.requestFocus()
	}

	// Full-screen overlay — clickable to block input to the screen behind
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color(0xDD000000))
			.focusable(),
		contentAlignment = Alignment.Center,
	) {
		// Card
		Column(
			modifier = Modifier
				.width(560.dp)
				.height(480.dp)
				.clip(RoundedCornerShape(16.dp))
				.background(Color(0xFF1A1212))
				.border(1.dp, Color(0xFF3A1A1A), RoundedCornerShape(16.dp)),
		) {
			// Header
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.background(Color(0xFF201414))
					.padding(horizontal = 28.dp, vertical = 20.dp),
			) {
				Text(
					text = stringResource(R.string.force_update_release_notes),
					color = Color.White,
					fontSize = 20.sp,
					fontWeight = FontWeight.Bold,
				)
				Spacer(modifier = Modifier.height(4.dp))
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
				) {
					Text(
						text = stringResource(R.string.force_update_version_display, version),
						color = Color(0xFFCC3333),
						fontSize = 14.sp,
						fontWeight = FontWeight.Medium,
					)
					Text(
						text = formatPublishedDate(publishedAt),
						color = Color(0xFF808080),
						fontSize = 13.sp,
					)
				}
			}

			HorizontalDivider(color = Color(0xFF3A1A1A), thickness = 1.dp)

			// Scrollable content (not focusable — scrolled by the Close button's key handler)
			Box(
				modifier = Modifier
					.weight(1f)
					.fillMaxWidth(),
			) {
				Column(
					modifier = Modifier
						.fillMaxSize()
						.verticalScroll(scrollState)
						.padding(28.dp),
				) {
					// Parse and render release notes
					val lines = releaseNotes.lines()
					for (line in lines) {
						when {
							line.startsWith("### ") -> {
								Spacer(modifier = Modifier.height(16.dp))
								Text(
									text = line.removePrefix("### "),
									color = Color.White,
									fontSize = 16.sp,
									fontWeight = FontWeight.SemiBold,
								)
								Spacer(modifier = Modifier.height(8.dp))
							}
							line.startsWith("## ") -> {
								Spacer(modifier = Modifier.height(16.dp))
								Text(
									text = line.removePrefix("## "),
									color = Color.White,
									fontSize = 18.sp,
									fontWeight = FontWeight.Bold,
								)
								Spacer(modifier = Modifier.height(8.dp))
							}
							line.startsWith("# ") -> {
								Spacer(modifier = Modifier.height(16.dp))
								Text(
									text = line.removePrefix("# "),
									color = Color.White,
									fontSize = 20.sp,
									fontWeight = FontWeight.Bold,
								)
								Spacer(modifier = Modifier.height(8.dp))
							}
							line.startsWith("- ") || line.startsWith("* ") -> {
								Row(
									modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
								) {
									Text(
										text = "\u2022  ",
										color = Color(0xFFCC3333),
										fontSize = 15.sp,
									)
									Text(
										text = line.removePrefix("- ").removePrefix("* "),
										color = Color(0xFFCCCCCC),
										fontSize = 15.sp,
										lineHeight = 21.sp,
									)
								}
							}
							line.isBlank() -> {
								Spacer(modifier = Modifier.height(8.dp))
							}
							else -> {
								Text(
									text = line,
									color = Color(0xFFCCCCCC),
									fontSize = 15.sp,
									lineHeight = 21.sp,
									modifier = Modifier.padding(bottom = 4.dp),
								)
							}
						}
					}
					// Bottom padding for scroll
					Spacer(modifier = Modifier.height(8.dp))
				}

				// Scroll fade at bottom
				Box(
					modifier = Modifier
						.align(Alignment.BottomCenter)
						.fillMaxWidth()
						.height(32.dp)
						.background(
							Brush.verticalGradient(
								colors = listOf(Color.Transparent, Color(0xFF1A1212))
							)
						),
				)
			}

			HorizontalDivider(color = Color(0xFF3A1A1A), thickness = 1.dp)

			// Footer with Close button
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.background(Color(0xFF201414))
					.padding(horizontal = 28.dp, vertical = 16.dp),
				contentAlignment = Alignment.CenterEnd,
			) {
				var closeButtonFocused by remember { mutableStateOf(false) }
				Button(
					onClick = onDismiss,
					colors = ButtonDefaults.buttonColors(
						containerColor = if (closeButtonFocused) Color(0xFFE04444) else Color(0xFFCC3333),
						contentColor = Color.White,
					),
					shape = RoundedCornerShape(8.dp),
					modifier = Modifier
						.width(120.dp)
						.height(40.dp)
						.focusRequester(closeFocusRequester)
						.onPreviewKeyEvent { event ->
							if (event.type == KeyEventType.KeyDown) {
								when (event.key) {
									Key.DirectionDown -> {
										scope.launch { scrollState.animateScrollBy(200f) }
										true
									}
									Key.DirectionUp -> {
										scope.launch { scrollState.animateScrollBy(-200f) }
										true
									}
									else -> false
								}
							} else false
						}
						.then(
							if (closeButtonFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
							else Modifier
						)
						.onFocusChanged { closeButtonFocused = it.isFocused },
				) {
					Text(
						text = stringResource(R.string.force_update_button_close),
						fontSize = 14.sp,
						fontWeight = FontWeight.Medium,
					)
				}
			}
		}
	}
}

private enum class ReportState {
	IDLE,
	SUBMITTING,
	SUCCESS,
	ERROR,
}

@Composable
private fun ReportIssueOverlay(
	updateVersion: String,
	issueReporterService: IssueReporterService,
	onDismiss: () -> Unit,
) {
	val reportContext = LocalContext.current
	val categories = IssueReporterService.IssueCategory.entries
	var selectedCategory by remember { mutableIntStateOf(0) }
	var description by remember { mutableStateOf("") }
	var reportState by remember { mutableStateOf(ReportState.IDLE) }
	var issueNumber by remember { mutableIntStateOf(0) }
	var errorMessage by remember { mutableStateOf("") }
	var duplicateNumber by remember { mutableIntStateOf(0) }
	var tokenExpired by remember { mutableStateOf(false) }
	val scope = rememberCoroutineScope()
	val scrollState = rememberScrollState()

	// Check token status on first load
	LaunchedEffect(Unit) {
		val status = issueReporterService.checkTokenStatus()
		tokenExpired = status == IssueReporterService.TokenStatus.EXPIRED_OR_INVALID
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color(0xDD000000)),
		contentAlignment = Alignment.Center,
	) {
		Column(
			modifier = Modifier
				.width(560.dp)
				.height(520.dp)
				.clip(RoundedCornerShape(16.dp))
				.background(Color(0xFF1A1212))
				.border(1.dp, Color(0xFF3A1A1A), RoundedCornerShape(16.dp)),
		) {
			// Header
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.background(Color(0xFF201414))
					.padding(horizontal = 28.dp, vertical = 20.dp),
			) {
				Text(
					text = stringResource(R.string.report_issue_title),
					color = Color.White,
					fontSize = 20.sp,
					fontWeight = FontWeight.Bold,
				)
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = stringResource(R.string.report_issue_subtitle),
					color = Color(0xFF808080),
					fontSize = 13.sp,
				)
			}

			HorizontalDivider(color = Color(0xFF3A1A1A), thickness = 1.dp)

			// Content
			Column(
				modifier = Modifier
					.weight(1f)
					.fillMaxWidth()
					.verticalScroll(scrollState)
					.padding(28.dp),
			) {
				if (reportState == ReportState.SUCCESS) {
					// Success message
					Column(
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.Center,
						modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
					) {
						Text(
							text = stringResource(R.string.report_issue_submitted_title),
							color = Color.White,
							fontSize = 20.sp,
							fontWeight = FontWeight.Bold,
						)
						Spacer(modifier = Modifier.height(12.dp))
						Text(
							text = stringResource(R.string.report_issue_submitted_message, issueNumber),
							color = Color(0xFFB0B0B0),
							fontSize = 15.sp,
						)
						Spacer(modifier = Modifier.height(8.dp))
						Text(
							text = stringResource(R.string.report_issue_thanks),
							color = Color(0xFFCC3333),
							fontSize = 14.sp,
						)
					}
				} else {
					// Category selection
					Text(
						text = stringResource(R.string.report_issue_category_label),
						color = Color.White,
						fontSize = 16.sp,
						fontWeight = FontWeight.Medium,
					)
					Spacer(modifier = Modifier.height(12.dp))

					categories.forEachIndexed { index, category ->
						var rowFocused by remember { mutableStateOf(false) }
						Row(
							verticalAlignment = Alignment.CenterVertically,
							modifier = Modifier
								.fillMaxWidth()
								.then(
									if (rowFocused) Modifier
										.background(Color(0xFF2A1616), RoundedCornerShape(8.dp))
										.border(1.dp, Color(0xFF3A1A1A), RoundedCornerShape(8.dp))
									else Modifier
								)
								.padding(vertical = 2.dp, horizontal = 4.dp)
								.onFocusChanged { rowFocused = it.hasFocus },
						) {
							RadioButton(
								selected = selectedCategory == index,
								onClick = { selectedCategory = index },
								colors = RadioButtonDefaults.colors(
									selectedColor = Color(0xFFCC3333),
									unselectedColor = Color(0xFF606060),
								),
							)
							Text(
								text = category.label,
								color = if (selectedCategory == index) Color.White else Color(0xFFB0B0B0),
								fontSize = 14.sp,
								modifier = Modifier.padding(start = 4.dp),
							)
						}
					}

					Spacer(modifier = Modifier.height(20.dp))

					// Description field
					Text(
						text = stringResource(R.string.report_issue_details_label),
						color = Color.White,
						fontSize = 16.sp,
						fontWeight = FontWeight.Medium,
					)
					Spacer(modifier = Modifier.height(8.dp))

					OutlinedTextField(
						value = description,
						onValueChange = { if (it.length <= 200) description = it },
						placeholder = {
							Text(
								text = stringResource(R.string.report_issue_details_hint),
								color = Color(0xFF606060),
							)
						},
						colors = OutlinedTextFieldDefaults.colors(
							focusedTextColor = Color.White,
							unfocusedTextColor = Color(0xFFCCCCCC),
							cursorColor = Color(0xFFCC3333),
							focusedBorderColor = Color(0xFFCC3333),
							unfocusedBorderColor = Color(0xFF3A1A1A),
							focusedContainerColor = Color(0xFF140E0E),
							unfocusedContainerColor = Color(0xFF140E0E),
						),
						shape = RoundedCornerShape(8.dp),
						modifier = Modifier
							.fillMaxWidth()
							.height(100.dp),
						maxLines = 3,
					)
					Spacer(modifier = Modifier.height(4.dp))
					Text(
						text = "${description.length}/200",
						color = Color(0xFF606060),
						fontSize = 11.sp,
						modifier = Modifier.align(Alignment.End),
					)

					if (reportState == ReportState.ERROR) {
						Spacer(modifier = Modifier.height(8.dp))
						Text(
							text = errorMessage.ifBlank { stringResource(R.string.report_issue_error_default) },
							color = Color(0xFFCC3333),
							fontSize = 13.sp,
						)
					}

					if (duplicateNumber > 0 && reportState != ReportState.SUCCESS) {
						Spacer(modifier = Modifier.height(8.dp))
						Text(
							text = stringResource(R.string.report_issue_duplicate_warning, duplicateNumber),
							color = Color(0xFFE0A030),
							fontSize = 13.sp,
						)
					}

					// Auto-collected info note
					Spacer(modifier = Modifier.height(12.dp))
					Text(
						text = stringResource(R.string.report_issue_device_info_note),
						color = Color(0xFF505050),
						fontSize = 11.sp,
					)

					if (tokenExpired) {
						Spacer(modifier = Modifier.height(8.dp))
						Text(
							text = stringResource(R.string.report_issue_token_expired),
							color = Color(0xFFCC3333),
							fontSize = 12.sp,
						)
					}
				}
			}

			HorizontalDivider(color = Color(0xFF3A1A1A), thickness = 1.dp)

			// Footer
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.background(Color(0xFF201414))
					.padding(horizontal = 28.dp, vertical = 16.dp),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically,
			) {
				if (reportState == ReportState.SUCCESS) {
					var doneButtonFocused by remember { mutableStateOf(false) }
					Button(
						onClick = onDismiss,
						colors = ButtonDefaults.buttonColors(
							containerColor = if (doneButtonFocused) Color(0xFFE04444) else Color(0xFFCC3333),
							contentColor = Color.White,
						),
						shape = RoundedCornerShape(8.dp),
						modifier = Modifier
							.width(120.dp)
							.height(40.dp)
							.then(
								if (doneButtonFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
								else Modifier
							)
							.onFocusChanged { doneButtonFocused = it.isFocused },
					) {
						Text(stringResource(R.string.force_update_button_close), fontSize = 14.sp, fontWeight = FontWeight.Medium)
					}
				} else {
					var cancelButtonFocused by remember { mutableStateOf(false) }
					TextButton(
						onClick = onDismiss,
						enabled = reportState != ReportState.SUBMITTING,
						colors = ButtonDefaults.textButtonColors(
							contentColor = if (cancelButtonFocused) Color.White else Color(0xFF808080),
						),
						shape = RoundedCornerShape(8.dp),
						modifier = Modifier
							.height(40.dp)
							.then(
								if (cancelButtonFocused) Modifier.border(1.dp, Color(0xFF808080), RoundedCornerShape(8.dp))
								else Modifier
							)
							.onFocusChanged { cancelButtonFocused = it.isFocused },
					) {
						Text(stringResource(R.string.report_issue_button_cancel), fontSize = 14.sp)
					}

					Spacer(modifier = Modifier.width(12.dp))

					var submitButtonFocused by remember { mutableStateOf(false) }
					Button(
						onClick = {
							reportState = ReportState.SUBMITTING
							errorMessage = ""
							scope.launch(Dispatchers.IO) {
								// Check rate limit first
								val cooldown = issueReporterService.getCooldownRemaining()
								if (cooldown > 0) {
									withContext(Dispatchers.Main) {
										errorMessage = reportContext.getString(R.string.report_issue_cooldown, cooldown / 60, cooldown % 60)
										reportState = ReportState.ERROR
									}
									return@launch
								}

								// Check for duplicates (only first time, skip if user already saw warning)
								if (duplicateNumber == 0) {
									val existing = issueReporterService.findDuplicate(categories[selectedCategory])
									if (existing != null) {
										withContext(Dispatchers.Main) {
											duplicateNumber = existing
											reportState = ReportState.IDLE
										}
										return@launch
									}
								}

								val result = issueReporterService.submitIssue(
									category = categories[selectedCategory],
									description = description,
									updateVersion = updateVersion,
								)
								withContext(Dispatchers.Main) {
									result.fold(
										onSuccess = { number ->
											issueNumber = number
											reportState = ReportState.SUCCESS
										},
										onFailure = {
											Timber.e(it, "Failed to submit issue")
											errorMessage = it.message ?: "Failed to submit report."
											reportState = ReportState.ERROR
										}
									)
								}
							}
						},
						enabled = reportState != ReportState.SUBMITTING && !tokenExpired,
						colors = ButtonDefaults.buttonColors(
							containerColor = if (submitButtonFocused) Color(0xFFE04444) else Color(0xFFCC3333),
							contentColor = Color.White,
							disabledContainerColor = Color(0xFF3A2020),
							disabledContentColor = Color(0xFF808080),
						),
						shape = RoundedCornerShape(8.dp),
						modifier = Modifier
							.width(140.dp)
							.height(40.dp)
							.then(
								if (submitButtonFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
								else Modifier
							)
							.onFocusChanged { submitButtonFocused = it.isFocused },
					) {
						Text(
							text = if (reportState == ReportState.SUBMITTING) stringResource(R.string.report_issue_button_submitting) else stringResource(R.string.report_issue_button_submit),
							fontSize = 14.sp,
							fontWeight = FontWeight.Medium,
						)
					}
				}
			}
		}
	}
}
