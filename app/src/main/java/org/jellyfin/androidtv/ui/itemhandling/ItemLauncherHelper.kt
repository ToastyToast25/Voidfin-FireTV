@file:Suppress("DEPRECATION") // Legacy Response class for Java interop

package org.jellyfin.androidtv.ui.itemhandling

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.androidtv.util.apiclient.Response
import org.jellyfin.androidtv.util.sdk.ApiClientFactory
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.java.KoinJavaComponent
import timber.log.Timber
import java.util.UUID

object ItemLauncherHelper {
	private fun resolveApiClient(serverId: UUID?): ApiClient {
		val defaultApi by KoinJavaComponent.inject<ApiClient>(ApiClient::class.java)
		
		return if (serverId != null) {
			val apiClientFactory by KoinJavaComponent.inject<ApiClientFactory>(ApiClientFactory::class.java)
			apiClientFactory.getApiClientForServer(serverId) ?: defaultApi
		} else {
			defaultApi
		}
	}
	
	@JvmStatic
	fun getItem(itemId: UUID, callback: Response<BaseItemDto>) {
		getItem(itemId, null, callback)
	}

	@JvmStatic
	fun getItem(itemId: UUID, serverId: UUID?, callback: Response<BaseItemDto>) {
		ProcessLifecycleOwner.get().lifecycleScope.launch {
			val api = resolveApiClient(serverId)

			try {
				val response = withContext(Dispatchers.IO) {
					api.userLibraryApi.getItem(itemId = itemId).content
				}
				callback.onResponse(response)
			} catch (error: ApiClientException) {
				callback.onError(error)
			}
		}
	}
	
	suspend fun getItemBlocking(itemId: UUID, serverId: UUID? = null): BaseItemDto? {
		val api = resolveApiClient(serverId)

		return try {
			withContext(Dispatchers.IO) {
				api.userLibraryApi.getItem(itemId = itemId).content
			}
		} catch (error: ApiClientException) {
			Timber.e(error, "Error fetching item $itemId")
			null
		}
	}
}
