package com.oem.collagekmpapp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.oem.collagekmpapp.ExportOptions
import com.oem.collagekmpapp.KeyValueStore
import com.oem.collagekmpapp.ui.DEFAULT_FORMAT
import com.oem.collagekmpapp.ui.DEFAULT_LANGUAGE
import com.oem.collagekmpapp.ui.DEFAULT_RESOLUTION
import com.oem.collagekmpapp.ui.DEFAULT_THEME

/**
 * Global, persisted app settings. Settings screen WRITES here, every export screen READS from here,
 * so the choices survive screen changes and app restarts.
 *
 * Call [attach] once (done at the top of App()).
 */
object AppSettings {
    private var store: KeyValueStore? = null
    private var attached = false

    var resolution by mutableStateOf(DEFAULT_RESOLUTION); private set
    var language by mutableStateOf(DEFAULT_LANGUAGE); private set
    var exportFormat by mutableStateOf(DEFAULT_FORMAT); private set      // "JPEG" | "PNG" | "WebP"
    var imageQuality by mutableFloatStateOf(90f); private set
    var watermark by mutableStateOf(false); private set
    var theme by mutableStateOf(DEFAULT_THEME); private set

    fun attach(kv: KeyValueStore) {
        if (attached && store === kv) return
        store = kv
        attached = true
        resolution = kv.getString(K_RES, DEFAULT_RESOLUTION)
        language = kv.getString(K_LANG, DEFAULT_LANGUAGE)
        exportFormat = kv.getString(K_FMT, DEFAULT_FORMAT)
        imageQuality = kv.getString(K_Q, "90").toFloatOrNull() ?: 90f
        watermark = kv.getString(K_WM, "false") == "true"
        theme = kv.getString(K_THEME, DEFAULT_THEME)
        ExportOptions.watermark = watermark
    }

    fun updateResolution(v: String) { resolution = v; store?.putString(K_RES, v) }
    fun updateLanguage(v: String) { language = v; store?.putString(K_LANG, v) }
    fun updateExportFormat(v: String) { exportFormat = v; store?.putString(K_FMT, v) }
    fun updateImageQuality(v: Float) { imageQuality = v; store?.putString(K_Q, v.toString()) }
    fun updateWatermark(v: Boolean) { watermark = v; ExportOptions.watermark = v; store?.putString(K_WM, v.toString()) }
    fun updateTheme(v: String) { theme = v; store?.putString(K_THEME, v) }

    fun resetToDefaults() {
        updateResolution(DEFAULT_RESOLUTION)
        updateLanguage(DEFAULT_LANGUAGE)
        updateExportFormat(DEFAULT_FORMAT)
        updateImageQuality(90f)
        updateWatermark(false)
        updateTheme(DEFAULT_THEME)
    }

    // ---- derived values used by the export screens ------------------------------------------

    /** Long side in px for the saved resolution: 720p / 1080p / 4K. */
    val longSide: Int
        get() = when {
            resolution.contains("4K", true) || resolution.contains("Ultra", true) -> 3840
            resolution.contains("1080", true) || resolution.contains("High", true) -> 1080
            else -> 720
        }

    /** Label used by the export screen's quality chips. */
    val qualityLabel: String get() = qualityLabelFor(longSide)

    /** Format token understood by the exporters: "JPG" | "PNG" | "WEBP". */
    val formatToken: String get() = formatTokenFor(exportFormat)

    fun qualityLabelFor(longSide: Int) = when (longSide) {
        3840 -> "Ultra HD (4K)"
        1080 -> "High (1080p)"
        else -> "Standard (720p)"
    }

    fun longSideForLabel(label: String) = when {
        label.contains("4K", true) || label.contains("Ultra", true) -> 3840
        label.contains("1080", true) || label.contains("High", true) -> 1080
        else -> 720
    }

    fun formatTokenFor(format: String) = when {
        format.startsWith("PNG", true) -> "PNG"
        format.startsWith("WEBP", true) -> "WEBP"
        else -> "JPG"
    }

    private const val K_RES = "set_resolution"
    private const val K_LANG = "set_language"
    private const val K_FMT = "set_format"
    private const val K_Q = "set_quality"
    private const val K_WM = "set_watermark"
    private const val K_THEME = "set_theme"
}

/** 14.2 MB / 830 KB / 0 KB */
fun formatBytes(bytes: Long): String = when {
    bytes <= 0L -> "0 KB"
    bytes < 1024L * 1024L -> "${(bytes / 1024L).coerceAtLeast(1L)} KB"
    bytes < 1024L * 1024L * 1024L -> {
        val tenths = (bytes * 10L) / (1024L * 1024L)
        "${tenths / 10}.${tenths % 10} MB"
    }
    else -> {
        val tenths = (bytes * 10L) / (1024L * 1024L * 1024L)
        "${tenths / 10}.${tenths % 10} GB"
    }
}