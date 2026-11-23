package com.oem.collagekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.draw.drawBehind
import kotlin.math.hypot
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.oem.collagekmpapp.FilteredPhoto
import com.oem.collagekmpapp.PhotoColorEngine
import com.oem.collagekmpapp.model.CollageLayout
import com.oem.collagekmpapp.model.Slot

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
    adjustValues: Map<String, Float> = emptyMap(),
    isDrawingMode: Boolean = false,
    slots: List<Slot>? = null,
    selectedSlotIndex: Int? = null,
    onSelectSlot: ((Int?) -> Unit)? = null,
    onSlotsChanged: ((List<Slot>) -> Unit)? = null,
    photoTransforms: Map<Int, com.oem.collagekmpapp.model.PhotoTransform>? = null,
    onPhotoTransformChanged: ((Int, com.oem.collagekmpapp.model.PhotoTransform) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    BoxWithConstraints(modifier = modifier) {
        val safePhotos = if (photos.isEmpty()) listOf("default") else photos
        val count = safePhotos.size

        val repository = remember { com.oem.collagekmpapp.data.LayoutRepository() }
        val defaultLayout = remember(layoutId, count) {
            repository.getAllLayouts().find { it.id == layoutId }
                ?: repository.getLayouts(count).firstOrNull()
                ?: CollageLayout(layoutId, "Default", count, "", listOf(Slot(0f, 0f, 1f, 1f)))
        }

        val activeSlots = slots ?: defaultLayout.slots
        val maxWidthDp = maxWidth
        val maxHeightDp = maxHeight

        Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(onTap = { onSelectSlot?.invoke(null) })
        }) {
            activeSlots.forEachIndexed { index, slot ->
                val photoUri = safePhotos.getOrElse(index) { safePhotos.first() }
                val isSelected = selectedSlotIndex == index

                val slotWidthDp = maxWidthDp * (slot.right - slot.left)
                val slotHeightDp = maxHeightDp * (slot.bottom - slot.top)
                val offsetXDp = maxWidthDp * slot.left
                val offsetYDp = maxHeightDp * slot.top
                val currentTransform = photoTransforms?.get(index)

                Box(
                    modifier = Modifier
                        .size(slotWidthDp, slotHeightDp)
                        .offset(x = offsetXDp, y = offsetYDp)
                        .padding(spacing / 2)
                        .pointerInput(index, isDrawingMode) {
                            if (!isDrawingMode) {
                                detectTapGestures(
                                    onTap = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelectSlot?.invoke(index)
                                    }
                                )
                            }
                        }
                ) {
                    CollageImageSlot(
                        uri = photoUri,
                        cornerRadius = cornerRadius,
                        filterPreset = filterPreset,
                        filterIntensity = filterIntensity,
                        glitterCard = glitterCard,
                        glitterDensity = glitterDensity,
                        adjustValues = adjustValues,
                        isDrawingMode = isDrawingMode,
                        initialScale = currentTransform?.scale ?: 1f,
                        initialOffset = if (currentTransform != null) Offset(currentTransform.panX * slotWidthDp.value, currentTransform.panY * slotHeightDp.value) else Offset.Zero,
                        onTransformChanged = { newScale, newOffset ->
                            val normPanX = if (slotWidthDp.value > 0f) newOffset.x / slotWidthDp.value else 0f
                            val normPanY = if (slotHeightDp.value > 0f) newOffset.y / slotHeightDp.value else 0f
                            onPhotoTransformChanged?.invoke(index, com.oem.collagekmpapp.model.PhotoTransform(
                                uri = photoUri,
                                panX = normPanX,
                                panY = normPanY,
                                scale = newScale,
                                rotation = 0f
                            ))
                        },
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
    adjustValues: Map<String, Float> = emptyMap(),
    isDrawingMode: Boolean = false,
    initialScale: Float = 1f,
    initialOffset: Offset = Offset.Zero,
    onTransformChanged: ((scale: Float, offset: Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember(initialScale) { mutableStateOf(initialScale) }
    var offset by remember(initialOffset) { mutableStateOf(initialOffset) }

    // Real per-pixel colour grading: filter preset + Adjust tab merged into ONE color matrix.
    // The same matrix is used by the exporter, so preview == saved image.
    val photoColorFilter = remember(filterPreset, filterIntensity, adjustValues) {
        PhotoColorEngine.buildMatrix(filterPreset, filterIntensity, adjustValues)
            ?.let { ColorFilter.colorMatrix(ColorMatrix(it)) }
    }
    val vignette = remember(filterPreset, filterIntensity) {
        PhotoColorEngine.vignetteStrength(filterPreset, filterIntensity)
    }
    val glow = remember(filterPreset, filterIntensity) {
        PhotoColorEngine.glowStrength(filterPreset, filterIntensity)
    }

    val boxModifier = modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(Color(0xFFCBD5E1))

    val finalModifier = if (!isDrawingMode) {
        boxModifier.pointerInput(uri) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(0.5f, 4f)
                offset += pan
                onTransformChanged?.invoke(scale, offset)
            }
        }
    } else {
        boxModifier
    }

    Box(modifier = finalModifier) {
        FilteredPhoto(
            uri = uri,
            colorFilter = photoColorFilter,
            vignette = vignette,
            glow = glow,
            modifier = Modifier.fillMaxSize(),
            imageModifier = Modifier.graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y
            )
        )

        // Distinct Glitter Overlays
        if (glitterDensity > 0f) {
            Box(modifier = Modifier.fillMaxSize()) {
                val tintColor = when (glitterCard) {
                    "Diamond Glow" -> Color(0xFF38BDF8)
                    "Heart Sparkle" -> Color(0xFFF43F5E)
                    "Golden Shimmer" -> Color(0xFFF59E0B)
                    "Dust" -> Color(0xFFFEF08A)
                    else -> Color(0xFFFFD700)
                }
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier
                        .size((20.dp * (glitterDensity / 50f)).coerceIn(16.dp, 36.dp))
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                )
                if (glitterDensity > 30f) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size((16.dp * (glitterDensity / 50f)).coerceIn(12.dp, 28.dp))
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    )
                }
                if (glitterDensity > 70f) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}