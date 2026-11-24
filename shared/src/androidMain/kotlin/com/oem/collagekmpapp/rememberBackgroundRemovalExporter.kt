package com.oem.collagekmpapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberBackgroundRemovalExporter(): BackgroundRemovalExporter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context) {
        BackgroundRemovalExporter { photoUri, strokes, onResult ->
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { exportCutout(context, photoUri, strokes) }
                }
                onResult(result)
            }
        }
    }
}

private const val MAX_SIDE = 3072

private fun exportCutout(context: Context, uriStr: String, strokes: List<BgMaskStroke>): String {
    val src = decodeUpright(context, Uri.parse(uriStr))
        ?: throw IllegalStateException("Could not read the image.")
    val w = src.width
    val h = src.height

    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    Canvas(result).drawBitmap(src, 0f, 0f, null)
    src.recycle()

    // Build the same alpha mask the preview shows: start opaque, then replay every stroke in order.
    val mask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val mc = Canvas(mask)
    mc.drawColor(Color.WHITE)
    val clear = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    val replace = PorterDuffXfermode(PorterDuff.Mode.SRC)

    strokes.forEach { s ->
        if (s.fill) {
            if (s.restore) mc.drawColor(Color.WHITE, PorterDuff.Mode.SRC)
            else mc.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            return@forEach
        }
        if (s.points.isEmpty()) return@forEach
        val widthPx = s.widthRatio * w
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            xfermode = if (s.restore) replace else clear
        }
        if (s.points.size == 1) {
            paint.style = Paint.Style.FILL
            mc.drawCircle(s.points[0].x * w, s.points[0].y * h, widthPx / 2f, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = widthPx
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            val path = Path().apply {
                moveTo(s.points[0].x * w, s.points[0].y * h)
                for (i in 1 until s.points.size) lineTo(s.points[i].x * w, s.points[i].y * h)
            }
            mc.drawPath(path, paint)
        }
    }

    Canvas(result).drawBitmap(mask, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) })
    mask.recycle()

    val saved = savePng(context, result)
    result.recycle()
    if (!saved) throw IllegalStateException("MediaStore could not create the file.")
    return "Pictures/CollageStudio"
}

private fun decodeUpright(context: Context, uri: Uri): Bitmap? {
    val cr = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2

    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val decoded = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

    val rotation = try {
        cr.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (_: Exception) {
        0
    }
    if (rotation == 0) return decoded

    val m = Matrix().apply { postRotate(rotation.toFloat()) }
    val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
    if (rotated != decoded) decoded.recycle()
    return rotated
}

private fun savePng(context: Context, bitmap: Bitmap): Boolean {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "RemoveBG_${System.currentTimeMillis()}.png")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CollageStudio")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    resolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
    return true
}
