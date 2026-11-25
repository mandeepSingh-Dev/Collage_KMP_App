package com.oem.collagekmpapp.ui

import com.oem.collagekmpapp.BackgroundTexture
import com.oem.collagekmpapp.FreestyleRenderer
import com.oem.collagekmpapp.PerspectiveFill
import com.oem.collagekmpapp.PhotoColorEngine
import com.oem.collagekmpapp.PhotoLook
import com.oem.collagekmpapp.exportFormatSpec
import com.oem.collagekmpapp.withExportWatermark



import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.oem.collagekmpapp.ui.SinglePhotoExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

// androidMain
@Composable
actual fun rememberSinglePhotoExporter(): SinglePhotoExporter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context) {
        SinglePhotoExporter { edit, longSide, format, onResult ->
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { renderAndSave(context, edit, longSide, format) }
                }
                onResult(result)
            }
        }
    }
}

private fun renderAndSave(context: Context, edit: SinglePhotoEdit, longSide: Int, format: String): String {
    val sizes = listOf(longSide, 2048, 1440, 1080, 720).filter { it <= longSide }.distinct()
    var last: Throwable? = null
    for (target in sizes) {
        try {
            val bmp = render(context, edit, target)
            try {
                if (!saveBitmap(context, bmp, format)) throw IllegalStateException("MediaStore could not create the file.")
            } finally {
                bmp.recycle()
            }
            return "Pictures/CollageStudio"
        } catch (oom: OutOfMemoryError) {
            last = oom
            System.gc()
        }
    }
    throw IllegalStateException("Not enough memory to export (${last?.message ?: "out of memory"}). Try a lower quality.")
}

/** Mirrors spBoxSize() of the editor: size of the un-rotated cropped photo box inside a w x h canvas. */
private fun boxSize(cw: Float, ch: Float, a: Float, odd: Boolean, fit: Boolean, w: Float, h: Float): Pair<Float, Float> {
    val cr = cw * a / ch
    val eff = if (odd) 1f / cr else cr
    val ratio = w / h
    val useWidth = if (fit) eff > ratio else eff <= ratio
    val vw = if (useWidth) w else h * eff
    val vh = if (useWidth) w / eff else h
    return if (odd) Pair(vh, vw) else Pair(vw, vh)
}

private fun render(context: Context, edit: SinglePhotoEdit, target: Int): Bitmap {
    val r = edit.ratio.coerceIn(0.2f, 5f)
    val ew: Int
    val eh: Int
    if (r >= 1f) { ew = target; eh = (target / r).roundToInt().coerceAtLeast(16) }
    else { eh = target; ew = (target * r).roundToInt().coerceAtLeast(16) }
    val w = ew.toFloat()
    val h = eh.toFloat()

    val result = Bitmap.createBitmap(ew, eh, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)

    // ---- 1. backdrop (colour / gradient / blurred photo + texture)
    val bgBmp = edit.project.backgroundPhotoUri?.let { decodeUpright(context, Uri.parse(it), max(ew, eh)) }
    FreestyleRenderer.draw(
        canvas, edit.project.copy(items = emptyList(), drawStrokes = emptyList()),
        ew, eh, emptyMap(), bgBmp
    )
    bgBmp?.recycle()

    // ---- 2. the photo
    val cl = edit.cropL.coerceIn(0f, 0.95f)
    val ct = edit.cropT.coerceIn(0f, 0.95f)
    val cw = (edit.cropR - cl).coerceIn(0.05f, 1f)
    val chh = (edit.cropB - ct).coerceIn(0.05f, 1f)

    // decode large enough for the biggest on-canvas use
    val approxFull = max(w, h) / min(cw, chh)
    val gain = max(1f, edit.scaleCenter * max(edit.scaleH, edit.scaleV))
    val photo = decodeUpright(context, Uri.parse(edit.photoUri), (approxFull * gain).toInt().coerceIn(512, 4096))
        ?: throw IllegalStateException("Could not read the photo.")

    try {
        val a = photo.width.toFloat() / photo.height
        val odd = edit.quarterTurns % 2 != 0
        val (dw, dh) = boxSize(cw, chh, a, odd, edit.fit, w, h)
        val fullW = dw / cw
        val fullH = dh / chh
        val colorMatrix = PhotoColorEngine.buildMatrix(edit.filterPreset, edit.filterIntensity, edit.adjustValues, edit.tools)
        val fx = PhotoColorEngine.effects(edit.filterPreset, edit.filterIntensity, edit.adjustValues, edit.tools)
        val exportScale = (w / 1080f).coerceAtLeast(1f)

        fun drawPhotoOn(c: Canvas) {
            val cx = w / 2f
            val cy = h / 2f
            c.save()
            // same order as the preview: flip (outer) -> rotate -> H / V / centre scale
            c.scale(if (edit.flipH) -1f else 1f, if (edit.flipV) -1f else 1f, cx, cy)
            c.rotate(edit.fineAngle + edit.quarterTurns * 90f, cx, cy)
            c.scale(edit.scaleCenter * edit.scaleH, edit.scaleCenter * edit.scaleV, cx, cy)
            val box = RectF(cx - dw / 2f, cy - dh / 2f, cx + dw / 2f, cy + dh / 2f)
            c.clipRect(box)
            val left = box.left - cl * fullW
            val top = box.top - ct * fullH
            val dest = RectF(left, top, left + fullW, top + fullH)
            PhotoLook.drawPhoto(c, photo, dest, box, 1f, colorMatrix, fx, exportScale, edit.tools.lens, edit.tools.selective)
            PhotoLook.drawVignette(c, box, fx.vignette)
            c.restore()
        }

        // spotlight blur included: everything that makes up the edited photo, painted onto [target]
        fun paintPhotoStack(target: Canvas) {
            if (!edit.blurOn) {
                drawPhotoOn(target)
            } else {
                val layer = Bitmap.createBitmap(ew, eh, Bitmap.Config.ARGB_8888)
                drawPhotoOn(Canvas(layer))
                val blurred = gaussianBlur(layer, edit.blurAmount * 0.04f * w)
                val base = if (edit.blurInside) layer else blurred
                val top = if (edit.blurInside) blurred else layer

                // keep only the soft-edged disc of the top layer
                val inner = (1f - edit.blurFeather).coerceIn(0f, 0.97f)
                val mask = Paint().apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                    shader = RadialGradient(
                        edit.blurCx * w, edit.blurCy * h, (edit.blurR * w).coerceAtLeast(1f),
                        intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT),
                        floatArrayOf(0f, inner, 1f), Shader.TileMode.CLAMP
                    )
                }
                Canvas(top).drawRect(0f, 0f, w, h, mask)
                target.drawBitmap(base, 0f, 0f, null)
                target.drawBitmap(top, 0f, 0f, null)
                layer.recycle()
                blurred.recycle()
            }
        }

        // Perspective is the last geometric step: the whole photo layer is warped, the gaps get the chosen fill.
        val persp = edit.tools.perspective
        if (persp.isIdentity) {
            paintPhotoStack(canvas)
        } else {
            val layer = Bitmap.createBitmap(ew, eh, Bitmap.Config.ARGB_8888)
            paintPhotoStack(Canvas(layer))
            when (persp.fill) {
                PerspectiveFill.WHITE -> canvas.drawColor(Color.WHITE)
                PerspectiveFill.BLACK -> canvas.drawColor(Color.BLACK)
                PerspectiveFill.SMART -> drawSmartFill(canvas, photo, ew, eh)
            }
            val warp = Matrix()
            warp.setPolyToPoly(floatArrayOf(0f, 0f, w, 0f, w, h, 0f, h), 0, persp.quad(w, h), 0, 4)
            canvas.drawBitmap(layer, warp, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            layer.recycle()
        }
    } finally {
        photo.recycle()
    }

    // ---- 3. stickers, text and doodles on top (transparent backdrop = nothing is repainted)
    FreestyleRenderer.draw(
        canvas,
        edit.project.copy(
            backgroundColorLong = 0L,
            backgroundGradientColors = emptyList(),
            backgroundPhotoUri = null,
            backgroundTexture = BackgroundTexture.NONE
        ),
        ew, eh, emptyMap(), null
    )
    return result
}

// ------------------------------------------------------------------------------------ perspective fill

/** "Smart" fill: the photo itself, scaled to cover the canvas and heavily blurred (same look as the preview). */
private fun drawSmartFill(canvas: Canvas, photo: Bitmap, ew: Int, eh: Int) {
    val sw = max(16, ew / 4)
    val sh = max(16, eh / 4)
    val small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
    val c = Canvas(small)
    val k = max(sw.toFloat() / photo.width, sh.toFloat() / photo.height)
    val dw = photo.width * k
    val dh = photo.height * k
    c.drawBitmap(photo, null, RectF((sw - dw) / 2f, (sh - dh) / 2f, (sw + dw) / 2f, (sh + dh) / 2f), Paint(Paint.FILTER_BITMAP_FLAG))
    val blurred = gaussianBlur(small, 0.05f * sw)
    canvas.drawBitmap(blurred, null, RectF(0f, 0f, ew.toFloat(), eh.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
    blurred.recycle()
    small.recycle()
}

// ------------------------------------------------------------------------------------ blur

/** Gaussian-like blur (3 box passes on a downscaled copy, premultiplied, transparent outside the bitmap). */
private fun gaussianBlur(src: Bitmap, sigma: Float): Bitmap {
    if (sigma < 0.5f) return src.copy(Bitmap.Config.ARGB_8888, true)
    val scale = if (sigma > 3f) 3f / sigma else 1f
    val sw = max(2, (src.width * scale).roundToInt())
    val sh = max(2, (src.height * scale).roundToInt())
    val small = if (scale < 1f) Bitmap.createScaledBitmap(src, sw, sh, true) else src.copy(Bitmap.Config.ARGB_8888, true)
    val s = sigma * scale
    val radius = max(1, ((sqrt(4f * s * s + 1f) - 1f) / 2f).roundToInt())

    val n = sw * sh
    val px = IntArray(n)
    small.getPixels(px, 0, sw, 0, 0, sw, sh)
    if (small !== src) small.recycle()

    val out = IntArray(n)
    val chan = FloatArray(n)
    val tmp = FloatArray(n)
    val alpha = FloatArray(n)

    // channel 0 = alpha (kept for un-premultiply), 1..3 = premultiplied R, G, B
    for (c in 0..3) {
        for (i in 0 until n) {
            val p = px[i]
            val al = (p ushr 24) and 0xFF
            chan[i] = when (c) {
                0 -> al.toFloat()
                1 -> ((p shr 16) and 0xFF) * al / 255f
                2 -> ((p shr 8) and 0xFF) * al / 255f
                else -> (p and 0xFF) * al / 255f
            }
        }
        repeat(3) {
            boxPass(chan, tmp, sw, sh, radius, true)
            boxPass(tmp, chan, sw, sh, radius, false)
        }
        if (c == 0) {
            for (i in 0 until n) alpha[i] = chan[i]
            for (i in 0 until n) out[i] = chan[i].roundToInt().coerceIn(0, 255) shl 24
        } else {
            val shift = when (c) { 1 -> 16; 2 -> 8; else -> 0 }
            for (i in 0 until n) {
                val al = alpha[i]
                val v = if (al > 0.5f) (chan[i] * 255f / al).roundToInt().coerceIn(0, 255) else 0
                out[i] = out[i] or (v shl shift)
            }
        }
    }

    val blurredSmall = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
    blurredSmall.setPixels(out, 0, sw, 0, 0, sw, sh)
    if (scale >= 1f) return blurredSmall
    val big = Bitmap.createScaledBitmap(blurredSmall, src.width, src.height, true)
    if (big !== blurredSmall) blurredSmall.recycle()
    return big
}

private fun boxPass(src: FloatArray, dst: FloatArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
    val len = if (horizontal) w else h
    val lines = if (horizontal) h else w
    val stride = if (horizontal) 1 else w
    val inv = 1f / (2 * r + 1)
    for (line in 0 until lines) {
        val base = if (horizontal) line * w else line
        var sum = 0f
        for (i in 0..min(r, len - 1)) sum += src[base + i * stride]
        for (i in 0 until len) {
            dst[base + i * stride] = sum * inv
            val add = i + r + 1
            val sub = i - r
            if (add < len) sum += src[base + add * stride]
            if (sub >= 0) sum -= src[base + sub * stride]
        }
    }
}

// ------------------------------------------------------------------------------------ io

/** Decodes the photo upright (EXIF applied), sub-sampled so its longest side stays >= [need] when possible. */
private fun decodeUpright(context: Context, uri: Uri, need: Int): Bitmap? {
    val cr = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= need) sample *= 2

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

private fun saveBitmap(context: Context, rawBitmap: Bitmap, format: String): Boolean {
    val spec = exportFormatSpec(format)          // JPG / PNG / WEBP from Settings
    val bitmap = rawBitmap.withExportWatermark() // optional watermark from Settings
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "Edit_${System.currentTimeMillis()}.${spec.ext}")
        put(MediaStore.MediaColumns.MIME_TYPE, spec.mime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CollageStudio")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    resolver.openOutputStream(uri)?.use { out ->
        bitmap.compress(spec.compress, spec.quality, out)
        out.flush()
    } ?: return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
    return true
}