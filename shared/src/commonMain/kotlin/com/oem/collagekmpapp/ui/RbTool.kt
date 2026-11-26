package com.oem.collagekmpapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.oem.collagekmpapp.BackHandler
import com.oem.collagekmpapp.BgMaskStroke
import com.oem.collagekmpapp.MASK_ANALYSIS_SIDE
import com.oem.collagekmpapp.MaskPixels
import com.oem.collagekmpapp.magicWandRects
import com.oem.collagekmpapp.rememberPhotoPixelLoader
import com.oem.collagekmpapp.USE_LEGACY_EXPORT_DIALOG
import com.oem.collagekmpapp.UndoRedoActions
import com.oem.collagekmpapp.UnifiedExportScreen
import com.oem.collagekmpapp.components.FluidSlider
import com.oem.collagekmpapp.rememberBackgroundRemovalExporter
import com.oem.collagekmpapp.rememberEditHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** ERASER / BRUSH paint the mask; WAND = tap a colour to erase / restore the matching contiguous area. */
private enum class RbTool { ERASER, BRUSH, WAND }

private const val RB_MAX_ZOOM = 5f

private enum class RbPreviewBg(val label: String) {
    CHECKER("Checker"),
    DOTS("Dots"),
    BLACK("Black"),
    WHITE("White")
}

/** Brush diameter = slider value / BRUSH_DIVISOR of the image width (resolution independent, same in export). */
private const val RB_BRUSH_DIVISOR = 500f

// =================================================================== drawing helpers

private fun DrawScope.rbDrawPreviewBackground(bg: RbPreviewBg) {
    when (bg) {
        RbPreviewBg.CHECKER -> {
            drawRect(Color.White)
            val s = 12.dp.toPx()
            var row = 0
            var y = 0f
            while (y < size.height) {
                var col = 0
                var x = 0f
                while (x < size.width) {
                    if ((row + col) % 2 == 1) drawRect(Color(0xFFD1D5DB), Offset(x, y), Size(s, s))
                    x += s
                    col++
                }
                y += s
                row++
            }
        }
        RbPreviewBg.DOTS -> {
            drawRect(Color(0xFFF8FAFC))
            val step = 14.dp.toPx()
            val r = 1.6.dp.toPx()
            var y = step / 2f
            while (y < size.height) {
                var x = step / 2f
                while (x < size.width) {
                    drawCircle(Color(0xFFCBD5E1), r, Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        RbPreviewBg.BLACK -> drawRect(Color.Black)
        RbPreviewBg.WHITE -> drawRect(Color.White)
    }
}

/**
 * Paints one stroke into the alpha mask.
 * Erase = BlendMode.Clear (alpha 0), restore = BlendMode.Src with opaque white (alpha 1).
 */
private fun DrawScope.rbDrawMaskStroke(s: BgMaskStroke) {
    val blend = if (s.restore) BlendMode.Src else BlendMode.Clear
    if (s.fill) {
        drawRect(Color.White, blendMode = blend)
        return
    }
    // Magic Wand / AI Auto-Cutout region (normalized rects), drawn as ONE path – same as the exporter
    s.rects?.let { r ->
        val region = Path()
        var i = 0
        while (i + 3 < r.size) {
            region.addRect(
                Rect(r[i] * size.width, r[i + 1] * size.height, r[i + 2] * size.width, r[i + 3] * size.height)
            )
            i += 4
        }
        drawPath(region, Color.White, blendMode = blend)
        return
    }
    if (s.points.isEmpty()) return
    val widthPx = s.widthRatio * size.width
    if (s.points.size == 1) {
        val p = s.points[0]
        drawCircle(Color.White, widthPx / 2f, Offset(p.x * size.width, p.y * size.height), blendMode = blend)
        return
    }
    val path = Path().apply {
        moveTo(s.points[0].x * size.width, s.points[0].y * size.height)
        for (i in 1 until s.points.size) lineTo(s.points[i].x * size.width, s.points[i].y * size.height)
    }
    drawPath(
        path, Color.White,
        style = Stroke(widthPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = blend
    )
}

@Composable
private fun RbChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF0F172A) else Color(0xFFF1F5F9))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color(0xFF334155)
        )
    }
}

// =================================================================== the screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveBackgroundScreen(
    photoUri: String,
    onBack: () -> Unit
) {
    val exporter = rememberBackgroundRemovalExporter()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var tool by remember { mutableStateOf(RbTool.ERASER) }
    var brushSize by remember { mutableStateOf(40f) }      // 8..120
    var brushOffset by remember { mutableStateOf(40f) }    // dp the brush sits ABOVE the finger, 0..120
    var previewBg by remember { mutableStateOf(RbPreviewBg.CHECKER) }

    var strokes by remember { mutableStateOf<List<BgMaskStroke>>(emptyList()) }
    var redoStack by remember { mutableStateOf<List<BgMaskStroke>>(emptyList()) }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }   // normalized points
    var cursor by remember { mutableStateOf<Offset?>(null) }                // px inside the image box
    var aspect by remember { mutableStateOf(1f) }
    var isSaving by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }

    // ---- canvas zoom / pan (two fingers). Only the VIEW changes; strokes stay normalized to the image.
    var zoom by remember { mutableStateOf(1f) }             // 1..RB_MAX_ZOOM
    var pan by remember { mutableStateOf(Offset.Zero) }     // px, applied after scaling around the centre

    // ---- Magic Wand + AI Auto-Cutout
    var wandTolerance by remember { mutableStateOf(30f) }   // % 10..50
    var wandRestore by remember { mutableStateOf(false) }   // false = erase the tapped area, true = restore it
    var analyzing by remember { mutableStateOf(false) }
    var pixels by remember(photoUri) { mutableStateOf<MaskPixels?>(null) }   // cached analysis copy
    val pixelLoader = rememberPhotoPixelLoader()

    suspend fun ensurePixels(): MaskPixels? =
        pixels ?: pixelLoader.load(photoUri, MASK_ANALYSIS_SIDE)?.also { pixels = it }

    /** Adds a region as ONE stroke -> one Undo step, rendered by preview and exporter alike. */
    fun addRegion(rects: FloatArray, restore: Boolean) {
        strokes = strokes + BgMaskStroke(emptyList(), 0f, restore = restore, rects = rects)
        redoStack = emptyList()
    }

    fun runMagicWand(nx: Float, ny: Float) {
        if (analyzing) return
        val restore = wandRestore
        val tol = wandTolerance / 100f
        scope.launch {
            analyzing = true
            val px = ensurePixels()
            val rects = px?.let { withContext(Dispatchers.Default) { magicWandRects(it, nx, ny, tol) } }
            analyzing = false
            if (rects != null) addRegion(rects, restore) else snackbar.showSnackbar("Couldn't read the photo")
        }
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            redoStack = redoStack + strokes.last()
            strokes = strokes.dropLast(1)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            strokes = strokes + redoStack.last()
            redoStack = redoStack.dropLast(1)
        }
    }

    fun addFill(restore: Boolean) {
        strokes = strokes + BgMaskStroke(emptyList(), 0f, restore = restore, fill = true)
        redoStack = emptyList()
    }

    // ---------------------------------------------------------------- Undo / Redo
    // History of the complete mask-stroke list: covers eraser strokes, brush (restore) strokes,
    // "Erase all", "Restore all" and "Reset", each as its own step.
    val history = rememberEditHistory(current = strokes, debounceMs = 0L) { snap ->
        strokes = snap
        redoStack = emptyList()
        current = emptyList()
        cursor = null
    }

    // ---------------------------------------------------------------- Unsaved-changes guardrail
    // Top-bar arrow AND system back/gesture: exit directly when nothing is undoable, otherwise confirm first.
    var showDiscardDialog by remember { mutableStateOf(false) }
    val requestBack: () -> Unit = {
        if (history.canUndo) showDiscardDialog = true else onBack()
    }
    BackHandler(enabled = LocalIsTopScreen.current) { requestBack() }
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onCancel = { showDiscardDialog = false },
            onDiscard = { showDiscardDialog = false; onBack() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Background Eraser", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = requestBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    UndoRedoActions(history)
                    // (legacy per-screen undo()/redo() above are superseded by the shared history buttons)
                    Button(
                        enabled = !isSaving,
                        onClick = {
                            if (!USE_LEGACY_EXPORT_DIALOG) {
                                showExport = true      // -> unified Export & Share screen
                            } else {
                                isSaving = true
                                exporter.export(photoUri, strokes) { result ->
                                    isSaving = false
                                    scope.launch {
                                        snackbar.showSnackbar(
                                            result.fold(
                                                onSuccess = { "Saved PNG to $it" },
                                                onFailure = { "Export failed: ${it.message ?: "unknown error"}" }
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text(if (isSaving) "Saving…" else "Save") }
                }
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .navigationBarsPadding()   // keep the controls above the system navigation bar
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // universal Reset: strokes, brush size / offset, preview background and tool back to their defaults
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Eraser tools", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    RbResetButton(enabled = strokes.isNotEmpty() || brushSize != 40f || brushOffset != 40f || previewBg != RbPreviewBg.CHECKER || tool != RbTool.ERASER || zoom > 1.05f || wandTolerance != 30f || wandRestore) {
                        strokes = emptyList(); redoStack = emptyList(); current = emptyList(); cursor = null
                        brushSize = 40f; brushOffset = 40f; previewBg = RbPreviewBg.CHECKER; tool = RbTool.ERASER
                        zoom = 1f; pan = Offset.Zero; wandTolerance = 30f; wandRestore = false
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    RbChip("Eraser", tool == RbTool.ERASER) { tool = RbTool.ERASER }
                    RbChip("Brush (restore)", tool == RbTool.BRUSH) { tool = RbTool.BRUSH }
                    RbChip("🪄 Magic Wand", tool == RbTool.WAND) { tool = RbTool.WAND }
                }
                if (tool == RbTool.WAND) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RbChip("Erase area", !wandRestore) { wandRestore = false }
                        RbChip("Restore area", wandRestore) { wandRestore = true }
                    }
                    Column(Modifier.fillMaxWidth()) {
                        Text("Wand tolerance: ${wandTolerance.toInt()}%", fontSize = 11.sp, color = Color(0xFF334155))
                        FluidSlider(
                            value = wandTolerance, onValueChange = { wandTolerance = it },
                            valueRange = 10f..50f, modifier = Modifier.fillMaxWidth().height(36.dp)
                        )
                        Text("Tap a colour on the photo to select its connected area.", fontSize = 10.sp, color = Color(0xFF64748B))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RbChip("Erase all", false) { addFill(restore = false) }
                    RbChip("Restore all", false) { addFill(restore = true) }
                    RbChip("Reset", false) { strokes = emptyList(); redoStack = emptyList() }
                }

                Column(Modifier.fillMaxWidth()) {
                    Text("Brush size: ${brushSize.toInt()}", fontSize = 11.sp, color = Color(0xFF334155))
                    FluidSlider(
                        value = brushSize, onValueChange = { brushSize = it },
                        valueRange = 8f..120f, modifier = Modifier.fillMaxWidth().height(36.dp)
                    )
                }
                Column(Modifier.fillMaxWidth()) {
                    Text("Brush offset: ${brushOffset.toInt()} (distance above your finger)", fontSize = 11.sp, color = Color(0xFF334155))
                    FluidSlider(
                        value = brushOffset, onValueChange = { brushOffset = it },
                        valueRange = 0f..120f, modifier = Modifier.fillMaxWidth().height(36.dp)
                    )
                }

                Text("Preview background", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RbPreviewBg.entries.forEach { b -> RbChip(b.label, previewBg == b) { previewBg = b } }
                }
            }
        }
    ) { inner ->
        BoxWithConstraints(
            Modifier.fillMaxSize().background(Color(0xFFE2E8F0)).padding(inner).padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            val cw = minOf(maxWidth, maxHeight * aspect)
            val ch = cw / aspect

            Box(
                Modifier
                    .size(cw, ch)
                    .clip(RoundedCornerShape(6.dp))
                    // two-finger pinch-to-zoom (1x..5x) + pan. One finger is left to the brush / wand below.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                if (event.changes.count { it.pressed } >= 2) {
                                    val zf = event.calculateZoom()
                                    val pd = event.calculatePan()
                                    val c = event.calculateCentroid(useCurrent = true)
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val nz = (zoom * zf).coerceIn(1f, RB_MAX_ZOOM)
                                    val k = nz / zoom
                                    // keep the content under the fingers' centroid in place while scaling
                                    var np = (c - center) * (1f - k) + pan * k + pd
                                    val mx = (nz - 1f) * size.width / 2f
                                    val my = (nz - 1f) * size.height / 2f
                                    np = Offset(np.x.coerceIn(-mx, mx), np.y.coerceIn(-my, my))
                                    zoom = nz
                                    pan = if (nz <= 1f) Offset.Zero else np
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                // everything inside scales / pans together; pointer positions arrive in un-zoomed image space
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom; scaleY = zoom
                            translationX = pan.x; translationY = pan.y
                        }
                ) {
                    // 1) transparency indicator
                    Canvas(Modifier.fillMaxSize()) { rbDrawPreviewBackground(previewBg) }

                    // 2) photo, multiplied by the alpha mask (all inside one offscreen layer)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawIntoCanvas { canvas ->
                                    val maskPaint = Paint().apply { blendMode = BlendMode.DstIn }
                                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), maskPaint)
                                    drawRect(Color.White)
                                    strokes.forEach { rbDrawMaskStroke(it) }
                                    if (current.isNotEmpty()) {
                                        rbDrawMaskStroke(
                                            BgMaskStroke(current, brushSize / RB_BRUSH_DIVISOR, restore = tool == RbTool.BRUSH)
                                        )
                                    }
                                    canvas.restore()
                                }
                            }
                            .pointerInput(tool, brushSize, brushOffset) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()

                                    // ---- Magic Wand: a clean single-finger tap samples the colour there
                                    if (tool == RbTool.WAND) {
                                        val slop = viewConfiguration.touchSlop / zoom
                                        var valid = true
                                        var up = false
                                        while (!up) {
                                            val ev = awaitPointerEvent()
                                            if (ev.changes.count { it.pressed } >= 2) valid = false
                                            val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                                            if ((c.position - down.position).getDistance() > slop) valid = false
                                            if (!c.pressed) up = true
                                        }
                                        if (valid && up) runMagicWand(down.position.x / w, down.position.y / h)
                                        return@awaitEachGesture
                                    }

                                    // offset is in screen dp: divide by zoom so it stays the same distance on screen
                                    val offPx = brushOffset.dp.toPx() / zoom
                                    val ratio = brushSize / RB_BRUSH_DIVISOR
                                    fun brushPx(p: Offset) = Offset(p.x, p.y - offPx)
                                    fun norm(p: Offset) = brushPx(p).let { Offset(it.x / w, it.y / h) }

                                    current = listOf(norm(down.position))
                                    cursor = brushPx(down.position)
                                    down.consume()

                                    var up = false
                                    var aborted = false
                                    while (!up) {
                                        val ev = awaitPointerEvent()
                                        if (ev.changes.count { it.pressed } >= 2) {
                                            // second finger -> this is a pinch, not a stroke: discard and wait for release
                                            aborted = true
                                            current = emptyList()
                                            cursor = null
                                            do {
                                                val e2 = awaitPointerEvent()
                                            } while (e2.changes.any { it.pressed })
                                            break
                                        }
                                        val c = ev.changes.firstOrNull() ?: break
                                        if (c.pressed) {
                                            current = current + norm(c.position)
                                            cursor = brushPx(c.position)
                                            c.consume()
                                        } else up = true
                                    }
                                    if (!aborted && current.isNotEmpty()) {
                                        strokes = strokes + BgMaskStroke(current, ratio, restore = tool == RbTool.BRUSH)
                                        redoStack = emptyList()
                                    }
                                    current = emptyList()
                                    cursor = null
                                }
                            }
                    ) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            onSuccess = { s ->
                                val sz = s.painter.intrinsicSize
                                if (sz.width > 0f && sz.height > 0f) aspect = sz.width / sz.height
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // 3) brush cursor ring (drawn above the finger when an offset is set).
                    // The radius is in image space, so it scales with the zoom like the stroke it previews;
                    // the outline width is divided by zoom so it stays a crisp hairline on screen.
                    Canvas(Modifier.fillMaxSize()) {
                        cursor?.let { c ->
                            val r = brushSize / RB_BRUSH_DIVISOR * size.width / 2f
                            drawCircle(Color.Black.copy(alpha = 0.6f), r + 1.5f / zoom, c, style = Stroke(3f / zoom))
                            drawCircle(
                                if (tool == RbTool.BRUSH) Color(0xFF22C55E) else Color.White,
                                r, c, style = Stroke(2f / zoom)
                            )
                        }
                    }
                }

                // analysing overlay (Auto-Cutout / Magic Wand)
                if (analyzing) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                    }
                }
            }

            // floating zoom chip + [ 100% ] reset (only while zoomed in)
            if (zoom > 1.05f) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE60F172A))
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${(zoom * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .clickable { zoom = 1f; pan = Offset.Zero }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("[ 100% ]", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    // unified Export & Share screen (always PNG – transparent cut-out)
    if (showExport) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showExport = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
            )
        ) {
            UnifiedExportScreen(
                previewAspect = aspect,
                forcePng = true,
                shareFallbackUri = photoUri,
                onBack = { showExport = false },
                onExport = { longSide, format, onResult ->
                    exporter.export(photoUri, strokes) { result -> onResult(result) }
                }
            ) {
                // checkerboard behind the photo, replaced by the real exported PNG after saving
                Box(Modifier.fillMaxSize().background(Color(0xFFE2E8F0))) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/** Prominent per-tool Reset button; enabled only while the tool has changes. */
@Composable
private fun RbResetButton(enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB), disabledContentColor = Color(0xFFCBD5E1)),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) Color(0xFF2563EB) else Color(0xFFE2E8F0)),
        modifier = Modifier.height(30.dp)
    ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}