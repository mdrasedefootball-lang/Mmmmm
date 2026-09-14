# ANISA AI — Android Voice Assistant

ANISA AI is a futuristic, voice-first personal AI assistant application built natively for Android using Kotlin, Jetpack Compose, Material 3, and the Gemini Live API.

---

## 1. Requirements

- **Operating System:** Android 8.0 (API Level 26) or higher
- **Build Tools:** JDK 17+, Android Gradle Plugin 8.9+, Gradle 8.11+
- **Network:** Active internet connection for real-time bidirectional WebSocket audio streaming
- **Microphone:** Hardware microphone for live audio capture
- **API Key:** Google Gemini API Key with access to native audio models (`gemini-2.5-flash-native-audio-preview-12-2025` or `gemini-2.0-flash-exp`)

---

## 2. Android Studio Setup

1. Open Android Studio Ladybug (or newer).
2. Choose **Open an Existing Project** and select this directory.
3. Allow Gradle to synchronize dependencies.
4. Set up an Android Virtual Device (AVD) running API 34+ or connect a physical Android device via USB with USB Debugging enabled.

---

## 3. Gemini Configuration

Anisa uses bidirectional WebSocket streaming with the Gemini Live API:
- **WebSocket Endpoint:** `wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent`
- **Audio Modalities:** Native real-time PCM audio streaming.
- **Audio Input:** 16,000 Hz, 16-bit Mono PCM.
- **Audio Output:** 24,000 Hz, 16-bit Mono PCM streamed through Android `AudioTrack`.
- **Prebuilt Voice:** `Aoede` (expressive, confident, youthful female tone).

---

## 4. API Credential Security

- **Development:** Add `GEMINI_API_KEY=your_key` into `.env` (or use the AI Studio Secrets panel). The Gradle Secrets Plugin automatically injects it into `BuildConfig.GEMINI_API_KEY`.
- **Production Architecture:** For consumer app distribution, do **not** bundle static API keys in the APK. Route WebSocket connections through an authenticated proxy backend or mint short-lived session tokens via Firebase App Check / Cloud Functions.

---

## 5. Android Permissions

The application requests only necessary permissions:
- `android.permission.RECORD_AUDIO`: Required for live microphone audio input.
- `android.permission.INTERNET`: Required to communicate with Gemini Live servers.
- `android.permission.ACCESS_NETWORK_STATE`: Required for connectivity checks.
- `android.permission.MODIFY_AUDIO_SETTINGS`: Required for speech audio routing.

---

## 6. Build Instructions

To build the debug application:

```bash
gradle assembleDebug
```

To run unit and local tests:

```bash
gradle testDebugUnitTest
```

---

## 7. Supported Android Actions (Tools)

Anisa includes real system intent tools with strict security validation:
1. **Website Launcher (`openWebsite`):** Opens URLs in browser. Rejects `javascript:`, `file:`, `data:`, and requires `http://` or `https://`.
2. **YouTube Tool (`openYouTube`):** Opens the YouTube app or searches for query. Falls back to web browser.
3. **WhatsApp Tool (`openWhatsApp`, `sendWhatsAppMessage`):** Launches WhatsApp or prepares pre-filled messages safely without silent transmission.
4. **Google Maps Tool (`openMaps`):** Opens maps or navigates to specified location.
5. **App Launcher (`openApp`):** Whitelisted launch of system apps (Camera, Settings, Gallery, Calculator, Gmail, Chrome).
6. **Phone Dialer (`makePhoneCall`):** Opens the device dialer with confirmation.
7. **Alarm Tool (`setAlarm`):** Opens the clock app to schedule alarms.

---

## 8. Troubleshooting

- **Microphone Denied:** Tap the center Orb or Mic button to trigger the permission dialog. If denied permanently, grant microphone permission in Android Settings > Apps > ANISA AI > Permissions.
- **No Response / Connection Error:** Verify your `GEMINI_API_KEY` is configured and device has active internet access.
- **Echo / Barge-In:** Anisa automatically mutes the input stream while speaking and detects user barge-in interruptions above threshold RMS amplitude to immediately stop playback.
