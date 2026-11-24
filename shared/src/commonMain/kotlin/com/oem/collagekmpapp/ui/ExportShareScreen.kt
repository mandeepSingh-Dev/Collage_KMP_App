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
    var exportQuality by remember { mutableStateOf("High (1080p)") }
    var exportFormat by remember { mutableStateOf("JPG") }
    var copiedToast by remember { mutableStateOf(false) }
    var isSavedToGallery by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var exportedUri by remember { mutableStateOf<String?>(null) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export & Share", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF0F172A)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onHome) {
                        Text("Home", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), fontSize = 15.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success Banner (Only visible when Saved to Gallery is clicked)
            if (isSavedToGallery) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF22C55E),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Saved to Gallery!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            val previewCardWidthDp = 220.dp
            val previewCardHeightDp = previewCardWidthDp * (resolvedProject.aspectRatioHeight / resolvedProject.aspectRatioWidth)

            // Collage Preview Card rendering the EXACT styled collage with all fields applied
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.width(previewCardWidthDp).aspectRatio(aspectRatioWidth / aspectRatioHeight)
            ) {
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

            Spacer(modifier = Modifier.height(20.dp))

            // Export Settings Card
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Export Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    // Quality Presets
                    Text(
                        text = "Quality Presets",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val qualities = listOf(
                            QualityPreset("Standard (720p)", "1.2 MB"),
                            QualityPreset("High (1080p)", "2.8 MB"),
                            QualityPreset("Ultra HD (4K)", "8.5 MB")
                        )
                        qualities.forEach { preset ->
                            val isSelected = exportQuality == preset.title
                            OutlinedCard(
                                onClick = { exportQuality = preset.title },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1)
                                ),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White
                                ),
                                modifier = Modifier.weight(1f).height(64.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = preset.title.substringBefore(" "),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = preset.size,
                                        fontSize = 10.sp,
                                        color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    // Format Toggle
                    Text(
                        text = "Format Toggle",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp)
                            .fillMaxWidth()
                    ) {
                        listOf("JPG", "PNG").forEach { fmt ->
                            val isSelected = exportFormat == fmt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent)
                                    .clickable { exportFormat = fmt },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = fmt,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Fast Share
                    Text(
                        text = "Fast Share",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                    val shareApps = listOf(
                        ShareApp("Instagram", Color(0xFFE1306C), Icons.Default.CameraAlt),
                        ShareApp("WhatsApp", Color(0xFF22C55E), Icons.AutoMirrored.Filled.Chat),
                        ShareApp("Facebook", Color(0xFF2563EB), Icons.Default.Share),
                        ShareApp("Stories", Color(0xFFD946EF), Icons.Default.AutoAwesome),
                        ShareApp("Snapchat", Color(0xFFEAB308), Icons.Default.Star),
                        ShareApp("TikTok", Color(0xFF0F172A), Icons.Default.MusicNote),
                        ShareApp("More", Color(0xFF64748B), Icons.Default.MoreHoriz)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    ) {
                        items(shareApps) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        sharer.shareToApp(app.name, exportedUri ?: selectedPhotos.firstOrNull() ?: "")
                                    }
                                    .padding(4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = app.color.copy(alpha = 0.12f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(imageVector = app.icon, contentDescription = app.name, tint = app.color, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.name,
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Copy Link Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Copy Link  collage_studio_2026_11...",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1
                        )
                        TextButton(onClick = { copiedToast = true }) {
                            Text("Copy", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), fontSize = 13.sp)
                        }
                    }

                    if (copiedToast) {
                        Text("Link copied to clipboard!", fontSize = 11.sp, color = Color(0xFF16A34A))
                    }

                    if (exportError != null) {
                        Text(
                            text = exportError ?: "Export failed",
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Save to Gallery Button
                    Button(
                        onClick = {
                            isExporting = true
                            exportError = null
                            sharer.saveProject(
                                project = resolvedProject,
                                quality = exportQuality,
                                format = exportFormat,
                                onSuccess = { uri ->
                                    isExporting = false
                                    isSavedToGallery = true
                                    exportedUri = uri
                                },
                                onError = { err ->
                                    isExporting = false
                                    exportError = err.message ?: "Export failed"
                                }
                            )
                        },
                        enabled = !isExporting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (isExporting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Exporting High-Res Image...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else {
                            Text("Save to Gallery", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
