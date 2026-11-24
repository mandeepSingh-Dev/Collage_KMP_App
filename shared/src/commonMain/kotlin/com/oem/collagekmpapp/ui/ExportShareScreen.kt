package com.oem.collagekmpapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.oem.collagekmpapp.AppSettings
import com.oem.collagekmpapp.UnifiedExportScreen
import com.oem.collagekmpapp.model.*
import com.oem.collagekmpapp.rememberImageSharer

data class QualityPreset(val title: String, val size: String)
data class ShareApp(val name: String, val color: Color, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportShareScreen(
    project: CollageProject? = null,
    layoutId: String = "2_grid",
    selectedPhotos: List<String>,
    slots: List<Slot> = emptyList(),
    spacingDp: Float = 8f,
    outerSpacingDp: Float = 12f,
    cornerRadiusDp: Float = 22f,
    filterPreset: String = "Original",
    filterIntensity: Float = 0f,
    backgroundPhoto: String? = null,
    backgroundFade: Float = 0f,
    backgroundColor: Color = Color.White,
    backgroundGradientColors: List<Color> = emptyList(),
    aspectRatioWidth: Float = 1f,
    aspectRatioHeight: Float = 1f,
    adjustValues: Map<String, Float> = emptyMap(),
    overlayItems: List<OverlayItem> = emptyList(),
    drawStrokes: List<DrawStroke> = emptyList(),
    collageText: String = "",
    textColor: Color = Color.White,
    selectedFont: String = "Modern",
    textOffset: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    onHome: () -> Unit,
    onBack: () -> Unit,
    onCreateAnother: () -> Unit
) {
    val sharer = rememberImageSharer()

    val resolvedProject = remember(
        project, layoutId, selectedPhotos, slots, spacingDp, outerSpacingDp, cornerRadiusDp,
        filterPreset, filterIntensity, backgroundPhoto, backgroundFade, backgroundColor,
        backgroundGradientColors, aspectRatioWidth, aspectRatioHeight, adjustValues,
        overlayItems, drawStrokes, collageText, textColor, selectedFont, textOffset
    ) {
        project ?: CollageProject(
            layoutId = layoutId,
            slots = slots,
            photos = selectedPhotos.map { PhotoTransform(uri = it) },
            aspectRatioWidth = aspectRatioWidth,
            aspectRatioHeight = aspectRatioHeight,
            outerSpacingRatio = outerSpacingDp / 320f,
            innerSpacingRatio = spacingDp / 320f,
            cornerRadiusRatio = cornerRadiusDp / 320f,
            backgroundColorLong = backgroundColor.toArgb().toLong(),
            backgroundGradientColors = backgroundGradientColors.map { it.toArgb().toLong() },
            backgroundPhotoUri = backgroundPhoto,
            backgroundFade = backgroundFade,
            filterPreset = filterPreset,
            filterIntensity = filterIntensity,
            adjustValues = adjustValues,
            overlays = overlayItems.map { item ->
                val nx = 0.5f + (item.offset.x / 320f)
                val ny = 0.5f + (item.offset.y / 320f)
                when (item) {
                    is OverlayItem.Emoji -> ProjectOverlayItem.Emoji(item.id, item.emoji, nx, ny, item.rotation, item.scale)
                    is OverlayItem.Photo -> ProjectOverlayItem.Photo(item.id, item.uri, nx, ny, item.rotation, item.scale)
                    is OverlayItem.Text -> ProjectOverlayItem.Emoji(item.id, item.text, nx, ny, item.rotation, item.scale)
                }
            },
            textLayers = if (collageText.isNotBlank()) listOf(
                ProjectTextLayer(
                    text = collageText,
                    x = 0.5f + (textOffset.x / 320f),
                    y = 0.5f + (textOffset.y / 320f),
                    fontName = selectedFont,
                    colorLong = textColor.toArgb().toLong()
                )
            ) else emptyList(),
            drawStrokes = drawStrokes.map { stroke ->
                NormalizedDrawStroke(
                    points = stroke.points.map { pt -> NormalizedPoint(pt.x / 320f, pt.y / 320f) },
                    colorLong = stroke.color.toArgb().toLong(),
                    strokeWidthRatio = stroke.strokeWidth / 320f,
                    isEraser = stroke.isEraser
                )
            }
        )
    }

    val previewCardWidthDp = 220.dp
    val previewCardHeightDp = previewCardWidthDp * (resolvedProject.aspectRatioHeight / resolvedProject.aspectRatioWidth)

    // The Export & Share UI now lives in the shared UnifiedExportScreen (same screen for every editor).
    UnifiedExportScreen(
        previewAspect = resolvedProject.aspectRatioWidth / resolvedProject.aspectRatioHeight,
        previewWidth = previewCardWidthDp,
        shareFallbackUri = selectedPhotos.firstOrNull() ?: "",
        onHome = onHome,
        onBack = onBack,
        onExport = { longSide, format, onResult ->
            sharer.saveProject(
                project = resolvedProject,
                quality = AppSettings.qualityLabelFor(longSide),
                format = format,
                onSuccess = { uri -> onResult(Result.success(uri)) },
                onError = { err -> onResult(Result.failure(err)) }
            )
        }
    ) {
        // Collage preview: the EXACT styled collage with all fields applied
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (resolvedProject.backgroundGradientColors.isNotEmpty()) androidx.compose.ui.graphics.Brush.linearGradient(resolvedProject.backgroundGradientColors.map { Color(it.toInt()) })
                    else androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(resolvedProject.backgroundColorLong.toInt()), Color(resolvedProject.backgroundColorLong.toInt())))
                )
                .padding((resolvedProject.outerSpacingRatio * previewCardWidthDp.value).dp)
        ) {
            if (resolvedProject.backgroundPhotoUri != null) {
                AsyncImage(
                    model = resolvedProject.backgroundPhotoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                )
            }
            if (resolvedProject.backgroundFade > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = (resolvedProject.backgroundFade / 100f) * 0.7f))
                )
            }

            RenderCollageLayout(
                layoutId = resolvedProject.layoutId,
                photos = selectedPhotos,
                spacing = (resolvedProject.innerSpacingRatio * previewCardWidthDp.value).dp,
                cornerRadius = (resolvedProject.cornerRadiusRatio * previewCardWidthDp.value).dp,
                filterPreset = resolvedProject.filterPreset,
                filterIntensity = resolvedProject.filterIntensity,
                glitterCard = "Glitz",
                glitterDensity = 0f,
                isDrawingMode = false,
                slots = resolvedProject.slots,
                photoTransforms = resolvedProject.photos.mapIndexed { idx, p -> idx to p }.toMap(),
                modifier = Modifier.fillMaxSize()
            )

            // Overlay Items (stickers, emojis, photo stickers)
            resolvedProject.overlays.forEach { item ->
                val itemOffsetX = (item.x - 0.5f) * previewCardWidthDp.value
                val itemOffsetY = (item.y - 0.5f) * previewCardHeightDp.value

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = itemOffsetX.dp, y = itemOffsetY.dp)
                        .graphicsLayer(rotationZ = item.rotation)
                ) {
                    when (item) {
                        is ProjectOverlayItem.Emoji -> {
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = item.emoji,
                                    fontSize = (item.scale * 0.11f * previewCardWidthDp.value).sp
                                )
                            }
                        }
                        is ProjectOverlayItem.Photo -> {
                            val sizeDp = (item.scale * 0.25f * previewCardWidthDp.value).dp
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            ) {
                                AsyncImage(
                                    model = item.uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(sizeDp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }

            // Freehand Drawn Strokes
            if (resolvedProject.drawStrokes.isNotEmpty()) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val cw = size.width
                    val ch = size.height
                    resolvedProject.drawStrokes.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                val first = stroke.points.first()
                                moveTo(first.x * cw, first.y * ch)
                                for (i in 1 until stroke.points.size) {
                                    lineTo(stroke.points[i].x * cw, stroke.points[i].y * ch)
                                }
                            }
                            drawPath(
                                path = path,
                                color = if (stroke.isEraser) Color.White else Color(stroke.colorLong.toInt()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = stroke.strokeWidthRatio * cw,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }

            resolvedProject.textLayers.forEach { textLayer ->
                val textOffsetX = (textLayer.x - 0.5f) * previewCardWidthDp.value
                val textOffsetY = (textLayer.y - 0.5f) * previewCardHeightDp.value

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = textOffsetX.dp, y = textOffsetY.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = textLayer.text,
                        color = Color(textLayer.colorLong.toInt()),
                        fontSize = (textLayer.fontSizeRatio * previewCardWidthDp.value).sp,
                        fontWeight = if (textLayer.fontName == "Bold" || textLayer.fontName == "Neon") FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}