package com.oem.collagekmpapp.firebase

import androidx.compose.runtime.Composable

data class UpdateInfo(
    val forceUpdate: Boolean = false,
    val latestVersion: String = "1.0",
    val updateUrl: String = "",
    val updateMessage: String = "A new version of Collage Studio is available. Please update to continue using the app."
)

expect class RemoteConfigManager {
    fun fetchAndCheckUpdate(currentVersionCode: Int, onResult: (UpdateInfo) -> Unit)
}

@Composable
expect fun rememberRemoteConfig(): RemoteConfigManager
