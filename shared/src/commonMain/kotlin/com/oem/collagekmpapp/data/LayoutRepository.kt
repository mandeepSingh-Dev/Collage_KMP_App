package com.oem.collagekmpapp.data

import com.oem.collagekmpapp.model.CollageLayout
import com.oem.collagekmpapp.model.Slot

class LayoutRepository {

    private fun rows(vararg heights: Float): List<Slot> {
        val slots = mutableListOf<Slot>()
        var currentTop = 0f
        for (h in heights) {
            val nextTop = currentTop + h
            slots.add(Slot(0f, currentTop, 1f, nextTop))
            currentTop = nextTop
        }
        return slots
    }

    private fun columns(vararg widths: Float): List<Slot> {
        val slots = mutableListOf<Slot>()
        var currentLeft = 0f
        for (w in widths) {
            val nextLeft = currentLeft + w
            slots.add(Slot(currentLeft, 0f, nextLeft, 1f))
            currentLeft = nextLeft
        }
        return slots
    }

    private fun grid(rowsCount: Int, colsCount: Int): List<Slot> {
        val slots = mutableListOf<Slot>()
        val rowHeight = 1f / rowsCount
        val colWidth = 1f / colsCount
        for (r in 0 until rowsCount) {
            for (c in 0 until colsCount) {
                val left = c * colWidth
                val top = r * rowHeight
                val right = if (c == colsCount - 1) 1f else (c + 1) * colWidth
                val bottom = if (r == rowsCount - 1) 1f else (r + 1) * rowHeight
                slots.add(Slot(left, top, right, bottom))
            }
        }
        return slots
    }

    private val allLayouts: List<CollageLayout> = listOf(
        // 1 Image
        CollageLayout("l1_full", "Full Canvas", 1, "Full single photo", rows(1f)),
        CollageLayout("l1_inset", "Inset Canvas", 1, "Single photo with border inset", listOf(Slot(0.05f, 0.05f, 0.95f, 0.95f))),

        // 2 Images
        CollageLayout("layout_2_vertical", "Split Vertical", 2, "2 photos side by side", columns(0.5f, 0.5f)),
        CollageLayout("layout_2_horizontal", "Split Horizontal", 2, "2 photos top & bottom", rows(0.5f, 0.5f)),
        CollageLayout("l2_left_1_3", "Left 1/3 + Right 2/3", 2, "Narrow left, wide right", columns(1f / 3f, 2f / 3f)),
        CollageLayout("l2_left_2_3", "Left 2/3 + Right 1/3", 2, "Wide left, narrow right", columns(2f / 3f, 1f / 3f)),
        CollageLayout("l2_top_1_3", "Top 1/3 + Bottom 2/3", 2, "Narrow top, tall bottom", rows(1f / 3f, 2f / 3f)),
        CollageLayout("l2_top_2_3", "Top 2/3 + Bottom 1/3", 2, "Tall top, narrow bottom", rows(2f / 3f, 1f / 3f)),

        // 3 Images
        CollageLayout("layout_3_top", "Triple Top", 3, "1 top, 2 bottom",
            listOf(Slot(0f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 0.5f, 1f), Slot(0.5f, 0.5f, 1f, 1f))
        ),
        CollageLayout("layout_3_left", "Triple Left", 3, "1 left, 2 right",
            listOf(Slot(0f, 0f, 0.5f, 1f), Slot(0.5f, 0f, 1f, 0.5f), Slot(0.5f, 0.5f, 1f, 1f))
        ),
        CollageLayout("l3_big_left", "Big Left + 2 Right", 3, "Large left + two stacked right",
            listOf(Slot(0f, 0f, 0.5f, 1f), Slot(0.5f, 0f, 1f, 0.5f), Slot(0.5f, 0.5f, 1f, 1f))
        ),
        CollageLayout("l3_big_right", "2 Left + Big Right", 3, "Two stacked left + large right",
            listOf(Slot(0f, 0f, 0.5f, 0.5f), Slot(0f, 0.5f, 0.5f, 1f), Slot(0.5f, 0f, 1f, 1f))
        ),
        CollageLayout("l3_big_top", "Big Top + 2 Bottom", 3, "Large top + two side-by-side bottom",
            listOf(Slot(0f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 0.5f, 1f), Slot(0.5f, 0.5f, 1f, 1f))
        ),
        CollageLayout("l3_big_bottom", "2 Top + Big Bottom", 3, "Two side-by-side top + large bottom",
            listOf(Slot(0f, 0f, 0.5f, 0.5f), Slot(0.5f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 1f, 1f))
        ),
        CollageLayout("l3_rows", "Three Equal Rows", 3, "Three horizontal rows", rows(1f/3f, 1f/3f, 1f/3f)),
        CollageLayout("l3_cols", "Three Equal Columns", 3, "Three vertical columns", columns(1f/3f, 1f/3f, 1f/3f)),
        CollageLayout("l3_left_col_two_stacked", "Left Col + 2 Stacked", 3, "1 left column + 2 stacked right",
            listOf(Slot(0f, 0f, 1f/3f, 1f), Slot(1f/3f, 0f, 1f, 0.5f), Slot(1f/3f, 0.5f, 1f, 1f))
        ),

        // 4 Images
        CollageLayout("layout_pinwheel", "Asymmetric Pinwheel", 4, "Dynamic split layout",
            listOf(Slot(0f, 0f, 0.6f, 0.6f), Slot(0.6f, 0f, 1f, 0.6f), Slot(0f, 0.6f, 0.4f, 1f), Slot(0.4f, 0.6f, 1f, 1f))
        ),
        CollageLayout("layout_vertical_strips", "Vertical Strips", 4, "3 vertical columns with middle split",
            listOf(Slot(0f, 0f, 1f/3f, 1f), Slot(1f/3f, 0f, 2f/3f, 0.66f), Slot(1f/3f, 0.66f, 2f/3f, 1f), Slot(2f/3f, 0f, 1f, 1f))
        ),
        CollageLayout("layout_top_right", "Top & Right Stack", 4, "Large left + 3 stacked right",
            listOf(Slot(0f, 0f, 0.6f, 1f), Slot(0.6f, 0f, 1f, 1f/3f), Slot(0.6f, 1f/3f, 1f, 2f/3f), Slot(0.6f, 2f/3f, 1f, 1f))
        ),
        CollageLayout("layout_large_top", "Large Top & 3 Bottom", 4, "Large top + 3 bottom tiles",
            listOf(Slot(0f, 0f, 1f, 0.6f), Slot(0f, 0.6f, 1f/3f, 1f), Slot(1f/3f, 0.6f, 2f/3f, 1f), Slot(2f/3f, 0.6f, 1f, 1f))
        ),
        CollageLayout("l4_grid_2x2", "2x2 Grid", 4, "Four equal grid tiles", grid(2, 2)),
        CollageLayout("l4_rows", "Four Equal Rows", 4, "Four horizontal rows", rows(0.25f, 0.25f, 0.25f, 0.25f)),
        CollageLayout("l4_cols", "Four Equal Columns", 4, "Four vertical columns", columns(0.25f, 0.25f, 0.25f, 0.25f)),
        CollageLayout("l4_mix_2col_left", "2 Tall Left + 2 Stacked", 4, "Two tall left columns + two stacked right",
            listOf(Slot(0f, 0f, 0.25f, 1f), Slot(0.25f, 0f, 0.5f, 1f), Slot(0.5f, 0f, 1f, 0.5f), Slot(0.5f, 0.5f, 1f, 1f))
        ),
        CollageLayout("l4_unequal_rows", "Unequal Width Rows", 4, "Top (1/3 + 2/3) over bottom (2/3 + 1/3)",
            listOf(Slot(0f, 0f, 1f/3f, 0.5f), Slot(1f/3f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 2f/3f, 1f), Slot(2f/3f, 0.5f, 1f, 1f))
        ),

        // 5 Images
        CollageLayout("layout_5_grid", "Multi Grid", 5, "Five photo custom layout",
            listOf(Slot(0f, 0f, 0.5f, 0.5f), Slot(0.5f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 1f/3f, 1f), Slot(1f/3f, 0.5f, 2f/3f, 1f), Slot(2f/3f, 0.5f, 1f, 1f))
        ),
        CollageLayout("l5_top2_bottom3", "Top 2 + Bottom 3", 5, "Two top tiles + three bottom tiles",
            listOf(Slot(0f, 0f, 0.5f, 0.5f), Slot(0.5f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 1f/3f, 1f), Slot(1f/3f, 0.5f, 2f/3f, 1f), Slot(2f/3f, 0.5f, 1f, 1f))
        ),
        CollageLayout("l5_top3_bottom2", "Top 3 + Bottom 2", 5, "Three top tiles + two bottom tiles",
            listOf(Slot(0f, 0f, 1f/3f, 0.5f), Slot(1f/3f, 0f, 2f/3f, 0.5f), Slot(2f/3f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 0.5f, 1f), Slot(0.5f, 0.5f, 1f, 1f))
        ),
        CollageLayout("l5_big_left_grid", "Big Left + 2x2 Grid Right", 5, "Large left + 2x2 grid right",
            listOf(Slot(0f, 0f, 0.5f, 1f), Slot(0.5f, 0f, 0.75f, 0.5f), Slot(0.75f, 0f, 1f, 0.5f), Slot(0.5f, 0.5f, 0.75f, 1f), Slot(0.75f, 0.5f, 1f, 1f))
        ),
        CollageLayout("l5_big_top_grid", "Big Top + 2x2 Grid Bottom", 5, "Large top + 2x2 grid bottom",
            listOf(Slot(0f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 0.5f, 0.75f), Slot(0.5f, 0.5f, 1f, 0.75f), Slot(0f, 0.75f, 0.5f, 1f), Slot(0.5f, 0.75f, 1f, 1f))
        ),
        CollageLayout("l5_rows", "Five Equal Rows", 5, "Five horizontal rows", rows(0.2f, 0.2f, 0.2f, 0.2f, 0.2f)),
        CollageLayout("l5_cols", "Five Equal Columns", 5, "Five vertical columns", columns(0.2f, 0.2f, 0.2f, 0.2f, 0.2f)),

        // 6 Images
        CollageLayout("l6_grid_2x3", "2x3 Grid", 6, "2 columns, 3 rows", grid(3, 2)),
        CollageLayout("l6_grid_3x2", "3x2 Grid", 6, "3 columns, 2 rows", grid(2, 3)),
        CollageLayout("l6_big_topleft", "Big Top-Left + 5 Small", 6, "Large top-left 2/3 + 5 surrounding slots",
            listOf(Slot(0f, 0f, 2f/3f, 2f/3f), Slot(2f/3f, 0f, 1f, 1f/3f), Slot(2f/3f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 1f/3f, 1f), Slot(1f/3f, 2f/3f, 2f/3f, 1f), Slot(2f/3f, 2f/3f, 1f, 1f))
        ),
        CollageLayout("l6_unequal_rows_3", "Alternating Unequal Rows (3)", 6, "Three rows of 2 with varying widths",
            listOf(Slot(0f, 0f, 1f/3f, 1f/3f), Slot(1f/3f, 0f, 1f, 1f/3f), Slot(0f, 1f/3f, 2f/3f, 2f/3f), Slot(2f/3f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 1f/3f, 1f), Slot(1f/3f, 2f/3f, 1f, 1f))
        ),
        CollageLayout("l6_unequal_rows_2", "Alternating Unequal Rows (2)", 6, "Two rows of 3 with varying widths",
            listOf(Slot(0f, 0f, 0.2f, 0.5f), Slot(0.2f, 0f, 0.7f, 0.5f), Slot(0.7f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 0.3f, 1f), Slot(0.3f, 0.5f, 0.6f, 1f), Slot(0.6f, 0.5f, 1f, 1f))
        ),

        // 7 Images
        CollageLayout("l7_grid", "7-Photo Grid Mix", 7, "Top 3, middle 2, bottom 2",
            listOf(Slot(0f, 0f, 1f/3f, 1f/3f), Slot(1f/3f, 0f, 2f/3f, 1f/3f), Slot(2f/3f, 0f, 1f, 1f/3f), Slot(0f, 1f/3f, 0.5f, 2f/3f), Slot(0.5f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 0.5f, 1f), Slot(0.5f, 2f/3f, 1f, 1f))
        ),
        CollageLayout("l7_mixed", "7-Photo Mixed Layout", 7, "Top 2, middle 3, bottom 2",
            listOf(Slot(0f, 0f, 0.5f, 1f/3f), Slot(0.5f, 0f, 1f, 1f/3f), Slot(0f, 1f/3f, 1f/3f, 2f/3f), Slot(1f/3f, 1f/3f, 2f/3f, 2f/3f), Slot(2f/3f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 0.5f, 1f), Slot(0.5f, 2f/3f, 1f, 1f))
        ),
        CollageLayout("l7_cols", "Seven Equal Columns", 7, "Seven vertical columns",
            columns(1f/7f, 1f/7f, 1f/7f, 1f/7f, 1f/7f, 1f/7f, 1f/7f)
        ),

        // 8 Images
        CollageLayout("l8_grid_2x4", "2x4 Grid", 8, "2 columns, 4 rows", grid(4, 2)),
        CollageLayout("l8_grid_4x2", "4x2 Grid", 8, "4 columns, 2 rows", grid(2, 4)),
        CollageLayout("l8_mixed", "8-Photo Mixed Grid", 8, "Top 3, middle 3, bottom 2",
            listOf(Slot(0f, 0f, 1f/3f, 1f/3f), Slot(1f/3f, 0f, 2f/3f, 1f/3f), Slot(2f/3f, 0f, 1f, 1f/3f), Slot(0f, 1f/3f, 1f/3f, 2f/3f), Slot(1f/3f, 1f/3f, 2f/3f, 2f/3f), Slot(2f/3f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 0.5f, 1f), Slot(0.5f, 2f/3f, 1f, 1f))
        ),

        // 9 Images
        CollageLayout("l9_grid_3x3", "3x3 Grid", 9, "3x3 equal grid", grid(3, 3)),
        CollageLayout("l9_mixed", "9-Photo Mixed Grid", 9, "Top 2, middle 4, bottom 3",
            listOf(Slot(0f, 0f, 0.5f, 1f/3f), Slot(0.5f, 0f, 1f, 1f/3f), Slot(0f, 1f/3f, 0.25f, 2f/3f), Slot(0.25f, 1f/3f, 0.5f, 2f/3f), Slot(0.5f, 1f/3f, 0.75f, 2f/3f), Slot(0.75f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 1f/3f, 1f), Slot(1f/3f, 2f/3f, 2f/3f, 1f), Slot(2f/3f, 2f/3f, 1f, 1f))
        ),
        CollageLayout("l9_cols", "Nine Equal Columns", 9, "Nine vertical columns",
            columns(1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f)
        )
    )

    fun getLayouts(count: Int): List<CollageLayout> {
        val exactMatch = allLayouts.filter { it.slotCount == count }
        if (exactMatch.isNotEmpty()) return exactMatch
        val rowsCount = kotlin.math.sqrt(count.toDouble()).toInt().coerceAtLeast(1)
        val colsCount = kotlin.math.ceil(count.toDouble() / rowsCount).toInt().coerceAtLeast(1)
        return listOf(
            CollageLayout(
                id = "layout_${count}_custom_grid",
                name = "$count-Photo Custom Grid",
                slotCount = count,
                description = "$count photos dynamic grid",
                slots = grid(rowsCount, colsCount).take(count)
            )
        )
    }

    fun getAllLayouts(): List<CollageLayout> = allLayouts
}
