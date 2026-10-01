package com.grunchy.workout.data

import com.grunchy.workout.model.Equip
import com.grunchy.workout.model.Exercise
import com.grunchy.workout.model.Group

/** Preset exercise library. Ids are stable: stored history references them. */
object ExerciseLibrary {

    val all: List<Exercise> = listOf(
        // PUSH
        Exercise("bench_press", "Barbell Bench Press", Group.PUSH, Equip.BARBELL),
        Exercise("incline_barbell_bench_press", "Incline Barbell Bench Press", Group.PUSH, Equip.BARBELL),
        Exercise("close_grip_bench_press", "Close-Grip Bench Press", Group.PUSH, Equip.BARBELL),
        Exercise("overhead_press", "Overhead Press", Group.PUSH, Equip.BARBELL),
        Exercise("push_press", "Push Press", Group.PUSH, Equip.BARBELL),
        Exercise("landmine_press", "Landmine Press", Group.PUSH, Equip.BARBELL),
        Exercise("upright_row", "Upright Row", Group.PUSH, Equip.BARBELL),
        Exercise("dumbbell_bench_press", "Dumbbell Bench Press", Group.PUSH, Equip.DUMBBELL),
        Exercise("incline_dumbbell_press", "Incline Dumbbell Press", Group.PUSH, Equip.DUMBBELL),
        Exercise("seated_dumbbell_shoulder_press", "Seated Dumbbell Shoulder Press", Group.PUSH, Equip.DUMBBELL),
        Exercise("arnold_press", "Arnold Press", Group.PUSH, Equip.DUMBBELL),
        Exercise("machine_chest_press", "Machine Chest Press", Group.PUSH, Equip.MACHINE),
        Exercise("incline_machine_chest_press", "Incline Machine Chest Press", Group.PUSH, Equip.MACHINE),
        Exercise("machine_shoulder_press", "Machine Shoulder Press", Group.PUSH, Equip.MACHINE),
        Exercise("kettlebell_floor_press", "Kettlebell Floor Press", Group.PUSH, Equip.KETTLEBELL),
        Exercise("kettlebell_overhead_press", "Kettlebell Overhead Press", Group.PUSH, Equip.KETTLEBELL),
        Exercise("kettlebell_push_press", "Kettlebell Push Press", Group.PUSH, Equip.KETTLEBELL),
        Exercise("dip", "Dip", Group.PUSH, Equip.BODYWEIGHT),
        Exercise("push_up", "Push-Up", Group.PUSH, Equip.BODYWEIGHT),
        Exercise("incline_push_up", "Incline Push-Up", Group.PUSH, Equip.BODYWEIGHT),
        Exercise("pike_push_up", "Pike Push-Up", Group.PUSH, Equip.BODYWEIGHT),
        Exercise("handstand_push_up", "Handstand Push-Up", Group.PUSH, Equip.BODYWEIGHT),
        Exercise("dumbbell_fly", "Dumbbell Fly", Group.PUSH, Equip.DUMBBELL),
        Exercise("cable_chest_fly", "Cable Chest Fly", Group.PUSH, Equip.CABLE),
        Exercise("cable_crossover", "Cable Crossover", Group.PUSH, Equip.CABLE),
        Exercise("pec_deck", "Pec Deck", Group.PUSH, Equip.MACHINE),
        Exercise("lateral_raise", "Lateral Raise", Group.PUSH, Equip.DUMBBELL),
        Exercise("cable_lateral_raise", "Cable Lateral Raise", Group.PUSH, Equip.CABLE),
        Exercise("machine_lateral_raise", "Machine Lateral Raise", Group.PUSH, Equip.MACHINE),
        Exercise("triceps_pushdown", "Triceps Pushdown", Group.PUSH, Equip.CABLE),
        Exercise("rope_triceps_pushdown", "Rope Triceps Pushdown", Group.PUSH, Equip.CABLE),
        Exercise("overhead_cable_triceps_extension", "Overhead Cable Triceps Extension", Group.PUSH, Equip.CABLE),
        Exercise("skull_crusher", "Skull Crusher", Group.PUSH, Equip.BARBELL),
        Exercise("overhead_dumbbell_triceps_extension", "Overhead Dumbbell Triceps Extension", Group.PUSH, Equip.DUMBBELL),
        Exercise("triceps_kickback", "Triceps Kickback", Group.PUSH, Equip.DUMBBELL),
        Exercise("machine_triceps_extension", "Machine Triceps Extension", Group.PUSH, Equip.MACHINE),

        // PULL
        Exercise("deadlift", "Deadlift", Group.PULL, Equip.BARBELL),
        Exercise("barbell_row", "Barbell Row", Group.PULL, Equip.BARBELL),
        Exercise("pendlay_row", "Pendlay Row", Group.PULL, Equip.BARBELL),
        Exercise("t_bar_row", "T-Bar Row", Group.PULL, Equip.BARBELL),
        Exercise("pull_up", "Pull-Up", Group.PULL, Equip.BODYWEIGHT),
        Exercise("chin_up", "Chin-Up", Group.PULL, Equip.BODYWEIGHT),
        Exercise("inverted_row", "Inverted Row", Group.PULL, Equip.BODYWEIGHT),
        Exercise("lat_pulldown", "Lat Pulldown", Group.PULL, Equip.CABLE),
        Exercise("close_grip_lat_pulldown", "Close-Grip Lat Pulldown", Group.PULL, Equip.CABLE),
        Exercise("seated_cable_row", "Seated Cable Row", Group.PULL, Equip.CABLE),
        Exercise("straight_arm_pulldown", "Straight-Arm Pulldown", Group.PULL, Equip.CABLE),
        Exercise("chest_supported_dumbbell_row", "Chest-Supported Dumbbell Row", Group.PULL, Equip.DUMBBELL),
        Exercise("one_arm_dumbbell_row", "One-Arm Dumbbell Row", Group.PULL, Equip.DUMBBELL),
        Exercise("bent_over_dumbbell_row", "Bent-Over Dumbbell Row", Group.PULL, Equip.DUMBBELL),
        Exercise("machine_row", "Machine Row", Group.PULL, Equip.MACHINE),
        Exercise("hammer_strength_row", "Hammer Strength Row", Group.PULL, Equip.MACHINE),
        Exercise("kettlebell_row", "Kettlebell Row", Group.PULL, Equip.KETTLEBELL),
        Exercise("barbell_shrug", "Barbell Shrug", Group.PULL, Equip.BARBELL),
        Exercise("dumbbell_shrug", "Dumbbell Shrug", Group.PULL, Equip.DUMBBELL),
        Exercise("face_pull", "Face Pull", Group.PULL, Equip.CABLE),
        Exercise("reverse_pec_deck", "Reverse Pec Deck", Group.PULL, Equip.MACHINE),
        Exercise("rear_delt_dumbbell_fly", "Rear Delt Dumbbell Fly", Group.PULL, Equip.DUMBBELL),
        Exercise("cable_rear_delt_fly", "Cable Rear Delt Fly", Group.PULL, Equip.CABLE),
        Exercise("barbell_curl", "Barbell Curl", Group.PULL, Equip.BARBELL),
        Exercise("ez_bar_curl", "EZ-Bar Curl", Group.PULL, Equip.BARBELL),
        Exercise("dumbbell_curl", "Dumbbell Curl", Group.PULL, Equip.DUMBBELL),
        Exercise("hammer_curl", "Hammer Curl", Group.PULL, Equip.DUMBBELL),
        Exercise("incline_dumbbell_curl", "Incline Dumbbell Curl", Group.PULL, Equip.DUMBBELL),
        Exercise("concentration_curl", "Concentration Curl", Group.PULL, Equip.DUMBBELL),
        Exercise("preacher_curl", "Preacher Curl", Group.PULL, Equip.BARBELL),
        Exercise("cable_curl", "Cable Curl", Group.PULL, Equip.CABLE),
        Exercise("machine_curl", "Machine Curl", Group.PULL, Equip.MACHINE),

        // LEGS
        Exercise("back_squat", "Back Squat", Group.LEGS, Equip.BARBELL),
        Exercise("front_squat", "Front Squat", Group.LEGS, Equip.BARBELL),
        Exercise("box_squat", "Box Squat", Group.LEGS, Equip.BARBELL),
        Exercise("barbell_lunge", "Barbell Lunge", Group.LEGS, Equip.BARBELL),
        Exercise("romanian_deadlift", "Romanian Deadlift", Group.LEGS, Equip.BARBELL),
        Exercise("sumo_deadlift", "Sumo Deadlift", Group.LEGS, Equip.BARBELL),
        Exercise("stiff_leg_deadlift", "Stiff-Legged Deadlift", Group.LEGS, Equip.BARBELL),
        Exercise("hip_thrust", "Hip Thrust", Group.LEGS, Equip.BARBELL),
        Exercise("good_morning", "Good Morning", Group.LEGS, Equip.BARBELL),
        Exercise("hack_squat", "Hack Squat", Group.LEGS, Equip.MACHINE),
        Exercise("leg_press", "Leg Press", Group.LEGS, Equip.MACHINE),
        Exercise("smith_machine_squat", "Smith Machine Squat", Group.LEGS, Equip.MACHINE),
        Exercise("goblet_squat", "Goblet Squat", Group.LEGS, Equip.KETTLEBELL),
        Exercise("kettlebell_swing", "Kettlebell Swing", Group.LEGS, Equip.KETTLEBELL),
        Exercise("bulgarian_split_squat", "Bulgarian Split Squat", Group.LEGS, Equip.DUMBBELL),
        Exercise("walking_lunge", "Walking Lunge", Group.LEGS, Equip.DUMBBELL),
        Exercise("reverse_lunge", "Reverse Lunge", Group.LEGS, Equip.DUMBBELL),
        Exercise("step_up", "Step-Up", Group.LEGS, Equip.DUMBBELL),
        Exercise("dumbbell_romanian_deadlift", "Dumbbell Romanian Deadlift", Group.LEGS, Equip.DUMBBELL),
        Exercise("pistol_squat", "Pistol Squat", Group.LEGS, Equip.BODYWEIGHT),
        Exercise("glute_bridge", "Glute Bridge", Group.LEGS, Equip.BODYWEIGHT),
        Exercise("leg_extension", "Leg Extension", Group.LEGS, Equip.MACHINE),
        Exercise("leg_curl", "Leg Curl", Group.LEGS, Equip.MACHINE),
        Exercise("seated_leg_curl", "Seated Leg Curl", Group.LEGS, Equip.MACHINE),
        Exercise("nordic_hamstring_curl", "Nordic Hamstring Curl", Group.LEGS, Equip.BODYWEIGHT),
        Exercise("cable_pull_through", "Cable Pull-Through", Group.LEGS, Equip.CABLE),
        Exercise("cable_kickback", "Cable Kickback", Group.LEGS, Equip.CABLE),
        Exercise("hip_abduction", "Hip Abduction", Group.LEGS, Equip.MACHINE),
        Exercise("hip_adduction", "Hip Adduction", Group.LEGS, Equip.MACHINE),
        Exercise("standing_calf_raise", "Standing Calf Raise", Group.LEGS, Equip.MACHINE),
        Exercise("seated_calf_raise", "Seated Calf Raise", Group.LEGS, Equip.MACHINE),
        Exercise("calf_press", "Calf Press", Group.LEGS, Equip.MACHINE),
        Exercise("dumbbell_calf_raise", "Dumbbell Calf Raise", Group.LEGS, Equip.DUMBBELL),
        Exercise("single_leg_calf_raise", "Single-Leg Calf Raise", Group.LEGS, Equip.BODYWEIGHT),

        // CORE
        Exercise("plank", "Plank", Group.CORE, Equip.BODYWEIGHT),
        Exercise("side_plank", "Side Plank", Group.CORE, Equip.BODYWEIGHT),
        Exercise("hanging_leg_raise", "Hanging Leg Raise", Group.CORE, Equip.BODYWEIGHT),
        Exercise("hanging_knee_raise", "Hanging Knee Raise", Group.CORE, Equip.BODYWEIGHT),
        Exercise("lying_leg_raise", "Lying Leg Raise", Group.CORE, Equip.BODYWEIGHT),
        Exercise("crunch", "Crunch", Group.CORE, Equip.BODYWEIGHT),
        Exercise("bicycle_crunch", "Bicycle Crunch", Group.CORE, Equip.BODYWEIGHT),
        Exercise("sit_up", "Sit-Up", Group.CORE, Equip.BODYWEIGHT),
        Exercise("dead_bug", "Dead Bug", Group.CORE, Equip.BODYWEIGHT),
        Exercise("ab_wheel_rollout", "Ab Wheel Rollout", Group.CORE, Equip.BODYWEIGHT),
        Exercise("cable_crunch", "Cable Crunch", Group.CORE, Equip.CABLE),
        Exercise("machine_crunch", "Machine Crunch", Group.CORE, Equip.MACHINE),
        Exercise("pallof_press", "Pallof Press", Group.CORE, Equip.CABLE),
        Exercise("cable_woodchopper", "Cable Woodchopper", Group.CORE, Equip.CABLE),
        Exercise("russian_twist", "Russian Twist", Group.CORE, Equip.DUMBBELL),
        Exercise("turkish_get_up", "Turkish Get-Up", Group.CORE, Equip.KETTLEBELL),
        Exercise("kettlebell_windmill", "Kettlebell Windmill", Group.CORE, Equip.KETTLEBELL),
        Exercise("back_extension", "Back Extension", Group.CORE, Equip.BODYWEIGHT),
    )

    private val index: Map<String, Exercise> = all.associateBy { it.id }

    fun byId(id: String): Exercise? = index[id]

    /** Never fails: unknown ids (e.g. removed from the library) still render. */
    fun nameOf(id: String): String = index[id]?.name ?: "Unknown exercise"

    fun byGroup(group: Group): List<Exercise> = all.filter { it.group == group }

    fun search(query: String): List<Exercise> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter { it.name.lowercase().contains(q) }
    }
}
