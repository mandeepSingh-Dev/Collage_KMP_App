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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.collagekmpapp.rememberGalleryPicker
import com.oem.collagekmpapp.model.Screen

@Composable
fun HomeScreen(
    onNavigate: (Screen) -> Unit
) {
    val scrollState = rememberScrollState()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Smooth entry animation state to prevent jank/lag and respect recompositions
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val galleryPicker = rememberGalleryPicker { selectedUris ->
        if (selectedUris.size <= 1) {
            errorMessage = "Validation Error: Please select more than 1 image (at least 2 images required to create a collage)."
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

    // Single-image picker for the Edit Photo feature (only the first pick is used)
    val editPhotoPicker = rememberGalleryPicker { selectedUris ->
        val uri = selectedUris.firstOrNull()
        if (uri == null) {
            errorMessage = "Validation Error: Please select an image to edit."
        } else {
            errorMessage = null
            onNavigate(Screen.EditPhoto(uri))
        }
    }

    // Single-image picker for the Remove Background feature (only the first pick is used)
    val removeBgPicker = rememberGalleryPicker { selectedUris ->
        val uri = selectedUris.firstOrNull()
        if (uri == null) {
            errorMessage = "Validation Error: Please select an image to remove its background."
        } else {
            errorMessage = null
            onNavigate(Screen.RemoveBackground(uri))
        }
    }

    Scaffold(
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
        )
        {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEDF2F7))
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Bar header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Collage Studio",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF475569)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Top Action Card: Choose from Gallery (Full Width)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .shadow(6.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF2563EB), Color(0xFF06B6D4))
                            )
                        )
                        .clickable { galleryPicker.launch() }
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(
                            text = "Choose from Gallery",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select multiple photos to start",
                            fontSize = 13.sp,
                            color = Color(0xFFE0F2FE)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.CenterEnd)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Grid Collage Card (Full Width)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .shadow(4.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF0EA5E9))
                            )
                        )
                        .clickable { galleryPicker.launch() }
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp).align(Alignment.End)
                        )
                        Column {
                            Text(
                                text = "Grid Collage",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Up to 50 photos",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Freestyle Card (Full Width)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .shadow(4.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF8B5CF6), Color(0xFFA855F7))
                            )
                        )
                        .clickable { freestyleGalleryPicker.launch() }
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(
                            text = "Freestyle Collage",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Unleash creativity with freeform layout",
                            fontSize = 13.sp,
                            color = Color(0xFFF3E8FF)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterEnd)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Edit Photo Card (Full Width) - single photo editor
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .shadow(4.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0D9488), Color(0xFF14B8A6))
                            )
                        )
                        .clickable { editPhotoPicker.launch() }
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(
                            text = "Edit Photo",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Crop, filter, blur, draw & more",
                            fontSize = 13.sp,
                            color = Color(0xFFCCFBF1)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterEnd)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Remove Background Card (Full Width)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .shadow(4.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0EA5E9), Color(0xFF6366F1))
                            )
                        )
                        .clickable { removeBgPicker.launch() }
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(
                            text = "Remove Background",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Erase or restore parts of your photo",
                            fontSize = 13.sp,
                            color = Color(0xFFE0E7FF)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterEnd)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Full-Width Card: My Creations
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .shadow(4.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFF43F5E), Color(0xFFFB7185))
                            )
                        )
                        .clickable { onNavigate(Screen.Creations) }
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text(
                            text = "My Creations",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "View & share your saved collages",
                            fontSize = 13.sp,
                            color = Color(0xFFFFF1F2)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterEnd)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Footer Branding
                BrandSignatureFooter()

                // =========================================================================
                // OTHER CARDS COMMENTED OUT AS REQUESTED (for future implementation)
                // =========================================================================
                /*
                data class FeatureItem(
                    val title: String,
                    val subtitle: String,
                    val icon: ImageVector,
                    val colors: List<Color>,
                    val onClick: () -> Unit
                )

                val otherFeatures = listOf(
                    FeatureItem("Freestyle", "Unleash creativity", Icons.Default.Brush, listOf(Color(0xFF8B5CF6), Color(0xFFA855F7))) { galleryPicker.launch() },
                    FeatureItem("Templates", "Quick & easy designs", Icons.Default.Dashboard, listOf(Color(0xFFF97316), Color(0xFFFB923C))) { galleryPicker.launch() },
                    FeatureItem("Camera", "Shoot & collage", Icons.Default.PhotoCamera, listOf(Color(0xFF16A34A), Color(0xFF22C55E))) { galleryPicker.launch() },
                    FeatureItem("Filters & Glitter", "Sparkling effects", Icons.Default.AutoAwesome, listOf(Color(0xFFEC4899), Color(0xFFF43F5E))) { galleryPicker.launch() },
                    FeatureItem("Photo Editor", "Refine your shots", Icons.Default.Edit, listOf(Color(0xFF0D9488), Color(0xFF14B8A6))) { galleryPicker.launch() },
                    FeatureItem("Scrapbook", "Personal journals", Icons.Default.Book, listOf(Color(0xFFEA580C), Color(0xFFEF4444))) { galleryPicker.launch() },
                    FeatureItem("Mirror & Shape", "Creative reflections", Icons.Default.Flip, listOf(Color(0xFFC026D3), Color(0xFF9333EA))) { galleryPicker.launch() },
                    FeatureItem("Background", "Patterns & colors", Icons.Default.Wallpaper, listOf(Color(0xFFEAB308), Color(0xFFF59E0B))) { galleryPicker.launch() }
                )
                */
            }
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        if (selected) {
            Surface(
                color = Color(0xFFDBEAFE),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(width = 56.dp, height = 30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = label, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = label, tint = Color(0xFF64748B), modifier = Modifier.size(22.dp))
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) Color(0xFF2563EB) else Color(0xFF64748B)
    )
}