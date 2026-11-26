package com.oem.collagekmpapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.oem.collagekmpapp.AppSettings
import com.oem.collagekmpapp.NeuColors
import com.oem.collagekmpapp.SyncNeuSystemTheme
import com.oem.collagekmpapp.rememberGalleryPicker
import com.oem.collagekmpapp.model.Screen

@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit
) {
    val scrollState = rememberScrollState()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Theme: keeps "System" mode in sync with the OS, and recomposes when AppSettings.theme changes
    SyncNeuSystemTheme()
    val isDarkTheme = NeuColors.isDark
    val haptic = LocalHapticFeedback.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val galleryPicker = rememberGalleryPicker { selectedUris ->
        if (selectedUris.isEmpty()) {
            errorMessage = "Validation Error: Please select at least 1 image."
        } else if (selectedUris.size == 1) {
            errorMessage = null
            onNavigate(Screen.Editor(layoutId = "l1_full", selectedPhotos = selectedUris))
        } else {
            errorMessage = null
            onNavigate(Screen.LayoutSelection(selectedUris))
        }
    }

    val freestyleGalleryPicker = rememberGalleryPicker { selectedUris ->
        if (selectedUris.isEmpty()) {
            errorMessage = "Validation Error: Please select at least 1 image for Freestyle editor."
        } else {
            errorMessage = null
            onNavigate(Screen.FreestyleEditor(selectedUris))
        }
    }

    val editPhotoPicker = rememberGalleryPicker { selectedUris ->
        val uri = selectedUris.firstOrNull()
        if (uri == null) {
            errorMessage = "Validation Error: Please select an image to edit."
        } else {
            errorMessage = null
            onNavigate(Screen.EditPhoto(uri))
        }
    }

    val removeBgPicker = rememberGalleryPicker { selectedUris ->
        val uri = selectedUris.firstOrNull()
        if (uri == null) {
            errorMessage = "Validation Error: Please select an image to remove its background."
        } else {
            errorMessage = null
            onNavigate(Screen.RemoveBackground(uri))
        }
    }

    // Templates: tap a template -> pick photos -> editor opens with the template applied
    var pendingTemplate by remember { mutableStateOf<com.oem.collagekmpapp.data.TemplateItem?>(null) }
    val templatePicker = rememberGalleryPicker { selectedUris ->
        val template = pendingTemplate
        if (template == null || selectedUris.isEmpty()) {
            errorMessage = "Validation Error: Please select at least 1 image to use a template."
        } else {
            errorMessage = null
            onNavigate(
                Screen.Editor(
                    layoutId = template.layoutIdFor(selectedUris.size),
                    // smart order: first picked photo -> the template's featured (largest) slot
                    selectedPhotos = template.orderPhotosForSlots(selectedUris),
                    templateId = template.id
                )
            )
        }
    }

    Scaffold(
        containerColor = NeuColors.Background,   // light value is the original #F4F6F9
        snackbarHost = {
            if (errorMessage != null) {
                Snackbar(
                    action = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("Dismiss", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFFDC2626),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(errorMessage!!, color = Color.White)
                }
            }
        }
    ) { innerPadding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                    slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing), initialOffsetY = { it / 10 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeuColors.Background)
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Bar header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Collage Studio",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeuColors.TextPrimary   // light value is the original #1E293B
                    )
                    // Theme toggle: observes AppSettings.theme (via NeuColors.isDark) and flips Light <-> Dark.
                    // Dark -> Sun icon (tap for light), Light -> Moon icon (tap for dark).
                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme) Color(0xFF334155) else Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                AppSettings.updateTheme(if (isDarkTheme) "Light" else "Dark")
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme",
                                tint = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF475569),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Card 1: Choose from Gallery
                NeumorphicFeatureCard(
                    title = "Choose from Gallery",
                    subtitle = "Select multiple photos to start",
                    backgroundColor = Color(0xFFE8F1FC),
                    icon = Icons.Default.PhotoLibrary,
                    onClick = { galleryPicker.launch() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Card 2: Grid Collage
                NeumorphicFeatureCard(
                    title = "Grid Collage",
                    subtitle = "Up to 50 photos",
                    backgroundColor = Color(0xFFEBF0FE),
                    icon = Icons.Default.GridOn,
                    onClick = { galleryPicker.launch() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Card 3: Freestyle Collage
                NeumorphicFeatureCard(
                    title = "Freestyle Collage",
                    subtitle = "Unleash creativity with freeform layout",
                    backgroundColor = Color(0xFFF3E8FF),
                    icon = Icons.Default.Brush,
                    onClick = { freestyleGalleryPicker.launch() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Card 4: Edit Photo
                NeumorphicFeatureCard(
                    title = "Edit Photo",
                    subtitle = "Crop, filter, blur, draw & more",
                    backgroundColor = Color(0xFFE6F4EA),
                    icon = Icons.Default.Edit,
                    onClick = { editPhotoPicker.launch() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Card 5: Remove Background
                NeumorphicFeatureCard(
                    title = "Remove Background",
                    subtitle = "Erase or restore parts of your photo",
                    backgroundColor = Color(0xFFEDE9FE),
                    icon = Icons.Default.AutoFixHigh,
                    onClick = { removeBgPicker.launch() }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Card 6: My Creations
                NeumorphicFeatureCard(
                    title = "My Creations",
                    subtitle = "View & share your saved collages",
                    subtitleColor = Color(0xFFE11D48),
                    backgroundColor = Color(0xFFFEE2E2),
                    icon = Icons.Default.Folder,
                    onClick = { onNavigate(Screen.Creations) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Templates: themed collages (Christmas, New Year, Birthday, Love, Story, Polaroid)
                TemplatesSection(
                    onTemplateClick = { template ->
                        pendingTemplate = template
                        templatePicker.launch()
                    }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Footer Branding
                BrandSignatureFooter()
            }
        }
    }
}

@Composable
fun NeumorphicFeatureCard(
    title: String,
    subtitle: String,
    subtitleColor: Color = Color(0xFF64748B),
    backgroundColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0xFFCBD5E1),
                spotColor = Color(0xFF94A3B8)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = subtitleColor
                )
            }
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.85f),
                shadowElevation = 2.dp,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}