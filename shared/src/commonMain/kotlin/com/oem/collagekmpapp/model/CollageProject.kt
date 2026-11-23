package com.oem.collagekmpapp.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * NOTE: every *Long color field in this file is an ARGB value from Color.toArgb(),
 * NOT Compose's packed Color.value. Decode with Color(value.toInt()) / android Color.
 *
 * Single unified data model representing the entire collage edit state.
 * All spatial coordinates, offsets, sizes, and padding are normalized (0.0 .. 1.0)
 * relative to canvas dimensions (width and height), making rendering 100% resolution-independent.
 */
data class PhotoTransform(
    val uri: String,
    val panX: Float = 0f,       // Normalized pan (-1f .. 1f) relative to slot width
    val panY: Float = 0f,       // Normalized pan (-1f .. 1f) relative to slot height
    val scale: Float = 1f,      // Relative scale multiplier (1f = fit/fill)
    val rotation: Float = 0f    // Rotation in degrees
)

data class NormalizedPoint(
    val x: Float,               // 0.0 .. 1.0 relative to canvas width
    val y: Float                // 0.0 .. 1.0 relative to canvas height
)

data class NormalizedDrawStroke(
    val points: List<NormalizedPoint>,
    val colorLong: Long,
    val strokeWidthRatio: Float, // Relative to canvas width
    val isEraser: Boolean = false
)

sealed class ProjectOverlayItem {
    abstract val id: String
    abstract val x: Float       // 0.0 .. 1.0 relative to canvas width (center anchor)
    abstract val y: Float       // 0.0 .. 1.0 relative to canvas height (center anchor)
    abstract val rotation: Float
    abstract val scale: Float   // Relative scale multiplier
    abstract val zIndex: Float

    data class Emoji(
        override val id: String,
        val emoji: String,
        override val x: Float,
        override val y: Float,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val zIndex: Float = 0f
    ) : ProjectOverlayItem()

    data class Photo(
        override val id: String,
        val uri: String,
        override val x: Float,
        override val y: Float,
        override val rotation: Float = 0f,
        override val scale: Float = 1f,
        override val zIndex: Float = 0f
    ) : ProjectOverlayItem()
}

data class ProjectTextLayer(
    val id: String = "default_text",
    val text: String,
    val x: Float = 0.5f,        // 0.0 .. 1.0 relative to canvas width (center anchor)
    val y: Float = 0.5f,        // 0.0 .. 1.0 relative to canvas height (center anchor)
    val fontName: String = "Script",
    val colorLong: Long = 0xFFFFFFFFL,
    val fontSizeRatio: Float = 0.075f, // Relative to canvas width
    val rotation: Float = 0f,
    val zIndex: Float = 10f
)

data class CollageProject(
    val layoutId: String = "2_grid",
    val slots: List<Slot> = emptyList(),
    val photos: List<PhotoTransform> = emptyList(),
    val aspectRatioWidth: Float = 1f,
    val aspectRatioHeight: Float = 1f,
    val outerSpacingRatio: Float = 0.03f,     // Spacing relative to canvas width
    val innerSpacingRatio: Float = 0.02f,     // Spacing relative to canvas width
    val cornerRadiusRatio: Float = 0.05f,     // Corner radius relative to canvas width
    val backgroundColorLong: Long = 0xFFFFFFFFL,
    val backgroundGradientColors: List<Long> = emptyList(),
    val backgroundPhotoUri: String? = null,
    val backgroundFade: Float = 0f,
    val filterPreset: String = "Original",
    val filterIntensity: Float = 0f,
    val adjustValues: Map<String, Float> = emptyMap(),
    val overlays: List<ProjectOverlayItem> = emptyList(),
    val textLayers: List<ProjectTextLayer> = emptyList(),
    val drawStrokes: List<NormalizedDrawStroke> = emptyList()
)
