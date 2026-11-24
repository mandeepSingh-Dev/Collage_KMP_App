package com.oem.collagekmpapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import coil3.compose.AsyncImage
import com.oem.collagekmpapp.BgMaskStroke
import com.oem.collagekmpapp.components.FluidSlider
import com.oem.collagekmpapp.rememberBackgroundRemovalExporter
import kotlinx.coroutines.launch

private enum class RbTool { ERASER, BRUSH }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Remove Background", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { undo() }, enabled = strokes.isNotEmpty()) { Icon(Icons.Default.Undo, "Undo") }
                    IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty()) { Icon(Icons.Default.Redo, "Redo") }
                    Button(
                        enabled = !isSaving,
                        onClick = {
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
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RbChip("Eraser", tool == RbTool.ERASER) { tool = RbTool.ERASER }
                    RbChip("Brush (restore)", tool == RbTool.BRUSH) { tool = RbTool.BRUSH }
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

            Box(Modifier.size(cw, ch).clip(RoundedCornerShape(6.dp))) {
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
                                val offPx = brushOffset.dp.toPx()
                                val ratio = brushSize / RB_BRUSH_DIVISOR
                                fun brushPx(p: Offset) = Offset(p.x, p.y - offPx)
                                fun norm(p: Offset) = brushPx(p).let { Offset(it.x / w, it.y / h) }

                                current = listOf(norm(down.position))
                                cursor = brushPx(down.position)
                                down.consume()

                                var up = false
                                while (!up) {
                                    val c = awaitPointerEvent().changes.firstOrNull() ?: break
                                    if (c.pressed) {
                                        current = current + norm(c.position)
                                        cursor = brushPx(c.position)
                                        c.consume()
                                    } else up = true
                                }
                                if (current.isNotEmpty()) {
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

                // 3) brush cursor ring (drawn above the finger when an offset is set)
                Canvas(Modifier.fillMaxSize()) {
                    cursor?.let { c ->
                        val r = brushSize / RB_BRUSH_DIVISOR * size.width / 2f
                        drawCircle(Color.Black.copy(alpha = 0.6f), r + 1.5f, c, style = Stroke(3f))
                        drawCircle(
                            if (tool == RbTool.BRUSH) Color(0xFF22C55E) else Color.White,
                            r, c, style = Stroke(2f)
                        )
                    }
                }
            }
        }
    }
}