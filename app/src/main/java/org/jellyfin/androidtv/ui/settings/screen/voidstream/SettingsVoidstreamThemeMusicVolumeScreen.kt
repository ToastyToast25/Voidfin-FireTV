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
fun SettingsVoidStreamThemeMusicVolumeScreen() {
	val router = LocalRouter.current
	val userSettingPreferences = koinInject<UserSettingPreferences>()
	var themeMusicVolume by rememberPreference(userSettingPreferences, UserSettingPreferences.themeMusicVolume)

	// 10-100% in 5% increments
	val options = (10..100 step 5).map { it to "$it%" }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_theme_music_title).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_theme_music_volume)) },
				captionContent = { Text(stringResource(R.string.pref_theme_music_volume_summary)) },
			)
		}

		items(options) { (value, label) ->
			ListButton(
				headingContent = { Text(label) },
				trailingContent = { RadioButton(checked = themeMusicVolume == value) },
				onClick = {
					themeMusicVolume = value
					router.back()
				}
			)
		}
	}
}
