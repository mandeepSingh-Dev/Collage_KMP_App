package com.oem.collagekmpapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.oem.collagekmpapp.ExportOptions
import com.oem.collagekmpapp.rememberGalleryOpener
import com.oem.collagekmpapp.rememberImageSharer
import com.oem.collagekmpapp.rememberSavedImageResolver
import com.oem.collagekmpapp.ui.QualityPreset
import com.oem.collagekmpapp.ui.ShareApp

/** The old per-editor AlertDialogs are kept in the code but switched off – every editor now opens [UnifiedExportScreen]. */
const val USE_LEGACY_EXPORT_DIALOG = false

/**
 * ONE export screen for every editing feature (Collage, Freestyle, Edit Photo, Remove Background).
 *
 *  - resolution / format / watermark start from the global Settings ([AppSettings])
 *  - "Save to Gallery" runs [onExport], then opens the saved picture in the device Gallery (ACTION_VIEW)
 *  - [preview] draws the picture inside the preview card; once saved, the real exported file replaces it
 *
 * @param onExport longSide in px (720 / 1080 / 3840), format token ("JPG" | "PNG" | "WEBP"),
 *                 callback with the saved location (content URI, or a display string for older exporters)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedExportScreen(
    previewAspect: Float,
    onExport: (longSide: Int, format: String, onResult: (Result<String>) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Export & Share",
    onHome: (() -> Unit)? = null,
    forcePng: Boolean = false,
    shareFallbackUri: String = "",
    previewWidth: Dp = 220.dp,
    preview: @Composable BoxScope.() -> Unit
) {
    val sharer = rememberImageSharer()
    val openGallery = rememberGalleryOpener()
    val resolveSaved = rememberSavedImageResolver()

    // defaults come from the global Settings
    var longSide by remember { mutableStateOf(AppSettings.longSide) }
    var format by remember { mutableStateOf(if (forcePng) "PNG" else AppSettings.formatToken) }
    var watermark by remember { mutableStateOf(AppSettings.watermark) }

    var copiedToast by remember { mutableStateOf(false) }
    var isSavedToGallery by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var exportedUri by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Box(modifier = Modifier.background(NeuColors.Surface)) {
                NeuTopBar(
                    title = title,
                    onBack = onBack,
                    actions = {
                        if (onHome != null) {
                            TextButton(onClick = onHome) {
                                Text("Home", fontWeight = FontWeight.Bold, color = NeuColors.Accent, fontSize = 15.sp)
                            }
                        }
                    }
                )
            }
        },
        containerColor = NeuColors.Surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSavedToGallery) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Surface(shape = CircleShape, color = Color(0xFF22C55E), modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saved to Gallery!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ---- preview card (neumorphic)
            NeuCard(shape = RoundedCornerShape(22.dp), elevation = 9.dp, contentPadding = PaddingValues(10.dp)) {
                Box(
                    modifier = Modifier
                        .width(previewWidth)
                        .aspectRatio(previewAspect.coerceIn(0.2f, 5f))
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    val saved = exportedUri
                    if (isSavedToGallery && saved != null && saved.startsWith("content://")) {
                        AsyncImage(
                            model = saved,
                            contentDescription = "Exported image",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        preview()
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ---- export settings (neumorphic)
            NeuCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                elevation = 9.dp,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Export Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeuColors.TextPrimary)

                    Text("Quality Presets", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeuColors.TextSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val qualities = listOf(
                            Triple(720, QualityPreset("Standard (720p)", "1.2 MB"), "720p"),
                            Triple(1080, QualityPreset("High (1080p)", "2.8 MB"), "1080p"),
                            Triple(3840, QualityPreset("Ultra HD (4K)", "8.5 MB"), "4K")
                        )
                        qualities.forEach { (px, preset, _) ->
                            val isSelected = longSide == px
                            val shape = RoundedCornerShape(16.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(66.dp)
                                    .then(
                                        if (isSelected) Modifier.neumorphicInset(shape)
                                        else Modifier.neumorphic(shape, elevation = 5.dp)
                                    )
                                    .then(if (isSelected) Modifier.border(1.5.dp, NeuColors.Accent, shape) else Modifier)
                                    .clip(shape)
                                    .clickable { longSide = px },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = preset.title.substringBefore(" "),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NeuColors.Accent else NeuColors.TextPrimary
                                    )
                                    Text(
                                        text = preset.title.substringAfter("(").substringBefore(")"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NeuColors.Accent else NeuColors.TextPrimary
                                    )
                                    Text(preset.size, fontSize = 10.sp, color = NeuColors.TextSecondary)
                                }
                            }
                        }
                    }

                    Text("Format", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeuColors.TextSecondary)
                    NeuSegmented(
                        options = if (forcePng) listOf("PNG") else listOf("JPG", "PNG", "WEBP"),
                        selected = format,
                        onSelect = { format = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // watermark (default from Settings)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                            NeuIconBadge(Icons.Default.WaterDrop, size = 36.dp)
                            Column {
                                Text("Add Watermark", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NeuColors.TextPrimary)
                                Text("Small label in the corner", fontSize = 11.sp, color = NeuColors.TextMuted)
                            }
                        }
                        NeuSwitch(checked = watermark, onCheckedChange = { watermark = it })
                    }

                    Text("Fast Share", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NeuColors.TextSecondary)
                    val shareApps = listOf(
                        ShareApp("Instagram", Color(0xFFE1306C), Icons.Default.CameraAlt),
                        ShareApp("WhatsApp", Color(0xFF22C55E), Icons.AutoMirrored.Filled.Chat),
                        ShareApp("Facebook", Color(0xFF2563EB), Icons.Default.Share),
                        ShareApp("Stories", Color(0xFFD946EF), Icons.Default.AutoAwesome),
                        ShareApp("Snapchat", Color(0xFFEAB308), Icons.Default.Star),
                        ShareApp("TikTok", Color(0xFF0F172A), Icons.Default.MusicNote),
                        ShareApp("More", Color(0xFF64748B), Icons.Default.MoreHoriz)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().height(170.dp)
                    ) {
                        items(shareApps) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { sharer.shareToApp(app.name, exportedUri ?: shareFallbackUri) }
                                    .padding(4.dp)
                            ) {
                                NeuIconBadge(app.icon, tint = app.color, size = 48.dp, contentDescription = app.name)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(app.name, fontSize = 11.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphicInset(RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Copy Link  collage_studio_2026_11...", fontSize = 12.sp, color = NeuColors.TextSecondary, maxLines = 1, modifier = Modifier.weight(1f))
                        TextButton(onClick = { copiedToast = true }) {
                            Text("Copy", fontWeight = FontWeight.Bold, color = NeuColors.Accent, fontSize = 13.sp)
                        }
                    }
                    if (copiedToast) Text("Link copied to clipboard!", fontSize = 11.sp, color = Color(0xFF16A34A))

                    if (exportError != null) {
                        Text(exportError ?: "Export failed", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                    }

                    // ---- Save to Gallery -> MediaStore (/Pictures/CollageStudio) -> open in Gallery
                    NeuButton(
                        onClick = {
                            if (isExporting) return@NeuButton
                            isExporting = true
                            exportError = null
                            ExportOptions.watermark = watermark
                            onExport(longSide, format) { result ->
                                isExporting = false
                                result.fold(
                                    onSuccess = { location ->
                                        val uri = resolveSaved(location)
                                        exportedUri = uri
                                        isSavedToGallery = true
                                        openGallery(uri)
                                    },
                                    onFailure = { exportError = it.message ?: "Export failed" }
                                )
                            }
                        },
                        enabled = !isExporting,
                        accent = NeuColors.Accent,
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Exporting High-Res Image...", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        } else {
                            Text("Save to Gallery", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        }
                    }

                    if (isSavedToGallery) {
                        NeuButton(
                            onClick = { exportedUri?.let { openGallery(it) } },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = NeuColors.Accent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Open in Gallery", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NeuColors.Accent)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}