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
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
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
import com.oem.collagekmpapp.AdjustTools
import com.oem.collagekmpapp.FilterPickerPanel
import com.oem.collagekmpapp.OverlayMetrics
import com.oem.collagekmpapp.UndoRedoActions
import com.oem.collagekmpapp.components.FluidSlider
import com.oem.collagekmpapp.rememberGalleryPicker
import com.oem.collagekmpapp.rememberPhotoRequest
import com.oem.collagekmpapp.model.*
import com.oem.collagekmpapp.rememberEditHistory

/** LayoutRepository provides layouts for 1..50 photos. */
private const val MAX_COLLAGE_PHOTOS = 50

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

    data class Text(
        override val id: String,
        val text: String,
        val font: String = "Modern",
        val textColor: Color = Color.White,
        override val offset: Offset = Offset.Zero,
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : OverlayItem()
}

data class AdjustOption(
    val name: String,
    val icon: ImageVector
)

/** Everything Undo / Redo restores in the collage editor (immutable value, compared by equality). */
private data class CollageSnap(
    val photos: List<String>,
    val layoutId: String,
    val slots: List<com.oem.collagekmpapp.model.Slot>,
    val aspectRatio: AspectRatio,
    val border: BorderSettings,
    val transforms: Map<Int, PhotoTransform>,
    val adjust: Map<String, Float>,
    val preset: String,
    val intensity: Float,
    val glitterDensity: Float,
    val glitterCard: String,
    val font: String,
    val text: String,
    val textColor: Color,
    val textOffset: Offset,
    val bgColor: Color,
    val bgGradient: Brush?,
    val bgGradientColors: List<Color>,
    val bgFade: Float,
    val bgPhoto: String?,
    val strokes: List<DrawStroke>,
    val overlays: List<OverlayItem>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageEditorScreen(
    layoutId: String,
    selectedPhotos: List<String>,
    onBack: () -> Unit,
    /** Optional themed template: its gradient, spacing, filter, text and decorations are applied up front. */
    templateId: String? = null
) {
    // photos currently shown in the grid boxes (starts as the picked photos; "Replace photo" swaps entries)
    var currentPhotos by remember(selectedPhotos) { mutableStateOf(selectedPhotos) }
    val template = remember(templateId) { com.oem.collagekmpapp.data.TemplateRepository.byId(templateId) }
    var selectedTab by remember(currentPhotos.size) {
        mutableStateOf(if (currentPhotos.size <= 1) EditorTool.RATIO else EditorTool.LAYOUT)
    }
    var currentLayoutId by remember { mutableStateOf(layoutId) }

    val layoutRepo = remember { com.oem.collagekmpapp.data.LayoutRepository() }
    val currentLayout = remember(currentLayoutId, currentPhotos.size) {
        layoutRepo.getAllLayouts().find { it.id == currentLayoutId }
            ?: layoutRepo.getLayouts(currentPhotos.size.coerceAtLeast(1)).firstOrNull()
            ?: com.oem.collagekmpapp.model.CollageLayout(currentLayoutId, "Default", currentPhotos.size, "", listOf(com.oem.collagekmpapp.model.Slot(0f, 0f, 1f, 1f)))
    }

    var currentSlots by remember(currentLayoutId) { mutableStateOf(currentLayout.slots) }
    var selectedSlotIndex by remember(currentLayoutId) { mutableStateOf<Int?>(null) }
    var aspectRatio by remember {
        mutableStateOf(
            template?.let { t ->
                AspectRatio.entries.firstOrNull { it.widthRatio == t.preset.aspectRatioWidth && it.heightRatio == t.preset.aspectRatioHeight }
            } ?: AspectRatio.SQUARE
        )
    }
    var borderSettings by remember {
        mutableStateOf(
            if (template != null) BorderSettings(
                outerSpacing = template.preset.outerSpacingRatio * 360f,
                cornerRadius = template.preset.cornerRadiusRatio * 360f,
                spacing = template.preset.innerSpacingRatio * 360f
            ) else BorderSettings(outerSpacing = 12f, cornerRadius = 22f, spacing = 8f)
        )
    }
    var photoTransforms by remember { mutableStateOf<Map<Int, PhotoTransform>>(emptyMap()) }
    var canvasSizePx by remember { mutableStateOf(IntSize.Zero) }

    // ---- Preview/export parity helpers -------------------------------------------------------
    // Overlay sizes are defined against a 360-unit reference canvas width (OverlayMetrics) and
    // scaled to the live canvas, so the preview and the exported bitmap use the same ratios.
    val uiDensity = LocalDensity.current
    val refUnitPx = if (canvasSizePx.width > 0) canvasSizePx.width / OverlayMetrics.REF_WIDTH else uiDensity.density
    val refDp: (Float) -> androidx.compose.ui.unit.Dp = { v -> with(uiDensity) { (v * refUnitPx).toDp() } }
    val refSp: (Float) -> androidx.compose.ui.unit.TextUnit = { v -> with(uiDensity) { (v * refUnitPx).toSp() } }
    val overlayTextStyle: (String, Color) -> TextStyle = { font, col ->
        val size = refSp(OverlayMetrics.textFont(font))
        when (font) {
            "Script" -> TextStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = size, color = col)
            "Bold" -> TextStyle(fontWeight = FontWeight.Bold, fontSize = size, color = col)
            "Retro" -> TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = size, color = col)
            "Serif" -> TextStyle(fontFamily = FontFamily.Serif, fontSize = size, color = col)
            "Neon" -> TextStyle(fontWeight = FontWeight.Bold, fontSize = size, color = col,
                shadow = Shadow(color = col, blurRadius = OverlayMetrics.NEON_BLUR * refUnitPx))
            else -> TextStyle(fontWeight = FontWeight.Medium, fontSize = size, color = col)
        }
    }
    var editingTextId by remember { mutableStateOf<String?>(null) }

    // Filter & Glitter states
    var filterIntensity by remember { mutableStateOf(template?.preset?.filterIntensity ?: 0f) }
    var selectedPreset by remember { mutableStateOf(template?.preset?.filterPreset ?: "Original") }
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
    var selectedAdjustGroup by remember { mutableStateOf("Light") }
    var adjustValues by remember { mutableStateOf((mapOf(
        "Brightness" to 0f,
        "Contrast" to 0f,
        "Highlight" to 0f,
        "Shadow" to 0f,
        "Saturation" to 0f,
        "Warmth" to 0f,
        "Tint" to 0f,
        "Fade" to 0f
    ) + (template?.preset?.adjustValues ?: emptyMap()))) }

    // Text typography states with custom color list
    var selectedFont by remember { mutableStateOf("Script") }
    var collageText by remember { mutableStateOf("") }
    var textColor by remember { mutableStateOf(Color.White) }
    var textOffset by remember { mutableStateOf(Offset.Zero) }
    var showAddTextDialog by remember { mutableStateOf(false) }

    // Background settings states matching 09_background_settings_screen.svg
    var canvasBgColor by remember { mutableStateOf<Color>(template?.let { Color(it.preset.backgroundColorLong.toInt()) } ?: Color.White) }
    var canvasBgGradient by remember {
        mutableStateOf<Brush?>(
            template?.preset?.backgroundGradientColors?.takeIf { it.size >= 2 }
                ?.let { cols -> Brush.linearGradient(cols.map { Color(it.toInt()) }) }
        )
    }
    var canvasBgGradientColors by remember { mutableStateOf(template?.preset?.backgroundGradientColors?.map { Color(it.toInt()) } ?: emptyList<Color>()) }
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

    // Template decorations (emoji stickers + text) are stored normalised; convert them to canvas-pixel
    // overlay items once the canvas has been measured, so they stay movable/editable like any sticker.
    var templateDecorApplied by remember { mutableStateOf(false) }
    LaunchedEffect(canvasSizePx, template) {
        val t = template
        if (t != null && !templateDecorApplied && canvasSizePx.width > 0 && canvasSizePx.height > 0) {
            templateDecorApplied = true
            val cw = canvasSizePx.width.toFloat()
            val ch = canvasSizePx.height.toFloat()
            val decor = t.preset.overlays.mapNotNull { o ->
                (o as? com.oem.collagekmpapp.model.ProjectOverlayItem.Emoji)?.let {
                    OverlayItem.Emoji(
                        id = "tpl_emoji_${kotlin.random.Random.nextLong().toULong()}",
                        emoji = it.emoji,
                        offset = Offset((it.x - 0.5f) * cw, (it.y - 0.5f) * ch),
                        rotation = it.rotation,
                        scale = it.scale
                    )
                }
            }
            val texts = t.preset.textLayers.map {
                OverlayItem.Text(
                    id = "tpl_text_${kotlin.random.Random.nextLong().toULong()}",
                    text = it.text,
                    font = it.fontName,
                    textColor = Color(it.colorLong.toInt()),
                    offset = Offset((it.x - 0.5f) * cw, (it.y - 0.5f) * ch),
                    rotation = it.rotation,
                    scale = it.scale
                )
            }
            overlayItems = overlayItems + decor + texts
        }
    }

    // Replace / swap the photo of one grid box via the device gallery
    var replaceIndex by remember { mutableStateOf<Int?>(null) }
    val replacePhotoPicker = rememberGalleryPicker { pickedUris ->
        val idx = replaceIndex
        val uri = pickedUris.firstOrNull()
        if (idx != null && uri != null && idx in currentPhotos.indices) {
            currentPhotos = currentPhotos.toMutableList().also { it[idx] = uri }
            photoTransforms = photoTransforms - idx      // new picture starts with a fresh pan / zoom
        }
        replaceIndex = null
    }

    // ---------------------------------------------------------------- Photo strip (thumbnail bar)
    // Every operation below only touches currentPhotos / layout id / photoTransforms, which are all part of
    // CollageSnap, so each one is a single Undo step. Layout transformations (resized boxes, ratio, border,
    // filters, stickers ...) are never touched, except that a changed photo COUNT needs a layout with that many boxes.
    var showPhotoStrip by remember { mutableStateOf(true) }

    fun relayoutFor(n: Int) {
        layoutRepo.getLayouts(n.coerceAtLeast(1)).firstOrNull()?.let { currentLayoutId = it.id }
    }

    val addPhotoPicker = rememberGalleryPicker { uris ->
        val room = (MAX_COLLAGE_PHOTOS - currentPhotos.size).coerceAtLeast(0)
        val add = uris.take(room)
        if (add.isNotEmpty()) {
            currentPhotos = currentPhotos + add
            relayoutFor(currentPhotos.size)
        }
    }

    /** Moves the photo at [from] to [to]; its pan / zoom travels with it. */
    fun movePhoto(from: Int, to: Int) {
        if (from !in currentPhotos.indices || to !in currentPhotos.indices || from == to) return
        val list = currentPhotos.toMutableList()
        list.add(to, list.removeAt(from))
        val remapped = HashMap<Int, PhotoTransform>()
        for (i in list.indices) {
            val src = when {
                i == to -> from
                from < to && i in from until to -> i + 1
                from > to && i in (to + 1)..from -> i - 1
                else -> i
            }
            photoTransforms[src]?.let { remapped[i] = it }
        }
        currentPhotos = list
        photoTransforms = remapped
        selectedSlotIndex = to
    }

    fun deletePhoto(idx: Int) {
        if (currentPhotos.size <= 1 || idx !in currentPhotos.indices) return
        val list = currentPhotos.toMutableList().also { it.removeAt(idx) }
        val remapped = HashMap<Int, PhotoTransform>()
        photoTransforms.forEach { (k, v) -> if (k < idx) remapped[k] = v else if (k > idx) remapped[k - 1] = v }
        currentPhotos = list
        photoTransforms = remapped
        relayoutFor(list.size)
    }

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

    val count = currentPhotos.size.coerceAtLeast(2)

    val layouts = remember(count) {
        com.oem.collagekmpapp.data.LayoutRepository().getLayouts(count).map {
            LayoutOption(it.id, it.name, it.slotCount, it.description)
        }
    }

    val glitterCards = listOf("Glitz", "Spark 1", "Flare 2", "Dust", "Diamond Glow", "Heart Sparkle", "Golden Shimmer")
    val fontCards = listOf("Modern", "Script", "Bold", "Retro", "Serif", "Neon")

    val adjustOptions = listOf(
        // Light
        AdjustOption("Brightness", Icons.Default.WbSunny),
        AdjustOption("Contrast", Icons.Default.Contrast),
        AdjustOption("Exposure", Icons.Default.Exposure),
        AdjustOption("Highlight", Icons.Default.LightMode),
        AdjustOption("Shadow", Icons.Default.Brightness2),
        AdjustOption("Whites", Icons.Default.Brightness7),
        AdjustOption("Blacks", Icons.Default.Brightness3),
        AdjustOption("Dehaze", Icons.Default.FilterDrama),
        // Color
        AdjustOption("Saturation", Icons.Default.Palette),
        AdjustOption("Hue", Icons.Default.ColorLens),
        AdjustOption("Warmth", Icons.Default.WbIncandescent),
        AdjustOption("Tint", Icons.Default.Gradient),
        AdjustOption("Red", Icons.Default.Lens),
        AdjustOption("Green", Icons.Default.Lens),
        AdjustOption("Blue", Icons.Default.Lens),
        AdjustOption("Sepia", Icons.Default.FilterVintage),
        // Detail
        AdjustOption("Clarity", Icons.Default.Hd),
        AdjustOption("Sharpen", Icons.Default.ChangeHistory),
        AdjustOption("Blur", Icons.Default.BlurLinear),
        AdjustOption("Soften", Icons.Default.Face),
        // Effects
        AdjustOption("Glow", Icons.Default.AutoAwesome),
        AdjustOption("Grain", Icons.Default.Grain),
        AdjustOption("Vignette", Icons.Default.Vignette),
        AdjustOption("Fade", Icons.Default.BlurOn)
    )
    val adjustIconTints = mapOf(
        "Red" to Color(0xFFEF4444),
        "Green" to Color(0xFF22C55E),
        "Blue" to Color(0xFF3B82F6)
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

    // One list of colour stops drives BOTH the swatch/preview brush and the exported gradient.
    val gradientColorSets: List<List<Color>> = listOf(
        listOf(Color(0xFFEF4444), Color(0xFFFACC15), Color(0xFF22C55E), Color(0xFF3B82F6)),
        listOf(Color(0xFFF43F5E), Color(0xFFFBBF24)),
        listOf(Color(0xFFFB923C), Color(0xFFF472B6), Color(0xFF38BDF8)),
        listOf(Color(0xFF34D399), Color(0xFF2563EB)),
        listOf(Color(0xFFF5D0FE), Color(0xFF93C5FD)),
        listOf(Color(0xFF0F172A), Color(0xFF7DD3FC)),
        listOf(Color(0xFFFF007F), Color(0xFFFF758C), Color(0xFFFF7EB3)),
        listOf(Color(0xFF651FFF), Color(0xFF00E5FF)),
        listOf(Color(0xFFFF9100), Color(0xFFFFEA00)),
        listOf(Color(0xFF00E676), Color(0xFF1DE9B6)),
        listOf(Color(0xFFF50057), Color(0xFFFF4081)),
        listOf(Color(0xFF3D5AFE), Color(0xFF2F80ED)),
        listOf(Color(0xFFFF3D00), Color(0xFFFF9100)),
        listOf(Color(0xFF1A237E), Color(0xFF3949AB)),
        listOf(Color(0xFF004D40), Color(0xFF00897B)),
        listOf(Color(0xFFB71C1C), Color(0xFFD32F2F)),
        listOf(Color(0xFF4A148C), Color(0xFF7B1FA2)),
        listOf(Color(0xFF880E4F), Color(0xFFC2185B)),
        listOf(Color(0xFF0D47A1), Color(0xFF1976D2)),
        listOf(Color(0xFF006064), Color(0xFF00ACC1)),
        listOf(Color(0xFF33691E), Color(0xFF689F38)),
        listOf(Color(0xFFE65100), Color(0xFFF57C00)),
        listOf(Color(0xFF3E2723), Color(0xFF5D4037)),
        listOf(Color(0xFF263238), Color(0xFF455A64)),
        listOf(Color(0xFFFF8A80), Color(0xFFFF80AB))
    )
    val gradients = gradientColorSets.map { Brush.linearGradient(it) }

    val brushTypes = listOf("Pen", "Eraser")

    // ---------------------------------------------------------------- Undo / Redo
    // Layout slots are keyed to the layout id (remember(currentLayoutId)); restoring an older layout must
    // therefore re-apply the saved slots AFTER that reset, otherwise resized boxes would be lost.
    var pendingRestoredSlots by remember { mutableStateOf<List<com.oem.collagekmpapp.model.Slot>?>(null) }
    LaunchedEffect(currentLayoutId, pendingRestoredSlots) {
        pendingRestoredSlots?.let { currentSlots = it; pendingRestoredSlots = null }
    }
    val history = rememberEditHistory(
        current = CollageSnap(
            photos = currentPhotos, layoutId = currentLayoutId, slots = currentSlots,
            aspectRatio = aspectRatio, border = borderSettings, transforms = photoTransforms,
            adjust = adjustValues, preset = selectedPreset, intensity = filterIntensity,
            glitterDensity = glitterDensity, glitterCard = selectedGlitterCard,
            font = selectedFont, text = collageText, textColor = textColor, textOffset = textOffset,
            bgColor = canvasBgColor, bgGradient = canvasBgGradient, bgGradientColors = canvasBgGradientColors,
            bgFade = backgroundFade, bgPhoto = selectedBackgroundPhoto,
            strokes = drawStrokes, overlays = overlayItems
        )
    ) { snap ->
        currentPhotos = snap.photos
        currentLayoutId = snap.layoutId
        currentSlots = snap.slots
        pendingRestoredSlots = snap.slots
        aspectRatio = snap.aspectRatio
        borderSettings = snap.border
        photoTransforms = snap.transforms
        adjustValues = snap.adjust
        selectedPreset = snap.preset
        filterIntensity = snap.intensity
        glitterDensity = snap.glitterDensity
        selectedGlitterCard = snap.glitterCard
        selectedFont = snap.font
        collageText = snap.text
        textColor = snap.textColor
        textOffset = snap.textOffset
        canvasBgColor = snap.bgColor
        canvasBgGradient = snap.bgGradient
        canvasBgGradientColors = snap.bgGradientColors
        backgroundFade = snap.bgFade
        selectedBackgroundPhoto = snap.bgPhoto
        drawStrokes = snap.strokes
        overlayItems = snap.overlays
        currentStrokePoints = emptyList()
        selectedSlotIndex = null
        if (selectedOverlayId != null && snap.overlays.none { it.id == selectedOverlayId }) selectedOverlayId = null
    }

    // ---------------------------------------------------------------- per-tool Reset
    // Defaults = the values the editor started with (so a template's own look is what "reset" returns to).
    val initialAspect = remember { aspectRatio }
    val initialBorder = remember { borderSettings }
    val initialPreset = remember { selectedPreset }
    val initialIntensity = remember { filterIntensity }
    val initialAdjust = remember { adjustValues.filterValues { it != 0f } }
    val initialBgColor = remember { canvasBgColor }
    val initialBgGradient = remember { canvasBgGradient }
    val initialBgGradientColors = remember { canvasBgGradientColors }
    val selectedTextOverlay = overlayItems.firstOrNull { it.id == selectedOverlayId } as? OverlayItem.Text
    val toolIsDirty: Boolean = when (selectedTab) {
        EditorTool.RATIO -> aspectRatio != initialAspect
        EditorTool.LAYOUT -> currentSlots != currentLayout.slots || photoTransforms.isNotEmpty()
        EditorTool.BORDER -> borderSettings != initialBorder
        EditorTool.FILTER -> selectedPreset != initialPreset || filterIntensity != initialIntensity
        EditorTool.STICKERS -> overlayItems.any { it !is OverlayItem.Text }
        EditorTool.ADJUST -> adjustValues.filterValues { it != 0f } != initialAdjust
        EditorTool.GLITTER -> glitterDensity != 0f
        EditorTool.TEXT -> selectedFont != "Script" || textColor != Color.White ||
                (selectedTextOverlay != null && (selectedTextOverlay.font != "Script" || selectedTextOverlay.textColor != Color.White))
        EditorTool.BACKGROUND -> canvasBgColor != initialBgColor || canvasBgGradientColors != initialBgGradientColors ||
                selectedBackgroundPhoto != null || backgroundFade != 0f
        EditorTool.DRAW -> drawStrokes.isNotEmpty() || selectedBrushType != "Pen" || brushDensity != 20f ||
                brushOpacity != 100f || brushColor != Color(0xFFF43F8E)
    }
    val resetTool: () -> Unit = {
        when (selectedTab) {
            EditorTool.RATIO -> aspectRatio = initialAspect
            EditorTool.LAYOUT -> { currentSlots = currentLayout.slots; photoTransforms = emptyMap(); selectedSlotIndex = null }
            EditorTool.BORDER -> borderSettings = initialBorder
            EditorTool.FILTER -> { selectedPreset = initialPreset; filterIntensity = initialIntensity }
            EditorTool.STICKERS -> {
                overlayItems = overlayItems.filter { it is OverlayItem.Text }
                if (overlayItems.none { it.id == selectedOverlayId }) selectedOverlayId = null
            }
            EditorTool.ADJUST -> adjustValues = initialAdjust
            EditorTool.GLITTER -> glitterDensity = 0f
            EditorTool.TEXT -> {
                selectedFont = "Script"; textColor = Color.White
                overlayItems = overlayItems.map {
                    if (it.id == selectedOverlayId && it is OverlayItem.Text) it.copy(font = "Script", textColor = Color.White) else it
                }
            }
            EditorTool.BACKGROUND -> {
                canvasBgColor = initialBgColor; canvasBgGradient = initialBgGradient
                canvasBgGradientColors = initialBgGradientColors; selectedBackgroundPhoto = null; backgroundFade = 0f
            }
            EditorTool.DRAW -> {
                drawStrokes = emptyList(); currentStrokePoints = emptyList(); selectedBrushType = "Pen"
                brushDensity = 20f; brushOpacity = 100f; brushColor = Color(0xFFF43F8E)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Collage Studio", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    UndoRedoActions(history)
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
                // Photo strip: view / select / add / replace / reorder / delete the photos of the collage
                CollagePhotoStrip(
                    photos = currentPhotos,
                    selectedIndex = selectedSlotIndex,
                    expanded = showPhotoStrip,
                    canAdd = currentPhotos.size < MAX_COLLAGE_PHOTOS,
                    onToggle = { showPhotoStrip = !showPhotoStrip },
                    onSelect = { selectedSlotIndex = it },
                    onAdd = { addPhotoPicker.launch() },
                    onReplace = { idx -> replaceIndex = idx; replacePhotoPicker.launch() },
                    onMove = { from, to -> movePhoto(from, to) },
                    onDelete = { deletePhoto(it) }
                )
                // Dynamic Tool Sub-Panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(262.dp)
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Column(Modifier.fillMaxSize()) {
                        // universal per-tool Reset
                        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.End) {
                            CollageResetButton(enabled = toolIsDirty, onClick = resetTool)
                        }
                        Box(Modifier.weight(1f).fillMaxWidth()) {
                            when (selectedTab) {
                                EditorTool.RATIO -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Canvas Aspect Ratio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(AspectRatio.entries.distinctBy { it.displayName }) { ratio ->
                                                val isSelected = aspectRatio.displayName == ratio.displayName
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
                                                            fontSize = 12.sp,
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
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                                        .clickable {
                                                            currentLayoutId = layout.id
                                                            val newLayout = layoutRepo.getAllLayouts().find { it.id == layout.id }
                                                                ?: layoutRepo.getLayouts(currentPhotos.size.coerceAtLeast(1)).firstOrNull()
                                                            if (newLayout != null) {
                                                                currentSlots = newLayout.slots
                                                            }
                                                            selectedSlotIndex = null
                                                        },
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
                                    FilterPickerPanel(
                                        // thumbnails use the selected slot's photo, else the first photo
                                        previewUri = selectedSlotIndex?.let { currentPhotos.getOrNull(it) }
                                            ?: currentPhotos.firstOrNull(),
                                        selectedPreset = selectedPreset,
                                        intensity = filterIntensity,
                                        onPresetSelected = { preset ->
                                            selectedPreset = preset
                                            // Instagram applies a filter at full strength; slider then dials it back
                                            filterIntensity = if (preset == "Original") 0f else 100f
                                        },
                                        onIntensityChange = { filterIntensity = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp)
                                    )
                                }
                                EditorTool.STICKERS -> {
                                    val currentStickers = if (selectedStickerCategory == "All") {
                                        stickerMap.values.flatten().distinct()
                                    } else {
                                        stickerMap[selectedStickerCategory] ?: emptyList()
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .animateContentSize()
                                            .padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Stickers", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

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
                                                        fontSize = 12.sp,
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
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                    val adjustDef = AdjustTools.def(selectedAdjustItem)
                                    val currentValue = adjustValues[selectedAdjustItem] ?: 0f
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "$selectedAdjustItem: ${if (currentValue > 0 && adjustDef.isBipolar) "+" else ""}${currentValue.toInt()}%",
                                                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)
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
                                                    adjustValues = adjustValues + (selectedAdjustItem to newVal)
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
                                                val active = group == selectedAdjustGroup
                                                Box(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(if (active) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                                        .clickable {
                                                            selectedAdjustGroup = group
                                                            AdjustTools.inGroup(group).firstOrNull()?.let { selectedAdjustItem = it.name }
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

                                        val groupNames = remember(selectedAdjustGroup) {
                                            AdjustTools.inGroup(selectedAdjustGroup).map { it.name }
                                        }
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(adjustOptions.filter { it.name in groupNames }) { option ->
                                                val isSelected = selectedAdjustItem == option.name
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
                                                            tint = adjustIconTints[option.name]
                                                                ?: if (isSelected) Color(0xFF2563EB) else Color(0xFF475569),
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
                                                    // dot = this tool is currently applied
                                                    if (isActive) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(6.dp)
                                                                .size(7.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF2563EB))
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
                                        Text("Glitters", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

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
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B),
                                                modifier = Modifier.clickable { editingTextId = null; showAddTextDialog = true }
                                            )
                                            val selectedTextItem = overlayItems.firstOrNull { it.id == selectedOverlayId } as? OverlayItem.Text
                                            if (selectedTextItem != null) {
                                                Text(
                                                    text = "Edit text",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF2563EB),
                                                    modifier = Modifier.clickable { editingTextId = selectedTextItem.id; showAddTextDialog = true }
                                                )
                                            }
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
                                                        .clickable {
                                                            textColor = col
                                                            overlayItems = overlayItems.map {
                                                                if (it.id == selectedOverlayId && it is OverlayItem.Text) it.copy(textColor = col) else it
                                                            }
                                                        }
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
                                                            color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1),
                                                            shape = RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable {
                                                            selectedFont = font
                                                            overlayItems = overlayItems.map {
                                                                if (it.id == selectedOverlayId && it is OverlayItem.Text) it.copy(font = font) else it
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = font,
                                                        fontSize = 12.sp,
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
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("Background", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                        Text("Solid Colors", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            itemsIndexed(gradients) { gIndex, grad ->
                                                val gSelected = selectedBackgroundPhoto == null && canvasBgGradientColors == gradientColorSets[gIndex]
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(grad)
                                                        .border(
                                                            width = if (gSelected) 2.5.dp else 1.dp,
                                                            color = if (gSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                            shape = CircleShape
                                                        )
                                                        .clickable {
                                                            canvasBgGradient = grad
                                                            canvasBgGradientColors = gradientColorSets[gIndex]
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
                                            items(currentPhotos) { photoUri ->
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
                                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Draw & Brush", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                            TextButton(onClick = { if (drawStrokes.isNotEmpty()) drawStrokes = drawStrokes.dropLast(1) }) {
                                                Text("Undo")
                                            }
                                            TextButton(onClick = { drawStrokes = emptyList() }) {
                                                Text("Clear")
                                            }
                                        }

                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text("Brush size: ${brushDensity.toInt()}%", fontSize = 11.sp, color = Color(0xFF334155))
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
                    }
                }

                // Horizontal scrollable bottom tabs list with ALL 10 tools
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tools = remember(currentPhotos.size) {
                        val baseTools = listOf(
                            Triple(EditorTool.RATIO, "Ratio", Icons.Default.Crop),
                            Triple(EditorTool.LAYOUT, "Layout", Icons.Default.GridView),
                            Triple(EditorTool.BORDER, "Border", Icons.Default.Dashboard),
                            Triple(EditorTool.FILTER, "Filter", Icons.Default.Star),
                            Triple(EditorTool.STICKERS, "Stickers", Icons.Default.EmojiEmotions),
                            Triple(EditorTool.ADJUST, "Adjust", Icons.Default.Tune),
                            Triple(EditorTool.TEXT, "Text", null),
                            Triple(EditorTool.BACKGROUND, "Background", Icons.Default.Wallpaper),
                            Triple(EditorTool.DRAW, "Draw", Icons.Default.Edit)
                        )
                        if (currentPhotos.size <= 1) {
                            baseTools.filter { it.first != EditorTool.LAYOUT }
                        } else {
                            baseTools
                        }
                    }

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
                                    modifier = Modifier.size(18.dp)
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

            val fadeVal = 1f - ((adjustValues["Fade"] ?: 0f) + 50f) / 100f * 0.3f

            // Collage Canvas Box with FunPic-style Layout Grid Thumbnails and Full Editing Suite
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(aspectRatio.widthRatio / aspectRatio.heightRatio)
                    .clipToBounds()
                    .then(bgModifier)
                    .onGloballyPositioned { coordinates ->
                        canvasSizePx = coordinates.size
                    }
                    .pointerInput(selectedTab, selectedBrushType, brushColor, brushDensity, brushOpacity, refUnitPx) {
                        if (selectedTab == EditorTool.DRAW)
                        {
                            val eraserR = 0.04f * size.width
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStrokePoints = listOf(offset)
                                    if (selectedBrushType == "Eraser") {
                                        drawStrokes = drawStrokes.filter { stroke ->
                                            stroke.points.none { sp ->
                                                val dx = sp.x - offset.x
                                                val dy = sp.y - offset.y
                                                (dx * dx + dy * dy) < (eraserR * eraserR)
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
                                                (dx * dx + dy * dy) < (eraserR * eraserR)
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (currentStrokePoints.isNotEmpty()) {
                                        val isEraser = selectedBrushType == "Eraser"
                                        if (!isEraser) {
                                            val strokeWidth = OverlayMetrics.BRUSH_BASE * (brushDensity / 50f) * refUnitPx
                                            drawStrokes = drawStrokes + DrawStroke(
                                                points = currentStrokePoints,
                                                // opacity is baked in so preview == export per stroke
                                                color = brushColor.copy(alpha = brushColor.alpha * brushOpacity / 100f),
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
                        model = rememberPhotoRequest(selectedBackgroundPhoto, 1024),
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
                        photos = currentPhotos,
                        spacing = borderSettings.spacing.dp,
                        cornerRadius = borderSettings.cornerRadius.dp,
                        filterPreset = selectedPreset,
                        filterIntensity = filterIntensity,
                        glitterCard = selectedGlitterCard,
                        glitterDensity = glitterDensity,
                        adjustValues = adjustValues,
                        isDrawingMode = (selectedTab == EditorTool.DRAW),
                        slots = currentSlots,
                        selectedSlotIndex = selectedSlotIndex,
                        onSelectSlot = { selectedSlotIndex = it },
                        onSlotsChanged = { currentSlots = it },
                        onReplacePhoto = { idx ->
                            replaceIndex = idx
                            replacePhotoPicker.launch()
                        },
                        canvasWidthPx = canvasSizePx.width.toFloat(),
                        photoTransforms = photoTransforms,
                        onPhotoTransformChanged = { idx, trans ->
                            photoTransforms = photoTransforms + (idx to trans)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
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
                        val strokeWidth = OverlayMetrics.BRUSH_BASE * (brushDensity / 50f) * refUnitPx
                        drawPath(
                            path = path,
                            color = brushColor.copy(alpha = brushColor.alpha * brushOpacity / 100f),
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
                    // read straight from the item (single source of truth) so Undo / Redo can restore it
                    val itemOffset = item.offset
                    val itemRotation = item.rotation
                    val itemScale = item.scale
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
                                            overlayItems = overlayItems.map {
                                                if (it.id == item.id) {
                                                    when (it) {
                                                        is OverlayItem.Emoji -> it.copy(offset = it.offset + dragAmount)
                                                        is OverlayItem.Photo -> it.copy(offset = it.offset + dragAmount)
                                                        is OverlayItem.Text -> it.copy(offset = it.offset + dragAmount)
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
                                .background(Color.Black.copy(alpha = OverlayMetrics.PILL_ALPHA), RoundedCornerShape(refDp(OverlayMetrics.PILL_RADIUS)))
                                .padding(refDp(OverlayMetrics.PILL_PAD))
                        ) {
                            when (item) {
                                is OverlayItem.Emoji -> {
                                    Text(text = item.emoji, fontSize = refSp(OverlayMetrics.EMOJI_FONT))
                                }
                                is OverlayItem.Photo -> {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(refDp(OverlayMetrics.PHOTO_SIZE)).clip(RoundedCornerShape(refDp(OverlayMetrics.PHOTO_RADIUS)))
                                    )
                                }
                                is OverlayItem.Text -> {
                                    val fontStyle = overlayTextStyle(item.font, item.textColor)
                                    Text(text = item.text, style = fontStyle)
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
                                        val newRotation = ((overlayItems.firstOrNull { it.id == item.id }?.rotation ?: itemRotation) + 15f) % 360f
                                        overlayItems = overlayItems.map {
                                            if (it.id == item.id) {
                                                when (it) {
                                                    is OverlayItem.Emoji -> it.copy(rotation = newRotation)
                                                    is OverlayItem.Photo -> it.copy(rotation = newRotation)
                                                    is OverlayItem.Text -> it.copy(rotation = newRotation)
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
                                            val newScale = ((overlayItems.firstOrNull { it.id == item.id }?.scale ?: itemScale) + scaleDelta).coerceIn(0.4f, 3.5f)
                                            overlayItems = overlayItems.map {
                                                if (it.id == item.id) {
                                                    when (it) {
                                                        is OverlayItem.Emoji -> it.copy(scale = newScale)
                                                        is OverlayItem.Photo -> it.copy(scale = newScale)
                                                        is OverlayItem.Text -> it.copy(scale = newScale)
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
            val editingItem = overlayItems.firstOrNull { it.id == editingTextId } as? OverlayItem.Text
            var tempText by remember(editingTextId) { mutableStateOf(editingItem?.text ?: "") }
            AlertDialog(
                onDismissRequest = { showAddTextDialog = false; editingTextId = null },
                title = { Text(if (editingItem != null) "Edit Text" else "Add Text") },
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
                        if (tempText.isNotBlank()) {
                            if (editingItem != null) {
                                overlayItems = overlayItems.map {
                                    if (it.id == editingItem.id && it is OverlayItem.Text) it.copy(text = tempText) else it
                                }
                            } else {
                                // Text is a regular movable overlay (z-ordered with stickers) and is
                                // exported with its real font, colour and size ratio.
                                val textOverlay = OverlayItem.Text(
                                    id = "text_${kotlin.random.Random.nextLong().toULong()}",
                                    text = tempText,
                                    font = selectedFont,
                                    textColor = textColor,
                                    offset = Offset.Zero
                                )
                                overlayItems = overlayItems + textOverlay
                                selectedOverlayId = textOverlay.id
                            }
                        }
                        showAddTextDialog = false
                        editingTextId = null
                    }) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTextDialog = false; editingTextId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showExportDialog) {
            val density = uiDensity
            val canvasWidthDp = if (canvasSizePx.width > 0) with(density) { canvasSizePx.width.toDp().value } else 320f
            val currentCanvasW = if (canvasSizePx.width > 0) canvasSizePx.width.toFloat() else 1000f
            val currentCanvasH = if (canvasSizePx.height > 0) canvasSizePx.height.toFloat() else 1000f

            val legacyTextRatio = OverlayMetrics.textFontRatio(selectedFont)

            val project = CollageProject(
                layoutId = currentLayoutId,
                slots = currentSlots,
                // one entry per SLOT, same fallback as the preview (slot i -> photo i, else first photo)
                photos = currentSlots.indices.map { i ->
                    val uri = currentPhotos.getOrElse(i) { currentPhotos.firstOrNull() ?: "" }
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
                overlays = overlayItems.withIndex().mapNotNull { (idx, item) ->
                    val nx = (currentCanvasW / 2f + item.offset.x) / currentCanvasW
                    val ny = (currentCanvasH / 2f + item.offset.y) / currentCanvasH
                    when (item) {
                        is OverlayItem.Emoji -> ProjectOverlayItem.Emoji(
                            id = item.id,
                            emoji = item.emoji,
                            x = nx,
                            y = ny,
                            rotation = item.rotation,
                            scale = item.scale,
                            zIndex = idx.toFloat()
                        )
                        is OverlayItem.Photo -> ProjectOverlayItem.Photo(
                            id = item.id,
                            uri = item.uri,
                            x = nx,
                            y = ny,
                            rotation = item.rotation,
                            scale = item.scale,
                            zIndex = idx.toFloat()
                        )
                        is OverlayItem.Text -> null // exported as a ProjectTextLayer below
                    }
                },
                textLayers = overlayItems.withIndex().mapNotNull { (idx, item) ->
                    (item as? OverlayItem.Text)?.let {
                        ProjectTextLayer(
                            id = it.id,
                            text = it.text,
                            x = (currentCanvasW / 2f + it.offset.x) / currentCanvasW,
                            y = (currentCanvasH / 2f + it.offset.y) / currentCanvasH,
                            fontName = it.font,
                            colorLong = it.textColor.toArgb().toLong(),
                            fontSizeRatio = OverlayMetrics.textFontRatio(it.font),
                            rotation = it.rotation,
                            scale = it.scale,
                            compact = true,
                            zIndex = idx.toFloat()
                        )
                    }
                } + (if (collageText.isNotBlank()) listOf(
                    ProjectTextLayer(
                        text = collageText,
                        x = (currentCanvasW / 2f + textOffset.x) / currentCanvasW,
                        y = (currentCanvasH / 2f + textOffset.y) / currentCanvasH,
                        fontName = selectedFont,
                        colorLong = textColor.toArgb().toLong(),
                        fontSizeRatio = legacyTextRatio,
                        zIndex = 1000f
                    )
                ) else emptyList()),
                drawStrokes = drawStrokes.map { stroke ->
                    NormalizedDrawStroke(
                        points = stroke.points.map { pt ->
                            NormalizedPoint(
                                x = pt.x / currentCanvasW,
                                y = pt.y / currentCanvasH
                            )
                        },
                        colorLong = stroke.color.toArgb().toLong(), // opacity already baked in
                        strokeWidthRatio = stroke.strokeWidth / currentCanvasW,
                        isEraser = stroke.isEraser
                    )
                }
            )

            Box(modifier = Modifier.fillMaxSize().zIndex(100f).background(Color.White)) {
                ExportShareScreen(
                    project = project,
                    layoutId = currentLayoutId,
                    selectedPhotos = currentPhotos,
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

// =====================================================================================
// Photo strip
// =====================================================================================

/**
 * Collapsible thumbnail bar above the tool panel.
 * Tap a thumbnail = select / focus its box on the canvas (tap again to deselect). With a photo selected the header
 * offers Replace (gallery), Move left / right (swap with the neighbour) and Delete. The trailing "+" adds photos.
 */
@Composable
private fun CollagePhotoStrip(
    photos: List<String>,
    selectedIndex: Int?,
    expanded: Boolean,
    canAdd: Boolean,
    onToggle: () -> Unit,
    onSelect: (Int?) -> Unit,
    onAdd: () -> Unit,
    onReplace: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onDelete: (Int) -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val sel = selectedIndex?.takeIf { it in photos.indices }
    LaunchedEffect(sel) { if (sel != null) listState.animateScrollToItem(sel) }

    Column(Modifier.fillMaxWidth().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))
        Row(
            Modifier.fillMaxWidth().height(36.dp).padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onToggle).padding(vertical = 6.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Photos · ${photos.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (expanded) "Hide photo strip" else "Show photo strip",
                    tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            if (expanded && sel != null) {
                IconButton(onClick = { onReplace(sel) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.SwapHoriz, "Replace photo", tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onMove(sel, sel - 1) }, enabled = sel > 0, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ChevronLeft, "Move left", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { onMove(sel, sel + 1) }, enabled = sel < photos.lastIndex, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ChevronRight, "Move right", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = { onDelete(sel) }, enabled = photos.size > 1, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Default.Delete, "Delete photo",
                        tint = if (photos.size > 1) Color(0xFFEF4444) else Color(0xFFCBD5E1), modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (expanded) {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(count = photos.size, key = { "$it|${photos[it]}" }) { i ->
                    val isSel = i == sel
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE2E8F0))
                            .border(if (isSel) 2.5.dp else 1.dp, if (isSel) Color(0xFF2563EB) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                            .clickable { onSelect(if (isSel) null else i) }
                    ) {
                        AsyncImage(
                            model = photos[i], contentDescription = "Photo ${i + 1}",
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            Modifier.align(Alignment.BottomStart).padding(3.dp).size(16.dp).clip(CircleShape)
                                .background(if (isSel) Color(0xFF2563EB) else Color(0xAA0F172A)),
                            contentAlignment = Alignment.Center
                        ) { Text("${i + 1}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                }
                item {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (canAdd) Color(0xFFEFF6FF) else Color(0xFFF1F5F9))
                            .border(1.dp, if (canAdd) Color(0xFF93C5FD) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .clickable(enabled = canAdd, onClick = onAdd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add, "Add photos",
                            tint = if (canAdd) Color(0xFF2563EB) else Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }
    }
}

/** Prominent per-tool Reset button; enabled only while the tool has changes. */
@Composable
private fun CollageResetButton(enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB), disabledContentColor = Color(0xFFCBD5E1)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
        modifier = Modifier.height(30.dp)
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}