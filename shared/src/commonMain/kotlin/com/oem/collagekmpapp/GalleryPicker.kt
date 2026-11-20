package com.oem.collagekmpapp

import androidx.compose.runtime.Composable

expect class GalleryPickerLauncher {
    fun launch()
}

@Composable
expect fun rememberGalleryPicker(onImagesSelected: (List<String>) -> Unit): GalleryPickerLauncher
