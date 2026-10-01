package com.grunchy.workout.ui

import com.grunchy.workout.model.Group

/** Picker contents shared by every screen. Kept short on purpose: scrolling costs refresh. */
val RepOptions: List<Int> = (1..30).toList()
val SetOptions: List<Int> = (1..10).toList()
val GroupOptions: List<Group> = Group.entries.toList()

/**
 * Routine names offered instead of a text field. The first entry is a sane default so a
 * new routine can be saved with zero typing.
 */
val RoutineNameOptions: List<String> = listOf(
    "Push Day A",
    "Push Day B",
    "Pull Day A",
    "Pull Day B",
    "Leg Day A",
    "Leg Day B",
    "Upper Body",
    "Lower Body",
    "Full Body A",
    "Full Body B",
    "Chest & Back",
    "Shoulders & Arms",
    "Arms & Core",
    "Quick Session",
)

const val CustomNameOption = "Custom name…"

/** Bounds offered when shaping a rep range in More. */
val RepRangeBoundOptions: List<Int> = (1..30).toList()

/** Upper bounds for a rep range; null is the open ended "and up" choice (15+). */
val RepRangeMaxOptions: List<Int?> = RepRangeBoundOptions.map { it as Int? } + listOf(null)

/**
 * Lower bound control of a rep range row. null is the AMRAP choice: it is not a range at all,
 * so picking it leaves a row with no upper bound to set — one control, nothing else.
 */
val RepRangeStartOptions: List<Int?> = RepRangeBoundOptions.map { it as Int? } + listOf(null)
