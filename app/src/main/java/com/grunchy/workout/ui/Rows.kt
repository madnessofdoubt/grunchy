package com.grunchy.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grunchy.workout.data.ExerciseLibrary
import com.grunchy.workout.model.Routine
import com.grunchy.workout.model.Session
import com.grunchy.workout.util.fmtDate
import com.mudita.mmd.components.cards.CardMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun RoutineCard(
    routine: Routine,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardMMD(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            TextMMD(
                text = routine.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Note(routineSummary(routine))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(Gap),
        ) {
            PrimaryAction("Start", onClick = onStart, modifier = Modifier.weight(1f))
            SecondaryAction("Edit", onClick = onEdit, modifier = Modifier.weight(1f))
        }
    }
}

/** "1 exercise" / "4 exercises" — a count the user reads, so it has to agree with its noun. */
internal fun counted(count: Int, noun: String): String =
    if (count == 1) "$count $noun" else "$count ${noun}s"

fun routineSummary(routine: Routine): String {
    if (routine.items.isEmpty()) return "This routine is empty. Tap Edit to build it."
    val names = routine.items.joinToString(", ") { ExerciseLibrary.nameOf(it.exerciseId) }
    return "${counted(routine.items.size, "exercise")} · ${counted(routine.setCount, "set")}\n$names"
}

@Composable
fun SessionRow(session: Session, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CardMMD(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextMMD(
                    text = session.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextMMD(fmtDate(session.finishedAt), fontSize = 12.sp, maxLines = 1)
            }
            Note(sessionSummary(session))
        }
    }
}

fun sessionSummary(session: Session): String =
    "${counted(session.exerciseCount, "exercise")} · ${counted(session.setCount, "set")}"
