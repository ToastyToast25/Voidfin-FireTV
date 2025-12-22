package org.jellyfin.androidtv.ui.preference.screen

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.data.repository.ParentalControlsRepository
import org.jellyfin.androidtv.ui.preference.dsl.OptionsFragment
import org.jellyfin.androidtv.ui.preference.dsl.checkbox
import org.jellyfin.androidtv.ui.preference.dsl.optionsScreen
import org.koin.android.ext.android.inject

class ParentalControlsPreferencesScreen : OptionsFragment() {
	private val parentalControlsRepository by inject<ParentalControlsRepository>()

	private val availableRatings = MutableStateFlow<List<String>>(emptyList())

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		// Load available ratings (uses cache if available)
		lifecycleScope.launch {
			val ratings = parentalControlsRepository.getAvailableRatings()
			if (ratings != availableRatings.value) {
				availableRatings.value = ratings
				rebuild() // Only rebuild once when ratings are loaded
			}
		}
	}

	override val screen by optionsScreen {
		setTitle(R.string.pref_parental_controls)

		category {
			setTitle(R.string.pref_parental_controls_category_ratings)

			val ratings = availableRatings.value

			if (ratings.isEmpty()) {
				// Show loading message
				checkbox {
					title = getString(R.string.pref_parental_controls_loading)
					enabled = false
					bind {
						get { false }
						set { }
						default { false }
					}
				}
			} else {
				ratings.forEach { rating ->
					checkbox {
						title = rating
						contentOn = getString(R.string.pref_parental_controls_rating_blocked)
						contentOff = getString(R.string.pref_parental_controls_rating_allowed)
						bind {
							// Read directly from repository for current state
							get { parentalControlsRepository.isRatingBlocked(rating) }
							set { isBlocked ->
								val currentBlocked = parentalControlsRepository.getBlockedRatings().toMutableSet()
								if (isBlocked) {
									currentBlocked.add(rating)
								} else {
									currentBlocked.remove(rating)
								}
								parentalControlsRepository.setBlockedRatings(currentBlocked)
							}
							default { false }
						}
					}
				}
			}
		}

		category {
			setTitle(R.string.pref_parental_controls_category_info)

			checkbox {
				title = getString(R.string.pref_parental_controls_info_description)
				enabled = false
				bind {
					get { false }
					set { }
					default { false }
				}
			}
		}
	}
}
