package com.grunchy.workout.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screens are plain state, not a navigation graph: there is nothing to animate, so a
 * navigation library would only add a dependency and a transition.
 */
sealed interface Screen {
    data object Plan : Screen
    data object History : Screen
    data object Progress : Screen
    data object Settings : Screen
    data class RoutineEditor(val routineId: String?) : Screen
    data object Workout : Screen
    data class SessionDetail(val sessionId: String) : Screen
    data class Graph(val exerciseId: String) : Screen
}

@Composable
fun AppRoot(state: AppState) {
    var stack by remember { mutableStateOf(listOf<Screen>(Screen.Plan)) }
    var workout by remember { mutableStateOf<ActiveWorkout?>(null) }
    val current = stack.last()

    fun push(screen: Screen) {
        stack = stack + screen
    }

    fun pop() {
        stack = if (stack.size > 1) stack.dropLast(1) else listOf(Screen.Plan)
    }

    fun goHome() {
        stack = listOf(Screen.Plan)
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(Modifier.weight(1f)) {
            when (current) {
                is Screen.Plan -> HomeScreen(
                    state = state,
                    onStart = { routine ->
                        workout = startWorkout(state, routine)
                        push(Screen.Workout)
                    },
                    onEditRoutine = { routineId -> push(Screen.RoutineEditor(routineId)) },
                    onOpenSettings = { push(Screen.Settings) },
                )

                is Screen.RoutineEditor -> RoutineEditorScreen(
                    state = state,
                    routineId = current.routineId,
                    onBack = { pop() },
                )

                is Screen.Workout -> {
                    val active = workout
                    if (active == null) {
                        // Nothing to log (e.g. finished elsewhere): fall back to the plan.
                        LaunchedEffect(Unit) { goHome() }
                    } else {
                        WorkoutScreen(
                            state = state,
                            workout = active,
                            onFinish = { finished ->
                                state.finishSession(finished, active.lastWeights())
                                workout = null
                                stack = listOf(Screen.Plan, Screen.SessionDetail(finished.id))
                            },
                            onDiscard = {
                                workout = null
                                goHome()
                            },
                        )
                    }
                }

                is Screen.History -> HistoryScreen(
                    state = state,
                    onOpenSession = { sessionId -> push(Screen.SessionDetail(sessionId)) },
                    onOpenSettings = { push(Screen.Settings) },
                )

                is Screen.SessionDetail -> SessionDetailScreen(
                    state = state,
                    sessionId = current.sessionId,
                    onBack = { pop() },
                )

                is Screen.Progress -> ProgressScreen(
                    state = state,
                    onOpenSettings = { push(Screen.Settings) },
                    onOpenGraph = { exerciseId -> push(Screen.Graph(exerciseId)) },
                )

                is Screen.Graph -> GraphScreen(
                    state = state,
                    exerciseId = current.exerciseId,
                    onBack = { pop() },
                )

                is Screen.Settings -> SettingsScreen(
                    state = state,
                    onBack = { pop() },
                )
            }
        }

        if (current.isTab()) {
            BottomBar(current = current, onSelect = { tab -> stack = listOf(tab) })
        }
    }
}

private fun Screen.isTab(): Boolean =
    this == Screen.Plan || this == Screen.History || this == Screen.Progress

private fun Screen.tabLabel(): String = when (this) {
    Screen.Plan -> "Plan"
    Screen.History -> "History"
    Screen.Progress -> "Stats"
    else -> ""
}

/**
 * Hand rolled instead of NavigationBarMMD: the MMD bar animates its selection indicator,
 * which on E Ink means a visible ghosting trail every time you switch tabs.
 *
 * Settings is not a tab any more — it sits behind the sliders button in the app bar — so
 * three tabs share the width, get taller targets, and the row is inset from the panel edge
 * instead of being pinned to it by a divider.
 */
@Composable
private fun BottomBar(current: Screen, onSelect: (Screen) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(Screen.Plan, Screen.History, Screen.Progress).forEach { tab ->
            BarButton(
                label = tab.tabLabel(),
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
                filled = tab == current,
                height = TabHeight,
                labelSize = 17.sp,
            )
        }
    }
}
