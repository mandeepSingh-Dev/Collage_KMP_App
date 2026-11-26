package com.oem.collagekmpapp.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTimeZone
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

// iosMain/kotlin/com/oem/collagekmpapp/data/CreationsPlatform.ios.kt

private fun projectsDir(): String? {
    val docs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).firstOrNull() as? String
    return docs?.let { "$it/projects" }
}

/** Sidecars live in Documents/projects/<image base name>.json (read them in your iOS CreationsRepository.load()). */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberProjectSidecarStore(): ProjectSidecarStore = remember {
    object : ProjectSidecarStore {
        override suspend fun save(imageFileName: String, json: String): Result<Unit> = runCatching {
            val dir = projectsDir() ?: error("No documents folder")
            NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
            @Suppress("CAST_NEVER_SUCCEEDS")
            val ok = (json as NSString).writeToFile("$dir/${projectSidecarName(imageFileName)}", true, NSUTF8StringEncoding, null)
            check(ok) { "Couldn't save the project file." }
        }

        override suspend fun delete(imageFileName: String) {
            val dir = projectsDir() ?: return
            NSFileManager.defaultManager.removeItemAtPath("$dir/${projectSidecarName(imageFileName)}", null)
        }
    }
}

@Composable
actual fun rememberMultiImageSharer(): MultiImageSharer = remember {
    MultiImageSharer { uris ->
        val items = uris.mapNotNull { u -> if (u.startsWith("/")) NSURL.fileURLWithPath(u) else NSURL.URLWithString(u) }
        if (items.isEmpty()) return@MultiImageSharer
        @Suppress("DEPRECATION")
        var top: UIViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return@MultiImageSharer
        while (top.presentedViewController != null) top = top.presentedViewController!!
        val vc = UIActivityViewController(activityItems = items, applicationActivities = null)
        vc.popoverPresentationController?.sourceView = top.view   // required on iPad
        top.presentViewController(vc, animated = true, completion = null)
    }
}

actual fun platformNowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun platformUtcOffsetMillis(nowMillis: Long): Long =
    NSTimeZone.localTimeZone.secondsFromGMTForDate(NSDate.dateWithTimeIntervalSince1970(nowMillis / 1000.0)) * 1000L