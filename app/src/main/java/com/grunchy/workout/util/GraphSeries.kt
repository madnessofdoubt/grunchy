package com.grunchy.workout.util

import com.grunchy.workout.model.Session
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * The windows the graph offers.
 *
 * A training log is kept for years, and drawing all of it at once answers nothing: the question is
 * always whether you are going the right way *lately*. So the graph opens on a window and the user
 * widens it when they want the whole story.
 */
enum class GraphRange(val label: String, val days: Int?) {
    Month("Last month", 31),
    ThreeMonths("Last 3 months", 92),
    SixMonths("Last 6 months", 183),
    Year("Last year", 366),
    All("All time", null);

    /**
     * The first moment inside the window, or null for everything on record. Counted in calendar
     * days rather than a fixed multiple of 24 hours, so "last month" means the same thing in March
     * as it does in February.
     */
    fun startFrom(now: Long, zone: ZoneId = ZoneId.systemDefault()): Long? = days?.let { day ->
        Instant.ofEpochMilli(now)
            .atZone(zone)
            .toLocalDate()
            .minusDays(day.toLong() - 1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }
}

/** How the drawn points are spaced, once history is thick enough to need spacing. */
enum class GraphBucket { Session, Week, Month }

/** A series ready to draw, plus what the caption needs to describe it honestly. */
data class GraphSeries(
    val points: List<ExercisePoint>,
    val bucket: GraphBucket,
    /** Sessions inside the window before thinning — what the caption counts. */
    val sessions: Int,
    /** Sessions in the window that have no estimate (every set over [MaxRepsForE1rm] reps). */
    val skipped: Int,
)

/**
 * Most points worth putting on a 336 dp wide chart: about 54, which is six dp apart and still
 * reads as a line. Set here rather than lower so that a year of training three times a week —
 * roughly fifty weekly buckets — stays weekly instead of collapsing to twelve months.
 */
const val MaxGraphPoints = 54

/**
 * The points to draw for one exercise: chronological, inside [range], estimated 1RM only.
 *
 * A session where every set went past [MaxRepsForE1rm] reps has no honest number for this axis, so
 * it is left out rather than faked from the top set. [GraphSeries.skipped] counts those, and the
 * caption says so — silently dropping them would flatter the line.
 */
fun graphPoints(
    exerciseId: String,
    sessions: List<Session>,
    range: GraphRange,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): GraphSeries {
    val from = range.startFrom(now, zone)
    val window = pointsFor(exerciseId, sessions).filter { from == null || it.date >= from }
    val plotted = window.filter { it.e1rmKg != null }.sortedBy { it.date }
    return thinForGraph(plotted, window.size - plotted.size, zone)
}

/**
 * Thins a series so the chart stays readable, while keeping the shape of the trend.
 *
 * Each bucket is represented by its best estimate — which is what a lifter means by "where I was
 * that week". Weeks first, then months: five years of weekly training lands on sixty monthly
 * points, which a 336 dp chart still draws as a line. The representative keeps its own date, so
 * the chart never invents one.
 */
fun thinForGraph(
    points: List<ExercisePoint>,
    skipped: Int = 0,
    zone: ZoneId = ZoneId.systemDefault(),
): GraphSeries {
    if (points.size <= MaxGraphPoints) {
        return GraphSeries(points, GraphBucket.Session, points.size + skipped, skipped)
    }
    val weeks = bucket(points, zone) { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    if (weeks.size <= MaxGraphPoints) {
        return GraphSeries(weeks, GraphBucket.Week, points.size + skipped, skipped)
    }
    val months = bucket(points, zone) { it.withDayOfMonth(1) }
    return GraphSeries(months, GraphBucket.Month, points.size + skipped, skipped)
}

/** One pass over a chronological series, keeping the best estimate in each bucket. */
private fun bucket(
    points: List<ExercisePoint>,
    zone: ZoneId,
    keyOf: (LocalDate) -> LocalDate,
): List<ExercisePoint> = points
    .groupBy { keyOf(it.localDate(zone)) }
    .toSortedMap()
    .values
    .map { group -> group.maxBy { it.e1rmKg ?: 0.0 } }

/**
 * The day a point was logged, in the reader's own time zone.
 *
 * Via [Instant.atZone] rather than the tidier `LocalDate.ofInstant(instant, zone)`: that overload
 * arrived in Java 9 and on Android it needs API 33, while the Kompakt runs API 31. It compiles,
 * passes JVM unit tests, and throws `NoSuchMethodError` on the device.
 */
fun ExercisePoint.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(date).atZone(zone).toLocalDate()
