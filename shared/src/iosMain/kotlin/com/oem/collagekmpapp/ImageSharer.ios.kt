package com.oem.collagekmpapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.oem.collagekmpapp.model.Slot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

actual class ImageSharer {
    actual fun saveToGallery(
        layoutId: String,
        photos: List<String>,
        slots: List<Slot>,
        spacingDp: Float,
        cornerRadiusDp: Float,
        filterPreset: String,
        filterIntensity: Float,
        backgroundPhoto: String?,
        backgroundFade: Float,
        outerSpacingDp: Float,
        aspectRatioWidth: Float,
        aspectRatioHeight: Float,
        backgroundColor: Color,
        backgroundGradientColors: List<Color>,
        adjustValues: Map<String, Float>,
        overlayItems: List<com.oem.collagekmpapp.ui.OverlayItem>,
        drawStrokes: List<com.oem.collagekmpapp.ui.DrawStroke>,
        collageText: String,
        textColor: Color,
        selectedFont: String,
        textOffset: Offset
    ) {}
    actual fun saveProject(
        project: com.oem.collagekmpapp.model.CollageProject,
        quality: String,
        format: String,
        onSuccess: (uri: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {}
    actual fun shareToApp(appName: String, imageUrl: String) {}
}

@Composable
actual fun rememberImageSharer(): ImageSharer {
    return remember { ImageSharer() }
}
