package com.oem.collagekmpapp.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.oem.collagekmpapp.model.*

enum class EditorTool {
    RATIO, LAYOUT, BORDER, FILTER, ADJUST, GLITTER, TEXT, BACKGROUND, DRAW
}

data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageEditorScreen(
    layoutId: String,
    selectedPhotos: List<String>,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(EditorTool.DRAW) }
    var currentLayoutId by remember { mutableStateOf(layoutId) }
    var aspectRatio by remember { mutableStateOf(AspectRatio.SQUARE) }
    var borderSettings by remember { mutableStateOf(BorderSettings(outerSpacing = 12f, cornerRadius = 22f, spacing = 8f)) }

    // Filter & Glitter states
    var filterIntensity by remember { mutableStateOf(75f) }
    var selectedPreset by remember { mutableStateOf("Golden") }
    var glitterDensity by remember { mutableStateOf(65f) }
    var selectedGlitterCard by remember { mutableStateOf("Flare 2") }

    // Color Grading / Adjust states matching 12_adjust_color_grading.svg
    var selectedAdjustItem by remember { mutableStateOf("Brightness") }
    var adjustValues by remember { mutableStateOf(mapOf(
        "Brightness" to 50f,
        "Contrast" to 50f,
        "Highlight" to 50f,
        "Shadow" to 50f,
        "Saturation" to 50f,
        "Warmth" to 50f,
        "Tint" to 50f,
        "Fade" to 0f
    )) }

    // Text typography states with custom color list
    var selectedFont by remember { mutableStateOf("Script") }
    var collageText by remember { mutableStateOf("") }
    var textColor by remember { mutableStateOf(Color.White) }
    var textOffset by remember { mutableStateOf(Offset.Zero) }
    var showAddTextDialog by remember { mutableStateOf(false) }

    // Background settings states matching 09_background_settings_screen.svg
    var canvasBgColor by remember { mutableStateOf<Color>(Color(0xFFF8FAFC)) }
    var canvasBgGradient by remember { mutableStateOf<Brush?>(null) }
    var backgroundBlur by remember { mutableStateOf(40f) }
    var selectedBackgroundPhoto by remember { mutableStateOf<String?>(null) }

    // Draw & Brush states matching 10_draw_brush_screen.svg
    var selectedBrushType by remember { mutableStateOf("Neon") }
    var brushDensity by remember { mutableStateOf(35f) }
    var brushOpacity by remember { mutableStateOf(100f) }
    var brushColor by remember { mutableStateOf(Color(0xFFF43F8E)) }

    // Live drawing strokes list
    var drawStrokes by remember { mutableStateOf(listOf<DrawStroke>()) }
    var currentStrokePoints by remember { mutableStateOf(listOf<Offset>()) }

    var showExportDialog by remember { mutableStateOf(false) }

    val count = selectedPhotos.size.coerceAtLeast(2)

    val layouts = remember(count) {
        when {
            count == 2 -> listOf(
                LayoutOption("layout_2_vertical", "Split Vertical", 2, "2 photos side by side"),
                LayoutOption("layout_2_horizontal", "Split Horizontal", 2, "2 photos top & bottom")
            )
            count == 3 -> listOf(
                LayoutOption("layout_3_top", "Triple Top", 3, "1 top, 2 bottom"),
                LayoutOption("layout_3_left", "Triple Left", 3, "1 left, 2 right")
            )
            count == 4 -> listOf(
                LayoutOption("layout_pinwheel", "Asymmetric Pinwheel", 4, "Dynamic split layout"),
                LayoutOption("layout_vertical_strips", "Vertical Strips", 4, "3 vertical columns"),
                LayoutOption("layout_top_right", "Top & Right Stack", 4, "Large top + stacked right"),
                LayoutOption("layout_large_top", "Large Top & Bottom", 4, "Large top + 3 bottom tiles")
            )
            else -> listOf(
                LayoutOption("layout_5_grid", "Multi Grid", count, "$count photo custom layout")
            )
        }
    }

    val filterPresets = listOf("Original", "Vivid", "Golden", "Retro", "Soft", "Moody")
    val glitterCategories = listOf("Sparkle", "Bokeh", "Stardust", "Light Leak", "Neon")
    val glitterCards = listOf("Glitz", "Spark 1", "Flare 2", "Dust")
    val fontCards = listOf("Modern", "Script", "Bold", "Retro", "Serif", "Neon")
    val adjustItems = listOf("Brightness", "Contrast", "Highlight", "Shadow", "Saturation", "Warmth", "Tint", "Fade")

    val solidColors = listOf(
        Color(0xFFFFFFFF),
        Color(0xFFCBD5E1),
        Color(0xFF1E293B),
        Color(0xFFEF4444),
        Color(0xFFF97316),
        Color(0xFFFACC15),
        Color(0xFF22C55E),
        Color(0xFF3B82F6),
        Color(0xFF9333EA)
    )

    val gradients = listOf(
        Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFFACC15), Color(0xFF22C55E), Color(0xFF3B82F6))),
        Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFFBBF24))),
        Brush.linearGradient(listOf(Color(0xFFFB923C), Color(0xFFF472B6), Color(0xFF38BDF8))),
        Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF2563EB))),
        Brush.linearGradient(listOf(Color(0xFFF5D0FE), Color(0xFF93C5FD))),
        Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF7DD3FC)))
    )

    val brushTypes = listOf("Pen", "Marker", "Neon", "Eraser")
    val brushColors = listOf(
        Color(0xFFF43F8E),
        Color(0xFFFFFFFF),
        Color(0xFFFACC15),
        Color(0xFFF97316),
        Color(0xFFEF4444),
        Color(0xFF3B82F6),
        Color(0xFF22C55E),
        Color(0xFF9333EA)
    )

    val textColors = listOf(
        Color.White,
        Color.Black,
        Color(0xFF2563EB),
        Color(0xFFEF4444),
        Color(0xFF22C55E),
        Color(0xFFFACC15),
        Color(0xFFEC4899),
        Color(0xFF9333EA),
        Color(0xFF06B6D4)
    )

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
                        .height(220.dp)
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    when (selectedTab) {
                        EditorTool.RATIO -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Canvas Aspect Ratio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilterChip(
                                        selected = aspectRatio == AspectRatio.SQUARE,
                                        onClick = { aspectRatio = AspectRatio.SQUARE },
                                        label = { Text("1:1 Square") }
                                    )
                                    FilterChip(
                                        selected = aspectRatio == AspectRatio.PORTRAIT,
                                        onClick = { aspectRatio = AspectRatio.PORTRAIT },
                                        label = { Text("4:5 Portrait") }
                                    )
                                    FilterChip(
                                        selected = aspectRatio == AspectRatio.STORY,
                                        onClick = { aspectRatio = AspectRatio.STORY },
                                        label = { Text("9:16 Story") }
                                    )
                                }
                            }
                        }
                        EditorTool.LAYOUT -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Select Layout", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(160.dp)
                                ) {
                                    gridItems(layouts) { layout ->
                                        val isSelected = currentLayoutId == layout.id
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF1F5F9))
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { currentLayoutId = layout.id },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = layout.name.take(6),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E293B)
                                            )
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

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Outer Spacing: ${borderSettings.outerSpacing.toInt()}px", fontSize = 11.sp)
                                    Slider(
                                        value = borderSettings.outerSpacing,
                                        onValueChange = { borderSettings = borderSettings.copy(outerSpacing = it) },
                                        valueRange = 0f..30f,
                                        modifier = Modifier.width(180.dp).height(20.dp)
                                    )
                                    Text("Inner Spacing: ${borderSettings.spacing.toInt()}px", fontSize = 11.sp)
                                    Slider(
                                        value = borderSettings.spacing,
                                        onValueChange = { borderSettings = borderSettings.copy(spacing = it) },
                                        valueRange = 0f..20f,
                                        modifier = Modifier.width(180.dp).height(20.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Corner Radius: ${borderSettings.cornerRadius.toInt()}px", fontSize = 11.sp)
                                    Slider(
                                        value = borderSettings.cornerRadius,
                                        onValueChange = { borderSettings = borderSettings.copy(cornerRadius = it) },
                                        valueRange = 0f..40f,
                                        modifier = Modifier.width(220.dp).height(20.dp)
                                    )
                                }
                            }
                        }
                        EditorTool.FILTER -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Filters", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Intensity", fontSize = 12.sp, color = Color(0xFF334155))
                                    Slider(
                                        value = filterIntensity,
                                        onValueChange = { filterIntensity = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.width(260.dp).height(20.dp)
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(filterPresets) { preset ->
                                        val isSelected = selectedPreset == preset
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { selectedPreset = preset }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE2E8F0))
                                                    .border(
                                                        width = if (isSelected) 2.5.dp else 0.dp,
                                                        color = Color(0xFF2563EB),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(preset.take(2), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
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
                        EditorTool.ADJUST -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Adjust & Color Grading", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                val currentValue = adjustValues[selectedAdjustItem] ?: 50f
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("$selectedAdjustItem: ${currentValue.toInt()}%", fontSize = 12.sp, color = Color(0xFF334155))
                                    Slider(
                                        value = currentValue,
                                        onValueChange = { newVal ->
                                            adjustValues = adjustValues + (selectedAdjustItem to newVal)
                                        },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.width(240.dp).height(20.dp)
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(adjustItems) { item ->
                                        val isSelected = selectedAdjustItem == item
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedAdjustItem = item },
                                            label = { Text(item) }
                                        )
                                    }
                                }
                            }
                        }
                        EditorTool.GLITTER -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Glitters & Effects", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    glitterCategories.forEach { category ->
                                        TextButton(onClick = {}) {
                                            Text(text = category, fontSize = 13.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Density", fontSize = 12.sp, color = Color(0xFF334155))
                                    Slider(
                                        value = glitterDensity,
                                        onValueChange = { glitterDensity = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.width(260.dp).height(20.dp)
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

                                // Text Color Palette List as requested
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
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedFont = font },
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
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
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
                                                    selectedBackgroundPhoto = null
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
                                                    selectedBackgroundPhoto = null
                                                }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Blur Effect: ${backgroundBlur.toInt()}%", fontSize = 12.sp, color = Color(0xFF334155))
                                    Slider(
                                        value = backgroundBlur,
                                        onValueChange = { backgroundBlur = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.width(220.dp).height(20.dp)
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

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Density: ${brushDensity.toInt()}%", fontSize = 11.sp)
                                    Slider(
                                        value = brushDensity,
                                        onValueChange = { brushDensity = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.width(160.dp).height(20.dp)
                                    )
                                    Text("Opacity: ${brushOpacity.toInt()}%", fontSize = 11.sp)
                                    Slider(
                                        value = brushOpacity,
                                        onValueChange = { brushOpacity = it },
                                        valueRange = 0f..100f,
                                        modifier = Modifier.width(160.dp).height(20.dp)
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

                // Horizontal scrollable bottom tabs list with ALL 9 tools (Ratio, Layout, Border, Filter, Adjust, Glitter, Text, Background, Draw)
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
                        Triple(EditorTool.ADJUST, "Adjust", Icons.Default.Tune),
                        Triple(EditorTool.GLITTER, "Glitter", Icons.Default.AutoAwesome),
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

            val fadeVal = 1f - ((adjustValues["Fade"] ?: 0f) / 100f * 0.3f)

            // Collage Canvas Box with Drawing (showing strokes on image) & Text overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(aspectRatio.widthRatio / aspectRatio.heightRatio)
                    .clip(RoundedCornerShape(borderSettings.cornerRadius.dp))
                    .then(bgModifier)
                    .alpha(fadeVal.coerceIn(0.5f, 1f))
                    .pointerInput(selectedTab, selectedBrushType, brushColor, brushDensity) {
                        if (selectedTab == EditorTool.DRAW) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStrokePoints = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    currentStrokePoints = currentStrokePoints + change.position
                                },
                                onDragEnd = {
                                    if (currentStrokePoints.isNotEmpty()) {
                                        val isEraser = selectedBrushType == "Eraser"
                                        val strokeWidth = when (selectedBrushType) {
                                            "Marker" -> 16f
                                            "Neon" -> 10f
                                            "Eraser" -> 24f
                                            else -> 6f
                                        } * (brushDensity / 50f)
                                        drawStrokes = drawStrokes + DrawStroke(
                                            points = currentStrokePoints,
                                            color = if (isEraser) Color.Transparent else brushColor,
                                            strokeWidth = strokeWidth,
                                            isEraser = isEraser
                                        )
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = (backgroundBlur / 100f) * 0.5f))
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
                        glitterCard = "Flare 2",
                        glitterDensity = glitterDensity,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Render Drawing Canvas Overlay over image so drawings show up!
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawStrokes.forEach { stroke ->
                        if (stroke.points.size > 1) {
                            for (i in 0 until stroke.points.size - 1) {
                                val start = stroke.points[i]
                                val end = stroke.points[i + 1]
                                if (!stroke.isEraser) {
                                    drawLine(
                                        color = stroke.color.copy(alpha = brushOpacity / 100f),
                                        start = start,
                                        end = end,
                                        strokeWidth = stroke.strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                    if (currentStrokePoints.size > 1) {
                        for (i in 0 until currentStrokePoints.size - 1) {
                            val start = currentStrokePoints[i]
                            val end = currentStrokePoints[i + 1]
                            val isEraser = selectedBrushType == "Eraser"
                            val strokeWidth = when (selectedBrushType) {
                                "Marker" -> 16f
                                "Neon" -> 10f
                                "Eraser" -> 24f
                                else -> 6f
                            } * (brushDensity / 50f)

                            if (!isEraser) {
                                drawLine(
                                    color = brushColor.copy(alpha = brushOpacity / 100f),
                                    start = start,
                                    end = end,
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }

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
                            .pointerInput(Unit) {
                                detectDragGestures { _, dragAmount ->
                                    textOffset += dragAmount
                                }
                            }
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
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export & Share Collage") },
                text = { Text("Your custom collage with ${selectedPhotos.size} photos is ready!") },
                confirmButton = {
                    Button(onClick = { showExportDialog = false }) {
                        Text("Save to Gallery")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
