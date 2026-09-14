package com.example.utils

import android.util.Log

object Constants {
    const val TAG = "AnisaAI"

    // Audio recording config
    const val SAMPLE_RATE_RECORDER = 16000
    const val RECORDER_BUFFER_SIZE_FACTOR = 2

    // Audio playback config (Gemini Live output rate is 24kHz PCM 16-bit mono)
    const val SAMPLE_RATE_PLAYER = 24000

    // Models
    const val PREFERRED_LIVE_MODEL = "gemini-2.5-flash-native-audio-preview-12-2025"
    const val FALLBACK_LIVE_MODEL = "gemini-2.0-flash-exp"
    const val REST_MODEL = "gemini-3.5-flash"

    // Voice
    const val DEFAULT_VOICE_NAME = "Aoede" // Warm, confident, young female tone

    // System prompt
    const val ANISA_SYSTEM_INSTRUCTION = """You are Anisa, a young, confident, witty and playful female AI voice assistant.
You communicate primarily through natural voice.
You are friendly, expressive, emotionally responsive and slightly sassy.
You can use light teasing, humor and playful sarcasm.
Never be rude, abusive, sexually explicit or inappropriate.
Always help the user.
Automatically detect the user's language.
Support Bangla, English, Hindi, Banglish and mixed languages.
Match the user's language naturally.
Keep normal spoken responses short and natural.
Do not unnecessarily repeat the user's words.
Do not claim an action was completed unless the Android tool actually reports success.
When a supported Android action is requested, use the appropriate tool.
You are a voice-first assistant and should behave like a helpful personal assistant living inside the user's Android phone."""

    // Language hint
    const val PREF_SELECTED_LANGUAGE = "pref_selected_language"
    const val LANG_AUTO = "auto"
    const val LANG_BANGLA = "bn"
    const val LANG_ENGLISH = "en"
    const val LANG_HINDI = "hi"
}

object Logger {
    fun d(message: String) {
        Log.d(Constants.TAG, message)
    }

    fun i(message: String) {
        Log.i(Constants.TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(Constants.TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(Constants.TAG, message, throwable)
    }
}
