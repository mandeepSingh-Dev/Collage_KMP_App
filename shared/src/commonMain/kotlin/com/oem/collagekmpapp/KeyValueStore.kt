package com.oem.collagekmpapp

import androidx.compose.runtime.Composable

// commonMain
// Small platform bridges (same pattern as rememberGalleryPicker / rememberImageSharer).

/** Tiny key/value store (SharedPreferences on Android, NSUserDefaults on iOS). */
interface KeyValueStore {
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
}

@Composable
expect fun rememberKeyValueStore(): KeyValueStore

/** Temp-cache access for Settings > "Clear Temp Cache". */
interface CacheManager {
    /** Total size in bytes of all temporary cache files. */
    fun sizeBytes(): Long
    /** Deletes every temporary cache file. Returns the number of bytes that were freed. */
    fun clear(): Long
}

@Composable
expect fun rememberCacheManager(): CacheManager

/**
 * Opens a saved image in the device's Gallery / Photos app.
 * [location] is the saved content URI when the exporter returns one; otherwise (older exporters
 * only return "Pictures/CollageStudio") the newest image in the app folder is opened.
 */
@Composable
expect fun rememberGalleryOpener(): (location: String) -> Unit

/**
 * Turns whatever an exporter reported ("content://…" or just "Pictures/CollageStudio") into a real
 * content URI string when possible (newest image in Pictures/CollageStudio), else returns it unchanged.
 */
@Composable
expect fun rememberSavedImageResolver(): (location: String) -> String

/**
 * Process-wide export switches that every exporter (collage, freestyle, edit photo, remove bg)
 * reads when it writes the final bitmap. Kept here so no exporter signature has to change.
 */
object ExportOptions {
    var watermark: Boolean = false
    var watermarkText: String = "Collage Studio"

    /** What gets stamped when [watermark] is on. [watermarkText] is the user's custom signature. */
    var watermarkType: WatermarkType = WatermarkType.APP_NAME
    /** Pattern used by [WatermarkType.DATE] (one of [WATERMARK_DATE_PATTERNS]). */
    var watermarkDatePattern: String = DEFAULT_WATERMARK_DATE_PATTERN

    /** The final text the exporters draw (App name / signature / today's date). */
    fun resolvedWatermarkText(): String =
        watermarkStampText(watermarkType, watermarkText, watermarkDatePattern)
}

// ---- Custom Watermark / Signature / Date Stamp ------------------------------------------------

/** Text stamped by [WatermarkType.APP_NAME] (and the fallback when a signature is left empty). */
const val APP_WATERMARK_TEXT = "Collage Studio"

/** Default custom signature (empty -> the placeholder shows, export falls back to the app name). */
const val DEFAULT_WATERMARK_SIGNATURE = ""

/** Longest signature we allow, so the stamp stays small and clean. */
const val WATERMARK_MAX_LENGTH = 40

/** Date stamp presets: 2026.11.26 / 26.11.2026 / 26 Nov 2026. */
val WATERMARK_DATE_PATTERNS = listOf("yyyy.MM.dd", "dd.MM.yyyy", "dd MMM yyyy")
const val DEFAULT_WATERMARK_DATE_PATTERN = "yyyy.MM.dd"

/** [key] is what gets persisted in [KeyValueStore], so labels can be renamed safely. */
enum class WatermarkType(val key: String, val label: String, val emoji: String) {
    APP_NAME("app_name", "App Name", "🏷️"),
    SIGNATURE("signature", "Signature", "✍️"),
    DATE("date", "Date", "📅");

    companion object {
        fun fromKey(key: String): WatermarkType = entries.firstOrNull { it.key == key } ?: APP_NAME
    }
}

/** Single source of truth for the stamp text (used by the exporters AND the UI previews). */
fun watermarkStampText(type: WatermarkType, signature: String, datePattern: String): String = when (type) {
    WatermarkType.APP_NAME -> APP_WATERMARK_TEXT
    WatermarkType.SIGNATURE -> signature.trim().ifEmpty { APP_WATERMARK_TEXT }
    WatermarkType.DATE -> currentDateStamp(datePattern)
}

/** Today's date formatted with [pattern] (SimpleDateFormat on Android, NSDateFormatter on iOS). */
expect fun currentDateStamp(pattern: String): String