package org.jellyfin.androidtv.ui.playback.stillwatching

import android.content.Context
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.InteractionTrackerViewModel
import org.jellyfin.androidtv.ui.playback.common.PlaybackPromptViewModel
import org.jellyfin.sdk.api.client.ApiClient

class StillWatchingViewModel(
	context: Context,
	api: ApiClient,
	userPreferences: UserPreferences,
	private val interactionTrackerViewModel: InteractionTrackerViewModel,
) : PlaybackPromptViewModel<StillWatchingState>(
	context,
	api,
	userPreferences,
	initialState = StillWatchingState.INITIALIZED,
	noDataState = StillWatchingState.NO_DATA,
) {
	fun stillWatching() {
		interactionTrackerViewModel.notifyStillWatching()
		setState(StillWatchingState.STILL_WATCHING)
	}

	fun close() {
		setState(StillWatchingState.CLOSE)
	}
}
