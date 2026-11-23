package com.oem.collagekmpapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.hypot

/** androidMain: applies the shared PhotoColorEngine look to the export canvas. */
object PhotoLook {

    /** Paint that colour-grades every pixel of the bitmap drawn with it. */
    fun photoPaint(preset: String, intensity: Float, adjustValues: Map<String, Float>): Paint {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        PhotoColorEngine.buildMatrix(preset, intensity, adjustValues)?.let {
            paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(it))
        }
        return paint
    }

    /** Edge darkening, matches the Compose preview overlay. Call inside the slot clip. */
    fun drawVignette(canvas: Canvas, rect: RectF, strength: Float) {
        if (strength <= 0.01f) return
        val radius = hypot(rect.width(), rect.height()) / 2f
        val edge = Color.argb((strength * 255f).toInt().coerceIn(0, 255), 0, 0, 0)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                rect.centerX(), rect.centerY(), radius,
                intArrayOf(0x00000000, edge), floatArrayOf(0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(rect, paint)
    }

    /**
     * Soft bloom: a heavily down-sampled (= blurred) copy of the graded photo is screen-blended
     * over it. Bright skin spreads a gentle light -> the "glowing face" look.
     */
    fun drawGlow(canvas: Canvas, bitmap: Bitmap, dest: RectF, colorFilter: ColorFilter?, glow: Float) {
        if (glow <= 0.01f || bitmap.isRecycled) return
        val sw = (dest.width() / 16f).toInt().coerceAtLeast(4)
        val sh = (dest.height() / 16f).toInt().coerceAtLeast(4)
        val small = Bitmap.createScaledBitmap(bitmap, sw, sh, true)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = (glow * 0.6f * 255f).toInt().coerceIn(0, 255)
            this.colorFilter = colorFilter
        }
        canvas.drawBitmap(small, null, dest, paint)
        if (small !== bitmap) small.recycle()
    }
}