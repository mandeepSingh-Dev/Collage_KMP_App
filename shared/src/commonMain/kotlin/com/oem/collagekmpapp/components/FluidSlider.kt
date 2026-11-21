package com.oem.collagekmpapp.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * A fluid, animated slider:
 *  - gradient progress track
 *  - thumb that grows with a springy bounce and glows while dragging
 *  - floating value bubble that pops in above the thumb
 *  - haptic tick on every whole-number change
 */

@Preview
@Composable
fun previewSlider() {

    var volume by remember { mutableFloatStateOf(0f) }
    FluidSlider(
        value = volume,
        onValueChange = { volume = it },
        valueRange = 0f..100f,
        gradient = listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
        valueLabel = { "${it.roundToInt()}%" },
        modifier = Modifier.safeContentPadding()
    )
}

@Composable
fun FluidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    gradient: List<Color> = listOf(
        Color(0xFF7F5AF0),
        Color(0xFFFF6B9D),
        Color(0xFFFFB86C),
    ),
    inactiveTrackColor: Color = Color.White.copy(alpha = 0.12f),
    valueLabel: (Float) -> String = { it.roundToInt().toString() },
) {
    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentRange by rememberUpdatedState(valueRange)
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    var widthPx by remember { mutableFloatStateOf(0f) }
    var bubbleWidthPx by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val thumbRadiusPx = with(density) { 14.dp.toPx() }
    val bubbleLiftPx = with(density) { 12.dp.toPx() }

    // value -> 0..1
    val span = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    // Tight spring while dragging (feels attached to the finger), loose + bouncy on taps
    val animFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = if (isDragging) {
            spring(stiffness = Spring.StiffnessHigh)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "fraction",
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.45f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "thumbScale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.55f else 0.18f,
        animationSpec = tween(250),
        label = "glow",
    )
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(180),
        label = "bubbleAlpha",
    )
    val bubbleHidden by animateFloatAsState(
        targetValue = if (isDragging) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bubbleHidden",
    )

    val thumbColor = sampleGradient(gradient, animFraction)

    fun updateFromX(x: Float) {
        val usable = (widthPx - 2 * thumbRadiusPx).coerceAtLeast(1f)
        val f = ((x - thumbRadiusPx) / usable).coerceIn(0f, 1f)
        val r = currentRange
        currentOnChange(r.start + f * (r.endInclusive - r.start))
    }

    // Subtle haptic tick on every whole step while dragging
    LaunchedEffect(value.roundToInt()) {
        if (isDragging) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        updateFromX(offset.x)
                        tryAwaitRelease()
                        isDragging = false
                    },
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        updateFromX(offset.x)
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                ) { change, _ ->
                    change.consume()
                    updateFromX(change.position.x)
                }
            },
    ) {
        val thumbX = thumbRadiusPx + animFraction * (widthPx - 2 * thumbRadiusPx)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackY = size.height - 24.dp.toPx()
            val trackWidth = 10.dp.toPx()
            val start = thumbRadiusPx
            val end = size.width - thumbRadiusPx

            // Inactive track
            drawLine(
                color = inactiveTrackColor,
                start = Offset(start, trackY),
                end = Offset(end, trackY),
                strokeWidth = trackWidth,
                cap = StrokeCap.Round,
            )

            // Active gradient track
            if (thumbX > start) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = gradient,
                        startX = start,
                        endX = end,
                    ),
                    start = Offset(start, trackY),
                    end = Offset(thumbX, trackY),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round,
                )
            }

            // Glow behind thumb
            val glowRadius = thumbRadiusPx * 2.6f * thumbScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(thumbColor.copy(alpha = glowAlpha), Color.Transparent),
                    center = Offset(thumbX, trackY),
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = Offset(thumbX, trackY),
            )

            // Thumb: white ring + colored core
            val r = thumbRadiusPx * thumbScale
            drawCircle(Color.White, radius = r, center = Offset(thumbX, trackY))
            drawCircle(thumbColor, radius = r * 0.55f, center = Offset(thumbX, trackY))
        }

        // Floating value bubble
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .onSizeChanged { bubbleWidthPx = it.width.toFloat() }
                .offset { IntOffset((thumbX - bubbleWidthPx / 2f).roundToInt(), 0) }
                .graphicsLayer {
                    alpha = bubbleAlpha
                    translationY = bubbleHidden * bubbleLiftPx
                    val s = 0.75f + 0.25f * bubbleAlpha
                    scaleX = s
                    scaleY = s
                }
                .background(
                    brush = Brush.linearGradient(
                        listOf(thumbColor, thumbColor.copy(alpha = 0.75f)),
                    ),
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = valueLabel(value),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun sampleGradient(colors: List<Color>, t: Float): Color {
    if (colors.size == 1) return colors.first()
    val scaled = t.coerceIn(0f, 1f) * (colors.size - 1)
    val i = scaled.toInt().coerceAtMost(colors.size - 2)
    return lerp(colors[i], colors[i + 1], scaled - i)
}

// ---------------------------------------------------------------------------
// Demo screen
// ---------------------------------------------------------------------------

@Composable
fun FluidSliderDemo() {
    var brightness by remember { mutableFloatStateOf(65f) }
    var volume by remember { mutableFloatStateOf(30f) }
    var temperature by remember { mutableFloatStateOf(22f) }

    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .background(Color(0xFF0F0E17))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Brightness", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        FluidSlider(
            value = brightness,
            onValueChange = { brightness = it },
            valueLabel = { "${it.roundToInt()}%" },
        )

        Text("Volume", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        FluidSlider(
            value = volume,
            onValueChange = { volume = it },
            gradient = listOf(Color(0xFF00C9FF), Color(0xFF92FE9D)),
            valueLabel = { "${it.roundToInt()}%" },
        )

        Text("Temperature", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        FluidSlider(
            value = temperature,
            onValueChange = { temperature = it },
            valueRange = 16f..32f,
            gradient = listOf(Color(0xFF4FACFE), Color(0xFFFFD166), Color(0xFFFF5D5D)),
            valueLabel = { "${it.roundToInt()}°C" },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0E17)
@Composable
private fun FluidSliderPreview() {
    FluidSliderDemo()
}