@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.grunchy.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.grunchy.workout.model.Session
import com.grunchy.workout.model.WeightUnit
import com.grunchy.workout.util.fmtDateFull
import com.grunchy.workout.util.fmtWeight
import com.grunchy.workout.util.fmtDuration
import com.grunchy.workout.util.fmtTime
import com.mudita.mmd.components.cards.CardMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

/** Read-only view of one finished workout, also shown right after you press Finish. */
@Composable
fun SessionDetailScreen(state: AppState, sessionId: String, onBack: () -> Unit) {
    val session = state.session(sessionId)
    val unit = state.settings.weightUnit
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = session?.title ?: "Workout",
            subtitle = session?.let {
                "${fmtDateFull(it.finishedAt)} · " +
                    "${fmtTime(it.startedAt)}–${fmtTime(it.finishedAt)}"
            },
            navigation = { BarButton("Back", onClick = onBack) },
        )

        if (session == null) {
            Column(Modifier.padding(16.dp)) { Note("This workout is no longer in your history.") }
        } else {
            SessionBody(session, unit, confirmDelete, { confirmDelete = true }, { confirmDelete = false }) {
                state.deleteSession(sessionId)
                onBack()
            }
        }
    }
}

@Composable
private fun SessionBody(
    session: Session,
    unit: WeightUnit,
    confirmDelete: Boolean,
    onAskDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onDelete: () -> Unit,
) {
    ScrollList(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        item {
            CardMMD(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    KeyValueRow("Exercises", "${session.exerciseCount}")
                    KeyValueRow("Sets", "${session.setCount}")
                    KeyValueRow("Took", fmtDuration(session.finishedAt - session.startedAt))
                }
            }
        }

        itemsIndexed(session.logs) { index, log ->
            CardMMD(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    TextMMD(
                        text = "${index + 1}. ${ExerciseLibrary.nameOf(log.exerciseId)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.padding(top = 4.dp))
                    log.sets.forEachIndexed { setIndex, set ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextMMD("Set ${setIndex + 1}", Modifier.width(56.dp), fontSize = 12.sp)
                            TextMMD(
                                text = "${fmtWeight(set.weightKg, unit)} × ${set.reps}",
                                modifier = Modifier.weight(1f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        item {
            if (confirmDelete) {
                InlineConfirm(
                    message = "Delete this workout? This cannot be undone.",
                    confirmLabel = "Delete",
                    onConfirm = onDelete,
                    onCancel = onCancelDelete,
                )
            } else {
                SecondaryAction(
                    label = "Delete workout",
                    onClick = onAskDelete,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
