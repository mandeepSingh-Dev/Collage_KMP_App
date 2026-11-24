package com.oem.collagekmpapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.collagekmpapp.model.Screen

enum class AppTab { Home, Creations, Settings }

@Composable
fun AppBottomBar(
    current: AppTab,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(32.dp),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeumorphicTabItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    selected = current == AppTab.Home,
                    onClick = { if (current != AppTab.Home) onNavigate(Screen.Home) }
                )
                NeumorphicTabItem(
                    icon = Icons.Default.GridView,
                    label = "Creations",
                    selected = current == AppTab.Creations,
                    onClick = { if (current != AppTab.Creations) onNavigate(Screen.Creations) }
                )
                NeumorphicTabItem(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    selected = current == AppTab.Settings,
                    onClick = { if (current != AppTab.Settings) onNavigate(Screen.Settings) }
                )
            }
        }
    }
}

@Composable
private fun NeumorphicTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) Color(0xFFE2E8F0) else Color.Transparent
    val contentColor = if (selected) Color(0xFF1E293B) else Color(0xFF64748B)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        if (selected) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
