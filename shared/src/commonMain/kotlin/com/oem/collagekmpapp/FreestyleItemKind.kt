package com.oem.collagekmpapp

import com.oem.collagekmpapp.model.NormalizedDrawStroke
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Freestyle (scrapbook) data model. Completely independent from the grid CollageProject.
 * All positions are normalized: x / y are 0..1 of canvas width / height (center anchor).
 * All sizes are expressed in "canvas-width units" so preview and export match at any resolution.
 * Z-order = list order (last item is drawn on top).
 */
enum class FreestyleItemKind { PHOTO, EMOJI, TEXT }

enum class PhotoFrame(val label: String) {
    NONE("None"),
    ROUNDED("Rounded"),
    POLAROID("Polaroid")
}

enum class BackgroundTexture(val label: String) {
    NONE("None"),
    DOTS("Dots"),
    GRID("Grid"),
    STRIPES("Stripes"),
    LINES("Notebook")
}

data class FreestyleItem(
    val id: String,
    val kind: FreestyleItemKind,
    /** PHOTO: image uri, EMOJI: the emoji, TEXT: the text. */
    val content: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    /** PHOTO / EMOJI: outer width as a fraction of canvas width. */
    val widthRatio: Float = 0.4f,
    /** PHOTO: width / height of the picture itself (updated once the image is loaded). */
    val aspect: Float = 1f,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    // ---- photo styling
    val frame: PhotoFrame = PhotoFrame.POLAROID,
    val shadow: Boolean = true,
    // ---- text styling
    val fontName: String = "Script",
    val colorLong: Long = 0xFFFFFFFFL,
    val fontSizeRatio: Float = 0.075f,
    val textPill: Boolean = true,
    val textShadow: Boolean = false
) {
    /** Outer width in canvas-width units. */
    val widthUnits: Float
        get() = when (kind) {
            FreestyleItemKind.TEXT -> max(content.length, 1) * fontSizeRatio * TEXT_CHAR_W + TEXT_PAD_H * 2f
            else -> widthRatio
        }

    /** Outer height in canvas-width units. */
    val heightUnits: Float
        get() = when (kind) {
            FreestyleItemKind.PHOTO -> {
                val a = aspect.coerceAtLeast(0.1f)
                if (frame == PhotoFrame.POLAROID)
                    widthRatio * ((1f - 2f * POLAROID_SIDE) / a + POLAROID_SIDE + POLAROID_BOTTOM)
                else widthRatio / a
            }
            FreestyleItemKind.EMOJI -> widthRatio
            FreestyleItemKind.TEXT -> fontSizeRatio * TEXT_LINE_H
        }

    /** Corner radius in canvas-width units (photos only). */
    val cornerUnits: Float
        get() = widthRatio * when (frame) {
            PhotoFrame.NONE -> 0f
            PhotoFrame.ROUNDED -> 0.08f
            PhotoFrame.POLAROID -> 0.012f
        }

    companion object {
        const val POLAROID_SIDE = 0.05f     // fraction of item width
        const val POLAROID_BOTTOM = 0.18f   // fraction of item width
        const val TEXT_CHAR_W = 0.6f
        const val TEXT_PAD_H = 0.04f
        const val TEXT_LINE_H = 1.6f
    }
}

data class FreestyleProject(
    val aspectRatioWidth: Float = 1f,
    val aspectRatioHeight: Float = 1f,
    // ---- background (ARGB values from Color.toArgb(), NOT Compose packed values)
    val backgroundColorLong: Long = 0xFFFFFFFFL,
    val backgroundGradientColors: List<Long> = emptyList(),
    val backgroundPhotoUri: String? = null,
    val backgroundBlur: Float = 1f,                // 0..1
    val backgroundTexture: BackgroundTexture = BackgroundTexture.NONE,
    // ---- global look (applied to every photo, same engine as the grid editor)
    val filterPreset: String = "Original",
    val filterIntensity: Float = 0f,
    val adjustValues: Map<String, Float> = emptyMap(),
    // ---- content
    val items: List<FreestyleItem> = emptyList(),
    val drawStrokes: List<NormalizedDrawStroke> = emptyList()
)

private fun newId() = "fs_${kotlin.random.Random.nextLong().toULong()}"

/** Scatters the picked photos over the canvas like pictures dropped on a table. */
fun buildInitialFreestyleItems(uris: List<String>): List<FreestyleItem> {
    if (uris.isEmpty()) return emptyList()
    val n = uris.size
    val cols = ceil(sqrt(n.toFloat())).toInt().coerceAtLeast(1)
    val rows = ceil(n / cols.toFloat()).toInt().coerceAtLeast(1)
    val width = min(0.7f, (1f / cols) * 1.15f)
    return uris.mapIndexed { i, uri ->
        val col = i % cols
        val row = i / cols
        FreestyleItem(
            id = newId(),
            kind = FreestyleItemKind.PHOTO,
            content = uri,
            x = (col + 0.5f) / cols,
            y = (row + 0.5f) / rows,
            widthRatio = width,
            rotation = (((i * 7) % 13) - 6).toFloat()
        )
    }
}

/** A single extra photo dropped near the middle of the canvas. */
fun newFreestylePhoto(uri: String, index: Int): FreestyleItem {
    val o = 0.04f * (index % 5)
    return FreestyleItem(
        id = newId(),
        kind = FreestyleItemKind.PHOTO,
        content = uri,
        x = 0.42f + o,
        y = 0.42f + o,
        widthRatio = 0.45f,
        rotation = (((index * 5) % 11) - 5).toFloat()
    )
}

fun newFreestyleEmoji(emoji: String): FreestyleItem =
    FreestyleItem(id = newId(), kind = FreestyleItemKind.EMOJI, content = emoji, widthRatio = 0.18f)

fun newFreestyleText(text: String): FreestyleItem =
    FreestyleItem(id = newId(), kind = FreestyleItemKind.TEXT, content = text)
