package com.pj95.clockwise.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.pj95.clockwise.ClockwiseApp
import com.pj95.clockwise.data.prefs.UserPreferences
import com.pj95.clockwise.model.timer.CountdownTimer
import com.pj95.clockwise.ui.screens.TimerRingingScreen
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import kotlinx.coroutines.launch

/**
 * The screen a finished timer shows, over the lock screen.
 *
 * Separate from [com.pj95.clockwise.MainActivity] for the same reason as the
 * alarm's ringing screen: the timer's notification is a full-screen intent, and
 * the screen has to work when the app was not running at all. It reads its row
 * from the repository rather than carrying the duration in the Intent.
 */
class TimerRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        showOverLockScreen()

        // Actions delivered without the activity already on screen (the
        // notification's Dismiss action) run straight away.
        when (intent?.action) {
            ACTION_DISMISS -> {
                dismiss()
                return
            }
            ACTION_RESTART -> {
                restart()
                return
            }
        }

        val container = (application as ClockwiseApp).container
        setContent {
            var timer: CountdownTimer? by remember { mutableStateOf(null) }
            var loaded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                timer = container.timerRepository.get()
                loaded = true
                if (timer == null) finish()
            }

            val preferences by container.clockRepository.preferences
                .collectAsStateWithLifecycle(initialValue = UserPreferences.Default)

            FixedClockwiseTheme(theme = preferences.theme) {
                TimerRingingScreen(
                    timer = timer,
                    isLoading = !loaded,
                    onDismiss = ::dismiss,
                    onRestart = ::restart,
                )
            }
        }
    }

    private fun dismiss() {
        lifecycleScope.launch {
            (application as ClockwiseApp).container.timerRepository.reset()
            finish()
        }
    }

    private fun restart() {
        lifecycleScope.launch {
            val repository = (application as ClockwiseApp).container.timerRepository
            val duration = repository.get()?.durationMillis
            if (duration != null) repository.start(duration)
            finish()
        }
    }

    /** Draws over the keyguard and wakes the display, as a ringing timer must. */
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val ACTION_SHOW: String = "com.pj95.clockwise.action.SHOW_TIMER"
        const val ACTION_DISMISS: String = "com.pj95.clockwise.action.DISMISS_TIMER_ALERT"
        const val ACTION_RESTART: String = "com.pj95.clockwise.action.RESTART_TIMER"
    }
}
