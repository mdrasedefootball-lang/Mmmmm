package com.example.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.gemini.GeminiConfig
import com.example.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val assistant = AnisaAssistant(application.applicationContext, viewModelScope)

    val assistantState: StateFlow<AssistantState> = assistant.assistantState
    val isConnected: StateFlow<Boolean> = assistant.isConnected
    val errorMessage: StateFlow<String?> = assistant.errorMessage
    val lastActionMessage: StateFlow<String?> = assistant.lastActionMessage

    val micAmplitude: StateFlow<Float> = assistant.micAmplitude
    val aiAmplitude: StateFlow<Float> = assistant.aiAmplitude

    private val selectedLanguageInternal = MutableStateFlow(Constants.LANG_AUTO)
    val selectedLanguage: StateFlow<String> = selectedLanguageInternal.asStateFlow()

    private val animationIntensityInternal = MutableStateFlow(1.0f)
    val animationIntensity: StateFlow<Float> = animationIntensityInternal.asStateFlow()

    private val activeApiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

    fun startAssistant() {
        assistant.updateConfig(
            GeminiConfig(
                languageMode = selectedLanguageInternal.value
            )
        )
        assistant.startAssistant(activeApiKey)
    }

    fun stopAssistant() {
        assistant.stopAssistant()
    }

    fun toggleAssistant() {
        when (assistantState.value) {
            AssistantState.DISCONNECTED, AssistantState.ERROR -> startAssistant()
            AssistantState.SPEAKING -> interruptAssistant()
            AssistantState.LISTENING, AssistantState.CONNECTING -> stopAssistant()
        }
    }

    fun interruptAssistant() {
        assistant.interruptAssistant()
    }

    fun clearError() {
        assistant.clearError()
    }

    fun setLanguage(langCode: String) {
        selectedLanguageInternal.value = langCode
        assistant.updateConfig(
            GeminiConfig(
                languageMode = langCode
            )
        )
    }

    fun setAnimationIntensity(intensity: Float) {
        animationIntensityInternal.value = intensity.coerceIn(0.2f, 1.5f)
    }

    override fun onCleared() {
        super.onCleared()
        assistant.release()
    }
}
