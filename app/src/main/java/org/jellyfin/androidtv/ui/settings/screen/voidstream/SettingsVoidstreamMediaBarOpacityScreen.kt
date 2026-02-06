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
fun SettingsVoidStreamMediaBarOpacityScreen() {
	val router = LocalRouter.current
	val userSettingPreferences = koinInject<UserSettingPreferences>()
	var mediaBarOverlayOpacity by rememberPreference(userSettingPreferences, UserSettingPreferences.mediaBarOverlayOpacity)

	// 10-90% in 5% increments
	val options = (10..90 step 5).map { it to "$it%" }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_media_bar_title).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_media_bar_overlay_opacity)) },
				captionContent = { Text(stringResource(R.string.pref_media_bar_overlay_opacity_summary)) },
			)
		}

		items(options) { (value, label) ->
			ListButton(
				headingContent = { Text(label) },
				trailingContent = { RadioButton(checked = mediaBarOverlayOpacity == value) },
				onClick = {
					mediaBarOverlayOpacity = value
					router.back()
				}
			)
		}
	}
}
