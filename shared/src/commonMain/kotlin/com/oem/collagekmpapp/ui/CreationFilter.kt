package com.oem.collagekmpapp.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.oem.collagekmpapp.BackHandler
import com.oem.collagekmpapp.data.CreationDateRange
import com.oem.collagekmpapp.data.CreationProject
import com.oem.collagekmpapp.data.ProjectEditor
import com.oem.collagekmpapp.data.ProjectRestore
import com.oem.collagekmpapp.data.deleteBatch
import com.oem.collagekmpapp.data.platformNowMillis
import com.oem.collagekmpapp.data.platformUtcOffsetMillis
import com.oem.collagekmpapp.data.rememberMultiImageSharer
import com.oem.collagekmpapp.data.Creation
import com.oem.collagekmpapp.data.CreationKind
import com.oem.collagekmpapp.data.rememberCreationsRepository
import com.oem.collagekmpapp.model.Screen
import com.oem.collagekmpapp.rememberGalleryPicker
import com.oem.collagekmpapp.rememberImageSharer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// commonMain/kotlin/com/oem/collagekmpapp/ui/CreationsScreen.kt

// Neumorphic palette: a soft blue-grey screen with slightly lighter, raised cards.
private val NeuBg = Color(0xFFEEF1F6)
private val NeuCard = Color(0xFFF5F7FA)

/** Raised neumorphic surface: soft ambient + spot shadow and a faint light edge on the top-left. */
private fun Modifier.neuRaised(shape: Shape, elevation: Dp = 4.dp): Modifier = this
    .shadow(
        elevation = elevation, shape = shape,
        ambientColor = Color(0xFFCBD5E1), spotColor = Color(0xFF94A3B8)
    )
    .border(
        1.dp,
        Brush.linearGradient(listOf(Color.White, Color(0x00FFFFFF), Color(0x33CBD5E1))),
        shape
    )

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

/** Where a saved project re-opens: the editor it was made in, with the original photos (and layout / template). */
private fun CreationProject.toScreen(): Screen = when (editor) {
    ProjectEditor.COLLAGE -> when {
        layoutId != null && templateId != null -> Screen.Editor(layoutId, photos, templateId)
        layoutId != null -> Screen.Editor(layoutId, photos)
        else -> Screen.LayoutSelection(photos)
    }
    ProjectEditor.FREESTYLE -> Screen.FreestyleEditor(photos)
    ProjectEditor.PHOTO -> Screen.EditPhoto(photos.first())
    ProjectEditor.REMOVE_BG -> Screen.RemoveBackground(photos.first())
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

    // ---- batch selection (long-press a card)
    var isBatchMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var batchDeleteOpen by remember { mutableStateOf(false) }
    val multiSharer = rememberMultiImageSharer()
    val haptic = LocalHapticFeedback.current

    fun exitBatch() {
        isBatchMode = false
        selectedIds = emptySet()
    }

    fun toggleSelected(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    // ---- date range filter
    var dateRange by remember { mutableStateOf(CreationDateRange.ALL_TIME) }

    /** Parses the project sidecar and opens the editor it came from; the editor restores the rest via ProjectRestore. */
    fun reEdit(c: Creation) {
        val project = c.projectJson?.let { CreationProject.parse(it) }
        if (project == null) {
            scope.launch { snackbar.showSnackbar("This project can't be opened – the project file is damaged or from a newer version.") }
            return
        }
        ProjectRestore.offer(project)
        onNavigate(project.toScreen())
    }

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

    LaunchedEffect(items) {
        val ids = items.mapTo(HashSet()) { it.id }
        selectedIds = selectedIds.filterTo(HashSet()) { it in ids }
        if (items.isEmpty()) exitBatch()
    }
    BackHandler(enabled = isBatchMode && LocalIsTopScreen.current) { exitBatch() }

    val createPicker = rememberGalleryPicker { uris ->
        if (uris.size < 2) {
            scope.launch { snackbar.showSnackbar("Select at least 2 photos to create a collage.") }
        } else {
            onNavigate(Screen.LayoutSelection(uris))
        }
    }

    val visible = remember(items, filter, sort, query, dateRange) {
        val now = platformNowMillis()
        val utcOffset = platformUtcOffsetMillis(now)
        items
            .filter { filter.kind == null || it.kind == filter.kind }
            .filter { dateRange.matches(it.dateMillis, now, utcOffset) }
            .filter {
                // search by file name, or by the date shown on the card ("24 Nov", "Nov 2026", "2026")
                query.isBlank() ||
                        it.name.contains(query.trim(), ignoreCase = true) ||
                        formatDate(it.dateMillis).contains(query.trim(), ignoreCase = true)
            }
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
        containerColor = NeuBg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(Modifier.background(NeuBg)) {
                if (isBatchMode) {
                    BatchActionBar(
                        selectedCount = selectedIds.size,
                        allVisibleSelected = visible.isNotEmpty() && visible.all { it.id in selectedIds },
                        onClose = { exitBatch() },
                        onToggleSelectAll = { selectAll ->
                            val ids = visible.map { it.id }
                            selectedIds = if (selectAll) selectedIds + ids else selectedIds - ids.toSet()
                        },
                        onShare = {
                            val uris = items.filter { it.id in selectedIds }.map { it.uri }
                            runCatching { multiSharer.share(uris) }
                                .onFailure { scope.launch { snackbar.showSnackbar("Couldn't open the share sheet.") } }
                        },
                        onDelete = { batchDeleteOpen = true }
                    )
                } else
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = NeuBg),
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
                                Column {
                                    Text("My Creations", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B), letterSpacing = (-0.3).sp)
                                    if (items.isNotEmpty()) {
                                        Text(
                                            if (items.size == 1) "1 creation" else "${items.size} creations",
                                            fontSize = 11.sp, color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
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
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CreationFilter.entries.forEach { f ->
                        val count = if (f.kind == null) items.size else items.count { it.kind == f.kind }
                        MinimalFilterPill(
                            label = if (items.isEmpty()) f.label else "${f.label} · $count",
                            selected = filter == f,
                            onClick = { filter = f }
                        )
                    }
                }
                // date range chips
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Date range", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    CreationDateRange.entries.forEach { r ->
                        MinimalFilterPill(label = r.label, selected = dateRange == r, onClick = { dateRange = r })
                    }
                }
            }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                loading && items.isEmpty() -> CreationsShimmerGrid()

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
                    onAction = { filter = CreationFilter.ALL; dateRange = CreationDateRange.ALL_TIME; query = ""; searching = false }
                )

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(visible, key = { it.id }) { c ->
                        CreationCard(
                            c,
                            onClick = { if (isBatchMode) toggleSelected(c.id) else sheetFor = c },
                            onShare = { sharer.shareToApp("More", c.uri) },
                            onDelete = { deleteTarget = c },
                            selected = c.id in selectedIds,
                            batchMode = isBatchMode,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isBatchMode) toggleSelected(c.id)
                                else { isBatchMode = true; selectedIds = setOf(c.id) }
                            }
                        )
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
        ModalBottomSheet(
            onDismissRequest = { sheetFor = null },
            containerColor = NeuCard,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 0.dp
        ) {
            Column(Modifier.padding(bottom = 16.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = c.uri, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE2E8F0))
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            listOfNotNull(
                                c.kind.label,
                                formatDate(c.dateMillis).ifEmpty { null },
                                formatSize(c.sizeBytes).ifEmpty { null },
                                if (c.width > 0 && c.height > 0) "${c.width}×${c.height}" else null
                            ).joinToString(" · "),
                            fontSize = 11.sp, color = Color(0xFF64748B)
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color(0xFFE5E7EB))
                if (c.projectJson != null) {
                    SheetAction(Icons.Default.EditNote, "Re-edit Project", tint = Color(0xFF2563EB)) {
                        sheetFor = null; reEdit(c)
                    }
                }
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

    // ============================================================ batch delete confirmation
    if (batchDeleteOpen) {
        val targets = items.filter { it.id in selectedIds }
        val n = targets.size
        AlertDialog(
            onDismissRequest = { batchDeleteOpen = false },
            icon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFDC2626)) },
            title = { Text(if (n == 1) "Delete 1 creation?" else "Delete $n creations?") },
            text = { Text(if (n == 1) "This image will be permanently removed from your device. This can't be undone." else "These $n images will be permanently removed from your device. This can't be undone.") },
            confirmButton = {
                TextButton(
                    enabled = n > 0,
                    onClick = {
                        batchDeleteOpen = false
                        scope.launch {
                            val result = repo.deleteBatch(targets)
                            items = items.filterNot { it.id in result.deletedIds }
                            exitBatch()
                            snackbar.showSnackbar(
                                when {
                                    result.allSucceeded -> if (n == 1) "Creation deleted" else "$n creations deleted"
                                    result.deletedIds.isEmpty() -> result.failed.first().second.message ?: "Couldn't delete these images."
                                    else -> "Deleted ${result.deletedIds.size} of $n – ${result.failed.size} couldn't be removed."
                                }
                            )
                        }
                    }
                ) { Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { batchDeleteOpen = false }) { Text("Cancel") } }
        )
    }
}

/** Contextual top bar while batch-selecting: count, Select all / Deselect all, Share, Delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchActionBar(
    selectedCount: Int,
    allVisibleSelected: Boolean,
    onClose: () -> Unit,
    onToggleSelectAll: (selectAll: Boolean) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = NeuBg),
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Exit selection") }
        },
        title = {
            Text("$selectedCount selected", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
        },
        actions = {
            TextButton(onClick = { onToggleSelectAll(!allVisibleSelected) }) {
                Text(
                    if (allVisibleSelected) "Deselect All" else "Select All",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB)
                )
            }
            IconButton(onClick = onShare, enabled = selectedCount > 0) {
                Icon(Icons.Default.Share, "Share selected", tint = if (selectedCount > 0) Color(0xFF2563EB) else Color(0xFFCBD5E1))
            }
            IconButton(onClick = onDelete, enabled = selectedCount > 0) {
                Icon(Icons.Default.Delete, "Delete selected", tint = if (selectedCount > 0) Color(0xFFDC2626) else Color(0xFFCBD5E1))
            }
        }
    )
}

// =====================================================================================
// Pieces
// =====================================================================================

@Composable
private fun CreationThumb(creation: Creation, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF1F1F1))
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
                .size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) { Icon(creation.kind.icon(), creation.kind.label, tint = Color(0xFF374151), modifier = Modifier.size(11.dp)) }
    }
}

/**
 * Neumorphic creation card: raised rounded container, thumbnail, name, date + size badge and share / delete buttons.
 * Tapping the card opens the actions sheet (same as before); share and delete call the same handlers.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CreationCard(
    creation: Creation,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    selected: Boolean = false,
    batchMode: Boolean = false,
    onLongClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .neuRaised(shape, 4.dp)
            .clip(shape)
            .background(NeuCard)
            .then(if (selected) Modifier.border(2.dp, Color(0xFF2563EB), shape) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp)).background(Color(0xFFE2E8F0))
        ) {
            SubcomposeAsyncImage(
                model = creation.uri,
                contentDescription = creation.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF94A3B8))
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize().background(Color(0xFFFEE2E2)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.BrokenImage, "Couldn't load", tint = Color(0xFFEF4444))
                    }
                }
            )
            Box(
                Modifier.align(Alignment.TopStart).padding(6.dp)
                    .size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) { Icon(creation.kind.icon(), creation.kind.label, tint = Color(0xFF475569), modifier = Modifier.size(11.dp)) }
            // re-editable project badge
            if (creation.projectJson != null) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(6.dp)
                        .size(20.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Layers, "Re-editable project", tint = Color(0xFF2563EB), modifier = Modifier.size(11.dp)) }
            }
            // batch selection: blue tint + check circle
            if (batchMode) {
                if (selected) Box(Modifier.matchParentSize().background(Color(0x262563EB)))
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .size(22.dp).clip(CircleShape)
                        .background(if (selected) Color(0xFF2563EB) else Color.White.copy(alpha = 0.9f))
                        .border(1.5.dp, if (selected) Color(0xFF2563EB) else Color(0xFFCBD5E1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(Icons.Default.Check, "Selected", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            creation.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatDate(creation.dateMillis).ifEmpty { creation.kind.label },
                fontSize = 10.sp, color = Color(0xFF94A3B8), maxLines = 1, modifier = Modifier.weight(1f)
            )
            formatSize(creation.sizeBytes).ifEmpty { null }?.let { size ->
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFE2E8F0)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(size, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B), maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            NeuIconAction(Icons.Default.Share, "Share", Color(0xFF2563EB), onShare)
            Spacer(Modifier.width(10.dp))
            NeuIconAction(Icons.Default.Delete, "Delete", Color(0xFFDC2626), onDelete)
        }
    }
}

/** Small raised round icon button. */
@Composable
private fun NeuIconAction(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).neuRaised(CircleShape, 3.dp).clip(CircleShape).background(NeuCard).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, tint = tint, modifier = Modifier.size(15.dp)) }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, tint: Color = Color(0xFF1E293B), onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, fontSize = 14.sp, color = tint, fontWeight = FontWeight.Medium) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { Icon(icon, null, tint = tint) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * Illustrated empty state: raised neumorphic badge (with two small floating accent bubbles),
 * headline, helper text and a tactile "Create Collage" button that opens the gallery picker ([onCreate]).
 */
@Composable
private fun EmptyCreationsState(onCreate: () -> Unit) {
    // gentle "breathing" float on the illustration
    val floatAnim = rememberInfiniteTransition(label = "emptyFloat")
    val bob by floatAnim.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )

    Column(
        Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            // outer sunken ring
            Box(
                Modifier.size(132.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFDDE3EC), NeuBg, Color.White)))
            )
            // raised badge
            Box(
                Modifier
                    .size(100.dp)
                    .graphicsLayer { translationY = -4.dp.toPx() * bob }
                    .neuRaised(CircleShape, 8.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color.White, NeuCard))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PhotoLibrary, null, tint = Color(0xFF2563EB), modifier = Modifier.size(42.dp))
            }
            // little decorative bubbles
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 6.dp)
                    .graphicsLayer { translationY = 3.dp.toPx() * bob }
                    .size(30.dp).neuRaised(CircleShape, 3.dp).clip(CircleShape).background(NeuCard),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.GridView, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(15.dp)) }
            Box(
                Modifier.align(Alignment.BottomStart).padding(bottom = 12.dp, start = 4.dp)
                    .graphicsLayer { translationY = -3.dp.toPx() * bob }
                    .size(26.dp).neuRaised(CircleShape, 3.dp).clip(CircleShape).background(NeuCard),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Brush, null, tint = Color(0xFFEC4899), modifier = Modifier.size(13.dp)) }
        }
        Spacer(Modifier.height(28.dp))
        Text("No Collages Yet", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Spacer(Modifier.height(8.dp))
        Text(
            "Choose photos from your gallery and create your first grid or freestyle collage!",
            textAlign = TextAlign.Center, color = Color(0xFF64748B), fontSize = 13.sp, lineHeight = 19.sp,
            modifier = Modifier.widthIn(max = 300.dp)
        )
        Spacer(Modifier.height(28.dp))
        NeuTactileButton(
            label = "Create Collage",
            icon = Icons.Default.AddPhotoAlternate,
            onClick = onCreate
        )
    }
}

/** Tactile neumorphic button: sinks (lower shadow + slight scale-down) while pressed, haptic tick on tap. */
@Composable
private fun NeuTactileButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val elevation by animateDpAsState(if (pressed) 1.dp else 6.dp, tween(110), label = "btnElev")
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(110), label = "btnScale")
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(48.dp)
            .neuRaised(shape, elevation)
            .clip(shape)
            .background(
                if (pressed) Brush.linearGradient(listOf(Color(0xFFE2E8F0), NeuCard))
                else Brush.linearGradient(listOf(Color.White, NeuCard))
            )
            .clickable(interactionSource = interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
    }
}

// =====================================================================================
// Shimmer loading placeholders (shown instead of a spinner while creations load)
// =====================================================================================

/** 2-column grid of neumorphic placeholder cards with a pulsing alpha + diagonal gradient sweep. */
@Composable
private fun CreationsShimmerGrid() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val sweep by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "sweep"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.65f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxSize()
    ) {
        items(6) {
            ShimmerCreationCard(sweep = { sweep }, pulse = { pulse })
        }
    }
}

/** Mirrors CreationCard's layout (thumb, name, date + size, two round actions) as shimmering blocks. */
@Composable
private fun ShimmerCreationCard(sweep: () -> Float, pulse: () -> Float) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .neuRaised(shape, 4.dp)
            .clip(shape)
            .background(NeuCard)
            .graphicsLayer { alpha = pulse() }
            .padding(10.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).shimmerBlock(RoundedCornerShape(14.dp), sweep))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(0.8f).height(11.dp).shimmerBlock(RoundedCornerShape(6.dp), sweep))
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).padding(end = 24.dp).height(9.dp).shimmerBlock(RoundedCornerShape(5.dp), sweep))
            Box(Modifier.width(34.dp).height(14.dp).shimmerBlock(RoundedCornerShape(7.dp), sweep))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(Modifier.size(30.dp).shimmerBlock(CircleShape, sweep))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(30.dp).shimmerBlock(CircleShape, sweep))
        }
    }
}

/**
 * Sunken placeholder block with a moving highlight. [progress] is read inside the draw phase,
 * so the animation only redraws – it never recomposes the grid.
 */
private fun Modifier.shimmerBlock(shape: Shape, progress: () -> Float): Modifier = this
    .clip(shape)
    .drawBehind {
        val base = Color(0xFFE2E8F0)
        val highlight = Color(0xFFF8FAFC)
        drawRect(base)
        val w = size.width.coerceAtLeast(1f)
        val x = -w + progress() * (w * 3f)    // sweep from off-left to off-right
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight.copy(alpha = 0.9f), Color.Transparent),
                start = Offset(x, 0f),
                end = Offset(x + w * 0.8f, size.height)
            )
        )
    }

/** Flat text pill: filled dark when selected, plain grey text otherwise. */
@Composable
private fun MinimalFilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .then(if (selected) Modifier else Modifier.neuRaised(shape, 3.dp))
            .clip(shape)
            .background(if (selected) Color(0xFFE2E8F0) else NeuCard)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) Color(0xFF2563EB) else Color(0xFF64748B)
        )
    }
}

@Composable
private fun MessageState(icon: ImageVector, title: String, body: String, actionLabel: String, onAction: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
        Spacer(Modifier.height(4.dp))
        Text(body, textAlign = TextAlign.Center, color = Color(0xFF64748B), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onAction) { Text(actionLabel, fontSize = 13.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold) }
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