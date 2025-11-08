// ui/feed/DateFmt.kt
package com.github.saintedlittle.bunnyviewer.ui.feed

private val ruMonths = arrayOf(
    "янв", "фев", "мар", "апр", "май", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек"
)

expect fun getCurrentTimeMillis(): Long
expect fun parseIsoToMillis(iso: String): Long?
expect fun formatDate(millis: Long): DateComponents

data class DateComponents(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int
)

fun formatPrettyDate(iso: String, nowMillis: Long = getCurrentTimeMillis()): String {
    val millis = parseIsoToMillis(iso) ?: return iso

    val dt = formatDate(millis)
    val today = formatDate(nowMillis)
    val yesterdayMillis = nowMillis - 86400000L // 24 часа в миллисекундах
    val yesterday = formatDate(yesterdayMillis)

    val hh = dt.hour.toString().padStart(2, '0')
    val mm = dt.minute.toString().padStart(2, '0')
    val time = "$hh:$mm"

    return when (dt.year) {
        today.year if dt.month == today.month && dt.day == today.day ->
            "сегодня, $time"
        yesterday.year if dt.month == yesterday.month && dt.day == yesterday.day ->
            "вчера, $time"
        else -> "${dt.day} ${ruMonths[dt.month - 1]} ${dt.year}, $time"
    }
}