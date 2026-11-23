package com.oem.collagekmpapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.oem.collagekmpapp.model.FilterRepository
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.oem.collagekmpapp.components.FluidSlider
import com.oem.collagekmpapp.rememberGalleryPicker
import com.oem.collagekmpapp.model.*

enum class EditorTool {
    RATIO, LAYOUT, BORDER, FILTER, STICKERS, ADJUST, GLITTER, TEXT, BACKGROUND, DRAW
}

data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean
)

sealed class OverlayItem {
    abstract val id: String
    abstract val offset: Offset
    abstract val rotation: Float
    abstract val scale: Float

    data class Emoji(
        override val id: String,
        val emoji: String,
        override val offset: Offset = Offset.Zero,
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : OverlayItem()

    data class Photo(
        override val id: String,
        val uri: String,
        override val offset: Offset = Offset.Zero,
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : OverlayItem()
}

data class AdjustOption(
    val name: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageEditorScreen(
    layoutId: String,
    selectedPhotos: List<String>,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(EditorTool.LAYOUT) }
    var currentLayoutId by remember { mutableStateOf(layoutId) }

    val layoutRepo = remember { com.oem.collagekmpapp.data.LayoutRepository() }
    val currentLayout = remember(currentLayoutId, selectedPhotos.size) {
        layoutRepo.getAllLayouts().find { it.id == currentLayoutId }
            ?: layoutRepo.getLayouts(selectedPhotos.size.coerceAtLeast(1)).firstOrNull()
            ?: com.oem.collagekmpapp.model.CollageLayout(currentLayoutId, "Default", selectedPhotos.size, "", listOf(com.oem.collagekmpapp.model.Slot(0f, 0f, 1f, 1f)))
    }

    var currentSlots by remember(currentLayoutId) { mutableStateOf(currentLayout.slots) }
    var selectedSlotIndex by remember(currentLayoutId) { mutableStateOf<Int?>(null) }
    var aspectRatio by remember { mutableStateOf(AspectRatio.SQUARE) }
    var borderSettings by remember { mutableStateOf(BorderSettings(outerSpacing = 12f, cornerRadius = 22f, spacing = 8f)) }
    var photoTransforms by remember { mutableStateOf<Map<Int, PhotoTransform>>(emptyMap()) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }

    // Filter & Glitter states
    var filterIntensity by remember { mutableStateOf(0f) }
    var selectedPreset by remember { mutableStateOf("Original") }
    var glitterDensity by remember { mutableStateOf(0f) }
    var selectedGlitterCard by remember { mutableStateOf("Glitz") }

    var selectedStickerCategory by remember { mutableStateOf("All") }
    val stickerCategories = listOf("All", "Emoji", "Love", "Party", "Travel")
    val stickerMap = remember {
        mapOf(
            "Emoji" to listOf("😍", "😎", "🔥", "✨", "🥳", "👑", "🌈", "⭐", "🦋", "💯", "🚀", "💎"),
            "Love" to listOf("🥰", "❤️", "💖", "💕", "💘", "💋", "😍", "💌", "💍", "🌹", "🫶", "✨"),
            "Party" to listOf("🥳", "🥂", "🎂", "🎈", "🎉", "🎊", "🍾", "🎁", "💃", "🎶", "🍸", "🍕"),
            "Travel" to listOf("✈️", "🌴", "🗺️", "🗽", "🗼", "🏕️", "🚗", "🏖️", "🎒", "🚂", "🚢", "🏔️")
        )
    }

    // Color Grading / Adjust states with -50 to 50 range and neutral at 0
    var selectedAdjustItem by remember { mutableStateOf("Brightness") }
    var adjustValues by remember { mutableStateOf(mapOf(
        "Brightness" to 0f,
        "Contrast" to 0f,
        "Highlight" to 0f,
        "Shadow" to 0f,
        "Saturation" to 0f,
        "Warmth" to 0f,
        "Tint" to 0f,
        "Fade" to 0f
    )) }

    // Text typography states with custom color list
    var selectedFont by remember { mutableStateOf("Script") }
    var collageText by remember { mutableStateOf("") }
    var textColor by remember { mutableStateOf(Color.White) }
    var textOffset by remember { mutableStateOf(Offset.Zero) }
    var showAddTextDialog by remember { mutableStateOf(false) }

    // Background settings states matching 09_background_settings_screen.svg
    var canvasBgColor by remember { mutableStateOf<Color>(Color.White) }
    var canvasBgGradient by remember { mutableStateOf<Brush?>(null) }
    var canvasBgGradientColors by remember { mutableStateOf(emptyList<Color>()) }
    var backgroundFade by remember { mutableStateOf(0f) }
    var selectedBackgroundPhoto by remember { mutableStateOf<String?>(null) }

    // Draw & Brush states matching 10_draw_brush_screen.svg
    var selectedBrushType by remember { mutableStateOf("Pen") }
    var brushDensity by remember { mutableStateOf(20f) }
    var brushOpacity by remember { mutableStateOf(100f) }
    var brushColor by remember { mutableStateOf(Color(0xFFF43F8E)) }

    // Live drawing strokes list
    var drawStrokes by remember { mutableStateOf(listOf<DrawStroke>()) }
    var currentStrokePoints by remember { mutableStateOf(listOf<Offset>()) }

    // Stickers overlay items list (emojis and gallery photo stickers)
    var overlayItems by remember { mutableStateOf(listOf<OverlayItem>()) }
    var selectedOverlayId by remember { mutableStateOf<String?>(null) }

    val photoStickerPicker = rememberGalleryPicker { selectedUris ->
        if (selectedUris.isNotEmpty()) {
            val newItem = OverlayItem.Photo(
                id = "photo_${kotlin.random.Random.nextLong()}",
                uri = selectedUris[0],
                offset = Offset.Zero,
                rotation = 0f,
                scale = 1f
            )
            overlayItems = overlayItems + newItem
            selectedOverlayId = newItem.id
        }
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportQuality by remember { mutableStateOf("HD 1080p") }
    var exportFormat by remember { mutableStateOf("JPG") }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    val count = selectedPhotos.size.coerceAtLeast(2)

    val layouts = remember(count) {
        com.oem.collagekmpapp.data.LayoutRepository().getLayouts(count).map {
            LayoutOption(it.id, it.name, it.slotCount, it.description)
        }
    }

    val filterPresets = FilterRepository.presets
    val glitterCards = listOf("Glitz", "Spark 1", "Flare 2", "Dust", "Diamond Glow", "Heart Sparkle", "Golden Shimmer")
    val fontCards = listOf("Modern", "Script", "Bold", "Retro", "Serif", "Neon")

    val adjustOptions = listOf(
        AdjustOption("Brightness", Icons.Default.WbSunny),
        AdjustOption("Contrast", Icons.Default.Contrast),
        AdjustOption("Highlight", Icons.Default.LightMode),
        AdjustOption("Shadow", Icons.Default.Brightness2),
        AdjustOption("Saturation", Icons.Default.Palette),
        AdjustOption("Warmth", Icons.Default.WbIncandescent),
        AdjustOption("Tint", Icons.Default.Gradient),
        AdjustOption("Fade", Icons.Default.BlurOn)
    )

    val emojis = listOf("😍", "😎", "🔥", "✨", "🥳", "👑", "🌈", "⭐", "🦋")

    val sharedColors = listOf(
        Color(0xFFFFFFFF), Color(0xFF0F172A), Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFACC15), Color(0xFF22C55E), Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF9333EA), Color(0xFFEC4899),
        Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B),
        Color(0xFFF87171), Color(0xFFDC2626), Color(0xFFB91C1C), Color(0xFF991B1B),
        Color(0xFFFB923C), Color(0xFFEA580C), Color(0xFFC2410C), Color(0xFF9A3412),
        Color(0xFFFDE047), Color(0xFFEAB308), Color(0xFFCA8A04), Color(0xFFA16207),
        Color(0xFF4ADE80), Color(0xFF16A34A), Color(0xFF15803D), Color(0xFF166534),
        Color(0xFF22D3EE), Color(0xFF0891B2), Color(0xFF0E7490), Color(0xFF155E75),
        Color(0xFF60A5FA), Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFF1E40AF),
        Color(0xFFA855F7), Color(0xFF7E22CE), Color(0xFFF43F5E), Color(0xFFBE185D)
    )

    val solidColors = sharedColors
    val brushColors = sharedColors
    val textColors = sharedColors

    val gradients = listOf(
        Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFFACC15), Color(0xFF22C55E), Color(0xFF3B82F6))),
        Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFFBBF24))),
        Brush.linearGradient(listOf(Color(0xFFFB923C), Color(0xFFF472B6), Color(0xFF38BDF8))),
        Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF2563EB))),
        Brush.linearGradient(listOf(Color(0xFFF5D0FE), Color(0xFF93C5FD))),
        Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF7DD3FC))),
        Brush.linearGradient(listOf(Color(0xFFFF007F), Color(0xFFFF758C), Color(0xFFFF7EB3))),
        Brush.linearGradient(listOf(Color(0xFF651FFF), Color(0xFF00E5FF))),
        Brush.linearGradient(listOf(Color(0xFFFF9100), Color(0xFFFFEA00))),
        Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF1DE9B6))),
        Brush.linearGradient(listOf(Color(0xFFF50057), Color(0xFFFF4081))),
        Brush.linearGradient(listOf(Color(0xFF3D5AFE), Color(0xFF2F80ED))),
        Brush.linearGradient(listOf(Color(0xFFFF3D00), Color(0xFFFF9100))),
        Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF3949AB))),
        Brush.linearGradient(listOf(Color(0xFF004D40), Color(0xFF00897B))),
        Brush.linearGradient(listOf(Color(0xFFB71C1C), Color(0xFFD32F2F))),
        Brush.linearGradient(listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))),
        Brush.linearGradient(listOf(Color(0xFF880E4F), Color(0xFFC2185B))),
        Brush.linearGradient(listOf(Color(0xFF0D47A1), Color(0xFF1976D2))),
        Brush.linearGradient(listOf(Color(0xFF006064), Color(0xFF00ACC1))),
        Brush.linearGradient(listOf(Color(0xFF33691E), Color(0xFF689F38))),
        Brush.linearGradient(listOf(Color(0xFFE65100), Color(0xFFF57C00))),
        Brush.linearGradient(listOf(Color(0xFF3E2723), Color(0xFF5D4037))),
        Brush.linearGradient(listOf(Color(0xFF263238), Color(0xFF455A64))),
        Brush.linearGradient(listOf(Color(0xFFFF8A80), Color(0xFFFF80AB)))
    )

    val brushTypes = listOf("Pen", "Eraser")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collage Studio Editor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Save")
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color.White)) {
                // Dynamic Tool Sub-Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    when (selectedTab) {
                        EditorTool.RATIO -> {
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Canvas Aspect Ratio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(AspectRatio.entries) { ratio ->
                                        val isSelected = aspectRatio == ratio
                                        Box(
                                            modifier = Modifier
                                                .width(76.dp)
                                                .height(56.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { aspectRatio = ratio },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = ratio.displayName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E293B)
                                                )
                                                if (ratio.badge.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = ratio.badge,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = when(ratio.badge) {
                                                            "Instagram" -> Color(0xFFE1306C)
                                                            "Pinterest" -> Color(0xFFE60023)
                                                            "YouTube" -> Color(0xFFFF0000)
                                                            else -> Color(0xFF64748B)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        EditorTool.LAYOUT -> {
                            // Layout tab showing visual layout grid thumbnail boxes matching FunPic app screenshot
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Select Layout Grid", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(160.dp)
                                ) {
                                    gridItems(layouts) { layout ->
                                        val isSelected = currentLayoutId == layout.id
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .shadow(3.dp, RoundedCornerShape(10.dp))
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFFFFFFF))
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { currentLayoutId = layout.id },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // Miniature Vector Grid Thumbnail Preview matching FunPic style
                                            LayoutThumbnailPreview(layoutId = layout.id, isSelected = isSelected)
                                        }
                                    }
                                }
                            }
                        }
                        EditorTool.BORDER -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text("Border & Spacing", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Outer Spacing: ${borderSettings.outerSpacing.toInt()}px", fontSize = 11.sp, color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FluidSlider(
                                        value = borderSettings.outerSpacing,
                                        onValueChange = { borderSettings = borderSettings.copy(outerSpacing = it) },
                                        valueRange = 0f..30f,
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    )
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Inner Spacing: ${borderSettings.spacing.toInt()}px", fontSize = 11.sp, color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FluidSlider(
                                        value = borderSettings.spacing,
                                        onValueChange = { borderSettings = borderSettings.copy(spacing = it) },
                                        valueRange = 0f..20f,
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    )
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Corner Radius: ${borderSettings.cornerRadius.toInt()}px", fontSize = 11.sp, color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FluidSlider(
                                        value = borderSettings.cornerRadius,
                                        onValueChange = { borderSettings = borderSettings.copy(cornerRadius = it) },
                                        valueRange = 0f..40f,
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    )
                                }
                            }
                        }
                        EditorTool.FILTER -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .animateContentSize()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Filters", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                AnimatedVisibility(visible = selectedPreset != "Original") {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("Intensity: ${filterIntensity.toInt()}%", fontSize = 12.sp, color = Color(0xFF334155))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        FluidSlider(
                                            value = filterIntensity,
                                            onValueChange = { filterIntensity = it },
                                            valueRange = 0f..100f,
                                            modifier = Modifier.fillMaxWidth().height(36.dp)
                                        )
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(filterPresets) { preset ->
                                        val isSelected = selectedPreset == preset
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable {
                                                selectedPreset = preset
                                                filterIntensity = if (preset == "Original") 0f else 50f
                                            }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(getFilterTintColor(preset))
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 1.dp,
                                                        color = if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (preset == "Original") {
                                                    Text("Orig", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = preset,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        EditorTool.STICKERS -> {
                            val currentStickers = if (selectedStickerCategory == "All") {
                                stickerMap.values.flatten().distinct()
                            } else {
                                stickerMap[selectedStickerCategory] ?: emptyList()
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .animateContentSize()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Stickers", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    stickerCategories.forEach { category ->
                                        val isSelected = selectedStickerCategory == category
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { selectedStickerCategory = category }
                                        ) {
                                            Text(
                                                text = category,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(16.dp)
                                                        .height(2.5.dp)
                                                        .background(Color(0xFF2563EB), RoundedCornerShape(1.5.dp))
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(2.5.dp))
                                            }
                                        }
                                    }
                                }

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(140.dp)
                                ) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFEFF6FF))
                                                .border(1.5.dp, Color(0xFF2563EB), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    photoStickerPicker.launch()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                                Text("Add Photo", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                            }
                                        }
                                    }

                                    gridItems(currentStickers) { emoji ->
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFF8FAFC))
                                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    val newItem = OverlayItem.Emoji(
                                                        id = "emoji_${kotlin.random.Random.nextLong()}",
                                                        emoji = emoji,
                                                        offset = Offset.Zero,
                                                        rotation = 0f,
                                                        scale = 1f
                                                    )
                                                    overlayItems = overlayItems + newItem
                                                    selectedOverlayId = newItem.id
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(emoji, fontSize = 24.sp)
                                        }
                                    }
                                }
                            }
                        }
                        EditorTool.ADJUST -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                val currentValue = adjustValues[selectedAdjustItem] ?: 0f
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("$selectedAdjustItem: ${if (currentValue > 0) "+" else ""}${currentValue.toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FluidSlider(
                                        value = currentValue,
                                        onValueChange = { newVal ->
                                            adjustValues = adjustValues + (selectedAdjustItem to newVal)
                                        },
                                        valueRange = -50f..50f,
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(adjustOptions) { option ->
                                        val isSelected = selectedAdjustItem == option.name
                                        Box(
                                            modifier = Modifier
                                                .width(72.dp)
                                                .height(68.dp)
                                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) {
                                                        Brush.verticalGradient(listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)))
                                                    } else {
                                                        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC)))
                                                    }
                                                )
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedAdjustItem = option.name },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = option.icon,
                                                    contentDescription = option.name,
                                                    tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF475569),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = option.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        /*
                        EditorTool.GLITTER -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .animateContentSize()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Glitters", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Density: ${glitterDensity.toInt()}%", fontSize = 12.sp, color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FluidSlider(
                                        value = glitterDensity,
                                        onValueChange = { glitterDensity = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(glitterCards) { card ->
                                        val isSelected = selectedGlitterCard == card
                                        Box(
                                            modifier = Modifier
                                                .width(76.dp)
                                                .height(64.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF0F172A))
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedGlitterCard = card },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFEF08A),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = card,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        */
                        EditorTool.GLITTER -> {}
                        EditorTool.TEXT -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "+ Add Text",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.clickable { showAddTextDialog = true }
                                    )
                                    Text(
                                        text = "Font: $selectedFont",
                                        fontSize = 12.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                }

                                Text("Text Color", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(textColors) { col ->
                                        val isSelected = textColor == col
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(col)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                    shape = CircleShape
                                                )
                                                .clickable { textColor = col }
                                        )
                                    }
                                }

                                val hasText = collageText.isNotBlank()
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(110.dp)
                                ) {
                                    gridItems(fontCards) { font ->
                                        val isSelected = selectedFont == font
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                                .alpha(if (hasText) 1f else 0.4f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable(enabled = hasText) { selectedFont = font },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = font,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E293B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        EditorTool.BACKGROUND -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Background", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                Text("Solid Colors", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(solidColors) { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                                                .clickable {
                                                    canvasBgColor = color
                                                    canvasBgGradient = null
                                                    canvasBgGradientColors = emptyList()
                                                    selectedBackgroundPhoto = null
                                                    backgroundFade = 0f
                                                }
                                        )
                                    }
                                }

                                Text("Gradients", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(gradients) { grad ->
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(grad)
                                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                                                .clickable {
                                                    canvasBgGradient = grad
                                                    canvasBgGradientColors = when (gradients.indexOf(grad)) {
                                                        0 -> listOf(Color(0xFFEF4444), Color(0xFFFACC15), Color(0xFF22C55E), Color(0xFF3B82F6))
                                                        1 -> listOf(Color(0xFFF43F5E), Color(0xFFFBBF24))
                                                        2 -> listOf(Color(0xFFFB923C), Color(0xFFF472B6), Color(0xFF38BDF8))
                                                        3 -> listOf(Color(0xFF34D399), Color(0xFF2563EB))
                                                        4 -> listOf(Color(0xFFF5D0FE), Color(0xFF93C5FD))
                                                        5 -> listOf(Color(0xFF0F172A), Color(0xFF7DD3FC))
                                                        6 -> listOf(Color(0xFFFF007F), Color(0xFFFF758C), Color(0xFFFF7EB3))
                                                        7 -> listOf(Color(0xFF651FFF), Color(0xFF00E5FF))
                                                        8 -> listOf(Color(0xFFFF9100), Color(0xFFFFEA00))
                                                        9 -> listOf(Color(0xFF00E676), Color(0xFF1DE9B6))
                                                        10 -> listOf(Color(0xFFF50057), Color(0xFFFF4081))
                                                        11 -> listOf(Color(0xFF3D5AFE), Color(0xFF2F80ED))
                                                        12 -> listOf(Color(0xFFFF3D00), Color(0xFFFF9100))
                                                        13 -> listOf(Color(0xFF1A237E), Color(0xFF3949AB))
                                                        14 -> listOf(Color(0xFF004D40), Color(0xFF00897B))
                                                        15 -> listOf(Color(0xFFB71C1C), Color(0xFFD32F2F))
                                                        16 -> listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))
                                                        17 -> listOf(Color(0xFF880E4F), Color(0xFFC2185B))
                                                        18 -> listOf(Color(0xFF0D47A1), Color(0xFF1976D2))
                                                        else -> emptyList()
                                                    }
                                                    selectedBackgroundPhoto = null
                                                    backgroundFade = 0f
                                                }
                                        )
                                    }
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Fade Effect: ${backgroundFade.toInt()}%", fontSize = 12.sp, color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FluidSlider(
                                        value = backgroundFade,
                                        onValueChange = { backgroundFade = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    )
                                }

                                Text("Use Added Collage Photos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(selectedPhotos) { photoUri ->
                                        val isSelected = selectedBackgroundPhoto == photoUri
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    selectedBackgroundPhoto = photoUri
                                                    backgroundFade = 0f
                                                }
                                        ) {
                                            AsyncImage(
                                                model = photoUri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        EditorTool.DRAW -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Draw & Brush", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                Text("Brush Type & Eraser", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    brushTypes.forEach { type ->
                                        val isSelected = selectedBrushType == type
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedBrushType = type },
                                            label = { Text(type) }
                                        )
                                    }
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Density: ${brushDensity.toInt()}%", fontSize = 11.sp, color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FluidSlider(
                                        value = brushDensity,
                                        onValueChange = { brushDensity = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    )
                                }

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Opacity: ${brushOpacity.toInt()}%", fontSize = 11.sp, color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    FluidSlider(
                                        value = brushOpacity,
                                        onValueChange = { brushOpacity = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    )
                                }

                                Text("Brush Color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(brushColors) { col ->
                                        val isSelected = brushColor == col
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(col)
                                                .border(
                                                    width = if (isSelected) 2.5.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                    shape = CircleShape
                                                )
                                                .clickable { brushColor = col }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Horizontal scrollable bottom tabs list with ALL 10 tools
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tools = listOf(
                        Triple(EditorTool.RATIO, "Ratio", Icons.Default.Crop),
                        Triple(EditorTool.LAYOUT, "Layout", Icons.Default.GridView),
                        Triple(EditorTool.BORDER, "Border", Icons.Default.Dashboard),
                        Triple(EditorTool.FILTER, "Filter", Icons.Default.Star),
                        Triple(EditorTool.STICKERS, "Stickers", Icons.Default.EmojiEmotions),
                        Triple(EditorTool.ADJUST, "Adjust", Icons.Default.Tune),
                        // Triple(EditorTool.GLITTER, "Glitter", Icons.Default.AutoAwesome),
                        Triple(EditorTool.TEXT, "Text", null),
                        Triple(EditorTool.BACKGROUND, "Background", Icons.Default.Wallpaper),
                        Triple(EditorTool.DRAW, "Draw", Icons.Default.Edit)
                    )

                    tools.forEach { (tool, label, icon) ->
                        val isSelected = selectedTab == tool
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
                                .clickable { selectedTab = tool }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = "Tt",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE2E8F0))
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            val bgModifier = when {
                selectedBackgroundPhoto != null -> Modifier
                canvasBgGradient != null -> Modifier.background(canvasBgGradient!!)
                else -> Modifier.background(canvasBgColor)
            }

            val brightnessVal = (adjustValues["Brightness"] ?: 0f) / 50f
            val contrastVal = (adjustValues["Contrast"] ?: 0f) / 50f
            val highlightVal = (adjustValues["Highlight"] ?: 0f) / 50f
            val shadowVal = (adjustValues["Shadow"] ?: 0f) / 50f
            val saturationVal = (adjustValues["Saturation"] ?: 0f) / 50f
            val warmthVal = (adjustValues["Warmth"] ?: 0f) / 50f
            val tintVal = (adjustValues["Tint"] ?: 0f) / 50f
            val fadeVal = 1f - ((adjustValues["Fade"] ?: 0f) + 50f) / 100f * 0.3f

            val brightnessColor = if (brightnessVal > 0f) Color.White.copy(alpha = brightnessVal * 0.35f) else Color.Black.copy(alpha = -brightnessVal * 0.35f)
            val contrastColor = if (contrastVal > 0f) Color.Black.copy(alpha = contrastVal * 0.2f) else Color.White.copy(alpha = -contrastVal * 0.2f)
            val highlightColor = Color.White.copy(alpha = maxOf(0f, highlightVal * 0.25f))
            val shadowColor = Color.Black.copy(alpha = maxOf(0f, -shadowVal * 0.25f))
            val saturationAlpha = if (saturationVal < 0f) -saturationVal * 0.5f else 0f
            val warmthColor = Color(0xFFFFB703).copy(alpha = maxOf(0f, warmthVal * 0.25f))
            val tintColor = Color(0xFFFB6F92).copy(alpha = maxOf(0f, tintVal * 0.25f))

            // Collage Canvas Box with FunPic-style Layout Grid Thumbnails and Full Editing Suite
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(aspectRatio.widthRatio / aspectRatio.heightRatio)
                    .clip(RoundedCornerShape(borderSettings.cornerRadius.dp))
                    .then(bgModifier)
                    .alpha(fadeVal.coerceIn(0.4f, 1f))
                    .onGloballyPositioned { coordinates ->
                        canvasSizePx = coordinates.size
                    }
                    .pointerInput(selectedTab, selectedBrushType, brushColor, brushDensity) {
                        if (selectedTab == EditorTool.DRAW)
                        {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStrokePoints = listOf(offset)
                                    if (selectedBrushType == "Eraser") {
                                        drawStrokes = drawStrokes.filter { stroke ->
                                            stroke.points.none { sp ->
                                                val dx = sp.x - offset.x
                                                val dy = sp.y - offset.y
                                                (dx * dx + dy * dy) < (40f * 40f)
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, _ ->
                                    currentStrokePoints = currentStrokePoints + change.position
                                    if (selectedBrushType == "Eraser") {
                                        val ep = change.position
                                        drawStrokes = drawStrokes.filter { stroke ->
                                            stroke.points.none { sp ->
                                                val dx = sp.x - ep.x
                                                val dy = sp.y - ep.y
                                                (dx * dx + dy * dy) < (40f * 40f)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (currentStrokePoints.isNotEmpty()) {
                                        val isEraser = selectedBrushType == "Eraser"
                                        if (!isEraser) {
                                            val strokeWidth = 6f * (brushDensity / 50f)
                                            drawStrokes = drawStrokes + DrawStroke(
                                                points = currentStrokePoints,
                                                color = brushColor,
                                                strokeWidth = strokeWidth,
                                                isEraser = false
                                            )
                                        }
                                        currentStrokePoints = emptyList()
                                    }
                                }
                            )
                        }
                    }
            ) {
                if (selectedBackgroundPhoto != null) {
                    AsyncImage(
                        model = selectedBackgroundPhoto,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (backgroundFade > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = (backgroundFade / 100f) * 0.7f))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(borderSettings.outerSpacing.dp)
                ) {
                    RenderCollageLayout(
                        layoutId = currentLayoutId,
                        photos = selectedPhotos,
                        spacing = borderSettings.spacing.dp,
                        cornerRadius = borderSettings.cornerRadius.dp,
                        filterPreset = selectedPreset,
                        filterIntensity = filterIntensity,
                        glitterCard = selectedGlitterCard,
                        glitterDensity = glitterDensity,
                        isDrawingMode = (selectedTab == EditorTool.DRAW),
                        slots = currentSlots,
                        selectedSlotIndex = selectedSlotIndex,
                        onSelectSlot = { selectedSlotIndex = it },
                        onSlotsChanged = { currentSlots = it },
                        photoTransforms = photoTransforms,
                        onPhotoTransformChanged = { idx, trans ->
                            photoTransforms = photoTransforms + (idx to trans)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (brightnessColor.alpha > 0.01f) {
                    Box(modifier = Modifier.fillMaxSize().background(brightnessColor))
                }
                if (contrastColor.alpha > 0.01f) {
                    Box(modifier = Modifier.fillMaxSize().background(contrastColor))
                }
                if (highlightColor.alpha > 0.01f) {
                    Box(modifier = Modifier.fillMaxSize().background(highlightColor))
                }
                if (shadowColor.alpha > 0.01f) {
                    Box(modifier = Modifier.fillMaxSize().background(shadowColor))
                }
                if (saturationAlpha > 0.01f) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = saturationAlpha)))
                }
                if (warmthColor.alpha > 0.01f) {
                    Box(modifier = Modifier.fillMaxSize().background(warmthColor))
                }
                if (tintColor.alpha > 0.01f) {
                    Box(modifier = Modifier.fillMaxSize().background(tintColor))
                }

                // Render Drawing Canvas Overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawStrokes.forEach { stroke ->
                        if (stroke.points.size > 1 && !stroke.isEraser) {
                            val path = Path().apply {
                                moveTo(stroke.points[0].x, stroke.points[0].y)
                                for (i in 1 until stroke.points.size) {
                                    lineTo(stroke.points[i].x, stroke.points[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = stroke.color,
                                alpha = brushOpacity / 100f,
                                style = Stroke(
                                    width = stroke.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                    if (currentStrokePoints.size > 1 && selectedBrushType != "Eraser") {
                        val path = Path().apply {
                            moveTo(currentStrokePoints[0].x, currentStrokePoints[0].y)
                            for (i in 1 until currentStrokePoints.size) {
                                lineTo(currentStrokePoints[i].x, currentStrokePoints[i].y)
                            }
                        }
                        val strokeWidth = 6f * (brushDensity / 50f)
                        drawPath(
                            path = path,
                            color = brushColor,
                            alpha = brushOpacity / 100f,
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Render Movable Sticker Overlays with Delete (X), Rotate (🔄), and Enlarge (⤢ with zoom in/out drag gesture)
                overlayItems.forEach { item ->
                    var itemOffset by remember(item.id) { mutableStateOf(item.offset) }
                    var itemRotation by remember(item.id) { mutableStateOf(item.rotation) }
                    var itemScale by remember(item.id) { mutableStateOf(item.scale) }
                    val isSelected = selectedOverlayId == item.id

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset(itemOffset.x.toInt(), itemOffset.y.toInt()) }
                            .rotate(itemRotation)
                            .scale(itemScale)
                            .then(
                                if (selectedTab != EditorTool.DRAW) {
                                    Modifier.pointerInput(item.id) {
                                        detectDragGestures { _, dragAmount ->
                                            selectedOverlayId = item.id
                                            itemOffset += dragAmount
                                            overlayItems = overlayItems.map {
                                                if (it.id == item.id) {
                                                    when (it) {
                                                        is OverlayItem.Emoji -> it.copy(offset = itemOffset)
                                                        is OverlayItem.Photo -> it.copy(offset = itemOffset)
                                                    }
                                                } else it
                                            }
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { selectedOverlayId = item.id }
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            when (item) {
                                is OverlayItem.Emoji -> {
                                    Text(text = item.emoji, fontSize = 36.sp)
                                }
                                is OverlayItem.Photo -> {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                        }

                        // Delete / Close Icon (Top-Right)
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-8).dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                                    .clickable {
                                        overlayItems = overlayItems.filter { it.id != item.id }
                                        if (selectedOverlayId == item.id) selectedOverlayId = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Rotate Icon (Top-Left)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-8).dp, y = (-8).dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
                                    .clickable {
                                        itemRotation = (itemRotation + 15f) % 360f
                                        overlayItems = overlayItems.map {
                                            if (it.id == item.id) {
                                                when (it) {
                                                    is OverlayItem.Emoji -> it.copy(rotation = itemRotation)
                                                    is OverlayItem.Photo -> it.copy(rotation = itemRotation)
                                                }
                                            } else it
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Rotate",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Enlarge / Scale Handle with drag gesture for zoomIn/zoomOut (Bottom-Right)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 12.dp, y = 12.dp)
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                                    .pointerInput(item.id) {
                                        detectDragGestures { _, dragAmount ->
                                            val scaleDelta = (dragAmount.x + dragAmount.y) * 0.005f
                                            itemScale = (itemScale + scaleDelta).coerceIn(0.4f, 3.5f)
                                            overlayItems = overlayItems.map {
                                                if (it.id == item.id) {
                                                    when (it) {
                                                        is OverlayItem.Emoji -> it.copy(scale = itemScale)
                                                        is OverlayItem.Photo -> it.copy(scale = itemScale)
                                                    }
                                                } else it
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInFull,
                                    contentDescription = "Enlarge / Scale",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Render Movable Text Sticker
                if (collageText.isNotBlank()) {
                    val fontStyle = when (selectedFont) {
                        "Script" -> TextStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = textColor)
                        "Bold" -> TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = textColor)
                        "Retro" -> TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = textColor)
                        "Serif" -> TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, color = textColor)
                        "Neon" -> TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, color = textColor, shadow = Shadow(color = textColor, blurRadius = 16f))
                        else -> TextStyle(fontWeight = FontWeight.Medium, fontSize = 26.sp, color = textColor)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset { IntOffset(textOffset.x.toInt(), textOffset.y.toInt()) }
                            .then(
                                if (selectedTab != EditorTool.DRAW) {
                                    Modifier.pointerInput(Unit) {
                                        detectDragGestures { _, dragAmount ->
                                            textOffset += dragAmount
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .border(1.5.dp, Color(0xFF2563EB), RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = collageText,
                            style = fontStyle
                        )
                    }
                }
            }
        }

        if (showAddTextDialog) {
            var tempText by remember { mutableStateOf(collageText) }
            AlertDialog(
                onDismissRequest = { showAddTextDialog = false },
                title = { Text("Add / Edit Text") },
                text = {
                    OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        label = { Text("Collage Text") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        collageText = tempText
                        showAddTextDialog = false
                    }) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTextDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showExportDialog) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val canvasWidthDp = if (canvasSizePx.width > 0) with(density) { canvasSizePx.width.toDp().value } else 320f
            val currentCanvasW = if (canvasSizePx.width > 0) canvasSizePx.width.toFloat() else 1000f
            val currentCanvasH = if (canvasSizePx.height > 0) canvasSizePx.height.toFloat() else 1000f

            val textSizeSp = when (selectedFont) { "Bold" -> 32f; "Retro" -> 26f; "Neon" -> 30f; else -> 28f }
            val textSizePx = with(density) { textSizeSp.sp.toPx() }

            val project = CollageProject(
                layoutId = currentLayoutId,
                slots = currentSlots,
                photos = selectedPhotos.mapIndexed { i, uri ->
                    photoTransforms[i]?.copy(uri = uri) ?: PhotoTransform(uri = uri)
                },
                aspectRatioWidth = aspectRatio.widthRatio,
                aspectRatioHeight = aspectRatio.heightRatio,
                outerSpacingRatio = borderSettings.outerSpacing / canvasWidthDp,
                innerSpacingRatio = borderSettings.spacing / canvasWidthDp,
                cornerRadiusRatio = borderSettings.cornerRadius / canvasWidthDp,
                backgroundColorLong = canvasBgColor.toArgb().toLong(),
                backgroundGradientColors = canvasBgGradientColors.map { it.toArgb().toLong() },
                backgroundPhotoUri = selectedBackgroundPhoto,
                backgroundFade = backgroundFade,
                filterPreset = selectedPreset,
                filterIntensity = filterIntensity,
                adjustValues = adjustValues,
                overlays = overlayItems.map { item ->
                    val nx = (currentCanvasW / 2f + item.offset.x) / currentCanvasW
                    val ny = (currentCanvasH / 2f + item.offset.y) / currentCanvasH
                    when (item) {
                        is OverlayItem.Emoji -> ProjectOverlayItem.Emoji(
                            id = item.id,
                            emoji = item.emoji,
                            x = nx,
                            y = ny,
                            rotation = item.rotation,
                            scale = item.scale
                        )
                        is OverlayItem.Photo -> ProjectOverlayItem.Photo(
                            id = item.id,
                            uri = item.uri,
                            x = nx,
                            y = ny,
                            rotation = item.rotation,
                            scale = item.scale
                        )
                    }
                },
                textLayers = if (collageText.isNotBlank()) listOf(
                    ProjectTextLayer(
                        text = collageText,
                        x = (currentCanvasW / 2f + textOffset.x) / currentCanvasW,
                        y = (currentCanvasH / 2f + textOffset.y) / currentCanvasH,
                        fontName = selectedFont,
                        colorLong = textColor.toArgb().toLong(),
                        fontSizeRatio = textSizePx / currentCanvasW
                    )
                ) else emptyList(),
                drawStrokes = drawStrokes.map { stroke ->
                    NormalizedDrawStroke(
                        points = stroke.points.map { pt ->
                            NormalizedPoint(
                                x = pt.x / currentCanvasW,
                                y = pt.y / currentCanvasH
                            )
                        },
                        colorLong = stroke.color
                            .copy(alpha = stroke.color.alpha * brushOpacity / 100f)
                            .toArgb().toLong(),
                        strokeWidthRatio = stroke.strokeWidth / currentCanvasW,
                        isEraser = stroke.isEraser
                    )
                }
            )

            Box(modifier = Modifier.fillMaxSize().zIndex(100f).background(Color.White)) {
                ExportShareScreen(
                    project = project,
                    layoutId = currentLayoutId,
                    selectedPhotos = selectedPhotos,
                    slots = currentSlots,
                    spacingDp = borderSettings.spacing,
                    outerSpacingDp = borderSettings.outerSpacing,
                    cornerRadiusDp = borderSettings.cornerRadius,
                    filterPreset = selectedPreset,
                    filterIntensity = filterIntensity,
                    backgroundPhoto = selectedBackgroundPhoto,
                    backgroundFade = backgroundFade,
                    backgroundColor = canvasBgColor,
                    backgroundGradientColors = canvasBgGradientColors,
                    aspectRatioWidth = aspectRatio.widthRatio,
                    aspectRatioHeight = aspectRatio.heightRatio,
                    adjustValues = adjustValues,
                    overlayItems = overlayItems,
                    drawStrokes = drawStrokes,
                    collageText = collageText,
                    textColor = textColor,
                    selectedFont = selectedFont,
                    textOffset = textOffset,
                    onHome = { showExportDialog = false; onBack() },
                    onBack = { showExportDialog = false },
                    onCreateAnother = { showExportDialog = false; onBack() }
                )
            }
        }
    }
}

fun getFilterTintColor(preset: String): Color {
    return when (preset) {
        "Clarendon" -> Color(0xFF38BDF8)
        "Gingham" -> Color(0xFFF1F5F9)
        "Moon" -> Color(0xFF94A3B8)
        "Lark" -> Color(0xFFFDE047)
        "Reyes" -> Color(0xFFFED7AA)
        "Juno" -> Color(0xFF60A5FA)
        "Slumber" -> Color(0xFFCBD5E1)
        "Crema" -> Color(0xFFFEF08A)
        "Ludwig" -> Color(0xFFFCA5A5)
        "Aden" -> Color(0xFFF472B6)
        "Perpetua" -> Color(0xFF34D399)
        "Amaro" -> Color(0xFFFCD34D)
        "Mayfair" -> Color(0xFFFBCFE8)
        "Rise" -> Color(0xFFFDE68A)
        "Hudson" -> Color(0xFF93C5FD)
        "Valencia" -> Color(0xFFFDBA74)
        "X-Pro II" -> Color(0xFF818CF8)
        "Sierra" -> Color(0xFFD8B4FE)
        "Willow" -> Color(0xFFE2E8F0)
        "Lo-fi" -> Color(0xFFF43F5E)
        "Vivid" -> Color(0xFFE11D48)
        "Golden" -> Color(0xFFF59E0B)
        "Retro" -> Color(0xFF9A3412)
        "Soft" -> Color(0xFFFBCFE8)
        "Moody" -> Color(0xFF1E3A8A)
        "Warm" -> Color(0xFFFB923C)
        "Vintage" -> Color(0xFF78350F)
        "B&W" -> Color.Gray
        "Neon" -> Color(0xFF10B981)
        else -> Color(0xFFE2E8F0)
    }
}

@Composable
fun LayoutThumbnailPreview(layoutId: String, isSelected: Boolean) {
    val strokeColor = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)
    val strokeWidth = if (isSelected) 2.5f else 1.5f

    val repository = remember { com.oem.collagekmpapp.data.LayoutRepository() }
    val layout = remember(layoutId) {
        repository.getAllLayouts().find { it.id == layoutId }
    }

    Canvas(modifier = Modifier.size(44.dp)) {
        val w = size.width
        val h = size.height

        drawRect(color = strokeColor, style = Stroke(width = strokeWidth))

        if (layout != null) {
            layout.slots.forEach { slot ->
                val left = slot.left * w
                val top = slot.top * h
                val width = (slot.right - slot.left) * w
                val height = (slot.bottom - slot.top) * h
                drawRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    style = Stroke(width = strokeWidth * 0.75f)
                )
            }
        } else {
            drawLine(color = strokeColor, start = Offset(w * 0.5f, 0f), end = Offset(w * 0.5f, h), strokeWidth = strokeWidth)
        }
    }
}
