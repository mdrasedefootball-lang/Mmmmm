package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.utils.Constants
import com.example.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AudioPlayer(
    private val scope: CoroutineScope
) {

    private val audioQueue = AudioQueue()
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val isPlayingState = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = isPlayingState.asStateFlow()

    private val outputAmplitudeState = MutableStateFlow(0f)
    val outputAmplitude: StateFlow<Float> = outputAmplitudeState.asStateFlow()

    private val isInterrupted = AtomicBoolean(false)
    private var onPlaybackFinished: (() -> Unit)? = null

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            val sampleRate = Constants.SAMPLE_RATE_PLAYER
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val format = AudioFormat.Builder()
                .setEncoding(audioFormat)
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfig)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            Logger.d("AudioTrack initialized at $sampleRate Hz, bufferSize: $bufferSize")
        } catch (e: Exception) {
            Logger.e("Failed to initialize AudioTrack", e)
        }
    }

    fun setOnPlaybackFinishedListener(listener: () -> Unit) {
        onPlaybackFinished = listener
    }

    fun enqueueAudio(chunk: ByteArray) {
        isInterrupted.set(false)
        audioQueue.enqueueAudio(chunk)
        ensurePlaybackLoop()
    }

    private fun ensurePlaybackLoop() {
        if (playbackJob?.isActive == true) return

        playbackJob = scope.launch(Dispatchers.IO) {
            val track = audioTrack ?: return@launch
            try {
                if (track.state != AudioTrack.STATE_INITIALIZED) {
                    initAudioTrack()
                }
                if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    track.play()
                }

                audioQueue.startPlayback()
                isPlayingState.value = true

                var emptyPollCount = 0
                while (isActive && !isInterrupted.get()) {
                    val chunk = audioQueue.pollAudio()
                    if (chunk != null) {
                        emptyPollCount = 0
                        // Calculate amplitude for waveform
                        val amp = PcmUtils.calculateRmsAmplitude(chunk)
                        outputAmplitudeState.value = amp

                        // Write to AudioTrack in streaming mode
                        var bytesWritten = 0
                        while (bytesWritten < chunk.size && isActive && !isInterrupted.get()) {
                            val result = track.write(
                                chunk,
                                bytesWritten,
                                chunk.size - bytesWritten,
                                AudioTrack.WRITE_BLOCKING
                            )
                            if (result < 0) {
                                Logger.w("AudioTrack write returned error: $result")
                                break
                            }
                            bytesWritten += result
                        }
                    } else {
                        // Queue temporarily empty
                        outputAmplitudeState.value = 0f
                        emptyPollCount++
                        if (emptyPollCount > 15) {
                            // Waited ~150ms with no incoming audio chunks
                            break
                        }
                        delay(10)
                    }
                }
            } catch (e: Exception) {
                Logger.e("Error during AudioTrack playback", e)
            } finally {
                outputAmplitudeState.value = 0f
                isPlayingState.value = false
                onPlaybackFinished?.invoke()
            }
        }
    }

    /**
     * Interruption mechanism:
     * Immediately stops AudioTrack, flushes audio buffers, clears queue,
     * resets playback job and amplitude.
     */
    fun stopSpeaking() {
        isInterrupted.set(true)
        audioQueue.clearQueue()
        playbackJob?.cancel()
        playbackJob = null

        try {
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.flush()
                }
            }
        } catch (e: Exception) {
            Logger.w("Error flushing audio track", e)
        }

        outputAmplitudeState.value = 0f
        isPlayingState.value = false
        Logger.d("AudioPlayer stopped and flushed")
    }

    fun clearAudioQueue() {
        audioQueue.clearQueue()
    }

    fun release() {
        stopSpeaking()
        audioQueue.release()
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Logger.w("Error releasing AudioTrack", e)
        }
        audioTrack = null
    }
}
