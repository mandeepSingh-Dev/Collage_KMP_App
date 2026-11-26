package com.oem.collagekmpapp.firebase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class FirebaseAnalyticsManager {
    actual fun logEvent(eventName: String, params: Map<String, String>) {}
    actual fun logToolUsage(screen: String, toolName: String, extraInfo: String) {}
    actual fun logTemplateSelected(templateId: String, templateName: String) {}
    actual fun logExportResolution(resolution: String, format: String) {}
}

@Composable
actual fun rememberFirebaseAnalytics(): FirebaseAnalyticsManager = remember { FirebaseAnalyticsManager() }
