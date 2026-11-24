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

enum class AspectRatio(val displayName: String, val widthRatio: Float, val heightRatio: Float, val badge: String = "") {
    SQUARE("1:1", 1f, 1f, "Instagram"),
    PORTRAIT("4:5", 4f, 5f, "Instagram"),
    STORY("9:16", 9f, 16f, "Pinterest"),
    LANDSCAPE("16:9", 16f, 9f, "YouTube"),
    WIDE("3:4", 3f, 4f, ""),
    R_5_4("5:4", 5f, 4f, "Box"),
    R_3_4("3:4", 3f, 4f, ""),
    R_4_3("4:3", 4f, 3f, ""),
    R_2_3("2:3", 2f, 3f, "Pinterest"),
    R_3_2("3:2", 3f, 2f, ""),
    R_1_2("1:2", 1f, 2f, "")
}

data class BorderSettings(
    val outerSpacing: Float = 5f,
    val cornerRadius: Float = 15f,
    val spacing: Float = 5f
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
    object HomeContainer : Screen()
    object Home : Screen()
    object Settings : Screen()
    data class SavePath(val selectedPath: String) : Screen()
    object Feedback : Screen()
    object Creations : Screen()
    data class LayoutSelection(val selectedPhotos: List<String>) : Screen()
    data class Editor(val layoutId: String, val selectedPhotos: List<String>) : Screen()
    data class FreestyleEditor(val selectedPhotos: List<String>) : Screen()
    data class EditPhoto(val photoUri: String) : Screen()
    data class RemoveBackground(val photoUri: String) : Screen()
}