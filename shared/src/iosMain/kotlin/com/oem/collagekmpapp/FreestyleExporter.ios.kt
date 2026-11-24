package com.oem.collagekmpapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberFreestyleExporter(): FreestyleExporter = remember {
    FreestyleExporter { _, _, _, onResult ->
        onResult(Result.failure(UnsupportedOperationException("Freestyle export is not implemented on iOS yet.")))
    }
}
