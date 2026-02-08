package org.jellyfin.androidtv.ui.startup

import android.Manifest
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import org.jellyfin.androidtv.BuildConfig
import org.jellyfin.androidtv.JellyfinApplication
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.SessionRepository
import org.jellyfin.androidtv.auth.repository.SessionRepositoryState
import org.jellyfin.androidtv.auth.repository.UserRepository
import org.jellyfin.androidtv.databinding.ActivityStartupBinding
import org.jellyfin.androidtv.preference.UserPreferences
import org.jellyfin.androidtv.ui.background.AppBackground
import org.jellyfin.androidtv.ui.browsing.MainActivity
import org.jellyfin.androidtv.ui.itemhandling.ItemLauncher
import org.jellyfin.androidtv.ui.navigation.Destinations
import org.jellyfin.androidtv.ui.navigation.NavigationRepository
import org.jellyfin.androidtv.ui.playback.MediaManager
import org.jellyfin.androidtv.data.service.UpdateCheckerService
import org.jellyfin.androidtv.ui.startup.fragment.ForceUpdateFragment
import org.jellyfin.androidtv.ui.startup.fragment.StoreUpdateFragment
import org.jellyfin.androidtv.ui.startup.fragment.WhatsNewFragment
import org.jellyfin.androidtv.ui.startup.fragment.SelectServerFragment
import org.jellyfin.androidtv.ui.startup.fragment.ServerFragment
import org.jellyfin.androidtv.ui.startup.fragment.SplashFragment
import org.jellyfin.androidtv.ui.startup.fragment.StartupToolbarFragment
import org.jellyfin.androidtv.util.applyTheme
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.UUID

class StartupActivity : FragmentActivity() {
	companion object {
		const val EXTRA_ITEM_ID = "ItemId"
		const val EXTRA_ITEM_IS_USER_VIEW = "ItemIsUserView"
		const val EXTRA_HIDE_SPLASH = "HideSplash"
	}

	private val startupViewModel: StartupViewModel by viewModel()
	private val api: ApiClient by inject()
	private val mediaManager: MediaManager by inject()
	private val sessionRepository: SessionRepository by inject()
	private val userRepository: UserRepository by inject()
	private val navigationRepository: NavigationRepository by inject()
	private val itemLauncher: ItemLauncher by inject()
	private val updateCheckerService: UpdateCheckerService? by lazy {
		if (!BuildConfig.IS_AMAZON_BUILD && !BuildConfig.IS_GOOGLE_PLAY_BUILD) {
			get<UpdateCheckerService>()
		} else {
			null
		}
	}
	private val userPreferences: UserPreferences by inject()
	private val issueReporterService: org.jellyfin.androidtv.data.service.IssueReporterService by inject()
	private val logCollector: org.jellyfin.androidtv.data.service.AppLogCollector = org.jellyfin.androidtv.data.service.AppLogCollector.instance

	private lateinit var binding: ActivityStartupBinding

	private val networkPermissionsRequester = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { grants ->
		val anyRejected = grants.any { !it.value }

		if (anyRejected) {
			// Permission denied, exit the app.
			Toast.makeText(this, R.string.no_network_permissions, Toast.LENGTH_LONG).show()
			finish()
		} else {
			onPermissionsGranted()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		applyTheme()

		super.onCreate(savedInstanceState)

		binding = ActivityStartupBinding.inflate(layoutInflater)
		binding.background.setContent { AppBackground() }
		binding.screensaver.isVisible = false
		setContentView(binding.root)

		if (!intent.getBooleanExtra(EXTRA_HIDE_SPLASH, false)) showSplash()

		// Ensure basic permissions
		networkPermissionsRequester.launch(arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE))
	}

	override fun onResume() {
		super.onResume()

		applyTheme()
	}

	private fun onPermissionsGranted() {
		lifecycleScope.launch {
			// OTA update features — only for sideloaded builds
			if (!BuildConfig.IS_AMAZON_BUILD && !BuildConfig.IS_GOOGLE_PLAY_BUILD) {
				// Show "What's New" if we just updated via OTA
				showWhatsNewIfPending()

				val updateBlocking = checkForForcedUpdate()
				if (updateBlocking) return@launch
			} else {
				// Store update detection — for Amazon & Google Play builds
				val storeUpdateBlocking = checkForStoreUpdate()
				if (storeUpdateBlocking) return@launch
			}

			// Check for pending crash report (store-compliant, opt-in only)
			val crashReportingEnabled = userPreferences[UserPreferences.crashReportingEnabled]
			if (crashReportingEnabled) {
				val hasPendingCrash = checkForPendingCrashReport()
				if (hasPendingCrash) return@launch
			}

			// No blocking update or crash, proceed with normal session flow
			startSessionFlow()
		}
	}

	private suspend fun showWhatsNewIfPending() {
		val whatsNew = updateCheckerService?.getPendingWhatsNew() ?: return
		val (version, notes) = whatsNew
		Timber.i("Showing What's New for version $version")

		suspendCancellableCoroutine { continuation ->
			supportFragmentManager.setFragmentResultListener("whats_new_done", this@StartupActivity) { _, _ ->
				continuation.resume(Unit)
			}
			supportFragmentManager.commit {
				replace(R.id.content_view, WhatsNewFragment.newInstance(version, notes))
			}
		}
	}

	private suspend fun checkForPendingCrashReport(): Boolean {
		return try {
			// Check if there's a crash log from previous session
			val crashLog = logCollector.getAndClearLastCrash()
			if (crashLog.isNullOrBlank()) return false

			Timber.i("Found pending crash report, showing dialog to user")

			// Show crash report dialog and wait for user decision
			suspendCancellableCoroutine { continuation ->
				supportFragmentManager.setFragmentResultListener("crash_report_done", this@StartupActivity) { _, result ->
					val sendReport = result.getBoolean("send_report", false)
					if (sendReport) {
						// Submit crash report via IssueReporterService
						lifecycleScope.launch {
							try {
								val submitResult = issueReporterService.submitIssue(
									category = org.jellyfin.androidtv.data.service.IssueReporterService.IssueCategory.APP_CRASH,
									description = "",  // Crash details are in the log
									updateVersion = BuildConfig.VERSION_NAME,
								)
								submitResult.fold(
									onSuccess = { issueNumber ->
										Timber.i("Crash report submitted successfully: #$issueNumber")
										Toast.makeText(this@StartupActivity, getString(R.string.crash_report_submitted_success, issueNumber), Toast.LENGTH_LONG).show()
									},
									onFailure = { err ->
										Timber.e(err, "Failed to submit crash report")
										Toast.makeText(this@StartupActivity, R.string.crash_report_submitted_failure, Toast.LENGTH_LONG).show()
									}
								)
							} catch (e: Exception) {
								Timber.e(e, "Error submitting crash report")
								Toast.makeText(this@StartupActivity, R.string.crash_report_submitted_failure, Toast.LENGTH_LONG).show()
							}
						}
					} else {
						Timber.i("User declined to send crash report")
					}
					continuation.resume(Unit)
				}
				supportFragmentManager.commit {
					replace(R.id.content_view, org.jellyfin.androidtv.ui.startup.fragment.CrashReportFragment.newInstance())
				}
			}
			true
		} catch (e: Exception) {
			Timber.e(e, "Failed to check for pending crash report, proceeding normally")
			false
		}
	}

	private suspend fun checkForForcedUpdate(): Boolean {
		return try {
			// Skip update check if no network
			if (updateCheckerService?.isNetworkAvailable() == false) {
				Timber.d("No network, skipping update check")
				return false
			}

			val betaEnabled = userPreferences[UserPreferences.betaUpdatesEnabled]
			val updateResult = withContext(Dispatchers.IO) {
				updateCheckerService?.checkForUpdate(includePrereleases = betaEnabled)
			}
			val updateInfo = updateResult?.getOrNull()
			if (updateInfo != null && updateInfo.isNewer) {
				// Optional update — don't block, just log
				if (!updateInfo.isForced) {
					Timber.i("Optional update available: ${updateInfo.version} (not blocking)")
					return false
				}

				Timber.i("Forced update required: current -> ${updateInfo.version}")

				// Try to get combined changelog if multiple versions behind
				val combinedNotes = try {
					withContext(Dispatchers.IO) { updateCheckerService?.getCombinedChangelog() }
				} catch (_: Exception) { null }

				val finalInfo = if (combinedNotes != null) {
					updateInfo.copy(releaseNotes = combinedNotes)
				} else {
					updateInfo
				}

				showForceUpdate(finalInfo)
				true
			} else {
				false
			}
		} catch (e: Exception) {
			Timber.e(e, "Failed to check for updates on startup, proceeding normally")
			false
		}
	}

	private suspend fun checkForStoreUpdate(): Boolean {
		return try {
			// Skip update check if no network
			if (updateCheckerService?.isNetworkAvailable() == false) {
				Timber.d("No network, skipping store update check")
				return false
			}

			val updateResult = withContext(Dispatchers.IO) {
				updateCheckerService?.checkForStoreUpdate(forceRefresh = false)
			}
			val updateInfo = updateResult?.getOrNull()

			if (updateInfo != null && updateInfo.isNewer) {
				Timber.i("Store update available: ${updateInfo.version}")

				val currentVersion = updateInfo.version
				val lastShownVersion = userPreferences[UserPreferences.storeUpdateLastShownVersion]
				val firstSeenTime = userPreferences[UserPreferences.storeUpdateFirstSeenTime]
				val now = System.currentTimeMillis()

				// Track first time seeing this version
				if (lastShownVersion != currentVersion || firstSeenTime == 0L) {
					userPreferences[UserPreferences.storeUpdateFirstSeenTime] = now
					userPreferences[UserPreferences.storeUpdateLastShownVersion] = currentVersion
				}

				// Calculate grace period (7 days)
				val gracePeriodMs = 7L * 24 * 60 * 60 * 1000
				val elapsed = now - userPreferences[UserPreferences.storeUpdateFirstSeenTime]
				val isForced = elapsed >= gracePeriodMs
				val daysRemaining = ((gracePeriodMs - elapsed) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)

				Timber.i("Store update: forced=$isForced, daysRemaining=$daysRemaining")

				// Determine store name
				val storeName = when {
					BuildConfig.IS_AMAZON_BUILD -> "Amazon Appstore"
					BuildConfig.IS_GOOGLE_PLAY_BUILD -> "Google Play Store"
					else -> "App Store"
				}

				// Show store update dialog
				suspendCancellableCoroutine { continuation ->
					supportFragmentManager.setFragmentResultListener("store_update_done", this@StartupActivity) { _, result ->
						val updateClicked = result.getBoolean("update_clicked", false)
						if (updateClicked) {
							updateCheckerService?.openAppStore()
						}
						continuation.resume(Unit)
					}
					supportFragmentManager.commit {
						replace(
							R.id.content_view,
							StoreUpdateFragment.newInstance(
								version = currentVersion,
								releaseNotes = updateInfo.releaseNotes,
								isForced = isForced,
								gracePeriodDays = daysRemaining,
								storeName = storeName
							)
						)
					}
				}

				// Return true if forced (blocks app), false if optional
				isForced
			} else {
				false
			}
		} catch (e: Exception) {
			Timber.e(e, "Failed to check for store updates, proceeding normally")
			false
		}
	}

	private fun startSessionFlow() = sessionRepository.state
		.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
		.filter { it == SessionRepositoryState.READY }
		.map { sessionRepository.currentSession.value }
		.distinctUntilChanged()
		.onEach { session ->
			if (session != null) {
				Timber.i("Found a session in the session repository, waiting for the currentUser in the application class.")

				showSplash()

				val currentUser = userRepository.currentUser.first { it != null }
				Timber.i("CurrentUser changed to ${currentUser?.id} while waiting for startup.")

				lifecycleScope.launch {
					openNextActivity()
				}
			} else {
				// Clear audio queue in case left over from last run
				mediaManager.clearAudioQueue()

				val server = startupViewModel.getLastServer()
				if (server != null) showServer(server.id)
				else showServerSelection()
			}
		}.launchIn(lifecycleScope)

	private suspend fun openNextActivity() {
		val itemId = when {
			intent.action == Intent.ACTION_VIEW && intent.data != null -> intent.data.toString()
			else -> intent.getStringExtra(EXTRA_ITEM_ID)
		}?.toUUIDOrNull()
		val itemIsUserView = intent.getBooleanExtra(EXTRA_ITEM_IS_USER_VIEW, false)

		Timber.i("Determining next activity (action=${intent.action}, itemId=$itemId, itemIsUserView=$itemIsUserView)")

		// Start session
		(application as? JellyfinApplication)?.onSessionStart()

		proceedToMainActivity(itemId, itemIsUserView)
	}

	private suspend fun proceedToMainActivity(itemId: UUID?, itemIsUserView: Boolean) {
		// Create destination
		val destination = when {
			// Search is requested
			intent.action === Intent.ACTION_SEARCH -> Destinations.search(
				query = intent.getStringExtra(SearchManager.QUERY)
			)
			// User view item is requested
			itemId != null && itemIsUserView -> runCatching {
				val item = withContext(Dispatchers.IO) {
					api.userLibraryApi.getItem(itemId = itemId).content
				}
				itemLauncher.getUserViewDestination(item)
			}.onFailure { throwable ->
				Timber.w(throwable, "Failed to retrieve item $itemId from server.")
			}.getOrNull()
			// Other item is requested
			itemId != null -> Destinations.itemDetails(itemId)
			// No destination requested, use default
			else -> null
		}

		navigationRepository.reset(destination, true)

		val intent = Intent(this, MainActivity::class.java)
		// Clear navigation history
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME)
		Timber.i("Opening next activity $intent")
		startActivity(intent)
		finishAfterTransition()
	}

	private fun showForceUpdate(updateInfo: UpdateCheckerService.UpdateInfo) {
		Timber.i("Showing forced update screen for version ${updateInfo.version}")
		supportFragmentManager.commit {
			replace(R.id.content_view, ForceUpdateFragment.newInstance(updateInfo))
		}
	}

	// Fragment switching
	private fun showSplash() {
		// Prevent progress bar flashing
		if (supportFragmentManager.findFragmentById(R.id.content_view) is SplashFragment) return

		supportFragmentManager.commit {
			replace<SplashFragment>(R.id.content_view)
		}
	}

	private fun showServer(id: UUID) = supportFragmentManager.commit {
		replace<StartupToolbarFragment>(R.id.content_view)
		add<ServerFragment>(
			R.id.content_view, null, bundleOf(
				ServerFragment.ARG_SERVER_ID to id.toString()
			)
		)
	}

	private fun showServerSelection() = supportFragmentManager.commit {
		replace<StartupToolbarFragment>(R.id.content_view)
		add<SelectServerFragment>(R.id.content_view)
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
	}
}
