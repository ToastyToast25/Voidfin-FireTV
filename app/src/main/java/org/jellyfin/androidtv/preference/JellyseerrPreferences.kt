package org.jellyfin.androidtv.preference

import android.content.Context
import org.jellyfin.androidtv.constant.JellyseerrFetchLimit
import org.jellyfin.preference.booleanPreference
import org.jellyfin.preference.enumPreference
import org.jellyfin.preference.store.SharedPreferenceStore
import org.jellyfin.preference.stringPreference

/**
 * Jellyseerr integration preferences
 * Stores Jellyseerr server configuration and user settings
 */
class JellyseerrPreferences(context: Context) : SharedPreferenceStore(
	sharedPreferences = context.getSharedPreferences("jellyseerr_prefs", Context.MODE_PRIVATE)
) {
	companion object {
		/**
		 * Whether Jellyseerr integration is enabled
		 */
		val enabled = booleanPreference("jellyseerr_enabled", false)

		/**
		 * Jellyseerr server URL (e.g., https://jellyseerr.example.com)
		 */
		val serverUrl = stringPreference("jellyseerr_server_url", "")

		/**
		 * Jellyfin password for Jellyseerr authentication (used for auto-re-login when cookies expire)
		 */
		val password = stringPreference("jellyseerr_password", "")

		/**
		 * Whether to show Jellyseerr in the main navigation
		 */
		val showInNavigation = booleanPreference("jellyseerr_show_in_navigation", true)

		/**
		 * Whether to show Jellyseerr button in the main toolbar
		 */
		val showInToolbar = booleanPreference("jellyseerr_show_in_toolbar", true)

		/**
		 * Whether to show request status in item details
		 */
		val showRequestStatus = booleanPreference("jellyseerr_show_request_status", true)

		/**
		 * Last time Jellyseerr connection was verified (timestamp)
		 */
		val lastVerifiedTime = stringPreference("jellyseerr_last_verified", "")

		/**
		 * Whether the last connection test was successful
		 */
		val lastConnectionSuccess = booleanPreference("jellyseerr_last_connection_success", false)

		/**
		 * Number of items to fetch per request (25, 50, or 75)
		 */
		val fetchLimit = enumPreference("jellyseerr_fetch_limit", JellyseerrFetchLimit.MEDIUM)

		/**
		 * Whether to block NSFW (adult) content from Jellyseerr results
		 */
		val blockNsfw = booleanPreference("jellyseerr_block_nsfw", true)

		/**
		 * Authentication method: "jellyfin" (cookie-based) or "local" (email/password with API key)
		 */
		val authMethod = stringPreference("jellyseerr_auth_method", "jellyfin")

		/**
		 * Whether to automatically regenerate API key after Jellyfin login for permanent auth
		 * If true, replaces 30-day cookie auth with permanent API key
		 */
		val autoGenerateApiKey = booleanPreference("jellyseerr_auto_generate_api_key", true)

		/**
		 * Email for local Jellyseerr authentication (only used if authMethod is "local")
		 */
		val localEmail = stringPreference("jellyseerr_local_email", "")

		/**
		 * Password for local Jellyseerr authentication (only used if authMethod is "local")
		 */
		val localPassword = stringPreference("jellyseerr_local_password", "")

		/**
		 * Stored API key (from local login or regenerated after Jellyfin login)
		 */
		val apiKey = stringPreference("jellyseerr_api_key", "")

		/**
		 * Last authenticated Jellyfin username (used to detect user changes and clear cookies)
		 */
		val lastJellyfinUser = stringPreference("jellyseerr_last_jellyfin_user", "")

		// Custom Request Profiles
		/**
		 * Default profile ID for HD movie requests (uses server default if empty/null)
		 */
		val hdMovieProfileId = stringPreference("jellyseerr_hd_movie_profile_id", "")

		/**
		 * Default profile ID for 4K movie requests (uses server default if empty/null)
		 */
		val fourKMovieProfileId = stringPreference("jellyseerr_4k_movie_profile_id", "")

		/**
		 * Default profile ID for HD TV requests (uses server default if empty/null)
		 */
		val hdTvProfileId = stringPreference("jellyseerr_hd_tv_profile_id", "")

		/**
		 * Default profile ID for 4K TV requests (uses server default if empty/null)
		 */
		val fourKTvProfileId = stringPreference("jellyseerr_4k_tv_profile_id", "")

		/**
		 * Default root folder ID for HD movie requests (uses server default if empty/null)
		 */
		val hdMovieRootFolderId = stringPreference("jellyseerr_hd_movie_root_folder_id", "")

		/**
		 * Default root folder ID for 4K movie requests (uses server default if empty/null)
		 */
		val fourKMovieRootFolderId = stringPreference("jellyseerr_4k_movie_root_folder_id", "")

		/**
		 * Default root folder ID for HD TV requests (uses server default if empty/null)
		 */
		val hdTvRootFolderId = stringPreference("jellyseerr_hd_tv_root_folder_id", "")

		/**
		 * Default root folder ID for 4K TV requests (uses server default if empty/null)
		 */
		val fourKTvRootFolderId = stringPreference("jellyseerr_4k_tv_root_folder_id", "")

		/**
		 * Default server ID for HD movie requests (uses server default if empty/null)
		 */
		val hdMovieServerId = stringPreference("jellyseerr_hd_movie_server_id", "")

		/**
		 * Default server ID for 4K movie requests (uses server default if empty/null)
		 */
		val fourKMovieServerId = stringPreference("jellyseerr_4k_movie_server_id", "")

		/**
		 * Default server ID for HD TV requests (uses server default if empty/null)
		 */
		val hdTvServerId = stringPreference("jellyseerr_hd_tv_server_id", "")

		/**
		 * Default server ID for 4K TV requests (uses server default if empty/null)
		 */
		val fourKTvServerId = stringPreference("jellyseerr_4k_tv_server_id", "")
	}
}
