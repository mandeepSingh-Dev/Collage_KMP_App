package com.oem.collagekmpapp

import com.oem.collagekmpapp.data.LayoutRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTime

class LayoutRepositoryTest {

    @Test
    fun testLayoutRepositoryIntegrityAndRules() {
        val repo = LayoutRepository()
        val maxCount = 50
        val layoutCountsSummary = mutableMapOf<Int, Int>()

        val duration = measureTime {
            for (count in 1..maxCount) {
                val layouts = repo.getLayouts(count)
                assertTrue(layouts.isNotEmpty(), "Layouts for count $count should not be empty")
                layoutCountsSummary[count] = layouts.size

                val countIds = mutableSetOf<String>()
                val countSlotSignatures = mutableSetOf<String>()

                for (layout in layouts) {
                    assertEquals(count, layout.slotCount, "Layout ${layout.id} slotCount should match requested count $count")
                    assertEquals(count, layout.slots.size, "Layout ${layout.id} should have exactly $count slots")
                    assertTrue(countIds.add(layout.id), "Layout ID ${layout.id} must be unique within count $count")

                    // Verify slot list uniqueness for the same n
                    val slotSig = layout.slots.joinToString(";") { "${it.left},${it.top},${it.right},${it.bottom}" }
                    assertTrue(countSlotSignatures.add(slotSig), "No two layouts for count $count should have identical slot lists (${layout.id})")

                    var totalArea = 0f
                    for (slot in layout.slots) {
                        assertTrue(slot.left >= 0f && slot.left <= 1f, "Left out of bounds (0..1): ${slot.left} in ${layout.id}")
                        assertTrue(slot.top >= 0f && slot.top <= 1f, "Top out of bounds (0..1): ${slot.top} in ${layout.id}")
                        assertTrue(slot.right > slot.left && slot.right <= 1f, "Right must be > left and <= 1: ${slot.right} in ${layout.id}")
                        assertTrue(slot.bottom > slot.top && slot.bottom <= 1f, "Bottom must be > top and <= 1: ${slot.bottom} in ${layout.id}")

                        val area = (slot.right - slot.left) * (slot.bottom - slot.top)
                        totalArea += area
                    }

                    // Inset layout (l1_inset) is intentionally padded and non-tiled (area = 0.81)
                    if (layout.id != "l1_inset") {
                        assertEquals(1.0f, totalArea, 0.001f, "Total slot area for tiled layout ${layout.id} should sum to 1.0, got $totalArea")
                    }
                }
            }
        }

        println("Layout generation for counts 1..$maxCount completed in ${duration.inWholeMilliseconds} ms")
        assertTrue(duration.inWholeMilliseconds < 1000, "Layout generation took too long: ${duration.inWholeMilliseconds} ms (must be under 1 second)")

        println("=== Layout Count Summary (1 to 50) ===")
        for (c in 1..maxCount) {
            println("Image Count $c: ${layoutCountsSummary[c]} layouts")
        }
    }
}
