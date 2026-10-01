package com.grunchy.workout.util

import com.grunchy.workout.model.SetLog
import com.grunchy.workout.model.Session

/**
 * Beyond this many reps the one-rep-max formulas stop being estimates and start being
 * fiction: Epley and Brzycki differ by ~27% at 20 reps, so the app shows no estimate there.
 */
const val MaxRepsForE1rm = 12

/** One point on an exercise's progression chart, derived from a finished session. */
data class ExercisePoint(
    val sessionId: String,
    val date: Long,
    val topWeightKg: Double,
    val topReps: Int,
    /** Epley estimate of a single-rep max, or null when every set was too long-repped. */
    val e1rmKg: Double?,
)

/** The heaviest set of the most recent session that trained an exercise. */
data class LastTopSet(val weightKg: Double, val reps: Int, val date: Long)

/**
 * Epley's estimate of a one-rep max: `weight x (1 + reps / 30)`.
 *
 * The "e" is for *estimated* — nobody actually tested the single, so it is inferred from a
 * multi-rep set. Epley adds 1/30 of the load per rep, so the ~3.3% per-rep bump on 20 kg is
 * 0.67 kg and eight reps land on 25.33 kg.
 */
fun epley1Rm(weightKg: Double, reps: Int): Double =
    if (reps <= 1 || weightKg <= 0.0) weightKg else weightKg * (1.0 + reps / 30.0)

/** Heaviest set of a session for one exercise, reps breaking ties. */
private fun topSetOf(sets: List<SetLog>): SetLog? =
    sets.maxWithOrNull(compareBy({ it.weightKg }, { it.reps }))

fun pointsFor(exerciseId: String, sessions: List<Session>): List<ExercisePoint> =
    sessions.filter { s -> s.logs.any { it.exerciseId == exerciseId && it.sets.isNotEmpty() } }
        .sortedByDescending { it.finishedAt }
        .map { s ->
            val sets = s.logs.first { it.exerciseId == exerciseId }.sets
            // Heaviest set first (reps break ties), so "62.5 kg x 6" always describes a set
            // that actually happened; the estimate is tracked separately and only when the
            // reps are low enough for the formula to mean anything.
            val top = topSetOf(sets) ?: SetLog(0.0, 0)
            ExercisePoint(
                sessionId = s.id,
                date = s.finishedAt,
                topWeightKg = top.weightKg,
                topReps = top.reps,
                e1rmKg = sets.filter { it.reps <= MaxRepsForE1rm && it.weightKg > 0.0 }
                    .maxOfOrNull { epley1Rm(it.weightKg, it.reps) },
            )
        }

/** The sets logged for this exercise in the most recent session that trained it. */
fun lastSetsFor(exerciseId: String, sessions: List<Session>): List<SetLog>? {
    val session = sessions
        .filter { s -> s.logs.any { it.exerciseId == exerciseId && it.sets.isNotEmpty() } }
        .maxByOrNull { it.finishedAt } ?: return null
    return session.logs.first { it.exerciseId == exerciseId }.sets
}

/**
 * The best set from last time, for the reference line above the pickers: what you managed
 * last session is the number you are trying to beat this session.
 */
fun lastTopSet(exerciseId: String, sessions: List<Session>): LastTopSet? {
    val session = sessions
        .filter { s -> s.logs.any { it.exerciseId == exerciseId && it.sets.isNotEmpty() } }
        .maxByOrNull { it.finishedAt } ?: return null
    val top = topSetOf(session.logs.first { it.exerciseId == exerciseId }.sets) ?: return null
    return LastTopSet(top.weightKg, top.reps, session.finishedAt)
}

/** Weight values the coarse dropdown offers: 0, step, 2*step ... up to max. */
fun weightOptions(stepKg: Double, maxKg: Double): List<Double> {
    if (stepKg <= 0.0) return listOf(0.0)
    val count = (maxKg / stepKg).toInt()
    return (0..count).map { it * stepKg }
}

/** Snaps to the fine step so the number on screen always matches a reachable load. */
fun snap(value: Double, stepKg: Double): Double =
    if (stepKg <= 0.0) value else Math.round(value / stepKg) * stepKg
