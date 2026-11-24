package com.oem.collagekmpapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// iosMain (only add this file if you have an iOS target)
@Composable
actual fun rememberBackgroundRemovalExporter(): BackgroundRemovalExporter = remember {
    BackgroundRemovalExporter { _, _, onResult ->
        onResult(Result.failure(UnsupportedOperationException("Background removal export is not implemented on iOS yet.")))
    }
}