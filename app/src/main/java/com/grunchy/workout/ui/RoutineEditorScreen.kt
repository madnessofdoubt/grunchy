@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.grunchy.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grunchy.workout.data.ExerciseLibrary
import com.grunchy.workout.model.Exercise
import com.grunchy.workout.model.Group
import com.grunchy.workout.model.Routine
import com.grunchy.workout.model.RoutineItem
import com.grunchy.workout.model.RepRange
import com.grunchy.workout.util.defaultRepRangeLabel
import com.grunchy.workout.util.parseRangeLabel
import com.mudita.mmd.components.cards.CardMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

/**
 * Routine builder. Name comes from a dropdown (typing only if you insist), exercises come
 * from the group/exercise pair of dropdowns, targets from two more dropdowns.
 */
@Composable
fun RoutineEditorScreen(
    state: AppState,
    routineId: String?,
    onBack: () -> Unit,
) {
    val existing = routineId?.let { state.routine(it) }
    var draft by remember { mutableStateOf(existing ?: Routine(newId(), RoutineNameOptions.first())) }
    var customName by remember { mutableStateOf(existing != null && existing.name !in RoutineNameOptions) }
    var confirmDelete by remember { mutableStateOf(false) }
    var addHint by remember { mutableStateOf(false) }
    var group by remember { mutableStateOf(Group.PUSH) }
    var picked by remember { mutableStateOf<Exercise?>(null) }
    val groupExercises = remember(group) { ExerciseLibrary.byGroup(group) }

    fun save() {
        state.saveRoutine(draft)
        onBack()
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = if (existing == null) "New routine" else "Edit routine",
            navigation = { BarButton("Back", onClick = onBack) },
            actions = { BarButton("Save", filled = true, onClick = { save() }) },
        )

        ScrollList(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(Gap),
        ) {
            item { SectionLabel("Name") }
            item {
                ValueDropdown(
                    options = RoutineNameOptions + CustomNameOption,
                    selected = if (customName) CustomNameOption else draft.name,
                    optionLabel = { it },
                    onSelect = { choice ->
                        if (choice == CustomNameOption) {
                            customName = true
                        } else {
                            customName = false
                            draft = draft.copy(name = choice)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (customName) {
                item {
                    NameField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it.take(28)) },
                        label = "Routine name",
                    )
                }
            }

            item { SectionLabel("Add exercise") }
            item {
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
                        options = groupExercises,
                        selected = picked,
                        optionLabel = { it.name },
                        onSelect = { picked = it },
                        modifier = Modifier.weight(2f),
                        menuHeight = 340.dp,
                    )
                }
            }
            item {
                PrimaryAction(
                    label = "Add exercise",
                    onClick = {
                        if (picked == null) {
                            addHint = true
                        } else {
                            picked?.let { exercise ->
                                draft = draft.copy(
                                    items = draft.items + RoutineItem(
                                        exerciseId = exercise.id,
                                        targetSets = 3,
                                        targetRange = defaultRepRangeLabel(state.settings.repRanges),
                                    ),
                                )
                            }
                            picked = null
                            addHint = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            // Hint instead of a greyed-out button: greys dither on a 1-bit E Ink panel.
            if (addHint) item { Note("You need to pick an exercise from the dropdown first.") }

            item { SectionLabel("${counted(draft.items.size, "exercise")} · ${counted(draft.setCount, "set")}") }
            if (draft.items.isEmpty()) {
                item { Note("No exercises in this routine yet. Pick one from the dropdown above and add it.") }
            } else {
                itemsIndexed(draft.items, key = { index, item -> "${item.exerciseId}_$index" }) { index, item ->
                    RoutineItemCard(
                        index = index,
                        item = item,
                        total = draft.items.size,
                        ranges = state.settings.repRanges,
                        onChange = { changed ->
                            draft = draft.copy(
                                items = draft.items.mapIndexed { i, old -> if (i == index) changed else old },
                            )
                        },
                        onMove = { delta ->
                            val target = index + delta
                            if (target in draft.items.indices) {
                                val moved = draft.items.toMutableList()
                                val item2 = moved.removeAt(index)
                                moved.add(target, item2)
                                draft = draft.copy(items = moved)
                            }
                        },
                        onRemove = {
                            draft = draft.copy(items = draft.items.filterIndexed { i, _ -> i != index })
                        },
                    )
                }
            }

            item { Spacer(Modifier.height(Gap)) }
            if (existing != null) {
                item {
                    if (confirmDelete) {
                        InlineConfirm(
                            message = "Delete “${draft.name}”? Your workouts you've already logged will not be deleted.",
                            confirmLabel = "Delete",
                            onConfirm = {
                                state.deleteRoutine(draft.id)
                                onBack()
                            },
                            onCancel = { confirmDelete = false },
                        )
                    } else {
                        SecondaryAction(
                            label = "Delete routine",
                            onClick = { confirmDelete = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                PrimaryAction(
                    label = "Save routine",
                    onClick = { save() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RoutineItemCard(
    index: Int,
    item: RoutineItem,
    total: Int,
    ranges: List<RepRange>,
    onChange: (RoutineItem) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    CardMMD(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(Modifier.weight(1f)) {
                TextMMD(
                    text = "${index + 1}. ${ExerciseLibrary.nameOf(item.exerciseId)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Note(equipmentLabel(item.exerciseId))
            }
            BarButton("↑", onClick = { onMove(-1) }, enabled = index > 0)
            BarButton("↓", onClick = { onMove(1) }, enabled = index < total - 1)
            BarButton("✕", onClick = onRemove)
        }
        ThinDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Gap),
        ) {
            TextMMD("Target", fontSize = 13.sp)
            ValueDropdown(
                options = SetOptions,
                selected = item.targetSets,
                optionLabel = { "$it" },
                onSelect = { onChange(item.copy(targetSets = it)) },
                modifier = Modifier.weight(1f),
                suffix = "sets",
                bold = true,
            )
            TextMMD("×", fontSize = 14.sp)
            ValueDropdown(
                options = ranges,
                // Falls back to the stored label if the range has since been edited away.
                selected = ranges.firstOrNull { it.label == item.targetRange }
                    ?: parseRangeLabel(item.targetRange),
                optionLabel = { it.label },
                menuOptionLabel = { if (it.amrap) it.label else "${it.label} reps" },
                onSelect = { onChange(item.copy(targetRange = it.label)) },
                modifier = Modifier.weight(1f),
                bold = true,
            )
        }
    }
}
