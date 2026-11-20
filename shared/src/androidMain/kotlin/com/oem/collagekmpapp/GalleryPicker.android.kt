package com.oem.collagekmpapp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class GalleryPickerLauncher(private val launcher: ActivityResultLauncher<String>) {
    actual fun launch() {
        launcher.launch("image/*")
    }
}

@Composable
actual fun rememberGalleryPicker(onImagesSelected: (List<String>) -> Unit): GalleryPickerLauncher {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val uriStrings = uris.map { it.toString() }
        onImagesSelected(uriStrings)
    }
    return remember { GalleryPickerLauncher(launcher) }
}
