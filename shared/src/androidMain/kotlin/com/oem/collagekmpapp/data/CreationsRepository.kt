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

actual class CreationsRepository(private val context: Context, folder: String) {

    private val folder = folder.trim('/')

    private val collection: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    @Suppress("DEPRECATION")
    actual suspend fun load(): List<Creation> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        val selection: String
        val args: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            args = arrayOf("$folder/%")
        } else {
            selection = "${MediaStore.Images.Media.DATA} LIKE ?"
            args = arrayOf("%/$folder/%")
        }

        val result = mutableListOf<Creation>()
        context.contentResolver.query(
            collection, projection, selection, args,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val wCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val hCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol) ?: "creation_$id"
                result += Creation(
                    id = id.toString(),
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    name = name,
                    dateMillis = c.getLong(dateCol) * 1000L,
                    sizeBytes = c.getLong(sizeCol),
                    width = c.getInt(wCol),
                    height = c.getInt(hCol),
                    kind = CreationKind.fromFileName(name)
                )
            }
        }
        result
    }

    actual suspend fun delete(creation: Creation): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val rows = context.contentResolver.delete(Uri.parse(creation.uri), null, null)
            if (rows > 0) Result.success(Unit)
            else Result.failure(IllegalStateException("This image was already removed."))
        } catch (e: SecurityException) {
            Result.failure(IllegalStateException("Couldn't delete this image. Remove it from your Gallery app instead."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Composable
actual fun rememberCreationsRepository(folder: String): CreationsRepository {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext, folder) { CreationsRepository(appContext, folder) }
}
