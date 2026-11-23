package com.oem.collagekmpapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.collagekmpapp.model.Screen
import com.oem.collagekmpapp.rememberGalleryPicker

private val feedbackCategories = listOf(
    listOf("Bad Result Quality", "Crash", "Best"),
    listOf("Good", "Bug", "Suggestions"),
    listOf("Slow Export", "UI / Layout", "Others")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Bad Result Quality") }
    var description by remember { mutableStateOf("") }
    var screenshotUri by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val screenshotPicker = rememberGalleryPicker { uris ->
        screenshotUri = uris.firstOrNull()
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Send Feedback", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        bottomBar = {
            Surface(color = Color(0xFFF8FAFC), tonalElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeedbackNavItem(Icons.Default.Home, "Home", selected = false) { onNavigate(Screen.Home) }
                    FeedbackNavItem(Icons.Default.GridView, "Creations", selected = false) { onNavigate(Screen.Home) }
                    FeedbackNavItem(Icons.Default.Settings, "Settings", selected = true) { onNavigate(Screen.Settings) }
                }
            }
        }
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What is this about?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    feedbackCategories.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { category ->
                                val selected = selectedCategory == category
                                Surface(
                                    modifier = Modifier.weight(1f).height(34.dp).clickable { selectedCategory = category },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (selected) Color(0xFFEFF6FF) else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selected) Color(0xFF2563EB) else Color(0xFFCBD5E1)
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            category,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (selected) Color(0xFF1D4ED8) else Color(0xFF334155),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Description", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; submitted = false },
                    placeholder = { Text("Tell us what happened or share your ideas...", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
                OutlinedButton(
                    onClick = { screenshotPicker.launch() },
                    modifier = Modifier.align(Alignment.End).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF475569))
                    Spacer(Modifier.width(6.dp))
                    Text(if (screenshotUri == null) "Attach Screenshot" else "Screenshot attached", fontSize = 11.sp, color = Color(0xFF334155))
                }
                screenshotUri?.let {
                    Text("Screenshot attached", fontSize = 11.sp, color = Color(0xFF16A34A))
                }
            }

            Spacer(Modifier.weight(1f))

            if (submitted) {
                Text("Thanks for your feedback.", color = Color(0xFF16A34A), fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Button(
                onClick = {
                    if (description.isNotBlank()) {
                        submitted = true
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(if (submitted) "Feedback Submitted" else "Submit Feedback", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun FeedbackNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) Color(0xFF0F172A) else Color(0xFF64748B),
            modifier = Modifier.size(20.dp)
        )
        Text(label, fontSize = 10.sp, color = if (selected) Color(0xFF2563EB) else Color(0xFF64748B), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
