package com.oem.collagekmpapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.io.File

// androidMain

@Composable
actual fun rememberKeyValueStore(): KeyValueStore {
    val context = LocalContext.current
    return remember(context) {
        val prefs = context.applicationContext.getSharedPreferences("collage_studio_settings", Context.MODE_PRIVATE)
        object : KeyValueStore {
            override fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default
            override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
        }
    }
}

@Composable
actual fun rememberCacheManager(): CacheManager {
    val context = LocalContext.current
    return remember(context) {
        object : CacheManager {
            private fun roots(): List<File> =
                listOfNotNull(context.cacheDir, context.externalCacheDir).filter { it.exists() }

            private fun File.sizeRecursive(): Long =
                if (isFile) length() else (listFiles()?.sumOf { it.sizeRecursive() } ?: 0L)

            override fun sizeBytes(): Long = roots().sumOf { it.sizeRecursive() }

            override fun clear(): Long {
                val before = sizeBytes()
                // delete the CONTENT of cacheDir / externalCacheDir (the folders themselves stay)
                roots().forEach { root -> root.listFiles()?.forEach { runCatching { it.deleteRecursively() } } }
                return (before - sizeBytes()).coerceAtLeast(0L)
            }
        }
    }
}

@Composable
actual fun rememberGalleryOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { location -> openSavedImageInGallery(context, location) } }
}

@Composable
actual fun rememberSavedImageResolver(): (String) -> String {
    val context = LocalContext.current
    return remember(context) { { location -> resolveSavedImageUri(context, location)?.toString() ?: location } }
}

/** content:// / file:// strings are used as they are; anything else resolves to the newest image in Pictures/CollageStudio. */
fun resolveSavedImageUri(context: Context, location: String): Uri? {
    if (location.startsWith("content://") || location.startsWith("file://")) return Uri.parse(location)
    return runCatching {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val (selection, args) =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?" to arrayOf("%CollageStudio%")
            else
                "${MediaStore.Images.Media.DATA} LIKE ?" to arrayOf("%/CollageStudio/%")
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, args,
            "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media._ID} DESC"
        )?.use { c ->
            if (c.moveToFirst()) {
                android.content.ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(0))
            } else null
        }
    }.getOrNull()
}

private fun openSavedImageInGallery(context: Context, location: String) {
    val uri = resolveSavedImageUri(context, location)
    if (uri == null) {
        Toast.makeText(context, "Saved to Pictures/CollageStudio", Toast.LENGTH_SHORT).show()
        return
    }
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(view)
    } catch (_: Exception) {
        Toast.makeText(context, "Saved to Pictures/CollageStudio", Toast.LENGTH_SHORT).show()
    }
}