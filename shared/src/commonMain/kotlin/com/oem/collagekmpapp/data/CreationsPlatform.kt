package com.oem.collagekmpapp.data

import androidx.compose.runtime.Composable

// commonMain/kotlin/com/oem/collagekmpapp/data/CreationsPlatform.kt
// (kept separate from CreationsRepository.kt: a common and an android file with the same name and package
//  that both contain top-level functions would clash as the same JVM class "CreationsRepositoryKt")

// =====================================================================================
//  Batch delete
// =====================================================================================

/** Outcome of [deleteBatch]: ids that are gone, plus every item that couldn't be deleted and why. */
data class BatchDeleteResult(
    val deletedIds: Set<String>,
    val failed: List<Pair<Creation, Throwable>>
) {
    val allSucceeded: Boolean get() = failed.isEmpty()
}

/** Deletes several creations one by one through the existing single-item [CreationsRepository.delete]. */
suspend fun CreationsRepository.deleteBatch(creations: List<Creation>): BatchDeleteResult {
    val deleted = LinkedHashSet<String>()
    val failed = ArrayList<Pair<Creation, Throwable>>()
    for (c in creations) {
        delete(c)
            .onSuccess { deleted += c.id }
            .onFailure { failed += c to it }
    }
    return BatchDeleteResult(deleted, failed)
}

// =====================================================================================
//  Batch share
// =====================================================================================

/** Shares several images in ONE system share sheet (Android: ACTION_SEND_MULTIPLE). */
fun interface MultiImageSharer {
    fun share(uris: List<String>)
}

@Composable
expect fun rememberMultiImageSharer(): MultiImageSharer

// =====================================================================================
//  Project sidecars (re-editable projects)
// =====================================================================================

/**
 * Saves / removes the project JSON that belongs to an exported image.
 * Exporters call [save] right after writing the image, with the image's file name
 * (e.g. CreationKind.COLLAGE.newFileName("png", ts)) and CreationProject(...).toJson().
 */
interface ProjectSidecarStore {
    suspend fun save(imageFileName: String, json: String): Result<Unit>
    suspend fun delete(imageFileName: String)
}

@Composable
expect fun rememberProjectSidecarStore(): ProjectSidecarStore

/** `Collage_1764000000000.png` -> `Collage_1764000000000.json` */
fun projectSidecarName(imageFileName: String): String = imageFileName.substringBeforeLast('.') + ".json"

// =====================================================================================
//  Date range filter
// =====================================================================================

expect fun platformNowMillis(): Long

/** Local time-zone offset from UTC at [nowMillis] (includes DST). */
expect fun platformUtcOffsetMillis(nowMillis: Long): Long

enum class CreationDateRange(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),     // since Monday 00:00 local time
    THIS_MONTH("This Month");   // since the 1st, 00:00 local time

    /** Inclusive start of the range in UTC millis, or null for [ALL_TIME]. */
    fun startMillis(nowMillis: Long, utcOffsetMillis: Long): Long? {
        if (this == ALL_TIME) return null
        val dayMs = 86_400_000L
        val localDay = (nowMillis + utcOffsetMillis).floorDiv(dayMs)   // days since 1970-01-01, local
        val startDay = when (this) {
            TODAY -> localDay
            THIS_WEEK -> localDay - (localDay + 3).mod(7L)              // 1970-01-01 was a Thursday
            THIS_MONTH -> localDay - (dayOfMonth(localDay) - 1)
            ALL_TIME -> localDay
        }
        return startDay * dayMs - utcOffsetMillis
    }

    fun matches(dateMillis: Long, nowMillis: Long, utcOffsetMillis: Long): Boolean {
        val start = startMillis(nowMillis, utcOffsetMillis) ?: return true
        return dateMillis >= start
    }

    private fun dayOfMonth(daysSinceEpoch: Long): Long {
        val z = daysSinceEpoch + 719_468L
        val era = (if (z >= 0) z else z - 146_096L) / 146_097L
        val doe = z - era * 146_097L
        val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        return doy - (153L * mp + 2L) / 5L + 1L
    }
}