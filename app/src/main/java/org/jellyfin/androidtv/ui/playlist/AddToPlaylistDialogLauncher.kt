package org.jellyfin.androidtv.ui.playlist

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.MultiServerRepository
import org.jellyfin.androidtv.data.repository.ServerUserSession
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.playlistsApi
import org.jellyfin.sdk.model.api.CreatePlaylistDto
import org.koin.compose.koinInject
import timber.log.Timber
import java.util.UUID

/**
 * Show the add to playlist dialog from Java code (e.g., FullDetailsFragment)
 */
fun showAddToPlaylistDialog(
	context: android.content.Context,
	itemId: UUID,
) {
	val dialog = androidx.appcompat.app.AppCompatDialog(context, androidx.appcompat.R.style.Theme_AppCompat_Dialog)
	dialog.setContentView(
		androidx.compose.ui.platform.ComposeView(context).apply {
			setContent {
				JellyfinTheme {
					val api = koinInject<ApiClient>()
					val userPreferences = koinInject<UserPreferences>()
					val multiServerRepository = koinInject<MultiServerRepository>()
					val scope = rememberCoroutineScope()
					var showDialog by remember { mutableStateOf(true) }
					
					// Check if multi-server is enabled
					val enableMultiServer = remember { 
						userPreferences[UserPreferences.enableMultiServerLibraries] ?: false 
					}
					
					// Load server sessions if multi-server is enabled
					var serverSessions by remember { mutableStateOf<List<ServerUserSession>>(emptyList()) }
					LaunchedEffect(enableMultiServer) {
						if (enableMultiServer) {
							serverSessions = withContext(Dispatchers.IO) {
								multiServerRepository.getLoggedInServers()
							}
						}
					}

					if (showDialog) {
						AddToPlaylistDialog(
							itemId = itemId,
							api = api,
							enableMultiServer = enableMultiServer,
							serverSessions = serverSessions,
							onDismiss = {
								showDialog = false
								dialog.dismiss()
							},
							onAddToPlaylist = { playlistId, serverApi ->
								scope.launch {
									try {
										withContext(Dispatchers.IO) {
											serverApi.playlistsApi.addItemToPlaylist(
												playlistId = playlistId,
												ids = listOf(itemId)
											)
										}
										withContext(Dispatchers.Main) {
											Toast.makeText(
												context,
												context.getString(R.string.msg_added_to_playlist),
												Toast.LENGTH_SHORT
											).show()
										}
									} catch (e: Exception) {
										Timber.e(e, "Failed to add item to playlist")
										withContext(Dispatchers.Main) {
											Toast.makeText(
												context,
												context.getString(R.string.msg_failed_to_add_to_playlist),
												Toast.LENGTH_SHORT
											).show()
										}
									}
									showDialog = false
									dialog.dismiss()
								}
							},
							onCreatePlaylist = { playlistName, isPublic, serverApi ->
								scope.launch {
									try {
										withContext(Dispatchers.IO) {
											val createRequest = CreatePlaylistDto(
												name = playlistName,
												ids = listOf(itemId),
												users = emptyList(),
												isPublic = isPublic,
											)
											serverApi.playlistsApi.createPlaylist(createRequest)
										}
										withContext(Dispatchers.Main) {
											Toast.makeText(
												context,
												context.getString(R.string.msg_playlist_created),
												Toast.LENGTH_SHORT
											).show()
										}
									} catch (e: Exception) {
										Timber.e(e, "Failed to create playlist")
										withContext(Dispatchers.Main) {
											Toast.makeText(
												context,
												context.getString(R.string.msg_failed_to_create_playlist),
												Toast.LENGTH_SHORT
											).show()
										}
									}
									showDialog = false
									dialog.dismiss()
								}
							}
						)
					}
				}
			}
		}
	)
	dialog.show()
}

