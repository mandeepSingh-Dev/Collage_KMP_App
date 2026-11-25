package com.oem.collagekmpapp

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * Snapshot based undo / redo shared by all editors.
 *
 * Each editor describes its complete editable state as ONE immutable value [S] (a data class of lists,
 * maps and primitives). Whenever that value changes the previous value is pushed on the undo stack
 * (and the redo stack is cleared). Undo / redo pop a snapshot, push the current one on the opposite
 * stack and hand the snapshot to the editor's `restore` callback.
 *
 * Continuous gestures (slider drags, moving / pinching items) produce a new value every frame; they are
 * coalesced with [debounceMs] so one gesture = one undo step.
 */
class EditHistory<S> internal constructor(private val limit: Int = 100) {
    private val undoStack = ArrayList<S>()
    private val redoStack = ArrayList<S>()
    private var undoSize by mutableIntStateOf(0)
    private var redoSize by mutableIntStateOf(0)

    internal var latest: S? = null            // value of the state in the latest composition
    internal var committed: S? = null         // last value that the stacks know about
    internal var restoring = false            // a restore was issued and the UI has not caught up yet
    internal var restoreFn: (S) -> Unit = {}

    private fun hasPending() = !restoring && committed != null && latest != null && committed != latest

    val canUndo: Boolean get() = undoSize > 0 || hasPending()
    val canRedo: Boolean get() = redoSize > 0 && !hasPending()

    private fun sync() { undoSize = undoStack.size; redoSize = redoStack.size }

    /** Records a pending change (if any) as an undo step. */
    internal fun flush() {
        if (restoring) return
        val prev = committed
        val now = latest
        if (prev != null && now != null && prev != now) {
            undoStack.add(prev)
            if (undoStack.size > limit) undoStack.removeAt(0)
            redoStack.clear()
            committed = now
            sync()
        } else if (prev == null) {
            committed = now
        }
    }

    fun undo() {
        flush()
        if (undoStack.isEmpty()) return
        val current = committed ?: latest ?: return
        val target = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(current)
        committed = target
        restoring = true
        sync()
        restoreFn(target)
    }

    fun redo() {
        flush()
        if (redoStack.isEmpty()) return
        val current = committed ?: latest ?: return
        val target = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(current)
        committed = target
        restoring = true
        sync()
        restoreFn(target)
    }

    /** Forget everything (e.g. after loading a different photo). */
    fun clear() {
        undoStack.clear(); redoStack.clear(); committed = latest; restoring = false; sync()
    }
}

/**
 * @param current  the editor's complete editable state, rebuilt on every composition
 * @param restore  applies a snapshot back onto the editor's state variables
 */
@Composable
fun <S : Any> rememberEditHistory(
    current: S,
    debounceMs: Long = 250L,
    restore: (S) -> Unit
): EditHistory<S> {
    val history = remember { EditHistory<S>() }
    history.latest = current
    history.restoreFn = restore
    if (history.committed == null) history.committed = current
    if (history.restoring && history.committed == current) history.restoring = false

    LaunchedEffect(current) {
        if (debounceMs > 0) delay(debounceMs)
        if (history.restoring) {
            // the UI did not converge on the restored snapshot (e.g. a derived value) -> accept what is shown
            history.restoring = false
            history.committed = current
        } else {
            history.flush()
        }
    }
    return history
}

/** Undo + Redo icon buttons for a top app bar `actions` slot. */
@Composable
fun UndoRedoActions(history: EditHistory<*>) {
    IconButton(onClick = { history.undo() }, enabled = history.canUndo) {
        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
    }
    IconButton(onClick = { history.redo() }, enabled = history.canRedo) {
        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
    }
}