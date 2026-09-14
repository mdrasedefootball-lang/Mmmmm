package com.example.gemini

import org.json.JSONArray
import org.json.JSONObject

object GeminiTools {

    /**
     * Builds the JSON array of tool declarations compatible with Gemini Live API setup.
     */
    fun getFunctionDeclarations(): JSONArray {
        val tools = JSONArray()

        // 1. openWebsite
        tools.put(JSONObject().apply {
            put("name", "openWebsite")
            put("description", "Open a website URL in the device browser. URL must start with http:// or https://.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("url", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The website URL to open, e.g. https://www.google.com")
                    })
                })
                put("required", JSONArray().put("url"))
            })
        })

        // 2. openYouTube
        tools.put(JSONObject().apply {
            put("name", "openYouTube")
            put("description", "Open the YouTube application or YouTube search on the user's phone.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional search query to search on YouTube")
                    })
                })
            })
        })

        // 3. openWhatsApp
        tools.put(JSONObject().apply {
            put("name", "openWhatsApp")
            put("description", "Open the WhatsApp application on the device.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 4. sendWhatsAppMessage
        tools.put(JSONObject().apply {
            put("name", "sendWhatsAppMessage")
            put("description", "Prepare a WhatsApp message to a contact or phone number without silently sending.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("phoneNumber", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Optional phone number in international format")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The message text to prepare")
                    })
                })
                put("required", JSONArray().put("message"))
            })
        })

        // 5. openMaps
        tools.put(JSONObject().apply {
            put("name", "openMaps")
            put("description", "Open Google Maps to view a location or search for places.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("location", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Location or address to search, e.g. Feni, Dhaka, Eiffel Tower")
                    })
                })
            })
        })

        // 6. openApp
        tools.put(JSONObject().apply {
            put("name", "openApp")
            put("description", "Open a whitelisted application on the phone (YouTube, WhatsApp, Maps, Chrome, Gmail, Settings, Camera, Gallery, Calculator).")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("appName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The name of the app to launch (e.g. Chrome, Camera, Calculator)")
                    })
                })
                put("required", JSONArray().put("appName"))
            })
        })

        // 7. makePhoneCall
        tools.put(JSONObject().apply {
            put("name", "makePhoneCall")
            put("description", "Prepare a phone call on the device dialer for safe user confirmation.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("phoneNumber", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "The telephone number to dial")
                    })
                })
                put("required", JSONArray().put("phoneNumber"))
            })
        })

        // 8. openSettings
        tools.put(JSONObject().apply {
            put("name", "openSettings")
            put("description", "Open the Android system settings screen.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 9. openCamera
        tools.put(JSONObject().apply {
            put("name", "openCamera")
            put("description", "Launch the camera app to take photos or record video.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 10. openGallery
        tools.put(JSONObject().apply {
            put("name", "openGallery")
            put("description", "Open device gallery or photos app to view images.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // 11. setAlarm
        tools.put(JSONObject().apply {
            put("name", "setAlarm")
            put("description", "Open clock app to set an alarm.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("hour", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Hour of day (0-23)")
                    })
                    put("minutes", JSONObject().apply {
                        put("type", "INTEGER")
                        put("description", "Minutes of hour (0-59)")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Alarm label/reason")
                    })
                })
            })
        })

        return tools
    }
}
