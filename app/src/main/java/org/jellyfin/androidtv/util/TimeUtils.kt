package org.jellyfin.androidtv.util

import android.content.Context
import org.jellyfin.androidtv.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

/**
 * Utility functions for time and date formatting.
 */
object TimeUtils {
	private const val MILLIS_PER_SEC = 1000
	private val MILLIS_PER_MIN = TimeUnit.MINUTES.toMillis(1)
	private val MILLIS_PER_HR = TimeUnit.HOURS.toMillis(1)
	
	private const val SECS_PER_MIN = 60
	private val SECS_PER_HR = TimeUnit.HOURS.toSeconds(1)
	
	private const val DURATION_TIME_FORMAT_NO_HOURS = "%d:%02d"
	private const val DURATION_TIME_FORMAT_WITH_HOURS = "%d:%02d:%02d"
	
	/**
	 * Convert seconds to milliseconds.
	 */
	@JvmStatic
	fun secondsToMillis(seconds: Double): Long {
		return (seconds * MILLIS_PER_SEC).roundToLong()
	}
	
	/**
	 * Formats time in milliseconds to hh:mm:ss string format.
	 *
	 * @param millis Time in milliseconds
	 * @return Formatted time
	 */
	@JvmStatic
	fun formatMillis(millis: Long): String {
		var remainingMillis = millis
		val hr = TimeUnit.MILLISECONDS.toHours(remainingMillis)
		remainingMillis %= MILLIS_PER_HR
		val min = TimeUnit.MILLISECONDS.toMinutes(remainingMillis)
		remainingMillis %= MILLIS_PER_MIN
		val sec = TimeUnit.MILLISECONDS.toSeconds(remainingMillis)
		
		return if (hr > 0) {
			DURATION_TIME_FORMAT_WITH_HOURS.format(hr, min, sec)
		} else {
			DURATION_TIME_FORMAT_NO_HOURS.format(min, sec)
		}
	}
	
	/**
	 * Format seconds into a human-readable string (e.g., "5 seconds", "2 minutes", "3 hours").
	 */
	@JvmStatic
	fun formatSeconds(context: Context, seconds: Int): String {
		return when {
			seconds < SECS_PER_MIN -> context.getQuantityString(R.plurals.seconds, seconds)
			seconds < SECS_PER_HR -> context.getQuantityString(R.plurals.minutes, seconds / SECS_PER_MIN)
			else -> context.getQuantityString(R.plurals.hours, (seconds / SECS_PER_HR).toInt())
		}
	}
	
	/**
	 * Get a friendly date string (e.g., "Today", "Tomorrow", "Monday", or a formatted date).
	 */
	@JvmStatic
	@JvmOverloads
	fun getFriendlyDate(
		context: Context,
		dateTime: LocalDateTime,
		relative: Boolean = false
	): String {
		val now = LocalDateTime.now()
		
		if (dateTime.year == now.year) {
			when {
				dateTime.dayOfYear == now.dayOfYear -> {
					return context.getString(R.string.lbl_today)
				}
				dateTime.dayOfYear == now.dayOfYear + 1 -> {
					return context.getString(R.string.lbl_tomorrow)
				}
				dateTime.dayOfYear < now.dayOfYear + 7 && dateTime.dayOfYear > now.dayOfYear -> {
					return dateTime.format(DateTimeFormatter.ofPattern("EE", context.locale))
				}
				relative -> {
					return context.getString(R.string.lbl_in_x_days, dateTime.dayOfYear - now.dayOfYear)
				}
			}
		}
		
		return context.getDateFormatter().format(dateTime)
	}
}
