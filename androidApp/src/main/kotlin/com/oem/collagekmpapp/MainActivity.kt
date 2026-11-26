package com.oem.collagekmpapp

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.IntentCompat
import com.oem.collagekmpapp.model.SharedImagesPayload

class MainActivity : ComponentActivity() {

    /** Images shared from another app that are waiting for the user to pick a feature (null = nothing pending). */
    private var sharedImages by mutableStateOf<SharedImagesPayload?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            // fresh launch (cold start from the share sheet, or from the launcher -> no-op)
            handleShareIntent(intent)
        } else {
            // activity recreated (rotation / process death) while the picker was still open -> restore it
            savedInstanceState.getStringArrayList(KEY_PENDING_SHARE)?.takeIf { it.isNotEmpty() }?.let {
                sharedImages = SharedImagesPayload(id = System.nanoTime(), uris = it)
            }
        }

        setContent {
            App(
                sharedImages = sharedImages,
                onSharedImagesConsumed = { sharedImages = null }
            )
        }
    }

    /** App already running (launchMode="singleTask") and the user shares again from a gallery app. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        sharedImages?.let { outState.putStringArrayList(KEY_PENDING_SHARE, ArrayList(it.uris)) }
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        // Re-opening from Recents re-delivers the old SEND intent -> don't show the picker again.
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return

        val uris = extractSharedImageUris(intent)
        if (uris.isEmpty()) return

        uris.forEach { keepReadPermission(intent, it) }
        // new id every time -> sharing the same photo twice still re-opens the picker
        sharedImages = SharedImagesPayload(id = System.nanoTime(), uris = uris.map(Uri::toString))
    }

    private fun extractSharedImageUris(intent: Intent): List<Uri> {
        val streamUris: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            )
            Intent.ACTION_SEND_MULTIPLE ->
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            else -> return emptyList()
        }
        // some senders only fill ClipData (that's also where the system attaches the read grants)
        val clipUris = intent.clipData?.let { clip ->
            (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
        }.orEmpty()

        return streamUris.ifEmpty { clipUris }
            // content:// only -> a hostile app can't make us open file:// paths into our own private storage
            .filter { it.scheme == ContentResolver.SCHEME_CONTENT }
            // mixed shares arrive as "*/*" and still match image/* -> drop videos etc.
            .filter { uri ->
                val type = runCatching { contentResolver.getType(uri) }.getOrNull()
                type == null || type.startsWith("image/")
            }
            .distinct()
    }

    /**
     * The system already grants FLAG_GRANT_READ_URI_PERMISSION on the shared URIs to this activity for as
     * long as its task lives. If the sender also offered a persistable grant, take it so the URI keeps
     * working after the task is gone (e.g. when re-opening a saved creation later).
     */
    private fun keepReadPermission(intent: Intent, uri: Uri) {
        if (intent.flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION == 0) return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // provider doesn't support persistable grants -> the temporary activity grant still applies
        }
    }

    private companion object {
        const val KEY_PENDING_SHARE = "pending_shared_image_uris"
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}