package com.oem.collagekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.collagekmpapp.NeuSegmented
import com.oem.collagekmpapp.model.Screen

data class LayoutOption(
    val id: String,
    val name: String,
    val slots: Int,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSelectionScreen(
    selectedPhotos: List<String>,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val count = selectedPhotos.size.coerceAtLeast(2)

    val layouts = remember(count) {
        com.oem.collagekmpapp.data.LayoutRepository().getLayouts(count).map {
            LayoutOption(it.id, it.name, it.slotCount, it.description)
        }
    }

    var selectedLayout by remember(layouts) { mutableStateOf(layouts[0]) }

    // "Standard Grids" <-> "Themed Templates"
    var templateMode by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<com.oem.collagekmpapp.data.TemplateItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Layout Studio ($count Photos)", fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
                            Text("Undo", fontSize = 9.sp, color = Color(0xFF475569))
                        }
                    }
                    IconButton(onClick = {}) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(18.dp))
                            Text("Redo", fontSize = 9.sp, color = Color(0xFF94A3B8))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val tpl = selectedTemplate
                            if (templateMode && tpl != null) {
                                // smart order: first picked photo -> the template's featured (largest) slot (matches the live preview)
                                onNavigate(Screen.Editor(tpl.layoutIdFor(selectedPhotos.size), tpl.orderPhotosForSlots(selectedPhotos), tpl.id))
                            } else {
                                onNavigate(Screen.Editor(selectedLayout.id, selectedPhotos))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Next")
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color.White)) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = false,
                        onClick = { },
                        icon = { Icon(Icons.Default.Crop, contentDescription = null) },
                        label = { Text("Ratio") }
                    )
                    NavigationBarItem(
                        selected = true,
                        onClick = { },
                        icon = { Icon(Icons.Default.GridView, contentDescription = null) },
                        label = { Text("Layout") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { onNavigate(Screen.Editor(selectedLayout.id, selectedPhotos)) },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text("Border") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { onNavigate(Screen.Editor(selectedLayout.id, selectedPhotos)) },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) },
                        label = { Text("Filter") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        label = { Text("Glitter") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { },
                        icon = { Text("Tt", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        label = { Text("Text") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            NeuSegmented(
                options = listOf("Standard Grids", "Themed Templates"),
                selected = if (templateMode) "Themed Templates" else "Standard Grids",
                onSelect = { templateMode = it == "Themed Templates" },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(260.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(8.dp)
            ) {
                val tplPreview = selectedTemplate
                if (templateMode && tplPreview != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TemplateLivePreview(tplPreview, selectedPhotos, Modifier.fillMaxHeight())
                    }
                } else {
                    RenderCollageLayout(
                        layoutId = selectedLayout.id,
                        photos = selectedPhotos,
                        spacing = 8.dp,
                        cornerRadius = 16.dp,
                        filterPreset = "Original",
                        filterIntensity = 0f,
                        glitterCard = "Flare 2",
                        glitterDensity = 0f,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (templateMode) "Themed Templates" else "Layouts for $count Photos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF475569))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (templateMode) {
                        TemplatePicker(
                            selectedId = selectedTemplate?.id,
                            onSelect = { selectedTemplate = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(layouts) { layout ->
                                val isSelected = selectedLayout.id == layout.id
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(70.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                                        .border(
                                            width = if (isSelected) 2.2.dp else 1.3.dp,
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF475569),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedLayout = layout }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = layout.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = layout.description,
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}