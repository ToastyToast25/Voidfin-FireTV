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
 * A custom view that renders a snowfall effect overlay.
 * Snowflakes fall from the top of the screen with varying sizes, speeds, and horizontal drift.
 * Snowmen periodically drop, bounce, settle, and fade out at the bottom.
 * 
 * Inspired by Home Assistant's seasonal surprise feature.
 * 
 * Performance optimized for Android TV / Fire TV devices:
 * - Uses cached bitmap rendering from vector drawables
 * - Reduced particle count for low-powered devices
 * - Frame-skipping animation loop to reduce CPU load
 * - Pre-calculated sine table for drift calculations
 */
class SnowfallView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	private data class Snowflake(
		var x: Float,
		var y: Float,
		val size: Float,
		val speed: Float,
		val driftAmplitude: Float,
		var driftIndex: Int,
		val driftIndexSpeed: Int,
		val alpha: Int,
		var rotation: Float,
		val rotationSpeed: Float
	)

	private enum class SnowmanState {
		WAITING, RISING, BOUNCING, SETTLING, FADING, DONE
	}

	private data class Snowman(
		var x: Float,
		var y: Float,
		var velocity: Float,
		val size: Float,
		var state: SnowmanState,
		var alpha: Int,
		var bounceCount: Int = 0,
		val groundY: Float,
		var waitTimer: Int = 0
	)

	companion object {
		// Pre-calculated sine table for faster drift calculations
		private const val SINE_TABLE_SIZE = 360
		private val sineTable = FloatArray(SINE_TABLE_SIZE) { i ->
			kotlin.math.sin(i * Math.PI / 180.0).toFloat()
		}
	}

	private val snowflakes = mutableListOf<Snowflake>()
	private val snowmen = mutableListOf<Snowman>()
	
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
	
	// Cached bitmaps for efficient drawing
	private var snowflakeBitmap: Bitmap? = null
	private var snowmanBitmap: Bitmap? = null
	private val bitmapCache = mutableMapOf<Int, Bitmap>()

	private val handler = Handler(Looper.getMainLooper())
	private var animationRunnable: Runnable? = null
	private var isSnowing = false
	private var snowmanSpawnTimer = 0
	private val snowmanSpawnInterval = 300
	private var frameCount = 0

	// Particle settings
	private val maxSnowflakes = 25
	private val minSize = 16f
	private val maxSize = 32f
	private val minSpeed = 1.0f
	private val maxSpeed = 2.5f
	private val minAlpha = 180
	private val maxAlpha = 255
	
	private val snowmanCount = 4
	private val snowmanSize = 70f
	private val gravity = 0.35f
	private val bounceDamping = 0.2f
	private val popUpVelocity = -6f

	init {
		isClickable = false
		isFocusable = false
		loadBitmaps()
	}
	
	private fun loadBitmaps() {
		ContextCompat.getDrawable(context, R.drawable.seasonal_snowflake)?.let { drawable ->
			snowflakeBitmap = drawable.toBitmap(48, 48)
		}
		ContextCompat.getDrawable(context, R.drawable.seasonal_snowman)?.let { drawable ->
			snowmanBitmap = drawable.toBitmap(80, 80)
		}
	}
	
	private fun getScaledBitmap(source: Bitmap?, size: Int): Bitmap? {
		source ?: return null
		return bitmapCache.getOrPut(System.identityHashCode(source) * 1000 + size) {
			Bitmap.createScaledBitmap(source, size, size, true)
		}
	}

	/**
	 * Start the snowfall animation.
	 */
	fun startSnowing() {
		if (isSnowing) return
		isSnowing = true
		
		initSnowflakes()
		startAnimation()
	}

	/**
	 * Stop the snowfall animation.
	 */
	fun stopSnowing() {
		if (!isSnowing) return
		isSnowing = false
		
		animationRunnable?.let { handler.removeCallbacks(it) }
		animationRunnable = null
		snowflakes.clear()
		snowmen.clear()
		snowmanSpawnTimer = 0
		frameCount = 0
		invalidate()
	}

	private fun initSnowflakes() {
		snowflakes.clear()
		
		if (width <= 0 || height <= 0) {
			return
		}

		repeat(maxSnowflakes) {
			snowflakes.add(createSnowflake(randomY = true))
		}
	}

	private fun createSnowflake(randomY: Boolean = false): Snowflake {
		val size = Random.nextFloat() * (maxSize - minSize) + minSize
		return Snowflake(
			x = Random.nextFloat() * width,
			y = if (randomY) Random.nextFloat() * height else -size * 2,
			size = size,
			speed = Random.nextFloat() * (maxSpeed - minSpeed) + minSpeed,
			driftAmplitude = Random.nextFloat() * 20f + 8f,
			driftIndex = Random.nextInt(SINE_TABLE_SIZE),
			driftIndexSpeed = Random.nextInt(1, 4),
			alpha = Random.nextInt(minAlpha, maxAlpha),
			rotation = Random.nextFloat() * 360f,
			rotationSpeed = Random.nextFloat() * 2f - 1f
		)
	}

	private fun startAnimation() {
		animationRunnable?.let { handler.removeCallbacks(it) }
		
		animationRunnable = object : Runnable {
			override fun run() {
				if (!isSnowing) return
				
				frameCount++
				updateSnowflakes()
				// Only update snowmen every other frame to reduce calculations
				if (frameCount % 2 == 0) {
					updateSnowmen()
				}
				invalidate()
				
				// Target ~30fps for smooth animations
				handler.postDelayed(this, 33L)
			}
		}
		handler.post(animationRunnable!!)
	}

	private fun updateSnowflakes() {
		if (width <= 0 || height <= 0) return

		snowflakes.forEachIndexed { index, flake ->
			flake.y += flake.speed
			flake.rotation += flake.rotationSpeed
			
			// Use pre-calculated sine table instead of Math.sin()
			flake.driftIndex = (flake.driftIndex + flake.driftIndexSpeed) % SINE_TABLE_SIZE
			flake.x += sineTable[flake.driftIndex] * flake.driftAmplitude * 0.015f
			
			if (flake.y > height + flake.size) {
				val newFlake = createSnowflake(randomY = false)
				snowflakes[index] = newFlake
			}
			
			if (flake.x < -flake.size) {
				flake.x = width + flake.size
			} else if (flake.x > width + flake.size) {
				flake.x = -flake.size
			}
		}
	}

	private fun updateSnowmen() {
		if (width <= 0 || height <= 0) return

		snowmanSpawnTimer++
		if (snowmanSpawnTimer >= snowmanSpawnInterval && snowmen.none { it.state != SnowmanState.DONE }) {
			snowmanSpawnTimer = 0
			spawnSnowmen()
		}

		val iterator = snowmen.iterator()
		while (iterator.hasNext()) {
			val snowman = iterator.next()
			
			when (snowman.state) {
				SnowmanState.WAITING -> {
					snowman.waitTimer--
					if (snowman.waitTimer <= 0) {
						snowman.state = SnowmanState.RISING
						snowman.velocity = popUpVelocity
					}
				}
				SnowmanState.RISING -> {
					snowman.velocity += gravity
					snowman.y += snowman.velocity
					
					if (snowman.velocity >= 0 && snowman.y >= snowman.groundY) {
						snowman.y = snowman.groundY
						snowman.velocity = popUpVelocity * bounceDamping  // Small bounce
						snowman.state = SnowmanState.BOUNCING
					}
				}
				SnowmanState.BOUNCING -> {
					snowman.velocity += gravity
					snowman.y += snowman.velocity
					
					if (snowman.y >= snowman.groundY) {
						snowman.y = snowman.groundY
						snowman.bounceCount++
						
						if (snowman.bounceCount >= 1) {
							snowman.state = SnowmanState.SETTLING
							snowman.velocity = 0f
						} else {
							snowman.velocity = popUpVelocity * bounceDamping * 0.5f
						}
					}
				}
				SnowmanState.SETTLING -> {
					snowman.state = SnowmanState.FADING
				}
				SnowmanState.FADING -> {
					snowman.alpha -= 3  // Fade out gradually
					if (snowman.alpha <= 0) {
						snowman.state = SnowmanState.DONE
					}
				}
				SnowmanState.DONE -> {
					iterator.remove()
				}
			}
		}
	}

	private fun spawnSnowmen() {
		if (width <= 0 || height <= 0) return
		
		val groundY = height - snowmanSize / 2 - 20f  // Slightly above bottom
		val spacing = width / (snowmanCount + 1)
		
		repeat(snowmanCount) { i ->
			val x = spacing * (i + 1) + Random.nextFloat() * 40 - 20  // Add some randomness
			val staggerDelay = Random.nextInt(15, 50)  // Random delay at 20fps
			snowmen.add(
				Snowman(
					x = x.toFloat(),
					y = height + snowmanSize,  // Start below screen
					velocity = 0f,
					size = snowmanSize,
					state = SnowmanState.WAITING,
					alpha = 255,
					groundY = groundY,
					waitTimer = staggerDelay * i + Random.nextInt(0, 20)  // Staggered start times
				)
			)
		}
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		
		if (isSnowing && snowflakes.isEmpty()) {
			initSnowflakes()
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		
		if (!isSnowing) return

		// Draw snowflakes using cached bitmaps
		snowflakeBitmap?.let { baseBitmap ->
			snowflakes.forEach { flake ->
				val size = flake.size.toInt()
				getScaledBitmap(baseBitmap, size)?.let { bitmap ->
					paint.alpha = flake.alpha
					canvas.save()
					canvas.translate(flake.x, flake.y)
					canvas.rotate(flake.rotation)
					canvas.drawBitmap(bitmap, -size / 2f, -size / 2f, paint)
					canvas.restore()
				}
			}
		}
		
		// Draw snowmen using cached bitmaps
		snowmanBitmap?.let { baseBitmap ->
			snowmen.forEach { snowman ->
				if (snowman.state != SnowmanState.DONE && snowman.state != SnowmanState.WAITING) {
					val size = snowman.size.toInt()
					getScaledBitmap(baseBitmap, size)?.let { bitmap ->
						paint.alpha = snowman.alpha
						canvas.drawBitmap(
							bitmap,
							snowman.x - size / 2f,
							snowman.y - size / 2f,
							paint
						)
					}
				}
			}
		}
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		stopSnowing()
		bitmapCache.clear()
	}

	override fun onVisibilityChanged(changedView: View, visibility: Int) {
		super.onVisibilityChanged(changedView, visibility)
		
		if (visibility == VISIBLE && isSnowing) {
			if (animationRunnable == null) {
				startAnimation()
			}
		} else {
			animationRunnable?.let { handler.removeCallbacks(it) }
		}
	}
}
