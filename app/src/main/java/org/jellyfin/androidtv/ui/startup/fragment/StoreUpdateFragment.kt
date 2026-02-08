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

class StoreUpdateFragment : Fragment() {
	companion object {
		private const val ARG_VERSION = "arg_version"
		private const val ARG_RELEASE_NOTES = "arg_release_notes"
		private const val ARG_IS_FORCED = "arg_is_forced"
		private const val ARG_GRACE_PERIOD_DAYS = "arg_grace_period_days"
		private const val ARG_STORE_NAME = "arg_store_name"

		fun newInstance(
			version: String,
			releaseNotes: String,
			isForced: Boolean = false,
			gracePeriodDays: Int = 0,
			storeName: String = "App Store"
		) = StoreUpdateFragment().apply {
			arguments = Bundle().apply {
				putString(ARG_VERSION, version)
				putString(ARG_RELEASE_NOTES, releaseNotes)
				putBoolean(ARG_IS_FORCED, isForced)
				putInt(ARG_GRACE_PERIOD_DAYS, gracePeriodDays)
				putString(ARG_STORE_NAME, storeName)
			}
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	) = content {
		val version = arguments?.getString(ARG_VERSION) ?: ""
		val releaseNotes = arguments?.getString(ARG_RELEASE_NOTES) ?: ""
		val isForced = arguments?.getBoolean(ARG_IS_FORCED, false) ?: false
		val gracePeriodDays = arguments?.getInt(ARG_GRACE_PERIOD_DAYS, 0) ?: 0
		val storeName = arguments?.getString(ARG_STORE_NAME) ?: "App Store"

		JellyfinTheme {
			StoreUpdateScreen(
				version = version,
				releaseNotes = releaseNotes,
				isForced = isForced,
				gracePeriodDays = gracePeriodDays,
				storeName = storeName,
				onUpdateClick = {
					parentFragmentManager.setFragmentResult("store_update_done", bundleOf("update_clicked" to true))
				},
				onNotNowClick = if (!isForced) {
					{
						parentFragmentManager.setFragmentResult("store_update_done", bundleOf("update_clicked" to false))
					}
				} else null
			)
		}
	}
}

@Composable
private fun StoreUpdateScreen(
	version: String,
	releaseNotes: String,
	isForced: Boolean,
	gracePeriodDays: Int,
	storeName: String,
	onUpdateClick: () -> Unit,
	onNotNowClick: (() -> Unit)?,
) {
	val updateBtnFocus = remember { FocusRequester() }

	LaunchedEffect(Unit) { updateBtnFocus.requestFocus() }

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
			StoreUpdateCard(
				version = version,
				releaseNotes = releaseNotes,
				isForced = isForced,
				gracePeriodDays = gracePeriodDays,
				storeName = storeName,
				updateBtnFocus = updateBtnFocus,
				onUpdateClick = onUpdateClick,
				onNotNowClick = onNotNowClick,
			)
		}
	}
}

@Composable
private fun StoreUpdateCard(
	version: String,
	releaseNotes: String,
	isForced: Boolean,
	gracePeriodDays: Int,
	storeName: String,
	updateBtnFocus: FocusRequester,
	onUpdateClick: () -> Unit,
	onNotNowClick: (() -> Unit)?,
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
				text = if (isForced) stringResource(R.string.store_update_required_title) else stringResource(R.string.store_update_available_title),
				color = Color.White,
				fontSize = 20.sp,
				fontWeight = FontWeight.Bold,
			)
			Spacer(modifier = Modifier.height(4.dp))
			Text(
				text = stringResource(R.string.store_update_version, version),
				color = accentRed,
				fontSize = 14.sp,
				fontWeight = FontWeight.Medium,
			)
		}

		HorizontalDivider(color = borderTint, thickness = 1.dp)

		// -- Body --
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(28.dp),
		) {
			val message = when {
				isForced && gracePeriodDays > 0 -> stringResource(R.string.store_update_grace_period_message, gracePeriodDays, storeName)
				isForced -> stringResource(R.string.store_update_forced_message, storeName)
				else -> stringResource(R.string.store_update_optional_message, storeName)
			}

			Text(
				text = message,
				color = Color(0xFFCCCCCC),
				fontSize = 15.sp,
				lineHeight = 21.sp,
				textAlign = TextAlign.Start,
			)

			if (releaseNotes.isNotBlank() && releaseNotes.length < 200) {
				Spacer(modifier = Modifier.height(16.dp))
				Text(
					text = stringResource(R.string.store_update_whats_new),
					color = Color.White,
					fontSize = 14.sp,
					fontWeight = FontWeight.SemiBold,
				)
				Spacer(modifier = Modifier.height(8.dp))
				Text(
					text = releaseNotes.take(200),
					color = Color(0xFF808080),
					fontSize = 13.sp,
					lineHeight = 18.sp,
					textAlign = TextAlign.Start,
				)
			}
		}

		HorizontalDivider(color = borderTint, thickness = 1.dp)

		// -- Footer with buttons --
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.background(headerBg)
				.padding(horizontal = 28.dp, vertical = 16.dp),
			horizontalArrangement = if (onNotNowClick != null) Arrangement.SpaceBetween else Arrangement.End,
		) {
			// "Not Now" button (only if update is optional)
			if (onNotNowClick != null) {
				var notNowFocused by remember { mutableStateOf(false) }
				Button(
					onClick = onNotNowClick,
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
						text = stringResource(R.string.store_update_not_now),
						fontSize = 14.sp,
						fontWeight = FontWeight.Medium,
					)
				}
			}

			// "Update Now" button
			var updateFocused by remember { mutableStateOf(false) }
			val focusedBg = Color(0xFFE04444)
			Button(
				onClick = onUpdateClick,
				colors = ButtonDefaults.buttonColors(
					containerColor = if (updateFocused) focusedBg else accentRed,
					contentColor = Color.White,
				),
				shape = RoundedCornerShape(8.dp),
				modifier = Modifier
					.width(160.dp)
					.height(40.dp)
					.focusRequester(updateBtnFocus)
					.then(
						if (updateFocused) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
						else Modifier
					)
					.onFocusChanged { updateFocused = it.isFocused },
			) {
				Text(
					text = stringResource(R.string.store_update_button),
					fontSize = 14.sp,
					fontWeight = FontWeight.Medium,
				)
			}
		}
	}
}
