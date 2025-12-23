package org.jellyfin.androidtv.ui.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.IOException
import java.util.UUID

/**
 * Manages background theme music playback for media items (shows, movies)
 * Theme music plays when viewing item details and fades out when navigating away
 */
class ThemeMusicPlayer(
	private val context: Context
) : KoinComponent {
	private val api by inject<ApiClient>()
	private val sessionRepository by inject<SessionRepository>()
	private val userPreferences by inject<UserSettingPreferences>()
	
	private var mediaPlayer: MediaPlayer? = null
	private var currentItemId: UUID? = null
	private val coroutineScope = CoroutineScope(Dispatchers.Main)
	private var fadeJob: Job? = null
	private var delayedPlayJob: Job? = null
	
	companion object {
		private const val FADE_DURATION_MS = 2000L
		private const val FADE_STEP_MS = 50L
		private const val DEFAULT_VOLUME = 0.3f // 30% volume for theme music
		private const val HOME_ROW_PLAY_DELAY_MS = 800L // Delay before playing on home row focus
	}
	
	/**
	 * Check if theme music should play for this item
	 */
	private fun shouldPlayThemeMusic(item: BaseItemDto): Boolean {
		// Check user preference
		val isEnabled = userPreferences[UserSettingPreferences.themeMusicEnabled]
		Timber.d("shouldPlayThemeMusic: themeMusicEnabled=$isEnabled, itemType=${item.type}")
		if (!isEnabled) return false
		
		// Only play for certain item types (Series, Movie, Episode, Season)
		// Episodes will use their parent series' theme music
		val validType = item.type in listOf(
			BaseItemKind.SERIES,
			BaseItemKind.MOVIE,
			BaseItemKind.SEASON,
			BaseItemKind.EPISODE
		)
		Timber.d("shouldPlayThemeMusic: validType=$validType")
		return validType
	}
	
	/**
	 * Start playing theme music for an item (used in detail pages)
	 */
	fun playThemeMusicForItem(item: BaseItemDto) {
		Timber.d("playThemeMusicForItem called for: ${item.name} (${item.type})")
		
		// Don't restart if already playing for this item
		if (currentItemId == item.id && mediaPlayer?.isPlaying == true) {
			Timber.d("Already playing theme music for this item, skipping")
			return
		}
		
		// Cancel any delayed play job
		delayedPlayJob?.cancel()
		
		// Stop any currently playing theme music
		stop()
		
		if (!shouldPlayThemeMusic(item)) {
			Timber.d("shouldPlayThemeMusic returned false for ${item.name}")
			return
		}
		
		Timber.d("Starting to fetch theme music for ${item.name}")
		currentItemId = item.id
		
		// For episodes, use the series ID to get theme music from the parent series
		val themeItemId = if (item.type == BaseItemKind.EPISODE && item.seriesId != null) {
			Timber.d("Episode detected, using seriesId ${item.seriesId} for theme music lookup")
			item.seriesId!!
		} else {
			item.id
		}
		
		// Fetch theme songs in background
		coroutineScope.launch(Dispatchers.IO) {
			try {
				// Get theme songs for this item using the official API
				val userId = sessionRepository.currentSession.value?.userId ?: run {
					Timber.w("No active session, cannot load theme music")
					return@launch
				}
				
				Timber.d("Fetching theme media for item $themeItemId")
				val themeMediaResponse = api.libraryApi.getThemeMedia(
					userId = userId,
					itemId = themeItemId,
					inheritFromParent = true
				)
				
				val themeSongsResult = themeMediaResponse.content.themeSongsResult
				val themeSongs = themeSongsResult?.items
				Timber.d("Theme songs result: ${themeSongs?.size ?: 0} songs found")
				
				if (themeSongs.isNullOrEmpty()) {
					Timber.d("No theme songs found for item ${item.name}")
					return@launch
				}
				
				// Pick a random theme song
				val themeSong = themeSongs.random()
				val audioUrl = buildThemeSongUrl(themeSong.id)
				Timber.d("Starting playback of theme song: ${themeSong.name}, URL: $audioUrl")
				
				// Initialize and start playback on main thread
				coroutineScope.launch(Dispatchers.Main) {
					startPlayback(audioUrl)
				}
			} catch (e: Exception) {
				Timber.e(e, "Failed to load theme music for item ${item.name}")
			}
		}
	}
	
	/**
	 * Build the audio URL for a theme song
	 */
	private fun buildThemeSongUrl(themeItemId: UUID): String {
		// Build direct stream URL for the theme song
		val baseUrl = api.baseUrl ?: throw IllegalStateException("API base URL is null")
		val token = api.accessToken ?: throw IllegalStateException("API token is null")
		
		// Use static stream with low bitrate for background music
		return "$baseUrl/Audio/$themeItemId/stream?static=true&audioCodec=mp3&audioBitrate=128000&api_key=$token"
	}
	
	/**
	 * Initialize MediaPlayer and start playback with fade in
	 */
	private fun startPlayback(audioUrl: String) {
		Timber.d("startPlayback called with URL: $audioUrl")
		try {
			mediaPlayer = MediaPlayer().apply {
				setAudioAttributes(
					AudioAttributes.Builder()
						.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
						.setUsage(AudioAttributes.USAGE_MEDIA)
						.build()
				)
				
				setDataSource(audioUrl)
				isLooping = true
				setVolume(0f, 0f) // Start silent for fade in
				
				setOnPreparedListener {
					Timber.d("MediaPlayer prepared, starting playback")
					start()
					fadeIn()
				}
				
				setOnErrorListener { _, what, extra ->
					Timber.e("MediaPlayer error: what=$what, extra=$extra")
					stop()
					true
				}
				
				prepareAsync()
			}
			
			Timber.d("MediaPlayer prepareAsync called")
		} catch (e: IOException) {
			Timber.e(e, "Failed to start theme music playback")
			stop()
		}
	}
	
	/**
	 * Fade in the theme music volume
	 */
	private fun fadeIn() {
		fadeJob?.cancel()
		fadeJob = coroutineScope.launch {
			// Get user's preferred volume (0-100) and convert to 0.0-1.0 range
			val volumePercent = userPreferences[UserSettingPreferences.themeMusicVolume]
			val targetVolume = volumePercent / 100f
			
			val steps = (FADE_DURATION_MS / FADE_STEP_MS).toInt()
			for (i in 0..steps) {
				val volume = (i.toFloat() / steps) * targetVolume
				mediaPlayer?.setVolume(volume, volume)
				delay(FADE_STEP_MS)
			}
		}
	}
	
	/**
	 * Play theme music on home row item focus with a delay
	 * This is used when browsing home rows - only plays if user lingers on an item
	 */
	fun playThemeMusicOnFocusDelayed(item: BaseItemDto) {
		// Check if user has enabled this feature
		if (!userPreferences[UserSettingPreferences.themeMusicOnHomeRows]) return
		
		// Cancel any existing delayed play job
		delayedPlayJob?.cancel()
		
		// Don't schedule if this item is already playing
		if (currentItemId == item.id && mediaPlayer?.isPlaying == true) {
			return
		}
		
		// Schedule theme music playback after delay
		delayedPlayJob = coroutineScope.launch {
			delay(HOME_ROW_PLAY_DELAY_MS)
			playThemeMusicForItem(item)
		}
	}
	
	/**
	 * Cancel delayed playback (called when user moves to another item quickly)
	 */
	fun cancelDelayedPlay() {
		delayedPlayJob?.cancel()
	}
	
	/**
	 * Fade out and stop theme music
	 */
	fun fadeOutAndStop() {
		val player = mediaPlayer ?: return // Nothing to fade out
		
		fadeJob?.cancel()
		fadeJob = coroutineScope.launch {
			// Get user's preferred volume (0-100) and convert to 0.0-1.0 range
			val volumePercent = userPreferences[UserSettingPreferences.themeMusicVolume]
			val currentVolume = volumePercent / 100f
			
			val steps = (FADE_DURATION_MS / FADE_STEP_MS).toInt()
			
			for (i in steps downTo 0) {
				if (mediaPlayer == null) break // Player was stopped elsewhere
				val volume = (i.toFloat() / steps) * currentVolume
				player.setVolume(volume, volume)
				delay(FADE_STEP_MS)
			}
			
			stop()
		}
	}
	
	/**
	 * Immediately stop theme music playback
	 */
	fun stop() {
		delayedPlayJob?.cancel()
		fadeJob?.cancel()
		mediaPlayer?.apply {
			try {
				if (isPlaying) stop()
				reset()
			} catch (e: IllegalStateException) {
				Timber.w("MediaPlayer already released")
			} finally {
				release()
			}
		}
		mediaPlayer = null
		currentItemId = null
	}
	
	/**
	 * Check if theme music is currently playing
	 */
	fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true
}
