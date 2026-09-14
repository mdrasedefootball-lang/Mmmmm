package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.utils.Logger
import java.util.Locale

class WebsiteLauncher(private val context: Context) {

    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim().lowercase(Locale.ROOT)

        // Strict whitelist of schemes
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            return false
        }

        // Explicit blacklists for safety
        val forbiddenSchemes = listOf("javascript:", "file:", "data:", "content:", "intent:", "about:")
        if (forbiddenSchemes.any { trimmed.startsWith(it) }) {
            return false
        }

        return try {
            val uri = Uri.parse(trimmed)
            val scheme = uri.scheme?.lowercase(Locale.ROOT)
            (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    fun openWebsite(rawUrl: String): ToolResult {
        var url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        if (!isValidUrl(url)) {
            Logger.w("Rejected invalid or dangerous URL: $rawUrl")
            return ToolResult(
                success = false,
                message = "Invalid or unsupported URL. Only https:// and http:// web addresses are allowed."
            )
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ToolResult(
                success = true,
                message = "Opened website: $url",
                actionType = "openWebsite"
            )
        } catch (e: Exception) {
            Logger.e("Failed to open website: $url", e)
            ToolResult(
                success = false,
                message = "Could not open browser for $url: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
