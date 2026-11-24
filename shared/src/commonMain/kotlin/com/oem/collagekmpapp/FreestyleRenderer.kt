package com.oem.collagekmpapp

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
import kotlin.math.max

/**
 * High-resolution renderer for Freestyle collages.
 * Fully separate from CollageRenderer's grid pipeline (CollageRenderer.renderFreestyleToBitmap just delegates here).
 * Z-order: background -> items (list order) -> freehand strokes.
 */
object FreestyleRenderer {

    fun render(
        project: FreestyleProject,
        width: Int,
        height: Int,
        photoBitmaps: Map<String, Bitmap> = emptyMap(),
        backgroundBitmap: Bitmap? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap), project, width, height, photoBitmaps, backgroundBitmap)
        return bitmap
    }

    fun draw(
        canvas: Canvas,
        project: FreestyleProject,
        width: Int,
        height: Int,
        photoBitmaps: Map<String, Bitmap>,
        backgroundBitmap: Bitmap?
    ) {
        val w = width.toFloat()
        val h = height.toFloat()

        drawBackground(canvas, project, w, h, backgroundBitmap)

        val matrix = PhotoColorEngine.buildMatrix(project.filterPreset, project.filterIntensity, project.adjustValues)
        val fx = PhotoColorEngine.effects(project.filterPreset, project.filterIntensity, project.adjustValues)
        val exportScale = (w / 1080f).coerceAtLeast(1f)

        project.items.forEach { raw ->
            val bmp = if (raw.kind == FreestyleItemKind.PHOTO) photoBitmaps[raw.content]?.takeIf { !it.isRecycled } else null
            // use the real (EXIF-corrected) bitmap aspect so the frame always hugs the picture
            val item = if (bmp != null) raw.copy(aspect = bmp.width.toFloat() / bmp.height) else raw

            val iw = item.widthUnits * w
            val ih = item.heightUnits * w
            val rect = RectF(-iw / 2f, -ih / 2f, iw / 2f, ih / 2f)

            canvas.save()
            canvas.translate(item.x * w, item.y * h)
            canvas.rotate(item.rotation)
            canvas.scale(item.scale, item.scale)
            when (item.kind) {
                FreestyleItemKind.PHOTO -> drawPhotoItem(canvas, item, bmp, rect, w, matrix, fx, exportScale)
                FreestyleItemKind.EMOJI -> drawEmoji(canvas, item, iw)
                FreestyleItemKind.TEXT -> drawTextItem(canvas, item, rect, w)
            }
            canvas.restore()
        }

        drawStrokes(canvas, project, w, h)
    }

    // ------------------------------------------------------------------ background

    private fun drawBackground(canvas: Canvas, project: FreestyleProject, w: Float, h: Float, bg: Bitmap?) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (bg != null && !bg.isRecycled) {
            // heavy blur = shrink hard, then scale back up with bilinear filtering
            val src = if (project.backgroundBlur > 0.01f) {
                val f = 1f / (1f + project.backgroundBlur * 40f)
                Bitmap.createScaledBitmap(
                    bg,
                    max(2, (bg.width * f).toInt()),
                    max(2, (bg.height * f).toInt()),
                    true
                )
            } else bg
            val s = max(w / src.width, h / src.height)
            val bw = src.width * s
            val bh = src.height * s
            val dest = RectF(w / 2f - bw / 2f, h / 2f - bh / 2f, w / 2f + bw / 2f, h / 2f + bh / 2f)
            canvas.drawBitmap(src, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            if (src !== bg) src.recycle()
        } else if (project.backgroundGradientColors.size >= 2) {
            val colors = project.backgroundGradientColors.map { it.toInt() }.toIntArray()
            val positions = FloatArray(colors.size) { it.toFloat() / (colors.size - 1) }
            paint.shader = LinearGradient(0f, 0f, w, h, colors, positions, Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, w, h, paint)
        } else {
            paint.color = project.backgroundColorLong.toInt()
            canvas.drawRect(0f, 0f, w, h, paint)
        }
        drawTexture(canvas, project.backgroundTexture, w, h)
    }

    /** Procedural wallpaper textures. Compose preview draws the exact same patterns. */
    private fun drawTexture(canvas: Canvas, texture: BackgroundTexture, w: Float, h: Float) {
        if (texture == BackgroundTexture.NONE) return
        val step = 0.05f * w
        val sw = 0.004f * w
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(34, 0, 0, 0)
            strokeWidth = sw
            style = Paint.Style.FILL
        }
        when (texture) {
            BackgroundTexture.DOTS -> {
                var y = step / 2f
                while (y < h) {
                    var x = step / 2f
                    while (x < w) { canvas.drawCircle(x, y, 0.006f * w, p); x += step }
                    y += step
                }
            }
            BackgroundTexture.GRID -> {
                var x = 0f
                while (x <= w) { canvas.drawLine(x, 0f, x, h, p); x += step }
                var y = 0f
                while (y <= h) { canvas.drawLine(0f, y, w, y, p); y += step }
            }
            BackgroundTexture.STRIPES -> {
                p.strokeWidth = sw * 2f
                var x = -h
                while (x < w) { canvas.drawLine(x, 0f, x + h, h, p); x += step * 1.5f }
            }
            BackgroundTexture.LINES -> {
                var y = step * 1.2f
                while (y < h) { canvas.drawLine(0f, y, w, y, p); y += step * 1.2f }
                p.color = Color.argb(68, 239, 68, 68)
                canvas.drawLine(0.12f * w, 0f, 0.12f * w, h, p)
            }
            BackgroundTexture.NONE -> Unit
        }
    }

    // ------------------------------------------------------------------ items

    private fun drawPhotoItem(
        canvas: Canvas,
        item: FreestyleItem,
        bmp: Bitmap?,
        rect: RectF,
        canvasW: Float,
        matrix: FloatArray?,
        fx: com.oem.collagekmpapp.PhotoEffects,
        exportScale: Float
    ) {
        val iw = rect.width()
        val radius = item.cornerUnits * canvasW
        val polaroid = item.frame == PhotoFrame.POLAROID

        if (item.shadow) {
            val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                setShadowLayer(0.025f * iw, 0f, 0.012f * iw, Color.argb(90, 0, 0, 0))
            }
            canvas.drawRoundRect(rect, radius, radius, sp)
        }

        val inner: RectF
        if (polaroid) {
            val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawRoundRect(rect, radius, radius, white)
            val side = FreestyleItem.POLAROID_SIDE * iw
            val bottom = FreestyleItem.POLAROID_BOTTOM * iw
            inner = RectF(rect.left + side, rect.top + side, rect.right - side, rect.bottom - bottom)
        } else {
            inner = rect
        }
        val innerRadius = if (polaroid) 0f else radius

        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(inner, innerRadius, innerRadius, Path.Direction.CW) })
        if (bmp != null) {
            val s = max(inner.width() / bmp.width, inner.height() / bmp.height)
            val dw = bmp.width * s
            val dh = bmp.height * s
            val dest = RectF(
                inner.centerX() - dw / 2f, inner.centerY() - dh / 2f,
                inner.centerX() + dw / 2f, inner.centerY() + dh / 2f
            )
            PhotoLook.drawPhoto(canvas, bmp, dest, inner, 1f, matrix, fx, exportScale)
            PhotoLook.drawVignette(canvas, inner, fx.vignette)
        } else {
            canvas.drawRect(inner, Paint().apply { color = Color.LTGRAY })
        }
        canvas.restore()
    }

    private fun drawEmoji(canvas: Canvas, item: FreestyleItem, iw: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = iw * 0.8f
            textAlign = Paint.Align.CENTER
            color = Color.WHITE
        }
        val b = Rect()
        paint.getTextBounds(item.content, 0, item.content.length, b)
        canvas.drawText(item.content, 0f, -b.exactCenterY(), paint)
    }

    private fun typefaceFor(font: String): Typeface = when (font) {
        "Retro" -> Typeface.MONOSPACE
        "Serif" -> Typeface.SERIF
        "Bold", "Neon" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        "Script" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        else -> Typeface.DEFAULT
    }

    private fun drawTextItem(canvas: Canvas, item: FreestyleItem, rect: RectF, canvasW: Float) {
        if (item.content.isBlank()) return
        if (item.textPill) {
            val pill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                alpha = (0.45f * 255).toInt()
            }
            canvas.drawRoundRect(rect, 0.015f * canvasW, 0.015f * canvasW, pill)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorLong.toInt()
            textSize = item.fontSizeRatio * canvasW
            textAlign = Paint.Align.CENTER
            typeface = typefaceFor(item.fontName)
            if (item.fontName == "Neon") {
                setShadowLayer(0.04f * canvasW, 0f, 0f, item.colorLong.toInt())
            } else if (item.textShadow) {
                setShadowLayer(0.012f * canvasW, 0.006f * canvasW, 0.006f * canvasW, Color.argb(153, 0, 0, 0))
            }
        }
        val b = Rect()
        paint.getTextBounds(item.content, 0, item.content.length, b)
        canvas.drawText(item.content, 0f, -b.exactCenterY(), paint)
    }

    // ------------------------------------------------------------------ doodles

    private fun drawStrokes(canvas: Canvas, project: FreestyleProject, w: Float, h: Float) {
        project.drawStrokes.forEach { stroke ->
            if (stroke.isEraser || stroke.points.isEmpty()) return@forEach
            val widthPx = stroke.strokeWidthRatio * w
            if (stroke.points.size == 1) {
                val p = stroke.points[0]
                val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = stroke.colorLong.toInt() }
                canvas.drawCircle(p.x * w, p.y * h, widthPx / 2f, fill)
                return@forEach
            }
            val path = Path().apply {
                moveTo(stroke.points.first().x * w, stroke.points.first().y * h)
                for (i in 1 until stroke.points.size) lineTo(stroke.points[i].x * w, stroke.points[i].y * h)
            }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stroke.colorLong.toInt()
                style = Paint.Style.STROKE
                strokeWidth = widthPx
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            canvas.drawPath(path, paint)
        }
    }
}
