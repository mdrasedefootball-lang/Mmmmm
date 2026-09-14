package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.example.utils.Constants
import com.example.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecorder(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val isRecordingState = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = isRecordingState.asStateFlow()

    private val inputAmplitudeState = MutableStateFlow(0f)
    val inputAmplitude: StateFlow<Float> = inputAmplitudeState.asStateFlow()

    private val isPaused = AtomicBoolean(false)

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startRecording(onAudioChunk: (ByteArray) -> Unit): Boolean {
        if (!hasPermission()) {
            Logger.w("Cannot start recording: RECORD_AUDIO permission missing")
            return false
        }

        if (recordingJob?.isActive == true) {
            isPaused.set(false)
            return true
        }

        val sampleRate = Constants.SAMPLE_RATE_RECORDER
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Logger.e("Invalid buffer size for AudioRecord: $minBufferSize")
            return false
        }

        val bufferSize = (minBufferSize * Constants.RECORDER_BUFFER_SIZE_FACTOR).coerceAtLeast(2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to MIC if VOICE_COMMUNICATION fails
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Logger.e("AudioRecord failed to initialize")
                return false
            }

            audioRecord?.startRecording()
            isRecordingState.value = true
            isPaused.set(false)

            recordingJob = scope.launch(Dispatchers.IO) {
                // Read chunks of 100ms: 16000 samples/sec * 0.1s * 2 bytes/sample = 3200 bytes
                val chunkSizeBytes = 3200
                val buffer = ByteArray(chunkSizeBytes)

                Logger.d("AudioRecorder recording loop started")
                while (isActive && isRecordingState.value) {
                    if (isPaused.get()) {
                        inputAmplitudeState.value = 0f
                        kotlinx.coroutines.delay(50)
                        continue
                    }

                    val record = audioRecord ?: break
                    val readBytes = record.read(buffer, 0, buffer.size)

                    if (readBytes > 0) {
                        val chunkCopy = buffer.copyOf(readBytes)
                        val amp = PcmUtils.calculateRmsAmplitude(chunkCopy)
                        inputAmplitudeState.value = amp
                        onAudioChunk(chunkCopy)
                    } else if (readBytes < 0) {
                        Logger.w("AudioRecord read error: $readBytes")
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Logger.e("Exception while starting AudioRecord", e)
            stopRecording()
            return false
        }
    }

    fun pauseRecording() {
        isPaused.set(true)
        inputAmplitudeState.value = 0f
    }

    fun resumeRecording() {
        isPaused.set(false)
    }

    fun stopRecording() {
        isRecordingState.value = false
        recordingJob?.cancel()
        recordingJob = null
        inputAmplitudeState.value = 0f

        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Logger.w("Error stopping/releasing AudioRecord", e)
        }
        audioRecord = null
        Logger.d("AudioRecorder stopped")
    }

    fun release() {
        stopRecording()
    }
}
