package org.jellyfin.androidtv.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import org.jellyfin.androidtv.R
import kotlin.random.Random

/**
 * A custom view that renders Halloween effects with ghosts, pumpkins,
 * spiders, and raining candy.
 * 
 * Performance optimized for Android TV / Fire TV devices:
 * - Uses cached bitmap rendering from vector drawables
 * - Reduced element count for low-powered devices
 * - Pre-calculated sine table for drift calculations
 */
class HalloweenView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	private enum class GhostState {
		WAITING, FLOATING, FADING, DONE
	}

	private data class Ghost(
		var x: Float,
		var y: Float,
		val baseY: Float,
		val speed: Float,
		val size: Float,
		var state: GhostState,
		var alpha: Int,
		var waitTimer: Int,
		var floatIndex: Int,
		val floatIndexSpeed: Int,
		val floatAmplitude: Float,
		val fromLeft: Boolean
	)

	private enum class PumpkinState {
		WAITING, RISING, BOUNCING, SETTLING, FADING, DONE
	}

	private data class Pumpkin(
		var x: Float,
		var y: Float,
		var velocity: Float,
		val size: Float,
		var state: PumpkinState,
		var alpha: Int,
		var bounceCount: Int = 0,
		val groundY: Float,
		var waitTimer: Int = 0
	)

	private enum class SpiderState {
		WAITING, APPEARING, VISIBLE, DISAPPEARING, DONE
	}

	private data class Spider(
		val x: Float,
		val y: Float,
		val size: Float,
		var state: SpiderState,
		var alpha: Int,
		var waitTimer: Int,
		var visibleTimer: Int
	)

	private data class Candy(
		var x: Float,
		var y: Float,
		val size: Float,
		val speed: Float,
		val driftAmplitude: Float,
		var driftIndex: Int,
		val driftIndexSpeed: Int,
		val alpha: Int,
		val colorIndex: Int
	)

	companion object {
		private const val SINE_TABLE_SIZE = 360
		private val sineTable = FloatArray(SINE_TABLE_SIZE) { i ->
			kotlin.math.sin(i * Math.PI / 180.0).toFloat()
		}
	}

	private val ghosts = mutableListOf<Ghost>()
	private val pumpkins = mutableListOf<Pumpkin>()
	private val spiders = mutableListOf<Spider>()
	private val candies = mutableListOf<Candy>()

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
	
	// Cached bitmaps for efficient drawing
	private var ghostBitmap: Bitmap? = null
	private var pumpkinBitmap: Bitmap? = null
	private var spiderBitmap: Bitmap? = null
	private var candyBitmap: Bitmap? = null
	private val bitmapCache = mutableMapOf<Int, Bitmap>()
	
	// Candy color tints
	private val candyColors = listOf(
		0xFFFF6B6B.toInt(),  // Red
		0xFFFFE66D.toInt(),  // Yellow
		0xFF4ECDC4.toInt(),  // Teal
		0xFFA855F7.toInt()   // Purple
	)
	private val candyColorFilters = candyColors.map { color ->
		PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
	}

	private val handler = Handler(Looper.getMainLooper())
	private var animationRunnable: Runnable? = null
	private var isActive = false
	private var ghostSpawnTimer = 0
	private var pumpkinSpawnTimer = 0
	private var spiderSpawnTimer = 0
	private val ghostSpawnInterval = 300
	private val pumpkinSpawnInterval = 400
	private val spiderSpawnInterval = 120
	private var frameCount = 0

	// Particle settings
	private val ghostCount = 3
	private val ghostSize = 55f

	private val pumpkinCount = 3
	private val pumpkinSize = 60f
	private val gravity = 0.35f
	private val bounceDamping = 0.2f
	private val popUpVelocity = -6f

	private val maxSpiders = 2
	private val spiderSize = 45f

	private val maxCandies = 12

	init {
		isClickable = false
		isFocusable = false
		loadBitmaps()
	}
	
	private fun loadBitmaps() {
		ContextCompat.getDrawable(context, R.drawable.seasonal_ghost)?.let { drawable ->
			ghostBitmap = drawable.toBitmap(64, 64)
		}
		ContextCompat.getDrawable(context, R.drawable.seasonal_jack_o_lantern)?.let { drawable ->
			pumpkinBitmap = drawable.toBitmap(64, 64)
		}
		ContextCompat.getDrawable(context, R.drawable.seasonal_spider)?.let { drawable ->
			spiderBitmap = drawable.toBitmap(48, 48)
		}
		ContextCompat.getDrawable(context, R.drawable.seasonal_candy)?.let { drawable ->
			candyBitmap = drawable.toBitmap(32, 32)
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
		initCandies()
		startAnimation()
	}

	fun stopEffect() {
		if (!isActive) return
		isActive = false

		animationRunnable?.let { handler.removeCallbacks(it) }
		animationRunnable = null
		ghosts.clear()
		pumpkins.clear()
		spiders.clear()
		candies.clear()
		ghostSpawnTimer = 0
		pumpkinSpawnTimer = 0
		spiderSpawnTimer = 0
		frameCount = 0
		invalidate()
	}

	private fun initCandies() {
		candies.clear()
		if (width <= 0 || height <= 0) return

		repeat(maxCandies) {
			candies.add(createCandy(randomY = true))
		}
	}

	private fun createCandy(randomY: Boolean = false): Candy {
		val size = Random.nextFloat() * 6f + 12f
		return Candy(
			x = Random.nextFloat() * width,
			y = if (randomY) Random.nextFloat() * height else -size * 2,
			size = size,
			speed = Random.nextFloat() * 0.8f + 0.6f,
			driftAmplitude = Random.nextFloat() * 15f + 8f,
			driftIndex = Random.nextInt(SINE_TABLE_SIZE),
			driftIndexSpeed = Random.nextInt(1, 3),
			alpha = Random.nextInt(180, 255),
			colorIndex = Random.nextInt(candyColors.size)
		)
	}

	private fun startAnimation() {
		animationRunnable?.let { handler.removeCallbacks(it) }

		animationRunnable = object : Runnable {
			override fun run() {
				if (!isActive) return
				
				frameCount++
				updateCandies()
				// Stagger updates to reduce per-frame work
				if (frameCount % 2 == 0) {
					updateGhosts()
				}
				if (frameCount % 3 == 0) {
					updatePumpkins()
					updateSpiders()
				}
				invalidate()
				
				// Target ~30fps
				handler.postDelayed(this, 33L)
			}
		}
		handler.post(animationRunnable!!)
	}

	private fun updateGhosts() {
		if (width <= 0 || height <= 0) return

		ghostSpawnTimer++
		if (ghostSpawnTimer >= ghostSpawnInterval && ghosts.none { it.state != GhostState.DONE }) {
			ghostSpawnTimer = 0
			spawnGhosts()
		}

		val iterator = ghosts.iterator()
		while (iterator.hasNext()) {
			val ghost = iterator.next()

			when (ghost.state) {
				GhostState.WAITING -> {
					ghost.waitTimer--
					if (ghost.waitTimer <= 0) {
						ghost.state = GhostState.FLOATING
					}
				}
				GhostState.FLOATING -> {
					if (ghost.fromLeft) {
						ghost.x += ghost.speed
					} else {
						ghost.x -= ghost.speed
					}

					ghost.floatIndex = (ghost.floatIndex + ghost.floatIndexSpeed) % SINE_TABLE_SIZE
					ghost.y = ghost.baseY + sineTable[ghost.floatIndex] * ghost.floatAmplitude

					val reachedEnd = if (ghost.fromLeft) ghost.x > width + ghost.size else ghost.x < -ghost.size
					if (reachedEnd) {
						ghost.state = GhostState.FADING
					}
				}
				GhostState.FADING -> {
					ghost.alpha -= 12
					if (ghost.alpha <= 0) {
						ghost.state = GhostState.DONE
					}
				}
				GhostState.DONE -> {
					iterator.remove()
				}
			}
		}
	}

	private fun spawnGhosts() {
		if (width <= 0 || height <= 0) return

		val usableHeight = height * 0.5f
		val topMargin = height * 0.15f
		val zoneHeight = usableHeight / ghostCount

		repeat(ghostCount) { i ->
			val fromLeft = Random.nextBoolean()
			val startX = if (fromLeft) -ghostSize else width + ghostSize
			val baseY = topMargin + (zoneHeight * i) + Random.nextFloat() * (zoneHeight * 0.6f)

			ghosts.add(
				Ghost(
					x = startX,
					y = baseY,
					baseY = baseY,
					speed = 1.8f + Random.nextFloat() * 0.8f,
					size = ghostSize + Random.nextFloat() * 8f,
					state = GhostState.WAITING,
					alpha = 200,
					waitTimer = i * 45 + Random.nextInt(15, 60),
					floatIndex = Random.nextInt(SINE_TABLE_SIZE),
					floatIndexSpeed = Random.nextInt(2, 5),
					floatAmplitude = 12f + Random.nextFloat() * 8f,
					fromLeft = fromLeft
				)
			)
		}
	}

	private fun updatePumpkins() {
		if (width <= 0 || height <= 0) return

		pumpkinSpawnTimer++
		if (pumpkinSpawnTimer >= pumpkinSpawnInterval && pumpkins.none { it.state != PumpkinState.DONE }) {
			pumpkinSpawnTimer = 0
			spawnPumpkins()
		}

		val iterator = pumpkins.iterator()
		while (iterator.hasNext()) {
			val pumpkin = iterator.next()

			when (pumpkin.state) {
				PumpkinState.WAITING -> {
					pumpkin.waitTimer--
					if (pumpkin.waitTimer <= 0) {
						pumpkin.state = PumpkinState.RISING
						pumpkin.velocity = popUpVelocity
					}
				}
				PumpkinState.RISING -> {
					pumpkin.velocity += gravity
					pumpkin.y += pumpkin.velocity

					if (pumpkin.velocity >= 0 && pumpkin.y >= pumpkin.groundY) {
						pumpkin.y = pumpkin.groundY
						pumpkin.velocity = popUpVelocity * bounceDamping
						pumpkin.state = PumpkinState.BOUNCING
					}
				}
				PumpkinState.BOUNCING -> {
					pumpkin.velocity += gravity
					pumpkin.y += pumpkin.velocity

					if (pumpkin.y >= pumpkin.groundY) {
						pumpkin.y = pumpkin.groundY
						pumpkin.bounceCount++

						if (pumpkin.bounceCount >= 1) {
							pumpkin.state = PumpkinState.SETTLING
							pumpkin.velocity = 0f
						} else {
							pumpkin.velocity = popUpVelocity * bounceDamping * 0.5f
						}
					}
				}
				PumpkinState.SETTLING -> {
					pumpkin.state = PumpkinState.FADING
				}
				PumpkinState.FADING -> {
					pumpkin.alpha -= 3
					if (pumpkin.alpha <= 0) {
						pumpkin.state = PumpkinState.DONE
					}
				}
				PumpkinState.DONE -> {
					iterator.remove()
				}
			}
		}
	}

	private fun spawnPumpkins() {
		if (width <= 0 || height <= 0) return

		val groundY = height - pumpkinSize / 2 - 20f
		val spacing = width / (pumpkinCount + 1)

		repeat(pumpkinCount) { i ->
			val x = spacing * (i + 1) + Random.nextFloat() * 40 - 20
			pumpkins.add(
				Pumpkin(
					x = x.toFloat(),
					y = height + pumpkinSize,
					velocity = 0f,
					size = pumpkinSize,
					state = PumpkinState.WAITING,
					alpha = 255,
					groundY = groundY,
					waitTimer = i * 30 + Random.nextInt(0, 40)
				)
			)
		}
	}

	private fun updateSpiders() {
		if (width <= 0 || height <= 0) return

		spiderSpawnTimer++
		val activeSpiders = spiders.count { it.state != SpiderState.DONE }
		if (spiderSpawnTimer >= spiderSpawnInterval && activeSpiders < maxSpiders) {
			spiderSpawnTimer = 0
			spawnSpider()
		}

		val iterator = spiders.iterator()
		while (iterator.hasNext()) {
			val spider = iterator.next()

			when (spider.state) {
				SpiderState.WAITING -> {
					spider.waitTimer--
					if (spider.waitTimer <= 0) {
						spider.state = SpiderState.APPEARING
					}
				}
				SpiderState.APPEARING -> {
					spider.alpha += 8
					if (spider.alpha >= 255) {
						spider.alpha = 255
						spider.state = SpiderState.VISIBLE
					}
				}
				SpiderState.VISIBLE -> {
					spider.visibleTimer--
					if (spider.visibleTimer <= 0) {
						spider.state = SpiderState.DISAPPEARING
					}
				}
				SpiderState.DISAPPEARING -> {
					spider.alpha -= 5
					if (spider.alpha <= 0) {
						spider.state = SpiderState.DONE
					}
				}
				SpiderState.DONE -> {
					iterator.remove()
				}
			}
		}
	}

	private fun spawnSpider() {
		if (width <= 0 || height <= 0) return

		val x = Random.nextFloat() * (width - spiderSize * 2) + spiderSize
		val y = Random.nextFloat() * (height * 0.25f) + spiderSize

		spiders.add(
			Spider(
				x = x,
				y = y,
				size = spiderSize + Random.nextFloat() * 10f,
				state = SpiderState.WAITING,
				alpha = 0,
				waitTimer = Random.nextInt(8, 25),
				visibleTimer = Random.nextInt(50, 100)  // 2.5-5 seconds visible
			)
		)
	}

	private fun updateCandies() {
		if (width <= 0 || height <= 0) return

		candies.forEachIndexed { index, candy ->
			candy.y += candy.speed
			candy.driftIndex = (candy.driftIndex + candy.driftIndexSpeed) % SINE_TABLE_SIZE
			candy.x += sineTable[candy.driftIndex] * candy.driftAmplitude * 0.012f

			if (candy.y > height + candy.size) {
				val newCandy = createCandy(randomY = false)
				candies[index] = newCandy
			}

			if (candy.x < -candy.size) {
				candy.x = width + candy.size
			} else if (candy.x > width + candy.size) {
				candy.x = -candy.size
			}
		}
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)

		if (isActive && candies.isEmpty()) {
			initCandies()
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)

		if (!isActive) return

		// Draw candies using cached bitmaps with color tints
		candyBitmap?.let { baseBitmap ->
			candies.forEach { candy ->
				val size = candy.size.toInt()
				getScaledBitmap(baseBitmap, size)?.let { bitmap ->
					paint.alpha = candy.alpha
					paint.colorFilter = candyColorFilters[candy.colorIndex]
					canvas.drawBitmap(
						bitmap,
						candy.x - size / 2f,
						candy.y - size / 2f,
						paint
					)
				}
			}
		}
		paint.colorFilter = null

		// Draw spiders using cached bitmaps
		spiderBitmap?.let { baseBitmap ->
			spiders.forEach { spider ->
				if (spider.state != SpiderState.DONE && spider.state != SpiderState.WAITING) {
					val size = spider.size.toInt()
					getScaledBitmap(baseBitmap, size)?.let { bitmap ->
						paint.alpha = spider.alpha.coerceIn(0, 255)
						canvas.drawBitmap(
							bitmap,
							spider.x - size / 2f,
							spider.y - size / 2f,
							paint
						)
					}
				}
			}
		}

		// Draw ghosts using cached bitmaps
		ghostBitmap?.let { baseBitmap ->
			ghosts.forEach { ghost ->
				if (ghost.state != GhostState.DONE && ghost.state != GhostState.WAITING) {
					val size = ghost.size.toInt()
					getScaledBitmap(baseBitmap, size)?.let { bitmap ->
						paint.alpha = ghost.alpha.coerceIn(0, 255)
						canvas.save()
						canvas.translate(ghost.x, ghost.y)
						if (!ghost.fromLeft) {
							canvas.scale(-1f, 1f)
						}
						canvas.drawBitmap(bitmap, -size / 2f, -size / 2f, paint)
						canvas.restore()
					}
				}
			}
		}

		// Draw pumpkins using cached bitmaps
		pumpkinBitmap?.let { baseBitmap ->
			pumpkins.forEach { pumpkin ->
				if (pumpkin.state != PumpkinState.DONE && pumpkin.state != PumpkinState.WAITING) {
					val size = pumpkin.size.toInt()
					getScaledBitmap(baseBitmap, size)?.let { bitmap ->
						paint.alpha = pumpkin.alpha.coerceIn(0, 255)
						canvas.drawBitmap(
							bitmap,
							pumpkin.x - size / 2f,
							pumpkin.y - size / 2f,
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
