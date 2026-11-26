package com.oem.collagekmpapp.firebase

import android.content.Context
import android.os.Build
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

object DeviceTracker {

    fun trackDeviceDetails(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")

                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    val fcmToken = if (task.isSuccessful) task.result ?: "" else ""
                    val dm = context.resources.displayMetrics
                    val pInfo = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
                    val versionCodeVal: Long = pInfo?.longVersionCode ?: 1L

                    val deviceData: Map<String, Any> = mapOf(
                        "device_model" to Build.MODEL,
                        "device_manufacturer" to Build.MANUFACTURER,
                        "android_version" to Build.VERSION.RELEASE,
                        "sdk_int" to Build.VERSION.SDK_INT,
                        "app_version_name" to (pInfo?.versionName ?: "1.0"),
                        "app_version_code" to versionCodeVal,
                        "screen_width_px" to dm.widthPixels,
                        "screen_height_px" to dm.heightPixels,
                        "screen_density_dpi" to dm.densityDpi,
                        "locale" to Locale.getDefault().toString(),
                        "fcm_token" to fcmToken,
                        "last_updated_timestamp" to System.currentTimeMillis()
                    )

                    val deviceId = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.VERSION.SDK_INT}".replace(" ", "_")
                    FirebaseFirestore.getInstance()
                        .collection("users_devices")
                        .document(deviceId)
                        .set(deviceData)
                }
            } catch (_: Exception) {}
        }
    }
}
