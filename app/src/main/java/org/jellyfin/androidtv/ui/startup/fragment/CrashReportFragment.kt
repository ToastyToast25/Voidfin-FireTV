package org.jellyfin.androidtv.ui.startup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.compose.content
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.base.JellyfinTheme

class CrashReportFragment : Fragment() {
	companion object {
		fun newInstance() = CrashReportFragment()
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	) = content {
		JellyfinTheme {
			CrashReportScreen(
				onSendReport = {
					parentFragmentManager.setFragmentResult("crash_report_done", bundleOf("send_report" to true))
				},
				onNotNow = {
					parentFragmentManager.setFragmentResult("crash_report_done", bundleOf("send_report" to false))
				}
			)
		}
	}
}

@Composable
private fun CrashReportScreen(
	onSendReport: () -> Unit,
	onNotNow: () -> Unit,
) {
	val sendBtnFocus = remember { FocusRequester() }

	LaunchedEffect(Unit) { sendBtnFocus.requestFocus() }

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(colorResource(R.color.not_quite_black))
			.focusable(),
		contentAlignment = Alignment.Center,
	) {
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			// App branding
			Image(
				painter = painterResource(R.drawable.app_logo),
				contentDescription = null,
				modifier = Modifier.height(72.dp).padding(bottom = 20.dp),
			)

			// Main card container
			CrashReportCard(
				sendBtnFocus = sendBtnFocus,
				onSendReport = onSendReport,
				onNotNow = onNotNow,
			)
		}
	}
}

@Composable
private fun CrashReportCard(
	sendBtnFocus: FocusRequester,
	onSendReport: () -> Unit,
	onNotNow: () -> Unit,
) {
	val cardShape = RoundedCornerShape(16.dp)
	val borderTint = Color(0xFF3A1A1A)
	val cardBg = Color(0xFF1A1212)
	val headerBg = Color(0xFF201414)
	val accentRed = Color(0xFFCC3333)

	Column(
		modifier = Modifier
			.width(480.dp)
			.clip(cardShape)
			.background(cardBg)
			.border(1.dp, borderTint, cardShape),
	) {
		// -- Card header --
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.background(headerBg)
				.padding(horizontal = 28.dp, vertical = 20.dp),
		) {
			Text(
				text = stringResource(R.string.crash_report_title),
				color = Color.White,
				fontSize = 20.sp,
				fontWeight = FontWeight.Bold,
			)
			Spacer(modifier = Modifier.height(4.dp))
			Text(
				text = stringResource(R.string.crash_report_subtitle),
				color = Color(0xFFB0B0B0),
				fontSize = 14.sp,
			)
		}

		HorizontalDivider(color = borderTint, thickness = 1.dp)

		// -- Body --
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(28.dp),
		) {
			Text(
				text = stringResource(R.string.crash_report_message),
				color = Color(0xFFCCCCCC),
				fontSize = 15.sp,
				lineHeight = 21.sp,
				textAlign = TextAlign.Start,
			)
			Spacer(modifier = Modifier.height(16.dp))
			Text(
				text = stringResource(R.string.crash_report_privacy_notice),
				color = Color(0xFF808080),
				fontSize = 13.sp,
				lineHeight = 18.sp,
				textAlign = TextAlign.Start,
			)
		}

		HorizontalDivider(color = borderTint, thickness = 1.dp)

		// -- Footer with buttons --
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.background(headerBg)
				.padding(horizontal = 28.dp, vertical = 16.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			// Not Now button
			var notNowFocused by remember { mutableStateOf(false) }
			Button(
				onClick = onNotNow,
				colors = ButtonDefaults.buttonColors(
					containerColor = if (notNowFocused) Color(0xFF3A2020) else Color(0xFF2A1515),
					contentColor = Color.White,
				),
				shape = RoundedCornerShape(8.dp),
				modifier = Modifier
					.width(120.dp)
					.height(40.dp)
					.then(
						if (notNowFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
						else Modifier
					)
					.onFocusChanged { notNowFocused = it.isFocused },
			) {
				Text(
					text = stringResource(R.string.crash_report_not_now),
					fontSize = 14.sp,
					fontWeight = FontWeight.Medium,
				)
			}

			// Send Report button
			var sendFocused by remember { mutableStateOf(false) }
			val focusedBg = Color(0xFFE04444)
			Button(
				onClick = onSendReport,
				colors = ButtonDefaults.buttonColors(
					containerColor = if (sendFocused) focusedBg else accentRed,
					contentColor = Color.White,
				),
				shape = RoundedCornerShape(8.dp),
				modifier = Modifier
					.width(160.dp)
					.height(40.dp)
					.focusRequester(sendBtnFocus)
					.then(
						if (sendFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
						else Modifier
					)
					.onFocusChanged { sendFocused = it.isFocused },
			) {
				Text(
					text = stringResource(R.string.crash_report_send),
					fontSize = 14.sp,
					fontWeight = FontWeight.Medium,
				)
			}
		}
	}
}
