package com.oem.collagekmpapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * iOS actual for the Magic Wand / AI Auto-Cutout pixel source (iosMain).
 * Stub for now: returns null, so the screen shows "Couldn't read the photo" instead of crashing.
 * To enable on iOS: decode the UIImage upright, draw it into a CGBitmapContext of at most maxSide px
 * (kCGImageAlphaPremultipliedLast), and convert RGBA bytes to ARGB ints for MaskPixels.
 */
@Composable
actual fun rememberPhotoPixelLoader(): PhotoPixelLoader = remember {
    object : PhotoPixelLoader {
        override suspend fun load(uri: String, maxSide: Int): MaskPixels? = null
    }
}