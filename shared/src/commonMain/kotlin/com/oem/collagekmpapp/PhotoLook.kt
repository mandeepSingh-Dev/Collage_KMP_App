package com.oem.collagekmpapp

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * androidMain: draws ONE photo onto the export canvas with the full look
 * (colour matrix + blur / soften / clarity / sharpen / glow / grain), mirroring FilteredPhoto in Compose.
 */
object PhotoLook {

    /** Kept for compatibility: paint that colour-grades a bitmap. */
    fun photoPaint(preset: String, intensity: Float, adjustValues: Map<String, Float>): Paint =
        gradePaint(PhotoColorEngine.buildMatrix(preset, intensity, adjustValues))

    private fun gradePaint(matrix: FloatArray?, alpha: Int = 255, mode: PorterDuff.Mode? = null): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (matrix != null) p.colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix))
        p.alpha = alpha.coerceIn(0, 255)
        if (mode != null) p.xfermode = PorterDuffXfermode(mode)
        return p
    }

    /**
     * @param dest        where the (zoomed/panned) photo lands on the canvas
     * @param slotRect    the visible slot rectangle (radii are relative to its width)
     * @param zoom        user pinch-zoom (preview blur is scaled by zoom too)
     * @param exportScale canvasWidth / 1080, keeps grain size consistent at 2K / 4K
     */
    fun drawPhoto(
        canvas: Canvas,
        bitmap: Bitmap,
        dest: RectF,
        slotRect: RectF,
        zoom: Float,
        matrix: FloatArray?,
        fx: PhotoEffects,
        exportScale: Float,
        lens: LensBlurState? = null,                       // Lens Blur, normalised over slotRect
        selective: List<SelectivePoint> = emptyList()      // Selective-edit control points, normalised over slotRect
    ) {
        val lensSpec = lens?.takeIf { it.active }
        if (lensSpec == null) {
            drawStack(canvas, bitmap, dest, slotRect, zoom, matrix, fx, selective)
        } else {
            // Render the fully processed stack into a slot-sized layer, then composite the sharp layer and a
            // blurred, masked copy of the same layer (mirrors FilteredPhoto's double stack).
            val bw = ceil(slotRect.width()).toInt().coerceAtLeast(2)
            val bh = ceil(slotRect.height()).toInt().coerceAtLeast(2)
            val layer = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            val lc = Canvas(layer)
            lc.translate(-slotRect.left, -slotRect.top)
            drawStack(lc, bitmap, dest, slotRect, zoom, matrix, fx, selective)
            canvas.drawBitmap(layer, null, slotRect, Paint(Paint.FILTER_BITMAP_FLAG))
            val blurred = blur(layer, EffectRadii.LENS * (lensSpec.amount / 100f) * slotRect.width() * zoom)
            drawLensBlurred(canvas, blurred, slotRect, lensSpec)
            blurred.recycle()
            layer.recycle()
        }

        // 6. film grain
        if (fx.grain > 0.01f) drawGrain(canvas, dest, fx.grain, exportScale)
    }

    /** Steps 1-5 (+ structure, glamour, selective): everything that touches the photo pixels. */
    private fun drawStack(
        canvas: Canvas,
        bitmap: Bitmap,
        dest: RectF,
        slotRect: RectF,
        zoom: Float,
        matrix: FloatArray?,
        fx: PhotoEffects,
        selective: List<SelectivePoint>
    ) {
        val slotW = slotRect.width()
        val needWork = fx.hasSpatial || selective.any { abs(it.structure) >= 0.5f }
        val work: Bitmap? = if (needWork) workBitmap(bitmap, dest) else null
        val toWork = if (work != null) work.width / dest.width() else 1f
        fun blurred(frac: Float): Bitmap = blur(work!!, frac * slotW * zoom * toWork)

        // 1. base photo (optionally real gaussian-style blur)
        if (fx.blur > 0.01f) {
            val b = blurred(EffectRadii.BLUR * fx.blur)
            canvas.drawBitmap(b, null, dest, gradePaint(matrix))
            b.recycle()
        } else {
            canvas.drawBitmap(bitmap, null, dest, gradePaint(matrix))
        }

        // 2. soften (skin-smoothing style blend of a blurred copy)
        if (fx.soften > 0.01f) {
            val b = blurred(EffectRadii.SOFTEN)
            canvas.drawBitmap(b, null, dest, gradePaint(matrix, (fx.soften * 0.85f * 255f).toInt()))
            b.recycle()
        }

        // 3. clarity: negative = dreamy soft, positive = local contrast (large-radius high-pass)
        if (fx.clarity < -0.01f) {
            val b = blurred(EffectRadii.CLARITY)
            canvas.drawBitmap(b, null, dest, gradePaint(matrix, (-fx.clarity * 0.7f * 255f).toInt()))
            b.recycle()
        } else if (fx.clarity > 0.01f) {
            val b = blurred(EffectRadii.CLARITY)
            highPass(canvas, bitmap, b, dest, matrix, fx.clarity)
            b.recycle()
        }

        // 4. sharpen (small-radius high-pass overlay = unsharp mask)
        if (fx.sharpen > 0.01f) {
            val b = blurred(EffectRadii.SHARPEN)
            highPass(canvas, bitmap, b, dest, matrix, fx.sharpen)
            b.recycle()
        }

        // 5. glow / bloom (blurred copy, screen blend)
        if (fx.glow > 0.01f) {
            val b = blurred(EffectRadii.GLOW)
            canvas.drawBitmap(b, null, dest, gradePaint(matrix, (fx.glow * 0.6f * 255f).toInt(), PorterDuff.Mode.SCREEN))
            b.recycle()
        }

        // 5a. structure: mid-tone local contrast (mid-radius high-pass overlay); negative = smoother
        if (fx.structure > 0.01f) {
            val b = blurred(EffectRadii.STRUCTURE)
            highPass(canvas, bitmap, b, dest, matrix, fx.structure)
            b.recycle()
        } else if (fx.structure < -0.01f) {
            val b = blurred(EffectRadii.STRUCTURE * 2f)
            canvas.drawBitmap(b, null, dest, gradePaint(matrix, (-fx.structure * 0.6f * 255f).toInt()))
            b.recycle()
        }

        // 5b. glamour glow: soft-focus blend + screen bloom of a heavily blurred copy
        if (fx.glamour > 0.01f) {
            val b = blurred(EffectRadii.GLAMOUR)
            canvas.drawBitmap(b, null, dest, gradePaint(matrix, (fx.glamour * 0.45f * 255f).toInt()))
            canvas.drawBitmap(b, null, dest, gradePaint(matrix, (fx.glamour * 0.5f * 255f).toInt(), PorterDuff.Mode.SCREEN))
            b.recycle()
        }

        // 5c. selective edit control points
        for (p in selective) if (!p.isNeutral) drawSelective(canvas, bitmap, dest, slotRect, zoom, matrix, p, work, toWork)

        if (work != null && work !== bitmap) work.recycle()
    }

    /** One control point: locally graded copy of the photo (+ structure) inside a soft circular mask. */
    private fun drawSelective(
        canvas: Canvas,
        bitmap: Bitmap,
        dest: RectF,
        slotRect: RectF,
        zoom: Float,
        baseMatrix: FloatArray?,
        p: SelectivePoint,
        work: Bitmap?,
        toWork: Float
    ) {
        val cx = slotRect.left + p.x * slotRect.width()
        val cy = slotRect.top + p.y * slotRect.height()
        val r = (p.radius * slotRect.width()).coerceAtLeast(1f)
        val bounds = RectF(cx - r, cy - r, cx + r, cy + r)
        if (!bounds.intersect(RectF(canvas.clipBounds))) return
        val pm = PhotoColorEngine.selectiveMatrix(baseMatrix, p)

        val save = canvas.saveLayer(bounds, null)
        canvas.drawBitmap(bitmap, null, dest, gradePaint(pm))
        if (work != null && p.structure >= 0.5f) {
            val b = blur(work, EffectRadii.STRUCTURE * slotRect.width() * zoom * toWork)
            highPass(canvas, bitmap, b, dest, pm, p.structure / 50f)
            b.recycle()
        } else if (work != null && p.structure <= -0.5f) {
            val b = blur(work, EffectRadii.CLARITY * slotRect.width() * zoom * toWork)
            canvas.drawBitmap(b, null, dest, gradePaint(pm, (-p.structure / 50f * 0.7f * 255f).toInt()))
            b.recycle()
        }
        val mask = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = RadialGradient(
                cx, cy, r,
                intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(bounds, mask)
        canvas.restoreToCount(save)
    }

    /** Blurred copy of the processed stack, visible only outside the clear zone / across the feather ring. */
    private fun drawLensBlurred(canvas: Canvas, blurred: Bitmap, rect: RectF, l: LensBlurState) {
        val cx = rect.left + l.cx * rect.width()
        val cy = rect.top + l.cy * rect.height()
        val total = (l.clear + l.feather).coerceAtLeast(0.001f)
        val rx = (total * rect.width() * l.axisX).coerceAtLeast(1f)
        val k = l.axisY / l.axisX
        val inner = (l.clear / total).coerceIn(0f, 0.99f)

        val save = canvas.saveLayer(rect, null)
        canvas.drawBitmap(blurred, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
        val mask = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = RadialGradient(
                cx, cy, rx,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.BLACK),
                floatArrayOf(0f, inner, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.save()
        canvas.scale(1f, k, cx, cy)                       // circle -> ellipse for Horizontal mode
        val big = max(rect.width(), rect.height()) * 4f   // DST_IN only touches what is drawn: cover the whole layer
        canvas.drawRect(rect.left - big, cy - big, rect.right + big, cy + big, mask)
        canvas.restore()
        canvas.restoreToCount(save)
    }

    /** Public gaussian-style blur (sigma ~ radius px) for other renderers (e.g. Freestyle background). Returns a NEW bitmap. */
    fun blurBitmap(src: Bitmap, radius: Float): Bitmap = blur(src, radius.coerceAtLeast(0.5f))

    /** Vignette. strength > 0 darkens the edges, < 0 lightens them (white vignette). */
    fun drawVignette(canvas: Canvas, rect: RectF, strength: Float) {
        val s = kotlin.math.abs(strength)
        if (s <= 0.01f) return
        val base = if (strength > 0f) 0 else 255
        val radius = hypot(rect.width(), rect.height()) / 2f
        val edge = Color.argb((s * 255f).toInt().coerceIn(0, 255), base, base, base)
        val clear = Color.argb(0, base, base, base)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                rect.centerX(), rect.centerY(), radius,
                intArrayOf(clear, edge), floatArrayOf(0.5f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(rect, paint)
    }

    // ------------------------------------------------------------------ helpers

    /** sharp + inverted-blur at 50% = high-pass; overlay-blended onto the photo. */
    private fun highPass(canvas: Canvas, sharp: Bitmap, blurred: Bitmap, dest: RectF, matrix: FloatArray?, strength: Float) {
        val bounds = RectF(dest)
        if (!bounds.intersect(RectF(canvas.clipBounds))) return
        val layerPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            alpha = (strength.coerceIn(0f, 1f) * 255f).toInt()
        }
        val save = canvas.saveLayer(bounds, layerPaint)
        canvas.drawBitmap(sharp, null, dest, gradePaint(matrix))
        canvas.drawBitmap(blurred, null, dest, gradePaint(PhotoColorEngine.inverted(matrix), 128))
        canvas.restoreToCount(save)
    }

    private val grainTile: Bitmap by lazy {
        val n = GrainPattern.SIZE
        val px = IntArray(n * n) { val v = GrainPattern.values[it]; Color.rgb(v, v, v) }
        Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888)
    }

    private fun drawGrain(canvas: Canvas, dest: RectF, amount: Float, exportScale: Float) {
        val bounds = RectF(dest)
        if (!bounds.intersect(RectF(canvas.clipBounds))) return
        val shader = BitmapShader(grainTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
            setLocalMatrix(Matrix().apply { setScale(exportScale, exportScale) })
        }
        val paint = Paint().apply {
            this.shader = shader
            xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            alpha = (amount * 0.55f * 255f).toInt().coerceIn(0, 255)
        }
        canvas.drawRect(bounds, paint)
    }

    /** Photo scaled to (at most) 1600px so the blurs stay fast even for 4K exports. */
    private fun workBitmap(src: Bitmap, dest: RectF): Bitmap {
        val k = min(1f, 1600f / max(dest.width(), dest.height()))
        val w = (dest.width() * k).toInt().coerceAtLeast(2)
        val h = (dest.height() * k).toInt().coerceAtLeast(2)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    /** 3-pass separable box blur (~gaussian). Always returns a NEW bitmap. radius ~ sigma in px. */
    private fun blur(src: Bitmap, radius: Float): Bitmap {
        var k = 1
        while (radius / k > 24f) k *= 2
        val scaled = if (k == 1) src else Bitmap.createScaledBitmap(src, (src.width / k).coerceAtLeast(2), (src.height / k).coerceAtLeast(2), true)
        val w = scaled.width
        val h = scaled.height
        val a = IntArray(w * h)
        scaled.getPixels(a, 0, w, 0, 0, w, h)
        if (scaled !== src) scaled.recycle()
        val b = IntArray(w * h)
        val r = (radius / k).roundToInt().coerceIn(1, 24)
        repeat(3) {
            boxBlur(a, b, w, h, r, true)
            boxBlur(b, a, w, h, r, false)
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(a, 0, w, 0, 0, w, h)
        return out
    }

    private fun boxBlur(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        val step = if (horizontal) 1 else w
        val lineStep = if (horizontal) w else 1
        val div = 2 * r + 1
        for (l in 0 until lines) {
            val base = l * lineStep
            var sr = 0
            var sg = 0
            var sb = 0
            for (i in -r..r) {
                val p = src[base + i.coerceIn(0, len - 1) * step]
                sr += (p shr 16) and 255
                sg += (p shr 8) and 255
                sb += p and 255
            }
            for (i in 0 until len) {
                dst[base + i * step] = (0xFF shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val add = src[base + (i + r + 1).coerceAtMost(len - 1) * step]
                val rem = src[base + (i - r).coerceAtLeast(0) * step]
                sr += ((add shr 16) and 255) - ((rem shr 16) and 255)
                sg += ((add shr 8) and 255) - ((rem shr 8) and 255)
                sb += (add and 255) - (rem and 255)
            }
        }
    }
}