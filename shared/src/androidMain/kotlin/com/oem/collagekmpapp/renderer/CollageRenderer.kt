package com.oem.collagekmpapp.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.oem.collagekmpapp.model.CollageProject
import com.oem.collagekmpapp.model.ProjectOverlayItem
import kotlin.math.abs
import kotlin.math.max

/**
 * Shared high-resolution offscreen canvas renderer.
 * Draws the complete CollageProject edit model onto any android.graphics.Canvas
 * using normalized relative coordinates (0.0 .. 1.0) so that preview and export
 * produce 100% identical visuals regardless of target resolution.
 */
object CollageRenderer {

    fun draw(
        canvas: Canvas,
        project: CollageProject,
        targetWidth: Int,
        targetHeight: Int,
        photoBitmaps: Map<String, Bitmap> = emptyMap(),
        backgroundBitmap: Bitmap? = null
    ) {
        val w = targetWidth.toFloat()
        val h = targetHeight.toFloat()

        // 1. GRADIENT OR SOLID OR PHOTO BACKGROUND
        drawBackground(canvas, project, w, h, backgroundBitmap)

        // 2. PHOTO GRID CELLS & PER-PHOTO CLIPPING / TRANSFORMS / FILTERS
        drawPhotoGrid(canvas, project, w, h, photoBitmaps)

        // 3. GLOBAL ADJUSTMENTS OVERLAY (Brightness, Contrast, Highlight, Shadow, Saturation, Warmth, Tint)
        drawGlobalAdjustments(canvas, project, w, h)

        // 4. FREEHAND DRAW STROKES
        drawStrokes(canvas, project, w, h)

        // 5. STICKERS & EMOJIS (Sorted by zIndex)
        drawOverlays(canvas, project, w, h, photoBitmaps)

        // 6. TEXT LAYERS (Sorted by zIndex)
        drawTextLayers(canvas, project, w, h)
    }

    private fun drawBackground(
        canvas: Canvas,
        project: CollageProject,
        w: Float,
        h: Float,
        backgroundBitmap: Bitmap?
    ) {
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (backgroundBitmap != null && !backgroundBitmap.isRecycled) {
            val bgScale = max(w / backgroundBitmap.width, h / backgroundBitmap.height)
            val bw = backgroundBitmap.width * bgScale
            val bh = backgroundBitmap.height * bgScale
            val bgDest = RectF(w / 2f - bw / 2f, h / 2f - bh / 2f, w / 2f + bw / 2f, h / 2f + bh / 2f)
            canvas.drawBitmap(backgroundBitmap, null, bgDest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else if (project.backgroundGradientColors.isNotEmpty()) {
            val colors = project.backgroundGradientColors.map { it.toInt() }.toIntArray()
            val positions = colors.indices.map { i -> i.toFloat() / (colors.size - 1).coerceAtLeast(1) }.toFloatArray()
            bgPaint.shader = LinearGradient(0f, 0f, w, h, colors, positions, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, bgPaint)
        } else {
            bgPaint.color = project.backgroundColorLong.toInt()
            canvas.drawRect(0f, 0f, w, h, bgPaint)
        }

        if (project.backgroundFade > 0f) {
            val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = ((project.backgroundFade / 100f) * 0.7f * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawRect(0f, 0f, w, h, fadePaint)
        }
    }

    private fun drawPhotoGrid(
        canvas: Canvas,
        project: CollageProject,
        w: Float,
        h: Float,
        photoBitmaps: Map<String, Bitmap>
    ) {
        val outerSpacing = project.outerSpacingRatio * w
        val innerSpacing = project.innerSpacingRatio * w
        val cornerRadius = project.cornerRadiusRatio * w

        val content = RectF(outerSpacing, outerSpacing, w - outerSpacing, h - outerSpacing)
        val pad = innerSpacing / 2f

        project.slots.forEachIndexed { index, slot ->
            val photoTransform = project.photos.getOrNull(index)
            val uri = photoTransform?.uri ?: project.photos.firstOrNull()?.uri ?: ""

            val rect = RectF(
                content.left + slot.left * content.width() + pad,
                content.top + slot.top * content.height() + pad,
                content.left + slot.right * content.width() - pad,
                content.top + slot.bottom * content.height() - pad
            )
            if (rect.width() <= 1f || rect.height() <= 1f) return@forEachIndexed

            val clipPath = Path().apply {
                addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
            }

            canvas.save()
            canvas.clipPath(clipPath)

            val photoBitmap = photoBitmaps[uri]
            if (photoBitmap != null && !photoBitmap.isRecycled) {
                val baseScale = max(rect.width() / photoBitmap.width, rect.height() / photoBitmap.height)
                val userScale = photoTransform?.scale ?: 1f
                val totalScale = baseScale * userScale

                val pw = photoBitmap.width * totalScale
                val ph = photoBitmap.height * totalScale

                val panX = (photoTransform?.panX ?: 0f) * rect.width()
                val panY = (photoTransform?.panY ?: 0f) * rect.height()

                val cx = rect.centerX() + panX
                val cy = rect.centerY() + panY

                canvas.save()
                if (photoTransform != null && photoTransform.rotation != 0f) {
                    canvas.rotate(photoTransform.rotation, rect.centerX(), rect.centerY())
                }
                val dest = RectF(cx - pw / 2f, cy - ph / 2f, cx + pw / 2f, cy + ph / 2f)
                canvas.drawBitmap(photoBitmap, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            } else {
                val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY }
                canvas.drawRect(rect, placeholderPaint)
            }

            // Per-photo Filter Tint
            val filterColor = getFilterColorInt(project.filterPreset)
            if (filterColor != Color.TRANSPARENT && project.filterIntensity > 0f) {
                val filterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = filterColor
                    alpha = ((project.filterIntensity / 100f) * 0.45f * 255).toInt().coerceIn(0, 255)
                }
                canvas.drawRect(rect, filterPaint)
            }

            canvas.restore()
        }
    }

    private fun drawGlobalAdjustments(
        canvas: Canvas,
        project: CollageProject,
        w: Float,
        h: Float
    ) {
        val brightnessVal = (project.adjustValues["Brightness"] ?: 0f) / 50f
        val contrastVal = (project.adjustValues["Contrast"] ?: 0f) / 50f
        val highlightVal = (project.adjustValues["Highlight"] ?: 0f) / 50f
        val shadowVal = (project.adjustValues["Shadow"] ?: 0f) / 50f
        val saturationVal = (project.adjustValues["Saturation"] ?: 0f) / 50f
        val warmthVal = (project.adjustValues["Warmth"] ?: 0f) / 50f
        val tintVal = (project.adjustValues["Tint"] ?: 0f) / 50f

        if (abs(brightnessVal) > 0.01f) {
            val color = if (brightnessVal > 0f) Color.WHITE else Color.BLACK
            val alpha = (abs(brightnessVal) * 0.35f * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, Paint().apply { this.color = color; this.alpha = alpha })
        }
        if (abs(contrastVal) > 0.01f) {
            val color = if (contrastVal > 0f) Color.BLACK else Color.WHITE
            val alpha = (abs(contrastVal) * 0.20f * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, Paint().apply { this.color = color; this.alpha = alpha })
        }
        if (highlightVal > 0.01f) {
            val alpha = (highlightVal * 0.25f * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, Paint().apply { color = Color.WHITE; this.alpha = alpha })
        }
        if (shadowVal < -0.01f) {
            val alpha = (-shadowVal * 0.25f * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, Paint().apply { color = Color.BLACK; this.alpha = alpha })
        }
        if (saturationVal < -0.01f) {
            val alpha = (-saturationVal * 0.5f * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, Paint().apply { color = Color.GRAY; this.alpha = alpha })
        }
        if (warmthVal > 0.01f) {
            val alpha = (warmthVal * 0.25f * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, Paint().apply { color = Color.rgb(255, 183, 3); this.alpha = alpha })
        }
        if (tintVal > 0.01f) {
            val alpha = (tintVal * 0.25f * 255).toInt().coerceIn(0, 255)
            canvas.drawRect(0f, 0f, w, h, Paint().apply { color = Color.rgb(251, 111, 146); this.alpha = alpha })
        }
    }

    private fun drawStrokes(
        canvas: Canvas,
        project: CollageProject,
        w: Float,
        h: Float
    ) {
        project.drawStrokes.forEach { stroke ->
            if (stroke.points.size > 1 && !stroke.isEraser) {
                val path = Path().apply {
                    val first = stroke.points.first()
                    moveTo(first.x * w, first.y * h)
                    stroke.points.drop(1).forEach { pt ->
                        lineTo(pt.x * w, pt.y * h)
                    }
                }
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stroke.colorLong.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = stroke.strokeWidthRatio * w
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawOverlays(
        canvas: Canvas,
        project: CollageProject,
        w: Float,
        h: Float,
        photoBitmaps: Map<String, Bitmap>
    ) {
        val sortedOverlays = project.overlays.sortedBy { it.zIndex }
        sortedOverlays.forEach { item ->
            val cx = item.x * w
            val cy = item.y * h

            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(item.rotation)
            canvas.scale(item.scale, item.scale)

            when (item) {
                is ProjectOverlayItem.Emoji -> {
                    val fontSize = 0.11f * w
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = fontSize
                        textAlign = Paint.Align.CENTER
                        color = Color.WHITE
                    }
                    val textBounds = Rect()
                    paint.getTextBounds(item.emoji, 0, item.emoji.length, textBounds)

                    // Draw semi-transparent pill matching Compose preview
                    val pillPadH = 0.035f * w
                    val pillPadV = 0.025f * w
                    val pillWidth = textBounds.width().toFloat() + pillPadH * 2f
                    val pillHeight = textBounds.height().toFloat() + pillPadV * 2f
                    val pillRect = RectF(-pillWidth / 2f, -pillHeight / 2f, pillWidth / 2f, pillHeight / 2f)
                    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        alpha = (0.35f * 255).toInt()
                    }
                    canvas.drawRoundRect(pillRect, 0.025f * w, 0.025f * w, pillPaint)

                    // Draw emoji precisely centered at (0, 0)
                    val yPos = -textBounds.exactCenterY()
                    canvas.drawText(item.emoji, 0f, yPos, paint)
                }

                is ProjectOverlayItem.Photo -> {
                    val stickerBitmap = photoBitmaps[item.uri]
                    if (stickerBitmap != null && !stickerBitmap.isRecycled) {
                        val size = 0.25f * w
                        val pillPad = 0.02f * w
                        val pillRect = RectF(-size / 2f - pillPad, -size / 2f - pillPad, size / 2f + pillPad, size / 2f + pillPad)
                        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.BLACK
                            alpha = (0.35f * 255).toInt()
                        }
                        canvas.drawRoundRect(pillRect, 0.025f * w, 0.025f * w, pillPaint)

                        val photoDest = RectF(-size / 2f, -size / 2f, size / 2f, size / 2f)
                        val photoClip = Path().apply {
                            addRoundRect(photoDest, 0.02f * w, 0.02f * w, Path.Direction.CW)
                        }
                        canvas.save()
                        canvas.clipPath(photoClip)
                        canvas.drawBitmap(stickerBitmap, null, photoDest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                        canvas.restore()
                    }
                }
            }
            canvas.restore()
        }
    }

    private fun drawTextLayers(
        canvas: Canvas,
        project: CollageProject,
        w: Float,
        h: Float
    ) {
        val sortedText = project.textLayers.sortedBy { it.zIndex }
        sortedText.forEach { textLayer ->
            if (textLayer.text.isBlank()) return@forEach

            val cx = textLayer.x * w
            val cy = textLayer.y * h

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textLayer.colorLong.toInt()
                textSize = textLayer.fontSizeRatio * w
                textAlign = Paint.Align.CENTER
                typeface = when (textLayer.fontName) {
                    "Retro" -> Typeface.MONOSPACE
                    "Serif" -> Typeface.SERIF
                    "Bold", "Neon" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    else -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                }
                if (textLayer.fontName == "Neon") {
                    setShadowLayer(0.04f * w, 0f, 0f, textLayer.colorLong.toInt())
                }
            }

            val textBounds = Rect()
            paint.getTextBounds(textLayer.text, 0, textLayer.text.length, textBounds)

            val hPad = 0.05f * w
            val vPad = 0.025f * w
            val pillWidth = textBounds.width().toFloat() + hPad * 2f
            val pillHeight = textBounds.height().toFloat() + vPad * 2f

            canvas.save()
            canvas.translate(cx, cy)
            if (textLayer.rotation != 0f) {
                canvas.rotate(textLayer.rotation)
            }

            // Draw dark rounded background pill matching Compose preview
            val pillRect = RectF(-pillWidth / 2f, -pillHeight / 2f, pillWidth / 2f, pillHeight / 2f)
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                alpha = (0.45f * 255).toInt()
            }
            canvas.drawRoundRect(pillRect, 0.015f * w, 0.015f * w, pillPaint)

            // Draw text precisely centered
            val yPos = -textBounds.exactCenterY()
            canvas.drawText(textLayer.text, 0f, yPos, paint)

            canvas.restore()
        }
    }

    private fun getFilterColorInt(preset: String): Int {
        return when (preset) {
            "Clarendon" -> Color.parseColor("#38BDF8")
            "Gingham" -> Color.parseColor("#F1F5F9")
            "Moon" -> Color.parseColor("#94A3B8")
            "Lark" -> Color.parseColor("#FDE047")
            "Reyes" -> Color.parseColor("#FED7AA")
            "Juno" -> Color.parseColor("#60A5FA")
            "Slumber" -> Color.parseColor("#CBD5E1")
            "Crema" -> Color.parseColor("#FEF08A")
            "Ludwig" -> Color.parseColor("#FCA5A5")
            "Aden" -> Color.parseColor("#F472B6")
            "Perpetua" -> Color.parseColor("#34D399")
            "Amaro" -> Color.parseColor("#FCD34D")
            "Mayfair" -> Color.parseColor("#FBCFE8")
            "Rise" -> Color.parseColor("#FDE68A")
            "Hudson" -> Color.parseColor("#93C5FD")
            "Valencia" -> Color.parseColor("#FDBA74")
            "X-Pro II" -> Color.parseColor("#818CF8")
            "Sierra" -> Color.parseColor("#D8B4FE")
            "Willow" -> Color.parseColor("#E2E8F0")
            "Lo-fi" -> Color.parseColor("#F43F5E")
            "Vivid" -> Color.parseColor("#E11D48")
            "Golden" -> Color.parseColor("#F59E0B")
            "Retro" -> Color.parseColor("#9A3412")
            "Soft" -> Color.parseColor("#FBCFE8")
            "Moody" -> Color.parseColor("#1E3A8A")
            "Warm" -> Color.parseColor("#FB923C")
            "Vintage" -> Color.parseColor("#78350F")
            "B&W" -> Color.GRAY
            "Neon" -> Color.parseColor("#10B981")
            else -> Color.TRANSPARENT
        }
    }
}
