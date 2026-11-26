package com.oem.collagekmpapp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import com.oem.collagekmpapp.ui.LocalIsTopScreen

/** What AnimatedContent animates between: the screen + its back-stack depth (depth tells push from pop). */
private data class NavEntry(val screen: Screen, val depth: Int)

private const val NAV_ANIM_MS = 320
private const val NAV_FADE_MS = 220

/**
 * push  (depth grows):   new screen slides in from the right, old one drifts 1/3 left + fades.
 * pop   (depth shrinks): previous screen slides back in from 1/3 left, current one slides out right.
 * replace (same depth, e.g. Splash -> Onboarding -> HomeContainer): plain cross-fade.
 */
private fun navTransition(from: NavEntry, to: NavEntry): ContentTransform = when {
    to.depth > from.depth ->
        (slideInHorizontally(tween(NAV_ANIM_MS, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(NAV_FADE_MS)))
            .togetherWith(
                slideOutHorizontally(tween(NAV_ANIM_MS, easing = FastOutSlowInEasing)) { -it / 3 } + fadeOut(tween(NAV_FADE_MS))
            )
    to.depth < from.depth ->
        (slideInHorizontally(tween(NAV_ANIM_MS, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn(tween(NAV_FADE_MS)))
            .togetherWith(
                slideOutHorizontally(tween(NAV_ANIM_MS, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(NAV_FADE_MS))
            )
            // keep the screen that is leaving ON TOP while it slides away (otherwise it pops behind = flicker)
            .apply { targetContentZIndex = -1f }
    else -> fadeIn(tween(NAV_ANIM_MS)).togetherWith(fadeOut(tween(NAV_ANIM_MS)))
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        // global, persisted settings (resolution / format / watermark / ...) used by every export screen
        val keyValueStore = rememberKeyValueStore()
        remember(keyValueStore) { AppSettings.attach(keyValueStore); true }
        SyncNeuSystemTheme()   // keeps NeuColors' "System" theme in sync with the OS (from the theme update)

        var selectedSavePath by remember { mutableStateOf("/Pictures/CollageStudio") }
        val backStack = remember { mutableStateListOf<Screen>(Screen.Splash) }
        val currentScreen = backStack.lastOrNull() ?: Screen.HomeContainer

        BackHandler(enabled = backStack.size > 1) {
            backStack.removeLastOrNull()
        }

        /**
         * Pops only if [entry] is still the top screen. A screen that is already animating out keeps its
         * callbacks for ~300 ms, so a quick double tap on "back" can't pop two screens.
         */
        fun popIfTop(entry: Screen) {
            if (backStack.lastOrNull() === entry) backStack.removeLastOrNull()
        }

        AnimatedContent(
            targetState = NavEntry(currentScreen, backStack.size),
            transitionSpec = { navTransition(initialState, targetState) },
            label = "screenNav"
        ) { entry ->
            val screen = entry.screen
            // opaque backdrop per screen so two screens never show through each other while cross-fading
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                CompositionLocalProvider(
                    // the outgoing screen gets false -> its editor BackHandler switches off immediately
                    LocalIsTopScreen provides (backStack.lastOrNull() === screen)
                ) {
                    when (screen) {
                        is Screen.Splash -> {
                            SplashScreen(
                                onFinished = {
                                    // first launch -> onboarding, afterwards straight to the home container
                                    val seen = keyValueStore.getString(ONBOARDING_DONE_KEY, "0") == "1"
                                    backStack.clear()
                                    backStack.add(if (seen) Screen.HomeContainer else Screen.Onboarding)
                                }
                            )
                        }
                        is Screen.Onboarding -> {
                            OnboardingScreen(
                                onFinished = {
                                    keyValueStore.putString(ONBOARDING_DONE_KEY, "1")
                                    backStack.clear()
                                    backStack.add(Screen.HomeContainer)
                                }
                            )
                        }
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
                                onBack = { popIfTop(screen) },
                                onSelect = { path ->
                                    selectedSavePath = path
                                    popIfTop(screen)
                                }
                            )
                        }
                        is Screen.Feedback -> {
                            FeedbackScreen(
                                onBack = { popIfTop(screen) },
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
                                onBack = { popIfTop(screen) }
                            )
                        }
                        is Screen.Editor -> {
                            CollageEditorScreen(
                                layoutId = screen.layoutId,
                                selectedPhotos = screen.selectedPhotos,
                                templateId = screen.templateId,
                                onBack = { popIfTop(screen) }
                            )
                        }
                        is Screen.FreestyleEditor -> {
                            FreestyleEditorScreen(
                                selectedPhotos = screen.selectedPhotos,
                                onBack = { popIfTop(screen) }
                            )
                        }
                        is Screen.EditPhoto -> {
                            SinglePhotoEditorScreen(
                                photoUri = screen.photoUri,
                                onBack = { popIfTop(screen) }
                            )
                        }
                        is Screen.RemoveBackground -> {
                            RemoveBackgroundScreen(
                                photoUri = screen.photoUri,
                                onBack = { popIfTop(screen) }
                            )
                        }
                    }
                }
            }
        }
    }
}