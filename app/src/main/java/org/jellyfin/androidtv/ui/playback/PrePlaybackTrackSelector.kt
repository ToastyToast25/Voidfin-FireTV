package org.jellyfin.androidtv.ui.playback

import android.content.Context
import android.content.SharedPreferences
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.MediaSourceInfo
import org.jellyfin.sdk.model.api.MediaStream
import org.jellyfin.sdk.model.api.MediaStreamType
import timber.log.Timber

/**
 * Manages pre-playback audio and subtitle track selection
 */
class PrePlaybackTrackSelector(private val context: Context) {
	private val prefs: SharedPreferences = context.getSharedPreferences("voidstream_track_selection", Context.MODE_PRIVATE)
	
	companion object {
		private const val PREF_AUDIO_STREAM_INDEX = "selected_audio_stream_index"
		private const val PREF_SUBTITLE_STREAM_INDEX = "selected_subtitle_stream_index"
		private const val PREF_ITEM_ID = "selected_for_item_id"
	}
	
	/**
	 * Get available audio tracks for an item
	 */
	fun getAudioTracks(item: BaseItemDto): List<MediaStream> {
		val mediaSource = getDefaultMediaSource(item) ?: return emptyList()
		return mediaSource.mediaStreams?.filter { it.type == MediaStreamType.AUDIO } ?: emptyList()
	}
	
	/**
	 * Get available subtitle tracks for an item
	 */
	fun getSubtitleTracks(item: BaseItemDto): List<MediaStream> {
		val mediaSource = getDefaultMediaSource(item) ?: return emptyList()
		val subtitles = mediaSource.mediaStreams?.filter { it.type == MediaStreamType.SUBTITLE } ?: emptyList()
		// Add a "None" option
		return subtitles
	}
	
	/**
	 * Get the default or first media source for an item
	 */
	private fun getDefaultMediaSource(item: BaseItemDto): MediaSourceInfo? {
		val sources = item.mediaSources
		if (sources.isNullOrEmpty()) return null
		
		// Try to find default source
		return sources.firstOrNull { it.id == item.id.toString() } ?: sources.firstOrNull()
	}
	
	/**
	 * Save selected audio track index for this item
	 */
	fun setSelectedAudioTrack(itemId: String, streamIndex: Int?) {
		prefs.edit().apply {
			putString(PREF_ITEM_ID, itemId)
			if (streamIndex != null) {
				putInt(PREF_AUDIO_STREAM_INDEX, streamIndex)
			} else {
				remove(PREF_AUDIO_STREAM_INDEX)
			}
			apply()
		}
		Timber.d("PrePlayback: Saved audio track $streamIndex for item $itemId")
	}
	
	/**
	 * Save selected subtitle track index for this item
	 */
	fun setSelectedSubtitleTrack(itemId: String, streamIndex: Int?) {
		prefs.edit().apply {
			putString(PREF_ITEM_ID, itemId)
			if (streamIndex != null) {
				putInt(PREF_SUBTITLE_STREAM_INDEX, streamIndex)
			} else {
				remove(PREF_SUBTITLE_STREAM_INDEX)
			}
			apply()
		}
		Timber.d("PrePlayback: Saved subtitle track $streamIndex for item $itemId")
	}
	
	/**
	 * Get selected audio track index for this item
	 */
	fun getSelectedAudioTrack(itemId: String): Int? {
		if (prefs.getString(PREF_ITEM_ID, null) != itemId) return null
		return if (prefs.contains(PREF_AUDIO_STREAM_INDEX)) {
			prefs.getInt(PREF_AUDIO_STREAM_INDEX, -1)
		} else null
	}
	
	/**
	 * Get selected subtitle track index for this item
	 */
	fun getSelectedSubtitleTrack(itemId: String): Int? {
		if (prefs.getString(PREF_ITEM_ID, null) != itemId) return null
		return if (prefs.contains(PREF_SUBTITLE_STREAM_INDEX)) {
			prefs.getInt(PREF_SUBTITLE_STREAM_INDEX, -1)
		} else null
	}
	
	/**
	 * Clear selections for current item
	 */
	fun clearSelections() {
		prefs.edit().clear().apply()
		Timber.d("PrePlayback: Cleared all track selections")
	}
	
	/**
	 * Get display name for an audio track
	 */
	fun getAudioTrackDisplayName(stream: MediaStream): String {
		val parts = mutableListOf<String>()
		
		// Language
		val language = stream.language
		if (!language.isNullOrEmpty()) {
			parts.add(language.uppercase())
		}
		
		// Codec
		val codec = stream.codec
		if (!codec.isNullOrEmpty()) {
			parts.add(codec.uppercase())
		}
		
		// Channels
		val channels = stream.channels
		if (channels != null && channels > 0) {
			parts.add("${channels}ch")
		}
		
		// Title if available
		val title = stream.title
		if (!title.isNullOrEmpty()) {
			parts.add(0, title)
		}
		
		return if (parts.isNotEmpty()) {
			parts.joinToString(" - ")
		} else {
			"Track ${stream.index ?: 0}"
		}
	}
	
	/**
	 * Get display name for a subtitle track
	 */
	fun getSubtitleTrackDisplayName(stream: MediaStream): String {
		val parts = mutableListOf<String>()
		
		// Language
		val language = stream.language
		if (!language.isNullOrEmpty()) {
			parts.add(language.uppercase())
		}
		
		// Title if available
		val title = stream.title
		if (!title.isNullOrEmpty()) {
			parts.add(title)
		}
		
		// Forced/SDH indicators
		if (stream.isForced == true) {
			parts.add("Forced")
		}
		if (stream.isHearingImpaired == true) {
			parts.add("SDH")
		}
		
		return if (parts.isNotEmpty()) {
			parts.joinToString(" - ")
		} else {
			"Subtitle ${stream.index ?: 0}"
		}
	}
}
