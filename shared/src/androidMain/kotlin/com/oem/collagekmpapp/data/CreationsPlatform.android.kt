package com.oem.collagekmpapp.data

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.TimeZone

// androidMain/kotlin/com/oem/collagekmpapp/data/CreationsPlatform.android.kt

// ---------------------------------------------------------------- project sidecars
// Stored app-private (filesDir/projects/<image base name>.json): on Android 10+ MediaStore refuses non-image
// files inside Pictures/, and private storage needs no permission and survives the gallery's media rescans.

internal fun projectSidecarDir(context: Context): File = File(context.filesDir, "projects")

internal fun projectSidecarFile(context: Context, imageFileName: String): File =
    File(projectSidecarDir(context), projectSidecarName(imageFileName))

@Composable
actual fun rememberProjectSidecarStore(): ProjectSidecarStore {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        object : ProjectSidecarStore {
            override suspend fun save(imageFileName: String, json: String): Result<Unit> = withContext(Dispatchers.IO) {
                runCatching {
                    val target = projectSidecarFile(appContext, imageFileName)
                    target.parentFile?.mkdirs()
                    // write to a temp file first so a crash never leaves half a project behind
                    val tmp = File(target.parentFile, target.name + ".tmp")
                    tmp.writeText(json, Charsets.UTF_8)
                    if (!tmp.renameTo(target)) {
                        target.delete()
                        check(tmp.renameTo(target)) { "Couldn't save the project file." }
                    }
                }
            }

            override suspend fun delete(imageFileName: String) {
                withContext(Dispatchers.IO) { runCatching { projectSidecarFile(appContext, imageFileName).delete() } }
            }
        }
    }
}

// ---------------------------------------------------------------- batch share

@Composable
actual fun rememberMultiImageSharer(): MultiImageSharer {
    val context = LocalContext.current
    return remember(context) {
        MultiImageSharer { uris ->
            if (uris.isEmpty()) return@MultiImageSharer
            val list = ArrayList(uris.map(Uri::parse))
            val send = if (list.size == 1) {
                Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, list[0])
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, list)
            }
            send.type = "image/*"
            // ClipData carries the read grant for EVERY uri (EXTRA_STREAM alone only grants reliably for one)
            send.clipData = ClipData.newRawUri(null, list[0]).apply {
                for (i in 1 until list.size) addItem(ClipData.Item(list[i]))
            }
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val chooser = Intent.createChooser(send, if (list.size == 1) "Share creation" else "Share ${list.size} creations")
            if (context.findActivity() == null) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ---------------------------------------------------------------- clock

actual fun platformNowMillis(): Long = System.currentTimeMillis()

actual fun platformUtcOffsetMillis(nowMillis: Long): Long = TimeZone.getDefault().getOffset(nowMillis).toLong()