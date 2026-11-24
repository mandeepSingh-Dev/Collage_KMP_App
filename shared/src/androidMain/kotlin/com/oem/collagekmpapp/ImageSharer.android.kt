package com.oem.collagekmpapp

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.oem.collagekmpapp.model.Slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual class ImageSharer(private val context: android.content.Context) {
    actual fun saveToGallery(
        layoutId: String,
        photos: List<String>,
        slots: List<Slot>,
        spacingDp: Float,
        cornerRadiusDp: Float,
        filterPreset: String,
        filterIntensity: Float,
        backgroundPhoto: String?,
        backgroundFade: Float,
        outerSpacingDp: Float,
        aspectRatioWidth: Float,
        aspectRatioHeight: Float,
        backgroundColor: androidx.compose.ui.graphics.Color,
        backgroundGradientColors: List<androidx.compose.ui.graphics.Color>,
        adjustValues: Map<String, Float>,
        overlayItems: List<com.oem.collagekmpapp.ui.OverlayItem>,
        drawStrokes: List<com.oem.collagekmpapp.ui.DrawStroke>,
        collageText: String,
        textColor: androidx.compose.ui.graphics.Color,
        selectedFont: String,
        textOffset: androidx.compose.ui.geometry.Offset
    ) {
        try {
            val repository = com.oem.collagekmpapp.data.LayoutRepository()
            val safePhotos = if (photos.isEmpty()) listOf("default") else photos
            val count = safePhotos.size
            val layout = repository.getAllLayouts().find { it.id == layoutId }
                ?: repository.getLayouts(count).firstOrNull()
                ?: com.oem.collagekmpapp.model.CollageLayout(layoutId, "Default", count, "", listOf(Slot(0f, 0f, 1f, 1f)))
            val activeSlots = if (slots.isNotEmpty()) slots else layout.slots

            val bitmapWidth = 1080
            val ratioWidth = aspectRatioWidth.coerceAtLeast(0.1f)
            val ratioHeight = aspectRatioHeight.coerceAtLeast(0.1f)
            val bitmapHeight = (bitmapWidth * ratioHeight / ratioWidth).toInt().coerceIn(608, 4320)
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val bgColorInt = backgroundColor.value.toLong().toInt()
            canvas.drawColor(bgColorInt)
            if (backgroundGradientColors.isNotEmpty()) {
                val colors = backgroundGradientColors.map { it.value.toLong().toInt() }.toIntArray()
                val positions = colors.indices.map { i -> i.toFloat() / (colors.size - 1).coerceAtLeast(1) }.toFloatArray()
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = android.graphics.LinearGradient(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat(), colors, positions, android.graphics.Shader.TileMode.CLAMP)
                }
                canvas.drawRect(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat(), paint)
            }

            if (!backgroundPhoto.isNullOrEmpty()) {
                loadBitmapFromUri(context, backgroundPhoto)?.let { bgBitmap ->
                    canvas.drawBitmap(bgBitmap, null, RectF(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat()), Paint(Paint.ANTI_ALIAS_FLAG))
                }
            }
            if (backgroundFade > 0f) {
                val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    alpha = ((backgroundFade / 100f) * 0.7f * 255).toInt().coerceIn(0, 255)
                }
                canvas.drawRect(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat(), fadePaint)
            }

            val unitScale = bitmapWidth / 360f
            val outer = (outerSpacingDp * unitScale).coerceAtMost(bitmapWidth * 0.2f)
            val content = RectF(outer, outer, bitmapWidth - outer, bitmapHeight - outer)
            val pad = spacingDp * unitScale / 2f
            val radius = cornerRadiusDp * unitScale
            val photoPaint = PhotoLook.photoPaint(filterPreset, filterIntensity, adjustValues)
            val vignette = PhotoColorEngine.vignetteStrength(filterPreset, filterIntensity)
            activeSlots.forEachIndexed { index, slot ->
                val uri = safePhotos.getOrElse(index) { safePhotos.first() }
                val rect = RectF(
                    content.left + slot.left * content.width() + pad,
                    content.top + slot.top * content.height() + pad,
                    content.left + slot.right * content.width() - pad,
                    content.top + slot.bottom * content.height() - pad
                )
                if (rect.width() <= 1f || rect.height() <= 1f) return@forEachIndexed
                val clipPath = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
                canvas.save()
                canvas.clipPath(clipPath)
                val photo = loadBitmapFromUri(context, uri)
                if (photo != null) {
                    val scale = maxOf(rect.width() / photo.width, rect.height() / photo.height)
                    val w = photo.width * scale
                    val h = photo.height * scale
                    val destination = RectF(rect.centerX() - w / 2f, rect.centerY() - h / 2f, rect.centerX() + w / 2f, rect.centerY() + h / 2f)
                    canvas.drawBitmap(photo, null, destination, photoPaint)
                } else {
                    canvas.drawColor(Color.LTGRAY)
                }
                PhotoLook.drawVignette(canvas, rect, vignette)
                canvas.restore()
            }

            val coordinateScale = unitScale
            drawStrokes.forEach { stroke ->
                if (stroke.points.size > 1 && !stroke.isEraser) {
                    val path = Path().apply {
                        moveTo(outer + stroke.points.first().x * coordinateScale, outer + stroke.points.first().y * coordinateScale)
                        stroke.points.drop(1).forEach { lineTo(outer + it.x * coordinateScale, outer + it.y * coordinateScale) }
                    }
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = stroke.color.value.toLong().toInt()
                        alpha = 255
                        style = Paint.Style.STROKE
                        strokeWidth = stroke.strokeWidth * coordinateScale
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                    canvas.drawPath(path, paint)
                }
            }

            overlayItems.forEach { item ->
                canvas.save()
                canvas.translate(bitmapWidth / 2f + item.offset.x * coordinateScale, bitmapHeight / 2f + item.offset.y * coordinateScale)
                canvas.rotate(item.rotation)
                canvas.scale(item.scale, item.scale)
                when (item) {
                    is com.oem.collagekmpapp.ui.OverlayItem.Emoji -> {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.WHITE
                            textSize = 48f * coordinateScale
                            textAlign = Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT
                        }
                        canvas.drawText(item.emoji, 0f, paint.textSize, paint)
                    }
                    is com.oem.collagekmpapp.ui.OverlayItem.Photo -> {
                        loadBitmapFromUri(context, item.uri)?.let { sticker ->
                            val size = 72f * coordinateScale
                            canvas.drawBitmap(sticker, null, RectF(-size / 2f, -size / 2f, size / 2f, size / 2f), Paint(Paint.ANTI_ALIAS_FLAG))
                        }
                    }
                    is com.oem.collagekmpapp.ui.OverlayItem.Text -> {
                        val fontStyle = when (item.font) {
                            "Script" -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD_ITALIC)
                            "Bold", "Neon" -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                            "Retro" -> android.graphics.Typeface.MONOSPACE
                            "Serif" -> android.graphics.Typeface.SERIF
                            else -> android.graphics.Typeface.DEFAULT
                        }
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = item.textColor.value.toLong().toInt()
                            textSize = 32f * coordinateScale
                            textAlign = Paint.Align.CENTER
                            typeface = fontStyle
                            if (item.font == "Neon") setShadowLayer(16f * coordinateScale, 0f, 0f, color)
                        }
                        canvas.drawText(item.text, 0f, paint.textSize, paint)
                    }
                }
                canvas.restore()
            }

            if (collageText.isNotBlank()) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = textColor.value.toLong().toInt()
                    textSize = when (selectedFont) { "Bold" -> 32f; "Retro" -> 26f; "Neon" -> 30f; else -> 28f } * coordinateScale
                    textAlign = Paint.Align.CENTER
                    typeface = when (selectedFont) {
                        "Retro" -> android.graphics.Typeface.MONOSPACE
                        "Serif" -> android.graphics.Typeface.SERIF
                        "Bold", "Neon" -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        else -> android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
                    }
                    if (selectedFont == "Neon") setShadowLayer(16f * coordinateScale, 0f, 0f, color)
                }
                val x = bitmapWidth / 2f + textOffset.x * coordinateScale
                val y = bitmapHeight / 2f + textOffset.y * coordinateScale
                val textBounds = android.graphics.Rect()
                paint.getTextBounds(collageText, 0, collageText.length, textBounds)
                val horizontalPadding = 16f * coordinateScale
                val verticalPadding = 8f * coordinateScale
                val labelBounds = RectF(
                    x - textBounds.width() / 2f - horizontalPadding,
                    y + textBounds.top - verticalPadding,
                    x + textBounds.width() / 2f + horizontalPadding,
                    y + textBounds.bottom + verticalPadding
                )
                canvas.drawRoundRect(labelBounds, 4f * coordinateScale, 4f * coordinateScale, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    alpha = (0.45f * 255).toInt()
                })
                canvas.drawText(collageText, x, y, paint)
            }

            // 3. Write JPEG file to MediaStore

            // 3. Write JPEG file to MediaStore
            val contentResolver = context.contentResolver
            val filename = "Collage_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CollageStudio")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                contentResolver.openOutputStream(imageUri)?.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
                    output.flush()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(imageUri, contentValues, null, null)
                }

                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(imageUri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(viewIntent, "Open with").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun saveProject(
        project: com.oem.collagekmpapp.model.CollageProject,
        quality: String,
        format: String,
        onSuccess: (uri: String) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val longSide = when {
            quality.contains("4K", ignoreCase = true) || quality.contains("Ultra", ignoreCase = true) -> 3840
            quality.contains("2K", ignoreCase = true) -> 2048
            else -> 1080
        }
        CoroutineScope(Dispatchers.Main).launch {
            val result = com.oem.collagekmpapp.export.CollageExporter.export(context, project, longSide, format)
            result.onSuccess { uri ->
                onSuccess(uri.toString())
            }.onFailure { err ->
                onError(err)
            }
        }
    }

    actual fun shareToApp(appName: String, imageUrl: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            if (imageUrl.isNotEmpty()) {
                putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUrl))
            }
            putExtra(Intent.EXTRA_TEXT, "Check out my custom collage created with Collage Studio!")
            when (appName.lowercase()) {
                "instagram" -> setPackage("com.instagram.android")
                "whatsapp" -> setPackage("com.whatsapp")
                "facebook" -> setPackage("com.facebook.katana")
                "stories" -> setPackage("com.instagram.android")
                "snapchat" -> setPackage("com.snapchat.android")
                "tiktok" -> setPackage("com.zhiliaoapp.musically")
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val chooser = Intent.createChooser(intent, "Share Collage via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }
}

private fun loadBitmapFromUri(context: android.content.Context, uriStr: String): Bitmap? {
    return try {
        if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
            context.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
                BitmapFactory.decodeStream(it)
            }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
actual fun rememberImageSharer(): ImageSharer {
    val context = LocalContext.current
    return remember { ImageSharer(context) }
}