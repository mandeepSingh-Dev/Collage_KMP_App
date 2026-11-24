package com.oem.collagekmpapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset

/**
 * One edit on the transparency mask. Points are normalized (0..1) to the image width / height,
 * widthRatio is the brush diameter as a fraction of the image width.
 * restore = true paints the photo back, false erases it. fill = true applies to the whole image.
 */
data class BgMaskStroke(
    val points: List<Offset>,
    val widthRatio: Float,
    val restore: Boolean,
    val fill: Boolean = false
)

/** Platform bridge: renders the original photo with the mask applied and saves a transparent PNG. */
fun interface BackgroundRemovalExporter {
    fun export(photoUri: String, strokes: List<BgMaskStroke>, onResult: (Result<String>) -> Unit)
}

@Composable
expect fun rememberBackgroundRemovalExporter(): BackgroundRemovalExporter