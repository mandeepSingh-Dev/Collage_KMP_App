package com.oem.collagekmpapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.collagekmpapp.model.Screen
import com.oem.collagekmpapp.rememberGalleryPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (Screen) -> Unit
) {
    val scrollState = rememberScrollState()
    var selectedResolution by remember { mutableStateOf("Standard Resolution (720p)") }
    var selectedLanguage by remember { mutableStateOf("English") }

    var showResolutionSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var languageQuery by remember { mutableStateOf("") }

    val galleryPicker = rememberGalleryPicker { selectedUris ->
        if (selectedUris.size > 1) {
            onNavigate(Screen.LayoutSelection(selectedUris))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1E293B)) },
                navigationIcon = {
                    IconButton(onClick = { onNavigate(Screen.Home) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavItem(icon = Icons.Default.Home, label = "Home", selected = false) {
                        onNavigate(Screen.Home)
                    }
                    BottomNavItem(icon = Icons.Default.GridView, label = "Creations", selected = false) {
                        galleryPicker.launch()
                    }
                    BottomNavItem(icon = Icons.Default.Settings, label = "Settings", selected = true) {}
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Section 1: Export & Storage
            SettingsSectionHeader("Export & Storage")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.PhotoSizeSelectLarge,
                    title = "Resolution",
                    subtitle = selectedResolution,
                    onClick = { showResolutionSheet = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                SettingsRow(
                    icon = Icons.Default.FolderOpen,
                    title = "Save Path",
                    subtitle = "/Pictures/CollageStudio",
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Preferences
            SettingsSectionHeader("Preferences")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Language,
                    title = "Select Language",
                    subtitle = selectedLanguage,
                    onClick = { showLanguageSheet = true }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Support & Legal
            SettingsSectionHeader("Support & Legal")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Feedback,
                    title = "Feedback",
                    subtitle = null,
                    onClick = {}
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                SettingsRow(
                    icon = Icons.Default.Description,
                    title = "Terms of Use",
                    subtitle = null,
                    onClick = {}
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                SettingsRow(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = null,
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reset to Default Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(18.dp))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .clickable {
                        selectedResolution = "Standard Resolution (720p)"
                        selectedLanguage = "English"
                    }
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Reset to Default",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer Branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Crafted for Memories",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "COLLAGE STUDIO",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // Resolution Bottom Sheet
        if (showResolutionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showResolutionSheet = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Resolution", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        IconButton(onClick = { showResolutionSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val resolutions = listOf(
                        Triple("Standard", "720p • Fast export, smaller size", "Standard Resolution (720p)"),
                        Triple("High", "1080p • Crisp HD for social media", "High Resolution (1080p)"),
                        Triple("Ultra", "4K • Maximum print quality", "Ultra Resolution (4K)")
                    )

                    resolutions.forEach { (title, subtitle, fullName) ->
                        val isSelected = selectedResolution == fullName
                        ResolutionOptionCard(
                            title = title,
                            subtitle = subtitle,
                            selected = isSelected,
                            onClick = {
                                selectedResolution = fullName
                                showResolutionSheet = false
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Select Language Bottom Sheet
        if (showLanguageSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLanguageSheet = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Language", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        IconButton(onClick = { showLanguageSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = languageQuery,
                        onValueChange = { languageQuery = it },
                        placeholder = { Text("Search language...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val languages = listOf(
                        "English (Default)", "Hindi (हिंदी)", "Español (Spanish)",
                        "Français (French)", "Deutsch (German)", "Arabic (العربية)",
                        "Português (Portuguese)", "Italiano (Italian)", "日本語 (Japanese)"
                    )
                    val filteredLanguages = languages.filter { it.contains(languageQuery, ignoreCase = true) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredLanguages.forEach { lang ->
                            val isSelected = selectedLanguage == lang
                            LanguageOptionRow(
                                language = lang,
                                selected = isSelected,
                                onClick = {
                                    selectedLanguage = lang
                                    showLanguageSheet = false
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(vertical = 4.dp),
        content = content
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = Color(0xFFEFF6FF),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                }
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ResolutionOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB))
            )
        }
    }
}

@Composable
fun LanguageOptionRow(
    language: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFEFF6FF) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = language,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color(0xFF2563EB) else Color(0xFF1E293B)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
