package org.jellyfin.androidtv.ui.playback

/**
 * Controller for managing audio delay/advance synchronization.
 * Positive values delay audio (audio plays later), negative values advance audio (audio plays earlier).
 */
class AudioDelayController(
	private val parentController: PlaybackController
) {
	enum class DelaySteps(val delayMs: Long, val label: String) {
		DELAY_NEG_2000(delayMs = -2000L, label = "-2.0s"),
		DELAY_NEG_1500(delayMs = -1500L, label = "-1.5s"),
		DELAY_NEG_1000(delayMs = -1000L, label = "-1.0s"),
		DELAY_NEG_750(delayMs = -750L, label = "-750ms"),
		DELAY_NEG_500(delayMs = -500L, label = "-500ms"),
		DELAY_NEG_250(delayMs = -250L, label = "-250ms"),
		DELAY_NEG_100(delayMs = -100L, label = "-100ms"),
		DELAY_NONE(delayMs = 0L, label = "No Delay"),
		DELAY_POS_100(delayMs = 100L, label = "+100ms"),
		DELAY_POS_250(delayMs = 250L, label = "+250ms"),
		DELAY_POS_500(delayMs = 500L, label = "+500ms"),
		DELAY_POS_750(delayMs = 750L, label = "+750ms"),
		DELAY_POS_1000(delayMs = 1000L, label = "+1.0s"),
		DELAY_POS_1500(delayMs = 1500L, label = "+1.5s"),
		DELAY_POS_2000(delayMs = 2000L, label = "+2.0s"),
	}

	companion object {
		// Preserve the currently selected delay during app lifetime
		private var previousDelaySelection = DelaySteps.DELAY_NONE
	}

	var currentDelay = previousDelaySelection
		set(value) {
			parentController.setAudioDelay(value.delayMs)
			previousDelaySelection = value
			field = value
		}

	init {
		// Apply the previous delay selection when controller is created
		currentDelay = previousDelaySelection
	}

	fun resetDelayToDefault() {
		currentDelay = DelaySteps.DELAY_NONE
	}
}
