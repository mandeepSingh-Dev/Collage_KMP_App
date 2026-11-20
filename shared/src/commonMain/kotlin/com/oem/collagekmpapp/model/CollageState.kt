package com.oem.collagekmpapp.model

data class CollagePhoto(
    val id: String,
    val uri: String,
    val filter: PhotoFilter = PhotoFilter.NORMAL,
    val rotation: Float = 0f,
    val scale: Float = 1f
)

enum class PhotoFilter(val displayName: String) {
    NORMAL("Normal"),
    VINTAGE("Vintage"),
    BW("B&W"),
    WARM("Warm"),
    COOL("Cool"),
    CINEMATIC("Cinematic"),
    RETRO("Retro")
}

enum class GlitterEffect(val displayName: String) {
    NONE("None"),
    SPARKLE("Sparkle Stars"),
    DIAMOND("Diamonds"),
    BOKEH("Bokeh Glow"),
    HEARTS("Love Hearts"),
    MAGIC_DUST("Magic Dust")
}

enum class AspectRatio(val displayName: String, val widthRatio: Float, val heightRatio: Float) {
    SQUARE("1:1", 1f, 1f),
    PORTRAIT("4:5", 4f, 5f),
    STORY("9:16", 9f, 16f),
    LANDSCAPE("16:9", 16f, 9f),
    WIDE("3:4", 3f, 4f)
}

data class BorderSettings(
    val outerSpacing: Float = 12f,
    val cornerRadius: Float = 22f,
    val spacing: Float = 8f
)

data class TextSticker(
    val id: String,
    val text: String,
    val x: Float = 100f,
    val y: Float = 100f,
    val fontSize: Float = 24f,
    val color: String = "#FFFFFF"
)

data class LayoutTemplate(
    val id: String,
    val name: String,
    val slotCount: Int,
    val description: String
)

sealed class Screen {
    object Home : Screen()
    data class LayoutSelection(val selectedPhotos: List<String>) : Screen()
    data class Editor(val layoutId: String, val selectedPhotos: List<String>) : Screen()
}
