package com.example.gemini

import com.example.utils.Constants
import org.json.JSONArray
import org.json.JSONObject

data class GeminiConfig(
    val model: String = Constants.PREFERRED_LIVE_MODEL,
    val voiceName: String = Constants.DEFAULT_VOICE_NAME,
    val languageMode: String = Constants.LANG_AUTO
) {

    fun buildWebSocketUrl(apiKey: String): String {
        return "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
    }

    /**
     * Builds the Gemini Live initialization "setup" JSON message.
     */
    fun buildSetupMessage(): String {
        val root = JSONObject()
        val setup = JSONObject()

        setup.put("model", "models/$model")

        // Generation config with AUDIO modality and voice specification
        val generationConfig = JSONObject().apply {
            put("responseModalities", JSONArray().put("AUDIO"))
            put("speechConfig", JSONObject().apply {
                put("voiceConfig", JSONObject().apply {
                    put("prebuiltVoiceConfig", JSONObject().apply {
                        put("voiceName", voiceName)
                    })
                })
            })
        }
        setup.put("generationConfig", generationConfig)

        // System instruction
        val effectiveInstruction = when (languageMode) {
            Constants.LANG_BANGLA -> "${Constants.ANISA_SYSTEM_INSTRUCTION}\nAlways speak and reply in Bengali (Bangla) unless the user requests otherwise."
            Constants.LANG_HINDI -> "${Constants.ANISA_SYSTEM_INSTRUCTION}\nAlways speak and reply in Hindi unless the user requests otherwise."
            Constants.LANG_ENGLISH -> "${Constants.ANISA_SYSTEM_INSTRUCTION}\nAlways speak and reply in English unless the user requests otherwise."
            else -> Constants.ANISA_SYSTEM_INSTRUCTION
        }

        val systemInstruction = JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().apply {
                put("text", effectiveInstruction)
            }))
        }
        setup.put("systemInstruction", systemInstruction)

        // Tools
        val toolsArray = JSONArray().apply {
            put(JSONObject().apply {
                put("functionDeclarations", GeminiTools.getFunctionDeclarations())
            })
        }
        setup.put("tools", toolsArray)

        root.put("setup", setup)
        return root.toString()
    }

    /**
     * Packages a 16000Hz PCM audio chunk into the Gemini Live realtimeInput JSON.
     */
    fun buildAudioChunkMessage(base64PcmAudio: String): String {
        val root = JSONObject()
        val realtimeInput = JSONObject()
        val mediaChunks = JSONArray()

        val chunk = JSONObject().apply {
            put("mimeType", "audio/pcm;rate=16000")
            put("data", base64PcmAudio)
        }
        mediaChunks.put(chunk)
        realtimeInput.put("mediaChunks", mediaChunks)
        root.put("realtimeInput", realtimeInput)

        return root.toString()
    }

    /**
     * Packages a function call execution response back to Gemini Live.
     */
    fun buildToolResponseMessage(callId: String, name: String, responseJson: JSONObject): String {
        val root = JSONObject()
        val toolResponse = JSONObject()
        val functionResponses = JSONArray()

        val resp = JSONObject().apply {
            put("id", callId)
            put("name", name)
            put("response", JSONObject().apply {
                put("output", responseJson)
            })
        }
        functionResponses.put(resp)
        toolResponse.put("functionResponses", functionResponses)
        root.put("toolResponse", toolResponse)

        return root.toString()
    }
}
