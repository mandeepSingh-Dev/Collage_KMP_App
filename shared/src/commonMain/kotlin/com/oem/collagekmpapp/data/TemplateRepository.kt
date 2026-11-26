package com.oem.collagekmpapp.data

import androidx.compose.runtime.mutableStateListOf
import com.oem.collagekmpapp.KeyValueStore
import com.oem.collagekmpapp.model.CollageProject
import com.oem.collagekmpapp.model.PhotoTransform
import com.oem.collagekmpapp.model.ProjectOverlayItem
import com.oem.collagekmpapp.model.ProjectTextLayer
import com.oem.collagekmpapp.model.Slot

// commonMain – themed collage templates.

enum class TemplateCategory(val displayName: String, val emoji: String) {
    All("All", "✨"),
    /** The user's own presets, saved from the collage editor ("Save as Template"). */
    Custom("My Presets", "⭐"),
    Christmas("Christmas", "🎄"),
    NewYear("New Year", "🎆"),
    Birthday("Birthday", "🎂"),
    Love("Love", "💖"),
    Story("Story", "📱"),
    Polaroid("Polaroid", "📷"),

    // ---- added themes
    Summer("Summer & Beach", "🏖️"),
    Travel("Travel & Adventure", "✈️"),
    Wedding("Wedding & Anniversaries", "💍"),
    Minimal("Minimal & Aesthetic", "🎨"),
    Food("Foodie & Café", "☕"),
    Party("Music & Party", "🎉"),

    // ---- social media formats (feed 1:1 / 4:5, stories & reels 9:16)
    Instagram("Instagram", "📸"),
    Facebook("Facebook", "👍"),
    Reels("Reels & Shorts", "🎬")
}

/** How the small preview card draws the template's photo slots. */
enum class TemplateThumbStyle { CLASSIC, POLAROID, STORY, BANNER }

/** Photo-count filter chips under the category chips (matches [TemplateItem.preferredPhotos]). */
enum class TemplatePhotoFilter(val label: String) {
    All("All Photos"),
    Two("2 Photos"),
    Three("3 Photos"),
    FourPlus("4+ Photos");

    fun matches(photos: Int): Boolean = when (this) {
        All -> true
        Two -> photos == 2
        Three -> photos == 3
        FourPlus -> photos >= 4
    }
}

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

    /**
     * Smart slot order for [photoCount] photos: the slot with the LARGEST area (the featured slot) first,
     * then every other slot in the layout's own order. Ties keep the first slot, so equal grids are unchanged.
     */
    fun smartSlotOrder(photoCount: Int, repo: LayoutRepository = LayoutRepository()): List<Int> {
        val slots = slotsFor(photoCount, repo)
        if (slots.size < 2) return slots.indices.toList()
        var primary = 0
        var best = -1f
        slots.forEachIndexed { i, s ->
            val area = (s.right - s.left) * (s.bottom - s.top)
            if (area > best + 1e-4f) { best = area; primary = i }   // strictly larger (with tolerance) -> ties keep the first
        }
        return listOf(primary) + slots.indices.filter { it != primary }
    }

    /**
     * The user's photos rearranged so that list index == slot index (what the editor and the renderer expect):
     * the FIRST picked photo goes into the featured (largest) slot, the remaining photos fill the other slots in order.
     * Returns the list unchanged when there is nothing to reorder.
     */
    fun orderPhotosForSlots(photoUris: List<String>, repo: LayoutRepository = LayoutRepository()): List<String> {
        if (photoUris.size < 2) return photoUris
        val slotCount = slotsFor(photoUris.size, repo).size
        if (slotCount != photoUris.size) return photoUris
        val order = smartSlotOrder(photoUris.size, repo)
        val out = MutableList(slotCount) { "" }
        order.forEachIndexed { k, slotIndex -> out[slotIndex] = photoUris[k] }
        return out
    }

    /** Preset + the user's photos dropped into the template's slots (first photo -> featured slot, see [orderPhotosForSlots]). */
    fun buildProject(photoUris: List<String>, repo: LayoutRepository = LayoutRepository()): CollageProject {
        val slots = slotsFor(photoUris.size, repo)
        val ordered = orderPhotosForSlots(photoUris, repo)
        return preset.copy(
            layoutId = layoutIdFor(photoUris.size, repo),
            slots = slots,
            photos = slots.indices.map { i ->
                PhotoTransform(uri = ordered.getOrElse(i) { ordered.firstOrNull() ?: "" })
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
            decor = listOf(dec("🎞️", 0.88f, 0.94f, 1.3f)), accent = 0xFF525252),

        // ------------------------------------------------------------------ Summer & Beach
        template("summer_beach", "Beach Days", TemplateCategory.Summer, TemplateThumbStyle.CLASSIC,
            4f, 5f, listOf(0xFF0EA5E9, 0xFF38BDF8, 0xFFFDE68A), photos = 4, layoutIndex = 4,
            filter = "Warm", filterIntensity = 35f,
            texts = listOf(txt("Beach Days", 0.5f, 0.07f, "Script", WHITE, 1.3f)),
            decor = listOf(dec("🏖️", 0.12f, 0.93f, 1.6f), dec("🌴", 0.88f, 0.92f, 1.6f, 8f), dec("☀️", 0.9f, 0.07f, 1.2f), dec("🐚", 0.1f, 0.07f, 1.1f, -10f))),
        template("summer_tropical", "Tropical Vibes", TemplateCategory.Summer, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF065F46, 0xFF10B981, 0xFFFDE047), photos = 3, layoutIndex = 1,
            filter = "Vivid", filterIntensity = 30f,
            texts = listOf(txt("Tropical Vibes", 0.5f, 0.06f, "Bold", WHITE, 1.15f), txt("stay sunny ☀️", 0.5f, 0.945f, "Script", GOLD, 1.1f)),
            decor = listOf(dec("🍍", 0.14f, 0.14f, 1.3f, -10f), dec("🌺", 0.86f, 0.18f, 1.3f, 12f), dec("🥥", 0.86f, 0.88f, 1.5f), dec("🌴", 0.14f, 0.88f, 1.6f))),
        template("summer_sunset", "Golden Hour", TemplateCategory.Summer, TemplateThumbStyle.BANNER,
            1f, 1f, listOf(0xFFF97316, 0xFFDB2777, 0xFF7C3AED), photos = 2, layoutIndex = 0,
            outer = 0.05f, corner = 0.06f, filter = "Golden", filterIntensity = 40f,
            texts = listOf(txt("Golden Hour", 0.5f, 0.93f, "Serif", WHITE, 1.2f)),
            decor = listOf(dec("🌅", 0.1f, 0.1f, 1.4f, -10f), dec("🕶️", 0.9f, 0.1f, 1.3f, 12f))),
        template("summer_pool", "Pool Party", TemplateCategory.Summer, TemplateThumbStyle.CLASSIC,
            1f, 1f, listOf(0xFF22D3EE, 0xFF3B82F6), photos = 5, layoutIndex = 0,
            filter = "Cool", filterIntensity = 25f,
            texts = listOf(txt("Pool Party", 0.5f, 0.5f, "Bold", WHITE, 1.4f)),
            decor = listOf(dec("🍉", 0.1f, 0.92f, 1.4f, -8f), dec("🍹", 0.9f, 0.08f, 1.3f, 10f), dec("🏊", 0.9f, 0.92f, 1.4f))),

        // ------------------------------------------------------------------ Travel & Adventure
        template("travel_passport", "Wanderlust", TemplateCategory.Travel, TemplateThumbStyle.POLAROID,
            4f, 5f, listOf(0xFFF5F0E6, 0xFFD6C7A1), photos = 4, layoutIndex = 4,
            outer = 0.05f, inner = 0.035f, corner = 0.01f, filter = "Film", filterIntensity = 35f,
            texts = listOf(txt("Wanderlust", 0.5f, 0.945f, "Script", 0xFF44403C, 1.15f)),
            decor = listOf(dec("🧭", 0.9f, 0.06f, 1.2f, 12f), dec("🗺️", 0.1f, 0.06f, 1.1f, -8f)), accent = 0xFF92400E),
        template("travel_roadtrip", "Road Trip", TemplateCategory.Travel, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF1E293B, 0xFFEA580C, 0xFFFBBF24), photos = 4, layoutIndex = 1,
            filter = "Sierra", filterIntensity = 30f,
            texts = listOf(txt("Road Trip", 0.5f, 0.06f, "Retro", WHITE, 1.25f), txt("miles & memories", 0.5f, 0.945f, "Script", GOLD, 1.05f)),
            decor = listOf(dec("🚗", 0.14f, 0.88f, 1.5f, -6f), dec("🛣️", 0.86f, 0.88f, 1.4f), dec("📍", 0.86f, 0.14f, 1.2f, 10f))),
        template("travel_mountains", "Into the Wild", TemplateCategory.Travel, TemplateThumbStyle.CLASSIC,
            4f, 5f, listOf(0xFF14532D, 0xFF0F766E, 0xFF94A3B8), photos = 3, layoutIndex = 2,
            filter = "Moody", filterIntensity = 25f,
            texts = listOf(txt("Into the Wild", 0.5f, 0.07f, "Serif", WHITE, 1.2f)),
            decor = listOf(dec("🏔️", 0.12f, 0.93f, 1.6f), dec("⛺", 0.88f, 0.93f, 1.5f), dec("🌲", 0.9f, 0.07f, 1.1f), dec("🦅", 0.1f, 0.07f, 1.1f, -10f))),
        template("travel_city", "City Break", TemplateCategory.Travel, TemplateThumbStyle.BANNER,
            1f, 1f, listOf(0xFF312E81, 0xFF0EA5E9), photos = 6, layoutIndex = 0,
            outer = 0.04f, inner = 0.015f, filter = "Vivid", filterIntensity = 25f,
            texts = listOf(txt("City Break", 0.5f, 0.93f, "Bold", WHITE, 1.15f)),
            decor = listOf(dec("✈️", 0.1f, 0.1f, 1.3f, -15f), dec("🏙️", 0.9f, 0.1f, 1.3f))),

        // ------------------------------------------------------------------ Wedding & Anniversaries
        template("wed_ivory", "Just Married", TemplateCategory.Wedding, TemplateThumbStyle.CLASSIC,
            4f, 5f, listOf(0xFFFFFBEB, 0xFFF5E6CA), photos = 3, layoutIndex = 0,
            outer = 0.05f, corner = 0.03f, filter = "Soft", filterIntensity = 35f,
            texts = listOf(txt("Just Married", 0.5f, 0.07f, "Script", 0xFF92400E, 1.3f), txt("forever begins today", 0.5f, 0.945f, "Serif", 0xFFB45309, 0.9f)),
            decor = listOf(dec("🤍", 0.1f, 0.07f, 1.1f, -8f), dec("🕊️", 0.9f, 0.07f, 1.1f, 10f)), accent = 0xFFB45309),
        template("wed_blush", "Our Big Day", TemplateCategory.Wedding, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFFFCE7F3, 0xFFFBCFE8, 0xFFF9A8D4), photos = 4, layoutIndex = 0,
            filter = "Dreamy", filterIntensity = 30f,
            texts = listOf(txt("Our Big Day", 0.5f, 0.06f, "Script", 0xFF9D174D, 1.3f), txt("Mr & Mrs", 0.5f, 0.945f, "Serif", 0xFFBE185D, 1.1f)),
            decor = listOf(dec("💐", 0.14f, 0.88f, 1.5f), dec("💒", 0.86f, 0.88f, 1.5f), dec("🥂", 0.86f, 0.14f, 1.2f, 12f)), accent = 0xFFDB2777),
        template("wed_anniversary", "Happy Anniversary", TemplateCategory.Wedding, TemplateThumbStyle.BANNER,
            1f, 1f, listOf(0xFF7F1D1D, 0xFFB45309), photos = 2, layoutIndex = 0,
            outer = 0.05f, corner = 0.05f, filter = "Golden", filterIntensity = 35f,
            texts = listOf(txt("Happy Anniversary", 0.5f, 0.93f, "Script", GOLD, 1.1f)),
            decor = listOf(dec("💞", 0.1f, 0.1f, 1.4f, -10f), dec("🍾", 0.9f, 0.1f, 1.4f, 10f))),
        template("wed_vows", "Golden Vows", TemplateCategory.Wedding, TemplateThumbStyle.POLAROID,
            4f, 5f, listOf(0xFF1C1917, 0xFF57534E), photos = 4, layoutIndex = 4,
            outer = 0.05f, inner = 0.035f, corner = 0.01f, filter = "Faded", filterIntensity = 35f,
            texts = listOf(txt("Always & Forever", 0.5f, 0.945f, "Serif", GOLD, 1.05f)),
            decor = listOf(dec("💍", 0.9f, 0.06f, 1.2f, 12f)), accent = 0xFFCA8A04),

        // ------------------------------------------------------------------ Minimal & Aesthetic
        template("min_mono", "Monochrome", TemplateCategory.Minimal, TemplateThumbStyle.CLASSIC,
            1f, 1f, listOf(0xFFFAFAFA, 0xFFF4F4F5), photos = 4, layoutIndex = 4,
            outer = 0.06f, inner = 0.03f, corner = 0f, filter = "Faded", filterIntensity = 25f,
            texts = listOf(txt("less is more", 0.5f, 0.95f, "Modern", 0xFF27272A, 0.9f)),
            accent = 0xFF52525B),
        template("min_neutral", "Soft Neutrals", TemplateCategory.Minimal, TemplateThumbStyle.CLASSIC,
            4f, 5f, listOf(0xFFF5F5F4, 0xFFE7E5E4, 0xFFD6D3D1), photos = 3, layoutIndex = 1,
            outer = 0.07f, inner = 0.03f, corner = 0.02f, filter = "Faded", filterIntensity = 35f,
            texts = listOf(txt("soft days", 0.5f, 0.955f, "Serif", 0xFF57534E, 1f)),
            decor = listOf(dec("🌾", 0.9f, 0.05f, 1f, 12f)), accent = 0xFF78716C),
        template("min_sage", "Sage Story", TemplateCategory.Minimal, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFFD9E4D3, 0xFFA3B18A), photos = 2, layoutIndex = 0,
            outer = 0.07f, inner = 0.03f, corner = 0.03f, filter = "Soft", filterIntensity = 25f,
            texts = listOf(txt("slow living", 0.5f, 0.05f, "Serif", 0xFF3F4E2F, 1.1f)),
            decor = listOf(dec("🌿", 0.86f, 0.95f, 1.1f)), accent = 0xFF6B7F52),
        template("min_grid", "Clean Grid", TemplateCategory.Minimal, TemplateThumbStyle.BANNER,
            1f, 1f, listOf(0xFF18181B, 0xFF3F3F46), photos = 6, layoutIndex = 0,
            outer = 0.05f, inner = 0.012f, corner = 0f, filter = "Film", filterIntensity = 25f,
            texts = listOf(txt("MOMENTS", 0.5f, 0.94f, "Modern", WHITE, 1f)),
            accent = 0xFF71717A),

        // ------------------------------------------------------------------ Foodie & Café
        template("food_coffee", "Coffee Date", TemplateCategory.Food, TemplateThumbStyle.CLASSIC,
            4f, 5f, listOf(0xFF451A03, 0xFF92400E, 0xFFD6A06A), photos = 3, layoutIndex = 0,
            filter = "Warm", filterIntensity = 35f,
            texts = listOf(txt("But First, Coffee", 0.5f, 0.07f, "Script", WHITE, 1.15f)),
            decor = listOf(dec("☕", 0.12f, 0.93f, 1.6f), dec("🥐", 0.88f, 0.93f, 1.5f, 10f), dec("🤎", 0.9f, 0.07f, 1f))),
        template("food_brunch", "Sunday Brunch", TemplateCategory.Food, TemplateThumbStyle.BANNER,
            1f, 1f, listOf(0xFFFEF3C7, 0xFFFDBA74), photos = 4, layoutIndex = 4,
            outer = 0.05f, corner = 0.05f, filter = "Rise", filterIntensity = 30f,
            texts = listOf(txt("Sunday Brunch", 0.5f, 0.93f, "Script", 0xFF9A3412, 1.2f)),
            decor = listOf(dec("🥞", 0.1f, 0.1f, 1.4f, -10f), dec("🍳", 0.9f, 0.1f, 1.4f, 10f)), accent = 0xFFEA580C),
        template("food_foodie", "Foodie Diaries", TemplateCategory.Food, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFFB91C1C, 0xFFF97316, 0xFFFACC15), photos = 4, layoutIndex = 0,
            filter = "Vivid", filterIntensity = 35f,
            texts = listOf(txt("Foodie Diaries", 0.5f, 0.06f, "Bold", WHITE, 1.15f), txt("eat • snap • repeat", 0.5f, 0.945f, "Script", WHITE, 1f)),
            decor = listOf(dec("🍕", 0.14f, 0.14f, 1.3f, -12f), dec("🍔", 0.86f, 0.18f, 1.3f, 10f), dec("🍟", 0.14f, 0.88f, 1.4f), dec("🍩", 0.86f, 0.88f, 1.4f))),
        template("food_dessert", "Sweet Treats", TemplateCategory.Food, TemplateThumbStyle.POLAROID,
            1f, 1f, listOf(0xFFFCE7F3, 0xFFFDE68A), photos = 4, layoutIndex = 4,
            outer = 0.055f, inner = 0.04f, corner = 0.012f, filter = "Soft", filterIntensity = 35f,
            texts = listOf(txt("sweet tooth", 0.5f, 0.94f, "Script", 0xFFBE185D, 1.1f)),
            decor = listOf(dec("🧁", 0.1f, 0.08f, 1.2f, -10f), dec("🍓", 0.9f, 0.08f, 1.2f, 12f)), accent = 0xFFEC4899),

        // ------------------------------------------------------------------ Music & Party
        template("party_neon", "Neon Nights", TemplateCategory.Party, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF0F0A1E, 0xFF581C87, 0xFFDB2777), photos = 3, layoutIndex = 0,
            filter = "Moody", filterIntensity = 25f,
            texts = listOf(txt("Neon Nights", 0.5f, 0.06f, "Neon", 0xFFF0ABFC, 1.25f), txt("let's dance", 0.5f, 0.945f, "Neon", 0xFF67E8F9, 1f)),
            decor = listOf(dec("🪩", 0.86f, 0.14f, 1.4f), dec("🎧", 0.14f, 0.88f, 1.4f, -8f), dec("💃", 0.86f, 0.88f, 1.4f)), accent = 0xFFC026D3),
        template("party_concert", "Live & Loud", TemplateCategory.Party, TemplateThumbStyle.CLASSIC,
            4f, 5f, listOf(0xFF020617, 0xFF1E3A8A, 0xFF06B6D4), photos = 4, layoutIndex = 4,
            filter = "Cool", filterIntensity = 30f,
            texts = listOf(txt("Live & Loud", 0.5f, 0.07f, "Bold", WHITE, 1.25f)),
            decor = listOf(dec("🎸", 0.12f, 0.93f, 1.6f, -12f), dec("🎤", 0.88f, 0.93f, 1.5f, 10f), dec("🎶", 0.9f, 0.07f, 1.1f), dec("⚡", 0.1f, 0.07f, 1.1f))),
        template("party_squad", "Squad Goals", TemplateCategory.Party, TemplateThumbStyle.BANNER,
            1f, 1f, listOf(0xFFF43F5E, 0xFFF59E0B, 0xFF8B5CF6), photos = 5, layoutIndex = 0,
            outer = 0.045f, filter = "Vivid", filterIntensity = 30f,
            texts = listOf(txt("Squad Goals", 0.5f, 0.93f, "Bold", WHITE, 1.15f)),
            decor = listOf(dec("🎉", 0.1f, 0.1f, 1.4f, -10f), dec("🕺", 0.9f, 0.1f, 1.4f, 10f))),
        template("party_disco", "Disco Fever", TemplateCategory.Party, TemplateThumbStyle.CLASSIC,
            1f, 1f, listOf(0xFF7C2D12, 0xFFEA580C, 0xFFFACC15), photos = 4, layoutIndex = 4,
            outer = 0.045f, filter = "Film", filterIntensity = 40f,
            texts = listOf(txt("Disco Fever", 0.5f, 0.5f, "Retro", WHITE, 1.3f)),
            decor = listOf(dec("🪩", 0.1f, 0.1f, 1.3f), dec("📻", 0.9f, 0.9f, 1.3f))),

        // ------------------------------------------------------------------ Instagram (1:1 & 4:5 feed, 9:16 story)
        template("ig_vibes", "Good Vibes Only", TemplateCategory.Instagram, TemplateThumbStyle.CLASSIC,
            1f, 1f, listOf(0xFF833AB4, 0xFFFD1D1D, 0xFFFCB045), photos = 4, layoutIndex = 4,
            outer = 0.04f, filter = "Vivid", filterIntensity = 25f,
            texts = listOf(txt("Good Vibes Only", 0.5f, 0.5f, "Script", WHITE, 1.2f)),
            decor = listOf(dec("📸", 0.1f, 0.1f, 1.3f, -10f), dec("❤️", 0.9f, 0.9f, 1.3f, 10f))),
        template("ig_carousel", "Carousel Post", TemplateCategory.Instagram, TemplateThumbStyle.CLASSIC,
            4f, 5f, listOf(0xFFFDF2F8, 0xFFE9D5FF), photos = 2, layoutIndex = 1,
            outer = 0.05f, corner = 0.04f, filter = "Soft", filterIntensity = 30f,
            texts = listOf(txt("swipe for more →", 0.5f, 0.95f, "Modern", 0xFF6B21A8, 0.95f)),
            decor = listOf(dec("✨", 0.9f, 0.05f, 1f, 10f)), accent = 0xFFA855F7),
        template("ig_story", "Story Highlights", TemplateCategory.Instagram, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF4F46E5, 0xFFC026D3, 0xFFF97316), photos = 3, layoutIndex = 1,
            filter = "Dreamy", filterIntensity = 25f,
            texts = listOf(txt("Highlights", 0.5f, 0.06f, "Script", WHITE, 1.3f), txt("tap to see more", 0.5f, 0.945f, "Modern", WHITE, 0.9f)),
            decor = listOf(dec("💫", 0.86f, 0.14f, 1.2f), dec("📍", 0.14f, 0.88f, 1.3f, -8f), dec("🔥", 0.86f, 0.88f, 1.3f))),
        template("ig_dump", "Photo Dump", TemplateCategory.Instagram, TemplateThumbStyle.BANNER,
            1f, 1f, listOf(0xFF0F172A, 0xFF334155), photos = 6, layoutIndex = 0,
            outer = 0.035f, inner = 0.012f, filter = "Film", filterIntensity = 30f,
            texts = listOf(txt("photo dump", 0.5f, 0.94f, "Retro", WHITE, 1f)),
            decor = listOf(dec("📷", 0.9f, 0.08f, 1.1f, 12f)), accent = 0xFF475569),

        // ------------------------------------------------------------------ Facebook (1:1 & 4:5 feed, 9:16 story)
        template("fb_family", "Family Moments", TemplateCategory.Facebook, TemplateThumbStyle.CLASSIC,
            4f, 5f, listOf(0xFF1877F2, 0xFF60A5FA), photos = 5, layoutIndex = 0,
            filter = "Warm", filterIntensity = 25f,
            texts = listOf(txt("Family Moments", 0.5f, 0.07f, "Script", WHITE, 1.2f)),
            decor = listOf(dec("🏡", 0.12f, 0.93f, 1.5f), dec("❤️", 0.88f, 0.93f, 1.4f, 10f), dec("🤗", 0.9f, 0.07f, 1.1f))),
        template("fb_event", "Event Recap", TemplateCategory.Facebook, TemplateThumbStyle.BANNER,
            1f, 1f, listOf(0xFF1E3A8A, 0xFF1877F2), photos = 4, layoutIndex = 4,
            outer = 0.045f, filter = "Vivid", filterIntensity = 20f,
            texts = listOf(txt("Event Recap", 0.5f, 0.93f, "Bold", WHITE, 1.15f)),
            decor = listOf(dec("📅", 0.1f, 0.1f, 1.3f, -8f), dec("👍", 0.9f, 0.1f, 1.4f, 10f))),
        template("fb_throwback", "Throwback", TemplateCategory.Facebook, TemplateThumbStyle.POLAROID,
            1f, 1f, listOf(0xFFE0E7FF, 0xFFBFDBFE), photos = 3, layoutIndex = 0,
            outer = 0.055f, inner = 0.04f, corner = 0.012f, filter = "Polaroid", filterIntensity = 50f,
            texts = listOf(txt("#throwback", 0.5f, 0.94f, "Script", 0xFF1E40AF, 1.1f)),
            decor = listOf(dec("⏳", 0.9f, 0.08f, 1.1f, 12f)), accent = 0xFF1877F2),
        template("fb_weekend", "Weekend Mode", TemplateCategory.Facebook, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF0C4A6E, 0xFF1877F2, 0xFF7DD3FC), photos = 2, layoutIndex = 0,
            filter = "Cool", filterIntensity = 25f,
            texts = listOf(txt("Weekend Mode", 0.5f, 0.06f, "Bold", WHITE, 1.15f)),
            decor = listOf(dec("🌤️", 0.86f, 0.14f, 1.2f), dec("😎", 0.86f, 0.88f, 1.5f))),

        // ------------------------------------------------------------------ Reels & Shorts (9:16 covers / status)
        template("reel_cover", "Viral Cover", TemplateCategory.Reels, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF000000, 0xFF0E7490, 0xFFFE2C55), photos = 3, layoutIndex = 0,
            filter = "Vivid", filterIntensity = 30f,
            texts = listOf(txt("WATCH THIS", 0.5f, 0.06f, "Bold", WHITE, 1.2f), txt("new reel ▶", 0.5f, 0.945f, "Modern", WHITE, 1f)),
            decor = listOf(dec("👀", 0.86f, 0.14f, 1.3f, 10f), dec("🎬", 0.14f, 0.88f, 1.4f, -8f), dec("🔥", 0.86f, 0.88f, 1.4f)), accent = 0xFFFE2C55),
        template("reel_dayinlife", "A Day in My Life", TemplateCategory.Reels, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFFFFF7ED, 0xFFFED7AA, 0xFFFB923C), photos = 5, layoutIndex = 0,
            filter = "Rise", filterIntensity = 25f,
            texts = listOf(txt("A Day in My Life", 0.5f, 0.06f, "Script", 0xFF7C2D12, 1.1f), txt("morning → night", 0.5f, 0.945f, "Modern", 0xFF9A3412, 0.95f)),
            decor = listOf(dec("☀️", 0.86f, 0.14f, 1.1f), dec("🌙", 0.86f, 0.88f, 1.2f)), accent = 0xFFEA580C),
        template("reel_glowup", "Before & After", TemplateCategory.Reels, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF831843, 0xFFDB2777, 0xFFF9A8D4), photos = 2, layoutIndex = 0,
            filter = "Dreamy", filterIntensity = 25f,
            texts = listOf(txt("Before & After", 0.5f, 0.06f, "Bold", WHITE, 1.1f), txt("glow up ✨", 0.5f, 0.945f, "Script", WHITE, 1.1f)),
            decor = listOf(dec("💅", 0.14f, 0.88f, 1.3f, -8f), dec("💖", 0.86f, 0.88f, 1.3f, 10f))),
        template("reel_status", "Status Vibes", TemplateCategory.Reels, TemplateThumbStyle.STORY,
            9f, 16f, listOf(0xFF064E3B, 0xFF25D366, 0xFFA7F3D0), photos = 3, layoutIndex = 2,
            filter = "Sierra", filterIntensity = 25f,
            texts = listOf(txt("Status Vibes", 0.5f, 0.06f, "Script", WHITE, 1.25f)),
            decor = listOf(dec("💬", 0.86f, 0.14f, 1.2f, 10f), dec("🌟", 0.14f, 0.88f, 1.3f)))
    )

    // =========================================================================== custom user presets
    // Snapshot-state list: composables that read it (chips, carousels, pickers) update as soon as a preset is
    // saved or deleted. Persisted in the app's KeyValueStore as one text record per preset (see encode / decode).

    private val custom = mutableStateListOf<TemplateItem>()
    private var store: KeyValueStore? = null
    private const val K_CUSTOM = "custom_templates_v1"

    /** The user's saved presets, newest first. */
    val customTemplates: List<TemplateItem> get() = custom

    /** Loads saved presets once (safe to call from several screens, like AppSettings.attach). */
    fun attach(kv: KeyValueStore) {
        if (store === kv) return
        store = kv
        val loaded = kv.getString(K_CUSTOM, "").split('\n').mapNotNull { decode(it) }
        custom.clear()
        custom.addAll(loaded)
    }

    fun addCustom(item: TemplateItem) {
        custom.add(0, item)
        persist()
    }

    fun removeCustom(id: String) {
        custom.removeAll { it.id == id }
        persist()
    }

    fun isCustom(id: String): Boolean = custom.any { it.id == id }

    /** Built-in + custom templates (custom first). */
    fun allTemplates(): List<TemplateItem> = custom + all

    /** Factory used by the editor's "Save as Template" (all values normalised like the built-in presets). */
    fun newCustomTemplate(
        title: String,
        ratioW: Float, ratioH: Float,
        outer: Float, inner: Float, corner: Float,
        backgroundColor: Long, gradient: List<Long>,
        filter: String, filterIntensity: Float, adjust: Map<String, Float>,
        texts: List<ProjectTextLayer>, emojis: List<ProjectOverlayItem.Emoji>,
        photos: Int, layoutIndex: Int
    ): TemplateItem {
        val thumb = if (ratioW / ratioH < 0.7f) TemplateThumbStyle.STORY else TemplateThumbStyle.CLASSIC
        val colors = if (gradient.size >= 2) gradient else listOf(backgroundColor)
        return template(
            "custom_${kotlin.random.Random.nextLong().toULong()}", title.ifBlank { "My Preset" },
            TemplateCategory.Custom, thumb, ratioW, ratioH, colors,
            photos = photos.coerceAtLeast(1), layoutIndex = layoutIndex.coerceAtLeast(0),
            outer = outer, inner = inner, corner = corner,
            filter = filter, filterIntensity = filterIntensity, adjust = adjust,
            texts = texts, decor = emojis
        )
    }

    private fun persist() {
        store?.putString(K_CUSTOM, custom.joinToString("\n") { encode(it) })
    }

    // ---- compact text encoding (no serialization library needed) --------------------------------------
    // record  = fields joined by '|'
    // lists   = items joined by ';', item fields joined by ',', map entries "key:value"
    // strings are %-escaped so user text can contain any of these separators.

    private fun esc(s: String): String = buildString {
        s.forEach { c ->
            when (c) {
                '%' -> append("%25"); '|' -> append("%7C"); ';' -> append("%3B")
                ',' -> append("%2C"); ':' -> append("%3A"); '\n' -> append("%0A"); '\r' -> append("%0D")
                else -> append(c)
            }
        }
    }

    private fun unesc(s: String): String = buildString {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '%' && i + 2 < s.length) {
                append(s.substring(i + 1, i + 3).toInt(16).toChar()); i += 3
            } else { append(c); i++ }
        }
    }

    private fun encode(t: TemplateItem): String {
        val p = t.preset
        val texts = p.textLayers.joinToString(";") { l ->
            listOf(esc(l.text), l.x, l.y, esc(l.fontName), l.colorLong, l.scale, l.rotation).joinToString(",")
        }
        val emojis = p.overlays.filterIsInstance<ProjectOverlayItem.Emoji>().joinToString(";") { e ->
            listOf(esc(e.emoji), e.x, e.y, e.scale, e.rotation).joinToString(",")
        }
        return listOf(
            esc(t.id), esc(t.title), t.thumbStyle.name,
            p.aspectRatioWidth, p.aspectRatioHeight,
            p.outerSpacingRatio, p.innerSpacingRatio, p.cornerRadiusRatio,
            p.backgroundColorLong, p.backgroundGradientColors.joinToString(","),
            esc(p.filterPreset), p.filterIntensity,
            p.adjustValues.entries.joinToString(",") { "${esc(it.key)}:${it.value}" },
            t.preferredPhotos, t.layoutIndex, t.accentColorLong,
            texts, emojis
        ).joinToString("|")
    }

    private fun decode(line: String): TemplateItem? = runCatching {
        if (line.isBlank()) return null
        val f = line.split('|')
        if (f.size < 18) return null
        val gradient = f[9].split(',').filter { it.isNotBlank() }.map { it.toLong() }
        val adjust = f[12].split(',').filter { it.contains(':') }.associate { e ->
            val (k, v) = e.split(':'); unesc(k) to v.toFloat()
        }
        val texts = f[16].split(';').filter { it.isNotBlank() }.mapIndexed { i, rec ->
            val v = rec.split(',')
            ProjectTextLayer(
                id = "tpl_txt_custom_$i", text = unesc(v[0]), x = v[1].toFloat(), y = v[2].toFloat(),
                fontName = unesc(v[3]), colorLong = v[4].toLong(), scale = v[5].toFloat(), rotation = v[6].toFloat(),
                compact = true
            )
        }
        val emojis = f[17].split(';').filter { it.isNotBlank() }.mapIndexed { i, rec ->
            val v = rec.split(',')
            ProjectOverlayItem.Emoji(
                id = "tpl_dec_custom_$i", emoji = unesc(v[0]), x = v[1].toFloat(), y = v[2].toFloat(),
                scale = v[3].toFloat(), rotation = v[4].toFloat()
            )
        }
        TemplateItem(
            id = unesc(f[0]), title = unesc(f[1]), category = TemplateCategory.Custom,
            thumbStyle = TemplateThumbStyle.valueOf(f[2]),
            preset = CollageProject(
                aspectRatioWidth = f[3].toFloat(), aspectRatioHeight = f[4].toFloat(),
                outerSpacingRatio = f[5].toFloat(), innerSpacingRatio = f[6].toFloat(), cornerRadiusRatio = f[7].toFloat(),
                backgroundColorLong = f[8].toLong(), backgroundGradientColors = gradient,
                filterPreset = unesc(f[10]), filterIntensity = f[11].toFloat(), adjustValues = adjust,
                overlays = emojis, textLayers = texts
            ),
            preferredPhotos = f[13].toInt(), layoutIndex = f[14].toInt(), accentColorLong = f[15].toLong()
        )
    }.getOrNull()

    // =========================================================================== lookups

    fun byCategory(category: TemplateCategory): List<TemplateItem> = when (category) {
        TemplateCategory.All -> allTemplates()
        TemplateCategory.Custom -> custom.toList()
        else -> all.filter { it.category == category }
    }

    /** Category + photo-count filter (the two chip rows). */
    fun byCategoryAndPhotoCount(category: TemplateCategory, photoFilter: TemplatePhotoFilter): List<TemplateItem> =
        byCategory(category).filter { photoFilter.matches(it.preferredPhotos) }

    fun byId(id: String?): TemplateItem? =
        id?.let { key -> custom.firstOrNull { it.id == key } ?: all.firstOrNull { it.id == key } }
}