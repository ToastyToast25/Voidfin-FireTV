package org.jellyfin.androidtv.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import timber.log.Timber

/**
 * Centralized manager for user feedback (toasts, notifications, etc.).
 * Provides consistent user messaging throughout the application with proper logging.
 */
class UserFeedbackManager(private val context: Context) {
	
	/**
	 * Display a short informational message to the user.
	 * @param message The message to display
	 */
	fun showMessage(message: String) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
		Timber.d("User message: $message")
	}

	/**
	 * Display a short informational message to the user using a string resource.
	 * @param messageRes The string resource ID
	 */
	fun showMessage(@StringRes messageRes: Int) {
		showMessage(context.getString(messageRes))
	}

	/**
	 * Display a longer informational message to the user.
	 * @param message The message to display
	 */
	fun showLongMessage(message: String) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show()
		Timber.d("User message (long): $message")
	}

	/**
	 * Display a longer informational message to the user using a string resource.
	 * @param messageRes The string resource ID
	 */
	fun showLongMessage(@StringRes messageRes: Int) {
		showLongMessage(context.getString(messageRes))
	}

	/**
	 * Display an error message to the user and log the error.
	 * @param message The error message to display
	 * @param error Optional throwable to log for debugging
	 */
	fun showError(message: String, error: Throwable? = null) {
		Toast.makeText(context, message, Toast.LENGTH_LONG).show()
		if (error != null) {
			Timber.e(error, "Error: $message")
		} else {
			Timber.e("Error: $message")
		}
	}

	/**
	 * Display an error message to the user using a string resource.
	 * @param messageRes The string resource ID
	 * @param error Optional throwable to log for debugging
	 */
	fun showError(@StringRes messageRes: Int, error: Throwable? = null) {
		showError(context.getString(messageRes), error)
	}

	/**
	 * Display a success message to the user.
	 * @param message The success message to display
	 */
	fun showSuccess(message: String) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
		Timber.i("Success: $message")
	}

	/**
	 * Display a success message to the user using a string resource.
	 * @param messageRes The string resource ID
	 */
	fun showSuccess(@StringRes messageRes: Int) {
		showSuccess(context.getString(messageRes))
	}
}

/**
 * Extension function to easily create a UserFeedbackManager from any Context.
 */
fun Context.getUserFeedbackManager(): UserFeedbackManager = UserFeedbackManager(this)
