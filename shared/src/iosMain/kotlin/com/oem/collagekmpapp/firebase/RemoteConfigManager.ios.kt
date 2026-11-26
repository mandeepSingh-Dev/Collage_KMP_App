package com.oem.collagekmpapp.firebase

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class RemoteConfigManager {
    actual fun fetchAndCheckUpdate(currentVersionCode: Int, onResult: (UpdateInfo) -> Unit) {
        onResult(UpdateInfo())
    }
}

@Composable
actual fun rememberRemoteConfig(): RemoteConfigManager = remember { RemoteConfigManager() }
