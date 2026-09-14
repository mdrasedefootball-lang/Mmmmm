package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.example.utils.Logger

class WhatsAppTool(private val context: Context) {

    private fun isWhatsAppInstalled(): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: Exception) {
            // Check WhatsApp Business as secondary
            try {
                context.packageManager.getPackageInfo("com.whatsapp.w4b", PackageManager.GET_ACTIVITIES)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun openWhatsApp(): ToolResult {
        if (!isWhatsAppInstalled()) {
            return ToolResult(
                success = false,
                message = "WhatsApp install করা নেই।"
            )
        }

        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                ?: context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")

            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(launchIntent)
                ToolResult(
                    success = true,
                    message = "WhatsApp খুলে দিয়েছি।",
                    actionType = "openWhatsApp"
                )
            } else {
                ToolResult(
                    success = false,
                    message = "WhatsApp open করতে পারিনি।"
                )
            }
        } catch (e: Exception) {
            Logger.e("Failed to open WhatsApp", e)
            ToolResult(
                success = false,
                message = "WhatsApp open করতে পারিনি।"
            )
        }
    }

    fun sendWhatsAppMessage(phoneNumber: String?, message: String): ToolResult {
        if (!isWhatsAppInstalled()) {
            return ToolResult(
                success = false,
                message = "WhatsApp install করা নেই।"
            )
        }

        return try {
            val formattedNumber = phoneNumber?.replace("[^0-9+]".toRegex(), "") ?: ""
            val encodedMessage = Uri.encode(message)

            val uri = if (formattedNumber.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=$encodedMessage")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMessage")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
            ToolResult(
                success = true,
                message = "WhatsApp মেসেজ প্রস্তুত করা হয়েছে।",
                actionType = "sendWhatsAppMessage"
            )
        } catch (e: Exception) {
            Logger.e("Failed to prepare WhatsApp message", e)
            ToolResult(
                success = false,
                message = "WhatsApp মেসেজ পাঠাতে পারিনি।"
            )
        }
    }
}
