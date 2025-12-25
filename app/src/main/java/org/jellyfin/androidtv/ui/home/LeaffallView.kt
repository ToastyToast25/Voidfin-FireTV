package org.jellyfin.androidtv.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
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
 * A custom view that renders falling autumn leaves for fall season.
 * Leaves fall slowly with gentle swaying and rotation.
 * 
 * Performance optimized for Android TV / Fire TV devices:
 * - Uses cached bitmap rendering from vector drawables
 * - Reduced particle count for low-powered devices
 * - Pre-calculated sine table for drift calculations
 */
class LeaffallView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	private data class Leaf(
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
		val colorIndex: Int  // 0 = orange, 1 = red, 2 = brown
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

	companion object {
		private const val SINE_TABLE_SIZE = 360
		private val sineTable = FloatArray(SINE_TABLE_SIZE) { i ->
			kotlin.math.sin(i * Math.PI / 180.0).toFloat()
		}
	}

	private val leaves = mutableListOf<Leaf>()
	private val pumpkins = mutableListOf<Pumpkin>()
	
	private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
	
	// Cached bitmaps for efficient drawing
	private var leafBitmap: Bitmap? = null
	private var pumpkinBitmap: Bitmap? = null
	private val bitmapCache = mutableMapOf<Int, Bitmap>()
	
	// Leaf color tints for autumn palette
	private val leafColors = listOf(
		0xFFFF8C00.toInt(),  // Dark orange
		0xFFCD5C5C.toInt(),  // Indian red
		0xFF8B4513.toInt()   // Saddle brown
	)
	private val colorFilters = leafColors.map { color ->
		PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
	}

	private val handler = Handler(Looper.getMainLooper())
	private var animationRunnable: Runnable? = null
	private var isFalling = false
	private var pumpkinSpawnTimer = 0
	private val pumpkinSpawnInterval = 300
	private var frameCount = 0

	// Particle settings
	private val maxLeaves = 18
	private val minSize = 24f
	private val maxSize = 40f
	private val minSpeed = 0.5f
	private val maxSpeed = 1.2f
	private val minAlpha = 220
	private val maxAlpha = 255

	private val pumpkinCount = 4
	private val pumpkinSize = 60f
	private val gravity = 0.35f
	private val bounceDamping = 0.2f
	private val popUpVelocity = -6f

	init {
		isClickable = false
		isFocusable = false
		loadBitmaps()
	}
	
	private fun loadBitmaps() {
		ContextCompat.getDrawable(context, R.drawable.seasonal_maple_leaf)?.let { drawable ->
			leafBitmap = drawable.toBitmap(48, 48)
		}
		ContextCompat.getDrawable(context, R.drawable.seasonal_pumpkin)?.let { drawable ->
			pumpkinBitmap = drawable.toBitmap(64, 64)
		}
	}
	
	private fun getScaledBitmap(source: Bitmap?, size: Int): Bitmap? {
		source ?: return null
		return bitmapCache.getOrPut(System.identityHashCode(source) * 1000 + size) {
			Bitmap.createScaledBitmap(source, size, size, true)
		}
	}

	/**
	 * Start the leaf fall animation.
	 */
	fun startFalling() {
		if (isFalling) return
		isFalling = true
		
		initLeaves()
		startAnimation()
	}

	/**
	 * Stop the leaf fall animation.
	 */
	fun stopFalling() {
		if (!isFalling) return
		isFalling = false
		
		animationRunnable?.let { handler.removeCallbacks(it) }
		animationRunnable = null
		leaves.clear()
		pumpkins.clear()
		pumpkinSpawnTimer = 0
		frameCount = 0
		invalidate()
	}

	private fun initLeaves() {
		leaves.clear()
		
		if (width <= 0 || height <= 0) {
			return
		}

		repeat(maxLeaves) {
			leaves.add(createLeaf(randomY = true))
		}
	}

	private fun createLeaf(randomY: Boolean = false): Leaf {
		val size = Random.nextFloat() * (maxSize - minSize) + minSize
		return Leaf(
			x = Random.nextFloat() * width,
			y = if (randomY) Random.nextFloat() * height else -size * 2,
			size = size,
			speed = Random.nextFloat() * (maxSpeed - minSpeed) + minSpeed,
			driftAmplitude = Random.nextFloat() * 40f + 20f,  // Wide drift for floating leaf effect
			driftIndex = Random.nextInt(SINE_TABLE_SIZE),
			driftIndexSpeed = Random.nextInt(1, 3),
			currentRotation = Random.nextFloat() * 360f,
			rotationSpeed = Random.nextFloat() * 1.5f + 0.2f,
			alpha = Random.nextInt(minAlpha, maxAlpha),
			colorIndex = Random.nextInt(3)
		)
	}

	private fun startAnimation() {
		animationRunnable?.let { handler.removeCallbacks(it) }
		
		animationRunnable = object : Runnable {
			override fun run() {
				if (!isFalling) return
				
				frameCount++
				updateLeaves()
				// Only update pumpkins every other frame
				if (frameCount % 2 == 0) {
					updatePumpkins()
				}
				invalidate()
				
				// Target ~30fps
				handler.postDelayed(this, 33L)
			}
		}
		handler.post(animationRunnable!!)
	}

	private fun updateLeaves() {
		if (width <= 0 || height <= 0) return

		leaves.forEachIndexed { index, leaf ->
			leaf.y += leaf.speed
			
			leaf.driftIndex = (leaf.driftIndex + leaf.driftIndexSpeed) % SINE_TABLE_SIZE
			leaf.x += sineTable[leaf.driftIndex] * leaf.driftAmplitude * 0.01f
			
			leaf.currentRotation += leaf.rotationSpeed
			if (leaf.currentRotation > 360f) leaf.currentRotation -= 360f
			
			if (leaf.y > height + leaf.size) {
				val newLeaf = createLeaf(randomY = false)
				leaves[index] = newLeaf
			}
			
			if (leaf.x < -leaf.size) {
				leaf.x = width + leaf.size
			} else if (leaf.x > width + leaf.size) {
				leaf.x = -leaf.size
			}
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
					waitTimer = i * 25 + Random.nextInt(0, 30)
				)
			)
		}
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		
		if (isFalling && leaves.isEmpty()) {
			initLeaves()
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		
		if (!isFalling) return

		// Draw leaves using cached bitmaps with color tints
		leafBitmap?.let { baseBitmap ->
			leaves.forEach { leaf ->
				val size = leaf.size.toInt()
				getScaledBitmap(baseBitmap, size)?.let { bitmap ->
					paint.alpha = leaf.alpha
					paint.colorFilter = colorFilters[leaf.colorIndex]
					canvas.save()
					canvas.translate(leaf.x, leaf.y)
					canvas.rotate(leaf.currentRotation)
					canvas.drawBitmap(bitmap, -size / 2f, -size / 2f, paint)
					canvas.restore()
				}
			}
		}
		paint.colorFilter = null
		
		// Draw pumpkins using cached bitmaps
		pumpkinBitmap?.let { baseBitmap ->
			pumpkins.forEach { pumpkin ->
				if (pumpkin.state != PumpkinState.DONE && pumpkin.state != PumpkinState.WAITING) {
					val size = pumpkin.size.toInt()
					getScaledBitmap(baseBitmap, size)?.let { bitmap ->
						paint.alpha = pumpkin.alpha
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
