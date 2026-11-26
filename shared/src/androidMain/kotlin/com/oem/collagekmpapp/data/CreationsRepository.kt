package com.oem.collagekmpapp.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class CreationsRepository(
    private val context: Context,
    private val folder: String
) {
    actual suspend fun load(): List<Creation> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Creation>()
        val folderName = folder.trim('/').substringAfterLast('/')

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATA
        )

        val resolver = context.contentResolver
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val selection: String
        val selectionArgs: Array<String>

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            selectionArgs = arrayOf("%$folderName%")
        } else {
            selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            selectionArgs = arrayOf("%$folderName%")
        }

        resolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                val name = cursor.getString(nameCol) ?: ""
                val dateSec = cursor.getLong(dateCol)
                val size = cursor.getLong(sizeCol)
                val width = cursor.getInt(widthCol)
                val height = cursor.getInt(heightCol)

                val projectJson = runCatching {
                    val sidecar = projectSidecarFile(context, name)
                    if (sidecar.exists()) sidecar.readText(Charsets.UTF_8) else null
                }.getOrNull()

                list.add(
                    Creation(
                        id = contentUri.toString(),
                        uri = contentUri.toString(),
                        name = name,
                        dateMillis = dateSec * 1000L,
                        sizeBytes = size,
                        width = width,
                        height = height,
                        kind = CreationKind.fromFileName(name),
                        projectJson = projectJson
                    )
                )
            }
        }
        list
    }

    actual suspend fun delete(creation: Creation): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(creation.uri)
            val deleted = context.contentResolver.delete(uri, null, null)
            if (deleted > 0) {
                projectSidecarFile(context, creation.name).delete()
                Unit
            } else {
                error("Could not delete creation: ${creation.name}")
            }
        }
    }
}

@Composable
actual fun rememberCreationsRepository(folder: String): CreationsRepository {
    val context = LocalContext.current.applicationContext
    return remember(context, folder) {
        CreationsRepository(context, folder)
    }
}
