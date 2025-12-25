package org.jellyfin.androidtv.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.jellyfin.androidtv.R
import kotlin.random.Random

/**
 * A custom view that renders summer effects with bouncing beach balls
 * and pulsing suns.
 * 
 * Performance optimized for Android TV / Fire TV devices:
 * - Uses cached bitmap rendering from vector drawables
 * - Reduced element count for low-powered devices
 * - Pre-calculated sine table for bounce calculations
 */
class SummerView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	private enum class BeachBallState {
		WAITING, BOUNCING, FADING, DONE
	}

	private data class BeachBall(
		var x: Float,
		var y: Float,
		val baseY: Float,
		val targetX: Float,
		val speed: Float,
		val size: Float,
		var state: BeachBallState,
		var alpha: Int,
		var waitTimer: Int,
		var bounceIndex: Int,
		val bounceIndexSpeed: Int,
		val bounceAmplitude: Float,
		val fromLeft: Boolean
	)

	private enum class SunState {
		WAITING, PULSING_IN, PULSING_OUT, FADING, DONE
	}

	private data class Sun(
		val x: Float,
		val y: Float,
		val size: Float,
		var state: SunState,
		var alpha: Int,
		var waitTimer: Int,
		var scale: Float,
		var pulseCount: Int
	)

	private enum class BeachUmbrellaState {
		WAITING, RISING, SETTLING, FADING, DONE
	}

	private data class BeachUmbrella(
		var x: Float,
		var y: Float,
		var velocity: Float,
		val size: Float,
		var state: BeachUmbrellaState,
		var alpha: Int,
		val groundY: Float,
		var waitTimer: Int = 0
	)

	companion object {
		private const val SINE_TABLE_SIZE = 360
		private val sineTable = FloatArray(SINE_TABLE_SIZE) { i ->
			kotlin.math.sin(i * Math.PI / 180.0).toFloat()
		}
	}

	private val beachBalls = mutableListOf<BeachBall>()
	private val suns = mutableListOf<Sun>()
	private val beachUmbrellas = mutableListOf<BeachUmbrella>()

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
	
	// Cached bitmaps for efficient drawing
	private var sunBitmap: Bitmap? = null
	private var beachBallBitmap: Bitmap? = null
	private var umbrellaBitmap: Bitmap? = null
	private val bitmapCache = mutableMapOf<Int, Bitmap>()

	private val handler = Handler(Looper.getMainLooper())
	private var animationRunnable: Runnable? = null
	private var isActive = false
	private var beachBallSpawnTimer = 0
	private var sunSpawnTimer = 0
	private var umbrellaSpawnTimer = 0
	private val beachBallSpawnInterval = 250
	private val sunSpawnInterval = 100
	private val umbrellaSpawnInterval = 350
	private var frameCount = 0

	// Particle settings
	private val beachBallCount = 2
	private val beachBallSize = 50f
	private val beachBallSpeed = 2f

	private val maxSuns = 2
	private val sunSize = 60f

	private val umbrellaCount = 3
	private val umbrellaSize = 60f
	private val umbrellaRiseSpeed = -3f

	init {
		isClickable = false
		isFocusable = false
		loadBitmaps()
	}
	
	private fun loadBitmaps() {
		ContextCompat.getDrawable(context, R.drawable.seasonal_sun)?.let { drawable ->
			sunBitmap = drawable.toBitmap(64, 64)
		}
		ContextCompat.getDrawable(context, R.drawable.seasonal_beach_ball)?.let { drawable ->
			beachBallBitmap = drawable.toBitmap(64, 64)
		}
		ContextCompat.getDrawable(context, R.drawable.seasonal_beach_umbrella)?.let { drawable ->
			umbrellaBitmap = drawable.toBitmap(64, 64)
		}
	}
	
	private fun getScaledBitmap(source: Bitmap?, size: Int): Bitmap? {
		source ?: return null
		return bitmapCache.getOrPut(System.identityHashCode(source) * 1000 + size) {
			Bitmap.createScaledBitmap(source, size, size, true)
		}
	}

	fun startEffect() {
		if (isActive) return
		isActive = true
		startAnimation()
	}

	fun stopEffect() {
		if (!isActive) return
		isActive = false

		animationRunnable?.let { handler.removeCallbacks(it) }
		animationRunnable = null
		beachBalls.clear()
		suns.clear()
		beachUmbrellas.clear()
		beachBallSpawnTimer = 0
		sunSpawnTimer = 0
		umbrellaSpawnTimer = 0
		frameCount = 0
		invalidate()
	}

	private fun startAnimation() {
		animationRunnable?.let { handler.removeCallbacks(it) }

		animationRunnable = object : Runnable {
			override fun run() {
				if (!isActive) return
				
				frameCount++
				updateBeachBalls()
				// Stagger updates to reduce per-frame work
				if (frameCount % 2 == 0) {
					updateSuns()
				}
				if (frameCount % 3 == 0) {
					updateBeachUmbrellas()
				}
				invalidate()
				
				// Target ~30fps
				handler.postDelayed(this, 33L)
			}
		}
		handler.post(animationRunnable!!)
	}

	private fun updateBeachBalls() {
		if (width <= 0 || height <= 0) return

		beachBallSpawnTimer++
		if (beachBallSpawnTimer >= beachBallSpawnInterval && beachBalls.none { it.state != BeachBallState.DONE }) {
			beachBallSpawnTimer = 0
			spawnBeachBalls()
		}

		val iterator = beachBalls.iterator()
		while (iterator.hasNext()) {
			val ball = iterator.next()

			when (ball.state) {
				BeachBallState.WAITING -> {
					ball.waitTimer--
					if (ball.waitTimer <= 0) {
						ball.state = BeachBallState.BOUNCING
					}
				}
				BeachBallState.BOUNCING -> {
					if (ball.fromLeft) {
						ball.x += ball.speed
					} else {
						ball.x -= ball.speed
					}

					ball.bounceIndex = (ball.bounceIndex + ball.bounceIndexSpeed) % SINE_TABLE_SIZE
					ball.y = ball.baseY + sineTable[ball.bounceIndex] * ball.bounceAmplitude * 0.3f

					val reachedEnd = if (ball.fromLeft) ball.x > width + ball.size else ball.x < -ball.size
					if (reachedEnd) {
						ball.state = BeachBallState.FADING
					}
				}
				BeachBallState.FADING -> {
					ball.alpha -= 15
					if (ball.alpha <= 0) {
						ball.state = BeachBallState.DONE
					}
				}
				BeachBallState.DONE -> {
					iterator.remove()
				}
			}
		}
	}

	private fun spawnBeachBalls() {
		if (width <= 0 || height <= 0) return

		val usableHeight = height * 0.5f
		val topMargin = height * 0.3f
		val zoneHeight = usableHeight / beachBallCount

		repeat(beachBallCount) { i ->
			val fromLeft = Random.nextBoolean()
			val startX = if (fromLeft) -beachBallSize else width + beachBallSize
			val baseY = topMargin + (zoneHeight * i) + (zoneHeight * 0.3f) + Random.nextFloat() * (zoneHeight * 0.4f)

			beachBalls.add(
				BeachBall(
					x = startX,
					y = baseY,
					baseY = baseY,
					targetX = if (fromLeft) width + beachBallSize else -beachBallSize,
					speed = beachBallSpeed + Random.nextFloat() * 0.8f,
					size = beachBallSize + Random.nextFloat() * 8f,
					state = BeachBallState.WAITING,
					alpha = 255,
					waitTimer = i * 40 + Random.nextInt(15, 50),
					bounceIndex = Random.nextInt(SINE_TABLE_SIZE),
					bounceIndexSpeed = Random.nextInt(3, 7),
					bounceAmplitude = 35f + Random.nextFloat() * 15f,
					fromLeft = fromLeft
				)
			)
		}
	}

	private fun updateSuns() {
		if (width <= 0 || height <= 0) return

		sunSpawnTimer++
		val activeSuns = suns.count { it.state != SunState.DONE }
		if (sunSpawnTimer >= sunSpawnInterval && activeSuns < maxSuns) {
			sunSpawnTimer = 0
			spawnSun()
		}

		val iterator = suns.iterator()
		while (iterator.hasNext()) {
			val sun = iterator.next()

			when (sun.state) {
				SunState.WAITING -> {
					sun.waitTimer--
					if (sun.waitTimer <= 0) {
						sun.state = SunState.PULSING_IN
					}
				}
				SunState.PULSING_IN -> {
					sun.scale += 0.02f
					sun.alpha = (sun.scale * 255).toInt().coerceIn(0, 255)
					if (sun.scale >= 1.2f) {
						sun.state = SunState.PULSING_OUT
					}
				}
				SunState.PULSING_OUT -> {
					sun.scale -= 0.015f
					if (sun.scale <= 0.8f) {
						sun.pulseCount++
						if (sun.pulseCount >= 2) {
							sun.state = SunState.FADING
						} else {
							sun.state = SunState.PULSING_IN
						}
					}
				}
				SunState.FADING -> {
					sun.alpha -= 5
					sun.scale -= 0.01f
					if (sun.alpha <= 0) {
						sun.state = SunState.DONE
					}
				}
				SunState.DONE -> {
					iterator.remove()
				}
			}
		}
	}

	private fun spawnSun() {
		if (width <= 0 || height <= 0) return

		val x = Random.nextFloat() * (width - sunSize * 2) + sunSize
		val y = Random.nextFloat() * (height * 0.4f) + sunSize

		suns.add(
			Sun(
				x = x,
				y = y,
				size = sunSize + Random.nextFloat() * 15f,
				state = SunState.WAITING,
				alpha = 0,
				waitTimer = Random.nextInt(10, 40),
				scale = 0.3f,
				pulseCount = 0
			)
		)
	}

	private fun updateBeachUmbrellas() {
		if (width <= 0 || height <= 0) return

		umbrellaSpawnTimer++
		if (umbrellaSpawnTimer >= umbrellaSpawnInterval && beachUmbrellas.none { it.state != BeachUmbrellaState.DONE }) {
			umbrellaSpawnTimer = 0
			spawnBeachUmbrellas()
		}

		val iterator = beachUmbrellas.iterator()
		while (iterator.hasNext()) {
			val umbrella = iterator.next()

			when (umbrella.state) {
				BeachUmbrellaState.WAITING -> {
					umbrella.waitTimer--
					if (umbrella.waitTimer <= 0) {
						umbrella.state = BeachUmbrellaState.RISING
						umbrella.velocity = umbrellaRiseSpeed
					}
				}
				BeachUmbrellaState.RISING -> {
					umbrella.velocity += 0.2f  // Slow deceleration
					umbrella.y += umbrella.velocity

					if (umbrella.velocity >= 0 && umbrella.y >= umbrella.groundY) {
						umbrella.y = umbrella.groundY
						umbrella.state = BeachUmbrellaState.SETTLING
					}
				}
				BeachUmbrellaState.SETTLING -> {
					umbrella.state = BeachUmbrellaState.FADING
				}
				BeachUmbrellaState.FADING -> {
					umbrella.alpha -= 3
					if (umbrella.alpha <= 0) {
						umbrella.state = BeachUmbrellaState.DONE
					}
				}
				BeachUmbrellaState.DONE -> {
					iterator.remove()
				}
			}
		}
	}

	private fun spawnBeachUmbrellas() {
		if (width <= 0 || height <= 0) return

		val groundY = height - umbrellaSize / 2 - 20f
		val spacing = width / (umbrellaCount + 1)

		repeat(umbrellaCount) { i ->
			val x = spacing * (i + 1) + Random.nextFloat() * 40 - 20
			beachUmbrellas.add(
				BeachUmbrella(
					x = x.toFloat(),
					y = height + umbrellaSize,
					velocity = 0f,
					size = umbrellaSize,
					state = BeachUmbrellaState.WAITING,
					alpha = 255,
					groundY = groundY,
					waitTimer = i * 25 + Random.nextInt(0, 30)
				)
			)
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		if (!isActive) return

		// Draw suns using cached bitmaps
		sunBitmap?.let { baseBitmap ->
			suns.forEach { sun ->
				if (sun.state != SunState.DONE && sun.state != SunState.WAITING) {
					val scaledSize = (sun.size * sun.scale).toInt()
					getScaledBitmap(baseBitmap, scaledSize)?.let { bitmap ->
						paint.alpha = sun.alpha.coerceIn(0, 255)
						canvas.drawBitmap(
							bitmap,
							sun.x - scaledSize / 2f,
							sun.y - scaledSize / 2f,
							paint
						)
					}
				}
			}
		}

		// Draw beach balls using cached bitmaps
		beachBallBitmap?.let { baseBitmap ->
			beachBalls.forEach { ball ->
				if (ball.state != BeachBallState.DONE && ball.state != BeachBallState.WAITING) {
					val size = ball.size.toInt()
					getScaledBitmap(baseBitmap, size)?.let { bitmap ->
						paint.alpha = ball.alpha.coerceIn(0, 255)
						canvas.drawBitmap(
							bitmap,
							ball.x - size / 2f,
							ball.y - size / 2f,
							paint
						)
					}
				}
			}
		}

		// Draw umbrellas using cached bitmaps
		umbrellaBitmap?.let { baseBitmap ->
			beachUmbrellas.forEach { umbrella ->
				if (umbrella.state != BeachUmbrellaState.DONE && umbrella.state != BeachUmbrellaState.WAITING) {
					val size = umbrella.size.toInt()
					getScaledBitmap(baseBitmap, size)?.let { bitmap ->
						paint.alpha = umbrella.alpha.coerceIn(0, 255)
						canvas.drawBitmap(
							bitmap,
							umbrella.x - size / 2f,
							umbrella.y - size / 2f,
							paint
						)
					}
				}
			}
		}
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		stopEffect()
		bitmapCache.clear()
	}

	override fun onVisibilityChanged(changedView: View, visibility: Int) {
		super.onVisibilityChanged(changedView, visibility)

		if (visibility == VISIBLE && isActive) {
			if (animationRunnable == null) {
				startAnimation()
			}
		} else {
			animationRunnable?.let { handler.removeCallbacks(it) }
		}
	}
}
