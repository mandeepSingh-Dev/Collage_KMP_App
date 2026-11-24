package com.oem.collagekmpapp

/**
 * Single source of truth for sticker / emoji / text-overlay geometry.
 *
 * Every size is expressed against a 360-unit-wide reference canvas and scaled by the REAL canvas width,
 *   preview (Compose): value * canvasWidthPx / REF_WIDTH
 *   export  (Canvas) : value * exportWidthPx / REF_WIDTH
 * so a sticker takes exactly the same share of the picture on every phone and in every export size.
 */
object OverlayMetrics {
    const val REF_WIDTH = 360f

    // emoji sticker
    const val EMOJI_FONT = 36f
    // photo sticker
    const val PHOTO_SIZE = 80f
    const val PHOTO_RADIUS = 6f
    // dark pill behind every sticker / text overlay
    const val PILL_PAD = 8f
    const val PILL_RADIUS = 8f
    const val PILL_ALPHA = 0.35f
    // neon glow
    const val NEON_BLUR = 16f
    // freehand brush: width at 50 % density
    const val BRUSH_BASE = 6f

    /** Font size of a text overlay (reference units). */
    fun textFont(font: String): Float = when (font) {
        "Script" -> 28f
        "Bold" -> 32f
        "Retro" -> 26f
        "Serif" -> 28f
        "Neon" -> 30f
        else -> 26f // "Modern"
    }

    /** Font size as a fraction of canvas width (what ProjectTextLayer.fontSizeRatio stores). */
    fun textFontRatio(font: String): Float = textFont(font) / REF_WIDTH
}