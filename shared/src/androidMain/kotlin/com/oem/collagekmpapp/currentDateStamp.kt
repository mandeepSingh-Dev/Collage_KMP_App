package com.oem.collagekmpapp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// androidMain

actual fun currentDateStamp(pattern: String): String =
    runCatching { SimpleDateFormat(pattern, Locale.getDefault()).format(Date()) }
        .getOrElse { SimpleDateFormat(DEFAULT_WATERMARK_DATE_PATTERN, Locale.getDefault()).format(Date()) }