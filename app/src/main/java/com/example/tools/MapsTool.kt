package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.utils.Logger

class MapsTool(private val context: Context) {

    fun openMaps(query: String? = null): ToolResult {
        return try {
            val uri = if (!query.isNullOrBlank()) {
                Uri.parse("geo:0,0?q=${Uri.encode(query.trim())}")
            } else {
                Uri.parse("geo:0,0?q=")
            }

            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val pm = context.packageManager
            if (mapIntent.resolveActivity(pm) != null) {
                context.startActivity(mapIntent)
                return ToolResult(
                    success = true,
                    message = if (!query.isNullOrBlank()) "Google Maps-এ $query দেখানো হচ্ছে।" else "Google Maps খোলা হয়েছে।",
                    actionType = "openMaps"
                )
            }

            // Fallback to browser Google Maps
            val webUri = if (!query.isNullOrBlank()) {
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query.trim())}")
            } else {
                Uri.parse("https://www.google.com/maps")
            }

            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)

            ToolResult(
                success = true,
                message = "Google Maps ব্রাউজারে খোলা হয়েছে।",
                actionType = "openMaps"
            )
        } catch (e: Exception) {
            Logger.e("Failed to open Google Maps", e)
            ToolResult(
                success = false,
                message = "Google Maps open করতে পারিনি।"
            )
        }
    }
}
