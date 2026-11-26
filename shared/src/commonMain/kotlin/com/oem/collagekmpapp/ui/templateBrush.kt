package com.oem.collagekmpapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.collagekmpapp.NeuColors
import com.oem.collagekmpapp.data.LayoutRepository
import com.oem.collagekmpapp.data.TemplateCategory
import com.oem.collagekmpapp.data.TemplateItem
import com.oem.collagekmpapp.data.TemplatePhotoFilter
import com.oem.collagekmpapp.data.TemplateRepository
import com.oem.collagekmpapp.rememberKeyValueStore
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.oem.collagekmpapp.data.TemplateThumbStyle
import com.oem.collagekmpapp.model.ProjectOverlayItem
import com.oem.collagekmpapp.neumorphic
import com.oem.collagekmpapp.neumorphicInset

// commonMain – visual pieces of the Templates feature (Home section + Layout Studio picker).

private fun templateBrush(t: TemplateItem): Brush {
    val colors = t.preset.backgroundGradientColors.map { Color(it.toInt()) }
    return if (colors.size >= 2) Brush.linearGradient(colors)
    else Brush.linearGradient(listOf(Color(t.preset.backgroundColorLong.toInt()), Color(t.preset.backgroundColorLong.toInt())))
}

private fun TemplateItem.aspect(): Float = preset.aspectRatioWidth / preset.aspectRatioHeight

/** Category filter chips (All, Christmas, New Year, Birthday, Love, Story, Polaroid). */
@Composable
fun TemplateCategoryChips(
    selected: TemplateCategory,
    onSelect: (TemplateCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TemplateCategory.entries.forEach { cat ->
            val isSel = cat == selected
            val shape = RoundedCornerShape(50)
            Row(
                modifier = Modifier
                    .then(
                        if (isSel) Modifier.neumorphicInset(shape)
                        else Modifier.neumorphic(shape, elevation = 4.dp)
                    )
                    .then(if (isSel) Modifier.border(1.5.dp, NeuColors.Accent, shape) else Modifier)
                    .clip(shape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(cat)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(cat.emoji, fontSize = 14.sp)
                Text(
                    cat.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) NeuColors.Accent else NeuColors.TextSecondary
                )
            }
        }
    }
}

/** Photo-count filter chips (All Photos, 2, 3, 4+) shown under the category chips. */
@Composable
fun TemplatePhotoCountChips(
    selected: TemplatePhotoFilter,
    onSelect: (TemplatePhotoFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TemplatePhotoFilter.entries.forEach { f ->
            val isSel = f == selected
            val shape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .then(if (isSel) Modifier.neumorphicInset(shape) else Modifier.neumorphic(shape, elevation = 3.dp))
                    .then(if (isSel) Modifier.border(1.2.dp, NeuColors.Accent, shape) else Modifier)
                    .clip(shape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(f)
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    f.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (isSel) NeuColors.Accent else NeuColors.TextSecondary
                )
            }
        }
    }
}

/** Confirmation before deleting one of the user's own presets. */
@Composable
private fun DeletePresetDialog(template: TemplateItem, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete preset?") },
        text = { Text("“${template.title}” will be removed from My Presets.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Shown when the current filters match nothing (e.g. My Presets before anything was saved). */
@Composable
private fun TemplatesEmptyHint(category: TemplateCategory, modifier: Modifier = Modifier) {
    Text(
        if (category == TemplateCategory.Custom)
            "No presets yet. Open a collage and tap the ★ Save as Template button to keep its look here."
        else "No templates match this photo count.",
        fontSize = 12.sp, color = NeuColors.TextSecondary,
        modifier = modifier.padding(horizontal = 8.dp, vertical = 16.dp)
    )
}

/** Text + emoji decorations of a template, positioned from the normalised preset. */
@Composable
private fun TemplateDecorLayer(template: TemplateItem, width: Dp, height: Dp) {
    Box(modifier = Modifier.size(width, height)) {
        template.preset.overlays.forEach { item ->
            if (item is ProjectOverlayItem.Emoji) {
                val size = width * 0.1f * item.scale
                Text(
                    text = item.emoji,
                    fontSize = (width.value * 0.075f * item.scale).sp,
                    modifier = Modifier
                        .offset(x = width * item.x - size / 2, y = height * item.y - size / 2)
                        .graphicsLayer(rotationZ = item.rotation)
                )
            }
        }
        template.preset.textLayers.forEach { layer ->
            val fs = (width.value * 0.065f * layer.scale)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = width * layer.x - width * 0.4f, y = height * layer.y - (fs * 0.9f).dp)
                    .width(width * 0.8f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = layer.text,
                    color = Color(layer.colorLong.toInt()),
                    fontSize = fs.sp,
                    maxLines = 1,
                    fontWeight = if (layer.fontName == "Bold" || layer.fontName == "Neon" || layer.fontName == "Retro") FontWeight.Bold else FontWeight.Medium,
                    fontStyle = if (layer.fontName == "Script") FontStyle.Italic else FontStyle.Normal,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(50))
                        .padding(horizontal = (fs * 0.5f).dp, vertical = (fs * 0.15f).dp)
                )
            }
        }
    }
}

/** Placeholder preview of a template (no photos needed): gradient + tinted slots + decorations. */
@Composable
fun TemplateThumbnail(template: TemplateItem, height: Dp, modifier: Modifier = Modifier) {
    val width = height * template.aspect()
    val repo = remember { LayoutRepository() }
    val slots = remember(template.id) { template.slotsFor(template.preferredPhotos, repo) }
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(14.dp))
            .background(templateBrush(template))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outer = template.preset.outerSpacingRatio * size.width
            val gap = template.preset.innerSpacingRatio * size.width / 2f
            val cw = size.width - 2 * outer
            val ch = size.height - 2 * outer
            val radius = template.preset.cornerRadiusRatio * size.width
            slots.forEach { s ->
                val l = outer + s.left * cw + gap
                val t = outer + s.top * ch + gap
                val w = (s.right - s.left) * cw - 2 * gap
                val h = (s.bottom - s.top) * ch - 2 * gap
                when (template.thumbStyle) {
                    TemplateThumbStyle.POLAROID -> {
                        drawRoundRect(Color.White, Offset(l, t), Size(w, h), CornerRadius(radius))
                        val pad = w * 0.07f
                        drawRoundRect(
                            Color(template.accentColorLong.toInt()).copy(alpha = 0.35f),
                            Offset(l + pad, t + pad), Size(w - 2 * pad, h - pad * 3.2f), CornerRadius(radius / 2f)
                        )
                    }
                    else -> drawRoundRect(Color.White.copy(alpha = 0.42f), Offset(l, t), Size(w, h), CornerRadius(radius))
                }
            }
            if (template.thumbStyle == TemplateThumbStyle.BANNER) {
                drawRect(Color.Black.copy(alpha = 0.18f), Offset(0f, size.height * 0.88f), Size(size.width, size.height * 0.12f))
            }
            if (template.thumbStyle == TemplateThumbStyle.STORY) {
                drawRoundRect(Color.White.copy(alpha = 0.7f), Offset(size.width * 0.38f, size.height * 0.015f), Size(size.width * 0.24f, size.height * 0.006f), CornerRadius(4f))
            }
        }
        TemplateDecorLayer(template, width, height)
    }
}

/** One card in the Templates carousel / picker. */
@Composable
fun TemplateCard(
    template: TemplateItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    thumbHeight: Dp = 190.dp,
    /** Non-null for the user's own presets: shows a small delete badge on the thumbnail. */
    onDelete: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    val width = thumbHeight * template.aspect()
    Column(
        modifier = modifier
            .width(width + 16.dp)
            .then(if (selected) Modifier.neumorphicInset(shape) else Modifier.neumorphic(shape, elevation = 7.dp))
            .then(if (selected) Modifier.border(2.dp, NeuColors.Accent, shape) else Modifier)
            .clip(shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            TemplateThumbnail(template, thumbHeight)
            if (onDelete != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(5.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) { Text("✕", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(template.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeuColors.TextPrimary, maxLines = 1)
        Text(
            "${template.category.displayName} • ${template.aspectLabel}",
            fontSize = 11.sp, color = NeuColors.TextSecondary, maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
    }
}

/** Home screen block: title, category chips and a horizontally scrolling row of template cards. */
@Composable
fun TemplatesSection(
    onTemplateClick: (TemplateItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var category by remember { mutableStateOf(TemplateCategory.All) }
    var photoFilter by remember { mutableStateOf(TemplatePhotoFilter.All) }
    var pendingDelete by remember { mutableStateOf<TemplateItem?>(null) }
    // loads saved presets once (idempotent)
    val kv = rememberKeyValueStore()
    remember(kv) { TemplateRepository.attach(kv); true }
    // not remembered: reading the preset list here keeps the row live when a preset is saved / deleted
    val items = TemplateRepository.byCategoryAndPhotoCount(category, photoFilter)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // NeuColors light values are the original #1E293B / #64748B; they turn light in dark mode
            Text("Templates", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = NeuColors.TextPrimary)
            Text("${items.size} designs", fontSize = 12.sp, color = NeuColors.TextSecondary)
        }
        Spacer(Modifier.height(6.dp))
        TemplateCategoryChips(selected = category, onSelect = { category = it })
        TemplatePhotoCountChips(selected = photoFilter, onSelect = { photoFilter = it })
        Spacer(Modifier.height(6.dp))
        if (items.isEmpty()) {
            TemplatesEmptyHint(category)
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(items, key = { it.id }) { t ->
                    TemplateCard(
                        template = t,
                        onClick = { onTemplateClick(t) },
                        onDelete = if (t.category == TemplateCategory.Custom) ({ pendingDelete = t }) else null
                    )
                }
            }
        }
    }
    pendingDelete?.let { t ->
        DeletePresetDialog(t, onDismiss = { pendingDelete = null }) {
            TemplateRepository.removeCustom(t.id)
            pendingDelete = null
        }
    }
}

/** Filterable 2-column picker used inside Layout Studio ("Themed Templates" mode). */
@Composable
fun TemplatePicker(
    selectedId: String?,
    onSelect: (TemplateItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var category by remember { mutableStateOf(TemplateCategory.All) }
    var photoFilter by remember { mutableStateOf(TemplatePhotoFilter.All) }
    var pendingDelete by remember { mutableStateOf<TemplateItem?>(null) }
    val kv = rememberKeyValueStore()
    remember(kv) { TemplateRepository.attach(kv); true }
    val items = TemplateRepository.byCategoryAndPhotoCount(category, photoFilter)
    Column(modifier = modifier) {
        TemplateCategoryChips(selected = category, onSelect = { category = it })
        TemplatePhotoCountChips(selected = photoFilter, onSelect = { photoFilter = it })
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            TemplatesEmptyHint(category)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { t ->
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        TemplateCard(
                            t, onClick = { onSelect(t) }, selected = t.id == selectedId, thumbHeight = 130.dp,
                            onDelete = if (t.category == TemplateCategory.Custom) ({ pendingDelete = t }) else null
                        )
                    }
                }
            }
        }
    }
    pendingDelete?.let { t ->
        DeletePresetDialog(t, onDismiss = { pendingDelete = null }) {
            TemplateRepository.removeCustom(t.id)
            pendingDelete = null
        }
    }
}

/** The user's photos dropped into the template (gradient, spacing, filter, text and decorations applied). */
@Composable
fun TemplateLivePreview(template: TemplateItem, photos: List<String>, modifier: Modifier = Modifier) {
    val repo = remember { LayoutRepository() }
    val count = photos.size.coerceAtLeast(1)
    val layoutId = remember(template.id, count) { template.layoutIdFor(count, repo) }
    // first picked photo -> featured (largest) slot, same order the editor will receive
    val orderedPhotos = remember(template.id, photos) { template.orderPhotosForSlots(photos, repo) }
    BoxWithConstraints(modifier = modifier.aspectRatio(template.aspect()).clip(RoundedCornerShape(16.dp))) {
        val w = maxWidth
        val h = maxHeight
        Box(modifier = Modifier.fillMaxSize().background(templateBrush(template)))
        RenderCollageLayout(
            layoutId = layoutId,
            photos = orderedPhotos,
            spacing = w * template.preset.innerSpacingRatio,
            cornerRadius = w * template.preset.cornerRadiusRatio,
            filterPreset = template.preset.filterPreset,
            filterIntensity = template.preset.filterIntensity,
            glitterCard = "Glitz",
            glitterDensity = 0f,
            adjustValues = template.preset.adjustValues,
            modifier = Modifier.fillMaxSize().padding(w * template.preset.outerSpacingRatio)
        )
        TemplateDecorLayer(template, w, h)
    }
}