package com.oem.collagekmpapp

import androidx.compose.runtime.Composable

/**
 * Platform bridge so the common-code FreestyleEditorScreen can trigger a high-res export.
 * (Same pattern as rememberGalleryPicker.)
 *
 * longSide = target size in px of the longest canvas edge, format = "JPG" or "PNG".
 * onResult is invoked on the main thread with a displayable location string, or the failure.
 */
fun interface FreestyleExporter {
    fun export(project: FreestyleProject, longSide: Int, format: String, onResult: (Result<String>) -> Unit)
}

@Composable
expect fun rememberFreestyleExporter(): FreestyleExporter
