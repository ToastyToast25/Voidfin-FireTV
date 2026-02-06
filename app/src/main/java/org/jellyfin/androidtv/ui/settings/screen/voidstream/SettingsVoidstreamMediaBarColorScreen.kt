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
fun SettingsVoidStreamMediaBarColorScreen() {
	val router = LocalRouter.current
	val userSettingPreferences = koinInject<UserSettingPreferences>()
	var mediaBarOverlayColor by rememberPreference(userSettingPreferences, UserSettingPreferences.mediaBarOverlayColor)

	val options = listOf(
		"black" to stringResource(R.string.pref_media_bar_color_black),
		"gray" to stringResource(R.string.pref_media_bar_color_gray),
		"dark_blue" to stringResource(R.string.pref_media_bar_color_dark_blue),
		"purple" to stringResource(R.string.pref_media_bar_color_purple),
		"teal" to stringResource(R.string.pref_media_bar_color_teal),
		"navy" to stringResource(R.string.pref_media_bar_color_navy),
		"charcoal" to stringResource(R.string.pref_media_bar_color_charcoal),
		"brown" to stringResource(R.string.pref_media_bar_color_brown),
		"dark_red" to stringResource(R.string.pref_media_bar_color_dark_red),
		"dark_green" to stringResource(R.string.pref_media_bar_color_dark_green),
		"slate" to stringResource(R.string.pref_media_bar_color_slate),
		"indigo" to stringResource(R.string.pref_media_bar_color_indigo)
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_media_bar_title).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_media_bar_overlay_color)) },
			)
		}

		items(options) { (value, label) ->
			ListButton(
				headingContent = { Text(label) },
				trailingContent = { RadioButton(checked = mediaBarOverlayColor == value) },
				onClick = {
					mediaBarOverlayColor = value
					router.back()
				}
			)
		}
	}
}
