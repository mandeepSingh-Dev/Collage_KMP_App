package com.oem.collagekmpapp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.oem.collagekmpapp.PhotoColorEngine
import com.oem.collagekmpapp.PhotoFilters
import com.oem.collagekmpapp.components.FluidSlider
import kotlin.math.hypot
import kotlin.math.max

/**
 * One photo with a REAL filter look: per-pixel colour matrix + optional soft-bloom glow + vignette.
 * Used by both the collage slots and the filter thumbnails, so the thumbnail you tap
 * looks exactly like what gets applied.
 */
@Composable
fun FilteredPhoto(
    uri: String,
    colorFilter: ColorFilter?,
    vignette: Float,
    glow: Float,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = colorFilter,
            modifier = Modifier.fillMaxSize().then(imageModifier)
        )

        if (glow > 0.01f) {
            // Blurred copy screen-blended on top -> light bleeds out of bright areas (skin glow).
            // Modifier.blur needs Android 12+; on older devices this degrades to a gentle brighten.
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier
                    .fillMaxSize()
                    .then(imageModifier)
                    .screenBlend(glow * 0.6f)
                    .blur(10.dp, BlurredEdgeTreatment.Rectangle)
            )
        }

        if (vignette > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val radius = hypot(size.width, size.height) / 2f
                        drawRect(
                            brush = Brush.radialGradient(
                                0.5f to Color.Transparent,
                                1f to Color.Black.copy(alpha = vignette),
                                center = center,
                                radius = radius
                            )
                        )
                    }
            )
        }
    }
}

private fun Modifier.screenBlend(alpha: Float): Modifier = drawWithContent {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            blendMode = BlendMode.Screen
            this.alpha = alpha
        }
        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
        drawContent()
        canvas.restore()
    }
}

/**
 * Instagram / photo-editor style filter picker:
 *  - category tabs (All, Popular, Glow, B&W, Warm, Cool, Vintage, Cinematic)
 *  - horizontally scrolling thumbnails of YOUR photo with each filter applied
 *  - intensity slider for the active filter
 */
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimatedVisibility(visible = selectedPreset != "Original") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
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

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(names, key = { it }) { name ->
                FilterThumbnail(
                    name = name,
                    uri = previewUri,
                    selected = name == selectedPreset,
                    onClick = { onPresetSelected(name) }
                )
            }
        }
    }
}

@Composable
private fun FilterThumbnail(
    name: String,
    uri: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Thumbnails always show the filter at full strength, like Instagram.
    val colorFilter = remember(name) {
        PhotoColorEngine.buildMatrix(name, 100f, emptyMap())?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
    }
    val vignette = remember(name) { PhotoColorEngine.vignetteStrength(name, 100f) }
    val glow = remember(name) { PhotoColorEngine.glowStrength(name, 100f) }
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
            FilteredPhoto(
                uri = uri,
                colorFilter = colorFilter,
                vignette = vignette,
                glow = glow,
                modifier = Modifier.fillMaxSize()
            )
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