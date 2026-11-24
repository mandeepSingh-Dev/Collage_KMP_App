package com.oem.collagekmpapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSUserDefaults

// iosMain (only add this file if you have an iOS target)

@Composable
actual fun rememberKeyValueStore(): KeyValueStore = remember {
    object : KeyValueStore {
        private val defaults = NSUserDefaults.standardUserDefaults
        override fun getString(key: String, default: String): String = defaults.stringForKey(key) ?: default
        override fun putString(key: String, value: String) { defaults.setObject(value, forKey = key) }
    }
}

@Composable
actual fun rememberCacheManager(): CacheManager = remember {
    object : CacheManager {
        override fun sizeBytes(): Long = 0L   // TODO: iOS cache folder accounting
        override fun clear(): Long = 0L
    }
}

@Composable
actual fun rememberGalleryOpener(): (String) -> Unit = remember { { _ -> /* TODO: open Photos on iOS */ } }

@Composable
actual fun rememberSavedImageResolver(): (String) -> String = remember { { it } }