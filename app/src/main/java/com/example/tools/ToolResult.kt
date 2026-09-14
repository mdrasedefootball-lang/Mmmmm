package com.example.tools

data class ToolResult(
    val success: Boolean,
    val message: String,
    val actionType: String? = null
)
