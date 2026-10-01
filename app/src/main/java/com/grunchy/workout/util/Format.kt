package com.grunchy.workout.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.round

private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM")
private val fullFmt = DateTimeFormatter.ofPattern("d MMM yyyy")
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

private fun local(ts: Long): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())

/** 60.0 -> "60", 62.5 -> "62.5": shortest possible string, i.e. fewest glyphs to refresh. */
fun fmtNumber(value: Double): String {
    val rounded = round(value * 100) / 100
    val asLong = rounded.toLong()
    return if (rounded == asLong.toDouble()) asLong.toString() else rounded.toString()
}

fun fmtDate(ts: Long): String = local(ts).format(dayFmt)

fun fmtDateFull(ts: Long): String = local(ts).format(fullFmt)

fun fmtTime(ts: Long): String = local(ts).format(timeFmt)

fun fmtDuration(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
