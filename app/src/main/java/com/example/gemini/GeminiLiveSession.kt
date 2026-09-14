package com.example.gemini

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import com.example.utils.Constants
import com.example.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GeminiLiveSession(
    private val context: Context,
    private val scope: CoroutineScope,
    private var config: GeminiConfig = GeminiConfig()
) {

    interface SessionCallback {
        fun onConnected()
        fun onConnecting()
        fun onDisconnected()
        fun onError(message: String)
        fun onAudioReceived(pcmAudioChunk: ByteArray)
        fun onTurnComplete()
        fun onInterrupted()
        fun onToolCall(callId: String, name: String, arguments: JSONObject)
    }

    var callback: SessionCallback? = null

    private var webSocket: WebSocket? = null
    private val isConnectedState = AtomicBoolean(false)
    private val isConnectingState = AtomicBoolean(false)
    private var currentModelIndex = 0
    private val modelCandidates = listOf(
        Constants.PREFERRED_LIVE_MODEL,
        Constants.FALLBACK_LIVE_MODEL,
        "gemini-2.0-flash"
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for WebSockets
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    fun isConnected(): Boolean = isConnectedState.get()

    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    fun updateConfig(newConfig: GeminiConfig) {
        config = newConfig
    }

    fun connect(apiKey: String) {
        if (isConnectedState.get() || isConnectingState.get()) {
            Logger.d("Session already connected or connecting")
            return
        }

        if (!isNetworkAvailable()) {
            callback?.onError("Internet connection নেই। Connection ঠিক করে আবার try করো।")
            return
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            callback?.onError("Gemini API Key missing or invalid. Please check your AI Studio secrets configuration.")
            return
        }

        isConnectingState.set(true)
        callback?.onConnecting()

        val activeModel = modelCandidates.getOrElse(currentModelIndex) { Constants.PREFERRED_LIVE_MODEL }
        config = config.copy(model = activeModel)
        val url = config.buildWebSocketUrl(apiKey)

        Logger.i("Connecting to Gemini Live with model: $activeModel")

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Logger.i("Gemini Live WebSocket opened successfully")
                isConnectingState.set(false)
                isConnectedState.set(true)

                // Send initial setup payload
                val setupPayload = config.buildSetupMessage()
                Logger.d("Sending setup payload: $setupPayload")
                webSocket.send(setupPayload)

                scope.launch(Dispatchers.Main) {
                    callback?.onConnected()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Logger.d("WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Logger.i("WebSocket closed: $code / $reason")
                isConnectingState.set(false)
                isConnectedState.set(false)
                scope.launch(Dispatchers.Main) {
                    callback?.onDisconnected()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorBody = try { response?.body?.string() } catch (e: Exception) { null }
                val errorMsg = t.localizedMessage ?: "Unknown error"
                Logger.e("Gemini Live WebSocket failure: $errorMsg, resp: $errorBody", t)

                isConnectingState.set(false)
                isConnectedState.set(false)

                scope.launch(Dispatchers.Main) {
                    // If model failed, attempt next candidate
                    if (currentModelIndex < modelCandidates.size - 1 && !isNetworkAvailable().not()) {
                        currentModelIndex++
                        Logger.i("Retrying connection with fallback model: ${modelCandidates[currentModelIndex]}")
                        connect(apiKey)
                    } else {
                        currentModelIndex = 0
                        val displayMsg = if (!isNetworkAvailable()) {
                            "Internet connection নেই। Connection ঠিক করে আবার try করো।"
                        } else {
                            "Connection failed: $errorMsg"
                        }
                        callback?.onError(displayMsg)
                        callback?.onDisconnected()
                    }
                }
            }
        })
    }

    private fun handleServerMessage(jsonText: String) {
        try {
            val root = JSONObject(jsonText)

            // 1. Check for serverContent
            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    scope.launch(Dispatchers.Main) {
                        callback?.onInterrupted()
                    }
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val base64Audio = inlineData.optString("data", "")
                                if (base64Audio.isNotEmpty()) {
                                    val pcmBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                                    scope.launch(Dispatchers.Main) {
                                        callback?.onAudioReceived(pcmBytes)
                                    }
                                }
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    scope.launch(Dispatchers.Main) {
                        callback?.onTurnComplete()
                    }
                }
            }

            // 2. Check for toolCall
            if (root.has("toolCall")) {
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val callId = call.optString("id", "")
                        val name = call.optString("name", "")
                        val args = call.optJSONObject("args") ?: JSONObject()

                        scope.launch(Dispatchers.Main) {
                            callback?.onToolCall(callId, name, args)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("Error parsing server message", e)
        }
    }

    fun sendAudio(pcmChunk: ByteArray) {
        if (!isConnectedState.get()) return
        val base64 = Base64.encodeToString(pcmChunk, Base64.NO_WRAP)
        val msg = config.buildAudioChunkMessage(base64)
        webSocket?.send(msg)
    }

    fun sendToolResponse(callId: String, name: String, resultJson: JSONObject) {
        if (!isConnectedState.get()) return
        val msg = config.buildToolResponseMessage(callId, name, resultJson)
        Logger.d("Sending tool response for $name: $msg")
        webSocket?.send(msg)
    }

    fun interrupt() {
        // Stop any currently received playback buffers locally
        // and optionally send client-side turn indicator if needed
        Logger.d("GeminiLiveSession interrupt triggered")
    }

    fun disconnect() {
        isConnectedState.set(false)
        isConnectingState.set(false)
        try {
            webSocket?.close(1000, "Session ended by user")
        } catch (e: Exception) {
            Logger.w("Error closing WebSocket", e)
        }
        webSocket = null
        callback?.onDisconnected()
    }
}
