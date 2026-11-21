package com.oem.collagekmpapp

import com.oem.collagekmpapp.data.LayoutRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LayoutRepositoryTest {

    @Test
    fun testLayoutRepositoryIntegrityAndRules() {
        val repo = LayoutRepository()
        val maxCount = 9
        val allIds = mutableSetOf<String>()

        val layoutCountsSummary = mutableMapOf<Int, Int>()

        for (count in 1..maxCount) {
            val layouts = repo.getLayouts(count)
            assertTrue(layouts.isNotEmpty(), "Layouts for count $count should not be empty")
            layoutCountsSummary[count] = layouts.size

            for (layout in layouts) {
                assertEquals(count, layout.slotCount, "Layout ${layout.id} slotCount should match requested count $count")
                assertEquals(count, layout.slots.size, "Layout ${layout.id} should have exactly $count slots")
                assertTrue(allIds.add(layout.id), "Layout ID ${layout.id} must be unique across all layouts")

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

        println("Layouts per image count summary: $layoutCountsSummary")
    }
}
