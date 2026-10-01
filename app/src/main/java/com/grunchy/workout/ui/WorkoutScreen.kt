@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.grunchy.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grunchy.workout.data.ExerciseLibrary
import com.grunchy.workout.model.AmrapLabel
import com.grunchy.workout.model.Exercise
import com.grunchy.workout.model.Group
import com.grunchy.workout.model.Session
import com.grunchy.workout.util.DefaultPrefillReps
import com.grunchy.workout.model.WeightUnit
import com.grunchy.workout.util.LastTopSet
import com.grunchy.workout.util.fmtWeight
import com.grunchy.workout.util.toKg
import com.grunchy.workout.util.fmtDate
import com.grunchy.workout.util.fmtTime
import com.grunchy.workout.util.lastTopSet
import com.grunchy.workout.util.snap
import com.grunchy.workout.util.weightOptions
import com.mudita.mmd.components.cards.CardMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

/**
 * Logging screen. Per set you get: a coarse weight dropdown and ± fine buttons, both in the
 * unit the user chose, a reps dropdown and one tick. Sets arrive pre-filled from last time, so
 * the common case is "tick, tick, tick".
 */
@Composable
fun WorkoutScreen(
    state: AppState,
    workout: ActiveWorkout,
    onFinish: (Session) -> Unit,
    onDiscard: () -> Unit,
) {
    val settings = state.settings
    val unit = settings.weightUnit
    val weightValues = remember(settings.coarseStep, settings.maxWeight) {
        weightOptions(settings.coarseStep, settings.maxWeight)
    }
    var confirmDiscard by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = workout.title,
            subtitle = "Started ${fmtTime(workout.startedAt)} · ${counted(workout.doneSetCount, "set")} done",
            navigation = { BarButton("Discard", onClick = { confirmDiscard = true }) },
            actions = {
                BarButton(
                    label = "Finish",
                    filled = true,
                    onClick = { onFinish(workout.toSession(System.currentTimeMillis(), newId())) },
                )
            },
        )

        if (confirmDiscard) {
            Box(Modifier.padding(8.dp)) {
                CardMMD(Modifier.fillMaxWidth()) {
                    Box(Modifier.padding(10.dp)) {
                        InlineConfirm(
                            message = "Nothing here will be saved. Discard this workout?",
                            confirmLabel = "Discard",
                            onConfirm = onDiscard,
                            onCancel = { confirmDiscard = false },
                        )
                    }
                }
            }
        }

        ScrollList(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(Gap),
        ) {
            if (workout.entries.isEmpty()) {
                item {
                    Note("Add your first exercise below.")
                }
            }
            items(workout.entries, key = { it.exerciseId }) { entry ->
                WorkoutEntryCard(
                    entry = entry,
                    unit = unit,
                    weightValues = weightValues,
                    fineStep = settings.fineStep,
                    lastTop = lastTopSet(entry.exerciseId, state.sessions),
                    onRemove = { workout.entries.remove(entry) },
                )
            }
            item {
                AddExerciseCard { exercise ->
                    val existing = workout.entryFor(exercise.id)
                    val last = existing?.sets?.lastOrNull()
                    if (existing != null) {
                        // Already in this session: another set of the same thing.
                        existing.sets += ActiveSet(last?.weightKg ?: 0.0, last?.reps ?: DefaultPrefillReps)
                    } else {
                        workout.entries += newEntry(state, exercise.id)
                    }
                }
            }
            item {
                PrimaryAction(
                    label = "Finish workout",
                    onClick = { onFinish(workout.toSession(System.currentTimeMillis(), newId())) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun WorkoutEntryCard(
    entry: ActiveEntry,
    unit: WeightUnit,
    weightValues: List<Double>,
    fineStep: Double,
    lastTop: LastTopSet?,
    onRemove: () -> Unit,
) {
    CardMMD(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                TextMMD(
                    text = entry.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Note(
                    // The target comes from the routine; a freestyle exercise has none.
                    when (entry.targetRange) {
                        null -> equipmentLabel(entry.exerciseId) + " · tap ✓ to log a set"
                        AmrapLabel ->
                            "${equipmentLabel(entry.exerciseId)} · AMRAP: as many reps as possible"
                        else -> "${equipmentLabel(entry.exerciseId)} · target ${entry.targetRange} reps"
                    },
                )
            }
            BarButton("✕", onClick = onRemove)
        }
        ThinDivider()
        // What you managed last session, sitting right above the numbers you are picking now.
        if (lastTop != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Note("Last time · ${fmtDate(lastTop.date)}")
                RowSpacer()
                TextMMD(
                    text = "${fmtWeight(lastTop.weightKg, unit)} × ${lastTop.reps}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            ThinDivider()
        }
        // Header and rows share the same padding, gaps and column widths, otherwise the
        // labels drift away from the controls they describe.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextMMD("SET", Modifier.width(26.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            TextMMD(
                text = "WEIGHT (${unit.suffix})",
                modifier = Modifier.weight(1f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            TextMMD(
                text = "REPS",
                modifier = Modifier.width(50.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(50.dp))
        }
        entry.sets.forEachIndexed { index, set ->
            SetRow(
                entry = entry,
                index = index,
                set = set,
                unit = unit,
                weightValues = weightValues,
                fineStep = fineStep,
            )
            ThinDivider()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(Gap),
        ) {
            SecondaryAction(
                label = "+ Set",
                onClick = {
                    val last = entry.sets.lastOrNull()
                    entry.sets += ActiveSet(last?.weightKg ?: 0.0, last?.reps ?: DefaultPrefillReps)
                },
                modifier = Modifier.weight(1f),
            )
            SecondaryAction(
                label = "− Set",
                onClick = { if (entry.sets.size > 1) entry.sets.removeAt(entry.sets.lastIndex) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SetRow(
    entry: ActiveEntry,
    index: Int,
    set: ActiveSet,
    unit: WeightUnit,
    weightValues: List<Double>,
    fineStep: Double,
) {
    if (set.done) {
        // Logged sets collapse to one tappable line: less ink on screen, less to refresh.
        TapRow(
            onClick = { set.done = false },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextMMD("✓", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                TextMMD(
                    text = "${fmtWeight(set.weightKg, unit)} × ${set.reps}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                RowSpacer()
                TextMMD("edit", fontSize = 11.sp)
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextMMD("${index + 1}", Modifier.width(26.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            WeightPicker(
                weightKg = set.weightKg,
                unit = unit,
                options = weightValues,
                fineStep = fineStep,
                onWeightChange = {
                    set.weightKg = unit.toKg(snap(it, fineStep)).coerceAtLeast(0.0)
                    set.edited = true
                },
                modifier = Modifier.weight(1f),
            )
            ValueDropdown(
                options = RepOptions,
                selected = set.reps,
                optionLabel = { "$it" },
                menuOptionLabel = { "$it reps" },
                onSelect = {
                    set.reps = it
                    set.edited = true
                },
                modifier = Modifier.width(50.dp),
                bold = true,
            )
            BarButton(
                label = "✓",
                onClick = { entry.completeSet(index) },
                filled = true,
                modifier = Modifier.width(50.dp),
            )
        }
    }
}

@Composable
private fun AddExerciseCard(onAdd: (Exercise) -> Unit) {
    var group by remember { mutableStateOf(Group.PUSH) }
    var picked by remember { mutableStateOf<Exercise?>(null) }
    var hint by remember { mutableStateOf(false) }
    val options = remember(group) { ExerciseLibrary.byGroup(group) }

    CardMMD(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            SectionLabel("Add exercise")
            Spacer(Modifier.height(Gap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Gap),
            ) {
                ValueDropdown(
                    options = GroupOptions,
                    selected = group,
                    optionLabel = { it.label },
                    onSelect = {
                        group = it
                        picked = null
                    },
                    modifier = Modifier.weight(1f),
                )
                ValueDropdown(
                    options = options,
                    selected = picked,
                    optionLabel = { it.name },
                    onSelect = { picked = it },
                    modifier = Modifier.weight(2f),
                    menuHeight = 340.dp,
                )
            }
            Spacer(Modifier.height(Gap))
            PrimaryAction(
                label = "Add exercise",
                onClick = {
                    if (picked == null) {
                        hint = true
                    } else {
                        picked?.let(onAdd)
                        picked = null
                        hint = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            // A disabled button would be drawn in mid grey, which dithers on a 1-bit panel.
            if (hint) Note("Pick an exercise from the dropdown first.")
        }
    }
}

fun equipmentLabel(exerciseId: String): String =
    ExerciseLibrary.byId(exerciseId)?.let { "${it.equip.label} · ${it.group.label}" } ?: "Custom"
