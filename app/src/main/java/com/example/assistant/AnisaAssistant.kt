package com.example.assistant

import android.content.Context
import com.example.audio.AudioPlayer
import com.example.audio.AudioRecorder
import com.example.gemini.GeminiConfig
import com.example.gemini.GeminiLiveSession
import com.example.tools.ToolManager
import com.example.tools.ToolResult
import com.example.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class AnisaAssistant(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val audioPlayer = AudioPlayer(scope)
    private val audioRecorder = AudioRecorder(context, scope)
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

    val micAmplitude: StateFlow<Float> = audioRecorder.inputAmplitude
    val aiAmplitude: StateFlow<Float> = audioPlayer.outputAmplitude

    init {
        setupSessionCallbacks()
        toolManager.onToolExecutedListener = { result: ToolResult ->
            lastActionMessageInternal.value = result.message
        }
        audioPlayer.setOnPlaybackFinishedListener {
            if (assistantStateInternal.value == AssistantState.SPEAKING) {
                assistantStateInternal.value = AssistantState.LISTENING
                audioRecorder.resumeRecording()
            }
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
                audioRecorder.stopRecording()
                audioPlayer.stopSpeaking()
                if (assistantStateInternal.value != AssistantState.ERROR) {
                    assistantStateInternal.value = AssistantState.DISCONNECTED
                }
            }

            override fun onError(message: String) {
                Logger.e("Assistant error: $message")
                errorMessageInternal.value = message
                assistantStateInternal.value = AssistantState.ERROR
                audioRecorder.stopRecording()
                audioPlayer.stopSpeaking()
            }

            override fun onAudioReceived(pcmAudioChunk: ByteArray) {
                if (assistantStateInternal.value != AssistantState.SPEAKING) {
                    assistantStateInternal.value = AssistantState.SPEAKING
                    // Pause mic or reduce input while AI speaks to prevent echo
                    audioRecorder.pauseRecording()
                }
                audioPlayer.enqueueAudio(pcmAudioChunk)
            }

            override fun onTurnComplete() {
                // AudioPlayer listener handles returning to LISTENING when buffered audio completes
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
        val success = audioRecorder.startRecording { chunk ->
            // Check for user voice barge-in interruption while AI is speaking
            if (assistantStateInternal.value == AssistantState.SPEAKING) {
                val amp = audioRecorder.inputAmplitude.value
                if (amp > 0.45f) {
                    Logger.d("User voice interruption detected (amp: $amp)")
                    interruptAssistant()
                }
            }
            geminiSession.sendAudio(chunk)
        }

        if (!success) {
            errorMessageInternal.value = "Microphone error. Please check permissions."
            assistantStateInternal.value = AssistantState.ERROR
        }
    }

    fun startAssistant(apiKey: String) {
        if (!audioRecorder.hasPermission()) {
            errorMessageInternal.value = "Microphone permission required."
            assistantStateInternal.value = AssistantState.ERROR
            return
        }

        errorMessageInternal.value = null
        geminiSession.connect(apiKey)
    }

    fun stopAssistant() {
        audioRecorder.stopRecording()
        audioPlayer.stopSpeaking()
        geminiSession.disconnect()
        assistantStateInternal.value = AssistantState.DISCONNECTED
        isConnectedInternal.value = false
    }

    fun interruptAssistant() {
        audioPlayer.stopSpeaking()
        geminiSession.interrupt()
        audioRecorder.resumeRecording()
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
        audioRecorder.release()
        audioPlayer.release()
        geminiSession.disconnect()
    }
}
