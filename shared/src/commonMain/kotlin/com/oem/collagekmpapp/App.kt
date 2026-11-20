package com.oem.collagekmpapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import com.oem.collagekmpapp.model.Screen
import com.oem.collagekmpapp.ui.CollageEditorScreen
import com.oem.collagekmpapp.ui.HomeScreen

@Composable
@Preview
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    onNavigate = { targetScreen ->
                        currentScreen = targetScreen
                    }
                )
            }
            is Screen.LayoutSelection -> {
                // Directly open the unified Collage Editor Studio with default layout
                currentScreen = Screen.Editor("layout_2_vertical", screen.selectedPhotos)
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
        }
    }
}
