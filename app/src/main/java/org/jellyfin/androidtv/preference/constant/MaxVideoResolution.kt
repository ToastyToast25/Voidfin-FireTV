package org.jellyfin.androidtv.preference.constant

import org.jellyfin.androidtv.R
import org.jellyfin.preference.PreferenceEnum

/**
 * Maximum video resolution for playback.
 * When set to a specific resolution, videos with higher resolution will be transcoded down.
 * This is useful for devices with 1080p displays that cannot play 4K content directly.
 */
enum class MaxVideoResolution(
	override val nameRes: Int,
	val maxWidth: Int,
	val maxHeight: Int,
) : PreferenceEnum {
	/**
	 * Auto-detect based on device capabilities (default behavior).
	 */
	AUTO(R.string.max_resolution_auto, Int.MAX_VALUE, Int.MAX_VALUE),

	/**
	 * Limit to 480p (SD).
	 */
	RES_480P(R.string.max_resolution_480p, 854, 480),

	/**
	 * Limit to 720p (HD).
	 */
	RES_720P(R.string.max_resolution_720p, 1280, 720),

	/**
	 * Limit to 1080p (Full HD).
	 */
	RES_1080P(R.string.max_resolution_1080p, 1920, 1080),

	/**
	 * Limit to 2160p (4K UHD).
	 */
	RES_2160P(R.string.max_resolution_2160p, 3840, 2160),
}
