package com.oem.collagekmpapp

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

// iosMain

actual fun currentDateStamp(pattern: String): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = pattern
    return formatter.stringFromDate(NSDate())
}