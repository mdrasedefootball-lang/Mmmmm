package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.utils.Logger
import java.util.Locale

class AppLauncher(private val context: Context) {

    companion object {
        val SUPPORTED_APP_WHITELIST = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "mail" to "com.google.android.gm",
            "settings" to "action:settings",
            "camera" to "action:camera",
            "gallery" to "action:gallery",
            "photos" to "action:gallery",
            "calculator" to "action:calculator"
        )
    }

    fun isAppSupported(appName: String): Boolean {
        val key = appName.trim().lowercase(Locale.ROOT)
        return SUPPORTED_APP_WHITELIST.containsKey(key) ||
                SUPPORTED_APP_WHITELIST.values.contains(appName.trim())
    }

    fun openApp(appNameOrPackage: String): ToolResult {
        val raw = appNameOrPackage.trim().lowercase(Locale.ROOT)
        val target = SUPPORTED_APP_WHITELIST[raw] ?: if (SUPPORTED_APP_WHITELIST.values.contains(appNameOrPackage.trim())) appNameOrPackage.trim() else null

        if (target == null) {
            Logger.w("Blocked attempt to launch non-whitelisted app: $appNameOrPackage")
            return ToolResult(
                success = false,
                message = "নিরাপত্তার জন্য '$appNameOrPackage' অ্যাপটি সাপোর্ট লিস্টে নেই।"
            )
        }

        return when {
            target == "action:settings" -> openSettings()
            target == "action:camera" -> openCamera()
            target == "action:gallery" -> openGallery()
            target == "action:calculator" -> openCalculator()
            else -> launchPackage(target, appNameOrPackage)
        }
    }

    private fun launchPackage(packageName: String, displayName: String): ToolResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                ToolResult(
                    success = true,
                    message = "$displayName খুলে দিয়েছি।",
                    actionType = "openApp"
                )
            } else {
                ToolResult(
                    success = false,
                    message = "$displayName ফোনে ইন্সটল করা নেই।"
                )
            }
        } catch (e: Exception) {
            Logger.e("Failed to launch $packageName", e)
            ToolResult(
                success = false,
                message = "$displayName খুলতে সমস্যা হয়েছে।"
            )
        }
    }

    fun openSettings(): ToolResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult(success = true, message = "Settings খোলা হয়েছে।", actionType = "openSettings")
        } catch (e: Exception) {
            Logger.e("Failed to open Settings", e)
            ToolResult(success = false, message = "Settings open করতে পারিনি।")
        }
    }

    fun openCamera(): ToolResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult(success = true, message = "Camera খোলা হয়েছে।", actionType = "openCamera")
            } else {
                ToolResult(success = false, message = "Camera অ্যাপ পাওয়া যায়নি।")
            }
        } catch (e: Exception) {
            Logger.e("Failed to open Camera", e)
            ToolResult(success = false, message = "Camera open করতে পারিনি।")
        }
    }

    fun openGallery(): ToolResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult(success = true, message = "Gallery খোলা হয়েছে।", actionType = "openGallery")
        } catch (e: Exception) {
            Logger.e("Failed to open Gallery", e)
            ToolResult(success = false, message = "Gallery open করতে পারিনি।")
        }
    }

    fun openCalculator(): ToolResult {
        val calcPackages = listOf(
            "com.google.android.calculator",
            "com.android.calculator2",
            "com.sec.android.app.popupcalculator"
        )
        for (pkg in calcPackages) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return ToolResult(success = true, message = "Calculator খোলা হয়েছে।", actionType = "openCalculator")
            }
        }
        return ToolResult(success = false, message = "Calculator অ্যাপ খুঁজে পাওয়া যায়নি।")
    }

    fun makePhoneCall(phoneNumber: String): ToolResult {
        return try {
            val sanitized = phoneNumber.replace("[^0-9+*#]".toRegex(), "")
            if (sanitized.isBlank()) {
                return ToolResult(success = false, message = "সঠিক ফোন নম্বর প্রদান করুন।")
            }
            // Prefer ACTION_DIAL for user security and confirmation
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
            ToolResult(
                success = true,
                message = "$sanitized ডায়াল স্ক্রিনে প্রস্তুত করা হয়েছে।",
                actionType = "makePhoneCall"
            )
        } catch (e: Exception) {
            Logger.e("Failed to dial phone number", e)
            ToolResult(success = false, message = "কল ডায়াল করতে পারিনি।")
        }
    }

    fun setAlarm(hour: Int?, minutes: Int?, message: String?): ToolResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (hour != null) putExtra(AlarmClock.EXTRA_HOUR, hour)
                if (minutes != null) putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                if (!message.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ToolResult(
                    success = true,
                    message = "অ্যালার্ম সেট করার স্ক্রিন খোলা হয়েছে।",
                    actionType = "setAlarm"
                )
            } else {
                ToolResult(success = false, message = "অ্যালার্ম অ্যাপ পাওয়া যায়নি।")
            }
        } catch (e: Exception) {
            Logger.e("Failed to set alarm", e)
            ToolResult(success = false, message = "অ্যালার্ম সেট করতে পারিনি।")
        }
    }
}
