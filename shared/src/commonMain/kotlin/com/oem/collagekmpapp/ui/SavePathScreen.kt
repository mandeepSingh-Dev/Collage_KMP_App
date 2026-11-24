package com.oem.collagekmpapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
        SaveFolder("CollageStudio (Default)", "Pictures/$COMMON_FOLDER_NAME • Default", icon = true),
        SaveFolder("DCIM", "Camera & media storage"),
        SaveFolder("Pictures", "Standard image directory"),
        SaveFolder("Downloads", "Downloaded assets & files"),
        SaveFolder("Screenshots", "Screen captures")
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
                    TextButton(onClick = {
                        val path = if (activeFolder == "CollageStudio (Default)" || activeFolder == "Pictures/$COMMON_FOLDER_NAME") {
                            DEFAULT_SAVE_PATH
                        } else {
                            "/${activeFolder.removeSuffix(" (Default)")}/$COMMON_FOLDER_NAME"
                        }
                        onSelect(path)
                    }) {
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
                        border = BorderStroke(1.5.dp, Color(0xFF0F172A)),
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
                val displayPath = if (activeFolder == "CollageStudio (Default)" || activeFolder == "Pictures/$COMMON_FOLDER_NAME") {
                    "Internal Storage  >  Pictures  >  $COMMON_FOLDER_NAME"
                } else {
                    "Internal Storage  >  ${activeFolder.removeSuffix(" (Default)")}  >  $COMMON_FOLDER_NAME"
                }
                Text(
                    text = displayPath,
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
        Dialog(
            onDismissRequest = { showCreateFolder = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        showCreateFolder = false
                    },
                contentAlignment = Alignment.Center
            ) {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.855f)
                            .shadow(16.dp, RoundedCornerShape(28.dp))
                            .clickable(enabled = false, onClick = {}),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CreateNewFolder,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Create New Folder",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enter a name for your custom storage directory.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            OutlinedTextField(
                                value = newFolderName,
                                onValueChange = { newFolderName = it },
                                label = { Text("Folder Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showCreateFolder = false },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Text("Cancel", color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = {
                                        val name = newFolderName.trim()
                                        if (name.isNotEmpty() && folders.none { it.name.equals(name, ignoreCase = true) }) {
                                            folders.add(SaveFolder(name, "Custom folder • Just now"))
                                            activeFolder = name
                                            newFolderName = ""
                                            showCreateFolder = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
