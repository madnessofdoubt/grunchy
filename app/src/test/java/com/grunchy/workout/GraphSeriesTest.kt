package com.grunchy.workout

import com.grunchy.workout.model.ExerciseLog
import com.grunchy.workout.model.Session
import com.grunchy.workout.model.SetLog
import com.grunchy.workout.util.GraphBucket
import com.grunchy.workout.util.GraphRange
import com.grunchy.workout.util.MaxGraphPoints
import com.grunchy.workout.util.graphPoints
import com.grunchy.workout.util.localDate
import com.grunchy.workout.util.thinForGraph
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The graph's data: which sessions land in a window, and how thick history gets thinned. */
class GraphSeriesTest {

    private val zone: ZoneId = ZoneId.of("Europe/Sofia")
    private val now = at(2026, 9, 30)

    private fun at(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(year, month, day, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

    /** A session that trains [exerciseId] with one set, so the e1RM is that set's estimate. */
    private fun session(on: Long, weightKg: Double, reps: Int, exerciseId: String = "squat") =
        Session(
            id = "s-$on-$weightKg",
            title = "Session",
            startedAt = on,
            finishedAt = on,
            logs = listOf(ExerciseLog(exerciseId, listOf(SetLog(weightKg, reps)))),
        )

    private val sessions = listOf(
        session(at(2026, 9, 28), 80.0, 5),
        session(at(2026, 9, 14), 77.5, 5),
        session(at(2026, 8, 20), 75.0, 5),
        // Outside a three-month window counted back from 30 September.
        session(at(2026, 5, 1), 70.0, 5),
        // Two years back: only "all time" reaches it.
        session(at(2024, 9, 1), 60.0, 5),
    )

    @Test
    fun `windows keep the sessions inside them and drop the rest`() {
        assertEquals(3, graphPoints("squat", sessions, GraphRange.ThreeMonths, now, zone).points.size)
        assertEquals(4, graphPoints("squat", sessions, GraphRange.Year, now, zone).points.size)
        assertEquals(5, graphPoints("squat", sessions, GraphRange.All, now, zone).points.size)
    }

    @Test
    fun `a month window starts one calendar month back, not thirty times 24 hours`() {
        // 31 days: the 30th of August is inside, the 29th is not.
        assertEquals(2, graphPoints("squat", sessions, GraphRange.Month, now, zone).points.size)
    }

    @Test
    fun `points come out oldest first, because a chart reads left to right`() {
        val dates = graphPoints("squat", sessions, GraphRange.All, now, zone).points.map { it.date }
        assertEquals(dates.sorted(), dates)
    }

    @Test
    fun `sessions with no estimable set are counted, not plotted`() {
        val highReps = sessions + session(at(2026, 9, 25), 30.0, 20)
        val series = graphPoints("squat", highReps, GraphRange.ThreeMonths, now, zone)
        assertEquals(3, series.points.size)
        assertEquals(1, series.skipped)
        assertEquals(4, series.sessions)
    }

    @Test
    fun `another exercise's sessions never appear`() {
        val mixed = sessions + session(at(2026, 9, 27), 100.0, 3, exerciseId = "bench")
        assertEquals(5, graphPoints("squat", mixed, GraphRange.All, now, zone).points.size)
    }

    @Test
    fun `a thin series is drawn session by session`() {
        val series = graphPoints("squat", sessions, GraphRange.All, now, zone)
        assertEquals(GraphBucket.Session, series.bucket)
        assertEquals(5, series.points.size)
    }

    @Test
    fun `two years of training is thinned to one point a week`() {
        // Every other day for a year: far more points than the chart can show.
        val dense = (0 until 180).map { session(at(2026, 9, 30) - it * 2L * 86_400_000L, 60.0 + it % 7, 5) }
        val series = graphPoints("squat", dense, GraphRange.All, now, zone)
        assertEquals(GraphBucket.Week, series.bucket)
        assertTrue("weekly: ${series.points.size}", series.points.size in 26..MaxGraphPoints)
        assertTrue("a year of weekly points must not need months", series.points.size > 40)
        assertEquals(180, series.sessions)
    }

    @Test
    fun `every bucket keeps its best estimate, so the trend keeps its high points`() {
        // Three sessions in the week of 14 September; the middle one is the best.
        val week = listOf(
            session(at(2026, 9, 15), 70.0, 5),
            session(at(2026, 9, 17), 90.0, 5),
            session(at(2026, 9, 19), 80.0, 5),
        )
        val thinned = thinForGraph(
            (week + (0 until 60).map { session(at(2026, 9, 30) - it * 3L * 86_400_000L, 50.0, 5) })
                .mapNotNull { graphPoints("squat", listOf(it), GraphRange.All, now, zone).points.firstOrNull() },
            zone = zone,
        )
        val best = thinned.points.maxBy { it.e1rmKg ?: 0.0 }
        assertEquals(LocalDate.of(2026, 9, 17), best.localDate(zone))
        assertTrue(best.e1rmKg!! > 100.0)
    }

    @Test
    fun `years of dense training falls back to months, not to hundreds of points`() {
        // Weekly training for five years: 260 sessions, which is still too dense by week.
        val long = (0 until 260).map { session(at(2026, 9, 30) - it * 7L * 86_400_000L, 60.0, 5) }
        val series = graphPoints("squat", long, GraphRange.All, now, zone)
        assertEquals(GraphBucket.Month, series.bucket)
        // Five years of weekly training is sixty months: thinned hard, and still one point a
        // month rather than one a week.
        assertTrue("monthly: ${series.points.size}", series.points.size in (MaxGraphPoints + 1)..70)
        assertEquals(260, series.sessions)
    }
}
