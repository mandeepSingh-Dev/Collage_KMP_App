package com.oem.collagekmpapp.data

import androidx.compose.runtime.Composable

// commonMain/kotlin/com/oem/collagekmpapp/data/CreationsRepository.kt

/**
 * What produced an exported image. There is no metadata store, so the kind is derived from the
 * file name. Make every exporter name its output with [newFileName] so the filter chips work.
 */
enum class CreationKind(val label: String, val filePrefix: String) {
    COLLAGE("Collage", "Collage"),
    FREESTYLE("Freestyle", "Freestyle"),
    EDITED("Edited", "Edited"),
    REMOVE_BG("Remove BG", "RemoveBG");

    /** e.g. `Freestyle_1764000000000.jpg` */
    fun newFileName(extension: String, timestampMillis: Long): String =
        "${filePrefix}_$timestampMillis.${extension.trimStart('.')}"

    companion object {
        fun fromFileName(name: String): CreationKind = when {
            name.startsWith("RemoveBG", ignoreCase = true) -> REMOVE_BG
            name.startsWith("Freestyle", ignoreCase = true) -> FREESTYLE
            name.startsWith("Edit", ignoreCase = true) -> EDITED
            else -> COLLAGE
        }
    }
}

/**
 * One exported image. [uri] is what Coil, the sharer and the editors all accept.
 * [projectJson] = contents of the matching project sidecar (`Collage_1764000000000.json` for
 * `Collage_1764000000000.png`) when the export saved one -> enables "Re-edit Project". See CreationProject.
 */
data class Creation(
    val id: String,
    val uri: String,
    val name: String,
    val dateMillis: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val kind: CreationKind,
    val projectJson: String? = null
)

/** Reads / deletes the images exported into the app's save folder (default `Pictures/CollageStudio`). */
expect class CreationsRepository {
    suspend fun load(): List<Creation>
    suspend fun delete(creation: Creation): Result<Unit>
}

@Composable
expect fun rememberCreationsRepository(folder: String): CreationsRepository