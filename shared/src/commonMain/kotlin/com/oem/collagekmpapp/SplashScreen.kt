package com.oem.collagekmpapp

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated splash: the 2x2 collage mark springs in (scale + fade), a ring sweeps around it, the title fades up.
 * After ~1.9 s [onFinished] is called once (the caller decides: onboarding on first launch, else home).
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textShift = remember { Animatable(14f) }
    val finish by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
            launch { alpha.animateTo(1f, tween(550)) }
            launch { ring.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) }
            launch { delay(380); textAlpha.animateTo(1f, tween(550)) }
            launch { delay(380); textShift.animateTo(0f, tween(550, easing = FastOutSlowInEasing)) }
        }
        delay(450)
        finish()
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF0B1220), Color(0xFF1E3A8A), Color(0xFF4C1D95)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(150.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White.copy(alpha = 0.35f),
                        startAngle = -90f, sweepAngle = 360f * ring.value, useCenter = false,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                CollageMark(size = 84)
            }
            Spacer(Modifier.height(28.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { this.alpha = textAlpha.value; translationY = textShift.value.dp.toPx() }
            ) {
                Text("Collage Studio", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(6.dp))
                Text("Create. Edit. Share.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, letterSpacing = 1.sp)
            }
        }
    }
}

/** App mark drawn in code (no image asset needed): four rounded tiles, one of them tall. */
@Composable
internal fun CollageMark(size: Int) {
    val gap = (size * 0.08f).dp
    val tile = ((size - size * 0.08f) / 2f).dp
    val r = RoundedCornerShape((size * 0.16f).dp)
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Box(Modifier.size(tile).clip(r).background(Color.White))
            Box(Modifier.size(tile).clip(r).background(Color(0xFFC4B5FD)))
        }
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Box(Modifier.size(tile).clip(r).background(Color(0xFF93C5FD)))
            Box(Modifier.size(tile).clip(r).background(Color(0xFFFDA4AF)))
        }
    }
}