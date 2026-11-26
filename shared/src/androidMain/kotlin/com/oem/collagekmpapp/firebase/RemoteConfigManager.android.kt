package com.oem.collagekmpapp.firebase

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

actual class RemoteConfigManager(private val context: Context) {
    private val remoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            setConfigSettingsAsync(configSettings)
            setDefaultsAsync(
                mapOf(
                    "min_required_version_code" to 1L,
                    "latest_version_name" to "1.0",
                    "force_update_message" to "A new update is available. Please update Collage Studio to continue.",
                    "update_url" to "https://play.google.com/store/apps/details?id=com.oem.collagekmpapp"
                )
            )
        }
    }

    actual fun fetchAndCheckUpdate(currentVersionCode: Int, onResult: (UpdateInfo) -> Unit) {
        try {
            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                val minRequiredCode = remoteConfig.getLong("min_required_version_code").toInt()
                val latestVersion = remoteConfig.getString("latest_version_name")
                val message = remoteConfig.getString("force_update_message")
                val url = remoteConfig.getString("update_url")

                val force = currentVersionCode < minRequiredCode
                onResult(
                    UpdateInfo(
                        forceUpdate = force,
                        latestVersion = latestVersion,
                        updateUrl = url,
                        updateMessage = message
                    )
                )
            }
        } catch (_: Exception) {
            onResult(UpdateInfo())
        }
    }
}

@Composable
actual fun rememberRemoteConfig(): RemoteConfigManager {
    val context = LocalContext.current
    return remember(context) { RemoteConfigManager(context) }
}
