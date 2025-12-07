package org.jellyfin.androidtv.ui.playback.common

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.preference.constant.NextUpBehavior
import org.jellyfin.androidtv.util.apiclient.JellyfinImage
import org.jellyfin.androidtv.util.apiclient.getLogoImage
import org.jellyfin.androidtv.util.apiclient.getPrimaryImage
import org.jellyfin.androidtv.util.apiclient.ioCall
import org.jellyfin.androidtv.util.sdk.getDisplayName
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.UUID
import org.jellyfin.sdk.model.api.BaseItemDto

/**
 * Data class representing item information for playback prompts.
 */
data class PlaybackPromptItemData(
	val baseItem: BaseItemDto,
	val id: UUID,
	val title: String,
	val thumbnail: JellyfinImage?,
	val logo: JellyfinImage?,
)

/**
 * Base ViewModel for playback prompts (Next Up, Still Watching, etc.).
 * Provides common functionality for loading item data and managing state.
 *
 * @param S The specific state enum type for the prompt
 */
abstract class PlaybackPromptViewModel<S : Enum<S>>(
	private val context: Context,
	private val api: ApiClient,
	private val userPreferences: UserPreferences,
	private val initialState: S,
	private val noDataState: S,
) : ViewModel() {
	
	private val _item = MutableStateFlow<PlaybackPromptItemData?>(null)
	val item: StateFlow<PlaybackPromptItemData?> = _item
	
	private val _state = MutableStateFlow(initialState)
	val state: StateFlow<S> = _state
	
	/**
	 * Update the current state.
	 */
	protected fun setState(newState: S) {
		_state.value = newState
	}
	
	/**
	 * Load item data for the given item ID.
	 */
	fun setItemId(id: UUID?) = viewModelScope.launch {
		if (id == null) {
			_item.value = null
			_state.value = noDataState
		} else {
			_item.value = loadItemData(id)
		}
	}
	
	/**
	 * Load item data from the API.
	 */
	private suspend fun loadItemData(id: UUID) = api.ioCall {
		val item by userLibraryApi.getItem(itemId = id)

		val thumbnail = when (userPreferences[UserPreferences.nextUpBehavior]) {
			NextUpBehavior.EXTENDED -> item.getPrimaryImage()
			else -> null
		}
		val logo = item.getLogoImage()
		val title = item.getDisplayName(context)

		PlaybackPromptItemData(
			item,
			item.id,
			title,
			thumbnail,
			logo,
		)
	}
}
