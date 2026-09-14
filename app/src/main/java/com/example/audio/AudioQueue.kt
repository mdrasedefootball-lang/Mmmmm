package com.example.audio

import com.example.utils.Logger
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class AudioQueue {

    private val queue = ConcurrentLinkedQueue<ByteArray>()
    private val isRunning = AtomicBoolean(false)

    fun enqueueAudio(chunk: ByteArray) {
        if (!isRunning.get()) {
            isRunning.set(true)
        }
        queue.offer(chunk)
    }

    fun pollAudio(): ByteArray? {
        if (!isRunning.get()) return null
        return queue.poll()
    }

    fun startPlayback() {
        isRunning.set(true)
    }

    fun stopPlayback() {
        isRunning.set(false)
        clearQueue()
    }

    fun clearQueue() {
        val count = queue.size
        queue.clear()
        if (count > 0) {
            Logger.d("AudioQueue cleared $count chunks")
        }
    }

    fun isEmpty(): Boolean = queue.isEmpty()

    fun size(): Int = queue.size

    fun release() {
        stopPlayback()
        clearQueue()
    }
}
