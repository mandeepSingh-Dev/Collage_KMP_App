package com.oem.collagekmpapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.oem.collagekmpapp.model.Screen

enum class AppTab { Home, Creations, Settings }

/**
 * The one bottom navigation bar shared by Home, Creations, Settings and Feedback.
 * Pass the tab the current screen belongs to (Feedback belongs to [AppTab.Settings]).
 */
@Composable
fun AppBottomBar(
    current: AppTab,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 12.dp, horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(Icons.Default.Home, "Home", current == AppTab.Home) {
                if (current != AppTab.Home) onNavigate(Screen.Home)
            }
            BottomNavItem(Icons.Default.GridView, "Creations", current == AppTab.Creations) {
                if (current != AppTab.Creations) onNavigate(Screen.Creations)
            }
            BottomNavItem(Icons.Default.Settings, "Settings", current == AppTab.Settings) {
                if (current != AppTab.Settings) onNavigate(Screen.Settings)
            }
        }
    }
}