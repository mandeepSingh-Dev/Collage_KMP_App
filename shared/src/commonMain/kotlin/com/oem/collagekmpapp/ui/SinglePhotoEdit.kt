package com.oem.collagekmpapp.ui

import androidx.compose.runtime.Composable
import com.oem.collagekmpapp.FreestyleProject

/**
 * Everything the single-photo editor (SpTool.kt) shows on screen, in a platform-neutral form.
 * [project] only carries the backdrop, the stickers / text overlays and the doodles
 * (items = overlays, no photo item); the photo itself is described by the crop / transform fields.
 *
 * Crop is in image-normalised coordinates (0..1). Scales are multipliers (1f = 100 %).
 * Blur amount is 0..1, blurR is a fraction of the canvas width.
 */
data class SinglePhotoEdit(
    val photoUri: String,
    val ratio: Float,                 // canvas width / height
    val cropL: Float, val cropT: Float, val cropR: Float, val cropB: Float,
    val fit: Boolean,
    val quarterTurns: Int,
    val fineAngle: Float,
    val flipH: Boolean,
    val flipV: Boolean,
    val scaleH: Float,
    val scaleV: Float,
    val scaleCenter: Float,
    val filterPreset: String,
    val filterIntensity: Float,
    val adjustValues: Map<String, Float>,
    val blurOn: Boolean,
    val blurInside: Boolean,
    val blurAmount: Float,
    val blurFeather: Float,
    val blurCx: Float,
    val blurCy: Float,
    val blurR: Float,
    val project: FreestyleProject
)

/** Platform bridge (same pattern as rememberFreestyleExporter). longSide = px of the longest edge, format = "JPG" | "PNG". */
fun interface SinglePhotoExporter {
    fun export(edit: SinglePhotoEdit, longSide: Int, format: String, onResult: (Result<String>) -> Unit)
}

@Composable
expect fun rememberSinglePhotoExporter(): SinglePhotoExporter
