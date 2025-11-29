package org.jellyfin.androidtv.ui.home.mediabar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.data.repository.ItemMutationRepository
import org.jellyfin.androidtv.preference.UserSettingPreferences
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.imageApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ImageType
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import timber.log.Timber
import java.util.UUID

class MediaBarSlideshowViewModel(
	private val api: ApiClient,
	private val userSettingPreferences: UserSettingPreferences,
	private val itemMutationRepository: ItemMutationRepository,
	private val userRepository: UserRepository,
) : ViewModel() {
	private fun getConfig() = MediaBarConfig(
		maxItems = userSettingPreferences[UserSettingPreferences.mediaBarItemCount].toIntOrNull() ?: 10
	)

	private val _state = MutableStateFlow<MediaBarState>(MediaBarState.Loading)
	val state: StateFlow<MediaBarState> = _state.asStateFlow()

	private val _playbackState = MutableStateFlow(SlideshowPlaybackState())
	val playbackState: StateFlow<SlideshowPlaybackState> = _playbackState.asStateFlow()

	private val _isFocused = MutableStateFlow(false)
	val isFocused: StateFlow<Boolean> = _isFocused.asStateFlow()

	private var items: List<MediaBarSlideItem> = emptyList()
	private var autoAdvanceJob: Job? = null
	private var currentUserId: UUID? = null
	private var isInitialLoad = true
	private var lastLoadTimestamp: Long = 0

	init {
		loadSlideshowItems()
		
		// Observe user changes and reload content when user switches
		userRepository.currentUser
			.filterNotNull()
			.onEach { user ->
				// Check if user has actually changed (not just initial load)
				if (currentUserId != null && currentUserId != user.id) {
					Timber.i("MediaBar: User switched from $currentUserId to ${user.id}, reloading content")
					reloadContent()
				}
				currentUserId = user.id
			}
			.launchIn(viewModelScope)
	}

	fun setFocused(focused: Boolean) {
		_isFocused.value = focused
		
		// When losing focus, stop auto-advance
		if (!focused) {
			autoAdvanceJob?.cancel()
		} else {
			// When gaining focus, restart auto-advance if not paused
			if (!_playbackState.value.isPaused) {
				resetAutoAdvanceTimer()
			}
		}
	}

	/**
	 * Load featured media items for the slideshow
	 */
	private fun loadSlideshowItems() {
		viewModelScope.launch {
		try {
			_state.value = MediaBarState.Loading
			val config = getConfig()

			// Fetch movies and shows randomly on IO dispatcher
			val (moviesResponse, showsResponse) = withContext(Dispatchers.IO) {
				val movies by api.itemsApi.getItems(
					includeItemTypes = setOf(BaseItemKind.MOVIE),
					recursive = true,
					sortBy = setOf(org.jellyfin.sdk.model.api.ItemSortBy.RANDOM),
					limit = config.maxItems * 2, // Get more items to filter from
						filters = setOf(ItemFilter.IS_NOT_FOLDER),
						fields = setOf(
							ItemFields.OVERVIEW,
							ItemFields.GENRES,
						),
						imageTypeLimit = 1,
						enableImageTypes = setOf(ImageType.BACKDROP, ImageType.LOGO),
					)

					val shows by api.itemsApi.getItems(
						includeItemTypes = setOf(BaseItemKind.SERIES),
						recursive = true,
						sortBy = setOf(org.jellyfin.sdk.model.api.ItemSortBy.RANDOM),
						limit = config.maxItems * 2, // Get more items to filter from
						filters = setOf(ItemFilter.IS_NOT_FOLDER),
						fields = setOf(
							ItemFields.OVERVIEW,
							ItemFields.GENRES,
						),
						imageTypeLimit = 1,
						enableImageTypes = setOf(ImageType.BACKDROP, ImageType.LOGO),
					)
					
					Pair(movies, shows)
				}

				val allItems = (moviesResponse.items.orEmpty() + showsResponse.items.orEmpty())
					.filter { it.backdropImageTags?.isNotEmpty() == true }
					.shuffled()
					.take(config.maxItems)

				items = allItems.map { item ->
					MediaBarSlideItem(
						itemId = item.id,
						title = item.name.orEmpty(),
						overview = item.overview,
						backdropUrl = item.backdropImageTags?.firstOrNull()?.let { tag ->
							api.imageApi.getItemImageUrl(
								itemId = item.id,
								imageType = ImageType.BACKDROP,
								tag = tag,
								maxWidth = 1920,
								quality = 90
							)
						},
						logoUrl = item.imageTags?.get(ImageType.LOGO)?.let { tag ->
							api.imageApi.getItemImageUrl(
								itemId = item.id,
								imageType = ImageType.LOGO,
								tag = tag,
								maxWidth = 800,
							)
						},
						rating = item.officialRating,
						year = item.productionYear,
						genres = item.genres.orEmpty().take(3),
						runtime = item.runTimeTicks?.let { ticks ->
							val minutes = (ticks / 600000000)
							"${minutes}m"
						},
						criticRating = item.criticRating?.toInt(),
						communityRating = item.communityRating,
					)
				}

				if (items.isNotEmpty()) {
					_state.value = MediaBarState.Ready(items)
					startAutoPlay()
					// Mark as loaded after successful initial load
					isInitialLoad = false
				} else {
					_state.value = MediaBarState.Error("No items found")
				}
			} catch (e: Exception) {
				Timber.e(e, "Failed to load slideshow items: ${e::class.simpleName} - ${e.message}")
				_state.value = MediaBarState.Error("Failed to load items: ${e::class.simpleName ?: "Unknown error"}")
			}
		}
	}

	/**
	 * Start automatic slideshow playback
	 */
	private fun startAutoPlay() {
		autoAdvanceJob?.cancel()
		
		// Only start auto-play if the media bar is focused
		if (!_isFocused.value) return
		
		val config = getConfig()
		autoAdvanceJob = viewModelScope.launch {
			delay(config.shuffleIntervalMs)
			if (!_playbackState.value.isPaused && !_playbackState.value.isTransitioning && _isFocused.value) {
				nextSlide()
			}
		}
	}
	
	/**
	 * Reset the auto-advance timer
	 */
	private fun resetAutoAdvanceTimer() {
		startAutoPlay()
	}

	/**
	 * Reload slideshow content (e.g., when user switches profiles)
	 */
	fun reloadContent() {
		Timber.d("MediaBar: Reloading slideshow content")
		// Cancel auto-advance
		autoAdvanceJob?.cancel()
		// Reset playback state
		_playbackState.value = SlideshowPlaybackState()
		// Reload items
		loadSlideshowItems()
	}

	/**
	 * Load content on first HomeFragment creation, ensuring fresh random items each app launch
	 */
	fun loadInitialContent() {
		if (isInitialLoad) {
			Timber.d("MediaBar: Loading fresh random items for new session")
			isInitialLoad = false
			lastLoadTimestamp = System.currentTimeMillis()
			loadSlideshowItems()
		} else {
			Timber.d("MediaBar: Using existing content from this session")
		}
	}
	
	/**
	 * Force reload with fresh random items (e.g., user manually requests refresh)
	 */
	fun forceRefresh() {
		Timber.d("MediaBar: Force refreshing with new random items")
		lastLoadTimestamp = System.currentTimeMillis()
		reloadContent()
	}

	/**
	 * Navigate to the next slide
	 */
	fun nextSlide() {
		if (_playbackState.value.isTransitioning) return

		val currentIndex = _playbackState.value.currentIndex
		val nextIndex = (currentIndex + 1) % items.size

		_playbackState.value = _playbackState.value.copy(
			currentIndex = nextIndex,
			isTransitioning = true
		)

		val config = getConfig()
		viewModelScope.launch {
			delay(config.fadeTransitionDurationMs)
			_playbackState.value = _playbackState.value.copy(isTransitioning = false)
			// Reset the auto-advance timer after manual or automatic navigation
			resetAutoAdvanceTimer()
		}
	}

	/**
	 * Navigate to the previous slide
	 */
	fun previousSlide() {
		if (_playbackState.value.isTransitioning) return

		val currentIndex = _playbackState.value.currentIndex
		val previousIndex = if (currentIndex == 0) items.size - 1 else currentIndex - 1

		_playbackState.value = _playbackState.value.copy(
			currentIndex = previousIndex,
			isTransitioning = true
		)

		val config = getConfig()
		viewModelScope.launch {
			delay(config.fadeTransitionDurationMs)
			_playbackState.value = _playbackState.value.copy(isTransitioning = false)
			// Reset the auto-advance timer after manual navigation
			resetAutoAdvanceTimer()
		}
	}

	/**
	 * Toggle pause/play state
	 */
	fun togglePause() {
		_playbackState.value = _playbackState.value.copy(
			isPaused = !_playbackState.value.isPaused
		)
	}
}
