package com.oem.collagekmpapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oem.collagekmpapp.NeuColors
import com.oem.collagekmpapp.model.Screen
import com.oem.collagekmpapp.neumorphic
import com.oem.collagekmpapp.neumorphicInset

enum class AppTab { Home, Creations, Settings }

/**
 * Premium animated neumorphic tab bar:
 *  - raised neumorphic bar with soft dual drop shadow
 *  - a sunken, accent-tinted "pill" that SLIDES (spring) to the active tab
 *  - the active icon scales up and lifts, colour cross-fades, label fades/slides in
 *  - tactile press state on every tab
 */
@Composable
fun AppBottomBar(
    current: AppTab,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Triple(AppTab.Home, Icons.Default.Home, "Home"),
        Triple(AppTab.Creations, Icons.Default.GridView, "Creations"),
        Triple(AppTab.Settings, Icons.Default.Settings, "Settings")
    )
    val barShape = RoundedCornerShape(32.dp)
    val pillShape = RoundedCornerShape(24.dp)
    val innerPad = 8.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .neumorphic(shape = barShape, elevation = 10.dp)
                .padding(innerPad)
        ) {
            val cellWidth = maxWidth / tabs.size
            val index = tabs.indexOfFirst { it.first == current }.coerceAtLeast(0)
            val pillX by animateDpAsState(
                targetValue = cellWidth * index,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
            )

            // sliding active indicator
            Box(
                modifier = Modifier
                    .offset(x = pillX)
                    .width(cellWidth)
                    .height(60.dp)
                    .padding(horizontal = 4.dp)
                    .neumorphicInset(shape = pillShape)
                    .background(NeuColors.Accent.copy(alpha = 0.10f), pillShape)
            )

            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { (tab, icon, label) ->
                    NeumorphicTabItem(
                        icon = icon,
                        label = label,
                        selected = current == tab,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (current != tab) onNavigate(
                                when (tab) {
                                    AppTab.Home -> Screen.Home
                                    AppTab.Creations -> Screen.Creations
                                    AppTab.Settings -> Screen.Settings
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NeumorphicTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val contentColor by animateColorAsState(
        if (selected) NeuColors.Accent else NeuColors.TextMuted, tween(260)
    )
    val iconScale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.88f
            selected -> 1.22f
            else -> 1f
        },
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
    )
    val lift by animateDpAsState(if (selected) (-5).dp else 0.dp, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
    val labelAlpha by animateFloatAsState(if (selected) 1f else 0f, tween(220))
    val labelHeight by animateDpAsState(if (selected) 14.dp else 0.dp, tween(220))

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier
                .size(24.dp)
                .offset(y = lift)
                .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
        )
        Box(modifier = Modifier.height(labelHeight).offset(y = lift), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.graphicsLayer { alpha = labelAlpha }
            )
        }
    }
}