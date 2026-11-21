package com.oem.collagekmpapp.data

import com.oem.collagekmpapp.model.CollageLayout
import com.oem.collagekmpapp.model.Slot
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

class LayoutRepository {

    private fun rows(vararg heights: Float): List<Slot> {
        val slots = mutableListOf<Slot>()
        var currentTop = 0f
        for (h in heights) {
            val nextTop = currentTop + h
            slots.add(Slot(0f, currentTop, 1f, nextTop))
            currentTop = nextTop
        }
        return normalizeSlotOrder(slots)
    }

    private fun columns(vararg widths: Float): List<Slot> {
        val slots = mutableListOf<Slot>()
        var currentLeft = 0f
        for (w in widths) {
            val nextLeft = currentLeft + w
            slots.add(Slot(currentLeft, 0f, nextLeft, 1f))
            currentLeft = nextLeft
        }
        return normalizeSlotOrder(slots)
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
        return normalizeSlotOrder(slots)
    }

    private fun normalizeSlotOrder(slots: List<Slot>): List<Slot> {
        return slots.sortedWith(compareBy({ (it.top * 10000).toInt() }, { (it.left * 10000).toInt() }))
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
            normalizeSlotOrder(listOf(Slot(0f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 0.5f, 1f), Slot(0.5f, 0.5f, 1f, 1f)))
        ),
        CollageLayout("layout_3_left", "Triple Left", 3, "1 left, 2 right",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 0.5f, 1f), Slot(0.5f, 0f, 1f, 0.5f), Slot(0.5f, 0.5f, 1f, 1f)))
        ),
        CollageLayout("l3_big_right", "2 Left + Big Right", 3, "Two stacked left + large right",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 0.5f, 0.5f), Slot(0f, 0.5f, 0.5f, 1f), Slot(0.5f, 0f, 1f, 1f)))
        ),
        CollageLayout("l3_big_bottom", "2 Top + Big Bottom", 3, "Two side-by-side top + large bottom",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 0.5f, 0.5f), Slot(0.5f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 1f, 1f)))
        ),
        CollageLayout("l3_rows", "Three Equal Rows", 3, "Three horizontal rows", rows(1f/3f, 1f/3f, 1f/3f)),
        CollageLayout("l3_cols", "Three Equal Columns", 3, "Three vertical columns", columns(1f/3f, 1f/3f, 1f/3f)),
        CollageLayout("l3_left_col_two_stacked", "Left Col + 2 Stacked", 3, "1 left column + 2 stacked right",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 1f/3f, 1f), Slot(1f/3f, 0f, 1f, 0.5f), Slot(1f/3f, 0.5f, 1f, 1f)))
        ),

        // 4 Images
        CollageLayout("layout_pinwheel", "Asymmetric Pinwheel", 4, "Dynamic split layout",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 0.6f, 0.6f), Slot(0.6f, 0f, 1f, 0.6f), Slot(0f, 0.6f, 0.4f, 1f), Slot(0.4f, 0.6f, 1f, 1f)))
        ),
        CollageLayout("layout_vertical_strips", "Vertical Strips", 4, "3 vertical columns with middle split",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 1f/3f, 1f), Slot(1f/3f, 0f, 2f/3f, 0.66f), Slot(1f/3f, 0.66f, 2f/3f, 1f), Slot(2f/3f, 0f, 1f, 1f)))
        ),
        CollageLayout("layout_top_right", "Top & Right Stack", 4, "Large left + 3 stacked right",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 0.6f, 1f), Slot(0.6f, 0f, 1f, 1f/3f), Slot(0.6f, 1f/3f, 1f, 2f/3f), Slot(0.6f, 2f/3f, 1f, 1f)))
        ),
        CollageLayout("layout_large_top", "Large Top & 3 Bottom", 4, "Large top + 3 bottom tiles",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 1f, 0.6f), Slot(0f, 0.6f, 1f/3f, 1f), Slot(1f/3f, 0.6f, 2f/3f, 1f), Slot(2f/3f, 0.6f, 1f, 1f)))
        ),
        CollageLayout("l4_grid_2x2", "2x2 Grid", 4, "Four equal grid tiles", grid(2, 2)),
        CollageLayout("l4_rows", "Four Equal Rows", 4, "Four horizontal rows", rows(0.25f, 0.25f, 0.25f, 0.25f)),
        CollageLayout("l4_cols", "Four Equal Columns", 4, "Four vertical columns", columns(0.25f, 0.25f, 0.25f, 0.25f)),
        CollageLayout("l4_mix_2col_left", "2 Tall Left + 2 Stacked", 4, "Two tall left columns + two stacked right",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 0.25f, 1f), Slot(0.25f, 0f, 0.5f, 1f), Slot(0.5f, 0f, 1f, 0.5f), Slot(0.5f, 0.5f, 1f, 1f)))
        ),
        CollageLayout("l4_unequal_rows", "Unequal Width Rows", 4, "Top (1/3 + 2/3) over bottom (2/3 + 1/3)",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 1f/3f, 0.5f), Slot(1f/3f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 2f/3f, 1f), Slot(2f/3f, 0.5f, 1f, 1f)))
        ),

        // 5 Images
        CollageLayout("layout_5_grid", "Multi Grid", 5, "Five photo custom layout",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 0.5f, 0.5f), Slot(0.5f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 1f/3f, 1f), Slot(1f/3f, 0.5f, 2f/3f, 1f), Slot(2f/3f, 0.5f, 1f, 1f)))
        ),
        CollageLayout("l5_top3_bottom2", "Top 3 + Bottom 2", 5, "Three top tiles + two bottom tiles",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 1f/3f, 0.5f), Slot(1f/3f, 0f, 2f/3f, 0.5f), Slot(2f/3f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 0.5f, 1f), Slot(0.5f, 0.5f, 1f, 1f)))
        ),
        CollageLayout("l5_big_left_grid", "Big Left + 2x2 Grid Right", 5, "Large left + 2x2 grid right",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 0.5f, 1f), Slot(0.5f, 0f, 0.75f, 0.5f), Slot(0.75f, 0f, 1f, 0.5f), Slot(0.5f, 0.5f, 0.75f, 1f), Slot(0.75f, 0.5f, 1f, 1f)))
        ),
        CollageLayout("l5_big_top_grid", "Big Top + 2x2 Grid Bottom", 5, "Large top + 2x2 grid bottom",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 0.5f, 0.75f), Slot(0.5f, 0.5f, 1f, 0.75f), Slot(0f, 0.75f, 0.5f, 1f), Slot(0.5f, 0.75f, 1f, 1f)))
        ),
        CollageLayout("l5_rows", "Five Equal Rows", 5, "Five horizontal rows", rows(0.2f, 0.2f, 0.2f, 0.2f, 0.2f)),
        CollageLayout("l5_cols", "Five Equal Columns", 5, "Five vertical columns", columns(0.2f, 0.2f, 0.2f, 0.2f, 0.2f)),

        // 6 Images
        CollageLayout("l6_grid_2x3", "2x3 Grid", 6, "2 columns, 3 rows", grid(3, 2)),
        CollageLayout("l6_grid_3x2", "3x2 Grid", 6, "3 columns, 2 rows", grid(2, 3)),
        CollageLayout("l6_big_topleft", "Big Top-Left + 5 Small", 6, "Large top-left 2/3 + 5 surrounding slots",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 2f/3f, 2f/3f), Slot(2f/3f, 0f, 1f, 1f/3f), Slot(2f/3f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 1f/3f, 1f), Slot(1f/3f, 2f/3f, 2f/3f, 1f), Slot(2f/3f, 2f/3f, 1f, 1f)))
        ),
        CollageLayout("l6_unequal_rows_3", "Alternating Unequal Rows (3)", 6, "Three rows of 2 with varying widths",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 1f/3f, 1f/3f), Slot(1f/3f, 0f, 1f, 1f/3f), Slot(0f, 1f/3f, 2f/3f, 2f/3f), Slot(2f/3f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 1f/3f, 1f), Slot(1f/3f, 2f/3f, 1f, 1f)))
        ),
        CollageLayout("l6_unequal_rows_2", "Alternating Unequal Rows (2)", 6, "Two rows of 3 with varying widths",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 0.2f, 0.5f), Slot(0.2f, 0f, 0.7f, 0.5f), Slot(0.7f, 0f, 1f, 0.5f), Slot(0f, 0.5f, 0.3f, 1f), Slot(0.3f, 0.5f, 0.6f, 1f), Slot(0.6f, 0.5f, 1f, 1f)))
        ),

        // 7 Images
        CollageLayout("l7_grid", "7-Photo Grid Mix", 7, "Top 3, middle 2, bottom 2",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 1f/3f, 1f/3f), Slot(1f/3f, 0f, 2f/3f, 1f/3f), Slot(2f/3f, 0f, 1f, 1f/3f), Slot(0f, 1f/3f, 0.5f, 2f/3f), Slot(0.5f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 0.5f, 1f), Slot(0.5f, 2f/3f, 1f, 1f)))
        ),
        CollageLayout("l7_mixed", "7-Photo Mixed Layout", 7, "Top 2, middle 3, bottom 2",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 0.5f, 1f/3f), Slot(0.5f, 0f, 1f, 1f/3f), Slot(0f, 1f/3f, 1f/3f, 2f/3f), Slot(1f/3f, 1f/3f, 2f/3f, 2f/3f), Slot(2f/3f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 0.5f, 1f), Slot(0.5f, 2f/3f, 1f, 1f)))
        ),
        CollageLayout("l7_cols", "Seven Equal Columns", 7, "Seven vertical columns",
            columns(1f/7f, 1f/7f, 1f/7f, 1f/7f, 1f/7f, 1f/7f, 1f/7f)
        ),

        // 8 Images
        CollageLayout("l8_grid_2x4", "2x4 Grid", 8, "2 columns, 4 rows", grid(4, 2)),
        CollageLayout("l8_grid_4x2", "4x2 Grid", 8, "4 columns, 2 rows", grid(2, 4)),
        CollageLayout("l8_mixed", "8-Photo Mixed Grid", 8, "Top 3, middle 3, bottom 2",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 1f/3f, 1f/3f), Slot(1f/3f, 0f, 2f/3f, 1f/3f), Slot(2f/3f, 0f, 1f, 1f/3f), Slot(0f, 1f/3f, 1f/3f, 2f/3f), Slot(1f/3f, 1f/3f, 2f/3f, 2f/3f), Slot(2f/3f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 0.5f, 1f), Slot(0.5f, 2f/3f, 1f, 1f)))
        ),

        // 9 Images
        CollageLayout("l9_grid_3x3", "3x3 Grid", 9, "3x3 equal grid", grid(3, 3)),
        CollageLayout("l9_mixed", "9-Photo Mixed Grid", 9, "Top 2, middle 4, bottom 3",
            normalizeSlotOrder(listOf(Slot(0f, 0f, 0.5f, 1f/3f), Slot(0.5f, 0f, 1f, 1f/3f), Slot(0f, 1f/3f, 0.25f, 2f/3f), Slot(0.25f, 1f/3f, 0.5f, 2f/3f), Slot(0.5f, 1f/3f, 0.75f, 2f/3f), Slot(0.75f, 1f/3f, 1f, 2f/3f), Slot(0f, 2f/3f, 1f/3f, 1f), Slot(1f/3f, 2f/3f, 2f/3f, 1f), Slot(2f/3f, 2f/3f, 1f, 1f)))
        ),
        CollageLayout("l9_cols", "Nine Equal Columns", 9, "Nine vertical columns",
            columns(1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f, 1f/9f)
        )
    )

    companion object {
        private val cache = mutableMapOf<Int, List<CollageLayout>>()
    }

    private fun generateEqualGrids(n: Int): List<List<Slot>> {
        val results = mutableListOf<List<Slot>>()
        val possiblePairs = mutableListOf<Pair<Int, Int>>()
        for (r in 1..n) {
            for (c in 1..n) {
                if (r * c >= n) {
                    possiblePairs.add(Pair(r, c))
                }
            }
        }
        possiblePairs.sortBy { abs(it.first - it.second) }

        for ((r, c) in possiblePairs.take(4)) {
            val gridSlots = createGridWithLastRowStretch(n, r, c)
            if (gridSlots != null) results.add(gridSlots)
            if (r != c) {
                val transSlots = createGridWithLastRowStretch(n, c, r)
                if (transSlots != null) results.add(transSlots)
            }
        }
        return results
    }

    private fun createGridWithLastRowStretch(n: Int, r: Int, c: Int): List<Slot>? {
        if (r * c < n) return null
        val fullRows = n / c
        val rem = n % c
        val totalRows = if (rem == 0) fullRows else fullRows + 1
        if (totalRows > r) return null

        val slots = mutableListOf<Slot>()
        val rowHeight = 1f / totalRows

        for (row in 0 until fullRows) {
            val rowTop = row * rowHeight
            val rowBottom = (row + 1) * rowHeight
            val colWidth = 1f / c
            for (col in 0 until c) {
                val left = col * colWidth
                val right = (col + 1) * colWidth
                slots.add(Slot(left, rowTop, right, rowBottom))
            }
        }

        if (rem > 0) {
            val rowTop = fullRows * rowHeight
            val rowBottom = 1f
            val colWidth = 1f / rem
            for (col in 0 until rem) {
                val left = col * colWidth
                val right = (col + 1) * colWidth
                slots.add(Slot(left, rowTop, right, rowBottom))
            }
        }

        if (slots.size != n) return null
        return normalizeSlotOrder(slots)
    }

    private fun generateRowBasedLayouts(n: Int): List<List<Slot>> {
        val results = mutableListOf<List<Slot>>()
        for (k in 2..minOf(6, n)) {
            val base = n / k
            val rem = n % k
            val rowCounts = IntArray(k) { i -> base + if (i < rem) 1 else 0 }
            if (rowCounts.all { it >= 1 && it <= 15 }) {
                // Row-based
                val slots = mutableListOf<Slot>()
                var currentTop = 0f
                for (i in 0 until k) {
                    val rc = rowCounts[i]
                    val nextTop = currentTop + (1f / k)
                    val colWidth = 1f / rc
                    for (c in 0 until rc) {
                        slots.add(Slot(c * colWidth, currentTop, (c + 1) * colWidth, nextTop))
                    }
                    currentTop = nextTop
                }
                if (slots.size == n) results.add(normalizeSlotOrder(slots))

                // Column-based transpose
                val colSlots = mutableListOf<Slot>()
                val colWidth = 1f / k
                for (i in 0 until k) {
                    val rc = rowCounts[i]
                    val nextLeft = (i + 1) * colWidth
                    val left = i * colWidth
                    val rowHeight = 1f / rc
                    for (r in 0 until rc) {
                        colSlots.add(Slot(left, r * rowHeight, nextLeft, (r + 1) * rowHeight))
                    }
                }
                if (colSlots.size == n) results.add(normalizeSlotOrder(colSlots))
            }
        }
        return results
    }

    private fun generateHeroLayouts(n: Int): List<List<Slot>> {
        if (n < 2) return emptyList()
        val results = mutableListOf<List<Slot>>()
        val subCount = n - 1

        fun subSlotsRight(xStart: Float, xEnd: Float): List<Slot> {
            val s = mutableListOf<Slot>()
            val rh = 1f / subCount
            for (i in 0 until subCount) {
                s.add(Slot(xStart, i * rh, xEnd, (i + 1) * rh))
            }
            return s
        }

        // Left Hero
        val leftHero = mutableListOf(Slot(0f, 0f, 0.5f, 1f))
        leftHero.addAll(subSlotsRight(0.5f, 1f))
        if (leftHero.size == n) results.add(normalizeSlotOrder(leftHero))

        // Right Hero
        val rightHero = mutableListOf<Slot>()
        rightHero.addAll(subSlotsRight(0f, 0.5f))
        rightHero.add(Slot(0.5f, 0f, 1f, 1f))
        if (rightHero.size == n) results.add(normalizeSlotOrder(rightHero))

        fun subSlotsBottom(yStart: Float, yEnd: Float): List<Slot> {
            val s = mutableListOf<Slot>()
            val cw = 1f / subCount
            for (i in 0 until subCount) {
                s.add(Slot(i * cw, yStart, (i + 1) * cw, yEnd))
            }
            return s
        }

        // Top Hero
        val topHero = mutableListOf(Slot(0f, 0f, 1f, 0.5f))
        topHero.addAll(subSlotsBottom(0.5f, 1f))
        if (topHero.size == n) results.add(normalizeSlotOrder(topHero))

        // Bottom Hero
        val bottomHero = mutableListOf<Slot>()
        bottomHero.addAll(subSlotsBottom(0f, 0.5f))
        bottomHero.add(Slot(0f, 0.5f, 1f, 1f))
        if (bottomHero.size == n) results.add(normalizeSlotOrder(bottomHero))

        return results
    }

    private fun generateAlternatingRows(n: Int): List<List<Slot>> {
        val results = mutableListOf<List<Slot>>()
        val patterns = listOf(
            listOf(3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2),
            listOf(2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3),
            listOf(4, 2, 4, 2, 4, 2, 4, 2, 4, 2),
            listOf(3, 4, 3, 4, 3, 4, 3, 4),
            listOf(2, 4, 2, 4, 2, 4, 2, 4, 2, 4)
        )

        for (pat in patterns) {
            var sum = 0
            val usedPat = mutableListOf<Int>()
            for (cnt in pat) {
                if (sum + cnt <= n) {
                    usedPat.add(cnt)
                    sum += cnt
                } else {
                    break
                }
            }
            if (sum == n && usedPat.size >= 2) {
                val slots = mutableListOf<Slot>()
                val k = usedPat.size
                var currentTop = 0f
                for (i in 0 until k) {
                    val rc = usedPat[i]
                    val nextTop = currentTop + (1f / k)
                    val colWidth = 1f / rc
                    for (c in 0 until rc) {
                        slots.add(Slot(c * colWidth, currentTop, (c + 1) * colWidth, nextTop))
                    }
                    currentTop = nextTop
                }
                if (slots.size == n) results.add(normalizeSlotOrder(slots))
            }
        }
        return results
    }

    private fun generateBrickLayouts(n: Int): List<List<Slot>> {
        val results = mutableListOf<List<Slot>>()
        for (c in 2..minOf(n, 6)) {
            val base = n / c
            val rem = n % c
            val colCounts = IntArray(c) { i -> base + if (i < rem) 1 else 0 }
            if (colCounts.all { it >= 1 && it <= 10 }) {
                val slots = mutableListOf<Slot>()
                val colWidth = 1f / c
                for (i in 0 until c) {
                    val left = i * colWidth
                    val right = (i + 1) * colWidth
                    val cc = colCounts[i]
                    val rowHeight = 1f / cc
                    for (r in 0 until cc) {
                        slots.add(Slot(left, r * rowHeight, right, (r + 1) * rowHeight))
                    }
                }
                if (slots.size == n) results.add(normalizeSlotOrder(slots))
            }
        }
        return results
    }

    private fun generateMosaicLayouts(n: Int): List<List<Slot>> {
        val results = mutableListOf<List<Slot>>()
        val ratios = listOf(0.5f, 0.4f)
        for (ratio in ratios) {
            val slots = mutableListOf(Slot(0f, 0f, 1f, 1f))
            var horizontal = true
            while (slots.size < n) {
                var largestIdx = 0
                var maxArea = -1f
                for (i in slots.indices) {
                    val area = (slots[i].right - slots[i].left) * (slots[i].bottom - slots[i].top)
                    if (area > maxArea) {
                        maxArea = area
                        largestIdx = i
                    }
                }
                val target = slots.removeAt(largestIdx)
                val width = target.right - target.left
                val height = target.bottom - target.top
                if (horizontal) {
                    val splitX = target.left + width * ratio
                    slots.add(Slot(target.left, target.top, splitX, target.bottom))
                    slots.add(Slot(splitX, target.top, target.right, target.bottom))
                } else {
                    val splitY = target.top + height * ratio
                    slots.add(Slot(target.left, target.top, target.right, splitY))
                    slots.add(Slot(target.left, splitY, target.right, target.bottom))
                }
                horizontal = !horizontal
            }
            if (slots.size == n) {
                results.add(normalizeSlotOrder(slots))
            }
        }
        return results
    }

    fun getLayouts(count: Int): List<CollageLayout> {
        val clampedCount = count.coerceIn(1, 50)
        return cache.getOrPut(clampedCount) {
            val candidateLayouts = mutableListOf<CollageLayout>()
            if (clampedCount <= 9) {
                candidateLayouts.addAll(allLayouts.filter { it.slotCount == clampedCount })
            } else {
                candidateLayouts.addAll(generateEqualGrids(clampedCount).mapIndexed { i, slots ->
                    CollageLayout("l${clampedCount}_grid_${i+1}", "$clampedCount Photos (Equal Grid ${i+1})", clampedCount, "Grid layout", slots)
                })
                candidateLayouts.addAll(generateRowBasedLayouts(clampedCount).mapIndexed { i, slots ->
                    CollageLayout("l${clampedCount}_row_${i+1}", "$clampedCount Photos (Row Split ${i+1})", clampedCount, "Row split layout", slots)
                })
                candidateLayouts.addAll(generateHeroLayouts(clampedCount).mapIndexed { i, slots ->
                    CollageLayout("l${clampedCount}_hero_${i+1}", "$clampedCount Photos (Hero ${i+1})", clampedCount, "Hero layout", slots)
                })
                candidateLayouts.addAll(generateAlternatingRows(clampedCount).mapIndexed { i, slots ->
                    CollageLayout("l${clampedCount}_alt_${i+1}", "$clampedCount Photos (Alternating ${i+1})", clampedCount, "Alternating row layout", slots)
                })
                candidateLayouts.addAll(generateBrickLayouts(clampedCount).mapIndexed { i, slots ->
                    CollageLayout("l${clampedCount}_brick_${i+1}", "$clampedCount Photos (Masonry ${i+1})", clampedCount, "Masonry brick layout", slots)
                })
                candidateLayouts.addAll(generateMosaicLayouts(clampedCount).mapIndexed { i, slots ->
                    CollageLayout("l${clampedCount}_mosaic_${i+1}", "$clampedCount Photos (Mosaic ${i+1})", clampedCount, "Mosaic layout", slots)
                })
            }

            val uniqueLayouts = mutableListOf<CollageLayout>()
            val seenSignatures = mutableSetOf<String>()
            val seenIds = mutableSetOf<String>()

            for (layout in candidateLayouts) {
                val sig = layout.slots.joinToString(";") { "${it.left},${it.top},${it.right},${it.bottom}" }
                if (seenSignatures.add(sig)) {
                    var uniqueId = layout.id
                    while (!seenIds.add(uniqueId)) {
                        uniqueId = "${layout.id}_${seenIds.size}"
                    }
                    uniqueLayouts.add(layout.copy(id = uniqueId))
                }
            }

            if (clampedCount >= 10 && uniqueLayouts.size < 5) {
                var gridCols = 2
                while (uniqueLayouts.size < 5 && gridCols < clampedCount) {
                    val fallbackGrid = normalizeSlotOrder(grid(ceil(clampedCount.toDouble() / gridCols).toInt(), gridCols).take(clampedCount))
                    val sig = fallbackGrid.joinToString(";") { "${it.left},${it.top},${it.right},${it.bottom}" }
                    if (seenSignatures.add(sig) && fallbackGrid.size == clampedCount) {
                        var uniqueId = "l${clampedCount}_fallback_$gridCols"
                        while (!seenIds.add(uniqueId)) {
                            uniqueId = "${uniqueId}_dup"
                        }
                        uniqueLayouts.add(
                            CollageLayout(
                                id = uniqueId,
                                name = "$clampedCount Photos (Grid Fallback $gridCols)",
                                slotCount = clampedCount,
                                description = "Fallback grid layout",
                                slots = fallbackGrid
                            )
                        )
                    }
                    gridCols++
                }
            }

            if (clampedCount >= 10) {
                uniqueLayouts.take(8)
            } else {
                uniqueLayouts
            }
        }
    }

    fun getAllLayouts(): List<CollageLayout> {
        val list = mutableListOf<CollageLayout>()
        for (n in 1..50) {
            list.addAll(getLayouts(n))
        }
        return list
    }
}
