package com.oem.collagekmpapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class GalleryPickerLauncher(private val onLaunch: () -> Unit) {
    actual fun launch() {
        onLaunch()
    }
}

@Composable
actual fun rememberGalleryPicker(onImagesSelected: (List<String>) -> Unit): GalleryPickerLauncher {
    return remember {
        GalleryPickerLauncher {
            onImagesSelected(listOf("content://media/1", "content://media/2", "content://media/3", "content://media/4"))
        }
    }
}
