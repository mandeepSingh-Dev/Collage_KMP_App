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
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.oem.collagekmpapp.model.SharedImagesPayload

class MainActivity : ComponentActivity() {

    /** Images shared from another app that are waiting for the user to pick a feature (null = nothing pending). */
    private var sharedImages by mutableStateOf<SharedImagesPayload?>(null)

    /** Shortcut action triggered via long-pressing the launcher icon (null = nothing pending). */
    private var pendingShortcutAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        publishDynamicShortcuts()
        com.oem.collagekmpapp.firebase.DeviceTracker.trackDeviceDetails(this)

        if (savedInstanceState == null) {
            // fresh launch (cold start from the share sheet, shortcut, or launcher)
            handleShareIntent(intent)
            pendingShortcutAction = extractShortcutAction(intent)
        } else {
            // activity recreated (rotation / process death) -> restore states
            savedInstanceState.getStringArrayList(KEY_PENDING_SHARE)?.takeIf { it.isNotEmpty() }?.let {
                sharedImages = SharedImagesPayload(id = System.nanoTime(), uris = it)
            }
            pendingShortcutAction = savedInstanceState.getString(KEY_PENDING_SHORTCUT_ACTION)
        }

        setContent {
            App(
                sharedImages = sharedImages,
                onSharedImagesConsumed = { sharedImages = null },
                shortcutAction = pendingShortcutAction,
                onShortcutActionConsumed = { pendingShortcutAction = null }
            )
        }
    }

    /** App already running (launchMode="singleTask") and the user triggers a share or shortcut again. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
        extractShortcutAction(intent)?.let { action ->
            pendingShortcutAction = action
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        sharedImages?.let { outState.putStringArrayList(KEY_PENDING_SHARE, ArrayList(it.uris)) }
        pendingShortcutAction?.let { outState.putString(KEY_PENDING_SHORTCUT_ACTION, it) }
    }

    private fun publishDynamicShortcuts() {
        try {
            val gridShortcut = ShortcutInfoCompat.Builder(this, "shortcut_grid_collage_dynamic")
                .setShortLabel(getString(R.string.shortcut_grid_collage_short))
                .setLongLabel(getString(R.string.shortcut_grid_collage_long))
                .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_grid))
                .setIntent(
                    Intent(Intent.ACTION_VIEW, null, this, MainActivity::class.java).apply {
                        putExtra("shortcut_action", "grid_collage")
                    }
                )
                .build()

            val freestyleShortcut = ShortcutInfoCompat.Builder(this, "shortcut_freestyle_dynamic")
                .setShortLabel(getString(R.string.shortcut_freestyle_short))
                .setLongLabel(getString(R.string.shortcut_freestyle_long))
                .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_brush))
                .setIntent(
                    Intent(Intent.ACTION_VIEW, null, this, MainActivity::class.java).apply {
                        putExtra("shortcut_action", "freestyle")
                    }
                )
                .build()

            val draftShortcut = ShortcutInfoCompat.Builder(this, "shortcut_resume_draft_dynamic")
                .setShortLabel(getString(R.string.shortcut_resume_draft_short))
                .setLongLabel(getString(R.string.shortcut_resume_draft_long))
                .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_folder))
                .setIntent(
                    Intent(Intent.ACTION_VIEW, null, this, MainActivity::class.java).apply {
                        putExtra("shortcut_action", "resume_draft")
                    }
                )
                .build()

            ShortcutManagerCompat.setDynamicShortcuts(this, listOf(gridShortcut, freestyleShortcut, draftShortcut))
        } catch (_: Exception) {
            // Ignore if shortcuts API is unavailable
        }
    }

    private fun extractShortcutAction(intent: Intent?): String? {
        if (intent == null) return null
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
        return intent.getStringExtra("shortcut_action")
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
        const val KEY_PENDING_SHORTCUT_ACTION = "pending_shortcut_action"
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}