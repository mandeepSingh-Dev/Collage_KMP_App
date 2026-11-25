package com.oem.collagekmpapp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** KeyValueStore key: "1" once the onboarding has been completed or skipped (so it only shows on first launch). */
const val ONBOARDING_DONE_KEY = "onboarding_done_v1"

private data class OnbPage(val title: String, val body: String, val accent: Color, val accent2: Color)

private val OnbPages = listOf(
    OnbPage(
        "Stunning Grid & Template Collages",
        "Choose from hundreds of grids and themed templates, then drag any edge to resize every box.",
        Color(0xFF2563EB), Color(0xFF7C3AED)
    ),
    OnbPage(
        "Unleash Freestyle Creativity",
        "Place, rotate and layer photos, stickers and text anywhere on the canvas. No rules, no grid.",
        Color(0xFFEC4899), Color(0xFFF59E0B)
    ),
    OnbPage(
        "AI Background Removal & Pro Edits",
        "Cut out subjects with precision, then polish with filters, selective edits, lens blur and more.",
        Color(0xFF059669), Color(0xFF0EA5E9)
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pager = rememberPagerState { OnbPages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == OnbPages.lastIndex

    Column(Modifier.fillMaxSize().background(Color(0xFFFAFAFA)).statusBarsPadding().navigationBarsPadding()) {
        // top row: Skip (hidden on the last page, space is kept so nothing jumps)
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (!last) {
                Text(
                    "Skip", color = Color(0xFF64748B), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onFinished).padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val p = OnbPages[page]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OnbIllustration(page, p.accent, p.accent2)
                }
                Text(p.title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A), textAlign = TextAlign.Center, lineHeight = 32.sp)
                Spacer(Modifier.height(12.dp))
                Text(p.body, fontSize = 15.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center, lineHeight = 22.sp)
                Spacer(Modifier.height(20.dp))
            }
        }

        // indicator dots
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            OnbPages.indices.forEach { i ->
                val sel = pager.currentPage == i
                val w by animateDpAsState(if (sel) 26.dp else 8.dp)
                val c by animateColorAsState(if (sel) OnbPages[pager.currentPage].accent else Color(0xFFCBD5E1))
                Box(Modifier.padding(horizontal = 3.dp).height(8.dp).width(w).clip(CircleShape).background(c))
            }
        }

        Button(
            onClick = {
                if (last) onFinished() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp).height(54.dp)
        ) {
            Text(if (last) "Get Started" else "Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// =====================================================================================
// Illustrations (pure Compose shapes, gently floating)
// =====================================================================================

@Composable
private fun OnbIllustration(page: Int, a: Color, b: Color) {
    val t = rememberInfiniteTransition()
    val float by t.animateFloat(-6f, 6f, infiniteRepeatable(tween(2200), RepeatMode.Reverse))
    val grad = Brush.linearGradient(listOf(a, b))
    Box(Modifier.size(260.dp).graphicsLayer { translationY = float.dp.toPx() }, contentAlignment = Alignment.Center) {
        // soft halo
        Box(Modifier.size(240.dp).clip(CircleShape).background(a.copy(alpha = 0.08f)))
        when (page) {
            0 -> { // grid collage
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(width = 84.dp, height = 120.dp).clip(RoundedCornerShape(18.dp)).background(grad))
                        Box(Modifier.size(width = 84.dp, height = 70.dp).clip(RoundedCornerShape(18.dp)).background(b.copy(alpha = 0.35f)))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(width = 84.dp, height = 70.dp).clip(RoundedCornerShape(18.dp)).background(a.copy(alpha = 0.35f)))
                        Box(Modifier.size(width = 84.dp, height = 120.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(b, a))))
                    }
                }
            }
            1 -> { // freestyle: overlapping rotated cards + emoji
                Box(Modifier.size(120.dp, 150.dp).graphicsLayer { rotationZ = -14f; translationX = -50.dp.toPx() }.clip(RoundedCornerShape(20.dp)).background(a.copy(alpha = 0.40f)))
                Box(Modifier.size(120.dp, 150.dp).graphicsLayer { rotationZ = 10f; translationX = 52.dp.toPx(); translationY = 10.dp.toPx() }.clip(RoundedCornerShape(20.dp)).background(b.copy(alpha = 0.55f)))
                Box(Modifier.size(130.dp, 160.dp).graphicsLayer { rotationZ = -2f }.clip(RoundedCornerShape(22.dp)).background(grad), contentAlignment = Alignment.Center) {
                    Text("✨", fontSize = 48.sp)
                }
                Text("🌈", fontSize = 34.sp, modifier = Modifier.graphicsLayer { translationX = 78.dp.toPx(); translationY = -78.dp.toPx() })
                Text("💖", fontSize = 30.sp, modifier = Modifier.graphicsLayer { translationX = -84.dp.toPx(); translationY = 82.dp.toPx() })
            }
            else -> { // cut-out on a transparency checkerboard
                Box(Modifier.size(190.dp).clip(CircleShape).background(Color.White)) {
                    Canvas(Modifier.fillMaxSize()) {
                        val s = 16.dp.toPx()
                        var row = 0
                        var y = 0f
                        while (y < size.height) {
                            var col = 0
                            var x = 0f
                            while (x < size.width) {
                                if ((row + col) % 2 == 1) drawRect(Color(0xFFE2E8F0), Offset(x, y), Size(s, s))
                                x += s; col++
                            }
                            y += s; row++
                        }
                    }
                }
                Box(Modifier.size(104.dp).clip(CircleShape).background(grad), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ContentCut, null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
                Box(
                    Modifier.graphicsLayer { translationX = 70.dp.toPx(); translationY = -70.dp.toPx() }
                        .size(46.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.AutoAwesome, null, tint = b, modifier = Modifier.size(24.dp)) }
            }
        }
    }
}