package com.oem.collagekmpapp.model

/**
 * Images handed to the app from outside (Android share sheet). Platform-neutral so it can live in commonMain.
 *
 * @param id   unique per share event, so sharing the same photo twice still re-opens the feature picker
 * @param uris content URIs as Strings (same format the in-app photo picker passes to the editors)
 */
data class SharedImagesPayload(
    val id: Long,
    val uris: List<String>
)

/** Features the user can open shared image(s) with. */
enum class ShareFeature(
    val emoji: String,
    val title: String,
    val description: String
) {
    EDIT_PHOTO("📸", "Single Photo Editing", "Edit and enhance this photo"),
    REMOVE_BACKGROUND("✂️", "Remove Background", "Cut the subject out of the photo"),
    FREESTYLE_COLLAGE("🎨", "Freestyle Collage", "Arrange photos freely on a canvas"),
    GRID_COLLAGE("🖼️", "Grid Collage", "Place photos into a grid layout");

    fun toScreen(uris: List<String>): Screen = when (this) {
        EDIT_PHOTO -> Screen.EditPhoto(uris.first())
        REMOVE_BACKGROUND -> Screen.RemoveBackground(uris.first())
        FREESTYLE_COLLAGE -> Screen.FreestyleEditor(uris)
        GRID_COLLAGE -> Screen.LayoutSelection(uris)
    }

    companion object {
        /** 1 image -> all four features; several images -> only the collage features. */
        fun availableFor(count: Int): List<ShareFeature> =
            if (count <= 1) listOf(EDIT_PHOTO, REMOVE_BACKGROUND, FREESTYLE_COLLAGE, GRID_COLLAGE)
            else listOf(GRID_COLLAGE, FREESTYLE_COLLAGE)
    }
}