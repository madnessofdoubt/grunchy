@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.grunchy.workout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.grunchy.workout.model.Routine

/**
 * What the ⓘ explains. The name is Bulgarian, and the sentence is the user's own wording: keep it
 * verbatim, Cyrillic and all.
 */
private const val About =
    "Grunchy, from the bulgarian \"грънчар\" [ɡrɤnt͡ʃar] - a potter, is a workout tracker app " +
        "built for the Mudita Kompakt."

/**
 * The Plan tab: start something, or build a routine. Everything on this screen is a tap.
 */
@Composable
fun HomeScreen(
    state: AppState,
    onStart: (Routine?) -> Unit,
    onEditRoutine: (String?) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val routines = state.routines
    var showAbout by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                title = "Grunchy",
                actions = {
                    InfoIcon(onClick = { showAbout = true })
                    SettingsIcon(onOpenSettings)
                },
            )
            ScrollList(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(Gap),
            ) {
                item {
                    PrimaryAction(
                        label = "Freestyle workout",
                        onClick = { onStart(null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item { SectionLabel("My routines") }
                if (routines.isEmpty()) {
                    item {
                        Note(
                            "You haven't created a workout program yet. Create a routine (a training " +
                                "day) below and use the dropdowns to choose muscle groups, exercises, " +
                                "sets and reps.",
                        )
                    }
                } else {
                    items(routines, key = { it.id }) { routine ->
                        RoutineCard(
                            routine = routine,
                            onStart = { onStart(routine) },
                            onEdit = { onEditRoutine(routine.id) },
                        )
                    }
                }
                item {
                    SecondaryAction(
                        label = "New routine",
                        onClick = { onEditRoutine(null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (showAbout) {
            InfoCard(text = About, onClose = { showAbout = false }, fontFamily = FontFamily.SansSerif)
        }
    }
}


