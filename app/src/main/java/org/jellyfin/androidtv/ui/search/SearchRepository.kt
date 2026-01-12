package org.jellyfin.androidtv.ui.search

import org.jellyfin.androidtv.constant.QueryDefaults
import org.jellyfin.androidtv.data.repository.ItemRepository
import org.jellyfin.androidtv.util.apiclient.ioCallContent
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.personsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import org.jellyfin.sdk.model.api.request.GetPersonsRequest
import timber.log.Timber

interface SearchRepository {
	suspend fun search(
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>,
	): Result<List<BaseItemDto>>
}

class SearchRepositoryImpl(
	private val apiClient: ApiClient
) : SearchRepository {
	override suspend fun search(
		searchTerm: String,
		itemTypes: Collection<BaseItemKind>,
	): Result<List<BaseItemDto>> = try {
		if (itemTypes.size == 1 && itemTypes.first() == BaseItemKind.PERSON) {
			val request = GetPersonsRequest(
				searchTerm = searchTerm,
				limit = QueryDefaults.SEARCH_PAGE_SIZE,
				imageTypeLimit = 1,
				fields = ItemRepository.itemFields,
			)

			val result = apiClient.ioCallContent {
				personsApi.getPersons(request)
			}

			return Result.success(result.items)
		}

		var request = GetItemsRequest(
			searchTerm = searchTerm,
			limit = QueryDefaults.SEARCH_PAGE_SIZE,
			imageTypeLimit = 1,
			includeItemTypes = itemTypes,
			fields = ItemRepository.itemFields,
			recursive = true,
			enableTotalRecordCount = false,
		)

		// Special case for video row
		if (itemTypes.size == 1 && itemTypes.first() == BaseItemKind.VIDEO) {
			request = request.copy(
				mediaTypes = setOf(MediaType.VIDEO),
				includeItemTypes = null,
				excludeItemTypes = setOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.TV_CHANNEL)
			)
		}

		val result = apiClient.ioCallContent {
			itemsApi.getItems(request)
		}

		Result.success(result.items)
	} catch (e: ApiClientException) {
		Timber.e(e, "Failed to search for items")
		Result.failure(e)
	}
}
