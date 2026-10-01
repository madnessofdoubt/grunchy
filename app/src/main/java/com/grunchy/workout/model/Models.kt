package com.grunchy.workout.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Muscle group buckets that filter the exercise library in the pickers. */
enum class Group(val label: String) {
    PUSH("Push"),
    PULL("Pull"),
    LEGS("Legs"),
    CORE("Core"),
}

enum class Equip(val label: String) {
    BARBELL("Barbell"),
    DUMBBELL("Dumbbell"),
    MACHINE("Machine"),
    CABLE("Cable"),
    BODYWEIGHT("Bodyweight"),
    KETTLEBELL("Kettlebell"),
}

/**
 * A library entry. Deliberately not serialized: the library lives in code, history stores
 * only [id]s, and [com.grunchy.workout.data.ExerciseLibrary.nameOf] resolves them safely.
 */
data class Exercise(val id: String, val name: String, val group: Group, val equip: Equip)

/** What the as-many-reps-as-possible range is called, and the identity routines store. */
const val AmrapLabel = "AMRAP"

/** Where an AMRAP set starts before history has anything to say about it. */
const val AmrapPrefillReps = 8

/**
 * A target rep range such as 5-10, or 15+ for open ended work, or AMRAP for a set taken to
 * failure.
 *
 * Identity is the derived [label], which is also what routines store, so a range can be
 * reshaped in Settings without breaking the routines that point at it.
 */
@Serializable
data class RepRange(
    val minReps: Int = 1,
    /** null means "and up", rendering as 15+. Ignored when [amrap] is set. */
    val maxReps: Int? = null,
    /**
     * As many reps as possible. Not a range at all — there is no upper bound to choose — so
     * the bounds above carry no meaning and the editor shows this one control alone.
     */
    val amrap: Boolean = false,
) {
    val label: String
        get() = when {
            amrap -> AmrapLabel
            maxReps == null -> "$minReps+"
            else -> "$minReps-$maxReps"
        }

    /**
     * The number a new set starts on: the middle of the range, rounded up (5-10 -> 8).
     * Starting mid-range leaves room to add reps or weight without leaving the target.
     * AMRAP has no range to sit in the middle of, so it starts like an untargeted set.
     */
    val prefillReps: Int
        get() = when {
            amrap -> AmrapPrefillReps
            maxReps == null -> minReps
            else -> (minReps + maxReps + 1) / 2
        }

    /** Keeps min <= max; changing min past max drags max along and vice versa. */
    fun normalized(): RepRange = when {
        amrap -> this
        maxReps == null -> this
        maxReps < minReps -> copy(maxReps = minReps)
        else -> this
    }
}

/** Offered until the user edits them in More: the ranges the app ships with. */
val DefaultRepRanges: List<RepRange> = listOf(
    RepRange(1, 5),
    RepRange(5, 10),
    RepRange(10, 15),
    RepRange(15, null),
    RepRange(amrap = true),
)

@Serializable
data class RoutineItem(
    val exerciseId: String,
    val targetSets: Int = 3,
    /** Label of a range from [Settings.repRanges], e.g. "5-10". */
    val targetRange: String = "5-10",
)

@Serializable
data class Routine(
    val id: String,
    val name: String,
    val items: List<RoutineItem> = emptyList(),
) {
    val setCount: Int get() = items.sumOf { it.targetSets }
}

@Serializable
data class SetLog(val weightKg: Double, val reps: Int)

@Serializable
data class ExerciseLog(val exerciseId: String, val sets: List<SetLog> = emptyList())

@Serializable
data class Session(
    val id: String,
    val title: String,
    val startedAt: Long,
    val finishedAt: Long,
    val logs: List<ExerciseLog> = emptyList(),
) {
    val setCount: Int get() = logs.sumOf { it.sets.size }
    val exerciseCount: Int get() = logs.count { it.sets.isNotEmpty() }
}

/**
 * How the user thinks about load. Every set is *stored* in kilos ([SetLog.weightKg]) whether the
 * user logs in kilos or pounds; this only decides what the pickers, set rows and stats display.
 */
enum class WeightUnit(val label: String, val suffix: String) {
    KG("Kilograms", "kg"),
    LB("Pounds", "lbs"),
}

@Serializable
data class Settings(
    /** The unit the weight controls are shown in. Changing it is display only. */
    val weightUnit: WeightUnit = WeightUnit.KG,
    /**
     * Coarse jump offered by the weight dropdown — 0, 10, 20 ... — expressed in [weightUnit],
     * not in kilos. The serial names date from when kilos were the only unit: the numbers meant
     * the same thing then, so files written by an older build still load unchanged.
     */
    @SerialName("coarseStepKg") val coarseStep: Double = 5.0,
    /** What one tap of the fine +/- buttons next to the weight changes, in [weightUnit]. */
    @SerialName("fineStepKg") val fineStep: Double = 0.5,
    @SerialName("maxWeightKg") val maxWeight: Double = 260.0,
    /** Rep ranges offered when building a routine; edited in Settings. */
    val repRanges: List<RepRange> = DefaultRepRanges,
    /** Last working weight per exercise id, in kilos, so a fresh session starts where you left off. */
    val lastWeightKg: Map<String, Double> = emptyMap(),
)

@Serializable
data class AppData(
    val routines: List<Routine> = emptyList(),
    val sessions: List<Session> = emptyList(),
    val settings: Settings = Settings(),
)
