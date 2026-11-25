package com.oem.collagekmpapp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// =====================================================================================
//  Snapseed-style editing tools: platform-neutral state (commonMain).
//  Everything is an immutable value so it can live inside SpSnap (Undo / Redo) and travel to the exporter
//  inside SinglePhotoEdit.
//
//  Coordinate convention: points / centres are normalised (0..1) over the *cropped photo box*, i.e. in the
//  photo's own axes BEFORE rotation / flip / scale. Radii are fractions of the photo-box width.
// =====================================================================================

// ------------------------------------------------------------------ 2. selective edit

/** One Snapseed-style control point. Adjustment values use the same -50..50 scale as the Adjust tab. */
data class SelectivePoint(
    val id: String,
    val x: Float,
    val y: Float,
    val radius: Float = 0.18f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val structure: Float = 0f
) {
    val isNeutral: Boolean
        get() = abs(brightness) < 0.5f && abs(contrast) < 0.5f && abs(saturation) < 0.5f && abs(structure) < 0.5f
}

const val SELECTIVE_MAX_POINTS = 8
const val SELECTIVE_MIN_RADIUS = 0.05f
const val SELECTIVE_MAX_RADIUS = 0.6f

// ------------------------------------------------------------------ 3. vintage

enum class VintageTint(val label: String, val swatch: Long, val r: Float, val g: Float, val b: Float) {
    YELLOW("Yellow", 0xFFF5C542, 1.12f, 1.04f, 0.78f),
    GREEN("Green", 0xFF7CB342, 0.90f, 1.08f, 0.88f),
    BLUE("Blue", 0xFF4F8FE8, 0.84f, 0.98f, 1.16f),
    RED("Red", 0xFFE5533D, 1.16f, 0.92f, 0.86f)
}

data class VintageState(
    val on: Boolean = false,
    val tint: VintageTint = VintageTint.YELLOW,
    val brightness: Float = 0f,       // -50..50
    val saturation: Float = 0f,       // -50..50
    val strength: Float = 60f,        // 0..100  style strength (tint + fade + grain)
    val vignette: Float = 40f         // 0..100
) {
    val active: Boolean get() = on
}

// ------------------------------------------------------------------ 4. lens blur

enum class LensBlurMode(val label: String) { CIRCULAR("Circular"), HORIZONTAL("Horizontal") }

/**
 * Inner clear (sharp) zone + transition ring + outer blur.
 * [clear] and [feather] are fractions of the photo-box width; the transition ring runs from `clear` to `clear + feather`.
 * [amount] 0..100 = outer blur intensity.
 */
data class LensBlurState(
    val on: Boolean = false,
    val mode: LensBlurMode = LensBlurMode.CIRCULAR,
    val cx: Float = 0.5f,
    val cy: Float = 0.5f,
    val clear: Float = 0.22f,
    val feather: Float = 0.18f,
    val amount: Float = 60f
) {
    /** Horizontal mode = ellipse (same area as the circle, wider than tall). */
    val axisX: Float get() = if (mode == LensBlurMode.HORIZONTAL) LENS_ELLIPSE else 1f
    val axisY: Float get() = if (mode == LensBlurMode.HORIZONTAL) 1f / LENS_ELLIPSE else 1f
    val active: Boolean get() = on && amount > 0.5f
}

const val LENS_ELLIPSE = 1.35f

// ------------------------------------------------------------------ 5. glamour glow

data class GlamourPreset(val name: String, val glow: Float, val saturation: Float, val warmth: Float)

object GlamourPresets {
    val all = listOf(
        GlamourPreset("Soft", 40f, 0f, 0f),
        GlamourPreset("Dreamy", 70f, -5f, 6f),
        GlamourPreset("Golden", 55f, 8f, 26f),
        GlamourPreset("Rose", 50f, 12f, 12f),
        GlamourPreset("Pearl", 60f, -12f, -8f),
        GlamourPreset("Radiant", 85f, 15f, 10f)
    )
}

data class GlamourState(
    val on: Boolean = false,
    val glow: Float = 40f,            // 0..100
    val saturation: Float = 0f,       // -50..50
    val warmth: Float = 0f            // -50..50
)

// ------------------------------------------------------------------ 6. perspective

enum class PerspectiveMode(val label: String) { TILT("Tilt"), ROTATE("Rotate"), SCALE("Scale"), FREE("Free") }
enum class PerspectiveFill(val label: String) { SMART("Smart"), WHITE("White"), BLACK("Black") }

/**
 * All four modes act on the same transform at once (so switching modes never loses an adjustment):
 * canvas rectangle -> tilt -> rotate -> scale -> free corner offsets = destination quad.
 * [corners] = x,y offsets (fractions of canvas width / height) for TL, TR, BR, BL.
 */
data class PerspectiveState(
    val mode: PerspectiveMode = PerspectiveMode.TILT,
    val fill: PerspectiveFill = PerspectiveFill.SMART,
    val tiltV: Float = 0f,            // -50..50
    val tiltH: Float = 0f,            // -50..50
    val rotate: Float = 0f,           // -45..45 degrees
    val scale: Float = 100f,          // 50..150 percent
    val corners: List<Float> = List(8) { 0f }
) {
    val isIdentity: Boolean
        get() = abs(tiltV) < 0.01f && abs(tiltH) < 0.01f && abs(rotate) < 0.01f && abs(scale - 100f) < 0.01f &&
            corners.all { abs(it) < 0.0005f }

    /** Destination quad in canvas pixels: TLx, TLy, TRx, TRy, BRx, BRy, BLx, BLy. */
    fun quad(w: Float, h: Float, withFree: Boolean = true): FloatArray {
        val p = floatArrayOf(0f, 0f, w, 0f, w, h, 0f, h)
        // tilt (vertical: top / bottom edge narrows, horizontal: left / right edge shortens)
        val kv = tiltV / 50f * 0.30f
        val kh = tiltH / 50f * 0.30f
        if (kv != 0f) {
            val a = abs(kv) * w / 2f
            if (kv > 0f) { p[0] += a; p[2] -= a } else { p[6] += a; p[4] -= a }
        }
        if (kh != 0f) {
            val a = abs(kh) * h / 2f
            if (kh > 0f) { p[1] += a; p[7] -= a } else { p[3] += a; p[5] -= a }
        }
        // rotate + scale about the canvas centre
        val cx = w / 2f
        val cy = h / 2f
        val rad = (rotate * PI / 180.0)
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        val k = scale / 100f
        for (i in 0 until 4) {
            val dx = p[i * 2] - cx
            val dy = p[i * 2 + 1] - cy
            p[i * 2] = cx + (dx * c - dy * s) * k
            p[i * 2 + 1] = cy + (dx * s + dy * c) * k
        }
        if (withFree) for (i in 0 until 4) {
            p[i * 2] += corners[i * 2] * w
            p[i * 2 + 1] += corners[i * 2 + 1] * h
        }
        return p
    }
}

/**
 * 4x4 column-major matrix (androidx.compose.ui.graphics.Matrix layout) of the projective map
 * canvas rectangle (w x h) -> [quad]. Returns null for a degenerate quad.
 * The perspective terms sit at indices 3, 7 and 15, which Compose forwards to the platform canvas.
 */
fun perspectiveMatrix(quad: FloatArray, w: Float, h: Float): FloatArray? {
    val x0 = quad[0]; val y0 = quad[1]
    val x1 = quad[2]; val y1 = quad[3]
    val x2 = quad[4]; val y2 = quad[5]
    val x3 = quad[6]; val y3 = quad[7]
    val dx1 = x1 - x2; val dx2 = x3 - x2; val dx3 = x0 - x1 + x2 - x3
    val dy1 = y1 - y2; val dy2 = y3 - y2; val dy3 = y0 - y1 + y2 - y3
    val det = dx1 * dy2 - dx2 * dy1
    if (abs(det) < 1e-6f) return null
    val g: Float
    val hh: Float
    if (abs(dx3) < 1e-6f && abs(dy3) < 1e-6f) { g = 0f; hh = 0f }
    else {
        g = (dx3 * dy2 - dx2 * dy3) / det
        hh = (dx1 * dy3 - dx3 * dy1) / det
    }
    val a = x1 - x0 + g * x1
    val b = x3 - x0 + hh * x3
    val d = y1 - y0 + g * y1
    val e = y3 - y0 + hh * y3
    val m = FloatArray(16)
    m[0] = a / w;  m[4] = b / h;  m[12] = x0       // x' numerator
    m[1] = d / w;  m[5] = e / h;  m[13] = y0       // y' numerator
    m[3] = g / w;  m[7] = hh / h; m[15] = 1f       // projective denominator
    m[10] = 1f
    return m
}

// ------------------------------------------------------------------ everything together

/** Complete state of the six Snapseed-style tools (one field of SpSnap / SinglePhotoEdit). */
data class PhotoToolsState(
    val selective: List<SelectivePoint> = emptyList(),
    val vintage: VintageState = VintageState(),
    val lens: LensBlurState = LensBlurState(),
    val glamour: GlamourState = GlamourState(),
    val perspective: PerspectiveState = PerspectiveState()
)