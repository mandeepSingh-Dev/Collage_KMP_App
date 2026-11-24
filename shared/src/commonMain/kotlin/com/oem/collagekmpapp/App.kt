package com.oem.collagekmpapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.oem.collagekmpapp.model.Screen
import com.oem.collagekmpapp.ui.CollageEditorScreen
import com.oem.collagekmpapp.ui.SavePathScreen
import com.oem.collagekmpapp.ui.FeedbackScreen
import com.oem.collagekmpapp.ui.HomeScreen
import com.oem.collagekmpapp.ui.RemoveBackgroundScreen
import com.oem.collagekmpapp.ui.SettingsScreen
import com.oem.collagekmpapp.ui.SinglePhotoEditorScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
        var selectedSavePath by remember { mutableStateOf("/Pictures/CollageStudio") }

        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    onNavigate = { targetScreen ->
                        currentScreen = targetScreen
                    }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    selectedSavePath = selectedSavePath,
                    onNavigate = { targetScreen ->
                        currentScreen = targetScreen
                    }
                )
            }
            is Screen.SavePath -> {
                SavePathScreen(
                    selectedPath = screen.selectedPath,
                    onBack = { currentScreen = Screen.Settings },
                    onSelect = { path ->
                        selectedSavePath = path
                        currentScreen = Screen.Settings
                    }
                )
            }
            is Screen.Feedback -> {
                FeedbackScreen(
                    onBack = { currentScreen = Screen.Settings },
                    onNavigate = { targetScreen -> currentScreen = targetScreen }
                )
            }
            is Screen.LayoutSelection -> {
                val count = screen.selectedPhotos.size.coerceAtLeast(1)
                val defaultLayoutId = com.oem.collagekmpapp.data.LayoutRepository().getLayouts(count).firstOrNull()?.id ?: "layout_2_vertical"
                currentScreen = Screen.Editor(defaultLayoutId, screen.selectedPhotos)
            }
            is Screen.Editor -> {
                CollageEditorScreen(
                    layoutId = screen.layoutId,
                    selectedPhotos = screen.selectedPhotos,
                    onBack = {
                        currentScreen = Screen.Home
                    }
                )
            }
            is Screen.FreestyleEditor -> {
                FreestyleEditorScreen(
                    selectedPhotos = screen.selectedPhotos,
                    onBack = {
                        currentScreen = Screen.Home
                    }
                )
            }
            is Screen.EditPhoto -> {
                SinglePhotoEditorScreen(
                    photoUri = screen.photoUri,
                    onBack = {
                        currentScreen = Screen.Home
                    }
                )
            }
            is Screen.RemoveBackground -> {
                RemoveBackgroundScreen(
                    photoUri = screen.photoUri,
                    onBack = {
                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}