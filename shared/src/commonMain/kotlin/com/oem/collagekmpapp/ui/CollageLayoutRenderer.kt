package com.oem.collagekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun RenderCollageLayout(
    layoutId: String,
    photos: List<String>,
    spacing: Dp,
    cornerRadius: Dp,
    filterPreset: String,
    filterIntensity: Float,
    glitterCard: String,
    glitterDensity: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val safePhotos = if (photos.isEmpty()) listOf("default") else photos
        val count = safePhotos.size

        val repository = remember { com.oem.collagekmpapp.data.LayoutRepository() }
        val layout = remember(layoutId, count) {
            repository.getAllLayouts().find { it.id == layoutId }
                ?: repository.getLayouts(count).firstOrNull()
                ?: com.oem.collagekmpapp.model.CollageLayout(layoutId, "Default", count, "", listOf(com.oem.collagekmpapp.model.Slot(0f, 0f, 1f, 1f)))
        }

        val maxWidthDp = maxWidth
        val maxHeightDp = maxHeight

        Box(modifier = Modifier.fillMaxSize()) {
            layout.slots.forEachIndexed { index, slot ->
                val photoUri = safePhotos.getOrElse(index) { safePhotos.first() }
                val slotWidth = maxWidthDp * (slot.right - slot.left)
                val slotHeight = maxHeightDp * (slot.bottom - slot.top)
                val offsetX = maxWidthDp * slot.left
                val offsetY = maxHeightDp * slot.top

                Box(
                    modifier = Modifier
                        .size(slotWidth, slotHeight)
                        .offset(x = offsetX, y = offsetY)
                        .padding(spacing / 2)
                ) {
                    CollageImageSlot(
                        uri = photoUri,
                        cornerRadius = cornerRadius,
                        filterPreset = filterPreset,
                        filterIntensity = filterIntensity,
                        glitterCard = glitterCard,
                        glitterDensity = glitterDensity,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun CollageImageSlot(
    uri: String,
    cornerRadius: Dp,
    filterPreset: String,
    filterIntensity: Float,
    glitterCard: String,
    glitterDensity: Float,
    modifier: Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFFCBD5E1))
            .pointerInput(uri) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 4f)
                    offset += pan
                }
            }
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )

        // Filter tint
        val alpha = (filterIntensity / 100f) * 0.45f
        val tint = when (filterPreset) {
            "Vivid" -> Color(0xFFE11D48)
            "Golden" -> Color(0xFFF59E0B)
            "Retro" -> Color(0xFF9A3412)
            "Soft" -> Color(0xFFFBCFE8)
            "Moody" -> Color(0xFF1E3A8A)
            "Warm" -> Color(0xFFFB923C)
            "Vintage" -> Color(0xFF78350F)
            "B&W" -> Color.Gray
            "Neon" -> Color(0xFF10B981)
            else -> Color.Transparent
        }

        if (filterPreset != "Original" && filterPreset != "Natural") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tint.copy(alpha = alpha))
            )
        }

        // Sparkling Glitter Overlays
        if (glitterDensity > 10f) {
            Box(modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFEF08A),
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                )
                if (glitterDensity > 50f) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFACC15),
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}
