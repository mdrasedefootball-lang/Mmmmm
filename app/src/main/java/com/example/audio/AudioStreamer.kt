package com.example.audio

import android.content.Context
import com.example.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * AudioStreamer cleanly encapsulates both audio input streaming (PCM 16-bit 16kHz)
 * from the microphone and audio output streaming (PCM 16-bit 24kHz) to the device speaker,
 * as well as real-time barge-in interruption detection.
 */
class AudioStreamer(
    context: Context,
    scope: CoroutineScope
) {
    private val recorder = AudioRecorder(context, scope)
    private val player = AudioPlayer(scope)

    val micAmplitude: StateFlow<Float> = recorder.inputAmplitude
    val playbackAmplitude: StateFlow<Float> = player.outputAmplitude
    val isRecording: StateFlow<Boolean> = recorder.isRecording
    val isPlaying: StateFlow<Boolean> = player.isPlaying

    var onUserBargeIn: (() -> Unit)? = null
    var onPlaybackFinished: (() -> Unit)? = null

    init {
        player.setOnPlaybackFinishedListener {
            onPlaybackFinished?.invoke()
        }
    }

    fun hasRecordPermission(): Boolean = recorder.hasPermission()

    /**
     * Starts streaming PCM16 16kHz mic audio.
     */
    fun startRecording(onAudioChunk: (ByteArray) -> Unit): Boolean {
        return recorder.startRecording { chunk ->
            // Check for user voice barge-in interruption when playback is active
            if (player.isPlaying.value) {
                val amp = recorder.inputAmplitude.value
                if (amp > 0.40f) {
                    Logger.d("AudioStreamer: User voice barge-in detected (amplitude: $amp)")
                    interruptPlayback()
                    onUserBargeIn?.invoke()
                }
            }
            onAudioChunk(chunk)
        }
    }

    fun stopRecording() {
        recorder.stopRecording()
    }

    fun pauseRecording() {
        recorder.pauseRecording()
    }

    fun resumeRecording() {
        recorder.resumeRecording()
    }

    /**
     * Enqueues received PCM 24kHz audio from Gemini Live for smooth streaming playback.
     */
    fun enqueueAudio(pcmChunk: ByteArray) {
        player.enqueueAudio(pcmChunk)
    }

    /**
     * Instantly halts playback, drains audio buffers, and resets playback state.
     */
    fun interruptPlayback() {
        player.stopSpeaking()
        Logger.d("AudioStreamer: Playback interrupted and buffer cleared")
    }

    fun stopPlayback() {
        player.stopSpeaking()
    }

    fun release() {
        recorder.release()
        player.release()
    }
}
