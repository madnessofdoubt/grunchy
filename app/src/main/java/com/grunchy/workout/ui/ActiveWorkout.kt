package com.grunchy.workout.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.grunchy.workout.data.ExerciseLibrary
import com.grunchy.workout.model.ExerciseLog
import com.grunchy.workout.model.Routine
import com.grunchy.workout.model.SetLog
import com.grunchy.workout.model.Settings
import com.grunchy.workout.model.Session
import com.grunchy.workout.util.DefaultPrefillReps
import com.grunchy.workout.util.lastSetsFor
import com.grunchy.workout.util.prefillRepsFor

/**
 * The workout currently being logged. Deliberately in-memory only: if the app dies mid
 * session you re-open it and everything you had *finished* is already on disk.
 */
class ActiveSet(weightKg: Double, reps: Int, done: Boolean = false) {
    var weightKg by mutableStateOf(weightKg)
    var reps by mutableStateOf(reps)
    var done by mutableStateOf(done)

    /**
     * True once the user has touched this set's numbers. Untouched sets silently follow the
     * set you just completed, so a straight-set workout needs one weight edit, not five.
     */
    var edited by mutableStateOf(false)
}

class ActiveEntry(val exerciseId: String, val targetRange: String? = null) {
    val sets = mutableStateListOf<ActiveSet>()
    val name: String get() = ExerciseLibrary.nameOf(exerciseId)
}

class ActiveWorkout(
    val title: String,
    val startedAt: Long,
) {
    /**
     * Snapshot backed: adding an exercise mid-session has to recompose the list, and this
     * is the only mutable list the workout screen grows.
     */
    val entries = mutableStateListOf<ActiveEntry>()
}

/**
 * Builds a session. Every set is pre-filled from the same exercise's last logged sets
 * (or the middle of its target rep range), so logging usually means tapping the tick.
 */
fun startWorkout(state: AppState, routine: Routine?): ActiveWorkout {
    val workout = ActiveWorkout(routine?.name ?: "Freestyle workout", System.currentTimeMillis())
    routine?.items?.forEach { item ->
        workout.entries += newEntry(
            state = state,
            exerciseId = item.exerciseId,
            setCount = item.targetSets,
            defaultReps = prefillRepsFor(item.targetRange, state.settings.repRanges),
            targetRange = item.targetRange,
        )
    }
    return workout
}

fun newEntry(
    state: AppState,
    exerciseId: String,
    setCount: Int = 3,
    defaultReps: Int = DefaultPrefillReps,
    targetRange: String? = null,
): ActiveEntry {
    val entry = ActiveEntry(exerciseId, targetRange)
    entry.sets.addAll(prefillSets(state.sessions, state.settings, exerciseId, setCount, defaultReps))
    return entry
}

/**
 * Pure prefill logic, kept out of [AppState] so it can be unit tested without an Android
 * context: set 1 mirrors last time's set 1, set 2 mirrors set 2, and any extra sets repeat
 * the last one you actually did.
 */
fun prefillSets(
    sessions: List<Session>,
    settings: Settings,
    exerciseId: String,
    setCount: Int,
    defaultReps: Int,
): List<ActiveSet> {
    val last = lastSetsFor(exerciseId, sessions)
    val remembered = settings.lastWeightKg[exerciseId]
    return (0 until setCount.coerceAtLeast(1)).map { i ->
        val prev = last?.getOrNull(i) ?: last?.lastOrNull()
        ActiveSet(
            weightKg = prev?.weightKg ?: remembered ?: 0.0,
            reps = prev?.reps ?: defaultReps,
        )
    }
}

fun ActiveWorkout.entryFor(exerciseId: String): ActiveEntry? = entries.firstOrNull { it.exerciseId == exerciseId }

/** Only completed sets are recorded; whatever is still pending is simply dropped. */
fun ActiveWorkout.toSession(finishedAt: Long, id: String): Session = Session(
    id = id,
    title = title,
    startedAt = startedAt,
    finishedAt = finishedAt,
    logs = entries.mapNotNull { entry ->
        val done = entry.sets.filter { it.done }
        if (done.isEmpty()) null else ExerciseLog(entry.exerciseId, done.map { SetLog(it.weightKg, it.reps) })
    },
)

/** Working weight to remember per exercise for the next session. */
fun ActiveWorkout.lastWeights(): Map<String, Double> {
    // Note: not buildMap { } — inside it `entries` would resolve to the map's own entries.
    val result = LinkedHashMap<String, Double>()
    entries.forEach { entry ->
        entry.sets.lastOrNull { it.done }?.let { result[entry.exerciseId] = it.weightKg }
    }
    return result
}

val ActiveWorkout.doneSetCount: Int get() = entries.sumOf { entry -> entry.sets.count { it.done } }

/**
 * Marks [index] complete and copies its numbers into the sets after it that the user has
 * not touched yet. Straight sets then cost one tap per set instead of a weight edit each.
 */
fun ActiveEntry.completeSet(index: Int) {
    val set = sets.getOrNull(index) ?: return
    set.done = true
    for (i in index + 1 until sets.size) {
        val next = sets[i]
        if (!next.done && !next.edited) {
            next.weightKg = set.weightKg
            next.reps = set.reps
        }
    }
}
