package com.oem.collagekmpapp.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.oem.collagekmpapp.model.CollageProject
import com.oem.collagekmpapp.renderer.CollageRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CollageExporter {

    /**
     * Executes the offscreen export pipeline on Dispatchers.IO.
     * Decodes original full-resolution photos, renders via CollageRenderer at high resolution,
     * compresses to JPEG 95% (or PNG), saves to MediaStore, and handles OutOfMemoryError with automatic fallback.
     */
    suspend fun export(
        context: Context,
        project: CollageProject,
        longSideTarget: Int = 2048,
        format: String = "JPG"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        val candidateSizes = listOf(longSideTarget, 1440, 1080, 720)
        var lastError: Throwable? = null

        for (targetLong in candidateSizes) {
            try {
                val ratioW = project.aspectRatioWidth.coerceAtLeast(0.1f)
                val ratioH = project.aspectRatioHeight.coerceAtLeast(0.1f)

                val exportW: Int
                val exportH: Int
                if (ratioW >= ratioH) {
                    exportW = targetLong
                    exportH = (targetLong * ratioH / ratioW).toInt().coerceAtLeast(320)
                } else {
                    exportH = targetLong
                    exportW = (targetLong * ratioW / ratioH).toInt().coerceAtLeast(320)
                }

                // 1. Decode original photos with inSampleSize and EXIF orientation
                val loadedBitmaps = mutableMapOf<String, Bitmap>()
                val uniqueUris = (project.photos.map { it.uri } + project.overlays.mapNotNull {
                    (it as? com.oem.collagekmpapp.model.ProjectOverlayItem.Photo)?.uri
                }).distinct()

                uniqueUris.forEach { uriStr ->
                    decodeSampledBitmap(context, uriStr, exportW, exportH)?.let {
                        loadedBitmaps[uriStr] = it
                    }
                }

                val bgBitmap = project.backgroundPhotoUri?.let {
                    decodeSampledBitmap(context, it, exportW, exportH)
                }

                // 2. Allocate offscreen ARGB_8888 bitmap
                val offscreenBitmap = Bitmap.createBitmap(exportW, exportH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(offscreenBitmap)

                // 3. Render complete model via shared renderer
                CollageRenderer.draw(
                    canvas = canvas,
                    project = project,
                    targetWidth = exportW,
                    targetHeight = exportH,
                    photoBitmaps = loadedBitmaps,
                    backgroundBitmap = bgBitmap
                )

                // 4. Save to MediaStore
                val savedUri = saveBitmapToMediaStore(context, offscreenBitmap, format)

                // Cleanup bitmaps
                offscreenBitmap.recycle()
                loadedBitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
                bgBitmap?.let { if (!it.isRecycled) it.recycle() }

                if (savedUri != null) {
                    return@withContext Result.success(savedUri)
                } else {
                    throw IllegalStateException("MediaStore returned null URI for saved image.")
                }
            } catch (oom: OutOfMemoryError) {
                lastError = oom
                System.gc()
                continue
            } catch (e: Exception) {
                lastError = e
                break
            }
        }
        Result.failure(lastError ?: IllegalStateException("Export failed."))
    }

    /**
     * Renders a smaller preview Bitmap for display in ExportShareScreen using the exact same CollageRenderer.
     */
    suspend fun renderPreviewBitmap(
        context: Context,
        project: CollageProject,
        previewLongSide: Int = 800
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val ratioW = project.aspectRatioWidth.coerceAtLeast(0.1f)
            val ratioH = project.aspectRatioHeight.coerceAtLeast(0.1f)

            val w: Int
            val h: Int
            if (ratioW >= ratioH) {
                w = previewLongSide
                h = (previewLongSide * ratioH / ratioW).toInt().coerceAtLeast(200)
            } else {
                h = previewLongSide
                w = (previewLongSide * ratioW / ratioH).toInt().coerceAtLeast(200)
            }

            val loadedBitmaps = mutableMapOf<String, Bitmap>()
            val uniqueUris = (project.photos.map { it.uri } + project.overlays.mapNotNull {
                (it as? com.oem.collagekmpapp.model.ProjectOverlayItem.Photo)?.uri
            }).distinct()

            uniqueUris.forEach { uriStr ->
                decodeSampledBitmap(context, uriStr, w, h)?.let {
                    loadedBitmaps[uriStr] = it
                }
            }

            val bgBitmap = project.backgroundPhotoUri?.let {
                decodeSampledBitmap(context, it, w, h)
            }

            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            CollageRenderer.draw(
                canvas = canvas,
                project = project,
                targetWidth = w,
                targetHeight = h,
                photoBitmaps = loadedBitmaps,
                backgroundBitmap = bgBitmap
            )

            loadedBitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
            bgBitmap?.let { if (!it.isRecycled) it.recycle() }

            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampledBitmap(context: Context, uriStr: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        if (uriStr.isBlank() || uriStr == "default") return null
        val uri = Uri.parse(uriStr)
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            val decoded = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            } ?: return null

            val rotation = getExifRotation(context, uri)
            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                if (rotated != decoded) {
                    decoded.recycle()
                }
                rotated
            } else {
                decoded
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getExifRotation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap, format: String): Uri? {
        val isPng = format.equals("PNG", ignoreCase = true)
        val ext = if (isPng) "png" else "jpg"
        val mime = if (isPng) "image/png" else "image/jpeg"
        val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

        val contentResolver = context.contentResolver
        val filename = "Collage_${System.currentTimeMillis()}.$ext"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CollageStudio")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null
        contentResolver.openOutputStream(imageUri)?.use { output ->
            bitmap.compress(compressFormat, 95, output)
            output.flush()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(imageUri, contentValues, null, null)
        }

        return imageUri
    }
}
