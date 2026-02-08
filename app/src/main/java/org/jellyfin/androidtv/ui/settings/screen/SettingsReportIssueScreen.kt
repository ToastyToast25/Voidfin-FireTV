package org.jellyfin.androidtv.ui.settings.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.IssueReporterService
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject
import timber.log.Timber

private enum class ReportState {
	IDLE,
	SUBMITTING,
	SUCCESS,
	ERROR,
}

@Composable
fun SettingsReportIssueScreen() {
	val issueReporterService = koinInject<IssueReporterService>()
	val categories = IssueReporterService.IssueCategory.entries
	var selectedCategory by remember { mutableIntStateOf(0) }
	var description by remember { mutableStateOf("") }
	var reportState by remember { mutableStateOf(ReportState.IDLE) }
	var issueNumber by remember { mutableIntStateOf(0) }
	var errorMessage by remember { mutableStateOf("") }
	var duplicateNumber by remember { mutableIntStateOf(0) }
	var tokenExpired by remember { mutableStateOf(false) }
	var cooldownSeconds by remember { mutableIntStateOf(0) }
	val scope = rememberCoroutineScope()

	// Check token status on first load
	LaunchedEffect(Unit) {
		val status = issueReporterService.checkTokenStatus()
		tokenExpired = status == IssueReporterService.TokenStatus.EXPIRED_OR_INVALID
	}

	// Countdown timer when in SUCCESS state
	LaunchedEffect(reportState) {
		if (reportState == ReportState.SUCCESS) {
			// Get the actual progressive cooldown duration
			cooldownSeconds = issueReporterService.getProgressiveCooldownDuration().toInt()
			while (cooldownSeconds > 0) {
				kotlinx.coroutines.delay(1000)
				cooldownSeconds--
			}
		}
	}

	SettingsColumn {
		item {
			ListSection(
				headingContent = { Text(stringResource(R.string.report_issue_title)) },
				captionContent = { Text(stringResource(R.string.report_issue_subtitle)) },
			)
		}

		if (reportState == ReportState.SUCCESS) {
			item {
				Column(
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center,
					modifier = Modifier
						.fillMaxWidth()
						.padding(vertical = 40.dp),
				) {
					androidx.compose.material3.Text(
						text = stringResource(R.string.report_issue_submitted_title),
						color = Color.White,
						fontSize = 20.sp,
						fontWeight = FontWeight.Bold,
					)
					Spacer(modifier = Modifier.height(12.dp))
					androidx.compose.material3.Text(
						text = stringResource(R.string.report_issue_submitted_message, issueNumber),
						color = Color(0xFFB0B0B0),
						fontSize = 15.sp,
					)
					Spacer(modifier = Modifier.height(8.dp))
					androidx.compose.material3.Text(
						text = stringResource(R.string.report_issue_thanks),
						color = Color(0xFFCC3333),
						fontSize = 14.sp,
					)

					if (cooldownSeconds > 0) {
						Spacer(modifier = Modifier.height(16.dp))
						val timeText = when {
							cooldownSeconds >= 60 -> {
								val minutes = cooldownSeconds / 60
								val seconds = cooldownSeconds % 60
								if (seconds > 0) "${minutes}m ${seconds}s" else "${minutes}m"
							}
							else -> "${cooldownSeconds}s"
						}
						androidx.compose.material3.Text(
							text = "You can submit another issue in $timeText",
							color = Color(0xFF808080),
							fontSize = 13.sp,
						)
					}

					Spacer(modifier = Modifier.height(24.dp))
					var resetFocused by remember { mutableStateOf(false) }
					Button(
						onClick = {
							reportState = ReportState.IDLE
							selectedCategory = 0
							description = ""
							issueNumber = 0
							duplicateNumber = 0
							errorMessage = ""
							cooldownSeconds = 0
						},
						enabled = cooldownSeconds == 0,
						colors = ButtonDefaults.buttonColors(
							containerColor = if (resetFocused) Color(0xFFE04444) else Color(0xFFCC3333),
							contentColor = Color.White,
							disabledContainerColor = Color(0xFF3A2020),
							disabledContentColor = Color(0xFF808080),
						),
						shape = RoundedCornerShape(8.dp),
						modifier = Modifier
							.width(200.dp)
							.height(44.dp)
							.then(
								if (resetFocused && cooldownSeconds == 0) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
								else Modifier
							)
							.onFocusChanged { resetFocused = it.isFocused },
					) {
						val buttonText = if (cooldownSeconds > 0) {
							when {
								cooldownSeconds >= 60 -> {
									val minutes = cooldownSeconds / 60
									val seconds = cooldownSeconds % 60
									if (seconds > 0) "Wait ${minutes}m ${seconds}s" else "Wait ${minutes}m"
								}
								else -> "Wait ${cooldownSeconds}s"
							}
						} else {
							stringResource(R.string.report_issue_button_submit_another)
						}

						androidx.compose.material3.Text(
							text = buttonText,
							fontSize = 14.sp
						)
					}
				}
			}
		} else {
			item {
				Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
					// Category selection
					androidx.compose.material3.Text(
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
							androidx.compose.material3.Text(
								text = category.label,
								color = if (selectedCategory == index) Color.White else Color(0xFFB0B0B0),
								fontSize = 14.sp,
								modifier = Modifier.padding(start = 4.dp),
							)
						}
					}

					Spacer(modifier = Modifier.height(20.dp))

					// Description field
					androidx.compose.material3.Text(
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
							androidx.compose.material3.Text(
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
					androidx.compose.material3.Text(
						text = "${description.length}/200",
						color = Color(0xFF606060),
						fontSize = 11.sp,
						modifier = Modifier.align(Alignment.End),
					)

					if (reportState == ReportState.ERROR) {
						Spacer(modifier = Modifier.height(8.dp))
						androidx.compose.material3.Text(
							text = errorMessage.ifBlank { stringResource(R.string.report_issue_error_default) },
							color = Color(0xFFCC3333),
							fontSize = 13.sp,
						)
					}

					if (duplicateNumber > 0 && reportState != ReportState.SUCCESS) {
						Spacer(modifier = Modifier.height(8.dp))
						androidx.compose.material3.Text(
							text = stringResource(R.string.report_issue_duplicate_warning, duplicateNumber),
							color = Color(0xFFE0A030),
							fontSize = 13.sp,
						)
					}

					// Auto-collected info note
					Spacer(modifier = Modifier.height(12.dp))
					androidx.compose.material3.Text(
						text = stringResource(R.string.report_issue_device_info_note),
						color = Color(0xFF505050),
						fontSize = 11.sp,
					)

					if (tokenExpired) {
						Spacer(modifier = Modifier.height(8.dp))
						androidx.compose.material3.Text(
							text = stringResource(R.string.report_issue_token_expired),
							color = Color(0xFFCC3333),
							fontSize = 12.sp,
						)
					}

					Spacer(modifier = Modifier.height(24.dp))

					// Submit button
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.End,
					) {
						var submitFocused by remember { mutableStateOf(false) }
						Button(
							onClick = {
								reportState = ReportState.SUBMITTING
								errorMessage = ""
								scope.launch(Dispatchers.IO) {
									val cooldown = issueReporterService.getCooldownRemaining()
									if (cooldown > 0) {
										withContext(Dispatchers.Main) {
											errorMessage = "Please wait ${cooldown} seconds before submitting again."
											reportState = ReportState.ERROR
										}
										return@launch
									}

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
										updateVersion = BuildConfig.VERSION_NAME,
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
								containerColor = if (submitFocused) Color(0xFFE04444) else Color(0xFFCC3333),
								contentColor = Color.White,
								disabledContainerColor = Color(0xFF3A2020),
								disabledContentColor = Color(0xFF808080),
							),
							shape = RoundedCornerShape(8.dp),
							modifier = Modifier
								.width(180.dp)
								.height(44.dp)
								.then(
									if (submitFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
									else Modifier
								)
								.onFocusChanged { submitFocused = it.isFocused },
						) {
							androidx.compose.material3.Text(
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
}
