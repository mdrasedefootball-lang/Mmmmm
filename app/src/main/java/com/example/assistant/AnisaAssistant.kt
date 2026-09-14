package com.example.assistant

import android.content.Context
import com.example.audio.AudioStreamer
import com.example.gemini.GeminiConfig
import com.example.gemini.GeminiLiveSession
import com.example.tools.ToolManager
import com.example.tools.ToolResult
import com.example.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class AnisaAssistant(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val audioStreamer = AudioStreamer(context, scope)
    private val toolManager = ToolManager(context)
    private val geminiSession = GeminiLiveSession(context, scope)

    private val assistantStateInternal = MutableStateFlow(AssistantState.DISCONNECTED)
    val assistantState: StateFlow<AssistantState> = assistantStateInternal.asStateFlow()

    private val isConnectedInternal = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = isConnectedInternal.asStateFlow()

    private val errorMessageInternal = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = errorMessageInternal.asStateFlow()

    private val lastActionMessageInternal = MutableStateFlow<String?>(null)
    val lastActionMessage: StateFlow<String?> = lastActionMessageInternal.asStateFlow()

    val micAmplitude: StateFlow<Float> = audioStreamer.micAmplitude
    val aiAmplitude: StateFlow<Float> = audioStreamer.playbackAmplitude

    init {
        setupSessionCallbacks()
        toolManager.onToolExecutedListener = { result: ToolResult ->
            lastActionMessageInternal.value = result.message
        }
        audioStreamer.onPlaybackFinished = {
            if (assistantStateInternal.value == AssistantState.SPEAKING) {
                assistantStateInternal.value = AssistantState.LISTENING
                audioStreamer.resumeRecording()
            }
        }
        audioStreamer.onUserBargeIn = {
            interruptAssistant()
        }
    }

    private fun setupSessionCallbacks() {
        geminiSession.callback = object : GeminiLiveSession.SessionCallback {
            override fun onConnected() {
                isConnectedInternal.value = true
                errorMessageInternal.value = null
                assistantStateInternal.value = AssistantState.LISTENING
                startMicrophoneStream()
            }

            override fun onConnecting() {
                assistantStateInternal.value = AssistantState.CONNECTING
                errorMessageInternal.value = null
            }

            override fun onDisconnected() {
                isConnectedInternal.value = false
                audioStreamer.stopRecording()
                audioStreamer.stopPlayback()
                if (assistantStateInternal.value != AssistantState.ERROR) {
                    assistantStateInternal.value = AssistantState.DISCONNECTED
                }
            }

            override fun onError(message: String) {
                Logger.e("Assistant error: $message")
                errorMessageInternal.value = message
                assistantStateInternal.value = AssistantState.ERROR
                audioStreamer.stopRecording()
                audioStreamer.stopPlayback()
            }

            override fun onAudioReceived(pcmAudioChunk: ByteArray) {
                if (assistantStateInternal.value != AssistantState.SPEAKING) {
                    assistantStateInternal.value = AssistantState.SPEAKING
                    // Pause mic or reduce input while AI speaks to prevent echo
                    audioStreamer.pauseRecording()
                }
                audioStreamer.enqueueAudio(pcmAudioChunk)
            }

            override fun onTurnComplete() {
                Logger.d("Gemini turn completed")
            }

            override fun onInterrupted() {
                Logger.d("Interruption signal from Gemini")
                interruptAssistant()
            }

            override fun onToolCall(callId: String, name: String, arguments: JSONObject) {
                scope.launch(Dispatchers.IO) {
                    val result = toolManager.executeTool(name, arguments)
                    val responseJson = JSONObject().apply {
                        put("success", result.success)
                        put("message", result.message)
                    }
                    geminiSession.sendToolResponse(callId, name, responseJson)
                }
            }
        }
    }

    private fun startMicrophoneStream() {
        val success = audioStreamer.startRecording { chunk ->
            geminiSession.sendAudio(chunk)
        }

        if (!success) {
            errorMessageInternal.value = "Microphone error. Please check permissions."
            assistantStateInternal.value = AssistantState.ERROR
        }
    }

    fun startAssistant(apiKey: String) {
        if (!audioStreamer.hasRecordPermission()) {
            errorMessageInternal.value = "Microphone permission required."
            assistantStateInternal.value = AssistantState.ERROR
            return
        }

        errorMessageInternal.value = null
        geminiSession.connect(apiKey)
    }

    fun stopAssistant() {
        audioStreamer.stopRecording()
        audioStreamer.stopPlayback()
        geminiSession.disconnect()
        assistantStateInternal.value = AssistantState.DISCONNECTED
        isConnectedInternal.value = false
    }

    fun interruptAssistant() {
        audioStreamer.interruptPlayback()
        geminiSession.interrupt()
        audioStreamer.resumeRecording()
        assistantStateInternal.value = AssistantState.LISTENING
        Logger.i("Assistant interrupted; switched to LISTENING")
    }

    fun updateConfig(config: GeminiConfig) {
        geminiSession.updateConfig(config)
    }

    fun clearError() {
        errorMessageInternal.value = null
        if (assistantStateInternal.value == AssistantState.ERROR) {
            assistantStateInternal.value = AssistantState.DISCONNECTED
        }
    }

    fun release() {
        audioStreamer.release()
        geminiSession.disconnect()
    }
}
