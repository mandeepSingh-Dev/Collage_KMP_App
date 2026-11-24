package com.oem.collagekmpapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build

// androidMain — shared by every exporter's "write bitmap to MediaStore" step.

internal class ExportFormatSpec(
    val ext: String,
    val mime: String,
    val compress: Bitmap.CompressFormat,
    val quality: Int
)

/** "JPG"/"JPEG" (default), "PNG", "WEBP". */
internal fun exportFormatSpec(format: String): ExportFormatSpec = when {
    format.startsWith("PNG", true) -> ExportFormatSpec("png", "image/png", Bitmap.CompressFormat.PNG, 100)
    format.startsWith("WEBP", true) -> ExportFormatSpec(
        "webp", "image/webp",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
        else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP,
        95
    )
    else -> ExportFormatSpec("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG, 95)
}

/**
 * When Settings > "Add Watermark" is on, stamps a small, semi-transparent label in the bottom-right
 * corner. Draws directly on a mutable bitmap, otherwise on a copy (the original is left untouched).
 */
internal fun Bitmap.withExportWatermark(): Bitmap {
    if (!ExportOptions.watermark) return this
    val target = if (isMutable) this else copy(Bitmap.Config.ARGB_8888, true) ?: return this
    val canvas = Canvas(target)
    val w = target.width.toFloat()
    val textSize = (w * 0.032f).coerceAtLeast(14f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = textSize
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = 0xFFFFFFFF.toInt()
        alpha = 215
        setShadowLayer(textSize * 0.18f, 0f, textSize * 0.06f, 0x99000000.toInt())
        textAlign = Paint.Align.RIGHT
    }
    val margin = w * 0.03f
    canvas.drawText(ExportOptions.watermarkText, target.width - margin, target.height - margin, paint)
    return target
}