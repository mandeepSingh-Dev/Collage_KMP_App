package com.oem.collagekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.oem.collagekmpapp.data.Creation
import com.oem.collagekmpapp.data.CreationKind
import com.oem.collagekmpapp.data.rememberCreationsRepository
import com.oem.collagekmpapp.model.Screen
import com.oem.collagekmpapp.rememberGalleryPicker
import com.oem.collagekmpapp.rememberImageSharer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// commonMain/kotlin/com/oem/collagekmpapp/ui/CreationsScreen.kt

private enum class CreationFilter(val label: String, val kind: CreationKind?) {
    ALL("All", null),
    COLLAGES("Collages", CreationKind.COLLAGE),
    FREESTYLE("Freestyle", CreationKind.FREESTYLE),
    EDITED("Edited", CreationKind.EDITED),
    REMOVE_BG("Remove BG", CreationKind.REMOVE_BG)
}

private enum class CreationSort(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    SIZE_DESC("Largest size"),
    SIZE_ASC("Smallest size")
}

private fun CreationKind.icon(): ImageVector = when (this) {
    CreationKind.COLLAGE -> Icons.Default.GridView
    CreationKind.FREESTYLE -> Icons.Default.Brush
    CreationKind.EDITED -> Icons.Default.Edit
    CreationKind.REMOVE_BG -> Icons.Default.ContentCut
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreationsScreen(
    savePath: String = "/Pictures/CollageStudio",
    onNavigate: (Screen) -> Unit
) {
    val repo = rememberCreationsRepository(savePath.trim('/'))
    val sharer = rememberImageSharer()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var items by remember { mutableStateOf<List<Creation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var filter by remember { mutableStateOf(CreationFilter.ALL) }
    var sort by remember { mutableStateOf(CreationSort.NEWEST) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    var sheetFor by remember { mutableStateOf<Creation?>(null) }
    var viewerStartId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<Creation?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            loadError = null
            runCatching { repo.load() }
                .onSuccess { items = it }
                .onFailure { loadError = it.message ?: "Couldn't read your creations." }
            loading = false
        }
    }
    LaunchedEffect(repo) { reload() }

    val createPicker = rememberGalleryPicker { uris ->
        if (uris.size < 2) {
            scope.launch { snackbar.showSnackbar("Select at least 2 photos to create a collage.") }
        } else {
            onNavigate(Screen.LayoutSelection(uris))
        }
    }

    val visible = remember(items, filter, sort, query) {
        items
            .filter { filter.kind == null || it.kind == filter.kind }
            .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
            .let { list ->
                when (sort) {
                    CreationSort.NEWEST -> list.sortedByDescending { it.dateMillis }
                    CreationSort.OLDEST -> list.sortedBy { it.dateMillis }
                    CreationSort.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                    CreationSort.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
                    CreationSort.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
                    CreationSort.SIZE_ASC -> list.sortedBy { it.sizeBytes }
                }
            }
    }

    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searching) { if (searching) searchFocus.requestFocus() }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(Modifier.background(Color(0xFFF8FAFC))) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC)),
                    navigationIcon = {
                        if (searching) {
                            IconButton(onClick = { searching = false; query = "" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close search")
                            }
                        }
                    },
                    title = {
                        if (searching) {
                            TextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                placeholder = { Text("Search by file name") },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth().focusRequester(searchFocus)
                            )
                        } else {
                            Text("My Creations", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                        }
                    },
                    actions = {
                        if (searching) {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, "Clear")
                            }
                        } else {
                            IconButton(onClick = { searching = true }) { Icon(Icons.Default.Search, "Search") }
                        }
                        Box {
                            IconButton(onClick = { sortMenuOpen = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, "Sort")
                            }
                            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                                CreationSort.entries.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.label) },
                                        leadingIcon = {
                                            if (sort == s) Icon(Icons.Default.Check, null)
                                            else Spacer(Modifier.size(24.dp))
                                        },
                                        onClick = { sort = s; sortMenuOpen = false }
                                    )
                                }
                            }
                        }
                    }
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CreationFilter.entries.forEach { f ->
                        val count = if (f.kind == null) items.size else items.count { it.kind == f.kind }
                        FilterChip(
                            selected = filter == f,
                            onClick = { filter = f },
                            label = { Text(if (items.isEmpty()) f.label else "${f.label} · $count") },
                            leadingIcon = if (filter == f) {
                                { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null
                        )
                    }
                }
            }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                loading && items.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                loadError != null -> MessageState(
                    icon = Icons.Default.BrokenImage,
                    title = "Couldn't load creations",
                    body = loadError ?: "",
                    actionLabel = "Try again",
                    onAction = { reload() }
                )

                items.isEmpty() -> EmptyCreationsState(onCreate = { createPicker.launch() })

                visible.isEmpty() -> MessageState(
                    icon = Icons.Default.SearchOff,
                    title = "Nothing here",
                    body = "No creations match your search or filter.",
                    actionLabel = "Show all",
                    onAction = { filter = CreationFilter.ALL; query = ""; searching = false }
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 108.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visible, key = { it.id }) { c ->
                        CreationThumb(c, onClick = { sheetFor = c })
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        BrandSignatureFooter()
                    }
                }
            }
        }
    }

    // ============================================================ actions sheet
    sheetFor?.let { c ->
        ModalBottomSheet(onDismissRequest = { sheetFor = null }) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = c.uri, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE2E8F0))
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(
                                c.kind.label,
                                formatDate(c.dateMillis).ifEmpty { null },
                                formatSize(c.sizeBytes).ifEmpty { null },
                                if (c.width > 0 && c.height > 0) "${c.width}×${c.height}" else null
                            ).joinToString(" · "),
                            fontSize = 12.sp, color = Color(0xFF64748B)
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SheetAction(Icons.Default.Fullscreen, "View full screen") {
                    sheetFor = null; viewerStartId = c.id
                }
                SheetAction(Icons.Default.Share, "Share") {
                    sheetFor = null; sharer.shareToApp("More", c.uri)
                }
                SheetAction(Icons.Default.Edit, "Edit photo") {
                    sheetFor = null; onNavigate(Screen.EditPhoto(c.uri))
                }
                SheetAction(Icons.Default.AutoAwesome, "Remix in Freestyle") {
                    sheetFor = null; onNavigate(Screen.FreestyleEditor(listOf(c.uri)))
                }
                SheetAction(Icons.Default.Delete, "Delete", tint = Color(0xFFDC2626)) {
                    sheetFor = null; deleteTarget = c
                }
            }
        }
    }

    // ============================================================ full-screen viewer
    viewerStartId?.let { startId ->
        if (visible.isEmpty()) {
            viewerStartId = null
        } else {
            CreationViewer(
                items = visible,
                startIndex = visible.indexOfFirst { it.id == startId }.coerceAtLeast(0),
                onDismiss = { viewerStartId = null },
                onShare = { sharer.shareToApp("More", it.uri) },
                onEdit = { viewerStartId = null; onNavigate(Screen.EditPhoto(it.uri)) },
                onDelete = { deleteTarget = it }
            )
        }
    }

    // ============================================================ delete confirmation
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFDC2626)) },
            title = { Text("Delete this creation?") },
            text = { Text("“${target.name}” will be permanently removed from your device. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    scope.launch {
                        val result = repo.delete(target)
                        if (result.isSuccess) {
                            items = items.filterNot { it.id == target.id }
                            snackbar.showSnackbar("Creation deleted")
                        } else {
                            snackbar.showSnackbar(result.exceptionOrNull()?.message ?: "Couldn't delete this image.")
                        }
                    }
                }) { Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

// =====================================================================================
// Pieces
// =====================================================================================

@Composable
private fun CreationThumb(creation: Creation, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE2E8F0))
            .clickable(onClick = onClick)
    ) {
        SubcomposeAsyncImage(
            model = creation.uri,
            contentDescription = creation.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF94A3B8))
                }
            },
            error = {
                Box(Modifier.fillMaxSize().background(Color(0xFFFEE2E2)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.BrokenImage, "Couldn't load", tint = Color(0xFFEF4444))
                }
            }
        )
        Box(
            Modifier.align(Alignment.BottomStart).padding(6.dp)
                .size(22.dp).clip(CircleShape).background(Color(0x99000000)),
            contentAlignment = Alignment.Center
        ) { Icon(creation.kind.icon(), creation.kind.label, tint = Color.White, modifier = Modifier.size(12.dp)) }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, tint: Color = Color(0xFF1E293B), onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = tint, fontWeight = FontWeight.Medium) },
        leadingContent = { Icon(icon, null, tint = tint) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun EmptyCreationsState(onCreate: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(120.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFFDBEAFE), Color(0xFFF3E8FF)))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF2563EB), modifier = Modifier.size(56.dp)) }
        Spacer(Modifier.height(24.dp))
        Text("No creations yet", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
        Spacer(Modifier.height(8.dp))
        Text(
            "Collages, freestyle designs and edited photos you save will show up here.",
            textAlign = TextAlign.Center, color = Color(0xFF64748B), fontSize = 14.sp
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onCreate,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            modifier = Modifier.height(50.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Create Collage", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MessageState(icon: ImageVector, title: String, body: String, actionLabel: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Spacer(Modifier.height(4.dp))
        Text(body, textAlign = TextAlign.Center, color = Color(0xFF64748B), fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onAction) { Text(actionLabel) }
    }
}

// =====================================================================================
// Full-screen viewer: swipe between creations, pinch / double-tap to zoom
// =====================================================================================

@Composable
private fun CreationViewer(
    items: List<Creation>,
    startIndex: Int,
    onDismiss: () -> Unit,
    onShare: (Creation) -> Unit,
    onEdit: (Creation) -> Unit,
    onDelete: (Creation) -> Unit
) {
    val pager = rememberPagerState(initialPage = startIndex) { items.size }
    var zoomed by remember { mutableStateOf(false) }
    LaunchedEffect(pager.currentPage) { zoomed = false }
    val current = items.getOrNull(pager.currentPage)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pager,
                userScrollEnabled = !zoomed,
                key = { items.getOrNull(it)?.id ?: it },
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImage(items[page].uri) { zoomed = it }
            }

            Row(
                Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)))
                    .statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text(current?.name.orEmpty(), color = Color.White, fontWeight = FontWeight.Bold,
                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${pager.currentPage + 1} of ${items.size}", color = Color(0xCCFFFFFF), fontSize = 11.sp)
                }
                if (current != null) {
                    IconButton(onClick = { onEdit(current) }) { Icon(Icons.Default.Edit, "Edit", tint = Color.White) }
                    IconButton(onClick = { onShare(current) }) { Icon(Icons.Default.Share, "Share", tint = Color.White) }
                    IconButton(onClick = { onDelete(current) }) { Icon(Icons.Default.Delete, "Delete", tint = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(uri: String, onZoomed: (Boolean) -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > 1.05f) 1f else 2.5f
                    offset = Offset.Zero
                    onZoomed(scale > 1.02f)
                })
            }
            .pointerInput(Unit) {
                // Only consumes the gesture when zoomed or pinching, so a 1-finger swipe still reaches the pager.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val ev = awaitPointerEvent()
                        if (ev.changes.size > 1 || scale > 1f) {
                            val ns = (scale * ev.calculateZoom()).coerceIn(1f, 5f)
                            val pan = ev.calculatePan()
                            val maxX = size.width * (ns - 1f) / 2f
                            val maxY = size.height * (ns - 1f) / 2f
                            offset = if (ns <= 1f) Offset.Zero else Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                            scale = ns
                            onZoomed(ns > 1.02f)
                            ev.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (ev.changes.any { it.pressed })
                }
            }
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale; scaleY = scale
                translationX = offset.x; translationY = offset.y
            }
        )
    }
}

// =====================================================================================
// Formatting (no date library needed)
// =====================================================================================

private val MonthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** UTC date, e.g. "24 Nov 2026" (civil-from-days algorithm). */
private fun formatDate(millis: Long): String {
    if (millis <= 0L) return ""
    val z = millis / 86_400_000L + 719_468L
    val era = (if (z >= 0) z else z - 146_096L) / 146_097L
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val day = doy - (153L * mp + 2L) / 5L + 1L
    val month = if (mp < 10) mp + 3 else mp - 9
    val year = yoe + era * 400L + if (month <= 2) 1 else 0
    return "$day ${MonthNames[(month - 1).toInt()]} $year"
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val mb = bytes / 1_048_576.0
    return if (mb >= 1.0) "${(mb * 10).roundToInt() / 10.0} MB" else "${(bytes / 1024.0).roundToInt()} KB"
}