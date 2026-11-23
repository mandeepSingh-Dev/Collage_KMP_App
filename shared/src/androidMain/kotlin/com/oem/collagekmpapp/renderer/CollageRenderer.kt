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
import com.oem.collagekmpapp.PhotoColorEngine
import com.oem.collagekmpapp.PhotoLook
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

        val photoPaint = PhotoLook.photoPaint(project.filterPreset, project.filterIntensity, project.adjustValues)
        val vignette = PhotoColorEngine.vignetteStrength(project.filterPreset, project.filterIntensity)
        val glow = PhotoColorEngine.glowStrength(project.filterPreset, project.filterIntensity)

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
                canvas.drawBitmap(photoBitmap, null, dest, photoPaint)
                PhotoLook.drawGlow(canvas, photoBitmap, dest, photoPaint.colorFilter, glow)
                canvas.restore()
            } else {
                val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY }
                canvas.drawRect(rect, placeholderPaint)
            }

            PhotoLook.drawVignette(canvas, rect, vignette)

            canvas.restore()
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
}