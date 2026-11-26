package com.oem.collagekmpapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.oem.collagekmpapp.LightThemeIsland
import com.oem.collagekmpapp.NeuColors
import com.oem.collagekmpapp.model.Screen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeContainer(
    selectedSavePath: String,
    onNavigate: (Screen) -> Unit,
    shortcutAction: String? = null,
    onShortcutActionConsumed: () -> Unit = {}
) {
    var currentTab by remember { mutableStateOf(AppTab.Home) }

    LaunchedEffect(shortcutAction) {
        if (shortcutAction == "resume_draft") {
            currentTab = AppTab.Creations
            onShortcutActionConsumed()
        }
    }

    Scaffold(
        // themed backdrop: in Dark mode the floating tab bar no longer sits on a white strip
        containerColor = NeuColors.Background,
        bottomBar = {
            AppBottomBar(
                current = currentTab,
                onNavigate = { screen ->
                    when (screen) {
                        is Screen.Home -> currentTab = AppTab.Home
                        is Screen.Creations -> currentTab = AppTab.Creations
                        is Screen.Settings -> currentTab = AppTab.Settings
                        else -> onNavigate(screen)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // the tabs have their own Scaffold / status-bar padding -> don't add the system bars twice
                .consumeWindowInsets(innerPadding)
        ) {
            when (currentTab) {
                AppTab.Home -> HomeScreen(
                    onNavigate = onNavigate,
                    shortcutAction = shortcutAction,
                    onShortcutActionConsumed = onShortcutActionConsumed
                )
                // Creations is still hard-coded light -> keep its Material parts light too
                AppTab.Creations -> LightThemeIsland {
                    CreationsScreen(savePath = selectedSavePath, onNavigate = onNavigate)
                }
                AppTab.Settings -> SettingsScreen(selectedSavePath = selectedSavePath, onNavigate = onNavigate)
            }
        }
    }
}