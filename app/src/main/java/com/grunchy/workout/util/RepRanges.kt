package com.grunchy.workout.util

import com.grunchy.workout.model.AmrapLabel
import com.grunchy.workout.model.RepRange

/**
 * Rep range helpers. Ranges are stored in a routine by their label, so everything here has
 * to survive a range being edited or deleted in Settings.
 */

/** The number a new set starts on for a stored range label, e.g. "5-10" -> 8. */
fun prefillRepsFor(label: String, ranges: List<RepRange>): Int {
    ranges.firstOrNull { it.label == label }?.let { return it.prefillReps }
    // The range no longer exists (edited or deleted): fall back to the label itself.
    return parseRangeLabel(label)?.prefillReps ?: DefaultPrefillReps
}

/** "5-10" -> RepRange(5, 10); "15+" -> RepRange(15, null); "AMRAP" -> amrap; else null. */
fun parseRangeLabel(label: String): RepRange? {
    val trimmed = label.trim()
    if (trimmed.equals(AmrapLabel, ignoreCase = true)) return RepRange(amrap = true)
    if (trimmed.endsWith("+")) {
        val min = trimmed.dropLast(1).trim().toIntOrNull() ?: return null
        return RepRange(min, null)
    }
    val parts = trimmed.split("-")
    if (parts.size != 2) return null
    val min = parts[0].trim().toIntOrNull() ?: return null
    val max = parts[1].trim().toIntOrNull() ?: return null
    return RepRange(min, max)
}

const val DefaultPrefillReps = 8

/**
 * Adds [added] unless an identical range is already there. Ranges are kept in reading
 * order: by lower bound, and where two share one, the narrower range first — so 1-3 sorts
 * ahead of 1-5, and an open ended "15+" sorts last of its group.
 */
fun withRepRange(ranges: List<RepRange>, added: RepRange): List<RepRange> {
    val normalized = added.normalized()
    if (ranges.any { it.label == normalized.label }) return ranges
    return (ranges + normalized).sortedWith(rangeOrder)
}

// AMRAP sorts last: it has no bounds, so it cannot take its place among the numeric ranges.
private val rangeOrder = compareBy<RepRange>({ it.amrap }, { it.minReps }, { it.maxReps ?: Int.MAX_VALUE })

/** Replaces the range at [index] after normalising it, dropping it if that collides. */
fun replaceRepRange(ranges: List<RepRange>, index: Int, changed: RepRange): List<RepRange> {
    if (index !in ranges.indices) return ranges
    val normalized = changed.normalized()
    val others = ranges.filterIndexed { i, _ -> i != index }
    if (others.any { it.label == normalized.label }) return others
    return (others + normalized).sortedWith(rangeOrder)
}

/** A sensible range to offer when the user taps "Add range": the next block of five. */
fun nextRepRange(ranges: List<RepRange>): RepRange {
    // AMRAP has no numeric bound, so it does not drag the next block of five upwards.
    val highest = ranges.filterNot { it.amrap }.mapNotNull { it.maxReps ?: it.minReps }.maxOrNull() ?: 0
    return RepRange(highest + 1, highest + 5)
}

/** The range a newly added exercise starts on: the classic 5-10 if it still exists. */
fun defaultRepRangeLabel(ranges: List<RepRange>): String =
    ranges.firstOrNull { it.label == "5-10" }?.label ?: ranges.firstOrNull()?.label ?: "5-10"
