package com.oem.collagekmpapp

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

// androidMain

/**
 * Re-applies edge-to-edge whenever the APP theme flips (Settings / Home toggle), so status- and
 * navigation-bar icons stay light on dark screens and dark on light screens - even when the app theme
 * differs from the phone's system theme. Call it inside setContent { }, next to App().
 */
@Composable
fun SystemBarsThemeSync(activity: ComponentActivity) {
    val dark = NeuColors.isDark
    LaunchedEffect(dark) {
        val style = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}