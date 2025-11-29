package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.Row
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jellyfin.androidtv.ui.home.mediabar.MediaBarSlideshowViewModel
import org.jellyfin.androidtv.ui.home.mediabar.MediaBarState
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter

/**
 * Home row for the Media Bar slideshow.
 * This creates a full-width row with Compose content for the slideshow.
 */
class HomeFragmentMediaBarRow(
	lifecycleScope: LifecycleCoroutineScope,
	private val viewModel: MediaBarSlideshowViewModel
) : HomeFragmentRow {
	private var row: MediaBarRow? = null
	private var rowsAdapter: MutableObjectAdapter<Row>? = null
	private var rowAdded = false

	init {
		// Observe state to show/hide the row
		viewModel.state.onEach { state ->
			val isEmpty = when (state) {
				is MediaBarState.Ready -> state.items.isEmpty()
				is MediaBarState.Error -> true
				else -> false // Keep row visible during Loading
			}
			update(isEmpty)
		}.launchIn(lifecycleScope)
	}

	private fun update(empty: Boolean) {
		if (rowsAdapter == null) return

		if (empty && rowAdded) {
			row?.let { rowsAdapter?.remove(it) }
			rowAdded = false
		}

		if (!empty && !rowAdded) {
			// Create the row with current slides
			val items = (viewModel.state.value as? MediaBarState.Ready)?.items ?: emptyList()
			row = MediaBarRow(items)
			row?.let { rowsAdapter?.add(0, it) }
			rowAdded = true
		}
	}

	override fun addToRowsAdapter(
		context: Context,
		cardPresenter: CardPresenter,
		rowsAdapter: MutableObjectAdapter<Row>
	) {
		this.rowsAdapter = rowsAdapter
		val isEmpty = when (val state = viewModel.state.value) {
			is MediaBarState.Ready -> state.items.isEmpty()
			is MediaBarState.Error -> true
			else -> false // Keep row visible during Loading
		}
		update(isEmpty)
	}
}
