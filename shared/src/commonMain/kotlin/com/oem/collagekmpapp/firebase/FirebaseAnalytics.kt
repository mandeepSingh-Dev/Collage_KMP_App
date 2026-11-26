package com.oem.collagekmpapp.firebase

import androidx.compose.runtime.Composable

expect class FirebaseAnalyticsManager {
    fun logEvent(eventName: String, params: Map<String, String> = emptyMap())
    fun logToolUsage(screen: String, toolName: String, extraInfo: String = "")
    fun logTemplateSelected(templateId: String, templateName: String)
    fun logExportResolution(resolution: String, format: String)
}

@Composable
expect fun rememberFirebaseAnalytics(): FirebaseAnalyticsManager
