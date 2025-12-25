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
 * A custom view that renders falling cherry blossom petals and flowers for spring.
 * Petals fall slower than snowflakes with gentle swaying motion.
 * 
 * Performance optimized for Android TV / Fire TV devices:
 * - Uses cached bitmap rendering from vector drawables
 * - Reduced particle count for low-powered devices
 * - Pre-calculated sine table for drift calculations
 */
class PetalfallView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	private data class Petal(
		var x: Float,
		var y: Float,
		val size: Float,
		val speed: Float,
		val driftAmplitude: Float,
		var driftIndex: Int,
		val driftIndexSpeed: Int,
		var currentRotation: Float,
		val rotationSpeed: Float,
		val alpha: Int,
		val isPink: Boolean  // true = pink petal, false = yellow flower center
	)

	private enum class BeeState {
		WAITING, FLYING, FADING, DONE
	}

	private data class Bee(
		var x: Float,
		var y: Float,
		val targetX: Float,
		val speed: Float,
		val size: Float,
		var state: BeeState,
		var alpha: Int,
		var waitTimer: Int,
		var buzzIndex: Int,
		val buzzIndexSpeed: Int,
		val buzzAmplitude: Float,
		val fromLeft: Boolean
	)

	companion object {
		private const val SINE_TABLE_SIZE = 360
		private val sineTable = FloatArray(SINE_TABLE_SIZE) { i ->
			kotlin.math.sin(i * Math.PI / 180.0).toFloat()
		}
	}

	private val petals = mutableListOf<Petal>()
	private val bees = mutableListOf<Bee>()
	
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
	
	// Cached bitmaps for efficient drawing
	private var cherryBlossomBitmap: Bitmap? = null
	private var beeBitmap: Bitmap? = null
	private val bitmapCache = mutableMapOf<Int, Bitmap>()

	private val handler = Handler(Looper.getMainLooper())
	private var animationRunnable: Runnable? = null
	private var isFalling = false
	private var beeSpawnTimer = 0
	private val beeSpawnInterval = 300
	private var frameCount = 0

	// Particle settings
	private val maxPetals = 20
	private val minSize = 20f
	private val maxSize = 36f
	private val minSpeed = 0.6f
	private val maxSpeed = 1.4f
	private val minAlpha = 200
	private val maxAlpha = 255

	private val beeCount = 3
	private val beeSize = 40f
	private val beeSpeed = 2.5f

	init {
		isClickable = false
		isFocusable = false
		loadBitmaps()
	}
	
	private fun loadBitmaps() {
		ContextCompat.getDrawable(context, R.drawable.seasonal_cherry_blossom)?.let { drawable ->
			cherryBlossomBitmap = drawable.toBitmap(48, 48)
		}
		ContextCompat.getDrawable(context, R.drawable.seasonal_bee)?.let { drawable ->
			beeBitmap = drawable.toBitmap(48, 48)
		}
	}
	
	private fun getScaledBitmap(source: Bitmap?, size: Int): Bitmap? {
		source ?: return null
		return bitmapCache.getOrPut(System.identityHashCode(source) * 1000 + size) {
			Bitmap.createScaledBitmap(source, size, size, true)
		}
	}

	/**
	 * Start the petal fall animation.
	 */
	fun startFalling() {
		if (isFalling) return
		isFalling = true
		
		initPetals()
		startAnimation()
	}

	/**
	 * Stop the petal fall animation.
	 */
	fun stopFalling() {
		if (!isFalling) return
		isFalling = false
		
		animationRunnable?.let { handler.removeCallbacks(it) }
		animationRunnable = null
		petals.clear()
		bees.clear()
		beeSpawnTimer = 0
		frameCount = 0
		invalidate()
	}

	private fun initPetals() {
		petals.clear()
		
		if (width <= 0 || height <= 0) {
			return
		}

		repeat(maxPetals) {
			petals.add(createPetal(randomY = true))
		}
	}

	private fun createPetal(randomY: Boolean = false): Petal {
		val size = Random.nextFloat() * (maxSize - minSize) + minSize
		return Petal(
			x = Random.nextFloat() * width,
			y = if (randomY) Random.nextFloat() * height else -size * 2,
			size = size,
			speed = Random.nextFloat() * (maxSpeed - minSpeed) + minSpeed,
			driftAmplitude = Random.nextFloat() * 35f + 15f,  // Drift for floating effect
			driftIndex = Random.nextInt(SINE_TABLE_SIZE),
			driftIndexSpeed = Random.nextInt(1, 3),
			currentRotation = Random.nextFloat() * 360f,
			rotationSpeed = Random.nextFloat() * 1.2f + 0.3f,
			alpha = Random.nextInt(minAlpha, maxAlpha),
			isPink = Random.nextFloat() > 0.2f  // 80% pink petals, 20% yellow
		)
	}

	private fun startAnimation() {
		animationRunnable?.let { handler.removeCallbacks(it) }
		
		animationRunnable = object : Runnable {
			override fun run() {
				if (!isFalling) return
				
				frameCount++
				updatePetals()
				// Only update bees every other frame
				if (frameCount % 2 == 0) {
					updateBees()
				}
				invalidate()
				
				// Target ~30fps
				handler.postDelayed(this, 33L)
			}
		}
		handler.post(animationRunnable!!)
	}

	private fun updatePetals() {
		if (width <= 0 || height <= 0) return

		petals.forEachIndexed { index, petal ->
			petal.y += petal.speed
			
			petal.driftIndex = (petal.driftIndex + petal.driftIndexSpeed) % SINE_TABLE_SIZE
			petal.x += sineTable[petal.driftIndex] * petal.driftAmplitude * 0.012f
			
			petal.currentRotation += petal.rotationSpeed
			if (petal.currentRotation > 360f) petal.currentRotation -= 360f
			
			if (petal.y > height + petal.size) {
				val newPetal = createPetal(randomY = false)
				petals[index] = newPetal
			}
			
			if (petal.x < -petal.size) {
				petal.x = width + petal.size
			} else if (petal.x > width + petal.size) {
				petal.x = -petal.size
			}
		}
	}

	private fun updateBees() {
		if (width <= 0 || height <= 0) return

		beeSpawnTimer++
		if (beeSpawnTimer >= beeSpawnInterval && bees.none { it.state != BeeState.DONE }) {
			beeSpawnTimer = 0
			spawnBees()
		}

		val iterator = bees.iterator()
		while (iterator.hasNext()) {
			val bee = iterator.next()
			
			when (bee.state) {
				BeeState.WAITING -> {
					bee.waitTimer--
					if (bee.waitTimer <= 0) {
						bee.state = BeeState.FLYING
					}
				}
				BeeState.FLYING -> {
					if (bee.fromLeft) {
						bee.x += bee.speed
					} else {
						bee.x -= bee.speed
					}
					
					bee.buzzIndex = (bee.buzzIndex + bee.buzzIndexSpeed) % SINE_TABLE_SIZE
					bee.y += sineTable[bee.buzzIndex] * bee.buzzAmplitude
					
					val reachedEnd = if (bee.fromLeft) bee.x > width + bee.size else bee.x < -bee.size
					if (reachedEnd) {
						bee.state = BeeState.FADING
					}
				}
				BeeState.FADING -> {
					bee.alpha -= 20
					if (bee.alpha <= 0) {
						bee.state = BeeState.DONE
					}
				}
				BeeState.DONE -> {
					iterator.remove()
				}
			}
		}
	}

	private fun spawnBees() {
		if (width <= 0 || height <= 0) return
		
		val usableHeight = height * 0.6f
		val topMargin = height * 0.2f
		val zoneHeight = usableHeight / beeCount
		
		repeat(beeCount) { i ->
			val fromLeft = Random.nextBoolean()
			val startX = if (fromLeft) -beeSize else width + beeSize
			val baseY = topMargin + (zoneHeight * i) + (zoneHeight * 0.2f)
			val y = baseY + Random.nextFloat() * (zoneHeight * 0.6f)
			
			bees.add(
				Bee(
					x = startX,
					y = y,
					targetX = if (fromLeft) width + beeSize else -beeSize,
					speed = beeSpeed + Random.nextFloat() * 1f,
					size = beeSize,
					state = BeeState.WAITING,
					alpha = 255,
					waitTimer = i * 30 + Random.nextInt(15, 45),
					buzzIndex = Random.nextInt(SINE_TABLE_SIZE),
					buzzIndexSpeed = Random.nextInt(15, 25),  // Fast vibration
					buzzAmplitude = 1.2f + Random.nextFloat() * 0.8f,
					fromLeft = fromLeft
				)
			)
		}
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		
		if (isFalling && petals.isEmpty()) {
			initPetals()
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		
		if (!isFalling) return

		// Draw petals using cached bitmaps
		cherryBlossomBitmap?.let { baseBitmap ->
			petals.forEach { petal ->
				val size = petal.size.toInt()
				getScaledBitmap(baseBitmap, size)?.let { bitmap ->
					paint.alpha = petal.alpha
					canvas.save()
					canvas.translate(petal.x, petal.y)
					canvas.rotate(petal.currentRotation)
					canvas.drawBitmap(bitmap, -size / 2f, -size / 2f, paint)
					canvas.restore()
				}
			}
		}

		// Draw bees using cached bitmaps
		beeBitmap?.let { baseBitmap ->
			bees.forEach { bee ->
				if (bee.state != BeeState.DONE && bee.state != BeeState.WAITING) {
					val size = bee.size.toInt()
					getScaledBitmap(baseBitmap, size)?.let { bitmap ->
						paint.alpha = bee.alpha
						canvas.save()
						canvas.translate(bee.x, bee.y)
						if (!bee.fromLeft) {
							canvas.scale(-1f, 1f)
						}
						canvas.drawBitmap(bitmap, -size / 2f, -size / 2f, paint)
						canvas.restore()
					}
				}
			}
		}
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		stopFalling()
		bitmapCache.clear()
	}

	override fun onVisibilityChanged(changedView: View, visibility: Int) {
		super.onVisibilityChanged(changedView, visibility)
		
		if (visibility == VISIBLE && isFalling) {
			if (animationRunnable == null) {
				startAnimation()
			}
		} else {
			animationRunnable?.let { handler.removeCallbacks(it) }
		}
	}
}
