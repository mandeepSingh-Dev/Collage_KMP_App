package com.oem.collagekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

        when {
            // 2-Photo Layouts
            count == 2 -> {
                when (layoutId) {
                    "layout_2_horizontal" -> {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                            CollageImageSlot(safePhotos[0], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxWidth())
                            CollageImageSlot(safePhotos[1], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxWidth())
                        }
                    }
                    else -> { // Vertical split
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            CollageImageSlot(safePhotos[0], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                            CollageImageSlot(safePhotos[1], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
            // 3-Photo Layouts
            count == 3 -> {
                when (layoutId) {
                    "layout_3_left" -> {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            CollageImageSlot(safePhotos[0], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                                CollageImageSlot(safePhotos[1], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxWidth())
                                CollageImageSlot(safePhotos[2], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    }
                    else -> { // Triple Top
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                            CollageImageSlot(safePhotos[0], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxWidth())
                            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                CollageImageSlot(safePhotos[1], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                                CollageImageSlot(safePhotos[2], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                }
            }
            // 4-Photo Layouts
            count == 4 -> {
                when (layoutId) {
                    "layout_pinwheel" -> {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                CollageImageSlot(safePhotos[0], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1.2f).fillMaxHeight())
                                CollageImageSlot(safePhotos[1], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(0.8f).fillMaxHeight())
                            }
                            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                CollageImageSlot(safePhotos[2], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(0.8f).fillMaxHeight())
                                CollageImageSlot(safePhotos[3], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1.2f).fillMaxHeight())
                            }
                        }
                    }
                    "layout_vertical_strips" -> {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            CollageImageSlot(safePhotos[0], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                                CollageImageSlot(safePhotos[1], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(2f).fillMaxWidth())
                                CollageImageSlot(safePhotos[2], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxWidth())
                            }
                            CollageImageSlot(safePhotos[3], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                    "layout_top_right" -> {
                        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            CollageImageSlot(safePhotos[0], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1.5f).fillMaxHeight())
                            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                                CollageImageSlot(safePhotos[1], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxWidth())
                                CollageImageSlot(safePhotos[2], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxWidth())
                                CollageImageSlot(safePhotos[3], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    }
                    else -> { // Large Top & 3 Bottom
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                            CollageImageSlot(safePhotos[0], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1.5f).fillMaxWidth())
                            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                                CollageImageSlot(safePhotos[1], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                                CollageImageSlot(safePhotos[2], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                                CollageImageSlot(safePhotos[3], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                }
            }
            // 5+ Photo Layouts
            else -> {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing)) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        CollageImageSlot(safePhotos[0], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                        CollageImageSlot(safePhotos.getOrElse(1) { safePhotos[0] }, cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        CollageImageSlot(safePhotos.getOrElse(2) { safePhotos[0] }, cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                        CollageImageSlot(safePhotos.getOrElse(3) { safePhotos[0] }, cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                        if (count >= 5) {
                            CollageImageSlot(safePhotos[4], cornerRadius, filterPreset, filterIntensity, glitterCard, glitterDensity, Modifier.weight(1f).fillMaxHeight())
                        }
                    }
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFFCBD5E1))
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
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
