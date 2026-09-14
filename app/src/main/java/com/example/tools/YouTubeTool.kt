package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.utils.Logger

class YouTubeTool(private val context: Context) {

    fun openYouTube(query: String? = null): ToolResult {
        return try {
            val targetUri = if (!query.isNullOrBlank()) {
                Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query.trim())}")
            } else {
                Uri.parse("https://www.youtube.com")
            }

            // Try YouTube App Intent
            val appIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                setPackage("com.google.android.youtube")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val pm = context.packageManager
            if (appIntent.resolveActivity(pm) != null) {
                context.startActivity(appIntent)
                return ToolResult(
                    success = true,
                    message = if (!query.isNullOrBlank()) "Opened YouTube search for '$query'" else "Opened YouTube app",
                    actionType = "openYouTube"
                )
            }

            // Fallback to browser
            val webIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            ToolResult(
                success = true,
                message = "YouTube app not found, opened in web browser",
                actionType = "openYouTube"
            )
        } catch (e: Exception) {
            Logger.e("Failed to open YouTube", e)
            ToolResult(
                success = false,
                message = "YouTube open করতে পারিনি।"
            )
        }
    }
}
