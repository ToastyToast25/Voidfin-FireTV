package org.jellyfin.androidtv.util.apiclient

import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType

/**
 * Helper functions for filtering media streams by type.
 */
object StreamHelper {
	/**
	 * Get all subtitle streams from a media source.
	 */
	@JvmStatic
	fun getSubtitleStreams(mediaSource: MediaSourceInfo?): List<MediaStream> {
		return getStreams(mediaSource, MediaStreamType.SUBTITLE)
	}
	
	/**
	 * Get all audio streams from a media source.
	 */
	@JvmStatic
	fun getAudioStreams(mediaSource: MediaSourceInfo?): List<MediaStream> {
		return getStreams(mediaSource, MediaStreamType.AUDIO)
	}
	
	/**
	 * Get all streams of a specific type from a media source.
	 */
	private fun getStreams(mediaSource: MediaSourceInfo?, type: MediaStreamType): List<MediaStream> {
		return mediaSource?.mediaStreams?.filter { it.type == type } ?: emptyList()
	}
}
