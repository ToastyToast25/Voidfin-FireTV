package org.jellyfin.androidtv.ui.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.EMPTY_BUFFER
import androidx.media3.common.util.UnstableApi
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue
import kotlin.math.max
import kotlin.math.min

/**
 * An AudioProcessor that delays audio samples by a configurable amount.
 * 
 * Positive delay values will delay the audio (make it play later),
 * which effectively makes video appear ahead of audio.
 * 
 * Negative delay values will advance the audio (skip initial samples),
 * which effectively makes audio appear ahead of video.
 */
@OptIn(UnstableApi::class)
class AudioDelayProcessor : AudioProcessor {

    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET
    private var isActive = false
    
    private var delayMs: Long = 0
    private var pendingDelayMs: Long = 0
    private var delaySamples: Int = 0
    
    private val delayBuffer: Queue<ByteBuffer> = LinkedList()
    private var bufferedSampleCount: Int = 0
    private var inputEnded = false
    private var outputBuffer = EMPTY_BUFFER
    private var skipSamples: Int = 0
    
    /**
     * Sets the audio delay in milliseconds.
     * Positive values delay audio (audio plays later).
     * Negative values advance audio (audio plays earlier by skipping initial samples).
     */
    fun setDelayMs(delayMs: Long) {
        Timber.d("AudioDelayProcessor: Setting delay to %d ms", delayMs)
        this.pendingDelayMs = delayMs
    }
    
    fun getDelayMs(): Long = delayMs

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        Timber.d("AudioDelayProcessor: configure called with format: %s", inputAudioFormat)
        
        if (inputAudioFormat.encoding == C.ENCODING_INVALID) {
            return AudioFormat.NOT_SET
        }
        
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        
        // Apply pending delay
        this.delayMs = pendingDelayMs
        
        // Calculate delay in samples
        if (delayMs > 0) {
            // Positive delay: buffer audio samples
            val bytesPerSample = getBytesPerSample(inputAudioFormat.encoding) * inputAudioFormat.channelCount
            val bytesPerMs = (inputAudioFormat.sampleRate * bytesPerSample) / 1000
            delaySamples = (delayMs * bytesPerMs).toInt()
            skipSamples = 0
            Timber.d("AudioDelayProcessor: Delay samples = %d bytes", delaySamples)
        } else if (delayMs < 0) {
            // Negative delay: skip initial samples
            val bytesPerSample = getBytesPerSample(inputAudioFormat.encoding) * inputAudioFormat.channelCount
            val bytesPerMs = (inputAudioFormat.sampleRate * bytesPerSample) / 1000
            skipSamples = (-delayMs * bytesPerMs).toInt()
            delaySamples = 0
            Timber.d("AudioDelayProcessor: Skip samples = %d bytes", skipSamples)
        } else {
            delaySamples = 0
            skipSamples = 0
        }
        
        isActive = delayMs != 0L
        return outputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return
        }
        
        // Handle negative delay (skip samples)
        if (skipSamples > 0) {
            val bytesToSkip = min(skipSamples, inputBuffer.remaining())
            inputBuffer.position(inputBuffer.position() + bytesToSkip)
            skipSamples -= bytesToSkip
            if (!inputBuffer.hasRemaining()) {
                return
            }
        }
        
        // Handle positive delay (buffer samples)
        if (delaySamples > 0) {
            // Add input to delay buffer
            val copy = ByteBuffer.allocateDirect(inputBuffer.remaining())
                .order(ByteOrder.nativeOrder())
            copy.put(inputBuffer)
            copy.flip()
            delayBuffer.offer(copy)
            bufferedSampleCount += copy.remaining()
            
            // Output from delay buffer if we have enough
            if (bufferedSampleCount >= delaySamples) {
                val excess = bufferedSampleCount - delaySamples
                if (excess > 0) {
                    outputFromDelayBuffer(excess)
                }
            }
        } else {
            // No delay, pass through
            val copy = ByteBuffer.allocateDirect(inputBuffer.remaining())
                .order(ByteOrder.nativeOrder())
            copy.put(inputBuffer)
            copy.flip()
            outputBuffer = copy
        }
    }
    
    private fun outputFromDelayBuffer(bytesToOutput: Int) {
        var remaining = bytesToOutput
        val output = ByteBuffer.allocateDirect(bytesToOutput).order(ByteOrder.nativeOrder())
        
        while (remaining > 0 && delayBuffer.isNotEmpty()) {
            val buffer = delayBuffer.peek() ?: break
            val bytesToRead = min(remaining, buffer.remaining())
            
            // Read bytes from buffer
            val oldLimit = buffer.limit()
            buffer.limit(buffer.position() + bytesToRead)
            output.put(buffer)
            buffer.limit(oldLimit)
            
            remaining -= bytesToRead
            bufferedSampleCount -= bytesToRead
            
            if (!buffer.hasRemaining()) {
                delayBuffer.poll()
            }
        }
        
        output.flip()
        outputBuffer = output
    }

    override fun queueEndOfStream() {
        inputEnded = true
        // Flush remaining buffered audio
        if (bufferedSampleCount > 0) {
            outputFromDelayBuffer(bufferedSampleCount)
        }
    }

    override fun getOutput(): ByteBuffer {
        val buffer = outputBuffer
        outputBuffer = EMPTY_BUFFER
        return buffer
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === EMPTY_BUFFER && delayBuffer.isEmpty()

    override fun flush() {
        outputBuffer = EMPTY_BUFFER
        delayBuffer.clear()
        bufferedSampleCount = 0
        inputEnded = false
        
        // Apply pending delay
        delayMs = pendingDelayMs
        
        // Recalculate delay/skip samples based on current format
        if (inputAudioFormat != AudioFormat.NOT_SET) {
            val bytesPerSample = getBytesPerSample(inputAudioFormat.encoding) * inputAudioFormat.channelCount
            val bytesPerMs = (inputAudioFormat.sampleRate * bytesPerSample) / 1000
            
            if (delayMs > 0) {
                // Positive delay: buffer audio samples
                delaySamples = (delayMs * bytesPerMs).toInt()
                skipSamples = 0
                Timber.d("AudioDelayProcessor flush: Delay samples = %d bytes", delaySamples)
            } else if (delayMs < 0) {
                // Negative delay: skip initial samples
                skipSamples = (-delayMs * bytesPerMs).toInt()
                delaySamples = 0
                Timber.d("AudioDelayProcessor flush: Skip samples = %d bytes", skipSamples)
            } else {
                delaySamples = 0
                skipSamples = 0
            }
        }
        
        isActive = delayMs != 0L
        Timber.d("AudioDelayProcessor flush: delay=%d ms, isActive=%b", delayMs, isActive)
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
        isActive = false
        delaySamples = 0
        skipSamples = 0
        delayMs = 0
        pendingDelayMs = 0
    }
    
    private fun getBytesPerSample(encoding: Int): Int {
        return when (encoding) {
            C.ENCODING_PCM_8BIT -> 1
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG_ENDIAN -> 2
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN -> 4
            C.ENCODING_PCM_FLOAT -> 4
            else -> 2 // Default to 16-bit
        }
    }
}
