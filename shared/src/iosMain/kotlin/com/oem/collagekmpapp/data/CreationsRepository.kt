package com.oem.collagekmpapp.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970

@OptIn(ExperimentalForeignApi::class)
actual class CreationsRepository(folder: String) {

    private val dirName = folder.trim('/').substringAfterLast('/')
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "heic")

    private fun directoryPath(): String? {
        val docs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String ?: return null
        return "$docs/$dirName"
    }

    actual suspend fun load(): List<Creation> = withContext(Dispatchers.Default) {
        val dir = directoryPath() ?: return@withContext emptyList()
        val fm = NSFileManager.defaultManager
        val names = (fm.contentsOfDirectoryAtPath(dir, error = null) as? List<*>)
            .orEmpty().filterIsInstance<String>()

        names
            .filter { it.substringAfterLast('.', "").lowercase() in imageExtensions }
            .mapNotNull { name ->
                val path = "$dir/$name"
                val attrs = fm.attributesOfItemAtPath(path, error = null) ?: return@mapNotNull null
                val modified = ((attrs[NSFileModificationDate] as? NSDate)?.timeIntervalSince1970 ?: 0.0) * 1000.0
                val size = (attrs[NSFileSize] as? NSNumber)?.longLongValue ?: 0L
                Creation(
                    id = path,
                    uri = "file://$path",
                    name = name,
                    dateMillis = modified.toLong(),
                    sizeBytes = size,
                    width = 0,
                    height = 0,
                    kind = CreationKind.fromFileName(name)
                )
            }
    }

    actual suspend fun delete(creation: Creation): Result<Unit> = withContext(Dispatchers.Default) {
        val ok = NSFileManager.defaultManager.removeItemAtPath(creation.id, error = null)
        if (ok) Result.success(Unit) else Result.failure(IllegalStateException("Couldn't delete this image."))
    }
}

@Composable
actual fun rememberCreationsRepository(folder: String): CreationsRepository =
    remember(folder) { CreationsRepository(folder) }
