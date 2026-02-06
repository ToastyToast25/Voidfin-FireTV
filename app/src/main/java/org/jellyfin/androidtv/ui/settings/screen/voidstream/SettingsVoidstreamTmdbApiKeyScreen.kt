package org.jellyfin.androidtv.ui.settings.screen.voidstream

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsVoidStreamTmdbApiKeyScreen() {
	val router = LocalRouter.current
	val context = LocalContext.current
	val userSettingPreferences = koinInject<UserSettingPreferences>()
	var tmdbApiKey by rememberPreference(userSettingPreferences, UserSettingPreferences.tmdbApiKey)
	var tempApiKey by remember { mutableStateOf(tmdbApiKey) }

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_tmdb_api_key)) },
				captionContent = { Text(stringResource(R.string.pref_tmdb_api_key_description)) },
			)
		}

		item {
			Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
				OutlinedTextField(
					value = tempApiKey,
					onValueChange = { tempApiKey = it },
					modifier = Modifier.fillMaxWidth(),
					label = { androidx.compose.material3.Text(stringResource(R.string.pref_tmdb_api_key_label), color = Color.White) },
					placeholder = { androidx.compose.material3.Text(stringResource(R.string.pref_tmdb_api_key_hint), color = Color.Gray) },
					singleLine = true,
					keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
					colors = OutlinedTextFieldDefaults.colors(
						focusedTextColor = Color.White,
						unfocusedTextColor = Color.White,
						focusedBorderColor = Color.White,
						unfocusedBorderColor = Color.Gray,
						cursorColor = Color.White
					)
				)
			}
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_save)) },
				onClick = {
					val trimmedKey = tempApiKey.trim()
					if (trimmedKey.isNotEmpty() && trimmedKey.length < 16) {
						Toast.makeText(context, context.getString(R.string.msg_api_key_invalid), Toast.LENGTH_SHORT).show()
						return@ListButton
					}
					tmdbApiKey = trimmedKey
					Toast.makeText(context, context.getString(R.string.msg_api_key_saved), Toast.LENGTH_SHORT).show()
					router.back()
				}
			)
		}

		item {
			ListButton(
				headingContent = { Text(stringResource(R.string.lbl_clear)) },
				onClick = {
					tempApiKey = ""
					tmdbApiKey = ""
					Toast.makeText(context, context.getString(R.string.msg_api_key_cleared), Toast.LENGTH_SHORT).show()
				}
			)
		}
	}
}
