package com.oem.collagekmpapp.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// commonMain – shared pieces of the "unsaved changes" guardrail used by all 4 editor screens.

/**
 * true while the screen is the top of the back stack (not the one animating OUT).
 * App.kt provides it per screen inside AnimatedContent; editors use it to enable their BackHandler,
 * so a screen that's already sliding away can't swallow the next back press.
 * Defaults to true, so the editors behave exactly as before when hosted somewhere else.
 */
val LocalIsTopScreen = compositionLocalOf { true }

/** "Discard unsaved changes?" confirmation. Cancel keeps the user in the editor, Discard exits. */
@Composable
fun DiscardChangesDialog(
    onCancel: () -> Unit,
    onDiscard: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(24.dp),
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B)) },
        title = { Text("Discard unsaved changes?", fontWeight = FontWeight.Bold) },
        text = { Text("You have unsaved edits. If you go back now, your changes will be lost.") },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel", color = Color(0xFF2563EB)) }
        }
    )
}