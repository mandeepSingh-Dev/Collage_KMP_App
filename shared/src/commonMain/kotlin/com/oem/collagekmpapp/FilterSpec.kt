package com.oem.collagekmpapp

/**
 * Pure-Kotlin photo look engine (put in commonMain).
 *
 * Produces a 4x5 color matrix (20 floats, row-major, translate column in 0..255) that is
 * applied to the PIXELS of each photo:
 *   - Compose preview : ColorFilter.colorMatrix(ColorMatrix(array))
 *   - Android export  : Paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(array))
 * Both use identical math, so preview == exported image.
 */
data class FilterSpec(
    val brightness: Float = 0f,   // -1..1
    val contrast: Float = 0f,     // -1..1
    val saturation: Float = 0f,   // -1 (grayscale) .. 1 (double)
    val warmth: Float = 0f,       // -1 cool .. 1 warm
    val tint: Float = 0f,         // -1 green .. 1 magenta
    val fade: Float = 0f,         // 0..1 lifts blacks (matte look)
    val rGain: Float = 1f,        // per-channel gain (colour grading)
    val gGain: Float = 1f,
    val bGain: Float = 1f,
    val rShift: Float = 0f,       // per-channel additive shift, fraction of full range
    val gShift: Float = 0f,
    val bShift: Float = 0f,
    val vignette: Float = 0f,     // 0..1 edge darkening
    val glow: Float = 0f          // 0..1 soft bloom (the 'glowing skin' look)
)

object PhotoFilters {
    val specs: Map<String, FilterSpec> = linkedMapOf(
        "Original" to FilterSpec(),
        "Natural" to FilterSpec(),

        // ---- existing filters, now real colour grades ----
        "Clarendon" to FilterSpec(brightness = .05f, contrast = .18f, saturation = .25f, bGain = 1.06f, rGain = .98f),
        "Gingham" to FilterSpec(brightness = .05f, contrast = -.1f, saturation = -.15f, warmth = .05f, fade = .3f),
        "Moon" to FilterSpec(brightness = .05f, contrast = .15f, saturation = -1f, bGain = 1.03f),
        "Lark" to FilterSpec(brightness = .10f, contrast = .05f, saturation = -.1f, warmth = -.03f, gGain = 1.03f, glow = 0.25f),
        "Reyes" to FilterSpec(brightness = .08f, contrast = -.08f, saturation = -.2f, warmth = .12f, fade = .35f),
        "Juno" to FilterSpec(contrast = .12f, saturation = .3f, warmth = .08f, tint = -.04f, rGain = 1.04f),
        "Slumber" to FilterSpec(brightness = -.03f, saturation = -.3f, warmth = .08f, fade = .3f),
        "Crema" to FilterSpec(brightness = .04f, saturation = -.15f, warmth = .10f, fade = .2f),
        "Ludwig" to FilterSpec(brightness = .03f, contrast = .08f, saturation = .1f, warmth = .04f),
        "Aden" to FilterSpec(brightness = .08f, saturation = -.2f, rGain = 1.03f, bGain = 1.02f, fade = .25f, tint = .05f, glow = 0.3f),
        "Perpetua" to FilterSpec(contrast = .06f, saturation = .1f, tint = -.06f, gGain = 1.04f, bGain = 1.02f),
        "Amaro" to FilterSpec(brightness = .10f, contrast = .10f, saturation = .2f, warmth = .08f, vignette = .15f, glow = 0.25f),
        "Mayfair" to FilterSpec(brightness = .04f, contrast = .10f, saturation = .1f, warmth = .06f, tint = .05f, vignette = .2f),
        "Rise" to FilterSpec(brightness = .10f, contrast = -.05f, saturation = -.05f, warmth = .10f, fade = .25f, glow = 0.3f),
        "Hudson" to FilterSpec(brightness = -.02f, contrast = .12f, saturation = .05f, warmth = -.12f, bGain = 1.08f, vignette = .15f),
        "Valencia" to FilterSpec(brightness = .05f, contrast = .05f, saturation = .1f, warmth = .12f, fade = .25f),
        "X-Pro II" to FilterSpec(contrast = .30f, saturation = .25f, warmth = .05f, bGain = .96f, vignette = .45f),
        "Sierra" to FilterSpec(contrast = -.05f, saturation = -.12f, warmth = .05f, fade = .45f, vignette = .15f),
        "Willow" to FilterSpec(brightness = .04f, contrast = .05f, saturation = -.85f, warmth = .02f, fade = .25f),
        "Lo-fi" to FilterSpec(brightness = -.03f, contrast = .30f, saturation = .35f, vignette = .35f),
        "Vivid" to FilterSpec(brightness = .02f, contrast = .15f, saturation = .55f),
        "Golden" to FilterSpec(brightness = .06f, contrast = .05f, saturation = .15f, warmth = .30f, rGain = 1.02f, bGain = .92f, glow = 0.4f),
        "Retro" to FilterSpec(contrast = .10f, saturation = -.2f, warmth = .15f, fade = .5f, vignette = .3f),
        "Soft" to FilterSpec(brightness = .08f, contrast = -.15f, saturation = -.08f, fade = .25f, glow = 0.5f),
        "Moody" to FilterSpec(brightness = -.12f, contrast = .20f, saturation = -.15f, warmth = -.10f, vignette = .35f),
        "Warm" to FilterSpec(brightness = .03f, warmth = .25f),
        "Vintage" to FilterSpec(contrast = .08f, saturation = -.3f, warmth = .20f, fade = .55f, bGain = .92f, vignette = .3f),
        "B&W" to FilterSpec(contrast = .10f, saturation = -1f),
        "Neon" to FilterSpec(contrast = .25f, saturation = .7f, rGain = 1.05f, gGain = .95f, bGain = 1.08f, tint = .1f, vignette = .2f),

        // ---- NEW filters ----
        "Noir" to FilterSpec(brightness = -.08f, contrast = .45f, saturation = -1f, vignette = .4f),
        "Silver" to FilterSpec(brightness = .06f, contrast = .10f, saturation = -1f, fade = .2f, bGain = 1.02f),
        "Sepia" to FilterSpec(contrast = .05f, saturation = -1f, rGain = 1.12f, bGain = .78f, fade = .12f),
        "Cinematic" to FilterSpec(contrast = .18f, saturation = -.05f, warmth = .05f, rShift = -.02f, bShift = .03f, vignette = .3f),
        "Teal & Orange" to FilterSpec(contrast = .20f, saturation = .2f, rGain = 1.08f, gGain = .98f, rShift = -.03f, bShift = .04f),
        "Film" to FilterSpec(contrast = .10f, saturation = -.1f, warmth = .06f, fade = .3f, vignette = .15f),
        "Kodak" to FilterSpec(contrast = .12f, saturation = .2f, warmth = .18f, rGain = 1.04f, fade = .12f),
        "Fuji" to FilterSpec(contrast = .10f, saturation = .15f, tint = -.05f, gGain = 1.02f, bGain = 1.04f, fade = .15f),
        "Polaroid" to FilterSpec(brightness = .05f, contrast = -.05f, saturation = -.05f, warmth = .08f, rShift = .02f, fade = .4f, vignette = .25f),
        "Faded" to FilterSpec(contrast = -.10f, saturation = -.25f, fade = .75f),
        "Matte" to FilterSpec(brightness = .02f, contrast = -.10f, saturation = -.1f, fade = .6f),
        "Dreamy" to FilterSpec(brightness = .12f, contrast = -.2f, saturation = -.1f, warmth = .05f, tint = .04f, fade = .25f, glow = 0.8f),
        "Cool" to FilterSpec(warmth = -.25f, bGain = 1.03f),
        "Sunset" to FilterSpec(contrast = .10f, saturation = .2f, warmth = .35f, tint = .10f, rGain = 1.05f),
        "Fresh" to FilterSpec(brightness = .10f, contrast = .05f, saturation = .2f, warmth = -.04f, tint = -.03f, glow = 0.3f),
        "Pastel" to FilterSpec(brightness = .12f, contrast = -.10f, saturation = -.25f, tint = .04f, fade = .5f, glow = 0.3f),
        "Cyber" to FilterSpec(contrast = .25f, saturation = .4f, rGain = 1.05f, gGain = .9f, bGain = 1.15f, vignette = .25f),
        "Sunkissed" to FilterSpec(brightness = .08f, saturation = .1f, warmth = .22f, fade = .15f, glow = 0.4f),
        "HDR" to FilterSpec(contrast = .35f, saturation = .3f),
        "Portrait" to FilterSpec(brightness = .08f, contrast = -.03f, saturation = -.05f, warmth = .06f, fade = .08f, glow = 0.6f),
        "Summer" to FilterSpec(brightness = .07f, contrast = .10f, saturation = .3f, warmth = .12f),
        "Autumn" to FilterSpec(contrast = .10f, saturation = .15f, warmth = .25f, tint = .04f, gGain = .97f, bGain = .9f),
        "Winter" to FilterSpec(brightness = .06f, contrast = .08f, saturation = -.1f, warmth = -.22f, bGain = 1.04f),
        "Dramatic" to FilterSpec(brightness = -.08f, contrast = .40f, saturation = -.2f, vignette = .4f),
        "Chrome" to FilterSpec(brightness = .02f, contrast = .25f, saturation = -.25f, bGain = 1.03f),
        "Tokyo" to FilterSpec(brightness = -.03f, contrast = .15f, saturation = .3f, tint = .08f, rGain = 1.04f, bGain = 1.10f),
        "Rosy" to FilterSpec(brightness = .06f, saturation = .05f, warmth = .08f, tint = .10f, fade = .2f, glow = 0.4f),
        "Emerald" to FilterSpec(contrast = .10f, saturation = .15f, tint = -.12f, gGain = 1.05f),
        "Lomo" to FilterSpec(contrast = .35f, saturation = .4f, warmth = .05f, vignette = .55f),
        "Bleach" to FilterSpec(brightness = .05f, contrast = .30f, saturation = -.5f),
        "Haze" to FilterSpec(brightness = .10f, contrast = -.2f, saturation = -.1f, bGain = 1.02f, fade = .55f, glow = 0.7f),
        // ---- GLOW filters: soft bloom + lifted skin tones ----
        "Glow" to FilterSpec(brightness = .08f, contrast = -.04f, saturation = .12f, warmth = .06f, fade = .08f, glow = .9f),
        "Beauty" to FilterSpec(brightness = .10f, contrast = -.05f, saturation = .05f, warmth = .05f, tint = .03f, fade = .12f, glow = .7f),
        "Radiant" to FilterSpec(brightness = .08f, contrast = .05f, saturation = .15f, warmth = .10f, glow = .8f),
        "Porcelain" to FilterSpec(brightness = .12f, contrast = -.05f, saturation = -.1f, warmth = -.04f, fade = .12f, glow = .6f),
        "Golden Hour" to FilterSpec(brightness = .06f, contrast = .08f, saturation = .2f, warmth = .35f, fade = .1f, vignette = .2f, glow = .6f),
        "Angel" to FilterSpec(brightness = .15f, contrast = -.15f, saturation = -.05f, fade = .2f, glow = 1f),
    )

    /** Use this to build the filter strip in your UI so UI + engine never drift apart. */
    val names: List<String> get() = specs.keys.toList()

    /** Instagram-style category tabs. "Original" is always first. */
    val categories: Map<String, List<String>> by lazy {
        fun cat(vararg n: String) = listOf("Original") + n.filter { specs.containsKey(it) }
        linkedMapOf(
            "All" to names,
            "Popular" to cat("Clarendon", "Juno", "Lark", "Valencia", "Aden", "Amaro", "Mayfair", "Glow", "Portrait", "Teal & Orange", "Vivid", "HDR"),
            "Glow" to cat("Glow", "Beauty", "Radiant", "Porcelain", "Angel", "Golden Hour", "Portrait", "Dreamy", "Soft", "Rosy", "Sunkissed", "Haze"),
            "B&W" to cat("Moon", "B&W", "Noir", "Silver", "Willow", "Bleach", "Chrome", "Dramatic"),
            "Warm" to cat("Warm", "Golden", "Sunset", "Sunkissed", "Autumn", "Kodak", "Summer", "Crema", "Rise", "Reyes", "Valencia", "Golden Hour"),
            "Cool" to cat("Cool", "Winter", "Hudson", "Moody", "Cyber", "Tokyo", "Fuji", "Juno", "Emerald", "Perpetua"),
            "Vintage" to cat("Vintage", "Retro", "Film", "Polaroid", "Sepia", "Faded", "Matte", "Sierra", "Gingham", "Lo-fi", "Lomo", "Slumber"),
            "Cinematic" to cat("Cinematic", "Teal & Orange", "Moody", "Dramatic", "Noir", "X-Pro II", "Tokyo", "Cyber")
        )
    }
}

object PhotoColorEngine {

    /** Adjust sliders are assumed to span -50..50 (same as your current `/ 50f`). Change if yours is ±100. */
    private const val ADJUST_RANGE = 50f

    /** Global multiplier on every filter preset. Raise for punchier Instagram-like looks, lower for subtle. */
    private const val LOOK_STRENGTH = 1.4f

    private val IDENTITY = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** Returns null when nothing needs to be applied (keeps the fast path free). */
    fun buildMatrix(preset: String, intensity: Float, adjustValues: Map<String, Float>): FloatArray? {
        val t = (intensity / 100f).coerceIn(0f, 1f)
        var m = IDENTITY
        val spec = PhotoFilters.specs[preset]
        if (spec != null && t > 0f) m = lerp(IDENTITY, specToMatrix(spec), t)
        val adj = adjustMatrix(adjustValues)
        if (adj != null) m = concat(adj, m)          // filter first, then user adjustments on top
        return if (m === IDENTITY) null else m
    }

    fun vignetteStrength(preset: String, intensity: Float): Float {
        val t = (intensity / 100f).coerceIn(0f, 1f)
        return ((PhotoFilters.specs[preset]?.vignette ?: 0f) * t).coerceIn(0f, 0.7f)
    }

    /** 0..1 strength of the soft-bloom glow layer for this filter. */
    fun glowStrength(preset: String, intensity: Float): Float {
        val t = (intensity / 100f).coerceIn(0f, 1f)
        return ((PhotoFilters.specs[preset]?.glow ?: 0f) * t).coerceIn(0f, 1f)
    }

    // ---------------------------------------------------------------- filter preset

    private fun specToMatrix(s: FilterSpec): FloatArray {
        val k = LOOK_STRENGTH
        fun amp(gain: Float) = 1f + (gain - 1f) * k
        val warmth = s.warmth * k
        val tint = s.tint * k
        var m = IDENTITY
        m = concat(saturationM(1f + (s.saturation * k).coerceAtLeast(-1f)), m)
        m = concat(
            diag(
                (1f + .18f * warmth) * (1f + .04f * tint) * amp(s.rGain),
                (1f + .03f * warmth) * (1f - .12f * tint) * amp(s.gGain),
                (1f - .18f * warmth) * (1f + .04f * tint) * amp(s.bGain),
                s.rShift * k * 255f, s.gShift * k * 255f, s.bShift * k * 255f
            ), m
        )
        m = concat(contrastM((s.contrast * k).coerceIn(-1f, 1f)), m)
        m = concat(brightnessM(s.brightness * k), m)
        m = concat(fadeM((s.fade * 0.4f * k).coerceAtMost(0.3f)), m)
        return m
    }

    // ---------------------------------------------------------------- adjust tab

    private fun adjustMatrix(v: Map<String, Float>): FloatArray? {
        fun a(name: String) = ((v[name] ?: 0f) / ADJUST_RANGE).coerceIn(-1f, 1f)
        val brightness = a("Brightness")
        val contrast = a("Contrast")
        val highlight = a("Highlight")
        val shadow = a("Shadow")
        val saturation = a("Saturation")
        val warmth = a("Warmth")
        val tint = a("Tint")
        val fade = a("Fade")
        val all = floatArrayOf(brightness, contrast, highlight, shadow, saturation, warmth, tint, fade)
        if (all.all { kotlin.math.abs(it) < 0.001f }) return null

        var m = IDENTITY
        m = concat(saturationM(1f + saturation), m)
        m = concat(diag(1f + .18f * warmth, 1f + .03f * warmth, 1f - .18f * warmth, 0f, 0f, 0f), m)
        m = concat(diag(1f + .04f * tint, 1f - .12f * tint, 1f + .04f * tint, 0f, 0f, 0f), m)
        // Shadow: pivot at white -> mostly moves dark pixels
        val sg = 1f - .15f * shadow
        m = concat(allChannels(sg, 255f * (1f - sg)), m)
        // Highlight: pivot at black -> mostly moves bright pixels
        m = concat(allChannels(1f + .20f * highlight, 0f), m)
        m = concat(contrastM(contrast), m)
        m = concat(brightnessM(brightness), m)
        m = concat(fadeM(fade * 0.2f), m)
        return m
    }

    // ---------------------------------------------------------------- building blocks

    private fun allChannels(gain: Float, offset: Float) = diag(gain, gain, gain, offset, offset, offset)

    private fun brightnessM(b: Float) = allChannels(1f + .30f * b, 30f * b)

    private fun contrastM(c: Float): FloatArray {
        val f = 1f + .70f * c
        return allChannels(f, 127.5f * (1f - f))
    }

    /** lift = fraction of full range that pure black is raised to (whites stay white). */
    private fun fadeM(lift: Float): FloatArray {
        val g = 1f - lift
        return allChannels(g, 255f * lift)
    }

    private fun saturationM(s: Float): FloatArray {
        val sat = s.coerceAtLeast(0f)
        val inv = 1f - sat
        val r = .213f * inv
        val g = .715f * inv
        val b = .072f * inv
        return floatArrayOf(
            r + sat, g, b, 0f, 0f,
            r, g + sat, b, 0f, 0f,
            r, g, b + sat, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    private fun diag(rg: Float, gg: Float, bg: Float, ro: Float, go: Float, bo: Float) = floatArrayOf(
        rg, 0f, 0f, 0f, ro,
        0f, gg, 0f, 0f, go,
        0f, 0f, bg, 0f, bo,
        0f, 0f, 0f, 1f, 0f
    )

    /** Returns a matrix equal to applying [first], then [second]. */
    private fun concat(second: FloatArray, first: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (i in 0 until 4) {
            for (j in 0 until 5) {
                var sum = 0f
                for (k in 0 until 4) sum += second[i * 5 + k] * first[k * 5 + j]
                if (j == 4) sum += second[i * 5 + 4]
                out[i * 5 + j] = sum
            }
        }
        return out
    }

    private fun lerp(a: FloatArray, b: FloatArray, t: Float) =
        FloatArray(20) { a[it] + (b[it] - a[it]) * t }
}