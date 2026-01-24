package org.jellyfin.androidtv.ui.home

import android.content.Context
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.Row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.itemhandling.BaseItemDtoBaseRowItem
import org.jellyfin.androidtv.ui.presentation.CardPresenter
import org.jellyfin.androidtv.ui.presentation.MutableObjectAdapter
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.androidtv.data.repository.ItemRepository
import timber.log.Timber

class HomeFragmentPlaylistsRow(
	private val api: ApiClient,
) : HomeFragmentRow {
	private var row: ListRow? = null
	private var listRowAdapter: MutableObjectAdapter<Any>? = null

	override fun addToRowsAdapter(
		context: Context,
		cardPresenter: CardPresenter,
		rowsAdapter: MutableObjectAdapter<Row>
	) {
		listRowAdapter = MutableObjectAdapter(cardPresenter)
		val header = HeaderItem(context.getString(R.string.lbl_playlists))
		row = ListRow(header, listRowAdapter!!)
		rowsAdapter.add(row!!)

		CoroutineScope(Dispatchers.Main).launch {
			loadPlaylistItems(listRowAdapter!!)
		}
	}

	fun refresh() {
		listRowAdapter?.let { adapter ->
			CoroutineScope(Dispatchers.Main).launch {
				adapter.clear()
				loadPlaylistItems(adapter)
			}
		}
	}

	private suspend fun loadPlaylistItems(adapter: MutableObjectAdapter<Any>) {
		try {
			val items = withContext(Dispatchers.IO) {
				api.itemsApi.getItems(
					includeItemTypes = setOf(BaseItemKind.PLAYLIST),
					recursive = true,
					sortBy = setOf(ItemSortBy.DATE_CREATED),
					sortOrder = setOf(SortOrder.DESCENDING),
					fields = ItemRepository.itemFields,
					imageTypeLimit = 1,
					limit = 50,
				).content.items
			}
			
			items.forEach { item ->
				adapter.add(BaseItemDtoBaseRowItem(item))
			}
		} catch (e: Exception) {
			Timber.e(e, "Failed to load playlists")
		}
	}
}
