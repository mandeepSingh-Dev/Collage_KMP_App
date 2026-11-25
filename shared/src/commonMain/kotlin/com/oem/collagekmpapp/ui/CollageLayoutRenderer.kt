package com.oem.collagekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.oem.collagekmpapp.FilteredPhoto
import com.oem.collagekmpapp.PhotoColorEngine
import com.oem.collagekmpapp.PhotoEffects
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
    canvasWidthPx: Float = 0f,
    photoTransforms: Map<Int, com.oem.collagekmpapp.model.PhotoTransform>? = null,
    onPhotoTransformChanged: ((Int, com.oem.collagekmpapp.model.PhotoTransform) -> Unit)? = null,
    /** Tapping the round button at the top-right of the selected box asks the caller to replace that box's photo. */
    onReplacePhoto: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

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

                // The photo box inside the slot (slot minus the inner-spacing padding), in PIXELS.
                // Pan is stored as a fraction of THIS box so the exporter (which multiplies by its own
                // box width) reproduces exactly the same shift. (It used to divide pixels by dp.)
                val boxWidthPx = with(density) { (slotWidthDp - spacing).toPx() }.coerceAtLeast(1f)
                val boxHeightPx = with(density) { (slotHeightDp - spacing).toPx() }.coerceAtLeast(1f)
                // Live canvas adaptation: pan is stored as a fraction of the box, so it scales with the box
                // while the slot is being resized; clamp so the photo can never be pushed out of a shrunken box.
                val slotScale = currentTransform?.scale ?: 1f
                val maxPanX = boxWidthPx * slotScale / 2f
                val maxPanY = boxHeightPx * slotScale / 2f

                val latestOnSelectSlot by rememberUpdatedState(onSelectSlot)

                Box(
                    modifier = Modifier
                        .size(slotWidthDp, slotHeightDp)
                        .offset(x = offsetXDp, y = offsetYDp)
                        .padding(spacing / 2)
                        .pointerInput(index, isDrawingMode, activeSlots) {
                            if (!isDrawingMode) {
                                detectTapGestures(
                                    onTap = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        latestOnSelectSlot?.invoke(index)
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
                        canvasWidthPx = canvasWidthPx,
                        initialScale = currentTransform?.scale ?: 1f,
                        initialOffset = if (currentTransform != null) Offset(
                            (currentTransform.panX * boxWidthPx).coerceIn(-maxPanX, maxPanX),
                            (currentTransform.panY * boxHeightPx).coerceIn(-maxPanY, maxPanY)
                        ) else Offset.Zero,
                        onTransformChanged = { newScale, newOffset ->
                            val normPanX = newOffset.x / boxWidthPx
                            val normPanY = newOffset.y / boxHeightPx
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

            // ---- selection border + resize handles for the selected box (drawn above every slot) ----
            val selIdx = selectedSlotIndex
            if (selIdx != null && selIdx in activeSlots.indices && !isDrawingMode) {
                val sel = activeSlots[selIdx]
                val selWidthDp = maxWidthDp * (sel.right - sel.left)
                val selHeightDp = maxHeightDp * (sel.bottom - sel.top)
                val layoutWidthPx = with(density) { maxWidthDp.toPx() }
                val layoutHeightPx = with(density) { maxHeightDp.toPx() }
                Box(
                    modifier = Modifier
                        .size(selWidthDp, selHeightDp)
                        .offset(x = maxWidthDp * sel.left, y = maxHeightDp * sel.top)
                ) {
                    // visual selection border around the box (same inset/rounding as the photo)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing / 2)
                            .border(2.5.dp, Color(0xFF2563EB), RoundedCornerShape(cornerRadius))
                    )
                    // draggable handle on every interior edge (not on the collage's outer border)
                    if (onSlotsChanged != null) {
                        SlotEdge.entries.forEach { edge ->
                            if (canResizeEdge(activeSlots, selIdx, edge)) {
                                ResizeHandle(
                                    edge = edge,
                                    index = selIdx,
                                    currentSlots = { activeSlots },
                                    containerSizePx = if (edge == SlotEdge.LEFT || edge == SlotEdge.RIGHT) layoutWidthPx else layoutHeightPx,
                                    onSlotsChanged = onSlotsChanged
                                )
                            }
                        }
                    }
                    // replace / swap photo button – top-right corner of the selected box (above the handles)
                    if (onReplacePhoto != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(spacing / 2 + 6.dp)
                                .size(38.dp)
                                .shadow(6.dp, CircleShape)
                                .background(Color.White, CircleShape)
                                .clip(CircleShape)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onReplacePhoto(selIdx)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Replace photo",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
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
    adjustValues: Map<String, Float> = emptyMap(),
    isDrawingMode: Boolean = false,
    canvasWidthPx: Float = 0f,
    initialScale: Float = 1f,
    initialOffset: Offset = Offset.Zero,
    onTransformChanged: ((scale: Float, offset: Offset) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var scale by remember(initialScale) { mutableStateOf(initialScale) }
    var offset by remember(initialOffset) { mutableStateOf(initialOffset) }

    // Real per-pixel grading (filter + all Adjust tools) -> ONE colour matrix,
    // plus spatial effects (blur, sharpen, grain, vignette...). Exporter uses the same engine.
    val matrix = remember(filterPreset, filterIntensity, adjustValues) {
        PhotoColorEngine.buildMatrix(filterPreset, filterIntensity, adjustValues)
    }
    val effects = remember(filterPreset, filterIntensity, adjustValues) {
        PhotoColorEngine.effects(filterPreset, filterIntensity, adjustValues)
    }

    val boxModifier = modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(Color(0xFFCBD5E1))

    val finalModifier = if (!isDrawingMode) {
        boxModifier.pointerInput(uri) {
            // Pan / pinch-zoom with snap-to-centre, plus double-tap-to-reset.
            // Single taps are NOT consumed, so the parent's "select this box" tap still works.
            val snapThresholdPx = 20f
            var lastTapUptime = 0L
            var lastTapPos = Offset.Zero
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var rawOffset = offset            // un-snapped position, so slow drags can leave the centre
                var pastSlop = false
                var zoomAcc = 1f
                var panAcc = Offset.Zero
                var multiTouch = false
                var lastEventTime = down.uptimeMillis
                do {
                    val event = awaitPointerEvent()
                    event.changes.firstOrNull()?.let { lastEventTime = it.uptimeMillis }
                    if (event.changes.count { it.pressed } > 1) multiTouch = true
                    if (event.changes.none { it.isConsumed }) {
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        if (!pastSlop) {
                            zoomAcc *= zoom
                            panAcc += pan
                            val zoomMotion = kotlin.math.abs(1f - zoomAcc) * 200f   // same feel as detectTransformGestures
                            if (zoomMotion > viewConfiguration.touchSlop || panAcc.getDistance() > viewConfiguration.touchSlop) {
                                pastSlop = true
                            }
                        }
                        if (pastSlop) {
                            if (zoom != 1f || pan != Offset.Zero) {
                                scale = (scale * zoom).coerceIn(0.5f, 4f)
                                rawOffset += pan
                                offset = if (kotlin.math.abs(rawOffset.x) < snapThresholdPx &&
                                    kotlin.math.abs(rawOffset.y) < snapThresholdPx) Offset.Zero else rawOffset
                                onTransformChanged?.invoke(scale, offset)
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    }
                } while (event.changes.any { it.pressed })

                // finger(s) lifted without dragging = a tap; two quick taps = reset
                if (!pastSlop && !multiTouch) {
                    val now = lastEventTime
                    if (lastTapUptime != 0L && now - lastTapUptime < 300L &&
                        (down.position - lastTapPos).getDistance() < viewConfiguration.touchSlop * 4f) {
                        scale = 1f
                        offset = Offset.Zero
                        onTransformChanged?.invoke(scale, offset)
                        lastTapUptime = 0L
                    } else {
                        lastTapUptime = now
                        lastTapPos = down.position
                    }
                }
            }
        }
    } else {
        boxModifier
    }

    Box(modifier = finalModifier) {
        FilteredPhoto(
            uri = uri,
            matrix = matrix,
            effects = effects,
            modifier = Modifier.fillMaxSize(),
            canvasWidthPx = canvasWidthPx,
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