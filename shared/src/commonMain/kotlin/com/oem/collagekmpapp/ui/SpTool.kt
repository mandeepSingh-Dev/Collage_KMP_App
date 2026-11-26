package com.oem.collagekmpapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.oem.collagekmpapp.rememberPhotoRequest
import com.oem.collagekmpapp.BackgroundTexture
import com.oem.collagekmpapp.AdjustTools
import com.oem.collagekmpapp.FilterPickerPanel
import com.oem.collagekmpapp.FilteredPhoto
import com.oem.collagekmpapp.GlamourPresets
import com.oem.collagekmpapp.GlamourState
import com.oem.collagekmpapp.LensBlurMode
import com.oem.collagekmpapp.LensBlurState
import com.oem.collagekmpapp.PerspectiveFill
import com.oem.collagekmpapp.PerspectiveMode
import com.oem.collagekmpapp.PerspectiveState
import com.oem.collagekmpapp.PhotoEffects
import com.oem.collagekmpapp.PhotoToolsState
import com.oem.collagekmpapp.SELECTIVE_MAX_POINTS
import com.oem.collagekmpapp.SELECTIVE_MAX_RADIUS
import com.oem.collagekmpapp.SELECTIVE_MIN_RADIUS
import com.oem.collagekmpapp.SelectivePoint
import com.oem.collagekmpapp.VintageState
import com.oem.collagekmpapp.VintageTint
import com.oem.collagekmpapp.perspectiveMatrix
import com.oem.collagekmpapp.FreestyleItem
import com.oem.collagekmpapp.FreestyleItemKind
import com.oem.collagekmpapp.FreestyleProject
import com.oem.collagekmpapp.PhotoColorEngine
import com.oem.collagekmpapp.USE_LEGACY_EXPORT_DIALOG
import com.oem.collagekmpapp.UndoRedoActions
import com.oem.collagekmpapp.BackHandler
import com.oem.collagekmpapp.UnifiedExportScreen
import com.oem.collagekmpapp.components.FluidSlider
import com.oem.collagekmpapp.model.*
import com.oem.collagekmpapp.newFreestyleEmoji
import com.oem.collagekmpapp.newFreestyleText
import com.oem.collagekmpapp.rememberEditHistory
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// =====================================================================================
// Models
// =====================================================================================

private enum class SpTool(val label: String, val icon: ImageVector) {
    RESIZE("Resize", Icons.Default.PhotoSizeSelectLarge),
    RATIO("Ratio", Icons.Default.AspectRatio),
    CROP("Crop", Icons.Default.CropRotate),
    FILTER("Filter", Icons.Default.Star),
    ADJUST("Adjust", Icons.Default.Tune),
    SELECTIVE("Selective", Icons.Default.TouchApp),
    VINTAGE("Vintage", Icons.Default.FilterVintage),
    GLAMOUR("Glamour", Icons.Default.AutoAwesome),
    BLUR("Blur", Icons.Default.BlurOn),
    LENS("Lens Blur", Icons.Default.BlurCircular),
    PERSPECTIVE("Perspective", Icons.Default.Transform),
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

/**
 * Maps between canvas pixels and the cropped photo box (normalised 0..1 in the photo's OWN axes) using the same
 * transform order as the preview / exporter: box -> centre scale -> rotate -> flip, all about the canvas centre.
 * Selective points and the lens-blur centre live in box space so they follow the photo when it is rotated / scaled.
 */
private class SpGeo(
    val w: Float, val h: Float,          // canvas px
    val dw: Float, val dh: Float,        // un-rotated cropped box px
    val angle: Float,                    // fine angle + quarter turns (degrees)
    val sx: Float, val sy: Float,        // centre scale * H / V scale
    val flipH: Boolean, val flipV: Boolean
) {
    fun toCanvas(n: Offset): Offset {
        var x = (n.x - 0.5f) * dw * sx
        var y = (n.y - 0.5f) * dh * sy
        val r = spRad(angle)
        val rx = x * cos(r) - y * sin(r)
        val ry = x * sin(r) + y * cos(r)
        x = if (flipH) -rx else rx
        y = if (flipV) -ry else ry
        return Offset(w / 2f + x, h / 2f + y)
    }

    fun toBox(p: Offset): Offset {
        var x = p.x - w / 2f
        var y = p.y - h / 2f
        if (flipH) x = -x
        if (flipV) y = -y
        val r = -spRad(angle)
        val ux = x * cos(r) - y * sin(r)
        val uy = x * sin(r) + y * cos(r)
        return Offset(ux / sx / dw + 0.5f, uy / sy / dh + 0.5f)
    }

    /** A canvas-space finger movement expressed in box-normalised units. */
    fun panToBox(pan: Offset): Offset {
        val c = Offset(w / 2f, h / 2f)
        return toBox(c + pan) - toBox(c)
    }

    /** Canvas px that one box-width occupies (uniform approximation of the H / V scales). */
    val pxPerBoxWidth: Float get() = dw * sqrt(sx * sy)
}

/** Projective warp of everything this modifier wraps (the photo layer), see PerspectiveState / perspectiveMatrix. */
private fun Modifier.spPerspective(m: Matrix): Modifier = this.drawWithContent {
    withTransform({ transform(m) }) { this@drawWithContent.drawContent() }
}

private enum class SelParam(val label: String, val icon: ImageVector, val short: String) {
    BRIGHTNESS("Brightness", Icons.Default.WbSunny, "B"),
    CONTRAST("Contrast", Icons.Default.Contrast, "C"),
    SATURATION("Saturation", Icons.Default.Palette, "S"),
    STRUCTURE("Structure", Icons.Default.Texture, "St")
}

/** "+15" / "-8" / "0" for the pin badges. */
private fun spSigned(v: Float): String {
    val n = v.roundToInt()
    return if (n > 0) "+$n" else "$n"
}

/** Angle with 0.5° precision, e.g. "-12.5°" / "3°". */
private fun spFmtAngle(a: Float): String {
    val halves = (a * 2f).roundToInt()
    val whole = halves / 2
    val sign = if (halves < 0 && whole == 0) "-" else ""
    return if (halves % 2 == 0) "$sign$whole°" else "$sign$whole.5°"
}

/**
 * Trash badge of a selective pin: on the ring at 45° (top-right), but never closer to the centre than [minPx]
 * so it can't overlap the bullseye on small rings. Shared by drawing and hit-testing.
 */
private fun spPinTrashPos(center: Offset, ringPx: Float, minPx: Float): Offset {
    val r = maxOf(ringPx, minPx)
    return center + Offset(r * 0.7071f, -r * 0.7071f)
}

private fun SelectivePoint.valueOf(p: SelParam): Float = when (p) {
    SelParam.BRIGHTNESS -> brightness
    SelParam.CONTRAST -> contrast
    SelParam.SATURATION -> saturation
    SelParam.STRUCTURE -> structure
}

private fun SelectivePoint.withValue(p: SelParam, v: Float): SelectivePoint = when (p) {
    SelParam.BRIGHTNESS -> copy(brightness = v)
    SelParam.CONTRAST -> copy(contrast = v)
    SelParam.SATURATION -> copy(saturation = v)
    SelParam.STRUCTURE -> copy(structure = v)
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
private fun SpHint(text: String) = Text(text, fontSize = 12.sp, color = Color(0xFF64748B))

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

/**
 * Fine rotation wheel: a horizontal ruler (−45°…+45°) that you drag like a dial; output snaps to 0.5°.
 * Ticks every 1°, mid ticks every 5°, labelled majors every 15°; a haptic tick at every 5° and when crossing 0°.
 * [onActive] is true while the finger is on the wheel (drives the straightening grid).
 */
@Composable
private fun SpRotationWheel(
    angle: Float,
    onAngle: (Float) -> Unit,
    onActive: (Boolean) -> Unit,
    onNudge: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val latestAngle by rememberUpdatedState(angle)
    val latestOnAngle by rememberUpdatedState(onAngle)
    val latestOnActive by rememberUpdatedState(onActive)
    val pxPerDeg = with(LocalDensity.current) { 7.dp.toPx() }
    val measurer = rememberTextMeasurer()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Straighten: ${spFmtAngle(angle)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpChip("−0.5°", false) { onNudge(-0.5f) }
                SpChip("+0.5°", false) { onNudge(0.5f) }
                SpChip("0° Reset", angle == 0f) { onNudge(-angle) }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .pointerInput(Unit) {
                    var raw = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { raw = latestAngle; latestOnActive(true) },
                        onDragEnd = { latestOnActive(false) },
                        onDragCancel = { latestOnActive(false) },
                        onHorizontalDrag = { change, dx ->
                            change.consume()
                            val before = latestAngle
                            // ruler follows the finger: dragging left turns the photo clockwise (+)
                            raw = (raw - dx / pxPerDeg).coerceIn(-45f, 45f)
                            val snapped = (raw * 2f).roundToInt() / 2f
                            if (snapped != before) {
                                val crossedZero = (before < 0f && snapped >= 0f) || (before > 0f && snapped <= 0f)
                                if (crossedZero || (snapped % 5f == 0f)) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                latestOnAngle(snapped)
                            }
                        }
                    )
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val bottom = size.height - 6.dp.toPx()
                for (d in -45..45) {
                    val x = cx + (d - angle) * pxPerDeg
                    if (x < -20f || x > size.width + 20f) continue
                    val major = d % 15 == 0
                    val mid = d % 5 == 0
                    val len = size.height * when { major -> 0.42f; mid -> 0.30f; else -> 0.18f }
                    val col = if (d == 0) Color(0xFF60A5FA)
                    else Color.White.copy(alpha = if (major) 0.9f else if (mid) 0.6f else 0.35f)
                    drawLine(col, Offset(x, bottom - len), Offset(x, bottom), if (major) 1.6.dp.toPx() else 1.dp.toPx())
                    if (major) {
                        val label = measurer.measure(AnnotatedString(if (d > 0) "+$d" else "$d"), style = TextStyle(fontSize = 9.sp, color = col))
                        drawText(label, topLeft = Offset(x - label.size.width / 2f, 3.dp.toPx()))
                    }
                }
                // fixed centre needle
                drawLine(Color(0xFF3B82F6), Offset(cx, 4.dp.toPx()), Offset(cx, size.height - 2.dp.toPx()), 2.5.dp.toPx(), cap = StrokeCap.Round)
                // soft edge fades so the ruler reads as a wheel
                val fade = size.width * 0.18f
                drawRect(Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color.Transparent), startX = 0f, endX = fade), size = Size(fade, size.height))
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF0F172A)), startX = size.width - fade, endX = size.width),
                    topLeft = Offset(size.width - fade, 0f), size = Size(fade, size.height)
                )
            }
        }
    }
}

/** Dense 9 x 9 straightening grid: dark halo + bright line so it reads on any photo. */
private fun DrawScope.spDrawStraightenGrid(alpha: Float) {
    val halo = Color.Black.copy(alpha = 0.45f * alpha)
    val line = Color.White.copy(alpha = 0.9f * alpha)
    val thin = 1.dp.toPx()
    for (i in 1..8) {
        val x = size.width * i / 9f
        val y = size.height * i / 9f
        drawLine(halo, Offset(x, 0f), Offset(x, size.height), thin * 2.2f)
        drawLine(line, Offset(x, 0f), Offset(x, size.height), thin)
        drawLine(halo, Offset(0f, y), Offset(size.width, y), thin * 2.2f)
        drawLine(line, Offset(0f, y), Offset(size.width, y), thin)
    }
    // centre cross a bit stronger, for horizons
    val accent = Color(0xFF60A5FA).copy(alpha = alpha)
    drawLine(accent, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), thin * 1.4f)
    drawLine(accent, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), thin * 1.4f)
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
    // --- new: full look (filter + adjust + Snapseed tools) through FilteredPhoto, same engine as the exporter.
    //     null effects = legacy path (colour filter + simple vignette only).
    matrix: FloatArray? = null,
    effects: PhotoEffects? = null,
    lens: LensBlurState? = null,
    selective: List<SelectivePoint> = emptyList(),
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
            if (effects != null) {
                FilteredPhoto(
                    uri = uri,
                    matrix = matrix,
                    effects = effects,
                    modifier = Modifier.fillMaxSize(),
                    imageModifier = Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(
                            Constraints.fixed(fullW.roundToInt().coerceAtLeast(1), fullH.roundToInt().coerceAtLeast(1))
                        )
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            p.place(-(crop.l * fullW).roundToInt(), -(crop.t * fullH).roundToInt())
                        }
                    },
                    canvasWidthPx = canvasW,
                    onImageAspect = onAspect,
                    lens = lens,
                    selective = selective
                )
            } else {
                AsyncImage(
                    model = rememberPhotoRequest(uri, 2048),
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
}

// =====================================================================================
// The screen
// =====================================================================================

/** Everything Undo / Redo restores in the Edit Photo editor. */
private data class SpSnap(
    val ratioKey: String, val ratioOverride: Float?, val fit: Boolean,
    val crop: SpCrop, val cropShape: SpCropShape,
    val fineAngle: Float, val quarterTurns: Int, val flipH: Boolean, val flipV: Boolean,
    val scaleH: Float, val scaleV: Float, val centerScale: Float,
    val filterPreset: String, val filterIntensity: Float, val adjustValues: Map<String, Float>,
    val blurOn: Boolean, val blurInside: Boolean, val blurAmount: Float, val blurFeather: Float,
    val blurCx: Float, val blurCy: Float, val blurR: Float,
    val overlays: List<FreestyleItem>,
    val bgColor: Color, val bgGradient: List<Long>, val bgBlurPhoto: Boolean, val bgBlur: Float, val bgTexture: BackgroundTexture,
    val strokes: List<NormalizedDrawStroke>,
    val tools: PhotoToolsState       // selective points, vintage, lens blur, glamour glow, perspective
)

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

    // ---- Snapseed-style tools (selective, vintage, lens blur, glamour glow, perspective) = ONE undoable value
    var tools by remember { mutableStateOf(PhotoToolsState()) }
    // view-only state (not part of Undo / Redo)
    var activePointId by remember { mutableStateOf<String?>(null) }
    var showPoints by remember { mutableStateOf(true) }
    var selParam by remember { mutableStateOf(SelParam.BRIGHTNESS) }
    var pointSeq by remember { mutableStateOf(0) }

    // ---- straightening grid (view-only): visible while the rotation wheel is held, fades out 400 ms after release
    var rotationActive by remember { mutableStateOf(false) }
    var gridVisible by remember { mutableStateOf(false) }
    var gridReleaseKey by remember { mutableStateOf(0) }
    LaunchedEffect(gridReleaseKey) {
        if (gridReleaseKey > 0) {
            delay(400)
            if (!rotationActive) gridVisible = false
        }
    }
    val gridAlpha by animateFloatAsState(
        targetValue = if (gridVisible) 1f else 0f,
        animationSpec = tween(if (gridVisible) 120 else 300),
        label = "straightenGrid"
    )
    /** Wheel pressed / released. */
    fun onRotationActive(active: Boolean) {
        rotationActive = active
        if (active) gridVisible = true else gridReleaseKey += 1
    }
    /** ±0.5° / reset taps: show the grid briefly, then fade like a released drag. */
    fun pulseGrid() {
        gridVisible = true
        gridReleaseKey += 1
    }

    // ---- lens blur ring: true while the finger is moving / pinching the focus zone (shows a live readout)
    var lensAdjusting by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val pinMeasurer = rememberTextMeasurer()
    val trashPainter = rememberVectorPainter(Icons.Default.Delete)

    // ---- export
    var showExport by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var exportQuality by remember { mutableStateOf(2048) }
    var exportFormat by remember { mutableStateOf("JPG") }

    fun updateOverlay(id: String, f: (FreestyleItem) -> FreestyleItem) {
        overlays = overlays.map { if (it.id == id) f(it) else it }
    }

    fun updatePoint(id: String, f: (SelectivePoint) -> SelectivePoint) {
        tools = tools.copy(selective = tools.selective.map { if (it.id == id) f(it) else it })
    }
    fun setVintage(f: (VintageState) -> VintageState) { tools = tools.copy(vintage = f(tools.vintage)) }
    fun setLens(f: (LensBlurState) -> LensBlurState) { tools = tools.copy(lens = f(tools.lens)) }
    fun setGlamour(f: (GlamourState) -> GlamourState) { tools = tools.copy(glamour = f(tools.glamour)) }
    fun setPersp(f: (PerspectiveState) -> PerspectiveState) { tools = tools.copy(perspective = f(tools.perspective)) }

    /** Canvas <-> photo-box mapping for the CURRENT crop / rotation / flip / scale. */
    fun spGeoAt(cw: Float, ch: Float): SpGeo {
        val b = spBoxSize(crop, imgAspect, quarterTurns % 2 != 0, fit, cw, ch)
        return SpGeo(
            cw, ch, b.x, b.y, fineAngle + quarterTurns * 90f,
            centerScale / 100f * scaleH / 100f, centerScale / 100f * scaleV / 100f, flipH, flipV
        )
    }

    val photoFilter = remember(filterPreset, filterIntensity, adjustValues) {
        PhotoColorEngine.buildMatrix(filterPreset, filterIntensity, adjustValues)
            ?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
    }
    val vignette = remember(filterPreset, filterIntensity, adjustValues) {
        PhotoColorEngine.effects(filterPreset, filterIntensity, adjustValues).vignette
    }
    // full look incl. Vintage / Glamour colour + bloom, Structure, Sharpening ... (same engine as the exporter)
    val photoMatrix = remember(filterPreset, filterIntensity, adjustValues, tools.vintage, tools.glamour) {
        PhotoColorEngine.buildMatrix(filterPreset, filterIntensity, adjustValues, tools)
    }
    val photoFx = remember(filterPreset, filterIntensity, adjustValues, tools.vintage, tools.glamour) {
        PhotoColorEngine.effects(filterPreset, filterIntensity, adjustValues, tools)
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
            project = overlayProject,
            tools = tools
        )
    }

    // ---------------------------------------------------------------- Undo / Redo
    val history = rememberEditHistory(
        current = SpSnap(
            ratioKey, ratioOverride, fit, crop, cropShape, fineAngle, quarterTurns, flipH, flipV,
            scaleH, scaleV, centerScale, filterPreset, filterIntensity, adjustValues,
            blurOn, blurInside, blurAmount, blurFeather, blurCx, blurCy, blurR,
            overlays, bgColor, bgGradient, bgBlurPhoto, bgBlur, bgTexture, strokes, tools
        )
    ) { snap ->
        ratioKey = snap.ratioKey; ratioOverride = snap.ratioOverride; fit = snap.fit
        crop = snap.crop; cropShape = snap.cropShape
        fineAngle = snap.fineAngle; quarterTurns = snap.quarterTurns; flipH = snap.flipH; flipV = snap.flipV
        scaleH = snap.scaleH; scaleV = snap.scaleV; centerScale = snap.centerScale
        filterPreset = snap.filterPreset; filterIntensity = snap.filterIntensity; adjustValues = snap.adjustValues
        blurOn = snap.blurOn; blurInside = snap.blurInside; blurAmount = snap.blurAmount; blurFeather = snap.blurFeather
        blurCx = snap.blurCx; blurCy = snap.blurCy; blurR = snap.blurR
        overlays = snap.overlays
        bgColor = snap.bgColor; bgGradient = snap.bgGradient; bgBlurPhoto = snap.bgBlurPhoto
        bgBlur = snap.bgBlur; bgTexture = snap.bgTexture
        strokes = snap.strokes
        tools = snap.tools
        if (activePointId != null && snap.tools.selective.none { it.id == activePointId }) activePointId = null
        currentStroke = emptyList()
        if (selectedId != null && snap.overlays.none { it.id == selectedId }) selectedId = null
    }

    // ---------------------------------------------------------------- per-tool Reset
    // "dirty" = the open tool has changes; Reset puts just that tool back to its default (each step is undoable).
    val defaultText = remember { newFreestyleText("") }
    val selText = selected?.takeIf { it.kind == FreestyleItemKind.TEXT }
    val toolIsDirty: Boolean = when (tool) {
        SpTool.RESIZE, SpTool.RATIO -> ratioKey != "Original" || ratioOverride != null || !fit
        SpTool.CROP -> crop != SpCrop() || cropShape != SpCropShape.FREE || fineAngle != 0f || quarterTurns != 0 ||
                flipH || flipV || scaleH != 100f || scaleV != 100f || centerScale != 100f
        SpTool.FILTER -> filterPreset != "Original" || filterIntensity != 0f
        SpTool.ADJUST -> adjustValues.any { it.value != 0f }
        SpTool.SELECTIVE -> tools.selective.isNotEmpty()
        SpTool.VINTAGE -> tools.vintage != VintageState()
        SpTool.GLAMOUR -> tools.glamour != GlamourState()
        SpTool.BLUR -> blurOn || blurInside || blurAmount != 60f || blurFeather != 0.35f ||
                blurCx != 0.5f || blurCy != 0.5f || blurR != 0.25f
        SpTool.LENS -> tools.lens != LensBlurState()
        SpTool.PERSPECTIVE -> tools.perspective != PerspectiveState()
        SpTool.STICKERS -> overlays.any { it.kind == FreestyleItemKind.EMOJI }
        SpTool.TEXT -> selText != null && (selText.fontName != defaultText.fontName || selText.colorLong != defaultText.colorLong ||
                selText.fontSizeRatio != defaultText.fontSizeRatio || selText.textPill != defaultText.textPill ||
                selText.textShadow != defaultText.textShadow)
        SpTool.BACKGROUND -> bgGradient.isNotEmpty() || bgBlurPhoto || bgBlur != 100f ||
                bgTexture != BackgroundTexture.NONE || bgColor != Color(0xFFFFFFFF)
        SpTool.DRAW -> strokes.isNotEmpty() || eraser || brushSize != 10f || brushOpacity != 100f || brushColor != Color(0xFFF43F8E)
    }
    val resetTool: () -> Unit = {
        when (tool) {
            SpTool.RESIZE, SpTool.RATIO -> { ratioKey = "Original"; ratioOverride = null; fit = true }
            SpTool.CROP -> {
                crop = SpCrop(); cropShape = SpCropShape.FREE
                fineAngle = 0f; quarterTurns = 0; flipH = false; flipV = false
                scaleH = 100f; scaleV = 100f; centerScale = 100f
            }
            SpTool.FILTER -> { filterPreset = "Original"; filterIntensity = 0f }
            SpTool.ADJUST -> adjustValues = emptyMap()
            SpTool.SELECTIVE -> { tools = tools.copy(selective = emptyList()); activePointId = null }
            SpTool.VINTAGE -> tools = tools.copy(vintage = VintageState())
            SpTool.GLAMOUR -> tools = tools.copy(glamour = GlamourState())
            SpTool.BLUR -> {
                blurOn = false; blurInside = false; blurAmount = 60f; blurFeather = 0.35f
                blurCx = 0.5f; blurCy = 0.5f; blurR = 0.25f
            }
            SpTool.LENS -> tools = tools.copy(lens = LensBlurState())
            SpTool.PERSPECTIVE -> tools = tools.copy(perspective = PerspectiveState())
            SpTool.STICKERS -> {
                overlays = overlays.filter { it.kind != FreestyleItemKind.EMOJI }
                if (selected?.kind == FreestyleItemKind.EMOJI) selectedId = null
            }
            SpTool.TEXT -> selText?.let { st ->
                updateOverlay(st.id) {
                    it.copy(
                        fontName = defaultText.fontName, colorLong = defaultText.colorLong,
                        fontSizeRatio = defaultText.fontSizeRatio, textPill = defaultText.textPill,
                        textShadow = defaultText.textShadow
                    )
                }
            }
            SpTool.BACKGROUND -> {
                bgColor = Color(0xFFFFFFFF); bgGradient = emptyList(); bgBlurPhoto = false
                bgBlur = 100f; bgTexture = BackgroundTexture.NONE
            }
            SpTool.DRAW -> {
                strokes = emptyList(); currentStroke = emptyList(); eraser = false
                brushColor = Color(0xFFF43F8E); brushSize = 10f; brushOpacity = 100f
            }
        }
    }

    // ---------------------------------------------------------------- Unsaved-changes guardrail
    // Top-bar arrow AND system back/gesture: exit directly when nothing is undoable, otherwise confirm first.
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
                title = { Text("Photo Editor", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = requestBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    UndoRedoActions(history)
                    Button(
                        onClick = { showExport = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Save") }
                }
            )
        },
        bottomBar = {
            // navigationBarsPadding: keeps the tool row above the gesture / 3-button bar (edge-to-edge)
            Column(Modifier.background(Color.White).navigationBarsPadding()) {
                Column(
                    Modifier.fillMaxWidth().height(264.dp).background(Color(0xFFF8FAFC))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // universal per-tool Reset
                    Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(tool.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        SpResetButton(enabled = toolIsDirty, onClick = resetTool)
                    }
                    Column(
                        Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                    // fine rotation wheel (0.5° steps) – the straightening grid shows while it's held
                                    SpRotationWheel(
                                        angle = fineAngle,
                                        onAngle = { fineAngle = it },
                                        onActive = { onRotationActive(it) },
                                        onNudge = { d ->
                                            fineAngle = ((((fineAngle + d) * 2f).roundToInt()) / 2f).coerceIn(-45f, 45f)
                                            pulseGrid()
                                        }
                                    )
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
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)
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
                                if (adjustGroup == "Detail") {
                                    SpLabel("Details")
                                    val st = adjustValues["Structure"] ?: 0f
                                    val sh = adjustValues["Sharpen"] ?: 0f
                                    SpSliderRow("Structure (mid-tone local contrast): ${if (st > 0) "+" else ""}${st.toInt()}%", st, -50f..50f) {
                                        adjustValues = adjustValues + ("Structure" to it)
                                    }
                                    SpSliderRow("Sharpening (edge definition): ${sh.toInt()}%", sh, 0f..100f) {
                                        adjustValues = adjustValues + ("Sharpen" to it)
                                    }
                                }
                            }
                            // ------------------------------------------------ 5. BLUR + OVERLAY CIRCLE
                            SpTool.BLUR -> {
                                SpLabel("Spotlight blur")
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpOptionCard("Original", !blurOn, icon = Icons.Default.Block) { blurOn = false }
                                    SpOptionCard("Keep circle sharp", blurOn && !blurInside, icon = Icons.Default.BlurOn) { blurOn = true; blurInside = false }
                                    SpOptionCard("Blur inside circle", blurOn && blurInside, icon = Icons.Default.BlurCircular) { blurOn = true; blurInside = true }
                                }
                                if (blurOn) {
                                    SpSliderRow("Blur amount: ${blurAmount.toInt()}%", blurAmount, 5f..100f) { blurAmount = it }
                                    SpSliderRow("Circle size: ${(blurR * 100).toInt()}", blurR * 100f, 8f..70f) { blurR = it / 100f }
                                    SpSliderRow("Edge softness: ${(blurFeather * 100).toInt()}%", blurFeather * 100f, 0f..100f) { blurFeather = it / 100f }
                                    SpHint("Drag the circle to move it, pinch or use the corner handle to resize.")
                                } else {
                                    SpHint("No blur is applied. Pick a style above to place a circle over the photo.")
                                }
                            }
                            // ------------------------------------------------ 5a. SELECTIVE
                            SpTool.SELECTIVE -> {
                                val ap = tools.selective.firstOrNull { it.id == activePointId }
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    // eye: show / hide the control-point circles (shown by default)
                                    Row(
                                        Modifier.clip(RoundedCornerShape(20.dp))
                                            .background(if (showPoints) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                            .clickable { showPoints = !showPoints }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (showPoints) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (showPoints) "Hide control points" else "Show control points",
                                            tint = if (showPoints) Color.White else Color(0xFF334155),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (showPoints) "Points shown" else "Points hidden", fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (showPoints) Color.White else Color(0xFF334155)
                                        )
                                    }
                                    SpChip("Delete point", false) {
                                        ap?.let { a ->
                                            tools = tools.copy(selective = tools.selective.filter { it.id != a.id })
                                            activePointId = null
                                        }
                                    }
                                    SpChip("Clear all", false) { tools = tools.copy(selective = emptyList()); activePointId = null }
                                }
                                SpLabel("Control points: ${tools.selective.size} / $SELECTIVE_MAX_POINTS")
                                if (ap != null) {
                                    // controller shown in place (no placeholder text while a point is active)
                                    SpSelectiveControls(
                                        point = ap, param = selParam, onParam = { selParam = it },
                                        onValue = { v -> updatePoint(ap.id) { it.withValue(selParam, v) } }
                                    )
                                } else {
                                    SpHint("Tap the photo to drop a control point. Drag a pin to move it, pinch to resize its ring, long-press (or tap its trash badge) to delete.")
                                }
                            }
                            // ------------------------------------------------ 5b. VINTAGE
                            SpTool.VINTAGE -> {
                                val v = tools.vintage
                                SpLabel("Vintage style")
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpOptionCard("Original", !v.on, icon = Icons.Default.Block) { setVintage { it.copy(on = false) } }
                                    VintageTint.entries.forEach { t ->
                                        SpOptionCard(t.label, v.on && v.tint == t, swatch = Color(t.swatch.toInt())) {
                                            setVintage { it.copy(on = true, tint = t) }
                                        }
                                    }
                                }
                                if (v.on) {
                                    SpSliderRow("Brightness: ${v.brightness.toInt()}", v.brightness, -50f..50f) { x -> setVintage { it.copy(brightness = x) } }
                                    SpSliderRow("Saturation: ${v.saturation.toInt()}", v.saturation, -50f..50f) { x -> setVintage { it.copy(saturation = x) } }
                                    SpSliderRow("Style strength: ${v.strength.toInt()}", v.strength, 0f..100f) { x -> setVintage { it.copy(strength = x) } }
                                    SpSliderRow("Vignette strength: ${v.vignette.toInt()}", v.vignette, 0f..100f) { x -> setVintage { it.copy(vignette = x) } }
                                    SpChip("Reset", false) { setVintage { VintageState(on = true, tint = it.tint) } }
                                } else {
                                    SpHint("No vintage effect yet. Pick a Yellow, Green, Blue or Red tint for a faded retro look.")
                                }
                            }
                            // ------------------------------------------------ 5c. GLAMOUR GLOW
                            SpTool.GLAMOUR -> {
                                val g = tools.glamour
                                SpLabel("Glamour glow")
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpOptionCard("Original", !g.on, icon = Icons.Default.Block) { setGlamour { it.copy(on = false) } }
                                    GlamourPresets.all.forEach { p ->
                                        val match = g.on && g.glow == p.glow && g.saturation == p.saturation && g.warmth == p.warmth
                                        SpOptionCard(p.name, match, icon = Icons.Default.AutoAwesome) {
                                            setGlamour { it.copy(on = true, glow = p.glow, saturation = p.saturation, warmth = p.warmth) }
                                        }
                                    }
                                }
                                if (g.on) {
                                    SpSliderRow("Glow: ${g.glow.toInt()}", g.glow, 0f..100f) { x -> setGlamour { it.copy(glow = x) } }
                                    SpSliderRow("Saturation: ${g.saturation.toInt()}", g.saturation, -50f..50f) { x -> setGlamour { it.copy(saturation = x) } }
                                    SpSliderRow("Warmth: ${g.warmth.toInt()}", g.warmth, -50f..50f) { x -> setGlamour { it.copy(warmth = x) } }
                                } else {
                                    SpHint("No glow yet. Pick a preset for a soft-focus, dreamy look.")
                                }
                            }
                            // ------------------------------------------------ 5d. LENS BLUR
                            SpTool.LENS -> {
                                val l = tools.lens
                                SpLabel("Lens blur")
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SpOptionCard("Original", !l.on, icon = Icons.Default.Block) { setLens { it.copy(on = false) } }
                                    LensBlurMode.entries.forEach { m ->
                                        SpOptionCard(
                                            m.label, l.on && l.mode == m,
                                            icon = if (m == LensBlurMode.CIRCULAR) Icons.Default.BlurCircular else Icons.Default.BlurLinear
                                        ) { setLens { it.copy(on = true, mode = m) } }
                                    }
                                }
                                if (l.on) {
                                    SpSliderRow("Clear zone: ${(l.clear * 100).toInt()}", l.clear * 100f, 4f..60f) { x -> setLens { it.copy(clear = x / 100f) } }
                                    SpSliderRow("Transition feather: ${(l.feather * 100).toInt()}", l.feather * 100f, 2f..50f) { x -> setLens { it.copy(feather = x / 100f) } }
                                    SpSliderRow("Blur intensity: ${l.amount.toInt()}", l.amount, 0f..100f) { x -> setLens { it.copy(amount = x) } }
                                    SpHint("Drag on the photo to move the focus ring, pinch to grow / shrink the sharp zone and feather together.")
                                } else {
                                    SpHint("No lens blur yet. Pick Circular or Horizontal to keep a zone sharp and blur the rest.")
                                }
                            }
                            // ------------------------------------------------ 5e. PERSPECTIVE
                            SpTool.PERSPECTIVE -> {
                                val ps = tools.perspective
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(PerspectiveMode.entries) { m -> SpChip(m.label, ps.mode == m) { setPersp { it.copy(mode = m) } } }
                                }
                                when (ps.mode) {
                                    PerspectiveMode.TILT -> {
                                        SpSliderRow("Vertical tilt: ${ps.tiltV.toInt()}", ps.tiltV, -50f..50f) { x -> setPersp { it.copy(tiltV = x) } }
                                        SpSliderRow("Horizontal tilt: ${ps.tiltH.toInt()}", ps.tiltH, -50f..50f) { x -> setPersp { it.copy(tiltH = x) } }
                                    }
                                    PerspectiveMode.ROTATE ->
                                        SpSliderRow("Rotate: ${ps.rotate.toInt()}°", ps.rotate, -45f..45f) { x -> setPersp { it.copy(rotate = x) } }
                                    PerspectiveMode.SCALE ->
                                        SpSliderRow("Scale: ${ps.scale.toInt()}%", ps.scale, 50f..150f) { x -> setPersp { it.copy(scale = x) } }
                                    PerspectiveMode.FREE ->
                                        SpHint("Drag the four corner handles on the photo to bend it freely.")
                                }
                                SpLabel("Fill empty areas")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PerspectiveFill.entries.forEach { f -> SpChip(f.label, ps.fill == f) { setPersp { it.copy(fill = f) } } }
                                    SpChip("Reset", false) { setPersp { PerspectiveState(mode = it.mode, fill = it.fill) } }
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
                                    Text("+ Add Text", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B),
                                        modifier = Modifier.clickable { editingTextId = null; textDialogOpen = true })
                                    if (st != null) Text("Edit text", fontSize = 12.sp, color = Color(0xFF2563EB),
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
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            val cw = minOf(maxWidth, maxHeight * canvasRatio)
            val ch = cw / canvasRatio
            val W = canvasSize.width.toFloat()
            val H = canvasSize.height.toFloat()
            // perspective is the LAST geometric step; Selective / Lens place things in photo space, so the
            // preview shows the un-warped photo while those two tools are open.
            val perspMatrix = remember(tools.perspective, W, H) {
                if (W > 0f && H > 0f) perspectiveMatrix(tools.perspective.quad(W, H), W, H)?.let { Matrix(it) } else null
            }
            val perspActive = perspMatrix != null && !tools.perspective.isIdentity && !editingCrop &&
                    tool != SpTool.SELECTIVE && tool != SpTool.LENS

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

                            // ------------------------- selective: tap = new point, drag = move, pinch = resize circle
                            if (tool == SpTool.SELECTIVE) {
                                val hitR = 36.dp.toPx()
                                val g0 = spGeoAt(w, h)

                                // trash badge on the active pin ring -> delete it
                                val ap0 = tools.selective.firstOrNull { it.id == activePointId }
                                if (showPoints && ap0 != null) {
                                    val tp = spPinTrashPos(g0.toCanvas(Offset(ap0.x, ap0.y)), ap0.radius * g0.pxPerBoxWidth, 34.dp.toPx())
                                    if ((down.position - tp).getDistance() < 20.dp.toPx()) {
                                        down.consume()
                                        if (waitForUpOrCancellation() != null) {
                                            tools = tools.copy(selective = tools.selective.filter { it.id != ap0.id })
                                            activePointId = null
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        return@awaitEachGesture
                                    }
                                }

                                val hitPt = tools.selective
                                    .map { it to (g0.toCanvas(Offset(it.x, it.y)) - down.position).getDistance() }
                                    .minByOrNull { it.second }
                                    ?.takeIf { it.second < hitR }?.first
                                down.consume()
                                if (hitPt != null) {
                                    activePointId = hitPt.id
                                    // tap = select · hold still = long-press delete · move / pinch = drag the pin / resize its ring
                                    val slop = viewConfiguration.touchSlop
                                    var outcome = 0   // 0 = long press (timed out), 1 = released (tap), 2 = moving / pinching
                                    withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                        while (true) {
                                            val ev = awaitPointerEvent()
                                            if (ev.changes.none { it.pressed }) { outcome = 1; break }
                                            if (ev.changes.size >= 2 || (ev.changes[0].position - down.position).getDistance() > slop) {
                                                outcome = 2; break
                                            }
                                        }
                                    }
                                    if (outcome == 0) {
                                        tools = tools.copy(selective = tools.selective.filter { it.id != hitPt.id })
                                        activePointId = null
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        do {
                                            val ev = awaitPointerEvent()
                                            ev.changes.forEach { it.consume() }
                                        } while (ev.changes.any { it.pressed })
                                        return@awaitEachGesture
                                    }
                                    if (outcome == 1) return@awaitEachGesture
                                    do {
                                        val ev = awaitPointerEvent()
                                        val zoom = ev.calculateZoom()
                                        val pan = ev.calculatePan()
                                        if (zoom != 1f || pan != Offset.Zero) {
                                            val d = spGeoAt(w, h).panToBox(pan)
                                            updatePoint(hitPt.id) {
                                                it.copy(
                                                    x = (it.x + d.x).coerceIn(0f, 1f),
                                                    y = (it.y + d.y).coerceIn(0f, 1f),
                                                    radius = (it.radius * zoom).coerceIn(SELECTIVE_MIN_RADIUS, SELECTIVE_MAX_RADIUS)
                                                )
                                            }
                                            ev.changes.forEach { if (it.positionChanged()) it.consume() }
                                        }
                                    } while (ev.changes.any { it.pressed })
                                } else {
                                    var moved = false
                                    var pinched = false
                                    val slop = viewConfiguration.touchSlop
                                    do {
                                        val ev = awaitPointerEvent()
                                        if (ev.changes.size >= 2) {
                                            pinched = true
                                            val zoom = ev.calculateZoom()
                                            val aid = activePointId
                                            if (aid != null && zoom != 1f) {
                                                updatePoint(aid) {
                                                    it.copy(radius = (it.radius * zoom).coerceIn(SELECTIVE_MIN_RADIUS, SELECTIVE_MAX_RADIUS))
                                                }
                                            }
                                        } else if ((ev.changes[0].position - down.position).getDistance() > slop) moved = true
                                        ev.changes.forEach { if (it.positionChanged()) it.consume() }
                                    } while (ev.changes.any { it.pressed })
                                    if (!moved && !pinched && tools.selective.size < SELECTIVE_MAX_POINTS) {
                                        val b = spGeoAt(w, h).toBox(down.position)
                                        if (b.x in 0f..1f && b.y in 0f..1f) {
                                            pointSeq += 1
                                            val np = SelectivePoint(id = "pt$pointSeq", x = b.x, y = b.y)
                                            tools = tools.copy(selective = tools.selective + np)
                                            activePointId = np.id
                                        }
                                    }
                                }
                                return@awaitEachGesture
                            }

                            // ------------------------- lens blur: drag = move the sharp zone, pinch = resize it
                            if (tool == SpTool.LENS) {
                                if (!tools.lens.on) return@awaitEachGesture
                                down.consume()
                                lensAdjusting = true   // live readout + highlighted rings while the finger is down
                                try {
                                    do {
                                        val ev = awaitPointerEvent()
                                        val zoom = ev.calculateZoom()
                                        val pan = ev.calculatePan()
                                        if (zoom != 1f || pan != Offset.Zero) {
                                            val d = spGeoAt(w, h).panToBox(pan)
                                            setLens {
                                                it.copy(
                                                    cx = (it.cx + d.x).coerceIn(0f, 1f),
                                                    cy = (it.cy + d.y).coerceIn(0f, 1f),
                                                    // pinch scales the sharp zone AND the feather ring together
                                                    // (same ranges as the Clear zone / Transition feather sliders)
                                                    clear = (it.clear * zoom).coerceIn(0.04f, 0.6f),
                                                    feather = (it.feather * zoom).coerceIn(0.02f, 0.5f)
                                                )
                                            }
                                            ev.changes.forEach { if (it.positionChanged()) it.consume() }
                                        }
                                    } while (ev.changes.any { it.pressed })
                                } finally {
                                    lensAdjusting = false
                                }
                                return@awaitEachGesture
                            }

                            // ------------------------- perspective (Free mode): drag the four corner handles
                            if (tool == SpTool.PERSPECTIVE) {
                                if (tools.perspective.mode != PerspectiveMode.FREE) return@awaitEachGesture
                                val base = tools.perspective.quad(w, h, withFree = false)
                                val cur = tools.perspective.quad(w, h)
                                var ci = 0
                                var best = Float.MAX_VALUE
                                for (i in 0..3) {
                                    val d = hypot(cur[i * 2] - down.position.x, cur[i * 2 + 1] - down.position.y)
                                    if (d < best) { best = d; ci = i }
                                }
                                if (best > 40.dp.toPx()) return@awaitEachGesture
                                down.consume()
                                drag(down.id) { pc ->
                                    val ox = ((pc.position.x - base[ci * 2]) / w).coerceIn(-0.45f, 0.45f)
                                    val oy = ((pc.position.y - base[ci * 2 + 1]) / h).coerceIn(-0.45f, 0.45f)
                                    setPersp { p ->
                                        val c = p.corners.toMutableList()
                                        c[ci * 2] = ox
                                        c[ci * 2 + 1] = oy
                                        p.copy(corners = c)
                                    }
                                    pc.consume()
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
                            model = rememberPhotoRequest(photoUri, 512),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
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
                                        W, H, photoFilter, vignette,
                                        matrix = photoMatrix, effects = photoFx
                                    ) { a -> if (abs(a - imgAspect) > 0.001f) imgAspect = a }
                                } else {
                                    SpPhotoLayer(
                                        photoUri, crop, imgAspect, fit, quarterTurns, fineAngle, flipH, flipV,
                                        scaleH / 100f, scaleV / 100f, centerScale / 100f, W, H, photoFilter, vignette,
                                        matrix = photoMatrix, effects = photoFx,
                                        lens = tools.lens, selective = tools.selective
                                    ) { a -> if (abs(a - imgAspect) > 0.001f) imgAspect = a }
                                }
                            }
                        }

                        val spot = blurOn && !editingCrop
                        // perspective: fill for the empty areas, then the warped photo layer
                        if (perspActive) {
                            when (tools.perspective.fill) {
                                PerspectiveFill.SMART -> AsyncImage(
                                    model = rememberPhotoRequest(photoUri, 512), contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                        .blur(with(density) { (0.05f * W).toDp() }, BlurredEdgeTreatment.Unbounded)
                                )
                                PerspectiveFill.WHITE -> Box(Modifier.fillMaxSize().background(Color.White))
                                PerspectiveFill.BLACK -> Box(Modifier.fillMaxSize().background(Color.Black))
                            }
                        }
                        Box(
                            Modifier.fillMaxSize().then(
                                if (perspActive && perspMatrix != null) Modifier.spPerspective(perspMatrix) else Modifier
                            )
                        ) {
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

                        // ---------------- straightening grid (fine rotation): 9 x 9, fades out after the wheel is released
                        if (gridAlpha > 0.01f && !editingCrop) {
                            Canvas(Modifier.fillMaxSize()) { spDrawStraightenGrid(gridAlpha) }
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

                        // ---------------- selective control-point circles (the eye button hides them)
                        if (tool == SpTool.SELECTIVE && showPoints) {
                            // Target pin rings: dashed radius ring + bullseye + "B: +15" badge (current parameter);
                            // the active pin also gets a trash badge on its ring (tap it, or long-press the pin, to delete).
                            Canvas(Modifier.fillMaxSize()) {
                                val g = spGeoAt(size.width, size.height)
                                val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()))
                                val blue = Color(0xFF2563EB)
                                val lightBlue = Color(0xFF60A5FA)
                                tools.selective.forEach { p ->
                                    val c = g.toCanvas(Offset(p.x, p.y))
                                    val r = p.radius * g.pxPerBoxWidth
                                    val active = p.id == activePointId

                                    // outer radius ring: dark halo for contrast + blue dashed ring
                                    drawCircle(Color.Black.copy(alpha = 0.35f), r, c, style = Stroke(4.dp.toPx()))
                                    drawCircle(
                                        if (active) blue else lightBlue, r, c,
                                        style = Stroke(if (active) 2.5.dp.toPx() else 1.75.dp.toPx(), pathEffect = dash)
                                    )

                                    // bullseye
                                    drawCircle(Color.Black.copy(alpha = 0.3f), 13.dp.toPx(), c)
                                    drawCircle(Color.White, 12.dp.toPx(), c)
                                    drawCircle(
                                        if (active) blue else if (p.isNeutral) Color(0xFF94A3B8) else Color(0xFF22C55E),
                                        8.dp.toPx(), c
                                    )
                                    drawCircle(Color.White, 3.dp.toPx(), c)

                                    // active-parameter badge under the bullseye, e.g. "B: +15"
                                    val label = pinMeasurer.measure(
                                        AnnotatedString("${selParam.short}: ${spSigned(p.valueOf(selParam))}"),
                                        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    )
                                    val padX = 6.dp.toPx()
                                    val padY = 2.dp.toPx()
                                    val bw = label.size.width + padX * 2
                                    val bh = label.size.height + padY * 2
                                    val btl = Offset(c.x - bw / 2f, c.y + 16.dp.toPx())
                                    drawRoundRect(if (active) blue else Color(0xCC0F172A), btl, Size(bw, bh), CornerRadius(bh / 2f))
                                    drawText(label, topLeft = Offset(btl.x + padX, btl.y + padY))

                                    // trash badge (active pin only) – same position as the hit test in the gesture code
                                    if (active) {
                                        val tp = spPinTrashPos(c, r, 34.dp.toPx())
                                        val icon = 14.dp.toPx()
                                        drawCircle(Color.White, 13.dp.toPx(), tp)
                                        drawCircle(Color(0xFFEF4444), 11.dp.toPx(), tp)
                                        translate(tp.x - icon / 2f, tp.y - icon / 2f) {
                                            with(trashPainter) { draw(Size(icon, icon), colorFilter = ColorFilter.tint(Color.White)) }
                                        }
                                    }
                                }
                            }
                        }

                        // ---------------- lens blur guide: clear zone (solid) + feather ring (dashed)
                        if (tool == SpTool.LENS && tools.lens.on) {
                            // Bokeh focus ring: solid inner ring = 100 % sharp zone (clear), dashed outer ring = end of the
                            // feather transition (clear + feather). Drag anywhere to move it, pinch to scale both rings.
                            Canvas(Modifier.fillMaxSize()) {
                                val g = spGeoAt(size.width, size.height)
                                val l = tools.lens
                                val c = g.toCanvas(Offset(l.cx, l.cy))
                                val dash = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                                val halo = Color.Black.copy(alpha = 0.35f)
                                val accent = Color(0xFF60A5FA)
                                rotate(g.angle, c) {
                                    fun radii(frac: Float) = Offset(frac * g.dw * l.axisX * g.sx, frac * g.dw * l.axisY * g.sy)
                                    fun ring(frac: Float, color: Color, width: Float, effect: PathEffect?) {
                                        val r = radii(frac)
                                        drawOval(
                                            color, topLeft = Offset(c.x - r.x, c.y - r.y), size = Size(r.x * 2f, r.y * 2f),
                                            style = Stroke(width, pathEffect = effect)
                                        )
                                    }
                                    // faint highlight of the sharp zone while adjusting
                                    if (lensAdjusting) {
                                        val r = radii(l.clear)
                                        drawOval(Color.White.copy(alpha = 0.10f), Offset(c.x - r.x, c.y - r.y), Size(r.x * 2f, r.y * 2f))
                                    }
                                    // inner solid ring (sharp zone)
                                    ring(l.clear, halo, 4.dp.toPx(), null)
                                    ring(l.clear, if (lensAdjusting) accent else Color.White, 2.dp.toPx(), null)
                                    // outer dashed ring (feather boundary)
                                    ring(l.clear + l.feather, halo, 3.dp.toPx(), dash)
                                    ring(l.clear + l.feather, Color.White.copy(alpha = 0.85f), 1.5.dp.toPx(), dash)
                                    // bokeh dots spaced around the feather ring
                                    val outer = radii(l.clear + l.feather)
                                    for (k in 0 until 12) {
                                        val a = (k / 12f) * 2f * PI.toFloat()
                                        val p = Offset(c.x + outer.x * cos(a), c.y + outer.y * sin(a))
                                        drawCircle(Color.White.copy(alpha = 0.55f), 3.dp.toPx(), p)
                                        drawCircle(halo, 3.dp.toPx(), p, style = Stroke(1.dp.toPx()))
                                    }
                                }
                                // centre handle with crosshair
                                val arm = 11.dp.toPx()
                                drawLine(Color.White, Offset(c.x - arm, c.y), Offset(c.x + arm, c.y), 1.5.dp.toPx())
                                drawLine(Color.White, Offset(c.x, c.y - arm), Offset(c.x, c.y + arm), 1.5.dp.toPx())
                                drawCircle(Color.White, 6.dp.toPx(), c)
                                drawCircle(Color(0xFF2563EB), 4.dp.toPx(), c)
                            }
                            // live readout while dragging / pinching (non-interactive, so gestures pass through)
                            if (lensAdjusting) {
                                Box(
                                    Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                                        .background(Color(0xCC0F172A), RoundedCornerShape(50))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        "Sharp ${(tools.lens.clear * 100).roundToInt()}  ·  Feather ${(tools.lens.feather * 100).roundToInt()}",
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                                    )
                                }
                            }
                        }

                        // ---------------- perspective quad (+ corner handles in Free mode)
                        if (tool == SpTool.PERSPECTIVE) {
                            Canvas(Modifier.fillMaxSize()) {
                                val q = tools.perspective.quad(size.width, size.height)
                                val path = Path().apply {
                                    moveTo(q[0], q[1]); lineTo(q[2], q[3]); lineTo(q[4], q[5]); lineTo(q[6], q[7]); close()
                                }
                                drawPath(path, Color.White, style = Stroke(2.dp.toPx()))
                                if (tools.perspective.mode == PerspectiveMode.FREE) {
                                    for (i in 0..3) {
                                        val o = Offset(q[i * 2], q[i * 2 + 1])
                                        drawCircle(Color.White, 10.dp.toPx(), o)
                                        drawCircle(Color(0xFF2563EB), 7.dp.toPx(), o)
                                    }
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

            // ---------------- selective tool: eye toggle + popup control strip (outside the gesture surface)
            if (tool == SpTool.SELECTIVE) {
                SpSelectiveOverlay(
                    point = null,   // the controller is shown in the tool panel now (SpSelectiveControls); the eye stays on the canvas
                    param = selParam,
                    showPoints = showPoints,
                    onParam = { selParam = it },
                    onValue = { v -> activePointId?.let { id -> updatePoint(id) { it.withValue(selParam, v) } } },
                    onDelete = {
                        val id = activePointId
                        if (id != null) {
                            tools = tools.copy(selective = tools.selective.filter { it.id != id })
                            activePointId = null
                        }
                    },
                    onToggleEye = { showPoints = !showPoints }
                )
            }
        }
    }

    // =============================================================== add / edit text (animated dialog)
    if (textDialogOpen) {
        val eid = editingTextId
        val editing = overlays.firstOrNull { it.id == eid }
        key(eid) {
            SpTextDialog(
                isEdit = eid != null,
                initialText = editing?.content ?: "",
                initialFont = editing?.fontName ?: defaultText.fontName,
                initialColor = Color((editing?.colorLong ?: defaultText.colorLong).toInt()),
                onDismiss = { textDialogOpen = false },
                onConfirm = { text, font, color ->
                    val argb = color.toArgb().toLong() and 0xFFFFFFFFL
                    if (eid != null) {
                        updateOverlay(eid) { it.copy(content = text, fontName = font, colorLong = argb) }
                    } else {
                        val n = newFreestyleText(text).copy(fontName = font, colorLong = argb)
                        overlays = overlays + n
                        selectedId = n.id
                    }
                    textDialogOpen = false
                }
            )
        }
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
                    model = rememberPhotoRequest(photoUri, 1024),
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = if (selected) Color(0xFF1D4ED8) else Color(0xFF1E293B))
            if (badge.isNotEmpty()) Text(badge, fontSize = 9.sp, color = Color(0xFF64748B))
        }
    }
}
@Composable
private fun SpColorChip(text: String, swatch: Color, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF0F172A) else Color(0xFFF1F5F9))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(swatch).border(1.dp, Color(0x33000000), CircleShape))
        Text(
            text, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFF334155)
        )
    }
}

/**
 * Floating UI of the Selective tool (sits above the stage, never inside the gesture surface):
 *  - eye button (top right): show / hide the control-point circles,
 *  - popup control strip (bottom) for the active point: Brightness / Contrast / Saturation / Structure + delete.
 */
@Composable
private fun SpSelectiveOverlay(
    point: SelectivePoint?,
    param: SelParam,
    showPoints: Boolean,
    onParam: (SelParam) -> Unit,
    onValue: (Float) -> Unit,
    onDelete: () -> Unit,
    onToggleEye: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp).size(40.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
                .clickable(onClick = onToggleEye),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (showPoints) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = if (showPoints) "Hide control points" else "Show control points",
                tint = Color(0xFF0F172A)
            )
        }
        if (point != null) {
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).fillMaxWidth(0.96f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SelParam.entries.forEach { sp ->
                            val sel = sp == param
                            val edited = kotlin.math.abs(point.valueOf(sp)) >= 0.5f
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) Color(0xFFEFF6FF) else Color.Transparent)
                                    .clickable { onParam(sp) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(sp.icon, null, modifier = Modifier.size(18.dp),
                                    tint = if (sel) Color(0xFF2563EB) else if (edited) Color(0xFF16A34A) else Color(0xFF64748B))
                                Text(sp.label, fontSize = 9.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (sel) Color(0xFF2563EB) else Color(0xFF64748B))
                            }
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, "Delete point", tint = Color(0xFFEF4444))
                        }
                    }
                    val v = point.valueOf(param)
                    Text("${param.label}: ${if (v > 0f) "+" else ""}${v.toInt()}", fontSize = 11.sp, color = Color(0xFF334155))
                    FluidSlider(
                        value = v, onValueChange = onValue, valueRange = -50f..50f,
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    )
                }
            }
        }
    }
}

/** Prominent per-tool Reset button; enabled only while the tool has changes. */
@Composable
private fun SpResetButton(enabled: Boolean, onClick: () -> Unit) {
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

/** Selectable option tile ("Original" / presets) used by Vintage, Glamour, Blur and Lens Blur. */
@Composable
private fun SpOptionCard(label: String, selected: Boolean, swatch: Color? = null, icon: ImageVector? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.width(84.dp).height(62.dp).clip(shape)
            .background(if (selected) Color(0xFFEFF6FF) else Color.White)
            .border(if (selected) 2.dp else 1.dp, if (selected) Color(0xFF2563EB) else Color(0xFFCBD5E1), shape)
            .clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            swatch != null -> Box(Modifier.size(18.dp).clip(CircleShape).background(swatch).border(1.dp, Color(0x33000000), CircleShape))
            icon != null -> Icon(icon, null, modifier = Modifier.size(18.dp), tint = if (selected) Color(0xFF2563EB) else Color(0xFF64748B))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label, fontSize = 11.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF2563EB) else Color(0xFF334155)
        )
    }
}

/** In-place controller of the active Selective point: Brightness / Contrast / Saturation / Structure + delete. */
@Composable
private fun SpSelectiveControls(point: SelectivePoint, param: SelParam, onParam: (SelParam) -> Unit, onValue: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SelParam.entries.forEach { sp ->
                val sel = sp == param
                val edited = abs(point.valueOf(sp)) >= 0.5f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(if (sel) Color(0xFFEFF6FF) else Color.Transparent)
                        .clickable { onParam(sp) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(sp.icon, null, modifier = Modifier.size(18.dp),
                        tint = if (sel) Color(0xFF2563EB) else if (edited) Color(0xFF16A34A) else Color(0xFF64748B))
                    Text(sp.label, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = if (sel) Color(0xFF2563EB) else Color(0xFF64748B))
                }
            }
        }
        val v = point.valueOf(param)
        Text("${param.label}: ${if (v > 0f) "+" else ""}${v.toInt()}", fontSize = 11.sp, color = Color(0xFF334155))
        FluidSlider(value = v, onValueChange = onValue, valueRange = -50f..50f, modifier = Modifier.fillMaxWidth().height(32.dp))
    }
}

private fun spFontPreview(font: String, size: androidx.compose.ui.unit.TextUnit): TextStyle = when (font) {
    "Script" -> TextStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = size)
    "Bold" -> TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = size)
    "Retro" -> TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = size)
    "Serif" -> TextStyle(fontFamily = FontFamily.Serif, fontSize = size)
    "Neon" -> TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = size)
    else -> TextStyle(fontWeight = FontWeight.Medium, fontSize = size)
}

/**
 * Animated add / edit text dialog: the card springs in (AnimatedVisibility), then the input, the font preview chips
 * and the colour picker fade in one after another. A live preview shows the text in the chosen font and colour.
 */
@Composable
private fun SpTextDialog(
    isEdit: Boolean,
    initialText: String,
    initialFont: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (text: String, font: String, color: Color) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var font by remember { mutableStateOf(initialFont) }
    var color by remember { mutableStateOf(initialColor) }
    var showCard by remember { mutableStateOf(false) }
    var showInput by remember { mutableStateOf(false) }
    var showFonts by remember { mutableStateOf(false) }
    var showColors by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        showCard = true
        delay(120); showInput = true
        delay(90); showFonts = true
        delay(90); showColors = true
        delay(100)
        runCatching { focus.requestFocus() }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = showCard,
            enter = fadeIn(tween(180)) +
                    scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                    slideInVertically(tween(260)) { it / 6 }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.92f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (isEdit) "Edit text" else "Add text", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                    // live preview
                    Box(
                        Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text.ifBlank { "Your text" },
                            style = spFontPreview(font, 24.sp).copy(color = if (text.isBlank()) color.copy(alpha = 0.4f) else color),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    AnimatedVisibility(showInput, enter = fadeIn(tween(220)) + expandVertically(tween(220))) {
                        OutlinedTextField(
                            value = text, onValueChange = { text = it },
                            singleLine = true,
                            placeholder = { Text("Type something…") },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2563EB), unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF8FAFC), unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier.fillMaxWidth().focusRequester(focus)
                        )
                    }

                    AnimatedVisibility(showFonts, enter = fadeIn(tween(220)) + expandVertically(tween(220))) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SpLabel("Font")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SpFonts) { f ->
                                    val sel = f == font
                                    Box(
                                        Modifier.clip(RoundedCornerShape(14.dp))
                                            .background(if (sel) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                                            .border(1.dp, if (sel) Color(0xFF0F172A) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                            .clickable { font = f }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(f, style = spFontPreview(f, 15.sp).copy(color = if (sel) Color.White else Color(0xFF334155)))
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(showColors, enter = fadeIn(tween(220)) + expandVertically(tween(220))) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SpLabel("Color")
                            SpColorRow(color) { color = it }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            enabled = text.isNotBlank(),
                            onClick = { onConfirm(text.trim(), font, color) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) { Text(if (isEdit) "Apply" else "Add") }
                    }
                }
            }
        }
    }
}