package org.jellyfin.androidtv.ui.jellyseerr

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.leanback.widget.Presenter
import coil3.load
import coil3.request.crossfade
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrNetworkDto
import org.jellyfin.androidtv.data.service.jellyseerr.JellyseerrStudioDto
import timber.log.Timber

/**
 * Presenter for network and studio cards with logo display
 * Uses pre-filtered duotone URLs from Seerr for consistent white appearance
 */
class NetworkStudioCardPresenter : Presenter() {
	private val CARD_WIDTH = 280
	private val CARD_HEIGHT = 140

	override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.jellyseerr_network_studio_card, parent, false) as FrameLayout

		return ViewHolder(view)
	}

	override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
		val container = viewHolder.view as FrameLayout
		val logoView = container.findViewById<ImageView>(R.id.logo_image)

		val layoutParams = container.layoutParams
		layoutParams.width = CARD_WIDTH
		layoutParams.height = CARD_HEIGHT
		container.layoutParams = layoutParams

		when (item) {
			is JellyseerrNetworkDto -> {
				Timber.d("NetworkStudioCardPresenter: Binding network ${item.name} with logo ${item.logoPath}")
				loadLogo(logoView, item.logoPath, item.name)
			}
			is JellyseerrStudioDto -> {
				Timber.d("NetworkStudioCardPresenter: Binding studio ${item.name} with logo ${item.logoPath}")
				loadLogo(logoView, item.logoPath, item.name)
			}
			else -> {
				Timber.w("NetworkStudioCardPresenter: Unknown item type: ${item?.javaClass?.simpleName}")
				logoView.setImageDrawable(null)
			}
		}
	}

	private fun loadLogo(logoView: ImageView, logoUrl: String?, name: String) {
		if (!logoUrl.isNullOrEmpty()) {
			logoView.load(logoUrl) {
				crossfade(true)
				listener(
					onSuccess = { _, _ ->
						Timber.d("NetworkStudioCardPresenter: Successfully loaded logo for $name")
					},
					onError = { _, result ->
						Timber.e("NetworkStudioCardPresenter: Failed to load logo for $name: ${result.throwable.message}")
					}
				)
			}
		} else {
			Timber.w("NetworkStudioCardPresenter: No logo URL for $name")
			logoView.setImageDrawable(null)
		}
	}

	override fun onUnbindViewHolder(viewHolder: ViewHolder) {
		val container = viewHolder.view as FrameLayout
		val logoView = container.findViewById<ImageView>(R.id.logo_image)
		logoView.setImageDrawable(null)
	}
}
