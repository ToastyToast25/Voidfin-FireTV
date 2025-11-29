package org.jellyfin.androidtv.ui.preference.category

import android.os.Build
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.ui.preference.dsl.OptionsScreen
import org.jellyfin.androidtv.ui.preference.dsl.action
import org.jellyfin.androidtv.ui.preference.dsl.link
import org.jellyfin.androidtv.ui.preference.screen.LicensesScreen

fun OptionsScreen.aboutCategory() = category {
	setTitle(R.string.pref_about_title)

	action {
		setTitle(R.string.pref_donate_title)
		setContent(R.string.pref_donate_description)
		icon = R.drawable.ic_heart
		onActivate = {
			showDonateDialog(this@category.context)
		}
	}

	link {
		// Hardcoded strings for troubleshooting purposes
		title = "Moonfin app version"
		content = "moonfin-androidtv ${BuildConfig.VERSION_NAME} ${BuildConfig.BUILD_TYPE}"
		icon = R.drawable.ic_moonfin
	}

	link {
		// HReference to base Jellyfin app this build is based on
		title = "Base Jellyfin app version"
		content = "jellyfin-androidtv 0.19.4"
		icon = R.drawable.ic_jellyfin
	}


	link {
		setTitle(R.string.pref_device_model)
		content = "${Build.MANUFACTURER} ${Build.MODEL}"
		icon = R.drawable.ic_tv
	}

	link {
		setTitle(R.string.licenses_link)
		setContent(R.string.licenses_link_description)
		icon = R.drawable.ic_guide
		withFragment<LicensesScreen>()
	}
}
