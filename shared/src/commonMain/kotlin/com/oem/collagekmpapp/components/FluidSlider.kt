package com.oem.collagekmpapp.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------
// Value gradient: low = cool sky blue -> normal = teal -> amber -> high = rose
// ---------------------------------------------------------------------------------------

val SliderValueGradient: List<Color> = listOf(
    Color(0xFF38BDF8), // low
    Color(0xFF22C7A0), // normal
    Color(0xFFFBBF24),
    Color(0xFFF43F5E)  // high
)
private val SliderValueStops = listOf(0f, 0.38f, 0.68f, 1f)

private fun evenStops(n: Int): List<Float> = List(n) { if (n <= 1) 0f else it / (n - 1f) }

/** Color of the value gradient at [fraction] (0..1). Handy for tinting your own labels. */
fun fluidSliderValueColor(
    fraction: Float,
    colors: List<Color> = SliderValueGradient,
    stops: List<Float> = if (colors === SliderValueGradient) SliderValueStops else evenStops(colors.size)
): Color {
    val t = fraction.coerceIn(0f, 1f)
    if (colors.size == 1 || t <= stops.first()) return colors.first()
    for (i in 1 until colors.size) {
        if (t <= stops[i]) {
            val span = (stops[i] - stops[i - 1]).coerceAtLeast(0.0001f)
            return lerp(colors[i - 1], colors[i], (t - stops[i - 1]) / span)
        }
    }
    return colors.last()
}

// Neumorphic light source: top-left
private val LightShadow = Color.White
private val DarkShadow = Color(0xFFBAC4D4)
private val InsetDark = Color(0xFFC2CBDA)

/** A blurred disc: solid inside, ~50% at the edge, fading to nothing [blur] px further out. */
private fun DrawScope.softCircle(center: Offset, radius: Float, color: Color, blur: Float) {
    val outer = radius + blur
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color,
                ((radius - blur * 0.5f).coerceAtLeast(0f) / outer) to color,
                (radius / outer) to color.copy(alpha = color.alpha * 0.5f),
                1f to Color.Transparent
            ),
            center = center,
            radius = outer
        ),
        radius = outer,
        center = center
    )
}

/**
 * Premium neumorphic slider.
 *  - Recessed track with inner shadows (dark top-left, light bottom-right)
 *  - Gradient fill that shifts with the value: low (blue) -> normal (teal) -> high (rose)
 *  - Raised knob with dual drop shadows, a bevel ring and a glowing value-colored core
 *  - Value bubble springs in the moment you touch, follows the knob, and fades out on release
 *  - Optional bipolar anchor (e.g. -50..+50 Brightness) that fills outward from the zero point
 *
 * Designed for a light neumorphic surface: place it on [inactiveTrackColor] (default #E9EEF5)
 * so the light/dark shadows blend, e.g. `Modifier.background(Color(0xFFE9EEF5))` on the panel.
 */
@Composable
fun FluidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    centerAnchor: Float? = null,
    trackThickness: Dp = 12.dp,
    thumbRadius: Dp = 14.dp,
    gradient: List<Color> = SliderValueGradient,
    inactiveTrackColor: Color = Color(0xFFE9EEF5),
    gradientStops: List<Float> = if (gradient === SliderValueGradient) SliderValueStops else evenStops(gradient.size),
    bubbleColor: Color = Color(0xFF1E293B),
    valueLabel: (Float) -> String = { it.roundToInt().toString() },
) {
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentRange by rememberUpdatedState(valueRange)
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    var widthPx by remember { mutableFloatStateOf(0f) }
    var bubbleWidthPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val thumbRadiusPx = with(density) { thumbRadius.toPx() }
    val trackPx = with(density) { trackThickness.toPx() }

    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val anchorFraction = centerAnchor?.let { ((it - valueRange.start) / span).coerceIn(0f, 1f) }

    val animFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = if (isDragging) spring(stiffness = Spring.StiffnessHigh)
        else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fraction"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isDragging) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    // Pop-in with a little overshoot, glide-out without bounce
    val bubbleProgress by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = if (isDragging) spring(dampingRatio = 0.5f, stiffness = 420f)
        else tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "bubbleProgress"
    )
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(durationMillis = if (isDragging) 140 else 220),
        label = "bubbleAlpha"
    )

    val valueColor = fluidSliderValueColor(animFraction, gradient, gradientStops)
    val stopsArray = remember(gradient, gradientStops) {
        Array(gradient.size) { gradientStops[it] to gradient[it] }
    }

    fun updateFromX(x: Float) {
        val usable = (widthPx - 2 * thumbRadiusPx).coerceAtLeast(1f)
        val f = ((x - thumbRadiusPx) / usable).coerceIn(0f, 1f)
        val r = currentRange
        currentOnChange(r.start + f * (r.endInclusive - r.start))
    }

    LaunchedEffect(value.roundToInt()) {
        if (isDragging) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Stronger "bump" the moment the knob reaches the minimum (0 %) or maximum (100 %) of the range.
    // Keyed on the edge state, so it fires once on arrival, not on every frame spent at the edge.
    val edgeState = when {
        fraction <= 0f -> -1
        fraction >= 1f -> 1
        else -> 0
    }
    LaunchedEffect(edgeState) {
        if (isDragging && edgeState != 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 36.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange)
                setProgress { v ->
                    currentOnChange(v.coerceIn(currentRange))
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isDragging = true
                    // tactile tick as soon as the finger grabs the slider
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    updateFromX(down.position.x)
                    // Horizontal drags are ours; a vertical swipe hands control back to a scrolling parent.
                    val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    if (slop != null) {
                        horizontalDrag(down.id) { change ->
                            change.consume()
                            updateFromX(change.position.x)
                        }
                    }
                    isDragging = false
                }
            }
    ) {
        val thumbX = thumbRadiusPx + animFraction * (widthPx - 2 * thumbRadiusPx)

        Canvas(Modifier.fillMaxSize()) {
            val cy = size.height / 2f
            val start = thumbRadiusPx
            val end = size.width - thumbRadiusPx
            val trackTop = cy - trackPx / 2f
            val trackW = end - start

            // ---- 1. Recessed track: base + inner shadows ------------------------------------
            drawRoundRect(
                color = inactiveTrackColor,
                topLeft = Offset(start, trackTop),
                size = Size(trackW, trackPx),
                cornerRadius = CornerRadius(trackPx / 2f)
            )
            val trackPath = Path().apply {
                addRoundRect(RoundRect(start, trackTop, end, trackTop + trackPx, trackPx / 2f, trackPx / 2f))
            }
            clipPath(trackPath) {
                val b = trackPx * 0.55f
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(InsetDark.copy(alpha = 0.95f), Color.Transparent),
                        startY = trackTop, endY = trackTop + b
                    ),
                    topLeft = Offset(start, trackTop), size = Size(trackW, b)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, LightShadow),
                        startY = trackTop + trackPx - b, endY = trackTop + trackPx
                    ),
                    topLeft = Offset(start, trackTop + trackPx - b), size = Size(trackW, b)
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(InsetDark.copy(alpha = 0.6f), Color.Transparent),
                        startX = start, endX = start + b * 1.5f
                    ),
                    topLeft = Offset(start, trackTop), size = Size(b * 1.5f, trackPx)
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, LightShadow),
                        startX = end - b * 1.5f, endX = end
                    ),
                    topLeft = Offset(end - b * 1.5f, trackTop), size = Size(b * 1.5f, trackPx)
                )
            }

            // ---- 2. Gradient fill (gradient is anchored to the whole track, so it reveals as you drag)
            val anchor = anchorFraction ?: 0f
            val fillA = start + minOf(anchor, animFraction) * trackW
            val fillB = start + maxOf(anchor, animFraction) * trackW
            if (fillB - fillA > 0.5f) {
                val inset = 1.dp.toPx()
                val fTop = trackTop + inset
                val fH = trackPx - 2 * inset
                val fillPath = Path().apply {
                    addRoundRect(RoundRect(fillA, fTop, fillB, fTop + fH, fH / 2f, fH / 2f))
                }
                clipPath(fillPath) {
                    drawRect(
                        brush = Brush.horizontalGradient(colorStops = stopsArray, startX = start, endX = end),
                        topLeft = Offset(fillA, fTop), size = Size(fillB - fillA, fH)
                    )
                    // glossy top highlight + soft bottom shade
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = 0.55f),
                            0.3f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1f to Color(0x2E0F172A),
                            startY = fTop, endY = fTop + fH
                        ),
                        topLeft = Offset(fillA, fTop), size = Size(fillB - fillA, fH)
                    )
                }
            }

            // ---- 3. Raised knob ---------------------------------------------------------------
            val r = thumbRadiusPx * pressScale
            val shadowOffset = 5.dp.toPx()
            val shadowBlur = 11.dp.toPx()
            softCircle(Offset(thumbX + shadowOffset, cy + shadowOffset), r, DarkShadow, shadowBlur)
            softCircle(Offset(thumbX - shadowOffset, cy - shadowOffset), r, LightShadow, shadowBlur)

            val c = Offset(thumbX, cy)
            val tl = Offset(thumbX - r, cy - r)
            val br = Offset(thumbX + r, cy + r)
            drawCircle(
                brush = Brush.linearGradient(listOf(Color.White, Color(0xFFDDE4EE)), start = tl, end = br),
                radius = r, center = c
            )
            // bevel: bright top-left rim, slate bottom-right rim
            drawCircle(
                brush = Brush.linearGradient(listOf(Color.White, Color(0x75949FB8)), start = tl, end = br),
                radius = r - 0.75.dp.toPx(), center = c,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // ---- 4. Value-colored core with glow ----------------------------------------------
            val dotR = r * 0.43f
            softCircle(c, dotR, valueColor.copy(alpha = 0.55f), 5.dp.toPx())
            drawCircle(
                brush = Brush.linearGradient(
                    listOf(lerp(valueColor, Color.White, 0.16f), valueColor),
                    start = Offset(thumbX - dotR, cy - dotR), end = Offset(thumbX + dotR, cy + dotR)
                ),
                radius = dotR, center = c
            )
            drawCircle(
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color(0x4D0F172A)),
                    start = Offset(thumbX - dotR, cy - dotR), end = Offset(thumbX + dotR, cy + dotR)
                ),
                radius = dotR - 0.5.dp.toPx(), center = c,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // ---- 5. Value bubble: shown for as long as the finger is down ----------------------------
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .onSizeChanged { bubbleWidthPx = it.width.toFloat() }
                .offset { IntOffset((thumbX - bubbleWidthPx / 2f).roundToInt(), 0) }
                .graphicsLayer {
                    alpha = bubbleAlpha
                    val s = 0.7f + 0.3f * bubbleProgress
                    scaleX = s
                    scaleY = s
                    translationY = (1f - bubbleProgress) * 10.dp.toPx()
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(50),
                    clip = false,
                    ambientColor = Color(0x401E293B),
                    spotColor = Color(0x401E293B)
                )
                .drawBehind {
                    val half = 5.dp.toPx()
                    val tail = Path().apply {
                        moveTo(size.width / 2f - half, size.height - 1.dp.toPx())
                        lineTo(size.width / 2f + half, size.height - 1.dp.toPx())
                        lineTo(size.width / 2f, size.height + 4.dp.toPx())
                        close()
                    }
                    drawPath(tail, bubbleColor)
                }
                .background(bubbleColor, RoundedCornerShape(50))
                .padding(horizontal = 11.dp, vertical = 4.dp)
        ) {
            Text(
                text = valueLabel(value),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Preview / demo — mirrors the approved design sample
// ---------------------------------------------------------------------------

@Composable
private fun DemoRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    anchor: Float? = null,
    signed: Boolean = false,
    onChange: (Float) -> Unit
) {
    val frac = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    val v = value.roundToInt()
    val label = if (signed) (if (v > 0) "+$v" else "$v") else "$v%"
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color(0xFF334155), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(label, color = fluidSliderValueColor(frac), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        FluidSlider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            centerAnchor = anchor,
            valueLabel = {
                val n = it.roundToInt()
                if (signed) (if (n > 0) "+$n" else "$n") else "$n%"
            }
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val small = Color(0xFF94A3B8)
            if (signed) {
                Text("-50", color = small, fontSize = 11.sp)
                Text("0", color = small, fontSize = 11.sp)
                Text("+50", color = small, fontSize = 11.sp)
            } else {
                Text("Low", color = small, fontSize = 11.sp)
                Text("Normal", color = small, fontSize = 11.sp)
                Text("High", color = small, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun FluidSliderDemo() {
    var blur by remember { mutableFloatStateOf(62f) }
    var brightness by remember { mutableFloatStateOf(18f) }
    var opacity by remember { mutableFloatStateOf(18f) }

    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .background(Color(0xFFE9EEF5), RoundedCornerShape(20.dp))
            .padding(26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        DemoRow("Blur amount", blur, 0f..100f) { blur = it }
        DemoRow("Brightness", brightness, -50f..50f, anchor = 0f, signed = true) { brightness = it }
        DemoRow("Brush opacity", opacity, 0f..100f) { opacity = it }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE9EEF5)
@Composable
private fun FluidSliderPreview() {
    FluidSliderDemo()
}