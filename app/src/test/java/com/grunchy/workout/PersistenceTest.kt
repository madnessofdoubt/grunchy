package com.grunchy.workout

import com.grunchy.workout.data.Store
import com.grunchy.workout.model.AppData
import com.grunchy.workout.model.DefaultRepRanges
import com.grunchy.workout.model.ExerciseLog
import com.grunchy.workout.model.RepRange
import com.grunchy.workout.model.Routine
import com.grunchy.workout.model.RoutineItem
import com.grunchy.workout.model.SetLog
import com.grunchy.workout.model.Session
import com.grunchy.workout.model.Settings
import com.grunchy.workout.model.WeightUnit
import com.grunchy.workout.util.prefillRepsFor
import org.junit.Assert.assertEquals
import org.junit.Test

class PersistenceTest {

    private val sample = AppData(
        routines = listOf(
            Routine(
                id = "r1",
                name = "Push Day A",
                items = listOf(
                    RoutineItem("bench_press", 3, "5-10"),
                    RoutineItem("lateral_raise", 4, "10-15"),
                ),
            ),
        ),
        sessions = listOf(
            Session(
                id = "s1",
                title = "Push Day A",
                startedAt = 1_700_000_000_000L,
                finishedAt = 1_700_003_600_000L,
                logs = listOf(
                    ExerciseLog("bench_press", listOf(SetLog(60.0, 8), SetLog(62.5, 6))),
                ),
            ),
        ),
        settings = Settings(
            weightUnit = WeightUnit.LB,
            coarseStep = 10.0,
            fineStep = 2.0,
            repRanges = listOf(RepRange(5, 10), RepRange(15, null)),
            lastWeightKg = mapOf("bench_press" to 62.5),
        ),
    )

    @Test
    fun roundTripKeepsEverythingIntact() {
        assertEquals(sample, Store.decode(Store.encode(sample)))
    }

    @Test
    fun unknownFieldsSurviveAForwardCompatibleRead() {
        // "showVolume" is a real example: it was in the settings until the app dropped
        // volume tracking, and files written back then still have to load.
        val withExtra = """
            {
              "routines": [
                {
                  "id": "r1",
                  "name": "Push Day A",
                  "items": [{ "exerciseId": "bench_press", "targetSets": 3, "targetReps": 8 }]
                }
              ],
              "sessions": [],
              "settings": {
                "coarseStepKg": 5.0,
                "fineStepKg": 0.5,
                "maxWeightKg": 260.0,
                "showVolume": true,
                "lastWeightKg": {}
              },
              "somethingFromAFutureVersion": 42
            }
        """.trimIndent()

        val decoded = Store.decode(withExtra)

        assertEquals(5.0, decoded.settings.coarseStep, 0.0001)
        // No unit in the file means it was written when kilos were the only choice.
        assertEquals(WeightUnit.KG, decoded.settings.weightUnit)
        // A routine from before rep ranges existed keeps its exercises and picks up the
        // default range rather than failing to load.
        assertEquals("bench_press", decoded.routines.single().items.single().exerciseId)
        assertEquals("5-10", decoded.routines.single().items.single().targetRange)
        assertEquals(DefaultRepRanges, decoded.settings.repRanges)
    }

    @Test
    fun aRoutineWithARangeThatNoLongerExistsStillLoads() {
        val json = """
            {
              "routines": [
                {
                  "id": "r1",
                  "name": "Leg Day A",
                  "items": [{ "exerciseId": "back_squat", "targetSets": 5, "targetRange": "4-6" }]
                }
              ],
              "sessions": [],
              "settings": { "repRanges": [{ "minReps": 1, "maxReps": 5 }] }
            }
        """.trimIndent()

        val decoded = Store.decode(json)

        assertEquals("4-6", decoded.routines.single().items.single().targetRange)
        assertEquals(5, prefillRepsFor("4-6", decoded.settings.repRanges))
    }
}
