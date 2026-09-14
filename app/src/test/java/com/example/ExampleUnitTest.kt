package com.example

import com.example.audio.PcmUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `pcm utils calculates rms amplitude within valid range`() {
    val silentBytes = ByteArray(200) { 0 }
    val silentAmp = PcmUtils.calculateRmsAmplitude(silentBytes)
    assertEquals(0.0f, silentAmp, 0.001f)

    // Generate simulated audio wave
    val waveBytes = ByteArray(200)
    for (i in waveBytes.indices step 2) {
      waveBytes[i] = 0x00
      waveBytes[i + 1] = 0x20 // ~8192 amplitude
    }
    val waveAmp = PcmUtils.calculateRmsAmplitude(waveBytes)
    assertTrue(waveAmp > 0f)
    assertTrue(waveAmp <= 1.0f)
  }

  @Test
  fun `pcm utils shorts to bytes and back`() {
    val originalShorts = shortArrayOf(0, 100, -200, 32000, -32000)
    val bytes = PcmUtils.shortsToBytes(originalShorts)
    val reconstructed = PcmUtils.bytesToShorts(bytes)
    assertEquals(originalShorts.size, reconstructed.size)
    for (i in originalShorts.indices) {
      assertEquals(originalShorts[i], reconstructed[i])
    }
  }
}


