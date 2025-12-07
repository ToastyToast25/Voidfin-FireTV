package org.jellyfin.androidtv.ui.livetv

import org.jellyfin.androidtv.preference.SystemPreferences
import org.jellyfin.androidtv.util.Utils
import org.jellyfin.sdk.model.api.BaseItemDto
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Manages filtering options for the Live TV guide.
 * Handles saving and loading filter preferences for various program types.
 */
class GuideFilters : KoinComponent {
	private val systemPreferences: SystemPreferences by inject()
	
	var movies: Boolean = false
		set(value) {
			field = value
			systemPreferences[SystemPreferences.liveTvGuideFilterMovies] = value
		}
	
	var news: Boolean = false
		set(value) {
			field = value
			systemPreferences[SystemPreferences.liveTvGuideFilterNews] = value
		}
	
	var series: Boolean = false
		set(value) {
			field = value
			systemPreferences[SystemPreferences.liveTvGuideFilterSeries] = value
		}
	
	var kids: Boolean = false
		set(value) {
			field = value
			systemPreferences[SystemPreferences.liveTvGuideFilterKids] = value
		}
	
	var sports: Boolean = false
		set(value) {
			field = value
			systemPreferences[SystemPreferences.liveTvGuideFilterSports] = value
		}
	
	var premiere: Boolean = false
		set(value) {
			field = value
			systemPreferences[SystemPreferences.liveTvGuideFilterPremiere] = value
		}
	
	init {
		load()
	}
	
	/**
	 * Load filter settings from preferences.
	 */
	fun load() {
		movies = systemPreferences[SystemPreferences.liveTvGuideFilterMovies]
		news = systemPreferences[SystemPreferences.liveTvGuideFilterNews]
		series = systemPreferences[SystemPreferences.liveTvGuideFilterSeries]
		kids = systemPreferences[SystemPreferences.liveTvGuideFilterKids]
		sports = systemPreferences[SystemPreferences.liveTvGuideFilterSports]
		premiere = systemPreferences[SystemPreferences.liveTvGuideFilterPremiere]
	}
	
	/**
	 * Check if any filters are active.
	 */
	fun any(): Boolean = movies || news || series || kids || sports || premiere
	
	/**
	 * Check if a program passes the active filters.
	 */
	fun passesFilter(program: BaseItemDto): Boolean {
		if (!any()) return true
		
		if (movies && Utils.isTrue(program.isMovie)) {
			return !premiere || Utils.isTrue(program.isPremiere)
		}
		if (news && Utils.isTrue(program.isNews)) {
			return !premiere || Utils.isTrue(program.isPremiere) || Utils.isTrue(program.isLive) || !Utils.isTrue(program.isRepeat)
		}
		if (series && Utils.isTrue(program.isSeries)) {
			return !premiere || Utils.isTrue(program.isPremiere) || !Utils.isTrue(program.isRepeat)
		}
		if (kids && Utils.isTrue(program.isKids)) {
			return !premiere || Utils.isTrue(program.isPremiere)
		}
		if (sports && Utils.isTrue(program.isSports)) {
			return !premiere || Utils.isTrue(program.isPremiere) || Utils.isTrue(program.isLive)
		}
		if (!movies && !news && !series && !kids && !sports) {
			return premiere && (Utils.isTrue(program.isPremiere) || 
				(Utils.isTrue(program.isSeries) && !Utils.isTrue(program.isRepeat)) || 
				(Utils.isTrue(program.isSports) && Utils.isTrue(program.isLive)))
		}
		
		return false
	}
	
	/**
	 * Clear all filters.
	 */
	fun clear() {
		news = false
		series = false
		sports = false
		kids = false
		movies = false
		premiere = false
	}
	
	override fun toString(): String {
		return if (any()) {
			"Content filtered. Showing channels with ${getFilterString()}"
		} else {
			"Showing all programs "
		}
	}
	
	private fun getFilterString(): String {
		return buildString {
			if (movies) append("movies")
			if (news) append(getSeparator(this), "news")
			if (sports) append(getSeparator(this), "sports")
			if (series) append(getSeparator(this), "series")
			if (kids) append(getSeparator(this), "kids")
			if (premiere) append(getSeparator(this), "ONLY new")
		}
	}
	
	private fun getSeparator(current: CharSequence): String {
		return if (current.isEmpty()) "" else ", "
	}
}
