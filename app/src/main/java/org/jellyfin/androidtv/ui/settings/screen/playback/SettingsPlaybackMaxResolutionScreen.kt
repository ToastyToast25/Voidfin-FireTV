package org.jellyfin.androidtv.ui.settings.screen.playback

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.MaxVideoResolution
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsPlaybackMaxResolutionScreen() {
	val router = LocalRouter.current
	val userPreferences = koinInject<UserPreferences>()
	var maxVideoResolution by rememberPreference(userPreferences, UserPreferences.maxVideoResolution)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_playback_advanced).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_max_resolution_title)) },
				captionContent = { Text(stringResource(R.string.pref_max_resolution_description)) },
			)
		}

		items(MaxVideoResolution.entries) { resolution ->
			ListButton(
				headingContent = { Text(stringResource(resolution.nameRes)) },
				trailingContent = { RadioButton(checked = maxVideoResolution == resolution) },
				onClick = {
					maxVideoResolution = resolution
					router.back()
				}
			)
		}
	}
}
