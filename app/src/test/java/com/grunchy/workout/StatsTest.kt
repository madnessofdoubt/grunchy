package com.grunchy.workout

import com.grunchy.workout.model.ExerciseLog
import com.grunchy.workout.model.SetLog
import com.grunchy.workout.model.Session
import com.grunchy.workout.util.epley1Rm
import com.grunchy.workout.util.fmtDuration
import com.grunchy.workout.util.fmtNumber
import com.grunchy.workout.util.lastSetsFor
import com.grunchy.workout.util.lastTopSet
import com.grunchy.workout.util.pointsFor
import com.grunchy.workout.util.snap
import com.grunchy.workout.util.weightOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsTest {

    private fun session(id: String, finishedAt: Long, exerciseId: String, sets: List<Pair<Double, Int>>) =
        Session(
            id = id,
            title = "Test session",
            startedAt = finishedAt - 60_000L,
            finishedAt = finishedAt,
            logs = listOf(ExerciseLog(exerciseId, sets.map { SetLog(it.first, it.second) })),
        )

    @Test
    fun epleyUsesTheStandardFormula() {
        assertEquals(0.0, epley1Rm(0.0, 5), 0.0001)
        assertEquals(100.0, epley1Rm(100.0, 1), 0.0001)
        assertEquals(120.0, epley1Rm(90.0, 10), 0.0001)
        assertEquals(60.0, epley1Rm(60.0, 1), 0.0001)
        // The example the app explains in Stats: 20 kg x 8 -> 20 x (1 + 8/30) = 25.33.
        assertEquals(25.3333, epley1Rm(20.0, 8), 0.0001)
    }

    @Test
    fun pointsAreNewestFirstAndCarryTheTopSet() {
        val older = session("s1", 1_000L, "bench_press", listOf(50.0 to 10, 55.0 to 8))
        val newer = session("s2", 5_000L, "bench_press", listOf(60.0 to 8, 62.5 to 6))
        val other = session("s3", 9_000L, "squat", listOf(100.0 to 5))

        val points = pointsFor("bench_press", listOf(older, other, newer))

        assertEquals(listOf("s2", "s1"), points.map { it.sessionId })
        assertEquals(62.5, points.first().topWeightKg, 0.0001)
        assertEquals(6, points.first().topReps)
        // Best estimate of a single lift comes from the 60 x 8 set: 60 * (1 + 8/30) = 76.0
        assertEquals(76.0, points.first().e1rmKg!!, 0.0001)
    }

    @Test
    fun longRepSetsCarryNoEstimate() {
        // 20 reps at 20 kg: Epley would claim 33.3 kg and Brzycki 40 kg, so the app shows
        // neither — the top set is reported and no estimate is invented.
        val endurance = session("s1", 1_000L, "push_up", listOf(20.0 to 20))

        val point = pointsFor("push_up", listOf(endurance)).single()

        assertNull(point.e1rmKg)
        assertEquals(20.0, point.topWeightKg, 0.0001)
        assertEquals(20, point.topReps)
    }

    @Test
    fun theEstimateUsesOnlySetsWithinTheRepCutoff() {
        val mixed = session("s1", 1_000L, "bench_press", listOf(40.0 to 20, 40.0 to 8))

        val point = pointsFor("bench_press", listOf(mixed)).single()

        assertEquals(epley1Rm(40.0, 8), point.e1rmKg!!, 0.0001)
    }

    @Test
    fun pointsAreEmptyForAnExerciseNeverTrained() {
        val s = session("s1", 1_000L, "bench_press", listOf(50.0 to 10))
        assertEquals(emptyList<Any>(), pointsFor("squat", listOf(s)))
    }

    @Test
    fun lastSetsComeFromTheMostRecentSessionThatTrainedIt() {
        val older = session("s1", 1_000L, "bench_press", listOf(50.0 to 10))
        val newer = session("s2", 5_000L, "bench_press", listOf(60.0 to 8, 62.5 to 6))
        val sets = lastSetsFor("bench_press", listOf(newer, older))
        assertEquals(listOf(60.0, 62.5), sets?.map { it.weightKg })
        assertNull(lastSetsFor("squat", listOf(newer, older)))
    }

    @Test
    fun lastTopSetIsTheHeaviestSetOfTheMostRecentSession() {
        val older = session("s1", 1_000L, "bench_press", listOf(70.0 to 5))
        val newer = session("s2", 5_000L, "bench_press", listOf(60.0 to 8, 62.5 to 6))

        val top = lastTopSet("bench_press", listOf(older, newer))

        // The newest session wins even though the older one was heavier: this is the number
        // to beat today, not the all-time best.
        assertEquals(62.5, top!!.weightKg, 0.0001)
        assertEquals(6, top.reps)
        assertEquals(5_000L, top.date)
        assertNull(lastTopSet("squat", listOf(older, newer)))
    }

    @Test
    fun weightOptionsAreSteppedFromZero() {
        assertEquals(listOf(0.0, 5.0, 10.0, 15.0, 20.0), weightOptions(5.0, 20.0))
        assertEquals(listOf(0.0, 2.5, 5.0), weightOptions(2.5, 5.0))
        assertEquals(53, weightOptions(5.0, 260.0).size)
    }

    @Test
    fun snapRoundsToTheNearestFineStep() {
        assertEquals(62.5, snap(62.3, 0.5), 0.0001)
        assertEquals(62.5, snap(62.4, 0.5), 0.0001)
        assertEquals(62.0, snap(62.2, 0.5), 0.0001)
        // Negative weights are not clamped here: the +/- buttons clamp before storing.
        assertEquals(-3.0, snap(-3.0, 0.5), 0.0001)
        assertEquals(0.0, snap(-3.0, 0.5).coerceAtLeast(0.0), 0.0001)
    }

    @Test
    fun formattingKeepsNumbersShort() {
        assertEquals("60", fmtNumber(60.0))
        assertEquals("62.5", fmtNumber(62.5))
        assertEquals("45m", fmtDuration(45 * 60_000L))
        assertEquals("1h 30m", fmtDuration(90 * 60_000L))
    }
}
