package com.oem.collagekmpapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.oem.collagekmpapp.model.Screen
import com.oem.collagekmpapp.ui.CollageEditorScreen
import com.oem.collagekmpapp.ui.CreationsScreen
import com.oem.collagekmpapp.ui.SavePathScreen
import com.oem.collagekmpapp.ui.FeedbackScreen
import com.oem.collagekmpapp.ui.HomeScreen
import com.oem.collagekmpapp.ui.RemoveBackgroundScreen
import com.oem.collagekmpapp.ui.SettingsScreen
import com.oem.collagekmpapp.ui.SinglePhotoEditorScreen
import com.oem.collagekmpapp.ui.HomeContainer

@Composable
@Preview
fun App() {
    MaterialTheme {
        // global, persisted settings (resolution / format / watermark / ...) used by every export screen
        val keyValueStore = rememberKeyValueStore()
        remember(keyValueStore) { AppSettings.attach(keyValueStore); true }

        var selectedSavePath by remember { mutableStateOf("/Pictures/CollageStudio") }
        val backStack = remember { mutableStateListOf<Screen>(Screen.HomeContainer) }
        val currentScreen = backStack.lastOrNull() ?: Screen.HomeContainer

        BackHandler(enabled = backStack.size > 1) {
            backStack.removeLastOrNull()
        }

        when (val screen = currentScreen) {
            is Screen.HomeContainer -> {
                HomeContainer(
                    selectedSavePath = selectedSavePath,
                    onNavigate = { targetScreen ->
                        backStack.add(targetScreen)
                    }
                )
            }
            is Screen.Home -> {
                HomeScreen(
                    onNavigate = { targetScreen ->
                        backStack.add(targetScreen)
                    }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    selectedSavePath = selectedSavePath,
                    onNavigate = { targetScreen ->
                        backStack.add(targetScreen)
                    }
                )
            }
            is Screen.SavePath -> {
                SavePathScreen(
                    selectedPath = screen.selectedPath,
                    onBack = { backStack.removeLastOrNull() },
                    onSelect = { path ->
                        selectedSavePath = path
                        backStack.removeLastOrNull()
                    }
                )
            }
            is Screen.Feedback -> {
                FeedbackScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigate = { targetScreen -> backStack.add(targetScreen) }
                )
            }
            is Screen.Creations -> {
                CreationsScreen(
                    savePath = selectedSavePath,
                    onNavigate = { targetScreen -> backStack.add(targetScreen) }
                )
            }
            is Screen.LayoutSelection -> {
                val count = screen.selectedPhotos.size.coerceAtLeast(1)
                val defaultLayoutId = com.oem.collagekmpapp.data.LayoutRepository().getLayouts(count).firstOrNull()?.id ?: "layout_2_vertical"
                CollageEditorScreen(
                    layoutId = defaultLayoutId,
                    selectedPhotos = screen.selectedPhotos,
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            is Screen.Editor -> {
                CollageEditorScreen(
                    layoutId = screen.layoutId,
                    selectedPhotos = screen.selectedPhotos,
                    templateId = screen.templateId,
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            is Screen.FreestyleEditor -> {
                FreestyleEditorScreen(
                    selectedPhotos = screen.selectedPhotos,
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            is Screen.EditPhoto -> {
                SinglePhotoEditorScreen(
                    photoUri = screen.photoUri,
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            is Screen.RemoveBackground -> {
                RemoveBackgroundScreen(
                    photoUri = screen.photoUri,
                    onBack = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        }
    }
}