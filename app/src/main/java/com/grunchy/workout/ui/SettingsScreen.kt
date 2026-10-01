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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grunchy.workout.model.AmrapLabel
import com.grunchy.workout.model.RepRange
import com.grunchy.workout.model.WeightUnit
import com.grunchy.workout.util.coarseStepChoices
import com.grunchy.workout.util.fineStepChoices
import com.grunchy.workout.util.fmtNumber
import com.grunchy.workout.util.maxWeightChoices
import com.grunchy.workout.util.withWeightUnit
import com.grunchy.workout.util.nextRepRange
import com.grunchy.workout.util.replaceRepRange
import com.grunchy.workout.util.withRepRange
import com.mudita.mmd.components.cards.CardMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

@Composable
fun SettingsScreen(state: AppState, onBack: () -> Unit) {
    val settings = state.settings
    val unit = settings.weightUnit
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Settings", navigation = { BarButton("Back", onClick = onBack) })
        ScrollList(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(Gap),
        ) {
            item { SectionLabel("Rep ranges") }
            item {
                CardMMD(Modifier.fillMaxWidth()) {
                    settings.repRanges.forEachIndexed { index, range ->
                        ThinDivider()
                        RepRangeRow(
                            range = range,
                            onChange = { changed ->
                                state.updateSettings(
                                    settings.copy(
                                        repRanges = replaceRepRange(settings.repRanges, index, changed),
                                    ),
                                )
                            },
                            // Keep at least one range: a routine with no target has nothing to pick.
                            onRemove = if (settings.repRanges.size > 1) {
                                {
                                    state.updateSettings(
                                        settings.copy(
                                            repRanges = settings.repRanges.filterIndexed { i, _ -> i != index },
                                        ),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                    ThinDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        SecondaryAction(
                            label = "+ Add range",
                            onClick = {
                                state.updateSettings(
                                    settings.copy(
                                        repRanges = withRepRange(
                                            settings.repRanges,
                                            nextRepRange(settings.repRanges),
                                        ),
                                    ),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item { SectionLabel("Units and steps") }
            item {
                CardMMD(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(Gap),
                    ) {
                        TextMMD("Weight unit", fontSize = 13.sp)
                        ValueDropdown(
                            options = WeightUnit.entries.toList(),
                            selected = settings.weightUnit,
                            optionLabel = { it.label },
                            onSelect = { state.updateSettings(settings.withWeightUnit(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            bold = true,
                        )
                        TextMMD("Weight jumps in the dropdown", fontSize = 13.sp)
                        ValueDropdown(
                            options = coarseStepChoices(unit),
                            selected = settings.coarseStep,
                            optionLabel = { "${fmtNumber(it)} ${unit.suffix}" },
                            onSelect = { state.updateSettings(settings.copy(coarseStep = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            bold = true,
                        )
                        TextMMD("Fine step for the ± buttons", fontSize = 13.sp)
                        ValueDropdown(
                            options = fineStepChoices(unit),
                            selected = settings.fineStep,
                            optionLabel = { "${fmtNumber(it)} ${unit.suffix}" },
                            onSelect = { state.updateSettings(settings.copy(fineStep = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            bold = true,
                        )
                        TextMMD("Heaviest weight in the list", fontSize = 13.sp)
                        ValueDropdown(
                            options = maxWeightChoices(unit),
                            selected = settings.maxWeight,
                            optionLabel = { "${fmtNumber(it)} ${unit.suffix}" },
                            onSelect = { state.updateSettings(settings.copy(maxWeight = it)) },
                            modifier = Modifier.fillMaxWidth(),
                            bold = true,
                        )
                    }
                }
            }

            item { SectionLabel("Data") }
            item {
                CardMMD(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        KeyValueRow("Workouts stored", "${state.sessions.size}")
                        KeyValueRow("Routines", "${state.routines.size}")
                        KeyValueRow("Stored as", "JSON in app files")
                    }
                }
            }
            item {
                if (state.sessions.isEmpty()) {
                    Note("You have not logged any workouts yet, so there is nothing to clear.")
                } else if (confirmClear) {
                    InlineConfirm(
                        message = "Delete all ${counted(state.sessions.size, "logged workout")}? Your routines will not be deleted.",
                        confirmLabel = "Clear history",
                        onConfirm = {
                            state.clearHistory()
                            confirmClear = false
                        },
                        onCancel = { confirmClear = false },
                    )
                } else {
                    SecondaryAction(
                        label = "Clear workout history",
                        onClick = { confirmClear = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * One editable range, as "from 5 to 10" with a "no limit" upper end for the 15+ style range.
 * Two dropdowns means shaping a range never involves the keyboard.
 */
@Composable
private fun RepRangeRow(
    range: RepRange,
    onChange: (RepRange) -> Unit,
    onRemove: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ValueDropdown(
            options = RepRangeStartOptions,
            // AMRAP has no lower bound either: choosing it replaces the whole range, which is
            // why it lives in this control and the upper one disappears.
            selected = if (range.amrap) null else range.minReps,
            optionLabel = { it?.toString() ?: AmrapLabel },
            menuOptionLabel = { it?.let { n -> "$n reps" } ?: AmrapLabel },
            nullLabel = AmrapLabel,
            onSelect = { picked ->
                onChange(
                    if (picked == null) range.copy(amrap = true)
                    else range.copy(minReps = picked, amrap = false),
                )
            },
            modifier = Modifier.weight(1f),
            bold = true,
        )
        if (!range.amrap) {
            // The separator belongs to a closed range: "15 to 20" reads, "15 to and up" does not.
            if (range.maxReps != null) {
                TextMMD("to", fontSize = 12.sp)
            }
            ValueDropdown(
                options = RepRangeMaxOptions,
                selected = range.maxReps,
                optionLabel = { it?.toString() ?: "and up" },
                menuOptionLabel = { it?.let { n -> "$n reps" } ?: "and up" },
                nullLabel = "and up",
                onSelect = { onChange(range.copy(maxReps = it)) },
                modifier = Modifier.weight(1f),
                bold = true,
            )
        }
        if (onRemove != null) {
            BarButton("✕", onClick = onRemove)
        } else {
            Spacer(Modifier.width(BarHeight))
        }
    }
}
