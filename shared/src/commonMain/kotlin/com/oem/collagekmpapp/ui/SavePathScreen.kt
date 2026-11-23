package com.oem.collagekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class SaveFolder(
    val name: String,
    val details: String,
    val icon: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavePathScreen(
    selectedPath: String,
    onBack: () -> Unit,
    onSelect: (String) -> Unit
) {
    val folders = remember { mutableStateListOf(
        SaveFolder("CollageStudio (Default)", "14 items • Just now", icon = true),
        SaveFolder("DCIM", "Camera, Screenshot • 120 items"),
        SaveFolder("Pictures", "Screenshots, Grids • 85 items"),
        SaveFolder("Instagram Collages", "Saved social media • 34 items"),
        SaveFolder("Downloads", "Imported assets • 12 items"),
        SaveFolder("Screenshots", "Captures • 48 items")
    ) }
    var activeFolder by remember(selectedPath) {
        mutableStateOf(selectedPath.substringAfterLast('/').ifBlank { "CollageStudio (Default)" })
    }
    var showCreateFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            TopAppBar(
                title = { Text("Save Path", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    TextButton(onClick = { onSelect("/Pictures/${activeFolder.removeSuffix(" (Default)")}") }) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2563EB))
                        Spacer(Modifier.width(4.dp))
                        Text("Select", color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        bottomBar = {
            Surface(color = Color(0xFFF8FAFC), shadowElevation = 4.dp) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    OutlinedButton(
                        onClick = { showCreateFolder = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0F172A)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A))
                    ) {
                        Text("+ New Folder", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Surface(color = Color(0xFFEFF6FF)) {
                Text(
                    text = "Internal Storage  >  Pictures  >  $activeFolder",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    fontSize = 12.sp,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                folders.forEach { folder ->
                    val pathName = folder.name.removeSuffix(" (Default)")
                    val selected = activeFolder == folder.name || activeFolder == pathName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (selected) Color(0xFFDBEAFE) else Color.Transparent, RoundedCornerShape(14.dp))
                            .clickable { activeFolder = folder.name }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (folder.icon) Icons.Default.Folder else Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = if (folder.icon) Color(0xFF2563EB) else Color(0xFF64748B),
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(folder.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Spacer(Modifier.height(2.dp))
                            Text(folder.details, fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Color(0xFF2563EB))
                        } else {
                            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                                Surface(shape = RoundedCornerShape(50), color = Color(0xFF94A3B8), modifier = Modifier.size(16.dp)) {}
                                Surface(shape = RoundedCornerShape(50), color = Color(0xFFF8FAFC), modifier = Modifier.size(12.dp)) {}
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateFolder) {
        AlertDialog(
            onDismissRequest = { showCreateFolder = false },
            title = { Text("New Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newFolderName.trim()
                        if (name.isNotEmpty() && folders.none { it.name.equals(name, ignoreCase = true) }) {
                            folders.add(SaveFolder(name, "New folder • Just now"))
                            activeFolder = name
                            newFolderName = ""
                            showCreateFolder = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolder = false }) { Text("Cancel") }
            }
        )
    }
}
