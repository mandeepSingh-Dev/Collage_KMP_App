package com.oem.collagekmpapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.oem.collagekmpapp.export.CollageExporter
import kotlinx.coroutines.launch

@Composable
actual fun rememberFreestyleExporter(): FreestyleExporter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context) {
        FreestyleExporter { project, longSide, format, onResult ->
            scope.launch {
                val result = CollageExporter.exportFreestyle(context, project, longSide, format)
                onResult(result.map { "Pictures/CollageStudio" })
            }
        }
    }
}
