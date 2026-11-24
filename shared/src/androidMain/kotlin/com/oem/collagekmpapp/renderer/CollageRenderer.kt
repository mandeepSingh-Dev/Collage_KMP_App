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
import com.oem.collagekmpapp.OverlayMetrics
import com.oem.collagekmpapp.PhotoColorEngine
import com.oem.collagekmpapp.PhotoLook
import com.oem.collagekmpapp.model.CollageProject
import com.oem.collagekmpapp.model.ProjectOverlayItem
import com.oem.collagekmpapp.model.ProjectTextLayer
import kotlin.math.abs
import kotlin.math.max

/**
 * Shared high-resolution offscreen canvas renderer.
 * Draws the complete CollageProject edit model onto any android.graphics.Canvas
 * using normalized relative coordinates (0.0 .. 1.0) so that preview and export
 * produce 100% identical visuals regardless of target resolution.
 */
object CollageRenderer {

    private const val SLOT_BACKING = 0xFFCBD5E1.toInt()

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

        // 5. STICKERS, EMOJIS and TEXT in ONE z-ordered pass (same stacking order as the editor:
        //    the editor keeps stickers and text overlays in a single list)
        drawOverlayStack(canvas, project, w, h, photoBitmaps)
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

        // Same engine as the Compose preview -> what you see is what you export
        val matrix = PhotoColorEngine.buildMatrix(project.filterPreset, project.filterIntensity, project.adjustValues)
        val fx = PhotoColorEngine.effects(project.filterPreset, project.filterIntensity, project.adjustValues)
        val exportScale = w / 1080f   // grain tile = 256px @ 1080px canvas, same ratio as the preview

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

            // same slate-300 backing the editor shows behind a photo that is zoomed out below 1x
            canvas.drawRect(rect, Paint().apply { color = SLOT_BACKING })

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
                PhotoLook.drawPhoto(canvas, photoBitmap, dest, rect, userScale, matrix, fx, exportScale)
                canvas.restore()
            } else {
                val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY }
                canvas.drawRect(rect, placeholderPaint)
            }

            PhotoLook.drawVignette(canvas, rect, fx.vignette)

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

    // ------------------------------------------------------------------ stickers + text

    private fun drawOverlayStack(
        canvas: Canvas,
        project: CollageProject,
        w: Float,
        h: Float,
        photoBitmaps: Map<String, Bitmap>
    ) {
        val layers: List<Pair<Float, Any>> =
            project.overlays.map { it.zIndex to (it as Any) } +
                    project.textLayers.map { it.zIndex to (it as Any) }
        layers.sortedBy { it.first }.forEach { (_, layer) ->
            when (layer) {
                is ProjectOverlayItem -> drawOverlay(canvas, layer, w, h, photoBitmaps)
                is ProjectTextLayer -> drawTextLayer(canvas, layer, w, h)
            }
        }
    }

    private fun refUnit(w: Float) = w / OverlayMetrics.REF_WIDTH

    private fun pillPaint(alpha: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        this.alpha = (alpha * 255).toInt()
    }

    private fun drawOverlay(
        canvas: Canvas,
        item: ProjectOverlayItem,
        w: Float,
        h: Float,
        photoBitmaps: Map<String, Bitmap>
    ) {
        val u = refUnit(w)
        canvas.save()
        canvas.translate(item.x * w, item.y * h)
        canvas.rotate(item.rotation)
        canvas.scale(item.scale, item.scale)

        val pad = OverlayMetrics.PILL_PAD * u
        val pillR = OverlayMetrics.PILL_RADIUS * u

        when (item) {
            is ProjectOverlayItem.Emoji -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = OverlayMetrics.EMOJI_FONT * u
                    textAlign = Paint.Align.CENTER
                    color = Color.WHITE
                }
                // pill = text line box + padding, exactly like Compose's Text inside a padded Box
                val fm = paint.fontMetrics
                val tw = paint.measureText(item.emoji)
                val th = fm.descent - fm.ascent
                val pill = RectF(-(tw / 2f + pad), -(th / 2f + pad), tw / 2f + pad, th / 2f + pad)
                canvas.drawRoundRect(pill, pillR, pillR, pillPaint(OverlayMetrics.PILL_ALPHA))
                canvas.drawText(item.emoji, 0f, -(fm.ascent + fm.descent) / 2f, paint)
            }

            is ProjectOverlayItem.Photo -> {
                val bmp = photoBitmaps[item.uri]
                if (bmp != null && !bmp.isRecycled) {
                    val size = OverlayMetrics.PHOTO_SIZE * u
                    val pill = RectF(-size / 2f - pad, -size / 2f - pad, size / 2f + pad, size / 2f + pad)
                    canvas.drawRoundRect(pill, pillR, pillR, pillPaint(OverlayMetrics.PILL_ALPHA))

                    val dest = RectF(-size / 2f, -size / 2f, size / 2f, size / 2f)
                    val r = OverlayMetrics.PHOTO_RADIUS * u
                    canvas.save()
                    canvas.clipPath(Path().apply { addRoundRect(dest, r, r, Path.Direction.CW) })
                    // ContentScale.Crop: fill the square, never stretch
                    val s = max(size / bmp.width, size / bmp.height)
                    val dw = bmp.width * s
                    val dh = bmp.height * s
                    canvas.drawBitmap(
                        bmp, null, RectF(-dw / 2f, -dh / 2f, dw / 2f, dh / 2f),
                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    )
                    canvas.restore()
                }
            }
        }
        canvas.restore()
    }

    /** Same weights / styles as the Compose TextStyles in the editor. */
    private fun typefaceFor(font: String): Typeface = when (font) {
        "Retro" -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        "Serif" -> Typeface.SERIF
        "Bold", "Neon" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        "Script" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        else -> if (android.os.Build.VERSION.SDK_INT >= 28)
            Typeface.create(Typeface.DEFAULT, 500, false)   // "Modern" = Medium, upright
        else Typeface.DEFAULT
    }

    private fun drawTextLayer(
        canvas: Canvas,
        textLayer: ProjectTextLayer,
        w: Float,
        h: Float
    ) {
        if (textLayer.text.isBlank()) return
        val u = refUnit(w)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textLayer.colorLong.toInt()
            textSize = textLayer.fontSizeRatio * w
            textAlign = Paint.Align.CENTER
            typeface = typefaceFor(textLayer.fontName)
            if (textLayer.fontName == "Neon") {
                setShadowLayer(
                    if (textLayer.compact) OverlayMetrics.NEON_BLUR * u else 0.04f * w,
                    0f, 0f, textLayer.colorLong.toInt()
                )
            }
        }

        val fm = paint.fontMetrics
        val tw = paint.measureText(textLayer.text)
        val th = fm.descent - fm.ascent

        val hPad: Float
        val vPad: Float
        val radius: Float
        val alpha: Float
        if (textLayer.compact) {
            hPad = OverlayMetrics.PILL_PAD * u
            vPad = OverlayMetrics.PILL_PAD * u
            radius = OverlayMetrics.PILL_RADIUS * u
            alpha = OverlayMetrics.PILL_ALPHA
        } else {
            hPad = 0.05f * w
            vPad = 0.025f * w
            radius = 0.015f * w
            alpha = 0.45f
        }

        canvas.save()
        canvas.translate(textLayer.x * w, textLayer.y * h)
        if (textLayer.rotation != 0f) canvas.rotate(textLayer.rotation)
        if (textLayer.scale != 1f) canvas.scale(textLayer.scale, textLayer.scale)

        val pill = RectF(-(tw / 2f + hPad), -(th / 2f + vPad), tw / 2f + hPad, th / 2f + vPad)
        canvas.drawRoundRect(pill, radius, radius, pillPaint(alpha))
        canvas.drawText(textLayer.text, 0f, -(fm.ascent + fm.descent) / 2f, paint)
        canvas.restore()
    }
}