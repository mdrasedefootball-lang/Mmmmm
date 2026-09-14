package com.example.tools

import android.content.Context
import com.example.utils.Logger
import org.json.JSONObject

class ToolManager(private val context: Context) {

    private val websiteLauncher = WebsiteLauncher(context)
    private val youTubeTool = YouTubeTool(context)
    private val whatsAppTool = WhatsAppTool(context)
    private val mapsTool = MapsTool(context)
    private val appLauncher = AppLauncher(context)

    var onToolExecutedListener: ((ToolResult) -> Unit)? = null

    fun executeTool(toolName: String, arguments: JSONObject): ToolResult {
        Logger.i("Executing tool: $toolName with arguments: $arguments")

        val result = try {
            when (toolName) {
                "openWebsite" -> {
                    val url = arguments.optString("url", "")
                    if (url.isBlank()) {
                        ToolResult(false, "Website URL is missing")
                    } else {
                        websiteLauncher.openWebsite(url)
                    }
                }

                "openYouTube" -> {
                    val query = arguments.optString("query", "").takeIf { it.isNotBlank() }
                    youTubeTool.openYouTube(query)
                }

                "openWhatsApp" -> {
                    whatsAppTool.openWhatsApp()
                }

                "sendWhatsAppMessage" -> {
                    val phone = arguments.optString("phoneNumber", "").takeIf { it.isNotBlank() }
                    val message = arguments.optString("message", "")
                    if (message.isBlank()) {
                        ToolResult(false, "মেসেজ লিখুন।")
                    } else {
                        whatsAppTool.sendWhatsAppMessage(phone, message)
                    }
                }

                "openMaps" -> {
                    val locationArg = arguments.optString("location", "")
                    val queryArg = arguments.optString("query", "")
                    val query = (if (locationArg.isNotBlank()) locationArg else queryArg).takeIf { it.isNotBlank() }
                    mapsTool.openMaps(query)
                }

                "openApp" -> {
                    val appNameArg = arguments.optString("appName", "")
                    val pkgArg = arguments.optString("packageName", "")
                    val appName = if (appNameArg.isNotBlank()) appNameArg else pkgArg
                    if (appName.isBlank()) {
                        ToolResult(false, "App name is required")
                    } else {
                        appLauncher.openApp(appName)
                    }
                }

                "makePhoneCall" -> {
                    val phone = arguments.optString("phoneNumber", "")
                    if (phone.isBlank()) {
                        ToolResult(false, "Phone number is required")
                    } else {
                        appLauncher.makePhoneCall(phone)
                    }
                }

                "openSettings" -> {
                    appLauncher.openSettings()
                }

                "openCamera" -> {
                    appLauncher.openCamera()
                }

                "openGallery" -> {
                    appLauncher.openGallery()
                }

                "setAlarm" -> {
                    val hour = if (arguments.has("hour")) arguments.optInt("hour") else null
                    val minutes = if (arguments.has("minutes")) arguments.optInt("minutes") else null
                    val message = arguments.optString("message", "").takeIf { it.isNotBlank() }
                    appLauncher.setAlarm(hour, minutes, message)
                }

                else -> {
                    Logger.w("Unknown tool name requested: $toolName")
                    ToolResult(false, "Unsupported tool: $toolName")
                }
            }
        } catch (e: Exception) {
            Logger.e("Exception during tool execution: $toolName", e)
            ToolResult(false, "Error executing $toolName: ${e.localizedMessage ?: "Unknown"}")
        }

        onToolExecutedListener?.invoke(result)
        return result
    }
}
