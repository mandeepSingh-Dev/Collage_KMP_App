package com.oem.collagekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.oem.collagekmpapp.model.Slot
import kotlin.math.abs

/* ------------------------------------------------------------------------- */
/*  Pure resize logic (no Compose, easy to unit test)                         */
/* ------------------------------------------------------------------------- */

enum class SlotEdge { LEFT, TOP, RIGHT, BOTTOM }

const val MIN_SLOT_FRACTION = 0.15f
// Tolerance for "same edge line" matching. Layouts rarely line up to the exact float.
private const val EPS = 0.05f

/**
 * Show a handle on every edge that is NOT the outer border of the collage.
 * (Does not require an exact neighbour match, so any layout shape works.)
 */
fun canResizeEdge(slots: List<Slot>, index: Int, edge: SlotEdge): Boolean {
    val v = slots[index].edgeValue(edge)
    val isOuter = when (edge) {
        SlotEdge.LEFT, SlotEdge.TOP -> v < EPS
        SlotEdge.RIGHT, SlotEdge.BOTTOM -> v > 1f - EPS
    }
    return !isOuter
}

private fun SlotEdge.isVertical() = this == SlotEdge.LEFT || this == SlotEdge.RIGHT

private fun Slot.edgeValue(edge: SlotEdge): Float = when (edge) {
    SlotEdge.LEFT -> left
    SlotEdge.RIGHT -> right
    SlotEdge.TOP -> top
    SlotEdge.BOTTOM -> bottom
}

/**
 * Universal 2-box resize logic supporting any layout shape (grids, pinwheel, hero, mixed, 3rd+ shapes).
 */
fun resizeDivider(
    startSlots: List<Slot>,
    index: Int,
    edge: SlotEdge,
    deltaFraction: Float,
    minSize: Float = MIN_SLOT_FRACTION,
): List<Slot> {
    val s = startSlots[index]
    val line = s.edgeValue(edge)

    // Find the direct neighbor touching this edge
    val neighborIdx = startSlots.indices.firstOrNull { i ->
        if (i == index) return@firstOrNull false
        val o = startSlots[i]
        when (edge) {
            SlotEdge.LEFT -> abs(o.right - line) < EPS && o.top < s.bottom - EPS && o.bottom > s.top + EPS
            SlotEdge.RIGHT -> abs(o.left - line) < EPS && o.top < s.bottom - EPS && o.bottom > s.top + EPS
            SlotEdge.TOP -> abs(o.bottom - line) < EPS && o.left < s.right - EPS && o.right > s.left + EPS
            SlotEdge.BOTTOM -> abs(o.top - line) < EPS && o.left < s.right - EPS && o.right > s.left + EPS
        }
    }

    var d = deltaFraction
    when (edge) {
        SlotEdge.LEFT -> {
            val newLeft = (s.left + d).coerceIn(0f, s.right - minSize)
            d = newLeft - s.left
            if (neighborIdx != null) {
                val o = startSlots[neighborIdx]
                if (o.right + d - o.left < minSize) {
                    d = o.left + minSize - o.right
                }
            }
        }
        SlotEdge.RIGHT -> {
            val newRight = (s.right + d).coerceIn(s.left + minSize, 1f)
            d = newRight - s.right
            if (neighborIdx != null) {
                val o = startSlots[neighborIdx]
                if (o.right - (o.left + d) < minSize) {
                    d = o.right - minSize - o.left
                }
            }
        }
        SlotEdge.TOP -> {
            val newTop = (s.top + d).coerceIn(0f, s.bottom - minSize)
            d = newTop - s.top
            if (neighborIdx != null) {
                val o = startSlots[neighborIdx]
                if (o.bottom + d - o.top < minSize) {
                    d = o.top + minSize - o.bottom
                }
            }
        }
        SlotEdge.BOTTOM -> {
            val newBottom = (s.bottom + d).coerceIn(s.top + minSize, 1f)
            d = newBottom - s.bottom
            if (neighborIdx != null) {
                val o = startSlots[neighborIdx]
                if (o.bottom - (o.top + d) < minSize) {
                    d = o.bottom - minSize - o.top
                }
            }
        }
    }

    return startSlots.mapIndexed { i, slot ->
        if (i == index) {
            when (edge) {
                SlotEdge.LEFT -> slot.copy(left = slot.left + d)
                SlotEdge.RIGHT -> slot.copy(right = slot.right + d)
                SlotEdge.TOP -> slot.copy(top = slot.top + d)
                SlotEdge.BOTTOM -> slot.copy(bottom = slot.bottom + d)
            }
        } else if (i == neighborIdx) {
            when (edge) {
                SlotEdge.LEFT -> slot.copy(right = slot.right + d)
                SlotEdge.RIGHT -> slot.copy(left = slot.left + d)
                SlotEdge.TOP -> slot.copy(bottom = slot.bottom + d)
                SlotEdge.BOTTOM -> slot.copy(top = slot.top + d)
            }
        } else {
            slot
        }
    }
}

/**
 * Same behaviour as [resizeDivider], but EVERY slot that touches the dragged edge moves with it.
 * (resizeDivider only moves the first touching neighbour, so in layouts like "1 big left + 2 stacked
 * right" dragging the big box left a gap / overlap next to the second stacked box.)
 * MIN_SLOT_FRACTION is enforced for the dragged box and for all of its neighbours.
 */
fun resizeDividerSynced(
    startSlots: List<Slot>,
    index: Int,
    edge: SlotEdge,
    deltaFraction: Float,
    minSize: Float = MIN_SLOT_FRACTION,
): List<Slot> {
    val s = startSlots[index]
    val line = s.edgeValue(edge)

    val neighbors = startSlots.indices.filter { i ->
        if (i == index) return@filter false
        val o = startSlots[i]
        when (edge) {
            SlotEdge.LEFT -> abs(o.right - line) < EPS && o.top < s.bottom - EPS && o.bottom > s.top + EPS
            SlotEdge.RIGHT -> abs(o.left - line) < EPS && o.top < s.bottom - EPS && o.bottom > s.top + EPS
            SlotEdge.TOP -> abs(o.bottom - line) < EPS && o.left < s.right - EPS && o.right > s.left + EPS
            SlotEdge.BOTTOM -> abs(o.top - line) < EPS && o.left < s.right - EPS && o.right > s.left + EPS
        }
    }

    // clamp the dragged box itself
    var d = when (edge) {
        SlotEdge.LEFT -> (s.left + deltaFraction).coerceIn(0f, s.right - minSize) - s.left
        SlotEdge.RIGHT -> (s.right + deltaFraction).coerceIn(s.left + minSize, 1f) - s.right
        SlotEdge.TOP -> (s.top + deltaFraction).coerceIn(0f, s.bottom - minSize) - s.top
        SlotEdge.BOTTOM -> (s.bottom + deltaFraction).coerceIn(s.top + minSize, 1f) - s.bottom
    }
    // clamp against EVERY neighbour so none of them collapses below the minimum
    neighbors.forEach { n ->
        val o = startSlots[n]
        when (edge) {
            SlotEdge.LEFT -> if (o.right + d - o.left < minSize) d = o.left + minSize - o.right
            SlotEdge.RIGHT -> if (o.right - (o.left + d) < minSize) d = o.right - minSize - o.left
            SlotEdge.TOP -> if (o.bottom + d - o.top < minSize) d = o.top + minSize - o.bottom
            SlotEdge.BOTTOM -> if (o.bottom - (o.top + d) < minSize) d = o.bottom - minSize - o.top
        }
    }

    return startSlots.mapIndexed { i, slot ->
        when {
            i == index -> when (edge) {
                SlotEdge.LEFT -> slot.copy(left = slot.left + d)
                SlotEdge.RIGHT -> slot.copy(right = slot.right + d)
                SlotEdge.TOP -> slot.copy(top = slot.top + d)
                SlotEdge.BOTTOM -> slot.copy(bottom = slot.bottom + d)
            }
            i in neighbors -> when (edge) {
                SlotEdge.LEFT -> slot.copy(right = slot.right + d)
                SlotEdge.RIGHT -> slot.copy(left = slot.left + d)
                SlotEdge.TOP -> slot.copy(bottom = slot.bottom + d)
                SlotEdge.BOTTOM -> slot.copy(top = slot.top + d)
            }
            else -> slot
        }
    }
}

/* ------------------------------------------------------------------------- */
/*  Handle composable                                                         */
/* ------------------------------------------------------------------------- */

@Composable
fun BoxScope.ResizeHandle(
    edge: SlotEdge,
    index: Int,
    currentSlots: () -> List<Slot>,
    containerSizePx: Float,
    onSlotsChanged: (List<Slot>) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val vertical = edge.isVertical()

    // Always read the LATEST values inside the long-living pointerInput block
    val latestSlots by rememberUpdatedState(currentSlots)
    val latestOnChanged by rememberUpdatedState(onSlotsChanged)
    val latestSize by rememberUpdatedState(containerSizePx.coerceAtLeast(1f))

    val alignment = when (edge) {
        SlotEdge.LEFT -> Alignment.CenterStart
        SlotEdge.RIGHT -> Alignment.CenterEnd
        SlotEdge.TOP -> Alignment.TopCenter
        SlotEdge.BOTTOM -> Alignment.BottomCenter
    }

    // Big touch target; horizontal handles are inset so they don't fight over the corners
    val hitArea = if (vertical) Modifier.width(36.dp).fillMaxHeight()
    else Modifier.fillMaxWidth().padding(horizontal = 36.dp).height(36.dp)

    Box(
        modifier = Modifier
            .align(alignment)
            .then(hitArea)
            .pointerInput(index, edge) {
                var startSlots: List<Slot> = emptyList()
                var totalPx = 0f
                // haptic "snap" tracking (feedback only – slot geometry is unchanged)
                var wasClamped = false
                var lastSideOfCenter = false
                detectDragGestures(
                    onDragStart = {
                        startSlots = latestSlots()
                        totalPx = 0f
                        wasClamped = false
                        lastSideOfCenter = startSlots.getOrNull(index)?.edgeValue(edge)?.let { it > 0.5f } ?: false
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalPx += if (vertical) dragAmount.x else dragAmount.y
                        if (startSlots.isNotEmpty()) {
                            val requested = totalPx / latestSize
                            val newSlots = resizeDividerSynced(startSlots, index, edge, requested)
                            latestOnChanged(newSlots)

                            val newEdge = newSlots[index].edgeValue(edge)
                            val moved = newEdge - startSlots[index].edgeValue(edge)

                            // 1) snapped against a limit (MIN_SLOT_FRACTION or the collage border)
                            val clamped = abs(moved - requested) > 0.002f
                            if (clamped && !wasClamped) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            wasClamped = clamped

                            // 2) light detent when the divider crosses the even-split (50 %) line
                            val side = newEdge > 0.5f
                            if (side != lastSideOfCenter) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastSideOfCenter = side
                        }
                    },
                )
            },
        contentAlignment = alignment,
    ) {
        Box(
            modifier = Modifier
                .then(if (vertical) Modifier.width(6.dp).height(40.dp) else Modifier.width(40.dp).height(6.dp))
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF2563EB))
        )
    }
}