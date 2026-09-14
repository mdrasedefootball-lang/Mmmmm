package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.tools.AppLauncher
import com.example.tools.WebsiteLauncher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ANISA AI", appName)
  }

  @Test
  fun `website launcher rejects dangerous schemes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val launcher = WebsiteLauncher(context)

    assertFalse(launcher.isValidUrl("javascript:alert(1)"))
    assertFalse(launcher.isValidUrl("file:///etc/passwd"))
    assertFalse(launcher.isValidUrl("data:text/html;base64,PHNjcmlwdD4="))
    assertFalse(launcher.isValidUrl("content://media/external"))
    assertTrue(launcher.isValidUrl("https://www.google.com"))
    assertTrue(launcher.isValidUrl("http://example.com"))
  }

  @Test
  fun `app launcher verifies whitelist`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val launcher = AppLauncher(context)

    assertTrue(launcher.isAppSupported("youtube"))
    assertTrue(launcher.isAppSupported("whatsapp"))
    assertTrue(launcher.isAppSupported("camera"))
    assertFalse(launcher.isAppSupported("malicious_unknown_package"))
  }

  @Test
  fun `audio queue enqueues and polls correctly`() {
    val queue = com.example.audio.AudioQueue()
    assertTrue(queue.isEmpty())

    val chunk1 = ByteArray(100) { 1 }
    val chunk2 = ByteArray(100) { 2 }

    queue.enqueueAudio(chunk1)
    queue.enqueueAudio(chunk2)

    assertEquals(2, queue.size())
    assertFalse(queue.isEmpty())

    val polled1 = queue.pollAudio()
    org.junit.Assert.assertNotNull(polled1)
    assertEquals(1.toByte(), polled1!![0])

    queue.clearQueue()
    assertTrue(queue.isEmpty())
    org.junit.Assert.assertNull(queue.pollAudio())
  }

  @Test
  fun `gemini config creates valid setup message`() {
    val config = com.example.gemini.GeminiConfig()
    val setupJsonStr = config.buildSetupMessage()
    val root = org.json.JSONObject(setupJsonStr)

    assertTrue(root.has("setup"))
    val setup = root.getJSONObject("setup")
    assertTrue(setup.has("model"))
    assertTrue(setup.has("generationConfig"))
    assertTrue(setup.has("tools"))
  }
}

