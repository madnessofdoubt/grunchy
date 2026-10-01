package com.grunchy.workout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.grunchy.workout.ui.AppRoot
import com.grunchy.workout.ui.AppState
import com.mudita.mmd.ThemeMMD

/**
 * The whole app is one activity: no fragments, no navigation graph, no transitions.
 * On E Ink a transition is a sequence of full panel refreshes, so screens are swapped
 * instantly and [AppRoot] keeps the back stack in plain state.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val state = AppState(applicationContext)
        setContent {
            // ThemeMMD: monochrome Material 3 colour scheme, Lato typography and
            // ripple globally disabled, all tuned for E Ink panels.
            ThemeMMD {
                AppRoot(state)
            }
        }
    }
}
