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
fun SettingsVoidStreamMediaBarItemCountScreen() {
	val router = LocalRouter.current
	val userSettingPreferences = koinInject<UserSettingPreferences>()
	var mediaBarItemCount by rememberPreference(userSettingPreferences, UserSettingPreferences.mediaBarItemCount)

	val options = listOf(
		"5" to stringResource(R.string.pref_media_bar_5_items),
		"10" to stringResource(R.string.pref_media_bar_10_items),
		"15" to stringResource(R.string.pref_media_bar_15_items)
	)

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_media_bar_title).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_media_bar_item_count)) },
			)
		}

		items(options) { (value, label) ->
			ListButton(
				headingContent = { Text(label) },
				trailingContent = { RadioButton(checked = mediaBarItemCount == value) },
				onClick = {
					mediaBarItemCount = value
					router.back()
				}
			)
		}
	}
}
