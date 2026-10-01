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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.cards.CardMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

private const val WEEK_MILLIS = 7L * 24L * 60L * 60L * 1000L

@Composable
fun HistoryScreen(
    state: AppState,
    onOpenSession: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val sessions = state.sessions.sortedByDescending { it.finishedAt }
    val now = System.currentTimeMillis()
    val thisWeek = sessions.count { it.finishedAt >= now - WEEK_MILLIS }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "History", actions = { SettingsIcon(onOpenSettings) })
        ScrollList(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(Gap),
        ) {
            item {
                CardMMD(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        KeyValueRow("Workouts logged", "${sessions.size}")
                        KeyValueRow("This week", "$thisWeek")
                        KeyValueRow("Sets logged", "${sessions.sumOf { it.setCount }}")
                    }
                }
            }

            if (sessions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp)) {
                        Note("You haven't logged a workout yet. Once you do, it will appear here.")
                    }
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(session = session, onClick = { onOpenSession(session.id) })
                }
            }
        }
    }
}
