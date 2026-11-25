package com.oem.collagekmpapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember


// iosMain (only add this file if you have an iOS target)
@Composable
actual fun rememberSinglePhotoExporter(): SinglePhotoExporter = remember {
    SinglePhotoExporter { _, _, _, onResult ->
        onResult(Result.failure(UnsupportedOperationException("Photo export is not implemented on iOS yet.")))
    }
}