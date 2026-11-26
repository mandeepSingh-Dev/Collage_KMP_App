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
    POLAROID("Polaroid"),
    /** Polaroid card + two diagonal adhesive-tape strips over the top corners. */
    TAPE("Sticky Tape"),
    /** Dark 35 mm film strip with sprocket-hole cut-outs in the top / bottom bands. */
    FILM("Film Roll"),
    /** White paper card with a jagged, torn outline. */
    TORN("Torn Paper")
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
    val frame: PhotoFrame = PhotoFrame.NONE,          // plain photo by default; frames are picked in the Frame tool
    val shadow: Boolean = true,
    // ---- text styling
    val fontName: String = "Script",
    val colorLong: Long = 0xFFFFFFFFL,
    val fontSizeRatio: Float = 0.075f,
    val textPill: Boolean = true,
    val textShadow: Boolean = false,
    // ---- layer state
    /** Locked items can't be dragged, scaled, rotated or deleted (on the canvas or in the Layers sheet) until unlocked. */
    val isLocked: Boolean = false
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
                // generic form of the old formula: NONE / ROUNDED (no insets) -> widthRatio / a,
                // POLAROID -> widthRatio * ((1 - 2*SIDE) / a + SIDE + BOTTOM), exactly as before
                val ins = frame.insets()
                widthRatio * ((1f - 2f * ins.side) / a + ins.top + ins.bottom)
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
            PhotoFrame.TAPE -> 0.012f
            PhotoFrame.FILM -> 0.015f
            PhotoFrame.TORN -> 0f
        }

    companion object {
        const val POLAROID_SIDE = 0.05f     // fraction of item width
        const val POLAROID_BOTTOM = 0.18f   // fraction of item width
        const val TEXT_CHAR_W = 0.6f
        const val TEXT_PAD_H = 0.04f
        const val TEXT_LINE_H = 1.6f

        // ---- expanded frames (all fractions of the item's outer width)
        const val FILM_SIDE = 0.035f        // thin left / right border of the film strip
        const val FILM_BAND = 0.13f         // top / bottom bands that carry the sprocket holes
        const val FILM_HOLES = 8            // holes per band
        const val TORN_BORDER = 0.07f       // white paper border around the photo
        const val TORN_AMPLITUDE = 0.02f    // depth of the torn edge
        const val TORN_TEETH = 28           // jags along the width
        const val FILM_COLOR = 0xFF1C1917L  // ARGB
        const val TAPE_COLOR = 0xC8F3E3B3L  // ARGB, translucent cream
        const val TAPE_EDGE_COLOR = 0x26000000L
    }
}

// =====================================================================================
// Frame geometry – shared by the Compose preview (FsTool.kt) and FreestyleRenderer, so
// both draw exactly the same shapes at any resolution.
// =====================================================================================

/** Insets of the photo window inside the frame, as fractions of the item's outer width. */
data class FrameInsets(val side: Float, val top: Float, val bottom: Float)

fun PhotoFrame.insets(): FrameInsets = when (this) {
    PhotoFrame.NONE, PhotoFrame.ROUNDED -> FrameInsets(0f, 0f, 0f)
    PhotoFrame.POLAROID, PhotoFrame.TAPE ->
        FrameInsets(FreestyleItem.POLAROID_SIDE, FreestyleItem.POLAROID_SIDE, FreestyleItem.POLAROID_BOTTOM)
    PhotoFrame.FILM -> FrameInsets(FreestyleItem.FILM_SIDE, FreestyleItem.FILM_BAND, FreestyleItem.FILM_BAND)
    PhotoFrame.TORN -> FrameInsets(FreestyleItem.TORN_BORDER, FreestyleItem.TORN_BORDER, FreestyleItem.TORN_BORDER)
}

/**
 * Film sprocket holes for an item of [iw] x [ih] px, relative to the item's top-left corner.
 * Each entry is floatArrayOf(left, top, right, bottom, cornerRadius).
 */
fun filmSprocketHoles(iw: Float, ih: Float): List<FloatArray> {
    val band = FreestyleItem.FILM_BAND * iw
    val n = FreestyleItem.FILM_HOLES
    val holeW = iw * 0.06f
    val holeH = band * 0.42f
    val r = holeW * 0.22f
    val spacing = iw / n
    val out = ArrayList<FloatArray>(n * 2)
    for (i in 0 until n) {
        val cx = (i + 0.5f) * spacing
        for (cy in floatArrayOf(band / 2f, ih - band / 2f)) {
            out += floatArrayOf(cx - holeW / 2f, cy - holeH / 2f, cx + holeW / 2f, cy + holeH / 2f, r)
        }
    }
    return out
}

/**
 * Torn-paper outline for an item of [iw] x [ih] px (relative to its top-left), as x,y pairs.
 * Deterministic for a given [seed] (use the item id's hashCode) and resolution independent:
 * the number of jags depends only on the item's proportions, and the jags only go INWARD,
 * so the outline always stays inside the item rect.
 */
fun tornPaperOutline(iw: Float, ih: Float, seed: Int): FloatArray {
    val rnd = kotlin.random.Random(seed)
    val amp = FreestyleItem.TORN_AMPLITUDE * iw
    val nx = FreestyleItem.TORN_TEETH
    val ny = max(4, (FreestyleItem.TORN_TEETH * ih / iw.coerceAtLeast(1f) + 0.5f).toInt())
    val pts = ArrayList<Float>((nx + ny) * 4)
    fun jag(i: Int) = amp * (if (i % 2 == 0) 0.25f + 0.75f * rnd.nextFloat() else 0.15f * rnd.nextFloat())
    for (i in 0 until nx) { pts += iw * i / nx; pts += jag(i) }                 // top, left -> right
    for (i in 0 until ny) { pts += iw - jag(i); pts += ih * i / ny }            // right, top -> bottom
    for (i in 0 until nx) { pts += iw - iw * i / nx; pts += ih - jag(i) }       // bottom, right -> left
    for (i in 0 until ny) { pts += jag(i); pts += ih - ih * i / ny }            // left, bottom -> top
    return pts.toFloatArray()
}

/** One adhesive tape strip: centre, size (px) and rotation (degrees), relative to the item's top-left. */
data class TapeStrip(val cx: Float, val cy: Float, val length: Float, val thickness: Float, val angle: Float)

/** Two diagonal tape strips over the top-left and top-right corners (they overhang the frame a little). */
fun tapeStrips(iw: Float, ih: Float): List<TapeStrip> {
    val len = iw * 0.32f
    val th = iw * 0.085f
    return listOf(
        TapeStrip(iw * 0.08f, iw * 0.04f, len, th, -40f),
        TapeStrip(iw * 0.92f, iw * 0.04f, len, th, 40f)
    )
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
            rotation = (((i * 7) % 13) - 6).toFloat(),
            frame = PhotoFrame.NONE
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
        rotation = (((index * 5) % 11) - 5).toFloat(),
        frame = PhotoFrame.NONE
    )
}

fun newFreestyleEmoji(emoji: String): FreestyleItem =
    FreestyleItem(id = newId(), kind = FreestyleItemKind.EMOJI, content = emoji, widthRatio = 0.18f)

fun newFreestyleText(text: String): FreestyleItem =
    FreestyleItem(id = newId(), kind = FreestyleItemKind.TEXT, content = text)