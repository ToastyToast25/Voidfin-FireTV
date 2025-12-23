package org.jellyfin.androidtv.ui.playback.overlay.action

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.playback.AudioDelayController
import org.jellyfin.androidtv.ui.playback.PlaybackController
import org.jellyfin.androidtv.ui.playback.overlay.CustomPlaybackTransportControlGlue
import org.jellyfin.androidtv.ui.playback.overlay.VideoPlayerAdapter

class AudioDelayAction(
	context: Context,
	customPlaybackTransportControlGlue: CustomPlaybackTransportControlGlue,
	playbackController: PlaybackController
) : CustomAction(context, customPlaybackTransportControlGlue) {
	private val delayController = AudioDelayController(playbackController)
	private val delaySteps = AudioDelayController.DelaySteps.entries.toTypedArray()
	private var popup: PopupMenu? = null

	init {
		initializeWithIcon(R.drawable.ic_audio_sync)
	}

	override fun handleClickAction(
		playbackController: PlaybackController,
		videoPlayerAdapter: VideoPlayerAdapter,
		context: Context,
		view: View,
	) {
		videoPlayerAdapter.leanbackOverlayFragment.setFading(false)
		dismissPopup()
		popup = populateMenu(context, view, delayController)

		popup?.setOnDismissListener {
			videoPlayerAdapter.leanbackOverlayFragment.setFading(true)
			popup = null
		}

		popup?.setOnMenuItemClickListener { menuItem ->
			delayController.currentDelay = delaySteps[menuItem.itemId]
			true
		}

		popup?.show()
	}

	private fun populateMenu(
		context: Context,
		view: View,
		delayController: AudioDelayController
	) = PopupMenu(context, view, Gravity.END).apply {
		delaySteps.forEachIndexed { i, step ->
			menu.add(0, i, i, step.label)
		}

		menu.setGroupCheckable(0, true, true)
		menu.getItem(delaySteps.indexOf(delayController.currentDelay)).isChecked = true
	}

	fun dismissPopup() {
		popup?.dismiss()
	}
}
