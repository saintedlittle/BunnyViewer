package com.github.saintedlittle.bunnyviewer.ui.feed

import java.text.SimpleDateFormat
import java.util.*

actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

actual fun parseIsoToMillis(iso: String): Long? {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        format.parse(iso)?.time
    } catch (e: Exception) {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(iso)?.time
        } catch (e: Exception) {
            null
        }
    }
}

actual fun formatDate(millis: Long): DateComponents {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = millis
    }
    return DateComponents(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH),
        hour = calendar.get(Calendar.HOUR_OF_DAY),
        minute = calendar.get(Calendar.MINUTE)
    )
}