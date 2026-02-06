package org.jellyfin.androidtv.ui.settings.screen.voidstream

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsVoidStreamDetailsBlurScreen() {
	val router = LocalRouter.current
	val userSettingPreferences = koinInject<UserSettingPreferences>()
	var detailsBackgroundBlurAmount by rememberPreference(userSettingPreferences, UserSettingPreferences.detailsBackgroundBlurAmount)

	val options = listOf(
		0 to stringResource(R.string.pref_blur_none),
		5 to stringResource(R.string.pref_blur_light),
		10 to stringResource(R.string.pref_blur_medium),
		15 to stringResource(R.string.pref_blur_strong),
		20 to stringResource(R.string.pref_blur_extra_strong)
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_appearance).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_details_background_blur_amount)) },
				captionContent = { Text(stringResource(R.string.pref_details_background_blur_amount_description)) },
			)
		}

		items(options) { (value, label) ->
			ListButton(
				headingContent = { Text(label) },
				trailingContent = { RadioButton(checked = detailsBackgroundBlurAmount == value) },
				onClick = {
					detailsBackgroundBlurAmount = value
					router.back()
				}
			)
		}
	}
}
