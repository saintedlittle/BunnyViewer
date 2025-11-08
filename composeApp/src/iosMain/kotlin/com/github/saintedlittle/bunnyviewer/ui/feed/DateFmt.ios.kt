package com.github.saintedlittle.bunnyviewer.ui.feed

import platform.Foundation.*

actual fun getCurrentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun parseIsoToMillis(iso: String): Long? {
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'"
        timeZone = NSTimeZone.timeZoneWithName("UTC")!!
    }
    val date = formatter.dateFromString(iso) ?: run {
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        formatter.dateFromString(iso)
    }
    return date?.let { (it.timeIntervalSince1970 * 1000).toLong() }
}

actual fun formatDate(millis: Long): DateComponents {
    val date = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                NSCalendarUnitHour or NSCalendarUnitMinute,
        fromDate = date
    )

    return DateComponents(
        year = components.year.toInt(),
        month = components.month.toInt(),
        day = components.day.toInt(),
        hour = components.hour.toInt(),
        minute = components.minute.toInt()
    )
}