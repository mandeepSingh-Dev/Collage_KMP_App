package com.oem.collagekmpapp.firebase

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.analytics.FirebaseAnalytics

actual class FirebaseAnalyticsManager(private val context: Context) {
    private val analytics by lazy { FirebaseAnalytics.getInstance(context) }

    actual fun logEvent(eventName: String, params: Map<String, String>) {
        try {
            val bundle = Bundle()
            params.forEach { (key, value) -> bundle.putString(key, value) }
            analytics.logEvent(eventName, bundle)
        } catch (_: Exception) {}
    }

    actual fun logToolUsage(screen: String, toolName: String, extraInfo: String) {
        logEvent("tool_used", mapOf(
            "screen" to screen,
            "tool_name" to toolName,
            "extra_info" to extraInfo
        ))
    }

    actual fun logTemplateSelected(templateId: String, templateName: String) {
        logEvent("template_selected", mapOf(
            "template_id" to templateId,
            "template_name" to templateName
        ))
    }

    actual fun logExportResolution(resolution: String, format: String) {
        logEvent("export_completed", mapOf(
            "resolution" to resolution,
            "format" to format
        ))
    }
}

@Composable
actual fun rememberFirebaseAnalytics(): FirebaseAnalyticsManager {
    val context = LocalContext.current
    return remember(context) { FirebaseAnalyticsManager(context) }
}
