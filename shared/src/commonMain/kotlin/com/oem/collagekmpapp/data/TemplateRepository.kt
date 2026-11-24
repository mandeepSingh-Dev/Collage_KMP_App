package com.oem.collagekmpapp.data

import com.oem.collagekmpapp.model.CollageProject
import com.oem.collagekmpapp.model.PhotoTransform
import com.oem.collagekmpapp.model.ProjectOverlayItem
import com.oem.collagekmpapp.model.ProjectTextLayer
import com.oem.collagekmpapp.model.Slot

// commonMain – themed collage templates.

enum class TemplateCategory(val displayName: String, val emoji: String) {
    All("All", "✨"),
    Christmas("Christmas", "🎄"),
    NewYear("New Year", "🎆"),
    Birthday("Birthday", "🎂"),
    Love("Love", "💖"),
    Story("Story", "📱"),
    Polaroid("Polaroid", "📷")
}

/** How the small preview card draws the template's photo slots. */
enum class TemplateThumbStyle { CLASSIC, POLAROID, STORY, BANNER }

/**
 * A themed collage preset. [preset] is a normal [CollageProject] (aspect ratio, spacing, background
 * gradient, filter, text layers and emoji/sticker overlays – all normalised 0..1) WITHOUT photos/slots;
 * those are filled in by [buildProject] once the user has picked photos.
 */
data class TemplateItem(
    val id: String,
    val title: String,
    val category: TemplateCategory,
    val thumbStyle: TemplateThumbStyle,
    val preset: CollageProject,
    /** The photo count the design looks best with (used for the card badge / recommended picks). */
    val preferredPhotos: Int,
    /** Which of the LayoutRepository layouts for the picked photo count to use (wraps around). */
    val layoutIndex: Int = 0,
    val accentColorLong: Long = 0xFF2563EB
) {
    val aspectLabel: String
        get() = when {
            kotlin.math.abs(preset.aspectRatioWidth / preset.aspectRatioHeight - 1f) < 0.01f -> "1:1"
            preset.aspectRatioWidth < preset.aspectRatioHeight && preset.aspectRatioWidth == 9f -> "9:16"
            preset.aspectRatioWidth == 4f && preset.aspectRatioHeight == 5f -> "4:5"
            else -> "${preset.aspectRatioWidth.toInt()}:${preset.aspectRatioHeight.toInt()}"
        }

    /** Layout id for [photoCount] photos – always a layout that really has that many slots. */
    fun layoutIdFor(photoCount: Int, repo: LayoutRepository = LayoutRepository()): String {
        val options = repo.getLayouts(photoCount.coerceAtLeast(1))
        return options[layoutIndex % options.size].id
    }

    fun slotsFor(photoCount: Int, repo: LayoutRepository = LayoutRepository()): List<Slot> {
        val id = layoutIdFor(photoCount, repo)
        return repo.getLayouts(photoCount.coerceAtLeast(1)).first { it.id == id }.slots
    }

    /** Preset + the user's photos dropped into the template's slots (slot i -> photo i). */
    fun buildProject(photoUris: List<String>, repo: LayoutRepository = LayoutRepository()): CollageProject {
        val slots = slotsFor(photoUris.size, repo)
        return preset.copy(
            layoutId = layoutIdFor(photoUris.size, repo),
            slots = slots,
            photos = slots.indices.map { i ->
                PhotoTransform(uri = photoUris.getOrElse(i) { photoUris.firstOrNull() ?: "" })
            }
        )
    }
}

object TemplateRepository {

    // ---- tiny DSL ---------------------------------------------------------------------------
    private fun txt(
        text: String, x: Float, y: Float, font: String = "Script",
        color: Long = 0xFFFFFFFF, scale: Float = 1f, rotation: Float = 0f
    ) = ProjectTextLayer(
        id = "tpl_txt_${text.hashCode()}_${x}_$y", text = text, x = x, y = y, fontName = font,
        colorLong = color, rotation = rotation, scale = scale, compact = true
    )

    private fun dec(emoji: String, x: Float, y: Float, scale: Float = 1.2f, rotation: Float = 0f) =
        ProjectOverlayItem.Emoji(
            id = "tpl_dec_${emoji.hashCode()}_${x}_$y", emoji = emoji, x = x, y = y,
            rotation = rotation, scale = scale
        )

    private fun template(
        id: String, title: String, category: TemplateCategory, thumb: TemplateThumbStyle,
        ratioW: Float, ratioH: Float, gradient: List<Long>, photos: Int, layoutIndex: Int = 0,
        outer: Float = 0.035f, inner: Float = 0.018f, corner: Float = 0.045f,
        filter: String = "Original", filterIntensity: Float = 0f,
        adjust: Map<String, Float> = emptyMap(),
        texts: List<ProjectTextLayer> = emptyList(), decor: List<ProjectOverlayItem> = emptyList(),
        accent: Long = gradient.first()
    ) = TemplateItem(
        id = id, title = title, category = category, thumbStyle = thumb,
        preset = CollageProject(
            aspectRatioWidth = ratioW, aspectRatioHeight = ratioH,
            outerSpacingRatio = outer, innerSpacingRatio = inner, cornerRadiusRatio = corner,
            backgroundColorLong = gradient.first(),
            backgroundGradientColors = if (gradient.size >= 2) gradient else emptyList(),
            filterPreset = filter, filterIntensity = filterIntensity, adjustValues = adjust,
            overlays = decor, textLayers = texts
        ),
        preferredPhotos = photos, layoutIndex = layoutIndex, accentColorLong = accent
    )

    private const val WHITE = 0xFFFFFFFFL
    private const val GOLD = 0xFFFDE68A

    val all: List<TemplateItem> = listOf(
        // ------------------------------------------------------------------ Christmas
        template("xmas_classic", "Merry Christmas", TemplateCategory.Christmas, TemplateThumbStyle.CLASSIC,
            4f, 5f, listOf(0xFFB91C1C, 0xFF14532D), photos = 4, layoutIndex = 4,
            filter = "Warm", filterIntensity = 40f,
            texts = listOf(txt("Merry Christmas", 0.5f, 0.07f, "Script", WHITE, 1.25f)),
            decor = listOf(dec("🎄", 0.12f, 0.93f, 1.7f), dec("🎅", 0.88f, 0.92f, 1.6f), dec("❄️", 0.9f, 0.07f, 1.1f, 15f), dec("⭐", 0.1f, 0.07f, 1.1f, -10f))),
        template("xmas_snow", "Snowy Night", TemplateCategory.Christmas, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF0F172A, 0xFF1E3A8A, 0xFF93C5FD), photos = 3, layoutIndex = 0,
            filter = "Cool", filterIntensity = 35f,
            texts = listOf(txt("Merry Christmas", 0.5f, 0.06f, "Script", WHITE, 1.2f), txt("& Happy Holidays", 0.5f, 0.945f, "Modern", GOLD, 1f)),
            decor = listOf(dec("❄️", 0.15f, 0.14f, 1.2f), dec("❄️", 0.85f, 0.2f, 0.9f, 20f), dec("⛄", 0.82f, 0.88f, 1.7f), dec("🎁", 0.14f, 0.88f, 1.4f))),
        template("xmas_gift", "Gift Box", TemplateCategory.Christmas, TemplateThumbStyle.BANNER,
            1f, 1f, listOf(0xFF166534, 0xFFDC2626), photos = 2, layoutIndex = 0,
            outer = 0.05f, corner = 0.06f,
            texts = listOf(txt("Season's Greetings", 0.5f, 0.93f, "Bold", WHITE, 1f)),
            decor = listOf(dec("🎁", 0.1f, 0.1f, 1.5f, -12f), dec("🎄", 0.9f, 0.1f, 1.5f, 12f), dec("🔔", 0.5f, 0.5f, 1.2f))),

        // ------------------------------------------------------------------ New Year
        template("ny_countdown", "Happy New Year 2026", TemplateCategory.NewYear, TemplateThumbStyle.BANNER,
            4f, 5f, listOf(0xFF0B1026, 0xFF4C1D95, 0xFFF59E0B), photos = 4, layoutIndex = 4,
            filter = "Vivid", filterIntensity = 30f,
            texts = listOf(txt("Happy New Year 2026", 0.5f, 0.07f, "Neon", GOLD, 1.15f)),
            decor = listOf(dec("🎆", 0.12f, 0.93f, 1.6f), dec("🥂", 0.5f, 0.94f, 1.6f), dec("🎇", 0.88f, 0.93f, 1.6f), dec("✨", 0.9f, 0.07f, 1.2f))),
        template("ny_sparkle", "Sparkle Midnight", TemplateCategory.NewYear, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF111827, 0xFF7C3AED), photos = 3, layoutIndex = 0,
            filter = "Moody", filterIntensity = 30f,
            texts = listOf(txt("Cheers to 2026", 0.5f, 0.06f, "Script", WHITE, 1.25f)),
            decor = listOf(dec("🎉", 0.14f, 0.92f, 1.5f), dec("🍾", 0.86f, 0.92f, 1.5f), dec("✨", 0.2f, 0.16f, 1.1f), dec("✨", 0.82f, 0.13f, 1.4f, 15f))),
        template("ny_gold", "Golden Countdown", TemplateCategory.NewYear, TemplateThumbStyle.CLASSIC,
            1f, 1f, listOf(0xFF1C1917, 0xFFB45309), photos = 4, layoutIndex = 4,
            outer = 0.045f, filter = "Golden", filterIntensity = 40f,
            texts = listOf(txt("Hello 2026", 0.5f, 0.5f, "Bold", GOLD, 1.5f)),
            decor = listOf(dec("🎊", 0.1f, 0.1f, 1.4f), dec("🎇", 0.9f, 0.9f, 1.4f))),

        // ------------------------------------------------------------------ Birthday
        template("bday_party", "Happy Birthday", TemplateCategory.Birthday, TemplateThumbStyle.CLASSIC,
            4f, 5f, listOf(0xFFEC4899, 0xFFF59E0B), photos = 4, layoutIndex = 4,
            filter = "Vivid", filterIntensity = 35f,
            texts = listOf(txt("Happy Birthday", 0.5f, 0.07f, "Script", WHITE, 1.3f)),
            decor = listOf(dec("🎂", 0.12f, 0.93f, 1.7f), dec("🎈", 0.9f, 0.92f, 1.6f, 10f), dec("🎉", 0.9f, 0.07f, 1.3f), dec("🎁", 0.1f, 0.07f, 1.2f, -10f))),
        template("bday_confetti", "Confetti Bash", TemplateCategory.Birthday, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF6366F1, 0xFFEC4899, 0xFFFBBF24), photos = 3, layoutIndex = 0,
            texts = listOf(txt("Happy Birthday!", 0.5f, 0.06f, "Bold", WHITE, 1.2f), txt("Make a wish ✨", 0.5f, 0.945f, "Script", WHITE, 1.1f)),
            decor = listOf(dec("🎈", 0.12f, 0.14f, 1.5f, -10f), dec("🎈", 0.88f, 0.18f, 1.3f, 12f), dec("🥳", 0.86f, 0.88f, 1.6f), dec("🎊", 0.14f, 0.88f, 1.4f))),
        template("bday_cake", "Cake Time", TemplateCategory.Birthday, TemplateThumbStyle.BANNER,
            1f, 1f, listOf(0xFFFDA4AF, 0xFFFDE68A), photos = 2, layoutIndex = 1,
            outer = 0.05f, corner = 0.07f, filter = "Soft", filterIntensity = 35f,
            texts = listOf(txt("Happy Birthday", 0.5f, 0.93f, "Script", 0xFF9D174D, 1.2f)),
            decor = listOf(dec("🎂", 0.89f, 0.1f, 1.6f, 10f), dec("🍰", 0.1f, 0.1f, 1.4f, -10f))),

        // ------------------------------------------------------------------ Love
        template("love_forever", "Love Forever", TemplateCategory.Love, TemplateThumbStyle.CLASSIC,
            4f, 5f, listOf(0xFFBE123C, 0xFFF472B6), photos = 2, layoutIndex = 0,
            outer = 0.045f, corner = 0.06f, filter = "Rise", filterIntensity = 35f,
            texts = listOf(txt("Love Forever", 0.5f, 0.07f, "Script", WHITE, 1.3f)),
            decor = listOf(dec("💖", 0.5f, 0.5f, 1.7f), dec("🌹", 0.1f, 0.93f, 1.5f, -10f), dec("💕", 0.9f, 0.93f, 1.5f, 10f))),
        template("love_us", "You & Me", TemplateCategory.Love, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF831843, 0xFFFB7185), photos = 2, layoutIndex = 1,
            filter = "Dreamy", filterIntensity = 30f,
            texts = listOf(txt("You & Me", 0.5f, 0.06f, "Script", WHITE, 1.3f), txt("Always", 0.5f, 0.945f, "Serif", 0xFFFFE4E6, 1.1f)),
            decor = listOf(dec("💘", 0.14f, 0.15f, 1.3f, -12f), dec("💞", 0.86f, 0.88f, 1.5f, 10f), dec("💍", 0.5f, 0.5f, 1.2f))),
        template("love_valentine", "Valentine", TemplateCategory.Love, TemplateThumbStyle.BANNER,
            1f, 1f, listOf(0xFFFFE4E6, 0xFFFDA4AF), photos = 3, layoutIndex = 0,
            outer = 0.05f, corner = 0.08f,
            texts = listOf(txt("Be My Valentine", 0.5f, 0.93f, "Script", 0xFFBE123C, 1.2f)),
            decor = listOf(dec("❤️", 0.1f, 0.1f, 1.5f, -10f), dec("💌", 0.9f, 0.1f, 1.4f, 10f))),

        // ------------------------------------------------------------------ Story (9:16)
        template("story_vibes", "Daily Vibes", TemplateCategory.Story, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF0EA5E9, 0xFF8B5CF6), photos = 3, layoutIndex = 0,
            filter = "Vivid", filterIntensity = 30f,
            texts = listOf(txt("Daily Vibes", 0.5f, 0.06f, "Bold", WHITE, 1.2f)),
            decor = listOf(dec("✨", 0.86f, 0.12f, 1.2f), dec("🔥", 0.14f, 0.93f, 1.3f))),
        template("story_travel", "Travel Diary", TemplateCategory.Story, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF065F46, 0xFF0891B2), photos = 4, layoutIndex = 0,
            filter = "Sierra", filterIntensity = 30f,
            texts = listOf(txt("Travel Diary", 0.5f, 0.06f, "Script", WHITE, 1.25f)),
            decor = listOf(dec("✈️", 0.87f, 0.93f, 1.4f, -15f), dec("🌴", 0.12f, 0.93f, 1.5f), dec("🗺️", 0.88f, 0.07f, 1.2f))),
        template("story_memories", "Memories", TemplateCategory.Story, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF1F2937, 0xFFF97316), photos = 3, layoutIndex = 1,
            filter = "Film", filterIntensity = 35f,
            texts = listOf(txt("Memories", 0.5f, 0.06f, "Serif", WHITE, 1.3f), txt("2026", 0.5f, 0.945f, "Retro", GOLD, 1.1f)),
            decor = listOf(dec("📸", 0.14f, 0.93f, 1.3f, -8f))),

        // ------------------------------------------------------------------ Polaroid (white frames)
        template("pol_classic", "Classic Polaroid", TemplateCategory.Polaroid, TemplateThumbStyle.POLAROID,
            4f, 5f, listOf(0xFFF5F0E6, 0xFFE7DCC8), photos = 4, layoutIndex = 4,
            outer = 0.05f, inner = 0.035f, corner = 0.01f, filter = "Polaroid", filterIntensity = 60f,
            texts = listOf(txt("good times", 0.5f, 0.945f, "Script", 0xFF44403C, 1.1f)),
            decor = listOf(dec("📷", 0.9f, 0.06f, 1.2f, 12f)), accent = 0xFFB45309),
        template("pol_pastel", "Pastel Frames", TemplateCategory.Polaroid, TemplateThumbStyle.POLAROID,
            1f, 1f, listOf(0xFFFBCFE8, 0xFFBAE6FD), photos = 4, layoutIndex = 4,
            outer = 0.055f, inner = 0.04f, corner = 0.012f, filter = "Faded", filterIntensity = 40f,
            texts = listOf(txt("little moments", 0.5f, 0.94f, "Script", WHITE, 1.1f)),
            decor = listOf(dec("🌸", 0.1f, 0.08f, 1.2f, -10f), dec("🦋", 0.9f, 0.08f, 1.2f, 12f)), accent = 0xFFEC4899),
        template("pol_film", "Film Roll", TemplateCategory.Polaroid, TemplateThumbStyle.POLAROID,
            9f, 16f, listOf(0xFF171717, 0xFF404040), photos = 3, layoutIndex = 0,
            outer = 0.06f, inner = 0.04f, corner = 0.008f, filter = "Kodak", filterIntensity = 55f,
            texts = listOf(txt("ANALOG", 0.5f, 0.05f, "Retro", WHITE, 1.1f)),
            decor = listOf(dec("🎞️", 0.88f, 0.94f, 1.3f)), accent = 0xFF525252)
    )

    fun byCategory(category: TemplateCategory): List<TemplateItem> =
        if (category == TemplateCategory.All) all else all.filter { it.category == category }

    fun byId(id: String?): TemplateItem? = id?.let { key -> all.firstOrNull { it.id == key } }
}