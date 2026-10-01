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
import com.grunchy.workout.model.Group
import com.grunchy.workout.util.MaxRepsForE1rm
import com.grunchy.workout.util.fmtDate
import com.grunchy.workout.util.fmtNumber
import com.grunchy.workout.util.fmtWeight
import com.grunchy.workout.util.fmtWeightValue
import com.grunchy.workout.util.pointsFor
import com.mudita.mmd.components.cards.CardMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * What the ⓘ on this page explains: the estimate behind every number on it, and the AMRAP
 * choice in the rep range picker. The user's own wording — keep it verbatim.
 */
private const val InfoText =
    "Estimated 1RM - Using Epley's formula, the app infers your probable best one rep max " +
        "from a set you did. According to Epley, each rep you complete means you could have " +
        "managed 3.3% more for a single rep. So, 220 lbs x 5 is then 5 x 3.3% which is approx. " +
        "17%. The one rep max in this example then is approx. 257 lbs.\n\n" +
        "AMRAP - As many reps as possible."

/** Per-exercise progression: pick an exercise, see every session it appeared in. */
@Composable
fun ProgressScreen(
    state: AppState,
    onOpenSettings: () -> Unit,
    onOpenGraph: (String) -> Unit,
) {
    val unit = state.settings.weightUnit
    var showInfo by remember { mutableStateOf(false) }
    // Default to whatever you trained most recently: that is the trend you came to see.
    val lastTrained = remember(state.sessions) {
        state.sessions.maxByOrNull { it.finishedAt }?.logs?.firstOrNull()?.exerciseId
    }
    var group by remember { mutableStateOf(ExerciseLibrary.byId(lastTrained ?: "")?.group ?: Group.PUSH) }
    val groupExercises = remember(group) { ExerciseLibrary.byGroup(group) }
    // remember(group) re-initialises the selection: switching group can't leave a stale pick.
    var picked by remember(group) {
        mutableStateOf(groupExercises.firstOrNull { it.id == lastTrained } ?: groupExercises.firstOrNull())
    }
    val points = remember(picked, state.sessions) {
        picked?.let { pointsFor(it.id, state.sessions) } ?: emptyList()
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                title = "Stats",
                actions = {
                    InfoIcon(onClick = { showInfo = true })
                    SettingsIcon(onOpenSettings)
                },
            )
            ScrollList(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(Gap),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Gap),
                    ) {
                        ValueDropdown(
                            options = GroupOptions,
                            selected = group,
                            optionLabel = { it.label },
                            onSelect = { group = it },
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

                if (points.isEmpty()) {
                    item {
                        Note(
                            "No sets logged for ${picked?.name ?: "this exercise"} yet. " +
                                "Log a workout that includes it and the trend shows up here.",
                        )
                    }
                } else {
                    // Across mixed rep ranges the estimate is the only fair comparison; if no set
                    // was short enough to estimate, fall back to the heaviest top set.
                    val best = points.filter { it.e1rmKg != null }.maxByOrNull { it.e1rmKg ?: 0.0 }
                        ?: points.maxByOrNull { it.topWeightKg }
                    item {
                        CardMMD(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                KeyValueRow("Sessions", "${points.size}")
                                KeyValueRow("Last time", fmtDate(points.first().date))
                                KeyValueRow(
                                    key = "Best set",
                                    value = "${fmtWeight(best?.topWeightKg ?: 0.0, unit)} × ${best?.topReps ?: 0}",
                                )
                                best?.e1rmKg?.let {
                                    KeyValueRow("Best estimated 1RM", fmtWeight(it, unit))
                                }
                            }
                        }
                    }
                    picked?.let { exercise ->
                        item {
                            PrimaryAction(
                                label = "View as graph",
                                onClick = { onOpenGraph(exercise.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    item { SectionLabel("Progression") }

                    val scale = (points.mapNotNull { it.e1rmKg }.maxOrNull()
                        ?: points.maxOf { it.topWeightKg }).coerceAtLeast(1.0)
                    itemsIndexed(points.take(24), key = { _, point -> point.sessionId }) { index, point ->
                        val older = points.getOrNull(index + 1)
                        val delta = older?.let { point.topWeightKg - it.topWeightKg }
                        // Bound to a non-null value first: the difference is computed in kilos and
                        // only becomes a label here, in the unit on screen.
                        val deltaLabel = delta?.let {
                            when {
                                it > 0 -> "↑ +${fmtWeightValue(it, unit)}"
                                it < 0 -> "↓ ${fmtWeightValue(it, unit)}"
                                else -> "="
                            }
                        }.orEmpty()
                        CardMMD(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextMMD(fmtDate(point.date), Modifier.width(78.dp), fontSize = 12.sp)
                                    TextMMD(
                                        text = "${fmtWeight(point.topWeightKg, unit)} × ${point.topReps}",
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    TextMMD(
                                        text = deltaLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                StaticBar(
                                    fraction = ((point.e1rmKg ?: point.topWeightKg) / scale).toFloat(),
                                )
                                Spacer(Modifier.height(2.dp))
                                Note(
                                    point.e1rmKg?.let { "estimated 1RM ${fmtWeight(it, unit)}" }
                                        ?: "All sets were over $MaxRepsForE1rm reps, so there is no 1RM to estimate.",
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showInfo) {
            InfoCard(text = InfoText, onClose = { showInfo = false })
        }
    }
}
