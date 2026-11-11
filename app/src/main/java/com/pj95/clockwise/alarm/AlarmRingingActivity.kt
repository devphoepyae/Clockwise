package com.pj95.clockwise.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.pj95.clockwise.ClockwiseApp
import com.pj95.clockwise.data.prefs.UserPreferences
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.ui.screens.AlarmRingingScreen
import com.pj95.clockwise.ui.theme.AlarmRingingTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The screen a ringing alarm shows, over the lock screen.
 *
 * It is a separate Activity rather than a dialog inside [com.pj95.clockwise.MainActivity]
 * because it has to survive the case the app is not running at all: the alarm
 * broadcast starts this activity through a full-screen intent, and it reads its
 * own row straight from the repository.
 *
 * Two ways out of a ring since v1.0.1: the deliberate slide to stop, and a
 * swipe up anywhere to snooze. The notification offers the same pair directly,
 * and all four entry points run the same methods here.
 */
class AlarmRingingActivity : ComponentActivity() {

    private var alarmId: Long = NO_ALARM

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, NO_ALARM) ?: NO_ALARM
        if (alarmId == NO_ALARM) {
            finish()
            return
        }

        // An action delivered while the activity was not running (a notification
        // tap) is performed straight away, without drawing a ringing screen the
        // user did not ask for.
        when (intent?.action) {
            ACTION_DISMISS -> {
                dismiss()
                return
            }
            ACTION_SNOOZE -> {
                snooze()
                return
            }
        }

        showOverLockScreen()

        val container = (application as ClockwiseApp).container
        setContent {
            var alarm: Alarm? by remember { mutableStateOf(null) }
            var loaded by remember { mutableStateOf(false) }

            LaunchedEffect(alarmId) {
                alarm = container.alarmRepository.get(alarmId)
                loaded = true
                // The row can be gone if it was deleted before the ring landed.
                if (alarm == null) finish()
            }

            val preferences by container.clockRepository.preferences
                .collectAsStateWithLifecycle(initialValue = UserPreferences.Default)

            // The ringing screen has its own fixed navy-and-gold palette since
            // v1.0.1: an alarm at 06:30 should look the same on every phone and
            // must never flash a bright theme.
            AlarmRingingTheme {
                AlarmRingingScreen(
                    alarm = alarm,
                    use24Hour = preferences.use24Hour,
                    isLoading = !loaded,
                    onDismiss = ::dismiss,
                    onSnooze = ::snooze,
                )
            }
        }
    }

    /**
     * `singleTop` plus this method: a notification action reaches the activity
     * that is already ringing instead of stacking a second one.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getLongExtra(EXTRA_ALARM_ID, NO_ALARM)
            .takeIf { it != NO_ALARM }
            ?.let { alarmId = it }

        when (intent.action) {
            ACTION_DISMISS -> dismiss()
            ACTION_SNOOZE -> snooze()
        }
    }

    /**
     * Stop: the one action that ends a ring for good.
     *
     * The sound and the notification are stopped before the database write so
     * the phone goes quiet on the tap, not after a disk round-trip.
     */
    private fun dismiss() {
        AlarmNotifications.cancel(this, alarmId)
        AlarmRingingService.stop(this)
        finish()
    }

    /**
     * Snooze: silence now, ring again in [SNOOZE_MINUTES].
     *
     * The repository write and the `AlarmManager` call must outlive this
     * activity, which is finishing immediately, so they run on a process-scoped
     * coroutine rather than `lifecycleScope`.
     */
    private fun snooze() {
        if (alarmId == NO_ALARM) {
            finish()
            return
        }

        AlarmNotifications.cancel(this, alarmId)
        AlarmRingingService.stop(this)

        val repository = (application as ClockwiseApp).container.alarmRepository
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { repository.snooze(alarmId, SNOOZE_MINUTES) }
                .onFailure { error ->
                    Log.w(TAG, "Alarm $alarmId could not be snoozed", error)
                }
        }
        finish()
    }

    /**
     * Draws over the keyguard and wakes the display, as a ringing alarm must.
     *
     * The legacy window flags are applied on every API level as well as the
     * modern setters: some OEM builds honour one but not the other, and an alarm
     * that stays dark behind the keyguard is worse than a duplicated flag.
     */
    private fun showOverLockScreen() {
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    companion object {
        /** The alarm this screen is ringing. */
        const val EXTRA_ALARM_ID: String = "com.pj95.clockwise.extra.RINGING_ALARM_ID"

        /** Brings the ringing screen up; used by the notification's content tap. */
        const val ACTION_SHOW: String = "com.pj95.clockwise.action.SHOW_ALARM"

        /** Stops the ring for good. */
        const val ACTION_DISMISS: String = "com.pj95.clockwise.action.DISMISS_ALARM"

        /** Silences the ring and brings it back after [SNOOZE_MINUTES]. */
        const val ACTION_SNOOZE: String = "com.pj95.clockwise.action.SNOOZE_ALARM"

        /** The conventional alarm snooze: long enough to be useful, short enough to matter. */
        private const val SNOOZE_MINUTES = 10

        private const val NO_ALARM = -1L
        private const val TAG = "AlarmRingingActivity"
    }
}
