package com.grunchy.workout.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.grunchy.workout.data.Store
import com.grunchy.workout.model.AppData
import com.grunchy.workout.model.Routine
import com.grunchy.workout.model.Session
import com.grunchy.workout.model.Settings
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString().substring(0, 8)

/**
 * Single source of truth. Every mutation writes straight through to disk: taps are
 * infrequent by design (that is the point of the app), so the cost is nil and a killed
 * process or flat battery can never cost you a logged set.
 */
class AppState(private val context: Context) {

    var data: AppData by mutableStateOf(Store.load(context))
        private set

    val routines: List<Routine> get() = data.routines
    val sessions: List<Session> get() = data.sessions
    val settings: Settings get() = data.settings

    fun mutate(block: (AppData) -> AppData) {
        val next = block(data)
        if (next == data) return
        data = next
        Store.save(context, next)
    }

    fun routine(id: String): Routine? = routines.firstOrNull { it.id == id }

    fun session(id: String): Session? = sessions.firstOrNull { it.id == id }

    fun saveRoutine(routine: Routine) = mutate { d ->
        val exists = d.routines.any { it.id == routine.id }
        d.copy(
            routines = if (exists) {
                d.routines.map { if (it.id == routine.id) routine else it }
            } else {
                d.routines + routine
            },
        )
    }

    fun deleteRoutine(id: String) = mutate { d ->
        d.copy(routines = d.routines.filterNot { it.id == id })
    }

    /** Records a finished workout and remembers the working weight per exercise. */
    fun finishSession(session: Session, lastWeights: Map<String, Double>) = mutate { d ->
        d.copy(
            sessions = d.sessions + session,
            settings = d.settings.copy(lastWeightKg = d.settings.lastWeightKg + lastWeights),
        )
    }

    fun deleteSession(id: String) = mutate { d ->
        d.copy(sessions = d.sessions.filterNot { it.id == id })
    }

    fun updateSettings(settings: Settings) = mutate { d -> d.copy(settings = settings) }

    fun clearHistory() = mutate { d -> d.copy(sessions = emptyList()) }
}
