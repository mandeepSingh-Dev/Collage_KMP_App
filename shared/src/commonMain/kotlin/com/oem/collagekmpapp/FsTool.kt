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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
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
import com.oem.collagekmpapp.ui.DiscardChangesDialog
import com.oem.collagekmpapp.ui.LocalIsTopScreen
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
    DRAW("Draw", Icons.Default.Edit),
    /** Opens the Layer Stack Manager sheet (it doesn't switch the tool panel). */
    LAYERS("Layers", Icons.Default.Layers)
}

/** Magnetic snap distance to the canvas centre lines (normalized canvas units). */
private const val FS_SNAP = 0.015f

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

/** Jagged torn-paper outline as a Shape (used for shadow + clip), same geometry as the exporter. */
private class FsTornShape(private val seed: Int) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val pts = tornPaperOutline(size.width, size.height, seed)
        val path = Path().apply {
            moveTo(pts[0], pts[1])
            var i = 2
            while (i < pts.size) { lineTo(pts[i], pts[i + 1]); i += 2 }
            close()
        }
        return Outline.Generic(path)
    }
}

/** Film strip frame drawn over the photo: even-odd path = real sprocket-hole cut-outs. */
private fun DrawScope.fsDrawFilmFrame(cornerPx: Float) {
    val iw = size.width
    val ih = size.height
    val ins = PhotoFrame.FILM.insets()
    val path = Path().apply {
        fillType = PathFillType.EvenOdd
        addRoundRect(RoundRect(0f, 0f, iw, ih, CornerRadius(cornerPx)))
        addRect(Rect(ins.side * iw, ins.top * iw, iw - ins.side * iw, ih - ins.bottom * iw))
        filmSprocketHoles(iw, ih).forEach { hole ->
            addRoundRect(RoundRect(hole[0], hole[1], hole[2], hole[3], CornerRadius(hole[4])))
        }
    }
    drawPath(path, Color(FreestyleItem.FILM_COLOR.toInt()))
}

/** Two translucent tape strips over the top corners (they may overhang the frame). */
private fun DrawScope.fsDrawTape() {
    val tape = Color(FreestyleItem.TAPE_COLOR.toInt())
    val edge = Color(FreestyleItem.TAPE_EDGE_COLOR.toInt())
    tapeStrips(size.width, size.height).forEach { t ->
        rotate(t.angle, pivot = Offset(t.cx, t.cy)) {
            val tl = Offset(t.cx - t.length / 2f, t.cy - t.thickness / 2f)
            val sz = Size(t.length, t.thickness)
            drawRect(tape, tl, sz)
            drawRect(edge, tl, sz, style = Stroke(size.width * 0.004f))
        }
    }
}

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
            .padding(horizontal = 12.dp, vertical = 6.dp)
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
    ) {
        when (item.kind) {
            FreestyleItemKind.PHOTO -> {
                // Card outline: torn paper uses its jagged shape for shadow + clip, every other frame the rounded rect.
                val outline: Shape = if (item.frame == PhotoFrame.TORN) remember(item.id) { FsTornShape(item.id.hashCode()) } else shape
                val whiteCard = item.frame == PhotoFrame.POLAROID || item.frame == PhotoFrame.TAPE || item.frame == PhotoFrame.TORN
                // Same modifier order as before (shadow -> clip -> white card), one level down so tape can overhang the clip.
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (item.shadow) Modifier.shadow(with(density) { (0.02f * wPx).toDp() }, outline, clip = false) else Modifier)
                        .clip(outline)
                        .then(if (whiteCard) Modifier.background(Color.White) else Modifier)
                ) {
                    // photo window from the shared frame insets (POLAROID: side / side / bottom exactly as before; NONE/ROUNDED: 0)
                    val ins = item.frame.insets()
                    val side = with(density) { (ins.side * wPx).toDp() }
                    val top = with(density) { (ins.top * wPx).toDp() }
                    val bottom = with(density) { (ins.bottom * wPx).toDp() }
                    Box(Modifier.fillMaxSize().padding(start = side, top = top, end = side, bottom = bottom)) {
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
                    if (item.frame == PhotoFrame.FILM) {
                        val cornerPx = item.cornerUnits * canvasW
                        Canvas(Modifier.fillMaxSize()) { fsDrawFilmFrame(cornerPx) }
                    }
                }
                if (item.frame == PhotoFrame.TAPE) Canvas(Modifier.fillMaxSize()) { fsDrawTape() }
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
        // locked items show a small padlock while selected (they're only selectable from the Layers sheet)
        if (selected && item.isLocked) {
            Box(
                Modifier.align(Alignment.TopStart).padding(4.dp).size(22.dp).clip(CircleShape).background(Color(0xCC0F172A)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Lock, "Locked", tint = Color.White, modifier = Modifier.size(13.dp)) }
        }
    }
}

// =================================================================== layer stack manager

@Composable
private fun FsLayerButton(
    icon: ImageVector,
    desc: String,
    tint: Color = Color(0xFF475569),
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = if (enabled) tint else Color(0xFFCBD5E1), modifier = Modifier.size(19.dp)) }
}

/**
 * Layer list, top row = front-most item. Drag the ≡ handle to reorder (rows shift live while dragging);
 * per row: forward / backward one step, bring to front, send to back, lock / unlock, delete (disabled while locked).
 * Indices passed to [onReorder] are z-indices in the items list (0 = back).
 */
@Composable
private fun FsLayerStack(
    items: List<FreestyleItem>,
    isSelected: (String) -> Boolean,
    onSelect: (FreestyleItem) -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    onForward: (String) -> Unit,
    onBackward: (String) -> Unit,
    onToFront: (String) -> Unit,
    onToBack: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val rowH = 60.dp
    val rowPx = with(LocalDensity.current) { rowH.toPx() }
    val latestItems by rememberUpdatedState(items)
    val latestReorder by rememberUpdatedState(onReorder)
    var dragFrom by remember { mutableStateOf<Int?>(null) }      // display index of the row being dragged
    var dragDy by remember { mutableStateOf(0f) }
    val display = items.asReversed()
    val n = display.size
    val from = dragFrom
    val target = from?.let { (it + (dragDy / rowPx).roundToInt()).coerceIn(0, (n - 1).coerceAtLeast(0)) }

    Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
        display.forEachIndexed { d, item ->
            key(item.id) {
                val shift = when {
                    from == null || target == null -> 0f
                    d == from -> dragDy
                    from < target && d in (from + 1)..target -> -rowPx
                    target < from && d in target until from -> rowPx
                    else -> 0f
                }
                val dragging = d == from
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(rowH)
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = shift
                            if (dragging) { shadowElevation = 8.dp.toPx(); shape = RoundedCornerShape(10.dp) }
                        }
                        .background(
                            when {
                                isSelected(item.id) -> Color(0xFFEFF6FF)
                                dragging -> Color.White
                                else -> Color.Transparent
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(item) }
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DragHandle, "Drag to reorder", tint = Color(0xFF94A3B8),
                        modifier = Modifier
                            .size(34.dp)
                            .pointerInput(item.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        dragFrom = latestItems.asReversed().indexOfFirst { it.id == item.id }.takeIf { it >= 0 }
                                        dragDy = 0f
                                    },
                                    onDrag = { change, amount -> change.consume(); dragDy += amount.y },
                                    onDragEnd = {
                                        val f = dragFrom
                                        val cnt = latestItems.size
                                        if (f != null && cnt > 1) {
                                            val t = (f + (dragDy / rowPx).roundToInt()).coerceIn(0, cnt - 1)
                                            // display index -> z-index (display is reversed)
                                            if (t != f) latestReorder(cnt - 1 - f, cnt - 1 - t)
                                        }
                                        dragFrom = null
                                        dragDy = 0f
                                    },
                                    onDragCancel = { dragFrom = null; dragDy = 0f }
                                )
                            }
                            .padding(5.dp)
                    )
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                        when (item.kind) {
                            FreestyleItemKind.PHOTO -> AsyncImage(item.content, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            FreestyleItemKind.EMOJI -> Text(item.content, fontSize = 20.sp)
                            FreestyleItemKind.TEXT -> Text("Tt", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when (item.kind) {
                                FreestyleItemKind.PHOTO -> "Photo"
                                FreestyleItemKind.EMOJI -> "Sticker"
                                FreestyleItemKind.TEXT -> item.content.take(16)
                            },
                            fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (item.isLocked) Text("Locked", fontSize = 10.sp, color = Color(0xFFB45309))
                    }
                    val locked = item.isLocked
                    FsLayerButton(Icons.Default.KeyboardArrowUp, "Bring forward") { onForward(item.id) }
                    FsLayerButton(Icons.Default.KeyboardArrowDown, "Send backward") { onBackward(item.id) }
                    FsLayerButton(Icons.Default.VerticalAlignTop, "Bring to front") { onToFront(item.id) }
                    FsLayerButton(Icons.Default.VerticalAlignBottom, "Send to back") { onToBack(item.id) }
                    FsLayerButton(
                        if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                        if (locked) "Unlock" else "Lock",
                        tint = if (locked) Color(0xFFB45309) else Color(0xFF475569)
                    ) { onToggleLock(item.id) }
                    FsLayerButton(Icons.Default.Delete, "Delete", tint = Color(0xFFEF4444), enabled = !locked) { onDelete(item.id) }
                }
            }
        }
    }
}

// =================================================================== the screen

/** Everything Undo / Redo restores in the Freestyle editor. Item `aspect` is derived from the loaded image, so it is excluded. */
private data class FsSnap(
    val items: List<FreestyleItem>,
    val aspect: AspectRatio,
    val bgColor: Color,
    val bgGradient: List<Long>,
    val bgPhoto: String?,
    val bgBlur: Float,
    val bgTexture: BackgroundTexture,
    val filterPreset: String,
    val filterIntensity: Float,
    val adjustValues: Map<String, Float>,
    val strokes: List<NormalizedDrawStroke>
)

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
    // starts on the first photo, so the Frame panel (the opening tool) applies to it straight away
    var selectedId by remember { mutableStateOf(freeItems.firstOrNull { it.kind == FreestyleItemKind.PHOTO }?.id) }
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

    // ---- alignment snap guides (shown while an item is magnetized to a centre line)
    var snapGuideX by remember { mutableStateOf(false) }   // vertical line at x = 0.5
    var snapGuideY by remember { mutableStateOf(false) }   // horizontal line at y = 0.5
    val haptic = LocalHapticFeedback.current

    fun updateItem(id: String, f: (FreestyleItem) -> FreestyleItem) {
        freeItems = freeItems.map { if (it.id == id) f(it) else it }
    }

    /** Moves item [id] to z-index [to] (0 = back, lastIndex = front). */
    fun moveItemTo(id: String, to: Int) {
        val idx = freeItems.indexOfFirst { it.id == id }
        if (idx < 0) return
        val m = freeItems.toMutableList()
        val it = m.removeAt(idx)
        m.add(to.coerceIn(0, m.size), it)
        freeItems = m
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

    // Opening the Frame tool: nothing selected (or a sticker / text selected) -> select the first photo,
    // so the frame chips apply to it immediately. Keyed on the tool only, so tapping empty canvas while the
    // Frame panel is open still deselects normally.
    LaunchedEffect(tool) {
        if (tool == FsTool.FRAME) {
            val current = freeItems.firstOrNull { it.id == selectedId }
            if (current == null || current.kind != FreestyleItemKind.PHOTO) {
                freeItems.firstOrNull { it.kind == FreestyleItemKind.PHOTO }?.let { selectedId = it.id }
            }
        }
    }

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

    // ---------------------------------------------------------------- Undo / Redo
    val knownAspects = remember { mutableMapOf<String, Float>() }
    freeItems.forEach { knownAspects[it.id] = it.aspect }
    val history = rememberEditHistory(
        current = FsSnap(
            items = freeItems.map { it.copy(aspect = 0f) },
            aspect = aspect, bgColor = bgColor, bgGradient = bgGradient, bgPhoto = bgPhoto, bgBlur = bgBlur,
            bgTexture = bgTexture, filterPreset = filterPreset, filterIntensity = filterIntensity,
            adjustValues = adjustValues, strokes = strokes
        )
    ) { snap ->
        // put the measured image aspect back (it is not part of the history)
        freeItems = snap.items.map { it.copy(aspect = knownAspects[it.id] ?: it.aspect) }
        aspect = snap.aspect
        bgColor = snap.bgColor
        bgGradient = snap.bgGradient
        bgPhoto = snap.bgPhoto
        bgBlur = snap.bgBlur
        bgTexture = snap.bgTexture
        filterPreset = snap.filterPreset
        filterIntensity = snap.filterIntensity
        adjustValues = snap.adjustValues
        strokes = snap.strokes
        currentStroke = emptyList()
        if (selectedId != null && snap.items.none { it.id == selectedId }) selectedId = null
    }

    // ---------------------------------------------------------------- per-tool Reset
    val defPhoto = remember { newFreestylePhoto("", 0) }
    val defText = remember { newFreestyleText("") }
    val selText = selected?.takeIf { it.kind == FreestyleItemKind.TEXT }
    val toolIsDirty: Boolean = when (tool) {
        FsTool.RATIO -> aspect != AspectRatio.SQUARE
        FsTool.BACKGROUND -> bgGradient.isNotEmpty() || bgPhoto != null || bgBlur != 100f ||
                bgTexture != BackgroundTexture.NONE || bgColor != Color(0xFFFFF7ED)
        FsTool.FRAME -> freeItems.any { it.kind == FreestyleItemKind.PHOTO && (it.frame != defPhoto.frame || it.shadow != defPhoto.shadow) }
        FsTool.TEXT -> selText != null && (selText.fontName != defText.fontName || selText.colorLong != defText.colorLong ||
                selText.fontSizeRatio != defText.fontSizeRatio || selText.textPill != defText.textPill ||
                selText.textShadow != defText.textShadow)
        FsTool.STICKERS -> freeItems.any { it.kind == FreestyleItemKind.EMOJI && !it.isLocked }
        FsTool.FILTER -> filterPreset != "Original" || filterIntensity != 0f
        FsTool.ADJUST -> adjustValues.any { it.value != 0f }
        FsTool.DRAW -> strokes.isNotEmpty() || eraser || brushSize != 10f || brushOpacity != 100f || brushColor != Color(0xFFF43F8E)
        FsTool.LAYERS -> false
    }
    val resetTool: () -> Unit = {
        when (tool) {
            FsTool.RATIO -> aspect = AspectRatio.SQUARE
            FsTool.BACKGROUND -> {
                bgColor = Color(0xFFFFF7ED); bgGradient = emptyList(); bgPhoto = null
                bgBlur = 100f; bgTexture = BackgroundTexture.NONE
            }
            FsTool.FRAME -> freeItems = freeItems.map {
                if (it.kind == FreestyleItemKind.PHOTO) it.copy(frame = defPhoto.frame, shadow = defPhoto.shadow) else it
            }
            FsTool.TEXT -> selText?.let { st ->
                updateItem(st.id) {
                    it.copy(
                        fontName = defText.fontName, colorLong = defText.colorLong, fontSizeRatio = defText.fontSizeRatio,
                        textPill = defText.textPill, textShadow = defText.textShadow
                    )
                }
            }
            FsTool.STICKERS -> {
                // locked stickers can't be deleted, so Reset keeps them
                freeItems = freeItems.filter { it.kind != FreestyleItemKind.EMOJI || it.isLocked }
                if (selected?.kind == FreestyleItemKind.EMOJI && selected?.isLocked == false) selectedId = null
            }
            FsTool.FILTER -> { filterPreset = "Original"; filterIntensity = 0f }
            FsTool.ADJUST -> adjustValues = emptyMap()
            FsTool.DRAW -> {
                strokes = emptyList(); currentStroke = emptyList(); eraser = false
                brushColor = Color(0xFFF43F8E); brushSize = 10f; brushOpacity = 100f
            }
            FsTool.LAYERS -> Unit
        }
    }

    // ---------------------------------------------------------------- Unsaved-changes guardrail
    // Top-bar arrow AND system back/gesture: exit directly when nothing is undoable, otherwise confirm first.
    // (The export / layers / text dialogs handle back themselves while they are open.)
    var showDiscardDialog by remember { mutableStateOf(false) }
    val requestBack: () -> Unit = {
        if (history.canUndo) showDiscardDialog = true else onBack()
    }
    BackHandler(enabled = LocalIsTopScreen.current) { requestBack() }
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onCancel = { showDiscardDialog = false },
            onDiscard = { showDiscardDialog = false; onBack() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Freestyle Studio", fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = requestBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    UndoRedoActions(history)
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
                Column(
                    Modifier.fillMaxWidth().height(264.dp).background(Color(0xFFF8FAFC))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // universal per-tool Reset
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(tool.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        FsResetButton(enabled = toolIsDirty, onClick = resetTool)
                    }
                    Column(
                        Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                                Text(r.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold,
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
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    // scrollable: 6 frame styles no longer fit on one screen width
                                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PhotoFrame.entries.forEach { f ->
                                            FsChip(f.label, sel.frame == f) { updateItem(sel.id) { it.copy(frame = f) } }
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Soft drop shadow", fontSize = 12.sp, color = Color(0xFF334155))
                                        Switch(checked = sel.shadow, onCheckedChange = { c -> updateItem(sel.id) { it.copy(shadow = c) } })
                                    }
                                    TextButton(onClick = {
                                        freeItems = freeItems.map {
                                            if (it.kind == FreestyleItemKind.PHOTO) it.copy(frame = sel.frame, shadow = sel.shadow) else it
                                        }
                                    }) { Text("Apply this frame to all photos") }
                                } else {
                                    Text("Tap a photo on the canvas to style its frame.\nDrag to move, pinch to resize, twist to rotate.",
                                        fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                                OutlinedButton(onClick = { addPhotosPicker.launch() }) { Text("+ Add photos") }
                            }
                            // ------------------------------------------------ TEXT
                            FsTool.TEXT -> {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("+ Add Text", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B),
                                        modifier = Modifier.clickable { editingTextId = null; textDialogOpen = true })
                                    val st = selected?.takeIf { it.kind == FreestyleItemKind.TEXT }
                                    if (st != null) Text("Edit text", fontSize = 12.sp, color = Color(0xFF2563EB),
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
                                        fontSize = 12.sp, color = Color(0xFF64748B))
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
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "$adjustItem: ${if (currentValue > 0 && adjustDef.isBipolar) "+" else ""}${currentValue.toInt()}%",
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            // ------------------------------------------------ LAYERS (opens the sheet; panel never switches here)
                            FsTool.LAYERS -> Unit
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FsTool.entries.forEach { t ->
                        // "Layers" opens the Layer Stack Manager sheet and keeps the current tool panel
                        val sel = if (t == FsTool.LAYERS) showLayers else tool == t
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (sel) Color(0xFFEFF6FF) else Color.Transparent)
                                .clickable { if (t == FsTool.LAYERS) showLayers = true else tool = t }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(t.icon, null, tint = if (sel) Color(0xFF2563EB) else Color(0xFF64748B), modifier = Modifier.size(18.dp))
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

                            val margin = 16.dp.toPx()
                            val hitR = 26.dp.toPx()

                            // ------------------------- handles of the selected item (never for locked items)
                            val sel = freeItems.firstOrNull { it.id == selectedId }?.takeIf { !it.isLocked }
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

                            // ------------------------- pick an item (top-most first; locked items are skipped so
                            // touches fall through to what's underneath – manage locked layers from the Layers sheet)
                            val hit = freeItems.lastOrNull { !it.isLocked && fsHitTest(it, down.position, w, h) }
                            if (hit == null) {
                                selectedId = null
                                return@awaitEachGesture
                            }
                            selectedId = hit.id
                            // tapping brings it to the front
                            val cur = freeItems
                            freeItems = cur.filter { it.id != hit.id } + cur.first { it.id == hit.id }
                            down.consume()

                            // un-snapped ("raw") centre of the touched item, so it can leave the magnet again
                            var rawX = hit.x
                            var rawY = hit.y

                            // ------------------------- 1-finger drag, 2-finger pinch + rotate (selected item only)
                            do {
                                val ev = awaitPointerEvent()
                                val zoom = ev.calculateZoom()
                                val rot = ev.calculateRotation()
                                val pan = ev.calculatePan()
                                if (zoom != 1f || rot != 0f || pan != Offset.Zero) {
                                    if (zoom == 1f && rot == 0f) {
                                        // pure drag -> magnetic centre-line snapping on the item's centre
                                        rawX += pan.x / w
                                        rawY += pan.y / h
                                        val snapX = abs(rawX - 0.5f) < FS_SNAP
                                        val snapY = abs(rawY - 0.5f) < FS_SNAP
                                        if ((snapX && !snapGuideX) || (snapY && !snapGuideY)) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        snapGuideX = snapX
                                        snapGuideY = snapY
                                        val tx = if (snapX) 0.5f else rawX
                                        val ty = if (snapY) 0.5f else rawY
                                        updateItem(hit.id) { it.copy(x = tx, y = ty) }
                                    } else {
                                        // pinch / twist (the original single-item maths)
                                        updateItem(hit.id) {
                                            it.copy(
                                                x = it.x + pan.x / w,
                                                y = it.y + pan.y / h,
                                                scale = (it.scale * zoom).coerceIn(0.15f, 8f),
                                                rotation = it.rotation + rot
                                            )
                                        }
                                        freeItems.firstOrNull { it.id == hit.id }?.let { rawX = it.x; rawY = it.y }
                                        snapGuideX = false
                                        snapGuideY = false
                                    }
                                    ev.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                            } while (ev.changes.any { it.pressed })
                            snapGuideX = false
                            snapGuideY = false
                        }
                    }
            ) {
                // ---------------- clipped paper
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                    // background
                    when {
                        bgPhoto != null -> AsyncImage(
                            model = rememberPhotoRequest(bgPhoto, 1024), contentDescription = null, contentScale = ContentScale.Crop,
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

                    // alignment guides: dashed centre lines while an item is snapped to them
                    if (snapGuideX || snapGuideY) {
                        Canvas(Modifier.fillMaxSize()) {
                            val guide = Color(0xFFEC4899)
                            val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                            val sw = 1.5.dp.toPx()
                            if (snapGuideX) drawLine(guide, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), sw, pathEffect = dash)
                            if (snapGuideY) drawLine(guide, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), sw, pathEffect = dash)
                        }
                    }
                }

                // ---------------- transform handles (outside the clip so they stay visible; none for locked items)
                val s = selected
                if (s != null && !s.isLocked && W > 0f && tool != FsTool.DRAW) {
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
                Text("Layers (top = front)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Drag ≡ to reorder · locked layers can't be moved or deleted", fontSize = 11.sp, color = Color(0xFF64748B))
                Spacer(Modifier.height(8.dp))
                if (freeItems.isEmpty()) Text("Nothing on the canvas yet.", color = Color(0xFF64748B))
                FsLayerStack(
                    items = freeItems,
                    isSelected = { id -> id == selectedId },
                    onSelect = { item -> selectedId = item.id },
                    onReorder = { from, to ->
                        val m = freeItems.toMutableList()
                        m.add(to, m.removeAt(from))
                        freeItems = m
                    },
                    onForward = { id -> moveItem(id, 1) },
                    onBackward = { id -> moveItem(id, -1) },
                    onToFront = { id -> moveItemTo(id, freeItems.lastIndex) },
                    onToBack = { id -> moveItemTo(id, 0) },
                    onToggleLock = { id ->
                        val nowLocked = freeItems.firstOrNull { it.id == id }?.isLocked != true
                        updateItem(id) { it.copy(isLocked = nowLocked) }
                    },
                    onDelete = { id ->
                        if (freeItems.firstOrNull { it.id == id }?.isLocked != true) {
                            freeItems = freeItems.filter { f -> f.id != id }
                            if (selectedId == id) selectedId = null
                        }
                    }
                )
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

/** Prominent per-tool Reset button; enabled only while the tool has changes. */
@Composable
private fun FsResetButton(enabled: Boolean, onClick: () -> Unit) {
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