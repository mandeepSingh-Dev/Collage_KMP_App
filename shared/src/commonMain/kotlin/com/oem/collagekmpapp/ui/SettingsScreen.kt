package com.oem.collagekmpapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.collagekmpapp.AppSettings
import com.oem.collagekmpapp.NeuButton
import com.oem.collagekmpapp.NeuCard
import com.oem.collagekmpapp.NeuIconBadge
import com.oem.collagekmpapp.NeuSwitch
import com.oem.collagekmpapp.NeuTopBar
import com.oem.collagekmpapp.formatBytes
import com.oem.collagekmpapp.model.Screen
import com.oem.collagekmpapp.neumorphic
import com.oem.collagekmpapp.neumorphicInset
import com.oem.collagekmpapp.rememberCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (Screen) -> Unit,
    selectedSavePath: String = DEFAULT_SAVE_PATH,
    onResetSavePath: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // Settings states -> GLOBAL (AppSettings is persisted and read by every export screen)
    val selectedResolution = AppSettings.resolution
    val selectedLanguage = AppSettings.language
    val exportFormat = AppSettings.exportFormat
    val imageQuality = AppSettings.imageQuality
    val watermarkEnabled = AppSettings.watermark
    val themeMode = AppSettings.theme

    // Real cache size / clearing
    val cacheManager = rememberCacheManager()
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableStateOf(0L) }
    val cacheSize = formatBytes(cacheBytes)
    var showClearedSnackbar by remember { mutableStateOf(false) }

    // Bottom sheets state
    var showResolutionSheet by remember { mutableStateOf(false) }
    val resolutionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showFormatSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var languageQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // measure the real cache size when the screen opens
    LaunchedEffect(Unit) {
        cacheBytes = withContext(Dispatchers.Default) { cacheManager.sizeBytes() }
    }

    LaunchedEffect(showClearedSnackbar) {
        if (showClearedSnackbar) {
            val freed = withContext(Dispatchers.Default) { cacheManager.clear() }
            // re-measure so the label shows what is REALLY left (files in use may remain)
            cacheBytes = withContext(Dispatchers.Default) { cacheManager.sizeBytes() }
            snackbarHostState.showSnackbar("Cache cleared successfully (${formatBytes(freed)} freed)")
            showClearedSnackbar = false
        }
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.background(Color(0xFFEDF2F7))) {
                NeuTopBar(title = "Settings", onBack = { onNavigate(Screen.Home) })
            }
        },
        containerColor = Color(0xFFEDF2F7),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDF2F7))
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
                    subtitle = selectedSavePath,
                    onClick = { onNavigate(Screen.SavePath(selectedSavePath)) }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                SettingsRow(
                    icon = Icons.Default.FilePresent,
                    title = "Export Format",
                    subtitle = exportFormat,
                    onClick = { showFormatSheet = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                SettingsSwitchRow(
                    icon = Icons.Default.WaterDrop,
                    title = "Add Watermark",
                    subtitle = "Watermark exported creations",
                    checked = watermarkEnabled,
                    onCheckedChange = { AppSettings.updateWatermark(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Preferences & Appearance
            SettingsSectionHeader("Preferences & Appearance")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Language,
                    title = "Select Language",
                    subtitle = selectedLanguage,
                    onClick = { showLanguageSheet = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    title = "App Theme",
                    subtitle = themeMode,
                    onClick = { showThemeSheet = true }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                SettingsRow(
                    icon = Icons.Default.CleaningServices,
                    title = "Clear Temp Cache",
                    subtitle = cacheSize,
                    onClick = { showClearedSnackbar = true }
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
                    onClick = { onNavigate(Screen.Feedback) }
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
            NeuButton(
                onClick = {
                    AppSettings.resetToDefaults()
                    onResetSavePath()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Reset to Default",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer Branding
            BrandSignatureFooter()
        }

        // Resolution Bottom Sheet
        if (showResolutionSheet) {
            ModalBottomSheet(
                sheetState = resolutionSheetState,
                onDismissRequest = { showResolutionSheet = false },
                containerColor = Color(0xFFEDF2F7),
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
                                AppSettings.updateResolution(fullName)
                                showResolutionSheet = false
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Export Format Bottom Sheet
        if (showFormatSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFormatSheet = false },
                containerColor = Color(0xFFEDF2F7),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("Select Export Format", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(16.dp))
                    listOf("JPEG (Best for Sharing)", "PNG (Lossless)", "WebP (Modern & Compact)").forEach { fmt ->
                        val shortName = fmt.substringBefore(" ")
                        ResolutionOptionCard(
                            title = fmt,
                            subtitle = "Recommended format for output files",
                            selected = exportFormat == shortName,
                            onClick = {
                                AppSettings.updateExportFormat(shortName)
                                showFormatSheet = false
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Theme Bottom Sheet
        if (showThemeSheet) {
            ModalBottomSheet(
                onDismissRequest = { showThemeSheet = false },
                containerColor = Color(0xFFEDF2F7),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("App Theme", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(16.dp))
                    listOf("System Default", "Light Mode", "Dark Mode").forEach { th ->
                        ResolutionOptionCard(
                            title = th,
                            subtitle = "Choose app color appearance",
                            selected = themeMode == th,
                            onClick = {
                                AppSettings.updateTheme(th)
                                showThemeSheet = false
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Select Language Bottom Sheet
        if (showLanguageSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLanguageSheet = false },
                containerColor = Color(0xFFEDF2F7),
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
                                    AppSettings.updateLanguage(lang)
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
    NeuCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 8.dp,
        contentPadding = PaddingValues(vertical = 4.dp),
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
            NeuIconBadge(icon = icon, size = 38.dp)
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
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            NeuIconBadge(icon = icon, size = 38.dp)
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B)
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
        NeuSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ResolutionOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected) Modifier.neumorphicInset(shape = shape)
                else Modifier.neumorphic(shape = shape, elevation = if (pressed) 2.dp else 6.dp, pressed = pressed)
            )
            .then(if (selected) Modifier.border(1.5.dp, Color(0xFF2563EB), shape) else Modifier)
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
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