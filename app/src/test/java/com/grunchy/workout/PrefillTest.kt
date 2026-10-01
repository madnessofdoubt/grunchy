package com.grunchy.workout

import com.grunchy.workout.model.ExerciseLog
import com.grunchy.workout.model.SetLog
import com.grunchy.workout.model.Session
import com.grunchy.workout.model.Settings
import com.grunchy.workout.ui.ActiveEntry
import com.grunchy.workout.ui.ActiveSet
import com.grunchy.workout.ui.ActiveWorkout
import com.grunchy.workout.ui.completeSet
import com.grunchy.workout.ui.lastWeights
import com.grunchy.workout.ui.prefillSets
import com.grunchy.workout.ui.toSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The prefill behaviour is the reason logging a set is normally a single tap. */
class PrefillTest {

    private fun session(id: String, finishedAt: Long, sets: List<Pair<Double, Int>>) =
        Session(
            id = id,
            title = "Push Day A",
            startedAt = finishedAt - 60_000L,
            finishedAt = finishedAt,
            logs = listOf(ExerciseLog("bench_press", sets.map { SetLog(it.first, it.second) }),),
        )

    @Test
    fun eachSetMirrorsLastSessionsSetOfTheSameNumber() {
        val history = listOf(
            session("old", 1_000L, listOf(50.0 to 10)),
            session("new", 5_000L, listOf(60.0 to 8, 62.5 to 6)),
        )

        val sets = prefillSets(history, Settings(), "bench_press", setCount = 3, defaultReps = 8)

        assertEquals(listOf(60.0, 62.5, 62.5), sets.map { it.weightKg })
        assertEquals(listOf(8, 6, 6), sets.map { it.reps })
        assertTrue("nothing is pre-ticked", sets.none { it.done })
    }

    @Test
    fun fallsBackToTheRememberedWeightWhenThereIsNoHistory() {
        val sets = prefillSets(
            sessions = emptyList(),
            settings = Settings(lastWeightKg = mapOf("bench_press" to 40.0)),
            exerciseId = "bench_press",
            setCount = 2,
            defaultReps = 12,
        )

        assertEquals(listOf(40.0, 40.0), sets.map { it.weightKg })
        assertEquals(listOf(12, 12), sets.map { it.reps })
    }

    @Test
    fun anExerciseYouHaveNeverDoneStartsAtZero() {
        val sets = prefillSets(emptyList(), Settings(), "squat", setCount = 1, defaultReps = 5)
        assertEquals(0.0, sets.single().weightKg, 0.0001)
        assertEquals(5, sets.single().reps)
    }

    @Test
    fun finishingRecordsOnlyCompletedSets() {
        val workout = ActiveWorkout("Push Day A", startedAt = 1_000L)
        val entry = ActiveEntry("bench_press")
        entry.sets += ActiveSet(60.0, 8, done = true)
        entry.sets += ActiveSet(62.5, 6, done = true)
        entry.sets += ActiveSet(65.0, 4, done = false)
        workout.entries += entry

        val finished = workout.toSession(finishedAt = 2_000L, id = "s1")

        assertEquals(1, finished.logs.size)
        assertEquals(2, finished.setCount)
        assertEquals(listOf(60.0, 62.5), finished.logs.single().sets.map { it.weightKg })
        assertEquals(listOf(8, 6), finished.logs.single().sets.map { it.reps })
    }

    @Test
    fun anExerciseWithNoCompletedSetsIsNotRecordedAtAll() {
        val workout = ActiveWorkout("Freestyle workout", startedAt = 1_000L)
        val entry = ActiveEntry("bench_press")
        entry.sets += ActiveSet(60.0, 8, done = false)
        workout.entries += entry

        val finished = workout.toSession(finishedAt = 2_000L, id = "s1")

        assertTrue(finished.logs.isEmpty())
        assertEquals(0, finished.setCount)
        assertFalse(finished.logs.any { it.exerciseId == "bench_press" })
    }

    @Test
    fun lastWeightsComeFromTheFinalCompletedSet() {
        val workout = ActiveWorkout("Push Day A", startedAt = 1_000L)
        val entry = ActiveEntry("bench_press")
        entry.sets += ActiveSet(60.0, 8, done = true)
        entry.sets += ActiveSet(70.0, 3, done = false)
        workout.entries += entry

        assertEquals(mapOf("bench_press" to 60.0), workout.lastWeights())
    }

    @Test
    fun completingASetPropagatesToUntouchedSetsBelowIt() {
        val entry = ActiveEntry("back_squat")
        entry.sets += ActiveSet(60.0, 8)
        entry.sets += ActiveSet(0.0, 8)
        entry.sets += ActiveSet(0.0, 8)

        entry.completeSet(0)

        assertEquals(listOf(60.0, 60.0, 60.0), entry.sets.map { it.weightKg })
        assertEquals(listOf(true, false, false), entry.sets.map { it.done })
    }

    @Test
    fun completingASetLeavesSetsTheUserAdjustedAlone() {
        val entry = ActiveEntry("back_squat")
        entry.sets += ActiveSet(60.0, 8)
        val backoff = ActiveSet(40.0, 12)
        backoff.edited = true
        entry.sets += backoff

        entry.completeSet(0)

        assertEquals(40.0, entry.sets[1].weightKg, 0.0001)
        assertEquals(12, entry.sets[1].reps)
    }

    @Test
    fun completingASetIgnoresSetsThatAreAlreadyLogged() {
        val entry = ActiveEntry("back_squat")
        entry.sets += ActiveSet(50.0, 8, done = true)
        entry.sets += ActiveSet(60.0, 8)

        entry.completeSet(1)

        assertEquals(50.0, entry.sets[0].weightKg, 0.0001)
        assertTrue(entry.sets[0].done)
    }
}
