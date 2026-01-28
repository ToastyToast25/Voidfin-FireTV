package org.jellyfin.androidtv.ui.browsing

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.koin.android.ext.android.inject
import timber.log.Timber

class FolderViewFragment : EnhancedBrowseFragment() {
	private val api by inject<ApiClient>()

	init {
		showViews = false
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		mTitle?.text = getString(R.string.lbl_folders)
	}

	override fun setupQueries(rowLoader: RowLoader) {
		lifecycleScope.launch {
			try {
				val response by api.itemsApi.getItems(
					includeItemTypes = setOf(BaseItemKind.FOLDER, BaseItemKind.COLLECTION_FOLDER),
					sortBy = setOf(ItemSortBy.SORT_NAME),
					sortOrder = setOf(SortOrder.ASCENDING),
					fields = ItemRepository.itemFields,
					recursive = false,
				)

				val rootFolders = response.items.orEmpty()
				Timber.d("Found ${rootFolders.size} root folders")

				if (rootFolders.isEmpty()) {
					mRows.add(BrowseRowDef(getString(R.string.msg_no_folders), GetItemsRequest(), 0))
				} else {
					for (folder in rootFolders) {
						val folderRequest = GetItemsRequest(
							parentId = folder.id,
							fields = ItemRepository.itemFields,
							sortBy = setOf(ItemSortBy.SORT_NAME),
							sortOrder = setOf(SortOrder.ASCENDING),
						)
						mRows.add(BrowseRowDef(folder.name ?: "Folder", folderRequest, 50))
					}
				}

				rowLoader.loadRows(mRows)
			} catch (e: Exception) {
				Timber.e(e, "Failed to load folders")
				mRows.add(BrowseRowDef(getString(R.string.msg_error_loading_data), GetItemsRequest(), 0))
				rowLoader.loadRows(mRows)
			}
		}
	}
}
