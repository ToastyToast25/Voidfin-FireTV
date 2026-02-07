package org.jellyfin.androidtv.ui.settings.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.UpdateCheckerService
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.preference.category.showDonateDialog
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

@Composable
fun SettingsMainScreen() {
	val router = LocalRouter.current
	val context = LocalContext.current
	val updateChecker by inject<UpdateCheckerService>(UpdateCheckerService::class.java)
	val userPreferences = koinInject<UserPreferences>()

	var showUpdateOverlay by remember { mutableStateOf(false) }
	var pendingUpdateInfo by remember { mutableStateOf<UpdateCheckerService.UpdateInfo?>(null) }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.app_name).uppercase()) },
				headingContent = { Text(stringResource(R.string.settings)) },
				captionContent = { Text(stringResource(R.string.settings_description)) },
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_users), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_login)) },
				onClick = { router.push(Routes.AUTHENTICATION) },
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_adjust), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_customization)) },
				onClick = { router.push(Routes.CUSTOMIZATION) }
			)
		}

		item {
			ListButton(
				leadingContent = {
					Icon(
						painterResource(R.drawable.ic_jellyseerr_jellyfish),
						contentDescription = null,
						modifier = Modifier.size(24.dp)
					)
				},
				headingContent = { Text(stringResource(R.string.jellyseerr)) },
				captionContent = { Text(stringResource(R.string.jellyseerr_settings_description)) },
				onClick = { router.push(Routes.JELLYSEERR) }
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_photos), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_screensaver)) },
				onClick = { router.push(Routes.CUSTOMIZATION_SCREENSAVER) }
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_syncplay), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.syncplay)) },
				captionContent = { Text(stringResource(R.string.syncplay_description)) },
				onClick = { router.push(Routes.SYNCPLAY) }
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_next), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_playback)) },
				onClick = { router.push(Routes.PLAYBACK) }
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_error), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_telemetry_category)) },
				onClick = { router.push(Routes.TELEMETRY) }
			)

		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_flask), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_developer_link)) },
				onClick = { router.push(Routes.DEVELOPER) }
			)
		}

		item {
			ListSection(
				headingContent = { Text(stringResource(R.string.settings_support_updates_section)) },
			)
		}

		item {
			ListButton(
				leadingContent = {
					Icon(
						painterResource(R.drawable.ic_get_app),
						contentDescription = null
					)
				},
				headingContent = { Text(stringResource(R.string.pref_check_for_updates)) },
				captionContent = { Text(stringResource(R.string.settings_check_updates_caption)) },
				onClick = {
					CoroutineScope(Dispatchers.Main).launch {
						Toast.makeText(context, context.getString(R.string.msg_checking_for_updates), Toast.LENGTH_SHORT).show()
						try {
							val result = updateChecker.checkForUpdate()
							result.fold(
								onSuccess = { info ->
									if (info == null) {
										Toast.makeText(context, context.getString(R.string.msg_update_check_failed), Toast.LENGTH_LONG).show()
									} else if (!info.isNewer) {
										Toast.makeText(context, context.getString(R.string.msg_no_updates_available), Toast.LENGTH_LONG).show()
									} else {
										pendingUpdateInfo = info
										showUpdateOverlay = true
									}
								},
								onFailure = { err ->
									Timber.e(err, "Failed to check for updates")
									Toast.makeText(context, context.getString(R.string.msg_update_check_failed), Toast.LENGTH_LONG).show()
								}
							)
						} catch (e: Exception) {
							Timber.e(e, "Error checking for updates")
							Toast.makeText(context, context.getString(R.string.msg_update_check_failed), Toast.LENGTH_LONG).show()
						}
					}
				}
			)
		}

		item {
			var updateNotificationsEnabled by rememberPreference(userPreferences, UserPreferences.updateNotificationsEnabled)
			ListButton(
				headingContent = { Text(stringResource(R.string.settings_update_notifications)) },
				captionContent = { Text(stringResource(R.string.settings_update_notifications_description)) },
				trailingContent = { Checkbox(checked = updateNotificationsEnabled) },
				onClick = { updateNotificationsEnabled = !updateNotificationsEnabled }
			)
		}

		item {
			var betaUpdatesEnabled by rememberPreference(userPreferences, UserPreferences.betaUpdatesEnabled)
			ListButton(
				headingContent = { Text(stringResource(R.string.pref_beta_channel)) },
				captionContent = { Text(stringResource(R.string.pref_beta_channel_description)) },
				trailingContent = { Checkbox(checked = betaUpdatesEnabled) },
				onClick = { betaUpdatesEnabled = !betaUpdatesEnabled }
			)
		}

		item {
			ListButton(
				leadingContent = {
					Icon(
						painterResource(R.drawable.ic_heart),
						contentDescription = null,
						tint = Color.Red
					)
				},
				headingContent = { Text(stringResource(R.string.settings_support_voidstream)) },
				captionContent = { Text(stringResource(R.string.settings_support_voidstream_description)) },
				onClick = {
					showDonateDialog(context)
				}
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_error), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.report_issue_title)) },
				captionContent = { Text(stringResource(R.string.settings_report_issue_description)) },
				onClick = { router.push(Routes.REPORT_ISSUE) }
			)
		}

		item {
			ListButton(
				leadingContent = { Icon(painterResource(R.drawable.ic_jellyfin), contentDescription = null) },
				headingContent = { Text(stringResource(R.string.pref_about_title)) },
				onClick = { router.push(Routes.ABOUT) }
			)
		}
	}

	if (showUpdateOverlay && pendingUpdateInfo != null) {
		SettingsUpdateOverlay(
			info = pendingUpdateInfo!!,
			updateChecker = updateChecker,
			onDismiss = { showUpdateOverlay = false },
		)
	}
}

private enum class DlState { IDLE, DOWNLOADING, VERIFYING, INSTALLING, DONE, FAILED }

private fun formatDate(isoDate: String): String {
	return try {
		val instant = java.time.Instant.parse(isoDate)
		val zoned = instant.atZone(java.time.ZoneId.systemDefault())
		java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy").format(zoned)
	} catch (_: Exception) { isoDate }
}

@Composable
private fun SettingsUpdateOverlay(
	info: UpdateCheckerService.UpdateInfo,
	updateChecker: UpdateCheckerService,
	onDismiss: () -> Unit,
) {
	val ctx = LocalContext.current
	val scope = rememberCoroutineScope()
	val scrollState = rememberScrollState()
	val dlBtnFocus = remember { FocusRequester() }

	var dlState by remember { mutableStateOf(DlState.IDLE) }
	var dlBytes by remember { mutableLongStateOf(0L) }
	var totalDlBytes by remember { mutableLongStateOf(0L) }

	val canDismiss = dlState == DlState.IDLE || dlState == DlState.FAILED || dlState == DlState.DONE

	// Lifecycle recovery: if the app resumes while in INSTALLING state, the install
	// didn't complete (user cancelled, installer failed, etc.) — reset to allow retry
	val lifecycleOwner = LocalLifecycleOwner.current
	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME && dlState == DlState.INSTALLING) {
				dlState = DlState.FAILED
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}

	// Timeout: if stuck in INSTALLING for 30s (system dialog didn't appear), reset
	LaunchedEffect(dlState) {
		if (dlState == DlState.INSTALLING) {
			delay(30_000L)
			if (dlState == DlState.INSTALLING) {
				Timber.w("Install timeout — stuck in INSTALLING for 30s, resetting to FAILED")
				dlState = DlState.FAILED
			}
		}
	}

	LaunchedEffect(Unit) { dlBtnFocus.requestFocus() }

	Dialog(
		onDismissRequest = { if (canDismiss) onDismiss() },
		properties = DialogProperties(
			dismissOnBackPress = canDismiss,
			dismissOnClickOutside = false,
			usePlatformDefaultWidth = false,
		),
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color(0xDD000000))
				.onKeyEvent { event ->
					// Consume all directional key events in the bubble phase
					// to prevent them from leaking to the settings list behind the dialog
					if (event.type == KeyEventType.KeyDown) {
						when (event.key) {
							Key.DirectionDown, Key.DirectionUp,
							Key.DirectionLeft, Key.DirectionRight -> true
							else -> false
						}
					} else false
				}
				.focusable(),
			contentAlignment = Alignment.Center,
		) {
			UpdateCard(
				info = info,
				updateChecker = updateChecker,
				dlState = dlState,
				dlBytes = dlBytes,
				totalDlBytes = totalDlBytes,
				scrollState = scrollState,
				scope = scope,
				dlBtnFocus = dlBtnFocus,
				onDownload = {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
						!ctx.packageManager.canRequestPackageInstalls()) {
						Toast.makeText(ctx, ctx.getString(R.string.force_update_install_permission), Toast.LENGTH_LONG).show()
						openInstallPermissionSettings(ctx)
						return@UpdateCard
					}
					dlState = DlState.DOWNLOADING
					dlBytes = 0L
					totalDlBytes = 0L
					scope.launch {
						try {
							val result = withContext(Dispatchers.IO) {
								updateChecker.downloadUpdate(
									downloadUrl = info.downloadUrl,
									expectedSha256 = info.expectedSha256,
								) { downloaded, total ->
									dlBytes = downloaded
									totalDlBytes = total
								}
							}
							result.fold(
								onSuccess = { apkUri ->
									Timber.d("Download complete, APK URI: $apkUri")
									updateChecker.savePendingWhatsNew(info.version, info.releaseNotes)
									val installed = updateChecker.installUpdate(apkUri)
									Timber.d("Install result: $installed")
									dlState = if (installed) DlState.INSTALLING else DlState.FAILED
								},
								onFailure = { err ->
									Timber.e(err, "Download failed: ${err.message}")
									dlState = DlState.FAILED
								}
							)
						} catch (e: Exception) {
							Timber.e(e, "Download error")
							dlState = DlState.FAILED
						}
					}
				},
				onDismiss = onDismiss,
			)
		}
	}
}

@Composable
private fun UpdateCard(
	info: UpdateCheckerService.UpdateInfo,
	updateChecker: UpdateCheckerService,
	dlState: DlState,
	dlBytes: Long,
	totalDlBytes: Long,
	scrollState: androidx.compose.foundation.ScrollState,
	scope: kotlinx.coroutines.CoroutineScope,
	dlBtnFocus: FocusRequester,
	onDownload: () -> Unit,
	onDismiss: () -> Unit,
) {
	val cardShape = RoundedCornerShape(16.dp)
	val borderClr = Color(0xFF3A1A1A)
	val cardBg = Color(0xFF1A1212)
	val hdrBg = Color(0xFF201414)
	val accent = Color(0xFFCC3333)
	val infoBg = Color(0xFF140E0E)

	val sizeMB = String.format("%.1f", info.apkSize / (1024.0 * 1024.0))
	val progress = if (totalDlBytes > 0) dlBytes.toFloat() / totalDlBytes.toFloat() else 0f

	Column(
		modifier = Modifier
			.width(520.dp)
			.clip(cardShape)
			.background(cardBg)
			.border(1.dp, borderClr, cardShape),
	) {
		// Header
		UpdateCardHeader(hdrBg, accent, info.version)

		HorizontalDivider(color = borderClr, thickness = 1.dp)

		// Version info panel
		UpdateCardInfoPanel(infoBg, info, sizeMB)

		// Progress section
		if (dlState == DlState.DOWNLOADING || dlState == DlState.VERIFYING) {
			UpdateCardProgress(accent, dlState, dlBytes, totalDlBytes, progress)
		}

		HorizontalDivider(color = borderClr, thickness = 1.dp)

		// Scrollable release notes
		Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(scrollState)
					.padding(24.dp),
			) {
				UpdateCardMarkdown(info.releaseNotes, accent)
				Spacer(modifier = Modifier.height(8.dp))
			}
			Box(
				modifier = Modifier
					.align(Alignment.BottomCenter)
					.fillMaxWidth()
					.height(32.dp)
					.background(Brush.verticalGradient(listOf(Color.Transparent, cardBg))),
			)
		}

		HorizontalDivider(color = borderClr, thickness = 1.dp)

		// Footer with buttons
		UpdateCardButtons(
			accent = accent,
			hdrBg = hdrBg,
			dlState = dlState,
			dlBtnFocus = dlBtnFocus,
			scrollState = scrollState,
			scope = scope,
			onDownload = onDownload,
			onDismiss = onDismiss,
		)
	}
}

@Composable
private fun UpdateCardHeader(hdrBg: Color, accent: Color, version: String) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.background(hdrBg)
			.padding(horizontal = 28.dp, vertical = 20.dp),
	) {
		androidx.compose.material3.Text(
			text = stringResource(R.string.settings_update_title),
			color = Color.White,
			fontSize = 22.sp,
			fontWeight = FontWeight.Bold,
		)
		Spacer(modifier = Modifier.height(4.dp))
		androidx.compose.material3.Text(
			text = stringResource(R.string.settings_update_message),
			color = Color(0xFFB0B0B0),
			fontSize = 14.sp,
		)
	}
}

@Composable
private fun UpdateCardInfoPanel(infoBg: Color, info: UpdateCheckerService.UpdateInfo, sizeMB: String) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 28.dp, vertical = 12.dp)
			.background(infoBg, RoundedCornerShape(8.dp))
			.padding(horizontal = 16.dp, vertical = 12.dp),
	) {
		InfoRow(stringResource(R.string.force_update_label_current_version), BuildConfig.VERSION_NAME)
		Spacer(modifier = Modifier.height(6.dp))
		InfoRow(stringResource(R.string.force_update_label_new_version), info.version)
		Spacer(modifier = Modifier.height(6.dp))
		InfoRow(stringResource(R.string.force_update_label_released), formatDate(info.publishedAt))
		Spacer(modifier = Modifier.height(6.dp))
		InfoRow(stringResource(R.string.force_update_label_size), "$sizeMB MB")
	}
}

@Composable
private fun InfoRow(label: String, value: String) {
	Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
		androidx.compose.material3.Text(label, color = Color(0xFF808080), fontSize = 13.sp)
		androidx.compose.material3.Text(value, color = Color(0xFFB0B0B0), fontSize = 13.sp, fontWeight = FontWeight.Medium)
	}
}

@Composable
private fun UpdateCardProgress(accent: Color, dlState: DlState, dlBytes: Long, totalDlBytes: Long, progress: Float) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 28.dp, vertical = 12.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
	) {
		LinearProgressIndicator(
			progress = { progress },
			modifier = Modifier
				.fillMaxWidth()
				.height(6.dp)
				.clip(RoundedCornerShape(3.dp)),
			color = accent,
			trackColor = Color(0xFF2A1515),
		)
		Spacer(modifier = Modifier.height(8.dp))
		val statusText = when (dlState) {
			DlState.VERIFYING -> stringResource(R.string.force_update_verifying)
			else -> {
				val dlMB = String.format("%.1f", dlBytes / (1024.0 * 1024.0))
				val totMB = String.format("%.1f", totalDlBytes / (1024.0 * 1024.0))
				stringResource(R.string.force_update_download_progress, dlMB, totMB)
			}
		}
		androidx.compose.material3.Text(statusText, color = Color(0xFF808080), fontSize = 12.sp)
	}
}

@Composable
private fun UpdateCardMarkdown(rawText: String, bulletClr: Color) {
	val bodyClr = Color(0xFFCCCCCC)
	for (line in rawText.lines()) {
		when {
			line.startsWith("### ") -> {
				Spacer(modifier = Modifier.height(14.dp))
				androidx.compose.material3.Text(line.removePrefix("### "), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
				Spacer(modifier = Modifier.height(6.dp))
			}
			line.startsWith("## ") -> {
				Spacer(modifier = Modifier.height(14.dp))
				androidx.compose.material3.Text(line.removePrefix("## "), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
				Spacer(modifier = Modifier.height(6.dp))
			}
			line.startsWith("# ") -> {
				Spacer(modifier = Modifier.height(14.dp))
				androidx.compose.material3.Text(line.removePrefix("# "), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
				Spacer(modifier = Modifier.height(6.dp))
			}
			line.startsWith("- ") || line.startsWith("* ") -> {
				Row(modifier = Modifier.padding(start = 8.dp, bottom = 5.dp)) {
					androidx.compose.material3.Text("\u2022  ", color = bulletClr, fontSize = 14.sp)
					androidx.compose.material3.Text(
						line.removePrefix("- ").removePrefix("* "),
						color = bodyClr, fontSize = 14.sp, lineHeight = 20.sp,
					)
				}
			}
			line.isBlank() -> Spacer(modifier = Modifier.height(6.dp))
			else -> androidx.compose.material3.Text(line, color = bodyClr, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(bottom = 3.dp))
		}
	}
}

@Composable
private fun UpdateCardButtons(
	accent: Color,
	hdrBg: Color,
	dlState: DlState,
	dlBtnFocus: FocusRequester,
	scrollState: androidx.compose.foundation.ScrollState,
	scope: kotlinx.coroutines.CoroutineScope,
	onDownload: () -> Unit,
	onDismiss: () -> Unit,
) {
	val focusedAccent = Color(0xFFE04444)
	var dlFocused by remember { mutableStateOf(false) }
	var laterFocused by remember { mutableStateOf(false) }
	val canDismiss = dlState == DlState.IDLE || dlState == DlState.FAILED || dlState == DlState.DONE

	val dlLabel = when (dlState) {
		DlState.IDLE -> stringResource(R.string.btn_download)
		DlState.DOWNLOADING -> stringResource(R.string.force_update_downloading)
		DlState.VERIFYING -> stringResource(R.string.force_update_verifying)
		DlState.INSTALLING -> stringResource(R.string.settings_update_installing)
		DlState.DONE -> stringResource(R.string.settings_update_installed)
		DlState.FAILED -> stringResource(R.string.settings_update_retry)
	}

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.background(hdrBg)
			.padding(horizontal = 28.dp, vertical = 16.dp),
		horizontalArrangement = Arrangement.SpaceBetween,
	) {
		// Later button
		Button(
			onClick = { if (canDismiss) onDismiss() },
			enabled = canDismiss,
			colors = ButtonDefaults.buttonColors(
				containerColor = if (laterFocused) Color(0xFF3A2020) else Color(0xFF2A1515),
				contentColor = Color.White,
				disabledContainerColor = Color(0xFF1A1010),
				disabledContentColor = Color(0xFF555555),
			),
			shape = RoundedCornerShape(8.dp),
			modifier = Modifier
				.width(120.dp)
				.height(40.dp)
				.onPreviewKeyEvent { ev ->
					if (ev.type == KeyEventType.KeyDown) {
						when (ev.key) {
							Key.DirectionDown -> { scope.launch { scrollState.animateScrollBy(200f) }; true }
							Key.DirectionUp -> { scope.launch { scrollState.animateScrollBy(-200f) }; true }
							else -> false
						}
					} else false
				}
				.then(if (laterFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier)
				.onFocusChanged { laterFocused = it.isFocused },
		) {
			androidx.compose.material3.Text(stringResource(R.string.btn_later), fontSize = 14.sp, fontWeight = FontWeight.Medium)
		}

		// Download button
		Button(
			onClick = onDownload,
			enabled = dlState == DlState.IDLE || dlState == DlState.FAILED,
			colors = ButtonDefaults.buttonColors(
				containerColor = if (dlFocused) focusedAccent else accent,
				contentColor = Color.White,
				disabledContainerColor = Color(0xFF8B2222),
				disabledContentColor = Color(0xFFCCCCCC),
			),
			shape = RoundedCornerShape(8.dp),
			modifier = Modifier
				.width(160.dp)
				.height(40.dp)
				.focusRequester(dlBtnFocus)
				.onPreviewKeyEvent { ev ->
					if (ev.type == KeyEventType.KeyDown) {
						when (ev.key) {
							Key.DirectionDown -> { scope.launch { scrollState.animateScrollBy(200f) }; true }
							Key.DirectionUp -> { scope.launch { scrollState.animateScrollBy(-200f) }; true }
							else -> false
						}
					} else false
				}
				.then(if (dlFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier)
				.onFocusChanged { dlFocused = it.isFocused },
		) {
			androidx.compose.material3.Text(dlLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium)
		}
	}
}

private fun openInstallPermissionSettings(context: Context) {
	try {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
				data = Uri.parse("package:${context.packageName}")
			}
			context.startActivity(intent)
		}
	} catch (e: Exception) {
		Timber.e(e, "Failed to open install permission settings")
		Toast.makeText(context, context.getString(R.string.msg_failed_to_open_settings), Toast.LENGTH_LONG).show()
	}
}
