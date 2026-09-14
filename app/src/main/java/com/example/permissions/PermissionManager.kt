package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionManager {

    const val RECORD_AUDIO = Manifest.permission.RECORD_AUDIO

    fun hasRecordAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    const val PERMISSION_DENIED_MESSAGE_BANGLA =
        "Anisa চালাতে microphone permission দরকার। Permission-টা allow করে আবার try করো।"

    const val PERMISSION_DENIED_MESSAGE_ENGLISH =
        "Microphone permission is required to talk to Anisa. Please grant permission to continue."
}
