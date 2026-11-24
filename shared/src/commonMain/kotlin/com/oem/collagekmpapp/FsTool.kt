package com.oem.collagekmpapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.oem.collagekmpapp.AdjustTools
import com.oem.collagekmpapp.FilterPickerPanel
import com.oem.collagekmpapp.FilteredPhoto
import com.oem.collagekmpapp.PhotoColorEngine
import com.oem.collagekmpapp.PhotoEffects
import com.oem.collagekmpapp.components.FluidSlider
import com.oem.collagekmpapp.model.*
import com.oem.collagekmpapp.rememberFreestyleExporter
import com.oem.collagekmpapp.rememberGalleryPicker
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class FsTool(val label: String, val icon: ImageVector) {
    RATIO("Ratio", Icons.Default.Crop),
    BACKGROUND("Backdrop", Icons.Default.Wallpaper),
    FRAME("Frame", Icons.Default.PhotoLibrary),
    TEXT("Text", Icons.Default.TextFields),
    STICKERS("Stickers", Icons.Default.EmojiEmotions),
    FILTER("Filter", Icons.Default.Star),
    ADJUST("Adjust", Icons.Default.Tune),
    DRAW("Draw", Icons.Default.Edit)
}

private val FsPalette: List<Color> = listOf(
    0xFFFFFFFFL, 0xFF0F172AL, 0xFFEF4444L, 0xFFF97316L, 0xFFFACC15L, 0xFF22C55EL, 0xFF06B6D4L,
    0xFF3B82F6L, 0xFF9333EAL, 0xFFEC4899L, 0xFFF1F5F9L, 0xFF94A3B8L, 0xFF475569L, 0xFFFDE68AL,
    0xFFFBCFE8L, 0xFFBAE6FDL, 0xFFBBF7D0L, 0xFFE9D5FFL, 0xFFFED7AAL, 0xFFD6BCFAL, 0xFF78350FL
).map { Color(it) }

private val FsGradients: List<List<Long>> = listOf(
    listOf(0xFFF43F5EL, 0xFFFBBF24L),
    listOf(0xFFFB923CL, 0xFFF472B6L, 0xFF38BDF8L),
    listOf(0xFF34D399L, 0xFF2563EBL),
    listOf(0xFFF5D0FEL, 0xFF93C5FDL),
    listOf(0xFF0F172AL, 0xFF7DD3FCL),
    listOf(0xFFFF007FL, 0xFFFF758CL, 0xFFFF7EB3L),
    listOf(0xFF651FFFL, 0xFF00E5FFL),
    listOf(0xFFFF9100L, 0xFFFFEA00L),
    listOf(0xFF00E676L, 0xFF1DE9B6L),
    listOf(0xFF1A237EL, 0xFF3949ABL),
    listOf(0xFF4A148CL, 0xFF7B1FA2L),
    listOf(0xFFEF4444L, 0xFFFACC15L, 0xFF22C55EL, 0xFF3B82F6L)
)

private val FsStickers = mapOf(
    "Emoji" to listOf("😍", "😎", "🔥", "✨", "🥳", "👑", "🌈", "⭐", "🦋", "💯", "🚀", "💎"),
    "Love" to listOf("🥰", "❤️", "💖", "💕", "💘", "💋", "💌", "💍", "🌹", "🫶", "💞", "😘"),
    "Party" to listOf("🥂", "🎂", "🎈", "🎉", "🎊", "🍾", "🎁", "💃", "🎶", "🍸", "🍕", "🪩"),
    "Travel" to listOf("✈️", "🌴", "🗺️", "🗽", "🗼", "🏕️", "🚗", "🏖️", "🎒", "🚂", "🚢", "🏔️")
)

private val FsFonts = listOf("Modern", "Script", "Bold", "Retro", "Serif", "Neon")

private data class AdjustOption(val name: String, val icon: ImageVector)

private val adjustOptions = listOf(
    AdjustOption("Brightness", Icons.Default.WbSunny),
    AdjustOption("Contrast", Icons.Default.Contrast),
    AdjustOption("Exposure", Icons.Default.Exposure),
    AdjustOption("Highlight", Icons.Default.LightMode),
    AdjustOption("Shadow", Icons.Default.Brightness2),
    AdjustOption("Whites", Icons.Default.Brightness7),
    AdjustOption("Blacks", Icons.Default.Brightness3),
    AdjustOption("Dehaze", Icons.Default.FilterDrama),
    AdjustOption("Saturation", Icons.Default.Palette),
    AdjustOption("Hue", Icons.Default.ColorLens),
    AdjustOption("Warmth", Icons.Default.WbIncandescent),
    AdjustOption("Tint", Icons.Default.Gradient),
    AdjustOption("Red", Icons.Default.Lens),
    AdjustOption("Green", Icons.Default.Lens),
    AdjustOption("Blue", Icons.Default.Lens),
    AdjustOption("Sepia", Icons.Default.FilterVintage),
    AdjustOption("Clarity", Icons.Default.Hd),
    AdjustOption("Sharpen", Icons.Default.ChangeHistory),
    AdjustOption("Blur", Icons.Default.BlurLinear),
    AdjustOption("Soften", Icons.Default.Face),
    AdjustOption("Glow", Icons.Default.AutoAwesome),
    AdjustOption("Grain", Icons.Default.Grain),
    AdjustOption("Vignette", Icons.Default.Vignette),
    AdjustOption("Fade", Icons.Default.BlurOn)
)

private val adjustIconTints = mapOf(
    "Red" to Color(0xFFEF4444),
    "Green" to Color(0xFF22C55E),
    "Blue" to Color(0xFF3B82F6)
)

// =================================================================== geometry helpers

private fun fsRad(deg: Float) = (deg * PI / 180.0).toFloat()

private fun fsHitTest(it: FreestyleItem, p: Offset, w: Float, h: Float): Boolean {
    val v = p - Offset(it.x * w, it.y * h)
    val r = -fsRad(it.rotation)
    val lx = (v.x * cos(r) - v.y * sin(r)) / it.scale
    val ly = (v.x * sin(r) + v.y * cos(r)) / it.scale
    return abs(lx) <= it.widthUnits * w / 2f && abs(ly) <= it.heightUnits * w / 2f
}

/** sy = -1 -> top-right corner (delete), sy = +1 -> bottom-right corner (scale / rotate). Clamped into the canvas. */
private fun fsHandlePos(it: FreestyleItem, w: Float, h: Float, sy: Float, margin: Float): Offset {
    val hw = it.widthUnits * w / 2f * it.scale
    val hh = it.heightUnits * w / 2f * it.scale * sy
    val r = fsRad(it.rotation)
    val x = it.x * w + hw * cos(r) - hh * sin(r)
    val y = it.y * h + hw * sin(r) + hh * cos(r)
    return Offset(x.coerceIn(margin, w - margin), y.coerceIn(margin, h - margin))
}

// =================================================================== drawing helpers

private fun DrawScope.fsDrawTexture(t: BackgroundTexture) {
    if (t == BackgroundTexture.NONE) return
    val w = size.width
    val h = size.height
    val step = 0.05f * w
    val c = Color(0x22000000)
    val sw = 0.004f * w
    when (t) {
        BackgroundTexture.DOTS -> {
            var y = step / 2f
            while (y < h) {
                var x = step / 2f
                while (x < w) { drawCircle(c, 0.006f * w, Offset(x, y)); x += step }
                y += step
            }
        }
        BackgroundTexture.GRID -> {
            var x = 0f
            while (x <= w) { drawLine(c, Offset(x, 0f), Offset(x, h), sw); x += step }
            var y = 0f
            while (y <= h) { drawLine(c, Offset(0f, y), Offset(w, y), sw); y += step }
        }
        BackgroundTexture.STRIPES -> {
            var x = -h
            while (x < w) { drawLine(c, Offset(x, 0f), Offset(x + h, h), sw * 2f); x += step * 1.5f }
        }
        BackgroundTexture.LINES -> {
            var y = step * 1.2f
            while (y < h) { drawLine(c, Offset(0f, y), Offset(w, y), sw); y += step * 1.2f }
            drawLine(Color(0x44EF4444), Offset(0.12f * w, 0f), Offset(0.12f * w, h), sw)
        }
        BackgroundTexture.NONE -> Unit
    }
}

private fun DrawScope.fsDrawStroke(points: List<Offset>, color: Color, widthRatio: Float) {
    if (points.isEmpty()) return
    val widthPx = widthRatio * size.width
    if (points.size == 1) {
        drawCircle(color, widthPx / 2f, Offset(points[0].x * size.width, points[0].y * size.height))
        return
    }
    val path = Path().apply {
        moveTo(points[0].x * size.width, points[0].y * size.height)
        for (i in 1 until points.size) lineTo(points[i].x * size.width, points[i].y * size.height)
    }
    drawPath(path, color, style = Stroke(widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun fsTextStyle(item: FreestyleItem, fontSize: androidx.compose.ui.unit.TextUnit, canvasW: Float): TextStyle {
    val color = Color(item.colorLong.toInt())
    // same canvas-width ratios as FreestyleRenderer.drawTextItem (was fixed px -> differed per device / export size)
    val shadow = when {
        item.fontName == "Neon" -> Shadow(color, Offset.Zero, 0.04f * canvasW)
        item.textShadow -> Shadow(Color(0x99000000), Offset(0.006f * canvasW, 0.006f * canvasW), 0.012f * canvasW)
        else -> null
    }
    val base = when (item.fontName) {
        "Script" -> TextStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
        "Bold" -> TextStyle(fontWeight = FontWeight.Bold)
        "Retro" -> TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        "Serif" -> TextStyle(fontFamily = FontFamily.Serif)
        "Neon" -> TextStyle(fontWeight = FontWeight.Bold)
        else -> TextStyle(fontWeight = FontWeight.Medium)
    }
    return base.copy(color = color, fontSize = fontSize, shadow = shadow)
}

// =================================================================== small UI pieces

@Composable
private fun FsChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF0F172A) else Color(0xFFF1F5F9))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFF334155)
        )
    }
}

@Composable
private fun FsColorRow(selected: Color?, onPick: (Color) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(FsPalette) { col ->
            val isSel = selected == col
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(col)
                    .border(if (isSel) 2.5.dp else 1.dp, if (isSel) Color(0xFF2563EB) else Color(0xFFCBD5E1), CircleShape)
                    .clickable { onPick(col) }
            )
        }
    }
}

@Composable
private fun FsLabel(text: String) =
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))

@Composable
private fun FsSliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 11.sp, color = Color(0xFF334155))
        FluidSlider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth().height(36.dp))
    }
}

@Composable
private fun FsHandle(pos: Offset, color: Color, icon: ImageVector, desc: String) {
    val r = with(LocalDensity.current) { 13.dp.toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((pos.x - r).roundToInt(), (pos.y - r).roundToInt()) }
            .size(26.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = Color.White, modifier = Modifier.size(14.dp)) }
}

// =================================================================== one canvas item (preview)

@Composable
private fun FsItemView(
    item: FreestyleItem,
    canvasW: Float,
    canvasH: Float,
    selected: Boolean,
    matrix: FloatArray?,
    effects: PhotoEffects,
    onAspect: (String, Float) -> Unit
) {
    val density = LocalDensity.current
    val wPx = item.widthUnits * canvasW
    val hPx = item.heightUnits * canvasW
    val shape = RoundedCornerShape(with(density) { (item.cornerUnits * canvasW).toDp() })
    val isPhoto = item.kind == FreestyleItemKind.PHOTO

    Box(
        modifier = Modifier
            .offset { IntOffset((item.x * canvasW - wPx / 2f).roundToInt(), (item.y * canvasH - hPx / 2f).roundToInt()) }
            .size(with(density) { wPx.toDp() }, with(density) { hPx.toDp() })
            .graphicsLayer { rotationZ = item.rotation; scaleX = item.scale; scaleY = item.scale }
            .then(if (isPhoto && item.shadow) Modifier.shadow(with(density) { (0.02f * wPx).toDp() }, shape, clip = false) else Modifier)
            .then(if (isPhoto) Modifier.clip(shape) else Modifier)
            .then(if (isPhoto && item.frame == PhotoFrame.POLAROID) Modifier.background(Color.White) else Modifier)
    ) {
        when (item.kind) {
            FreestyleItemKind.PHOTO -> {
                val polaroid = item.frame == PhotoFrame.POLAROID
                val side = if (polaroid) with(density) { (FreestyleItem.POLAROID_SIDE * wPx).toDp() } else 0.dp
                val bottom = if (polaroid) with(density) { (FreestyleItem.POLAROID_BOTTOM * wPx).toDp() } else 0.dp
                Box(Modifier.fillMaxSize().padding(start = side, top = side, end = side, bottom = bottom)) {
                    // Same composable as the grid editor: colour matrix + blur/soften/clarity/sharpen/glow/grain/vignette,
                    // so every Adjust/Filter result matches PhotoLook in FreestyleRenderer (it used to show colour + a rough vignette only).
                    FilteredPhoto(
                        uri = item.content,
                        matrix = matrix,
                        effects = effects,
                        modifier = Modifier.fillMaxSize(),
                        canvasWidthPx = canvasW,
                        onImageAspect = { a -> onAspect(item.id, a) }
                    )
                }
            }
            FreestyleItemKind.EMOJI -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(item.content, fontSize = with(density) { (wPx * 0.8f).toSp() })
            }
            FreestyleItemKind.TEXT -> Box(
                modifier = Modifier.fillMaxSize().then(
                    if (item.textPill) Modifier.background(
                        Color.Black.copy(alpha = 0.45f),
                        RoundedCornerShape(with(density) { (0.015f * canvasW).toDp() })
                    ) else Modifier
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.content,
                    style = fsTextStyle(item, with(density) { (item.fontSizeRatio * canvasW).toSp() }, canvasW),
                    maxLines = 1, softWrap = false
                )
            }
        }
        if (selected) Box(Modifier.fillMaxSize().border(1.5.dp, Color(0xFF2563EB)))
    }
}

// =================================================================== the screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreestyleEditorScreen(
    selectedPhotos: List<String>,
    onBack: () -> Unit
) {
    val exporter = rememberFreestyleExporter()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val density = LocalDensity.current

    var tool by remember { mutableStateOf(FsTool.FRAME) }
    var freeItems by remember { mutableStateOf(buildInitialFreestyleItems(selectedPhotos)) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var aspect by remember { mutableStateOf(AspectRatio.SQUARE) }
    val ratios = remember { AspectRatio.entries.distinctBy { it.displayName } }

    var bgColor by remember { mutableStateOf(Color(0xFFFFF7ED)) }
    var bgGradient by remember { mutableStateOf<List<Long>>(emptyList()) }
    var bgPhoto by remember { mutableStateOf<String?>(null) }
    var bgBlur by remember { mutableStateOf(100f) }
    var bgTexture by remember { mutableStateOf(BackgroundTexture.NONE) }

    var filterPreset by remember { mutableStateOf("Original") }
    var filterIntensity by remember { mutableStateOf(0f) }
    var adjustValues by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var adjustGroup by remember { mutableStateOf("Light") }
    var adjustItem by remember { mutableStateOf("Brightness") }

    var strokes by remember { mutableStateOf<List<NormalizedDrawStroke>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var eraser by remember { mutableStateOf(false) }
    var brushColor by remember { mutableStateOf(Color(0xFFF43F8E)) }
    var brushSize by remember { mutableStateOf(10f) }
    var brushOpacity by remember { mutableStateOf(100f) }

    var stickerCategory by remember { mutableStateOf("Emoji") }
    var showLayers by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var exportQuality by remember { mutableStateOf(2048) }
    var exportFormat by remember { mutableStateOf("JPG") }
    var textDialogOpen by remember { mutableStateOf(false) }
    var editingTextId by remember { mutableStateOf<String?>(null) }

    fun updateItem(id: String, f: (FreestyleItem) -> FreestyleItem) {
        freeItems = freeItems.map { if (it.id == id) f(it) else it }
    }

    fun moveItem(id: String, delta: Int) {
        val idx = freeItems.indexOfFirst { it.id == id }
        val to = idx + delta
        if (idx < 0 || to !in freeItems.indices) return
        val m = freeItems.toMutableList()
        val tmp = m[idx]; m[idx] = m[to]; m[to] = tmp
        freeItems = m
    }

    val addPhotosPicker = rememberGalleryPicker { uris ->
        if (uris.isNotEmpty()) {
            val base = freeItems.size
            val added = uris.mapIndexed { i, u -> newFreestylePhoto(u, base + i) }
            freeItems = freeItems + added
            selectedId = added.last().id
        }
    }

    val photoMatrix = remember(filterPreset, filterIntensity, adjustValues) {
        PhotoColorEngine.buildMatrix(filterPreset, filterIntensity, adjustValues)
    }
    val photoEffects = remember(filterPreset, filterIntensity, adjustValues) {
        PhotoColorEngine.effects(filterPreset, filterIntensity, adjustValues)
    }

    val selected = freeItems.firstOrNull { it.id == selectedId }

    fun buildProject() = FreestyleProject(
        aspectRatioWidth = aspect.widthRatio,
        aspectRatioHeight = aspect.heightRatio,
        backgroundColorLong = bgColor.toArgb().toLong() and 0xFFFFFFFFL,
        backgroundGradientColors = bgGradient,
        backgroundPhotoUri = bgPhoto,
        backgroundBlur = bgBlur / 100f,
        backgroundTexture = bgTexture,
        filterPreset = filterPreset,
        filterIntensity = filterIntensity,
        adjustValues = adjustValues,
        items = freeItems,
        drawStrokes = strokes
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Freestyle Studio", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { showLayers = true }) { Icon(Icons.Default.Layers, "Layers") }
                    Button(
                        onClick = { showExport = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Save") }
                }
            )
        },
        bottomBar = {
            Column(Modifier.background(Color.White)) {
                Box(
                    Modifier.fillMaxWidth().height(240.dp).background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (tool) {
                            // ------------------------------------------------ RATIO
                            FsTool.RATIO -> {
                                FsLabel("Canvas ratio")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(ratios) { r ->
                                        val sel = aspect.displayName == r.displayName
                                        Box(
                                            Modifier.width(76.dp).height(56.dp).clip(RoundedCornerShape(10.dp))
                                                .background(if (sel) Color(0xFFEFF6FF) else Color.White)
                                                .border(if (sel) 2.dp else 1.dp, if (sel) Color(0xFF2563EB) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                                .clickable { aspect = r },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(r.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                                    color = if (sel) Color(0xFF1D4ED8) else Color(0xFF1E293B))
                                                if (r.badge.isNotEmpty()) Text(r.badge, fontSize = 9.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }
                            }
                            // ------------------------------------------------ BACKGROUND
                            FsTool.BACKGROUND -> {
                                FsLabel("Solid colors")
                                FsColorRow(if (bgGradient.isEmpty() && bgPhoto == null) bgColor else null) {
                                    bgColor = it; bgGradient = emptyList(); bgPhoto = null
                                }
                                FsLabel("Gradients")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(FsGradients) { g ->
                                        Box(
                                            Modifier.size(32.dp).clip(CircleShape)
                                                .background(Brush.linearGradient(g.map { Color(it.toInt()) }))
                                                .border(if (bgGradient == g) 2.5.dp else 1.dp, if (bgGradient == g) Color(0xFF2563EB) else Color(0xFFCBD5E1), CircleShape)
                                                .clickable { bgGradient = g; bgPhoto = null }
                                        )
                                    }
                                }
                                FsLabel("Wallpaper texture")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(BackgroundTexture.entries) { t ->
                                        FsChip(t.label, bgTexture == t) { bgTexture = t }
                                    }
                                }
                                FsLabel("Blurred photo background")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(selectedPhotos) { uri ->
                                        val sel = bgPhoto == uri
                                        Box(
                                            Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                                                .border(if (sel) 2.5.dp else 1.dp, if (sel) Color(0xFF2563EB) else Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                                .clickable { bgPhoto = if (sel) null else uri; bgGradient = emptyList() }
                                        ) { AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                    }
                                }
                                if (bgPhoto != null) FsSliderRow("Blur: ${bgBlur.toInt()}%", bgBlur, 0f..100f) { bgBlur = it }
                            }
                            // ------------------------------------------------ FRAME
                            FsTool.FRAME -> {
                                val sel = selected
                                if (sel != null && sel.kind == FreestyleItemKind.PHOTO) {
                                    FsLabel("Photo frame")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PhotoFrame.entries.forEach { f ->
                                            FsChip(f.label, sel.frame == f) { updateItem(sel.id) { it.copy(frame = f) } }
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Soft drop shadow", fontSize = 13.sp, color = Color(0xFF334155))
                                        Switch(checked = sel.shadow, onCheckedChange = { c -> updateItem(sel.id) { it.copy(shadow = c) } })
                                    }
                                    TextButton(onClick = {
                                        freeItems = freeItems.map {
                                            if (it.kind == FreestyleItemKind.PHOTO) it.copy(frame = sel.frame, shadow = sel.shadow) else it
                                        }
                                    }) { Text("Apply this frame to all photos") }
                                } else {
                                    Text("Tap a photo on the canvas to style its frame.\nDrag to move, pinch to resize, twist to rotate.",
                                        fontSize = 13.sp, color = Color(0xFF64748B))
                                }
                                OutlinedButton(onClick = { addPhotosPicker.launch() }) { Text("+ Add photos") }
                            }
                            // ------------------------------------------------ TEXT
                            FsTool.TEXT -> {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("+ Add Text", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B),
                                        modifier = Modifier.clickable { editingTextId = null; textDialogOpen = true })
                                    val st = selected?.takeIf { it.kind == FreestyleItemKind.TEXT }
                                    if (st != null) Text("Edit text", fontSize = 13.sp, color = Color(0xFF2563EB),
                                        modifier = Modifier.clickable { editingTextId = st.id; textDialogOpen = true })
                                }
                                val st = selected?.takeIf { it.kind == FreestyleItemKind.TEXT }
                                if (st != null) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(FsFonts) { f -> FsChip(f, st.fontName == f) { updateItem(st.id) { it.copy(fontName = f) } } }
                                    }
                                    FsColorRow(Color(st.colorLong.toInt())) { c -> updateItem(st.id) { it.copy(colorLong = c.toArgb().toLong() and 0xFFFFFFFFL) } }
                                    FsSliderRow("Size: ${(st.fontSizeRatio * 100).toInt()}", st.fontSizeRatio * 100f, 3f..20f) { v ->
                                        updateItem(st.id) { it.copy(fontSizeRatio = v / 100f) }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FsChip("Background pill", st.textPill) { updateItem(st.id) { it.copy(textPill = !it.textPill) } }
                                        FsChip("Drop shadow", st.textShadow) { updateItem(st.id) { it.copy(textShadow = !it.textShadow) } }
                                    }
                                } else {
                                    Text("Add text, then tap it on the canvas to change font, color, size, pill and shadow.",
                                        fontSize = 13.sp, color = Color(0xFF64748B))
                                }
                            }
                            // ------------------------------------------------ STICKERS
                            FsTool.STICKERS -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FsStickers.keys.forEach { c -> FsChip(c, stickerCategory == c) { stickerCategory = c } }
                                    FsChip("+ Photo", false) { addPhotosPicker.launch() }
                                }
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(6),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(130.dp)
                                ) {
                                    gridItems(FsStickers[stickerCategory].orEmpty()) { e ->
                                        Box(
                                            Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                                                .background(Color.White).border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                                .clickable {
                                                    val n = newFreestyleEmoji(e)
                                                    freeItems = freeItems + n
                                                    selectedId = n.id
                                                },
                                            contentAlignment = Alignment.Center
                                        ) { Text(e, fontSize = 22.sp) }
                                    }
                                }
                            }
                            // ------------------------------------------------ FILTER
                            FsTool.FILTER -> FilterPickerPanel(
                                previewUri = selected?.takeIf { it.kind == FreestyleItemKind.PHOTO }?.content
                                    ?: selectedPhotos.firstOrNull(),
                                selectedPreset = filterPreset,
                                intensity = filterIntensity,
                                onPresetSelected = { p ->
                                    filterPreset = p
                                    filterIntensity = if (p == "Original") 0f else 100f
                                },
                                onIntensityChange = { filterIntensity = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            // ------------------------------------------------ ADJUST
                            FsTool.ADJUST -> {
                                val adjustDef = AdjustTools.def(adjustItem)
                                val currentValue = adjustValues[adjustItem] ?: 0f
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "$adjustItem: ${if (currentValue > 0 && adjustDef.isBipolar) "+" else ""}${currentValue.toInt()}%",
                                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                "Reset all",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF2563EB),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { adjustValues = emptyMap() }
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        FluidSlider(
                                            value = currentValue,
                                            onValueChange = { newVal ->
                                                adjustValues = adjustValues + (adjustItem to newVal)
                                            },
                                            valueRange = adjustDef.min..adjustDef.max,
                                            modifier = Modifier.fillMaxWidth().height(36.dp)
                                        )
                                    }

                                    // group tabs: Light / Color / Detail / Effects
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(AdjustTools.groups) { group ->
                                            val active = group == adjustGroup
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(if (active) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                                    .clickable {
                                                        adjustGroup = group
                                                        AdjustTools.inGroup(group).firstOrNull()?.let { adjustItem = it.name }
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = group,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (active) Color.White else Color(0xFF475569)
                                                )
                                            }
                                        }
                                    }

                                    val groupNames = remember(adjustGroup) {
                                        AdjustTools.inGroup(adjustGroup).map { it.name }
                                    }
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(adjustOptions.filter { it.name in groupNames }) { option ->
                                            val isSelected = adjustItem == option.name
                                            val isActive = (adjustValues[option.name] ?: 0f) != 0f
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
                                                    .clickable { adjustItem = option.name },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = option.icon,
                                                        contentDescription = option.name,
                                                        tint = adjustIconTints[option.name]
                                                            ?: if (isSelected) Color(0xFF2563EB) else Color(0xFF475569),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = option.name,
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155),
                                                        maxLines = 1
                                                    )
                                                }
                                                if (isActive) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(6.dp)
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF2563EB))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // ------------------------------------------------ DRAW
                            FsTool.DRAW -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FsChip("Pen", !eraser) { eraser = false }
                                    FsChip("Eraser", eraser) { eraser = true }
                                    FsChip("Undo", false) { if (strokes.isNotEmpty()) strokes = strokes.dropLast(1) }
                                    FsChip("Clear", false) { strokes = emptyList() }
                                }
                                FsSliderRow("Brush size: ${brushSize.toInt()}", brushSize, 2f..40f) { brushSize = it }
                                FsSliderRow("Opacity: ${brushOpacity.toInt()}%", brushOpacity, 10f..100f) { brushOpacity = it }
                                FsColorRow(brushColor) { brushColor = it; eraser = false }
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FsTool.entries.forEach { t ->
                        val sel = tool == t
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (sel) Color(0xFFEFF6FF) else Color.Transparent)
                                .clickable { tool = t }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(t.icon, null, tint = if (sel) Color(0xFF2563EB) else Color(0xFF64748B), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(2.dp))
                            Text(t.label, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                color = if (sel) Color(0xFF2563EB) else Color(0xFF64748B))
                        }
                    }
                }
            }
        }
    ) { inner ->
        BoxWithConstraints(
            Modifier.fillMaxSize().background(Color(0xFFE2E8F0)).padding(inner).padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val r = aspect.widthRatio / aspect.heightRatio
            val cw = minOf(maxWidth, maxHeight * r)
            val ch = cw / r
            val W = canvasSize.width.toFloat()
            val H = canvasSize.height.toFloat()

            Box(
                Modifier
                    .size(cw, ch)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(tool) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()

                            // ------------------------- freehand drawing / eraser
                            if (tool == FsTool.DRAW) {
                                fun norm(o: Offset) = Offset(o.x / w, o.y / h)
                                fun eraseAt(p: Offset) {
                                    val n = norm(p)
                                    strokes = strokes.filterNot { s ->
                                        s.points.any { hypot((it.x - n.x) * w, (it.y - n.y) * h) < 0.05f * w }
                                    }
                                }
                                if (eraser) eraseAt(down.position) else currentStroke = listOf(norm(down.position))
                                down.consume()
                                var up = false
                                while (!up) {
                                    val c = awaitPointerEvent().changes.firstOrNull() ?: break
                                    if (c.pressed) {
                                        if (eraser) eraseAt(c.position) else currentStroke = currentStroke + norm(c.position)
                                        c.consume()
                                    } else up = true
                                }
                                if (!eraser && currentStroke.isNotEmpty()) {
                                    strokes = strokes + NormalizedDrawStroke(
                                        points = currentStroke.map { NormalizedPoint(it.x, it.y) },
                                        colorLong = brushColor.copy(alpha = brushOpacity / 100f).toArgb().toLong() and 0xFFFFFFFFL,
                                        strokeWidthRatio = brushSize / 1000f
                                    )
                                }
                                currentStroke = emptyList()
                                return@awaitEachGesture
                            }

                            // ------------------------- handles of the selected item
                            val margin = 16.dp.toPx()
                            val hitR = 26.dp.toPx()
                            val sel = freeItems.firstOrNull { it.id == selectedId }
                            if (sel != null) {
                                val del = fsHandlePos(sel, w, h, -1f, margin)
                                val scl = fsHandlePos(sel, w, h, 1f, margin)
                                if ((down.position - del).getDistance() < hitR) {
                                    down.consume()
                                    val up = waitForUpOrCancellation()
                                    if (up != null) {
                                        freeItems = freeItems.filter { it.id != sel.id }
                                        selectedId = null
                                    }
                                    return@awaitEachGesture
                                }
                                if ((down.position - scl).getDistance() < hitR) {
                                    down.consume()
                                    val center = Offset(sel.x * w, sel.y * h)
                                    val v0 = down.position - center
                                    val d0 = v0.getDistance().coerceAtLeast(1f)
                                    val a0 = atan2(v0.y, v0.x)
                                    drag(down.id) { change ->
                                        val v = change.position - center
                                        val ns = (sel.scale * v.getDistance() / d0).coerceIn(0.15f, 8f)
                                        val dr = (atan2(v.y, v.x) - a0) * 180f / PI.toFloat()
                                        updateItem(sel.id) { it.copy(scale = ns, rotation = sel.rotation + dr) }
                                        change.consume()
                                    }
                                    return@awaitEachGesture
                                }
                            }

                            // ------------------------- pick an item (top-most first)
                            val hit = freeItems.lastOrNull { fsHitTest(it, down.position, w, h) }
                            if (hit == null) {
                                selectedId = null
                                return@awaitEachGesture
                            }
                            selectedId = hit.id
                            // tapping brings it to the front
                            val cur = freeItems
                            freeItems = cur.filter { it.id != hit.id } + cur.first { it.id == hit.id }
                            down.consume()

                            // ------------------------- 1-finger drag, 2-finger pinch + rotate
                            do {
                                val ev = awaitPointerEvent()
                                val zoom = ev.calculateZoom()
                                val rot = ev.calculateRotation()
                                val pan = ev.calculatePan()
                                if (zoom != 1f || rot != 0f || pan != Offset.Zero) {
                                    updateItem(hit.id) {
                                        it.copy(
                                            x = it.x + pan.x / w,
                                            y = it.y + pan.y / h,
                                            scale = (it.scale * zoom).coerceIn(0.15f, 8f),
                                            rotation = it.rotation + rot
                                        )
                                    }
                                    ev.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            } while (ev.changes.any { it.pressed })
                        }
                    }
            ) {
                // ---------------- clipped paper
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                    // background
                    when {
                        bgPhoto != null -> AsyncImage(
                            model = bgPhoto, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                                .blur(with(density) { (bgBlur / 100f * 0.05f * W).toDp() }, BlurredEdgeTreatment.Rectangle)
                        )
                        bgGradient.size >= 2 -> Box(Modifier.fillMaxSize().background(Brush.linearGradient(bgGradient.map { Color(it.toInt()) })))
                        else -> Box(Modifier.fillMaxSize().background(bgColor))
                    }
                    Canvas(Modifier.fillMaxSize()) { fsDrawTexture(bgTexture) }

                    // items in z-order
                    if (W > 0f) {
                        freeItems.forEach { item ->
                            key(item.id) {
                                FsItemView(
                                    item = item, canvasW = W, canvasH = H,
                                    selected = item.id == selectedId && tool != FsTool.DRAW,
                                    matrix = photoMatrix, effects = photoEffects,
                                    onAspect = { id, a ->
                                        val old = freeItems.firstOrNull { it.id == id }?.aspect ?: a
                                        if (abs(old - a) > 0.01f) updateItem(id) { it.copy(aspect = a) }
                                    }
                                )
                            }
                        }
                    }

                    // doodles on top
                    Canvas(Modifier.fillMaxSize()) {
                        strokes.forEach { s ->
                            if (!s.isEraser) fsDrawStroke(s.points.map { Offset(it.x, it.y) }, Color(s.colorLong.toInt()), s.strokeWidthRatio)
                        }
                        if (!eraser && currentStroke.isNotEmpty()) {
                            fsDrawStroke(currentStroke, brushColor.copy(alpha = brushOpacity / 100f), brushSize / 1000f)
                        }
                    }
                }

                // ---------------- transform handles (outside the clip so they stay visible)
                val s = selected
                if (s != null && W > 0f && tool != FsTool.DRAW) {
                    val m = with(density) { 16.dp.toPx() }
                    FsHandle(fsHandlePos(s, W, H, -1f, m), Color(0xFFEF4444), Icons.Default.Close, "Delete")
                    FsHandle(fsHandlePos(s, W, H, 1f, m), Color(0xFF2563EB), Icons.Default.OpenInFull, "Scale and rotate")
                }
            }
        }
    }

    // =============================================================== layers sheet
    if (showLayers) {
        ModalBottomSheet(onDismissRequest = { showLayers = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text("Layers (top = front)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (freeItems.isEmpty()) Text("Nothing on the canvas yet.", color = Color(0xFF64748B))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(freeItems.asReversed(), key = { it.id }) { it2 ->
                        Row(
                            Modifier.fillMaxWidth()
                                .background(if (it2.id == selectedId) Color(0xFFEFF6FF) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedId = it2.id }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                                when (it2.kind) {
                                    FreestyleItemKind.PHOTO -> AsyncImage(it2.content, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    FreestyleItemKind.EMOJI -> Text(it2.content, fontSize = 20.sp)
                                    FreestyleItemKind.TEXT -> Text("Tt", fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                when (it2.kind) {
                                    FreestyleItemKind.PHOTO -> "Photo"
                                    FreestyleItemKind.EMOJI -> "Sticker"
                                    FreestyleItemKind.TEXT -> it2.content.take(16)
                                },
                                modifier = Modifier.weight(1f), fontSize = 14.sp
                            )
                            IconButton(onClick = { moveItem(it2.id, 1) }) { Icon(Icons.Default.KeyboardArrowUp, "Bring forward") }
                            IconButton(onClick = { moveItem(it2.id, -1) }) { Icon(Icons.Default.KeyboardArrowDown, "Send backward") }
                            IconButton(onClick = {
                                freeItems = freeItems.filter { f -> f.id != it2.id }
                                if (selectedId == it2.id) selectedId = null
                            }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF4444)) }
                        }
                    }
                }
            }
        }
    }

    // =============================================================== add / edit text
    if (textDialogOpen) {
        val eid = editingTextId
        var temp by remember(eid) { mutableStateOf(freeItems.firstOrNull { it.id == eid }?.content ?: "") }
        AlertDialog(
            onDismissRequest = { textDialogOpen = false },
            title = { Text(if (eid == null) "Add text" else "Edit text") },
            text = { OutlinedTextField(value = temp, onValueChange = { temp = it }, singleLine = true, label = { Text("Text") }) },
            confirmButton = {
                Button(onClick = {
                    if (temp.isNotBlank()) {
                        if (eid != null) updateItem(eid) { it.copy(content = temp) }
                        else {
                            val n = newFreestyleText(temp)
                            freeItems = freeItems + n
                            selectedId = n.id
                        }
                    }
                    textDialogOpen = false
                }) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { textDialogOpen = false }) { Text("Cancel") } }
        )
    }

    // =============================================================== unified export screen
    if (showExport) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showExport = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val exportProject = buildProject()
            UnifiedExportScreen(
                previewAspect = exportProject.aspectRatioWidth / exportProject.aspectRatioHeight,
                shareFallbackUri = exportProject.items.firstOrNull { it.kind == FreestyleItemKind.PHOTO }?.content ?: "",
                onBack = { showExport = false },
                onExport = { longSide, format, onResult ->
                    selectedId = null
                    exporter.export(buildProject(), longSide, format) { result -> onResult(result) }
                }
            ) {
                // before saving: lightweight layout preview (photo positions); after saving: the real exported file
                androidx.compose.foundation.layout.BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color(exportProject.backgroundColorLong.toInt()))
                ) {
                    val cw = maxWidth
                    val ch = maxHeight
                    exportProject.items.filter { it.kind == FreestyleItemKind.PHOTO }.forEach { it ->
                        val iw = cw * it.widthUnits
                        val ih = cw * it.heightUnits
                        coil3.compose.AsyncImage(
                            model = it.content,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.TopStart)
                                .offset(x = cw * it.x - iw / 2, y = ch * it.y - ih / 2)
                                .size(iw, ih)
                                .graphicsLayer(rotationZ = it.rotation, scaleX = it.scale, scaleY = it.scale)
                        )
                    }
                }
            }
        }
    }

    // =============================================================== export dialog (legacy, switched off)
    if (showExport && USE_LEGACY_EXPORT_DIALOG) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showExport = false },
            title = { Text("Save collage") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FsLabel("Quality")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("HD" to 1080, "2K" to 2048, "4K" to 3840).forEach { (n, px) ->
                            FsChip(n, exportQuality == px) { exportQuality = px }
                        }
                    }
                    FsLabel("Format")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("JPG", "PNG").forEach { f -> FsChip(f, exportFormat == f) { exportFormat = f } }
                    }
                    if (isSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(enabled = !isSaving, onClick = {
                    isSaving = true
                    selectedId = null
                    exporter.export(buildProject(), exportQuality, exportFormat) { result ->
                        isSaving = false
                        showExport = false
                        scope.launch {
                            snackbar.showSnackbar(
                                result.fold(
                                    onSuccess = { "Saved to $it" },
                                    onFailure = { "Export failed: ${it.message ?: "unknown error"}" }
                                )
                            )
                        }
                    }
                }) { Text(if (isSaving) "Saving…" else "Save") }
            },
            dismissButton = { TextButton(enabled = !isSaving, onClick = { showExport = false }) { Text("Cancel") } }
        )
    }
}