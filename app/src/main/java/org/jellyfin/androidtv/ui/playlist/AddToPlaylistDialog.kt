package org.jellyfin.androidtv.ui.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ServerUserSession
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.JellyfinTheme
import org.jellyfin.androidtv.ui.base.button.Button
import org.jellyfin.androidtv.ui.base.button.ButtonDefaults
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import timber.log.Timber
import java.util.UUID

internal enum class PlaylistDialogMode {
	SELECT_SERVER, MAIN, SELECT_PLAYLIST, CREATE_NEW
}

@Composable
fun AddToPlaylistDialog(
	itemId: UUID,
	api: ApiClient,
	onDismiss: () -> Unit,
	onAddToPlaylist: (playlistId: UUID, serverApi: ApiClient) -> Unit,
	onCreatePlaylist: (name: String, isPublic: Boolean, serverApi: ApiClient) -> Unit,
	enableMultiServer: Boolean = false,
	serverSessions: List<ServerUserSession> = emptyList(),
) {
	// Start with server selection if multi-server is enabled and we have multiple servers
	val initialMode = if (enableMultiServer && serverSessions.size > 1) {
		PlaylistDialogMode.SELECT_SERVER
	} else {
		PlaylistDialogMode.MAIN
	}
	
	var mode by remember { mutableStateOf(initialMode) }
	var playlists by remember { mutableStateOf<List<BaseItemDto>>(emptyList()) }
	var loadingPlaylists by remember { mutableStateOf(false) }
	var newPlaylistName by remember { mutableStateOf("") }
	var isPlaylistPublic by remember { mutableStateOf(false) }
	
	// Track selected server - default to the first session or use the main api
	var selectedServerSession by remember { 
		mutableStateOf(serverSessions.firstOrNull())
	}
	val activeApi = selectedServerSession?.apiClient ?: api

	LaunchedEffect(mode, selectedServerSession) {
		if (mode == PlaylistDialogMode.SELECT_PLAYLIST) {
			// Reset and reload playlists when entering playlist selection
			playlists = emptyList()
			loadingPlaylists = true
			try {
				val response = withContext(Dispatchers.IO) {
					activeApi.itemsApi.getItems(
						includeItemTypes = setOf(BaseItemKind.PLAYLIST),
						recursive = true,
						sortBy = setOf(ItemSortBy.SORT_NAME),
						sortOrder = setOf(SortOrder.ASCENDING),
						fields = setOf(ItemFields.CAN_DELETE),
					)
				}
				playlists = response.content.items.filter { it.canDelete == true }
			} catch (e: Exception) {
				Timber.e(e, "Failed to load playlists")
			}
			loadingPlaylists = false
		}
	}

	Surface(
		modifier = Modifier,
		color = Color.Black.copy(alpha = 0.95f),
		shape = androidx.compose.material3.MaterialTheme.shapes.extraLarge
	) {
		AlertDialog(
			onDismissRequest = onDismiss,
			title = {
				androidx.compose.material3.Text(
					when (mode) {
						PlaylistDialogMode.SELECT_SERVER -> stringResource(R.string.lbl_select_server)
						PlaylistDialogMode.MAIN -> stringResource(R.string.lbl_add_to_playlist)
						PlaylistDialogMode.SELECT_PLAYLIST -> stringResource(R.string.lbl_select_playlist)
						PlaylistDialogMode.CREATE_NEW -> stringResource(R.string.lbl_create_new_playlist)
					},
					color = Color.Gray
				)
			},
			text = {
				Column(modifier = Modifier.fillMaxWidth()) {
					when (mode) {
						PlaylistDialogMode.SELECT_SERVER -> {
							LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
								items(serverSessions) { session ->
									Button(
										onClick = {
											selectedServerSession = session
											mode = PlaylistDialogMode.MAIN
										},
										modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
										colors = ButtonDefaults.colors(
											containerColor = JellyfinTheme.colorScheme.button,
											contentColor = JellyfinTheme.colorScheme.onButton,
											focusedContainerColor = JellyfinTheme.colorScheme.buttonFocused,
											focusedContentColor = JellyfinTheme.colorScheme.onButtonFocused,
										)
									) {
										androidx.compose.material3.Text(
											session.server.name,
											modifier = Modifier.fillMaxWidth(),
											color = Color.White
										)
									}
								}
							}
						}
						PlaylistDialogMode.MAIN -> {
							Button(
								onClick = { mode = PlaylistDialogMode.SELECT_PLAYLIST },
								modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
								colors = ButtonDefaults.colors(
									containerColor = JellyfinTheme.colorScheme.button,
									contentColor = JellyfinTheme.colorScheme.onButton,
									focusedContainerColor = JellyfinTheme.colorScheme.buttonFocused,
									focusedContentColor = JellyfinTheme.colorScheme.onButtonFocused,
								)
							) {
								Row(
									modifier = Modifier.fillMaxWidth(),
									horizontalArrangement = Arrangement.Start,
									verticalAlignment = Alignment.CenterVertically
								) {
									Icon(
										imageVector = ImageVector.vectorResource(R.drawable.ic_folder),
										contentDescription = null,
										modifier = Modifier.padding(end = 12.dp)
									)
									androidx.compose.material3.Text(stringResource(R.string.lbl_select_playlist), color = Color.White)
								}
							}
							Button(
								onClick = { mode = PlaylistDialogMode.CREATE_NEW },
								modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
								colors = ButtonDefaults.colors(
									containerColor = JellyfinTheme.colorScheme.button,
									contentColor = JellyfinTheme.colorScheme.onButton,
									focusedContainerColor = JellyfinTheme.colorScheme.buttonFocused,
									focusedContentColor = JellyfinTheme.colorScheme.onButtonFocused,
								)
							) {
								Row(
									modifier = Modifier.fillMaxWidth(),
									horizontalArrangement = Arrangement.Start,
									verticalAlignment = Alignment.CenterVertically
								) {
									Icon(
										imageVector = ImageVector.vectorResource(R.drawable.ic_add),
										contentDescription = null,
										modifier = Modifier.padding(end = 12.dp)
									)
									androidx.compose.material3.Text(stringResource(R.string.lbl_create_new_playlist), color = Color.White)
								}
							}
						}
						PlaylistDialogMode.SELECT_PLAYLIST -> {
							if (loadingPlaylists) {
								Row(
									modifier = Modifier.fillMaxWidth().padding(16.dp),
									horizontalArrangement = Arrangement.Center
								) {
									CircularProgressIndicator(strokeWidth = 2.dp)
								}
							} else if (playlists.isEmpty()) {
								androidx.compose.material3.Text(
									"No playlists found. Create a new one!",
									color = Color.Gray,
									modifier = Modifier.padding(16.dp)
								)
							} else {
								LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
									items(playlists) { playlist ->
										Button(
											onClick = {
												onAddToPlaylist(playlist.id, activeApi)
											},
											modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
											colors = ButtonDefaults.colors(
												containerColor = JellyfinTheme.colorScheme.button,
												contentColor = JellyfinTheme.colorScheme.onButton,
												focusedContainerColor = JellyfinTheme.colorScheme.buttonFocused,
												focusedContentColor = JellyfinTheme.colorScheme.onButtonFocused,
											)
										) {
											androidx.compose.material3.Text(
												playlist.name ?: "Unknown",
												modifier = Modifier.fillMaxWidth(),
												color = Color.White
											)
										}
									}
								}
							}
						}
						PlaylistDialogMode.CREATE_NEW -> {
							OutlinedTextField(
								value = newPlaylistName,
								onValueChange = { newPlaylistName = it },
								label = { androidx.compose.material3.Text(stringResource(R.string.lbl_playlist_name)) },
								modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
								singleLine = true
							)
							Row(
								modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
								verticalAlignment = Alignment.CenterVertically,
								horizontalArrangement = Arrangement.SpaceBetween
							) {
								androidx.compose.material3.Text(
									stringResource(R.string.lbl_public_playlist),
									color = Color.Gray
								)
								androidx.compose.material3.Switch(
									checked = isPlaylistPublic,
									onCheckedChange = { isPlaylistPublic = it }
								)
							}
						}
					}
				}
			},
			confirmButton = {
				when (mode) {
					PlaylistDialogMode.SELECT_SERVER -> { }
					PlaylistDialogMode.MAIN -> { 
						// Show back button to server selection if multi-server is enabled
						if (enableMultiServer && serverSessions.size > 1) {
							TextButton(onClick = { mode = PlaylistDialogMode.SELECT_SERVER }) {
								androidx.compose.material3.Text("Change Server")
							}
						}
					}
					PlaylistDialogMode.SELECT_PLAYLIST -> {
						TextButton(onClick = { mode = PlaylistDialogMode.MAIN }) {
							androidx.compose.material3.Text("Back")
						}
					}
					PlaylistDialogMode.CREATE_NEW -> {
						Row {
							TextButton(onClick = { mode = PlaylistDialogMode.MAIN }) {
								androidx.compose.material3.Text("Back")
							}
							TextButton(
								onClick = {
									if (newPlaylistName.isNotBlank()) {
										onCreatePlaylist(newPlaylistName.trim(), isPlaylistPublic, activeApi)
									}
								},
								enabled = newPlaylistName.isNotBlank()
							) {
								androidx.compose.material3.Text("Create & Add")
							}
						}
					}
				}
			},
			dismissButton = {
				TextButton(onClick = onDismiss) {
					androidx.compose.material3.Text("Cancel")
				}
			}
		)
	}
}
