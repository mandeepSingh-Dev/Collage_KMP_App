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
}