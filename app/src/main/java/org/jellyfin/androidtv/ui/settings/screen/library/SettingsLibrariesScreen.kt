package org.jellyfin.androidtv.ui.settings.screen.library

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.MultiServerRepository
import org.jellyfin.androidtv.data.repository.UserViewsRepository
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType
import org.koin.compose.koinInject

/**
 * Wrapper to hold library info with optional server context for display
 */
private data class LibraryDisplayItem(
	val library: BaseItemDto,
	val displayName: String,
	val isMultiServer: Boolean
)

@Composable
fun SettingsLibrariesScreen() {
	val router = LocalRouter.current
	val userViewsRepository = koinInject<UserViewsRepository>()
	val multiServerRepository = koinInject<MultiServerRepository>()
	
	var libraries by remember { mutableStateOf<List<LibraryDisplayItem>>(emptyList()) }
	
	// Load libraries based on server count (supports multi-server)
	LaunchedEffect(Unit) {
		val loggedInServers = multiServerRepository.getLoggedInServers()
		
		if (loggedInServers.size > 1) {
			// Multi-server: use aggregated libraries with server names
			val aggregatedLibraries = multiServerRepository.getAggregatedLibraries(includeHidden = true)
			libraries = aggregatedLibraries.map { aggregated ->
				LibraryDisplayItem(
					library = aggregated.library,
					displayName = aggregated.displayName,
					isMultiServer = true
				)
			}
		} else {
			// Single server: use existing behavior
			userViewsRepository.views.collect { views ->
				libraries = views.map { library ->
					LibraryDisplayItem(
						library = library,
						displayName = library.name ?: "",
						isMultiServer = false
					)
				}
			}
		}
	}

	SettingsColumn {
		item {
			ListSection(
				overlineContent = { Text(stringResource(R.string.pref_customization).uppercase()) },
				headingContent = { Text(stringResource(R.string.pref_libraries)) },
			)
		}

		items(libraries) { item ->
			val allowGridView = userViewsRepository.allowGridView(item.library.collectionType)
			val displayPreferencesId = item.library.displayPreferencesId

			if (item.library.collectionType == CollectionType.LIVETV) {
				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_guide), contentDescription = null) },
					headingContent = { Text(item.displayName) },
					onClick = { router.push(Routes.LIVETV_GUIDE_OPTIONS) }
				)
			} else {
				val canOpen = allowGridView && displayPreferencesId != null

				ListButton(
					leadingContent = { Icon(painterResource(R.drawable.ic_folder), contentDescription = null) },
					headingContent = { Text(item.displayName) },
					enabled = canOpen,
					onClick = {
						if (canOpen) {
							router.push(
								Routes.LIBRARIES_DISPLAY,
								mapOf("itemId" to item.library.id.toString(), "displayPreferencesId" to item.library.displayPreferencesId!!)
							)
						}
					}
				)
			}
		}
	}
}
