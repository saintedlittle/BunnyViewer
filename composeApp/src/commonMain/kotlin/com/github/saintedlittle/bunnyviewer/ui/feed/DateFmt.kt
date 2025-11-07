// ui/feed/DateFmt.kt
package com.github.saintedlittle.bunnyviewer.ui.feed

import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val ruMonths = arrayOf(
    "янв", "фев", "мар", "апр", "май", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек"
)

@OptIn(ExperimentalTime::class)
fun formatPrettyDate(iso: String, now: kotlin.time.Instant = Clock.System.now()): String {
    val instant = runCatching { kotlin.time.Instant.parse(iso) }.getOrNull() ?: return iso
    val tz = TimeZone.currentSystemDefault()
    val dt = instant.toLocalDateTime(tz)
    val today = now.toLocalDateTime(tz).date
    val yesterday = today.minus(DatePeriod(days = 1))

    val hh = dt.hour.toString().padStart(2, '0')
    val mm = dt.minute.toString().padStart(2, '0')
    val time = "$hh:$mm"

    return when (dt.date) {
        today -> "сегодня, $time"
        yesterday -> "вчера, $time"
        else -> "${dt.dayOfMonth} ${ruMonths[dt.monthNumber - 1]} ${dt.year}, $time"
    }
}
