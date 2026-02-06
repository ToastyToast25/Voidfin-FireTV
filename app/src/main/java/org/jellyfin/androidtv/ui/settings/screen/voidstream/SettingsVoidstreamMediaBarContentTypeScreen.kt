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
fun SettingsVoidStreamMediaBarContentTypeScreen() {
	val router = LocalRouter.current
	val userSettingPreferences = koinInject<UserSettingPreferences>()
	var mediaBarContentType by rememberPreference(userSettingPreferences, UserSettingPreferences.mediaBarContentType)

	val options = listOf(
		"movies" to stringResource(R.string.pref_shuffle_movies),
		"tv" to stringResource(R.string.pref_shuffle_tv),
		"both" to stringResource(R.string.pref_shuffle_both)
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_media_bar_title).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_media_bar_content_type)) },
			)
		}

		items(options) { (value, label) ->
			ListButton(
				headingContent = { Text(label) },
				trailingContent = { RadioButton(checked = mediaBarContentType == value) },
				onClick = {
					mediaBarContentType = value
					router.back()
				}
			)
		}
	}
}
