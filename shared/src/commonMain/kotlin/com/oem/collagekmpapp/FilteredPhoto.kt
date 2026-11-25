package com.oem.collagekmpapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.oem.collagekmpapp.components.FluidSlider
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

// ====================================================================================
//  High-resolution photo loading
//  12-48 MP camera photos must never be decoded at native size just to fill a phone-sized box.
//  Coil decodes on its own background dispatcher; here we additionally cap the DECODED size.
//  Sizes are bucketed (512 / 1024 / 2048) so a box that is being resized does not re-decode every frame,
//  and all layers of one photo share a single cached bitmap.
// ====================================================================================
/** Longest-side decode cap (px) for photos drawn below this point in the tree. 0 = let Coil use the layout size. */
internal val LocalPhotoDecodePx = compositionLocalOf { 0 }

private const val MAX_DECODE_PX = 2048

/** Rounds up to 512 / 1024 / 2048 (never above [MAX_DECODE_PX]). */
internal fun decodeBucket(px: Int): Int = when {
    px <= 512 -> 512
    px <= 1024 -> 1024
    else -> MAX_DECODE_PX
}

/**
 * Coil request that decodes [data] no larger than [maxSidePx] on its longest side (aspect ratio kept,
 * precision INEXACT so the decoder may sub-sample). Use instead of passing the raw uri as `model`
 * for any local (camera / gallery) photo.
 */
@Composable
fun rememberPhotoRequest(data: Any?, maxSidePx: Int = MAX_DECODE_PX): ImageRequest {
    val context = LocalPlatformContext.current
    val side = decodeBucket(maxSidePx)
    return remember(context, data, side) {
        ImageRequest.Builder(context)
            .data(data)
            .size(coil3.size.Size(side, side))
            .scale(coil3.size.Scale.FIT)
            .precision(coil3.size.Precision.INEXACT)
            .build()
    }
}

// ====================================================================================
//  FilteredPhoto: one photo with the COMPLETE look
//  colour matrix (filter + adjust) + blur, soften, clarity, sharpen, glow, grain, vignette
//  Export twin: PhotoLook.drawPhoto (same math, same radii).
//  NOTE: Modifier.blur needs Android 12+. On older phones blur/sharpen/clarity/soften/glow
//  degrade gracefully (no effect / slight brighten) while colour tools still work.
// ====================================================================================
@Composable
fun FilteredPhoto(
    uri: String,
    matrix: FloatArray?,
    effects: PhotoEffects,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    /** Width in px of the whole collage canvas. Film grain is sized relative to it (tile = 256 px @ 1080 px canvas) so preview == export. 0 = legacy 1:1 pixels. */
    canvasWidthPx: Float = 0f,
    /** Reports the decoded image aspect (w/h) once loaded (used by Freestyle frames). */
    onImageAspect: ((Float) -> Unit)? = null,
    /** Lens Blur (clear zone + feather ring + outer blur). Position is normalised over this photo box. null / off = none. */
    lens: LensBlurState? = null,
    /** Selective-edit control points (normalised over this photo box). */
    selective: List<SelectivePoint> = emptyList()
) {
    val grade = remember(matrix) { matrix?.let { ColorFilter.colorMatrix(ColorMatrix(it)) } }
    val inverted = remember(matrix) { ColorFilter.colorMatrix(ColorMatrix(PhotoColorEngine.inverted(matrix))) }
    val edge = BlurredEdgeTreatment.Rectangle   // clamp edges -> no dark fringe

    BoxWithConstraints(
        modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val w = maxWidth
        // 2x headroom for pinch-zoom (up to 4x is cropped away), then bucketed + capped
        val boxPx = maxOf(
            if (constraints.hasBoundedWidth) constraints.maxWidth else MAX_DECODE_PX,
            if (constraints.hasBoundedHeight) constraints.maxHeight else MAX_DECODE_PX
        )
        val decodePx = decodeBucket(boxPx * 2)

        CompositionLocalProvider(LocalPhotoDecodePx provides decodePx) {
            // Everything that touches the photo pixels. Drawn once, or twice when Lens Blur is on
            // (a sharp copy + a blurred, masked copy of the SAME fully-processed stack).
            val stack: @Composable () -> Unit = {
                // 1. base photo (+ real blur)
                PhotoLayer(
                    uri, grade, imageModifier,
                    if (effects.blur > 0.01f) Modifier.blur(w * (EffectRadii.BLUR * effects.blur), edge) else Modifier,
                    onImageAspect
                )

                // 2. soften
                if (effects.soften > 0.01f) {
                    PhotoLayer(uri, grade, imageModifier, Modifier.alpha(effects.soften * 0.85f).blur(w * EffectRadii.SOFTEN, edge))
                }

                // 3. clarity (negative = dreamy soft, positive = local contrast)
                if (effects.clarity < -0.01f) {
                    PhotoLayer(uri, grade, imageModifier, Modifier.alpha(-effects.clarity * 0.7f).blur(w * EffectRadii.CLARITY, edge))
                } else if (effects.clarity > 0.01f) {
                    HighPassLayer(uri, grade, inverted, imageModifier, w * EffectRadii.CLARITY, effects.clarity)
                }

                // 4. sharpen
                if (effects.sharpen > 0.01f) {
                    HighPassLayer(uri, grade, inverted, imageModifier, w * EffectRadii.SHARPEN, effects.sharpen)
                }

                // 5. glow / bloom
                if (effects.glow > 0.01f) {
                    PhotoLayer(
                        uri, grade, imageModifier,
                        Modifier.blendLayer(BlendMode.Screen, effects.glow * 0.6f).blur(w * EffectRadii.GLOW, edge)
                    )
                }

                // 3b. structure (mid-tone local contrast; negative = smoother)
                if (effects.structure > 0.01f) {
                    HighPassLayer(uri, grade, inverted, imageModifier, w * EffectRadii.STRUCTURE, effects.structure)
                } else if (effects.structure < -0.01f) {
                    PhotoLayer(uri, grade, imageModifier, Modifier.alpha(-effects.structure * 0.6f).blur(w * (EffectRadii.STRUCTURE * 2f), edge))
                }

                // 5b. glamour glow: soft-focus blend + screen bloom of a heavily blurred copy
                if (effects.glamour > 0.01f) {
                    PhotoLayer(uri, grade, imageModifier, Modifier.alpha(effects.glamour * 0.45f).blur(w * EffectRadii.GLAMOUR, edge))
                    PhotoLayer(
                        uri, grade, imageModifier,
                        Modifier.blendLayer(BlendMode.Screen, effects.glamour * 0.5f).blur(w * EffectRadii.GLAMOUR, edge)
                    )
                }

                // 5c. selective edit: each control point = a locally graded copy of the photo, masked by a soft circle
                selective.forEach { p ->
                    if (!p.isNeutral) key(p.id) {
                        val pm = remember(matrix, p.brightness, p.contrast, p.saturation) { PhotoColorEngine.selectiveMatrix(matrix, p) }
                        val pGrade = remember(pm) { ColorFilter.colorMatrix(ColorMatrix(pm)) }
                        val pInv = remember(pm) { ColorFilter.colorMatrix(ColorMatrix(PhotoColorEngine.inverted(pm))) }
                        Box(Modifier.fillMaxSize().pointMask(p.x, p.y, p.radius)) {
                            PhotoLayer(uri, pGrade, imageModifier, Modifier)
                            if (p.structure > 0.5f) {
                                HighPassLayer(uri, pGrade, pInv, imageModifier, w * EffectRadii.STRUCTURE, p.structure / 50f)
                            } else if (p.structure < -0.5f) {
                                PhotoLayer(uri, pGrade, imageModifier, Modifier.alpha(-p.structure / 50f * 0.7f).blur(w * EffectRadii.CLARITY, edge))
                            }
                        }
                    }
                }
            }

            stack()

            // 5d. lens blur: blurred copy of the whole stack, hidden inside the clear zone, fading in across the feather ring
            val lensSpec = lens?.takeIf { it.active }
            if (lensSpec != null) {
                Box(
                    Modifier.fillMaxSize()
                        .lensOuterMask(lensSpec)
                        .blur(w * (EffectRadii.LENS * (lensSpec.amount / 100f)), edge)
                ) { stack() }
            }

            // 6. film grain
            if (effects.grain > 0.01f) {
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        val k = if (canvasWidthPx > 0f) canvasWidthPx / 1080f else 1f   // exporter: exportScale = canvasW / 1080
                        withTransform({ scale(k, k, Offset.Zero) }) {
                            drawRect(
                                brush = GrainTexture.brush,
                                topLeft = Offset.Zero,
                                size = Size(size.width / k, size.height / k),
                                alpha = effects.grain * 0.55f,
                                blendMode = BlendMode.Overlay
                            )
                        }
                    }
                )
            }

            // 7. vignette (negative = white)
            if (abs(effects.vignette) > 0.01f) {
                val base = if (effects.vignette > 0f) Color.Black else Color.White
                Box(
                    Modifier.fillMaxSize().drawBehind {
                        val radius = hypot(size.width, size.height) / 2f
                        drawRect(
                            brush = Brush.radialGradient(
                                0.5f to base.copy(alpha = 0f),
                                1f to base.copy(alpha = abs(effects.vignette)),
                                center = center,
                                radius = radius
                            )
                        )
                    }
                )
            }
        }   // CompositionLocalProvider
    }
}

@Composable
private fun PhotoLayer(
    uri: String,
    filter: ColorFilter?,
    imageModifier: Modifier,
    extra: Modifier,
    onAspect: ((Float) -> Unit)? = null
) {
    val decodePx = LocalPhotoDecodePx.current
    AsyncImage(
        model = if (decodePx > 0) rememberPhotoRequest(uri, decodePx) else uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        colorFilter = filter,
        onSuccess = { s ->
            val sz = s.painter.intrinsicSize
            if (onAspect != null && sz.width > 0f && sz.height > 0f) onAspect(sz.width / sz.height)
        },
        modifier = Modifier.fillMaxSize().then(imageModifier).then(extra)
    )
}

/** sharp + inverted-blur @50% = high-pass; overlay-blended onto the photo = unsharp mask. */
@Composable
private fun HighPassLayer(
    uri: String,
    grade: ColorFilter?,
    inverted: ColorFilter,
    imageModifier: Modifier,
    radius: Dp,
    strength: Float
) {
    Box(Modifier.fillMaxSize().blendLayer(BlendMode.Overlay, strength.coerceIn(0f, 1f))) {
        PhotoLayer(uri, grade, imageModifier, Modifier)
        PhotoLayer(uri, inverted, imageModifier, Modifier.alpha(0.5f).blur(radius, BlurredEdgeTreatment.Rectangle))
    }
}

private fun Modifier.blendLayer(mode: BlendMode, alpha: Float): Modifier = drawWithContent {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            blendMode = mode
            this.alpha = alpha
        }
        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
        drawContent()
        canvas.restore()
    }
}

/** Keeps only the soft-edged disc (normalised centre / radius-as-fraction-of-width) of whatever this wraps. */
private fun Modifier.pointMask(nx: Float, ny: Float, nr: Float): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to Color.Black, 0.35f to Color.Black, 1f to Color.Transparent),
                center = Offset(nx * size.width, ny * size.height),
                radius = (nr * size.width).coerceAtLeast(1f)
            ),
            blendMode = BlendMode.DstIn
        )
    }

/**
 * Lens blur mask: transparent inside the clear zone, opaque outside the feather ring.
 * Circular = circle, Horizontal = ellipse (the gradient is a circle drawn in a vertically squashed space).
 */
private fun Modifier.lensOuterMask(l: LensBlurState): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val cx = l.cx * size.width
        val cy = l.cy * size.height
        val total = (l.clear + l.feather).coerceAtLeast(0.001f)
        val rx = (total * size.width * l.axisX).coerceAtLeast(1f)
        val k = l.axisY / l.axisX
        val inner = (l.clear / total).coerceIn(0f, 0.99f)
        withTransform({ scale(1f, k, Offset(cx, cy)) }) {
            drawRect(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(0f to Color.Transparent, inner to Color.Transparent, 1f to Color.Black),
                    center = Offset(cx, cy),
                    radius = rx
                ),
                topLeft = Offset(0f, cy - cy / k),
                size = Size(size.width, size.height / k),
                blendMode = BlendMode.DstIn
            )
        }
    }

/** Tiled noise texture built from the shared GrainPattern (same pattern as the exporter). */
private object GrainTexture {
    private const val LEVELS = 32
    val brush: ShaderBrush by lazy {
        val n = GrainPattern.SIZE
        val image = ImageBitmap(n, n)
        val canvas = androidx.compose.ui.graphics.Canvas(image)
        val groups = Array(LEVELS) { ArrayList<Offset>() }
        for (i in 0 until n * n) {
            val level = GrainPattern.values[i] * LEVELS / 256
            groups[level].add(Offset((i % n) + 0.5f, (i / n) + 0.5f))
        }
        for (level in 0 until LEVELS) {
            val g = (level + 0.5f) / LEVELS
            val paint = Paint().apply {
                color = Color(g, g, g, 1f)
                strokeWidth = 1f
                strokeCap = StrokeCap.Square
                isAntiAlias = false
            }
            canvas.drawPoints(PointMode.Points, groups[level], paint)
        }
        ShaderBrush(ImageShader(image, TileMode.Repeated, TileMode.Repeated))
    }
}

// ====================================================================================
//  Instagram-style filter picker
// ====================================================================================
@Composable
fun FilterPickerPanel(
    previewUri: String?,
    selectedPreset: String,
    intensity: Float,
    onPresetSelected: (String) -> Unit,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var category by remember { mutableStateOf("All") }
    val names = remember(category) { PhotoFilters.categories[category] ?: PhotoFilters.names }
    val listState = rememberLazyListState()

    LaunchedEffect(category) { listState.scrollToItem(0) }
    LaunchedEffect(selectedPreset, category) {
        val i = names.indexOf(selectedPreset)
        if (i >= 0) listState.animateScrollToItem(max(0, i - 1))
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedVisibility(visible = selectedPreset != "Original") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(selectedPreset, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("${intensity.toInt()}", fontSize = 13.sp, color = Color(0xFF334155))
                }
                FluidSlider(
                    value = intensity,
                    onValueChange = onIntensityChange,
                    valueRange = 0f..100f,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(PhotoFilters.categories.keys.toList()) { cat ->
                val active = cat == category
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (active) Color(0xFF0F172A) else Color(0xFFF1F5F9))
                        .clickable { category = cat }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) Color.White else Color(0xFF475569)
                    )
                }
            }
        }

        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            items(names, key = { it }) { name ->
                FilterThumbnail(name, previewUri, name == selectedPreset) { onPresetSelected(name) }
            }
        }
    }
}

@Composable
private fun FilterThumbnail(name: String, uri: String?, selected: Boolean, onClick: () -> Unit) {
    val matrix = remember(name) { PhotoColorEngine.buildMatrix(name, 100f, emptyMap()) }
    val fx = remember(name) { PhotoColorEngine.effects(name, 100f, emptyMap()) }
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .width(76.dp)
            .height(96.dp)
            .clip(shape)
            .background(Color(0xFFCBD5E1))
            .border(
                width = if (selected) 2.5.dp else 0.dp,
                color = if (selected) Color(0xFF2563EB) else Color.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        if (uri != null) {
            FilteredPhoto(uri = uri, matrix = matrix, effects = fx, modifier = Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(if (selected) Color(0xFF2563EB) else Color.Black.copy(alpha = 0.55f))
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}