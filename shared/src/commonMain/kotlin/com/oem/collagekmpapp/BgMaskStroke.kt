package com.oem.collagekmpapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import kotlin.math.sqrt

/**
 * One edit on the transparency mask. Points are normalized (0..1) to the image width / height,
 * widthRatio is the brush diameter as a fraction of the image width.
 * restore = true paints the photo back, false erases it. fill = true applies to the whole image.
 *
 * [rects] (optional) = a pixel REGION instead of a brush path, used by Magic Wand and AI Auto-Cutout:
 * normalized rectangles packed as left, top, right, bottom, left, top, ... (0..1 of the image).
 * Preview and exporter both fill all rects with the same erase / restore mode, so the region is part of the
 * stroke list like any brush stroke (Undo / Redo, export).
 */
data class BgMaskStroke(
    val points: List<Offset>,
    val widthRatio: Float,
    val restore: Boolean,
    val fill: Boolean = false,
    val rects: FloatArray? = null
)

/** Platform bridge: renders the original photo with the mask applied and saves a transparent PNG. */
fun interface BackgroundRemovalExporter {
    fun export(photoUri: String, strokes: List<BgMaskStroke>, onResult: (Result<String>) -> Unit)
}

@Composable
expect fun rememberBackgroundRemovalExporter(): BackgroundRemovalExporter

// =====================================================================================
//  Pixel access for Magic Wand / Auto-Cutout
// =====================================================================================

/** A downscaled, upright (EXIF-corrected) copy of the photo: ARGB ints, row-major. */
class MaskPixels(val width: Int, val height: Int, val argb: IntArray)

/** Platform bridge: decodes [uri] upright, scaled so the longest side is at most [maxSide] px. null = unreadable. */
interface PhotoPixelLoader {
    suspend fun load(uri: String, maxSide: Int): MaskPixels?
}

@Composable
expect fun rememberPhotoPixelLoader(): PhotoPixelLoader

/** Longest side of the analysis copy. Regions are found at this resolution and scaled up for preview / export. */
const val MASK_ANALYSIS_SIDE = 768

// =====================================================================================
//  Region algorithms (pure Kotlin, run off the main thread)
// =====================================================================================

/** RGB distance 0..1 (0 = identical, 1 = black vs white). */
private fun colorDist(a: Int, b: Int): Float {
    val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
    val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
    val db = (a and 0xFF) - (b and 0xFF)
    return sqrt((dr * dr + dg * dg + db * db).toFloat()) / 441.673f
}

/**
 * Magic wand: flood-fills the 4-connected area around the tapped point ([nx], [ny], normalized) whose colour is
 * within [tolerance] (0..1) of the tapped pixel. Returns the region as normalized rects, or null if nothing matched.
 */
fun magicWandRects(px: MaskPixels, nx: Float, ny: Float, tolerance: Float): FloatArray? {
    val w = px.width
    val h = px.height
    if (w <= 0 || h <= 0) return null
    val sx = (nx * w).toInt().coerceIn(0, w - 1)
    val sy = (ny * h).toInt().coerceIn(0, h - 1)
    val seed = px.argb[sy * w + sx]
    val mask = BooleanArray(w * h)
    val stack = IntArray(w * h)
    var sp = 0
    mask[sy * w + sx] = true
    stack[sp++] = sy * w + sx
    while (sp > 0) {
        val i = stack[--sp]
        val x = i % w
        val y = i / w
        fun visit(n: Int) {
            if (!mask[n] && colorDist(px.argb[n], seed) <= tolerance) {
                mask[n] = true
                stack[sp++] = n
            }
        }
        if (x > 0) visit(i - 1)
        if (x < w - 1) visit(i + 1)
        if (y > 0) visit(i - w)
        if (y < h - 1) visit(i + w)
    }
    return maskToRects(mask, w, h)
}

/**
 * 1-tap subject isolation (on-device heuristic, no ML model):
 *  1. learns the background colours from the photo's border (up to 12 dominant colour buckets),
 *  2. grows the background inward from the border through pixels that look like that background AND have no strong
 *     edge to the pixel they came from (so it stops at the subject's outline),
 *  3. keeps the main foreground blob(s) (largest component + any component >= 15 % of it), drops speckles,
 *  4. grows the subject by 1 px so its edge isn't nibbled.
 * Returns the BACKGROUND as normalized rects (to erase), or null when no clear subject was found.
 */
fun autoCutoutBackgroundRects(px: MaskPixels): FloatArray? {
    val w = px.width
    val h = px.height
    if (w < 8 || h < 8) return null
    val n = w * h
    val c = px.argb

    // ---- 1. border palette
    val counts = IntArray(4096)
    val sumR = LongArray(4096); val sumG = LongArray(4096); val sumB = LongArray(4096)
    var borderTotal = 0
    fun addBorder(i: Int) {
        val v = c[i]
        val r = (v shr 16) and 0xFF; val g = (v shr 8) and 0xFF; val b = v and 0xFF
        val k = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
        counts[k]++; sumR[k] += r.toLong(); sumG[k] += g.toLong(); sumB[k] += b.toLong()
        borderTotal++
    }
    for (x in 0 until w) { addBorder(x); addBorder((h - 1) * w + x) }
    for (y in 1 until h - 1) { addBorder(y * w); addBorder(y * w + w - 1) }
    val order = (0 until 4096).filter { counts[it] > 0 }.sortedByDescending { counts[it] }
    val palette = ArrayList<Int>()
    var covered = 0
    for (k in order) {
        if (palette.size >= 12 || covered >= borderTotal * 0.85f) break
        val cnt = counts[k]
        palette += (0xFF shl 24) or (((sumR[k] / cnt).toInt()) shl 16) or (((sumG[k] / cnt).toInt()) shl 8) or (sumB[k] / cnt).toInt()
        covered += cnt
    }
    fun paletteDist(v: Int): Float {
        var best = 1f
        for (p in palette) { val d = colorDist(v, p); if (d < best) best = d }
        return best
    }

    // ---- 2. grow the background from the border
    val BG_MATCH = 0.24f     // how close to a border colour a pixel must be
    val EDGE = 0.075f        // max step between neighbours (bigger = an edge the fill must not cross)
    val bg = BooleanArray(n)
    val stack = IntArray(n)
    var sp = 0
    fun seed(i: Int) { if (!bg[i] && paletteDist(c[i]) <= BG_MATCH) { bg[i] = true; stack[sp++] = i } }
    for (x in 0 until w) { seed(x); seed((h - 1) * w + x) }
    for (y in 1 until h - 1) { seed(y * w); seed(y * w + w - 1) }
    while (sp > 0) {
        val i = stack[--sp]
        val x = i % w
        val y = i / w
        fun grow(j: Int) {
            if (!bg[j] && colorDist(c[j], c[i]) <= EDGE && paletteDist(c[j]) <= BG_MATCH) {
                bg[j] = true
                stack[sp++] = j
            }
        }
        if (x > 0) grow(i - 1)
        if (x < w - 1) grow(i + 1)
        if (y > 0) grow(i - w)
        if (y < h - 1) grow(i + w)
    }

    // ---- 3. keep the main foreground blob(s)
    val label = IntArray(n) { -1 }
    val sizes = ArrayList<Int>()
    for (start in 0 until n) {
        if (bg[start] || label[start] >= 0) continue
        val id = sizes.size
        var size = 0
        sp = 0
        stack[sp++] = start
        label[start] = id
        while (sp > 0) {
            val i = stack[--sp]
            size++
            val x = i % w
            val y = i / w
            fun join(j: Int) { if (!bg[j] && label[j] < 0) { label[j] = id; stack[sp++] = j } }
            if (x > 0) join(i - 1)
            if (x < w - 1) join(i + 1)
            if (y > 0) join(i - w)
            if (y < h - 1) join(i + w)
        }
        sizes += size
    }
    val largest = sizes.maxOrNull() ?: return null
    val keepMin = maxOf((largest * 0.15f).toInt(), (n * 0.01f).toInt())
    for (i in 0 until n) if (!bg[i] && sizes[label[i]] < keepMin) bg[i] = true

    // ---- 4. grow the subject by one pixel (background erodes by one)
    val fgCopy = BooleanArray(n) { !bg[it] }
    for (i in 0 until n) {
        if (!bg[i]) continue
        val x = i % w
        val y = i / w
        if ((x > 0 && fgCopy[i - 1]) || (x < w - 1 && fgCopy[i + 1]) || (y > 0 && fgCopy[i - w]) || (y < h - 1 && fgCopy[i + w])) bg[i] = false
    }

    // sanity: nothing found, or everything is background -> no clear subject
    val bgCount = bg.count { it }
    if (bgCount < n * 0.05f || bgCount > n * 0.95f) return null
    return maskToRects(bg, w, h)
}

/**
 * Converts a boolean mask into normalized rects: horizontal runs per row, merged vertically when a run continues
 * unchanged on the next row (keeps the rect count small). Returns null for an empty mask.
 */
fun maskToRects(mask: BooleanArray, w: Int, h: Int): FloatArray? {
    val out = ArrayList<Float>()
    var open = HashMap<Long, Int>()          // key = x0 * (w + 1) + x1  ->  top row
    val stride = (w + 1).toLong()
    for (y in 0..h) {
        val next = HashMap<Long, Int>()
        if (y < h) {
            var x = 0
            val row = y * w
            while (x < w) {
                if (mask[row + x]) {
                    val x0 = x
                    while (x < w && mask[row + x]) x++
                    val key = x0 * stride + x
                    next[key] = open.remove(key) ?: y
                } else x++
            }
        }
        for ((key, top) in open) {
            val x0 = (key / stride).toInt()
            val x1 = (key % stride).toInt()
            out += x0.toFloat() / w; out += top.toFloat() / h; out += x1.toFloat() / w; out += y.toFloat() / h
        }
        open = next
    }
    return if (out.isEmpty()) null else out.toFloatArray()
}