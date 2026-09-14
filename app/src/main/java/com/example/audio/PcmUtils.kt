package com.example.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object PcmUtils {

    /**
     * Calculates the Root Mean Square (RMS) amplitude of a 16-bit mono PCM byte array.
     * Returns a normalized value between 0.0f and 1.0f.
     */
    fun calculateRmsAmplitude(pcmBytes: ByteArray, length: Int = pcmBytes.size): Float {
        if (length < 2) return 0.0f

        val shortCount = length / 2
        var sumSquares = 0.0

        for (i in 0 until shortCount) {
            val low = pcmBytes[i * 2].toInt() and 0xFF
            val high = pcmBytes[i * 2 + 1].toInt()
            val sample = (high shl 8) or low
            sumSquares += sample.toDouble() * sample.toDouble()
        }

        val rms = sqrt(sumSquares / shortCount)
        // Normalize against max amplitude of 16-bit PCM (32767)
        val normalized = (rms / 32767.0).toFloat()
        return min(1.0f, max(0.0f, normalized * 3.5f)) // slight boost for natural sensitivity
    }

    /**
     * Helper to wrap raw PCM bytes into ShortArray.
     */
    fun bytesToShorts(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): ShortArray {
        val shortCount = length / 2
        val shorts = ShortArray(shortCount)
        val buffer = ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.LITTLE_ENDIAN)
        buffer.asShortBuffer().get(shorts)
        return shorts
    }

    /**
     * Helper to wrap ShortArray into ByteArray.
     */
    fun shortsToBytes(shorts: ShortArray): ByteArray {
        val bytes = ByteArray(shorts.size * 2)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.asShortBuffer().put(shorts)
        return bytes
    }
}
