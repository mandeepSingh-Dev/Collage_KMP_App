package com.oem.collagekmpapp.ui

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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.oem.collagekmpapp.BackgroundTexture
import com.oem.collagekmpapp.AdjustTools
import com.oem.collagekmpapp.FilterPickerPanel
import com.oem.collagekmpapp.FreestyleItem
import com.oem.collagekmpapp.FreestyleItemKind
import com.oem.collagekmpapp.FreestyleProject
import com.oem.collagekmpapp.PhotoColorEngine
import com.oem.collagekmpapp.USE_LEGACY_EXPORT_DIALOG
import com.oem.collagekmpapp.UnifiedExportScreen
import com.oem.collagekmpapp.components.FluidSlider
import com.oem.collagekmpapp.model.*
import com.oem.collagekmpapp.newFreestyleEmoji
import com.oem.collagekmpapp.newFreestyleText
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

// =====================================================================================
// Models
// =====================================================================================

private enum class SpTool(val label: String, val icon: ImageVector) {
    RESIZE("Resize", Icons.Default.PhotoSizeSelectLarge),
    RATIO("Ratio", Icons.Default.AspectRatio),
    CROP("Crop", Icons.Default.CropRotate),
    FILTER("Filter", Icons.Default.Star),
    ADJUST("Adjust", Icons.Default.Tune),
    BLUR("Blur", Icons.Default.BlurOn),
    STICKERS("Stickers", Icons.Default.EmojiEmotions),
    TEXT("Text", Icons.Default.TextFields),
    BACKGROUND("Backdrop", Icons.Default.Wallpaper),
    DRAW("Draw", Icons.Default.Edit)
}

private enum class SpCropShape(val label: String, val ratio: Float?) {
    FREE("Free", null),
    R_1_1("1:1", 1f),
    R_4_5("4:5", 4f / 5f),
    R_5_4("5:4", 5f / 4f),
    R_3_4("3:4", 3f / 4f),
    R_4_3("4:3", 4f / 3f),
    R_2_3("2:3", 2f / 3f),
    R_3_2("3:2", 3f / 2f),
    R_9_16("9:16", 9f / 16f),
    R_16_9("16:9", 16f / 9f),
    R_1_2("1:2", 1f / 2f)
}

/** Crop rectangle in *image-normalised* coordinates (0..1 of the source photo). */
private data class SpCrop(val l: Float = 0f, val t: Float = 0f, val r: Float = 1f, val b: Float = 1f) {
    val w get() = r - l
    val h get() = b - t
}

private val SpPalette: List<Color> = listOf(
    0xFFFFFFFFL, 0xFF0F172AL, 0xFFEF4444L, 0xFFF97316L, 0xFFFACC15L, 0xFF22C55EL, 0xFF06B6D4L,
    0xFF3B82F6L, 0xFF9333EAL, 0xFFEC4899L, 0xFFF1F5F9L, 0xFF94A3B8L, 0xFF475569L, 0xFFFDE68AL,
    0xFFFBCFE8L, 0xFFBAE6FDL, 0xFFBBF7D0L, 0xFFE9D5FFL, 0xFFFED7AAL, 0xFFD6BCFAL, 0xFF78350FL
).map { Color(it) }

private val SpGradients: List<List<Long>> = listOf(
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

private val SpStickers = mapOf(
    "Emoji" to listOf("😍", "😎", "🔥", "✨", "🥳", "👑", "🌈", "⭐", "🦋", "💯", "🚀", "💎"),
    "Love" to listOf("🥰", "❤️", "💖", "💕", "💘", "💋", "💌", "💍", "🌹", "🫶", "💞", "😘"),
    "Party" to listOf("🥂", "🎂", "🎈", "🎉", "🎊", "🍾", "🎁", "💃", "🎶", "🍸", "🍕", "🪩"),
    "Travel" to listOf("✈️", "🌴", "🗺️", "🗽", "🗼", "🏕️", "🚗", "🏖️", "🎒", "🚂", "🚢", "🏔️")
)

private val SpFonts = listOf("Modern", "Script", "Bold", "Retro", "Serif", "Neon")

// =====================================================================================
// Geometry helpers
// =====================================================================================

private fun spRad(deg: Float) = (deg * PI / 180.0).toFloat()

/** Largest centred crop with the given pixel ratio (w/h) inside an image of aspect [a]. */
private fun spCenteredCrop(target: Float, a: Float): SpCrop {
    val w: Float
    val h: Float
    if (target >= a) { w = 1f; h = a / target } else { w = target / a; h = 1f }
    return SpCrop((1f - w) / 2f, (1f - h) / 2f, (1f + w) / 2f, (1f + h) / 2f)
}

/** corner: 0 = TL, 1 = TR, 2 = BL, 3 = BR. [p] is the pointer in normalised coords. */
private fun spResizeCrop(c: SpCrop, corner: Int, p: Offset, lock: Float?, a: Float): SpCrop {
    val minS = 0.08f
    val fx = if (corner == 0 || corner == 2) c.r else c.l
    val fy = if (corner == 0 || corner == 1) c.b else c.t
    val goRight = p.x >= fx
    val goDown = p.y >= fy
    val maxW = if (goRight) 1f - fx else fx
    val maxH = if (goDown) 1f - fy else fy
    var w = abs(p.x - fx).coerceAtLeast(minS).coerceAtMost(maxW)
    var h: Float
    if (lock != null) {
        h = w * a / lock
        if (h > maxH) { h = maxH; w = h * lock / a }
    } else {
        h = abs(p.y - fy).coerceAtLeast(minS).coerceAtMost(maxH)
    }
    val l = if (goRight) fx else fx - w
    val t = if (goDown) fy else fy - h
    return SpCrop(l, t, l + w, t + h)
}

private fun spMoveCrop(c: SpCrop, dx: Float, dy: Float): SpCrop {
    val x = dx.coerceIn(-c.l, 1f - c.r)
    val y = dy.coerceIn(-c.t, 1f - c.b)
    return SpCrop(c.l + x, c.t + y, c.r + x, c.b + y)
}

/**
 * Size of the *un-rotated* photo box (px) once the crop has been fitted / filled into a
 * canvas of [cw] x [ch]. If the photo is turned by an odd number of quarter turns the visual
 * footprint is the transpose of the box, so the fit is computed on the transposed ratio.
 */
private fun spBoxSize(crop: SpCrop, a: Float, odd: Boolean, fit: Boolean, cw: Float, ch: Float): Offset {
    val cr = crop.w * a / crop.h
    val eff = if (odd) 1f / cr else cr
    val ratio = cw / ch
    val useWidth = if (fit) eff > ratio else eff <= ratio
    val vw = if (useWidth) cw else ch * eff
    val vh = if (useWidth) cw / eff else ch
    return if (odd) Offset(vh, vw) else Offset(vw, vh)
}

private fun spHitTest(it: FreestyleItem, p: Offset, w: Float, h: Float): Boolean {
    val v = p - Offset(it.x * w, it.y * h)
    val r = -spRad(it.rotation)
    val lx = (v.x * cos(r) - v.y * sin(r)) / it.scale
    val ly = (v.x * sin(r) + v.y * cos(r)) / it.scale
    return abs(lx) <= it.widthUnits * w / 2f && abs(ly) <= it.heightUnits * w / 2f
}

/** sy = -1 -> top-right (delete), sy = +1 -> bottom-right (scale / rotate). */
private fun spHandlePos(it: FreestyleItem, w: Float, h: Float, sy: Float, margin: Float): Offset {
    val hw = it.widthUnits * w / 2f * it.scale
    val hh = it.heightUnits * w / 2f * it.scale * sy
    val r = spRad(it.rotation)
    val x = it.x * w + hw * cos(r) - hh * sin(r)
    val y = it.y * h + hw * sin(r) + hh * cos(r)
    return Offset(x.coerceIn(margin, w - margin), y.coerceIn(margin, h - margin))
}

// =====================================================================================
// Drawing helpers
// =====================================================================================

private fun DrawScope.spDrawTexture(t: BackgroundTexture) {
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

private fun DrawScope.spDrawStroke(points: List<Offset>, color: Color, widthRatio: Float) {
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

private fun spTextStyle(item: FreestyleItem, fontSize: androidx.compose.ui.unit.TextUnit): TextStyle {
    val color = Color(item.colorLong.toInt())
    val shadow = when {
        item.fontName == "Neon" -> Shadow(color, Offset.Zero, 16f)
        item.textShadow -> Shadow(Color(0x99000000), Offset(3f, 3f), 6f)
        else -> null
    }
    val base = when (item.fontName) {
        "Script" -> TextStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold)
        "Bold" -> TextStyle(fontWeight = FontWeight.ExtraBold)
        "Retro" -> TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        "Serif" -> TextStyle(fontFamily = FontFamily.Serif)
        "Neon" -> TextStyle(fontWeight = FontWeight.ExtraBold)
        else -> TextStyle(fontWeight = FontWeight.Medium)
    }
    return base.copy(color = color, fontSize = fontSize, shadow = shadow)
}

/**
 * Keeps only the soft-edged disc described by (cx, cy, r) of whatever this modifier wraps.
 * [feather] 0 = hard edge, 1 = fully gradual from the centre.
 */
private fun Modifier.spCircleMask(cx: Float, cy: Float, r: Float, feather: Float): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val inner = (1f - feather).coerceIn(0f, 0.97f)
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to Color.Black, inner to Color.Black, 1f to Color.Transparent),
                center = Offset(cx, cy),
                radius = r.coerceAtLeast(1f)
            ),
            blendMode = BlendMode.DstIn
        )
    }

// =====================================================================================
// Small UI pieces
// =====================================================================================

@Composable
private fun SpChip(text: String, selected: Boolean, onClick: () -> Unit) {
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
private fun SpColorRow(selected: Color?, onPick: (Color) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        items(SpPalette) { col ->
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
private fun SpLabel(text: String) =
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))

@Composable
private fun SpHint(text: String) = Text(text, fontSize = 13.sp, color = Color(0xFF64748B))

@Composable
private fun SpSliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 11.sp, color = Color(0xFF334155))
        FluidSlider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.fillMaxWidth().height(36.dp))
    }
}

@Composable
private fun SpHandle(pos: Offset, color: Color, icon: ImageVector, desc: String) {
    val r = with(LocalDensity.current) { 13.dp.toPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((pos.x - r).roundToInt(), (pos.y - r).roundToInt()) }
            .size(26.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = Color.White, modifier = Modifier.size(14.dp)) }
}

/** Text / emoji overlay (the photo itself is rendered by [SpPhotoLayer]). */
@Composable
private fun SpOverlayView(item: FreestyleItem, canvasW: Float, canvasH: Float, selected: Boolean) {
    val density = LocalDensity.current
    val wPx = item.widthUnits * canvasW
    val hPx = item.heightUnits * canvasW
    Box(
        modifier = Modifier
            .offset { IntOffset((item.x * canvasW - wPx / 2f).roundToInt(), (item.y * canvasH - hPx / 2f).roundToInt()) }
            .size(with(density) { wPx.toDp() }, with(density) { hPx.toDp() })
            .graphicsLayer { rotationZ = item.rotation; scaleX = item.scale; scaleY = item.scale }
    ) {
        when (item.kind) {
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
                    style = spTextStyle(item, with(density) { (item.fontSizeRatio * canvasW).toSp() }),
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Visible
                )
            }
            else -> Unit
        }
        if (selected) Box(Modifier.fillMaxSize().border(1.5.dp, Color(0xFF2563EB)))
    }
}

/**
 * The photo with crop → fit/fill → rotate / flip / H-V-centre scale applied.
 * Draws into a canvas of [canvasW] x [canvasH] px.
 */
@Composable
private fun SpPhotoLayer(
    uri: String,
    crop: SpCrop,
    imgAspect: Float,
    fit: Boolean,
    quarterTurns: Int,
    fineAngle: Float,
    flipH: Boolean,
    flipV: Boolean,
    scaleH: Float,
    scaleV: Float,
    centerScale: Float,
    canvasW: Float,
    canvasH: Float,
    colorFilter: ColorFilter?,
    vignette: Float,
    onAspect: (Float) -> Unit
) {
    val density = LocalDensity.current
    val size = spBoxSize(crop, imgAspect, quarterTurns % 2 != 0, fit, canvasW, canvasH)
    val dw = size.x
    val dh = size.y
    val fullW = dw / crop.w
    val fullH = dh / crop.h
    // Outer layer = mirror flips (always about the VISUAL axes, whatever the rotation is).
    Box(
        Modifier.fillMaxSize().graphicsLayer {
            scaleX = if (flipH) -1f else 1f
            scaleY = if (flipV) -1f else 1f
        },
        contentAlignment = Alignment.Center
    ) {
        // Inner layer = rotate, then horizontal / vertical / centre scale (applied in the photo's own axes).
        Box(
            Modifier
                .size(with(density) { dw.toDp() }, with(density) { dh.toDp() })
                .graphicsLayer {
                    rotationZ = fineAngle + quarterTurns * 90f
                    scaleX = centerScale * scaleH
                    scaleY = centerScale * scaleV
                }
                .clip(RectangleShape)
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = colorFilter,
                onSuccess = { s ->
                    val sz = s.painter.intrinsicSize
                    if (sz.width > 0f && sz.height > 0f) onAspect(sz.width / sz.height)
                },
                modifier = Modifier.layout { measurable, constraints ->
                    val p = measurable.measure(
                        Constraints.fixed(fullW.roundToInt().coerceAtLeast(1), fullH.roundToInt().coerceAtLeast(1))
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        p.place(-(crop.l * fullW).roundToInt(), -(crop.t * fullH).roundToInt())
                    }
                }
            )
            if (abs(vignette) > 0.01f) {
                val edge = (if (vignette > 0) Color.Black else Color.White).copy(alpha = abs(vignette) * 0.9f)
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(0.55f to Color.Transparent, 1f to edge)))
            }
        }
    }
}

// =====================================================================================
// The screen
// =====================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SinglePhotoEditorScreen(
    photoUri: String,
    onBack: () -> Unit
) {
    val exporter = rememberSinglePhotoExporter()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val density = LocalDensity.current

    var tool by remember { mutableStateOf(SpTool.RESIZE) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var imgAspect by remember { mutableStateOf(1f) }

    // ---- resize / ratio
    var ratioKey by remember { mutableStateOf("Original") }
    var ratioOverride by remember { mutableStateOf<Float?>(null) } // null = follow the cropped photo
    var fit by remember { mutableStateOf(true) }
    val ratios = remember { AspectRatio.entries.distinctBy { it.displayName } }

    // ---- crop & rotate
    var cropTab by remember { mutableStateOf(0) } // 0 = crop, 1 = rotate
    var crop by remember { mutableStateOf(SpCrop()) }
    var cropShape by remember { mutableStateOf(SpCropShape.FREE) }
    var fineAngle by remember { mutableStateOf(0f) }
    var quarterTurns by remember { mutableStateOf(0) }
    var flipH by remember { mutableStateOf(false) }
    var flipV by remember { mutableStateOf(false) }
    var scaleH by remember { mutableStateOf(100f) } // percent
    var scaleV by remember { mutableStateOf(100f) } // percent
    var centerScale by remember { mutableStateOf(100f) } // percent

    // ---- filter
    var filterPreset by remember { mutableStateOf("Original") }
    var filterIntensity by remember { mutableStateOf(0f) }
    var adjustValues by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var adjustGroup by remember { mutableStateOf(AdjustTools.groups.first()) }
    var adjustItem by remember { mutableStateOf(AdjustTools.inGroup(AdjustTools.groups.first()).first().name) }

    // ---- spotlight blur
    var blurOn by remember { mutableStateOf(false) }
    var blurInside by remember { mutableStateOf(false) } // false = keep circle sharp, blur the rest
    var blurAmount by remember { mutableStateOf(60f) }
    var blurFeather by remember { mutableStateOf(0.35f) }
    var blurCx by remember { mutableStateOf(0.5f) }
    var blurCy by remember { mutableStateOf(0.5f) }
    var blurR by remember { mutableStateOf(0.25f) } // fraction of canvas width

    // ---- overlays (text + stickers)
    var overlays by remember { mutableStateOf<List<FreestyleItem>>(emptyList()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var stickerCategory by remember { mutableStateOf("Emoji") }
    var textDialogOpen by remember { mutableStateOf(false) }
    var editingTextId by remember { mutableStateOf<String?>(null) }

    // ---- background
    var bgColor by remember { mutableStateOf(Color(0xFFFFFFFF)) }
    var bgGradient by remember { mutableStateOf<List<Long>>(emptyList()) }
    var bgBlurPhoto by remember { mutableStateOf(false) }
    var bgBlur by remember { mutableStateOf(100f) }
    var bgTexture by remember { mutableStateOf(BackgroundTexture.NONE) }

    // ---- draw
    var strokes by remember { mutableStateOf<List<NormalizedDrawStroke>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var eraser by remember { mutableStateOf(false) }
    var brushColor by remember { mutableStateOf(Color(0xFFF43F8E)) }
    var brushSize by remember { mutableStateOf(10f) }
    var brushOpacity by remember { mutableStateOf(100f) }

    // ---- export
    var showExport by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var exportQuality by remember { mutableStateOf(2048) }
    var exportFormat by remember { mutableStateOf("JPG") }

    fun updateOverlay(id: String, f: (FreestyleItem) -> FreestyleItem) {
        overlays = overlays.map { if (it.id == id) f(it) else it }
    }

    val photoFilter = remember(filterPreset, filterIntensity, adjustValues) {
        PhotoColorEngine.buildMatrix(filterPreset, filterIntensity, adjustValues)
            ?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
    }
    val vignette = remember(filterPreset, filterIntensity, adjustValues) {
        PhotoColorEngine.effects(filterPreset, filterIntensity, adjustValues).vignette
    }

    val selected = overlays.firstOrNull { it.id == selectedId }
    val editingCrop = tool == SpTool.CROP && cropTab == 0
    val croppedRatio = crop.w * imgAspect / crop.h
    val oddTurn = quarterTurns % 2 != 0
    val visualCroppedRatio = if (oddTurn) 1f / croppedRatio else croppedRatio
    val canvasRatio = (if (editingCrop) imgAspect else (ratioOverride ?: visualCroppedRatio)).coerceIn(0.2f, 5f)
    val totalAngle = fineAngle + quarterTurns * 90f

    /**
     * Snapshot of everything on screen. The platform exporter re-renders it at full resolution
     * with the same maths as the preview (crop, fit/fill, rotation, flips, H/V/centre scale,
     * filter + adjustments, spotlight blur, backdrop, stickers, text and doodles).
     */
    fun buildEdit(): SinglePhotoEdit {
        val ratio = (ratioOverride ?: visualCroppedRatio).coerceIn(0.2f, 5f)
        val overlayProject = FreestyleProject(
            aspectRatioWidth = ratio,
            aspectRatioHeight = 1f,
            backgroundColorLong = bgColor.toArgb().toLong() and 0xFFFFFFFFL,
            backgroundGradientColors = bgGradient,
            backgroundPhotoUri = if (bgBlurPhoto) photoUri else null,
            backgroundBlur = bgBlur / 100f,
            backgroundTexture = bgTexture,
            items = overlays,
            drawStrokes = strokes
        )
        return SinglePhotoEdit(
            photoUri = photoUri,
            ratio = ratio,
            cropL = crop.l, cropT = crop.t, cropR = crop.r, cropB = crop.b,
            fit = fit,
            quarterTurns = quarterTurns,
            fineAngle = fineAngle,
            flipH = flipH,
            flipV = flipV,
            scaleH = scaleH / 100f,
            scaleV = scaleV / 100f,
            scaleCenter = centerScale / 100f,
            filterPreset = filterPreset,
            filterIntensity = filterIntensity,
            adjustValues = adjustValues,
            blurOn = blurOn,
            blurInside = blurInside,
            blurAmount = blurAmount / 100f,
            blurFeather = blurFeather,
            blurCx = blurCx,
            blurCy = blurCy,
            blurR = blurR,
            project = overlayProject
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Photo", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
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
                            // ------------------------------------------------ 1. RESIZE
                            SpTool.RESIZE -> {
                                SpLabel("Resize photo")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpChip("Original", ratioKey == "Original") { ratioKey = "Original"; ratioOverride = null }
                                    SpChip("Square 1:1", ratioKey == "1:1") { ratioKey = "1:1"; ratioOverride = 1f }
                                }
                                SpLabel("Photo placement")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpChip("Fit (show all)", fit) { fit = true }
                                    SpChip("Fill (cover)", !fit) { fit = false }
                                }
                                SpHint("Fit keeps the whole photo and shows the backdrop in the gaps. Fill covers the canvas and trims the edges.")
                            }
                            // ------------------------------------------------ 2. ASPECT RATIO
                            SpTool.RATIO -> {
                                SpLabel("Canvas ratio")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        val sel = ratioKey == "Original"
                                        SpRatioTile("Original", "", sel) { ratioKey = "Original"; ratioOverride = null }
                                    }
                                    items(ratios) { r ->
                                        SpRatioTile(r.displayName, r.badge, ratioKey == r.displayName) {
                                            ratioKey = r.displayName
                                            ratioOverride = r.widthRatio / r.heightRatio
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpChip("Fit", fit) { fit = true }
                                    SpChip("Fill", !fit) { fit = false }
                                }
                            }
                            // ------------------------------------------------ 3. CROP & ROTATE
                            SpTool.CROP -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpChip("Crop", cropTab == 0) { cropTab = 0 }
                                    SpChip("Rotate", cropTab == 1) { cropTab = 1 }
                                }
                                if (cropTab == 0) {
                                    SpLabel("Crop shape")
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(SpCropShape.entries) { s ->
                                            SpChip(s.label, cropShape == s) {
                                                cropShape = s
                                                s.ratio?.let { crop = spCenteredCrop(it, imgAspect) }
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SpChip("Reset crop", false) { crop = SpCrop(); cropShape = SpCropShape.FREE }
                                    }
                                    SpHint("Drag the corners to crop, drag inside to move the frame.")
                                } else {
                                    SpSliderRow("Rotate: ${fineAngle.toInt()}°", fineAngle, -45f..45f) { fineAngle = it }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SpChip("Rotate 90°", false) { quarterTurns = (quarterTurns + 1) % 4 }
                                        SpChip("Flip ↔", flipH) { flipH = !flipH }
                                        SpChip("Flip ↕", flipV) { flipV = !flipV }
                                    }
                                    SpLabel("Correction")
                                    SpSliderRow("Horizontal scale: ${scaleH.toInt()}%", scaleH, 50f..150f) { scaleH = it }
                                    SpSliderRow("Vertical scale: ${scaleV.toInt()}%", scaleV, 50f..150f) { scaleV = it }
                                    SpSliderRow("Center scale: ${centerScale.toInt()}%", centerScale, 50f..150f) { centerScale = it }
                                    SpChip("Reset rotation", false) {
                                        fineAngle = 0f; quarterTurns = 0; flipH = false; flipV = false
                                        scaleH = 100f; scaleV = 100f; centerScale = 100f
                                    }
                                }
                            }
                            // ------------------------------------------------ 4. FILTERS
                            SpTool.FILTER -> FilterPickerPanel(
                                previewUri = photoUri,
                                selectedPreset = filterPreset,
                                intensity = filterIntensity,
                                onPresetSelected = { p ->
                                    filterPreset = p
                                    filterIntensity = if (p == "Original") 0f else 100f
                                },
                                onIntensityChange = { filterIntensity = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                            // ------------------------------------------------ 4b. ADJUST
                            SpTool.ADJUST -> {
                                val def = AdjustTools.def(adjustItem)
                                val cur = adjustValues[adjustItem] ?: 0f
                                Row(
                                    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "$adjustItem: ${if (cur > 0 && def.isBipolar) "+" else ""}${cur.toInt()}%",
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)
                                    )
                                    Text("Reset all", fontSize = 12.sp, color = Color(0xFF2563EB),
                                        modifier = Modifier.clickable { adjustValues = emptyMap() })
                                }
                                FluidSlider(
                                    value = cur,
                                    onValueChange = { adjustValues = adjustValues + (adjustItem to it) },
                                    valueRange = def.min..def.max,
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(AdjustTools.groups) { g ->
                                        SpChip(g, adjustGroup == g) {
                                            adjustGroup = g
                                            AdjustTools.inGroup(g).firstOrNull()?.let { adjustItem = it.name }
                                        }
                                    }
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(AdjustTools.inGroup(adjustGroup)) { d ->
                                        val active = (adjustValues[d.name] ?: 0f) != 0f
                                        SpChip(d.name + if (active) " •" else "", adjustItem == d.name) { adjustItem = d.name }
                                    }
                                }
                            }
                            // ------------------------------------------------ 5. BLUR + OVERLAY CIRCLE
                            SpTool.BLUR -> {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("Spotlight blur", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                    Switch(checked = blurOn, onCheckedChange = { blurOn = it })
                                }
                                if (blurOn) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SpChip("Keep circle sharp", !blurInside) { blurInside = false }
                                        SpChip("Blur inside circle", blurInside) { blurInside = true }
                                    }
                                    SpSliderRow("Blur amount: ${blurAmount.toInt()}%", blurAmount, 5f..100f) { blurAmount = it }
                                    SpSliderRow("Circle size: ${(blurR * 100).toInt()}", blurR * 100f, 8f..70f) { blurR = it / 100f }
                                    SpSliderRow("Edge softness: ${(blurFeather * 100).toInt()}%", blurFeather * 100f, 0f..100f) { blurFeather = it / 100f }
                                    SpHint("Drag the circle to move it, pinch or use the corner handle to resize.")
                                } else {
                                    SpHint("Turn this on to place a circle over the photo. Everything outside it is blurred (or inside, if you flip the mode).")
                                }
                            }
                            // ------------------------------------------------ 6. STICKERS
                            SpTool.STICKERS -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpStickers.keys.forEach { c -> SpChip(c, stickerCategory == c) { stickerCategory = c } }
                                }
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(6),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(130.dp)
                                ) {
                                    gridItems(SpStickers[stickerCategory].orEmpty()) { e ->
                                        Box(
                                            Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                                                .background(Color.White).border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                                .clickable {
                                                    val n = newFreestyleEmoji(e)
                                                    overlays = overlays + n
                                                    selectedId = n.id
                                                },
                                            contentAlignment = Alignment.Center
                                        ) { Text(e, fontSize = 22.sp) }
                                    }
                                }
                            }
                            // ------------------------------------------------ 7. TEXT
                            SpTool.TEXT -> {
                                val st = selected?.takeIf { it.kind == FreestyleItemKind.TEXT }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("+ Add Text", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B),
                                        modifier = Modifier.clickable { editingTextId = null; textDialogOpen = true })
                                    if (st != null) Text("Edit text", fontSize = 13.sp, color = Color(0xFF2563EB),
                                        modifier = Modifier.clickable { editingTextId = st.id; textDialogOpen = true })
                                }
                                if (st != null) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(SpFonts) { f -> SpChip(f, st.fontName == f) { updateOverlay(st.id) { it.copy(fontName = f) } } }
                                    }
                                    SpColorRow(Color(st.colorLong.toInt())) { c ->
                                        updateOverlay(st.id) { it.copy(colorLong = c.toArgb().toLong() and 0xFFFFFFFFL) }
                                    }
                                    SpSliderRow("Size: ${(st.fontSizeRatio * 100).toInt()}", st.fontSizeRatio * 100f, 3f..20f) { v ->
                                        updateOverlay(st.id) { it.copy(fontSizeRatio = v / 100f) }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SpChip("Background pill", st.textPill) { updateOverlay(st.id) { it.copy(textPill = !it.textPill) } }
                                        SpChip("Drop shadow", st.textShadow) { updateOverlay(st.id) { it.copy(textShadow = !it.textShadow) } }
                                    }
                                } else {
                                    SpHint("Add text, then tap it on the photo to change font, color, size, pill and shadow. Drag to move.")
                                }
                            }
                            // ------------------------------------------------ 8. BACKGROUND
                            SpTool.BACKGROUND -> {
                                SpLabel("Solid colors")
                                SpColorRow(if (bgGradient.isEmpty() && !bgBlurPhoto) bgColor else null) {
                                    bgColor = it; bgGradient = emptyList(); bgBlurPhoto = false
                                }
                                SpLabel("Gradients")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    items(SpGradients) { g ->
                                        Box(
                                            Modifier.size(32.dp).clip(CircleShape)
                                                .background(Brush.linearGradient(g.map { Color(it.toInt()) }))
                                                .border(if (bgGradient == g) 2.5.dp else 1.dp, if (bgGradient == g) Color(0xFF2563EB) else Color(0xFFCBD5E1), CircleShape)
                                                .clickable { bgGradient = g; bgBlurPhoto = false }
                                        )
                                    }
                                }
                                SpLabel("Wallpaper texture")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(BackgroundTexture.entries) { t -> SpChip(t.label, bgTexture == t) { bgTexture = t } }
                                }
                                SpLabel("Blurred photo background")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpChip("Use blurred photo", bgBlurPhoto) { bgBlurPhoto = !bgBlurPhoto; if (bgBlurPhoto) bgGradient = emptyList() }
                                }
                                if (bgBlurPhoto) SpSliderRow("Blur: ${bgBlur.toInt()}%", bgBlur, 0f..100f) { bgBlur = it }
                            }
                            // ------------------------------------------------ 9. DRAW
                            SpTool.DRAW -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    SpChip("Pen", !eraser) { eraser = false }
                                    SpChip("Eraser", eraser) { eraser = true }
                                    SpChip("Undo", false) { if (strokes.isNotEmpty()) strokes = strokes.dropLast(1) }
                                    SpChip("Clear", false) { strokes = emptyList() }
                                }
                                SpSliderRow("Brush size: ${brushSize.toInt()}", brushSize, 2f..40f) { brushSize = it }
                                SpSliderRow("Opacity: ${brushOpacity.toInt()}%", brushOpacity, 10f..100f) { brushOpacity = it }
                                SpColorRow(brushColor) { brushColor = it; eraser = false }
                            }
                        }
                    }
                }
                // ---- tool bar
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpTool.entries.forEach { t ->
                        val sel = tool == t
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (sel) Color(0xFFEFF6FF) else Color.Transparent)
                                .clickable { tool = t; if (t != SpTool.TEXT && t != SpTool.STICKERS) selectedId = null }
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
            val cw = minOf(maxWidth, maxHeight * canvasRatio)
            val ch = cw / canvasRatio
            val W = canvasSize.width.toFloat()
            val H = canvasSize.height.toFloat()

            Box(
                Modifier
                    .size(cw, ch)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(tool, cropTab) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()

                            // ------------------------- freehand drawing / eraser
                            if (tool == SpTool.DRAW) {
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

                            // ------------------------- crop frame (stage shows the full photo)
                            if (tool == SpTool.CROP && cropTab == 0) {
                                val hit = 28.dp.toPx()
                                val c0 = crop
                                val corners = listOf(
                                    Offset(c0.l * w, c0.t * h), Offset(c0.r * w, c0.t * h),
                                    Offset(c0.l * w, c0.b * h), Offset(c0.r * w, c0.b * h)
                                )
                                val ci = corners.indexOfFirst { (it - down.position).getDistance() < hit }
                                down.consume()
                                if (ci >= 0) {
                                    drag(down.id) { pc ->
                                        crop = spResizeCrop(crop, ci, Offset(pc.position.x / w, pc.position.y / h), cropShape.ratio, imgAspect)
                                        pc.consume()
                                    }
                                } else if (down.position.x / w in c0.l..c0.r && down.position.y / h in c0.t..c0.b) {
                                    drag(down.id) { pc ->
                                        val d = pc.positionChange()
                                        crop = spMoveCrop(crop, d.x / w, d.y / h)
                                        pc.consume()
                                    }
                                }
                                return@awaitEachGesture
                            }

                            // ------------------------- spotlight circle: move / pinch / corner-handle resize
                            if (tool == SpTool.BLUR) {
                                if (!blurOn) return@awaitEachGesture
                                val center = Offset(blurCx * w, blurCy * h)
                                val rad = blurR * w
                                val handle = center + Offset(rad * 0.7071f, -rad * 0.7071f)
                                val hitR = 28.dp.toPx()
                                if ((down.position - handle).getDistance() < hitR) {
                                    down.consume()
                                    drag(down.id) { pc ->
                                        val c = Offset(blurCx * w, blurCy * h)
                                        blurR = ((pc.position - c).getDistance() / 0.7071f / w).coerceIn(0.05f, 0.9f)
                                        pc.consume()
                                    }
                                    return@awaitEachGesture
                                }
                                if ((down.position - center).getDistance() < rad + hitR / 2f) {
                                    down.consume()
                                    do {
                                        val ev = awaitPointerEvent()
                                        val zoom = ev.calculateZoom()
                                        val pan = ev.calculatePan()
                                        blurR = (blurR * zoom).coerceIn(0.05f, 0.9f)
                                        blurCx = (blurCx + pan.x / w).coerceIn(0f, 1f)
                                        blurCy = (blurCy + pan.y / h).coerceIn(0f, 1f)
                                        ev.changes.forEach { if (it.positionChanged()) it.consume() }
                                    } while (ev.changes.any { it.pressed })
                                }
                                return@awaitEachGesture
                            }

                            // ------------------------- handles of the selected overlay
                            val margin = 16.dp.toPx()
                            val hitR = 26.dp.toPx()
                            val sel = overlays.firstOrNull { it.id == selectedId }
                            if (sel != null) {
                                val del = spHandlePos(sel, w, h, -1f, margin)
                                val scl = spHandlePos(sel, w, h, 1f, margin)
                                if ((down.position - del).getDistance() < hitR) {
                                    down.consume()
                                    val up = waitForUpOrCancellation()
                                    if (up != null) {
                                        overlays = overlays.filter { it.id != sel.id }
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
                                        updateOverlay(sel.id) { it.copy(scale = ns, rotation = sel.rotation + dr) }
                                        change.consume()
                                    }
                                    return@awaitEachGesture
                                }
                            }

                            // ------------------------- pick an overlay (top-most first)
                            val hit = overlays.lastOrNull { spHitTest(it, down.position, w, h) }
                            if (hit == null) {
                                selectedId = null
                                return@awaitEachGesture
                            }
                            selectedId = hit.id
                            val cur = overlays
                            overlays = cur.filter { it.id != hit.id } + cur.first { it.id == hit.id }
                            down.consume()

                            // ------------------------- 1-finger drag, 2-finger pinch + rotate
                            do {
                                val ev = awaitPointerEvent()
                                val zoom = ev.calculateZoom()
                                val rot = ev.calculateRotation()
                                val pan = ev.calculatePan()
                                if (zoom != 1f || rot != 0f || pan != Offset.Zero) {
                                    updateOverlay(hit.id) {
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
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp))) {
                    // ---------------- backdrop
                    when {
                        bgBlurPhoto -> AsyncImage(
                            model = photoUri, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                                .blur(with(density) { (bgBlur / 100f * 0.05f * W).toDp() }, BlurredEdgeTreatment.Unbounded)
                        )
                        bgGradient.size >= 2 -> Box(Modifier.fillMaxSize().background(Brush.linearGradient(bgGradient.map { Color(it.toInt()) })))
                        else -> Box(Modifier.fillMaxSize().background(bgColor))
                    }
                    Canvas(Modifier.fillMaxSize()) { spDrawTexture(bgTexture) }

                    if (W > 0f && H > 0f) {
                        // ---------------- the photo (full image while cropping, edited result otherwise)
                        val photoLayer: @Composable (Boolean) -> Unit = { blurred ->
                            val blurDp = with(density) { (blurAmount / 100f * 0.04f * W).toDp() }
                            Box(
                                Modifier.fillMaxSize().then(
                                    if (blurred) Modifier.blur(blurDp, BlurredEdgeTreatment.Unbounded) else Modifier
                                )
                            ) {
                                if (editingCrop) {
                                    SpPhotoLayer(
                                        photoUri, SpCrop(), imgAspect, true, 0, 0f, false, false, 1f, 1f, 1f,
                                        W, H, photoFilter, vignette
                                    ) { a -> if (abs(a - imgAspect) > 0.001f) imgAspect = a }
                                } else {
                                    SpPhotoLayer(
                                        photoUri, crop, imgAspect, fit, quarterTurns, fineAngle, flipH, flipV,
                                        scaleH / 100f, scaleV / 100f, centerScale / 100f, W, H, photoFilter, vignette
                                    ) { a -> if (abs(a - imgAspect) > 0.001f) imgAspect = a }
                                }
                            }
                        }

                        val spot = blurOn && !editingCrop
                        if (!spot) {
                            photoLayer(false)
                        } else {
                            val cx = blurCx * W
                            val cy = blurCy * H
                            val rr = blurR * W
                            if (blurInside) {
                                photoLayer(false)
                                Box(Modifier.fillMaxSize().spCircleMask(cx, cy, rr, blurFeather)) { photoLayer(true) }
                            } else {
                                photoLayer(true)
                                Box(Modifier.fillMaxSize().spCircleMask(cx, cy, rr, blurFeather)) { photoLayer(false) }
                            }
                        }

                        if (!editingCrop) {
                            // ---------------- text + stickers
                            overlays.forEach { item ->
                                key(item.id) {
                                    SpOverlayView(item, W, H, item.id == selectedId && tool != SpTool.DRAW)
                                }
                            }
                            // ---------------- doodles
                            Canvas(Modifier.fillMaxSize()) {
                                strokes.forEach { s ->
                                    if (!s.isEraser) spDrawStroke(s.points.map { Offset(it.x, it.y) }, Color(s.colorLong.toInt()), s.strokeWidthRatio)
                                }
                                if (!eraser && currentStroke.isNotEmpty()) {
                                    spDrawStroke(currentStroke, brushColor.copy(alpha = brushOpacity / 100f), brushSize / 1000f)
                                }
                            }
                        }

                        // ---------------- crop frame overlay
                        if (editingCrop) {
                            Canvas(Modifier.fillMaxSize()) {
                                val l = crop.l * size.width; val t = crop.t * size.height
                                val r = crop.r * size.width; val b = crop.b * size.height
                                val dim = Color(0x99000000)
                                drawRect(dim, Offset(0f, 0f), androidx.compose.ui.geometry.Size(size.width, t))
                                drawRect(dim, Offset(0f, b), androidx.compose.ui.geometry.Size(size.width, size.height - b))
                                drawRect(dim, Offset(0f, t), androidx.compose.ui.geometry.Size(l, b - t))
                                drawRect(dim, Offset(r, t), androidx.compose.ui.geometry.Size(size.width - r, b - t))
                                drawRect(Color.White, Offset(l, t), androidx.compose.ui.geometry.Size(r - l, b - t), style = Stroke(2.dp.toPx()))
                                val thirds = Color(0x88FFFFFF)
                                for (i in 1..2) {
                                    drawLine(thirds, Offset(l + (r - l) * i / 3f, t), Offset(l + (r - l) * i / 3f, b), 1.dp.toPx())
                                    drawLine(thirds, Offset(l, t + (b - t) * i / 3f), Offset(r, t + (b - t) * i / 3f), 1.dp.toPx())
                                }
                                listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach {
                                    drawCircle(Color.White, 8.dp.toPx(), it)
                                    drawCircle(Color(0xFF2563EB), 5.dp.toPx(), it)
                                }
                            }
                        }

                        // ---------------- spotlight circle guide
                        if (tool == SpTool.BLUR && blurOn) {
                            Canvas(Modifier.fillMaxSize()) {
                                val c = Offset(blurCx * size.width, blurCy * size.height)
                                val rad = blurR * size.width
                                drawCircle(
                                    Color.White, rad, c,
                                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f)))
                                )
                                val hp = c + Offset(rad * 0.7071f, -rad * 0.7071f)
                                drawCircle(Color.White, 10.dp.toPx(), hp)
                                drawCircle(Color(0xFF2563EB), 7.dp.toPx(), hp)
                            }
                        }
                    }
                }

                // ---------------- overlay transform handles (outside the clip so they stay visible)
                val s = selected
                if (s != null && W > 0f && !editingCrop && tool != SpTool.DRAW && tool != SpTool.BLUR) {
                    val m = with(density) { 16.dp.toPx() }
                    SpHandle(spHandlePos(s, W, H, -1f, m), Color(0xFFEF4444), Icons.Default.Close, "Delete")
                    SpHandle(spHandlePos(s, W, H, 1f, m), Color(0xFF2563EB), Icons.Default.OpenInFull, "Scale and rotate")
                }
            }
        }
    }

    // =============================================================== add / edit text
    if (textDialogOpen) {
        val eid = editingTextId
        var temp by remember(eid) { mutableStateOf(overlays.firstOrNull { it.id == eid }?.content ?: "") }
        AlertDialog(
            onDismissRequest = { textDialogOpen = false },
            title = { Text(if (eid == null) "Add text" else "Edit text") },
            text = { OutlinedTextField(value = temp, onValueChange = { temp = it }, singleLine = true, label = { Text("Text") }) },
            confirmButton = {
                Button(onClick = {
                    if (temp.isNotBlank()) {
                        if (eid != null) updateOverlay(eid) { it.copy(content = temp) }
                        else {
                            val n = newFreestyleText(temp)
                            overlays = overlays + n
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
            UnifiedExportScreen(
                previewAspect = (ratioOverride ?: visualCroppedRatio).coerceIn(0.2f, 5f),
                shareFallbackUri = photoUri,
                onBack = { showExport = false },
                onExport = { longSide, format, onResult ->
                    selectedId = null
                    exporter.export(buildEdit(), longSide, format) { result -> onResult(result) }
                }
            ) {
                // before saving: the photo being edited; after saving: the real exported file
                coil3.compose.AsyncImage(
                    model = photoUri,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // =============================================================== export dialog (legacy, switched off)
    if (showExport && USE_LEGACY_EXPORT_DIALOG) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showExport = false },
            title = { Text("Save photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpLabel("Quality")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("HD" to 1080, "2K" to 2048, "4K" to 3840).forEach { (n, px) ->
                            SpChip(n, exportQuality == px) { exportQuality = px }
                        }
                    }
                    SpLabel("Format")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("JPG", "PNG").forEach { f -> SpChip(f, exportFormat == f) { exportFormat = f } }
                    }
                    if (isSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(enabled = !isSaving, onClick = {
                    isSaving = true
                    selectedId = null
                    exporter.export(buildEdit(), exportQuality, exportFormat) { result ->
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

@Composable
private fun SpRatioTile(name: String, badge: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.width(76.dp).height(56.dp).clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFFEFF6FF) else Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) Color(0xFF2563EB) else Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFF1D4ED8) else Color(0xFF1E293B))
            if (badge.isNotEmpty()) Text(badge, fontSize = 9.sp, color = Color(0xFF64748B))
        }
    }
}