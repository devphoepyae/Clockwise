package com.pj95.clockwise.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pj95.clockwise.ClockwiseApp
import com.pj95.clockwise.data.repository.TimerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives the timer trigger and the notification's actions.
 *
 * Every entry point runs the same repository methods the screen uses, so a Pause
 * tapped on the shade and a Pause tapped in the app cannot mean different
 * things. The broadcast can arrive in a process with nothing else running, so
 * the work is asynchronous and [goAsync] keeps the process alive until the
 * notification has been updated.
 */
class TimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? ClockwiseApp ?: return
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = app.container.timerRepository
                when (action) {
                    ACTION_FIRE -> deliver(repository, appContext)
                    ACTION_PAUSE -> repository.pause()
                    ACTION_RESUME -> repository.resume()
                    ACTION_CANCEL, ACTION_DISMISS -> repository.reset()
                }
            } catch (error: Exception) {
                Log.e(TAG, "Timer action $action could not be delivered", error)
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * Rings only if the stored timer really is due.
     *
     * A wake-up can arrive a moment early, after the user has already paused or
     * reset the timer, or after the row was rewritten. Checking the row here
     * rather than trusting the broadcast keeps the two in step.
     */
    private suspend fun deliver(
        repository: TimerRepository,
        context: Context,
    ) {
        val timer = repository.get()
        if (timer == null || !timer.running) {
            TimerNotifications.cancelRunning(context)
            return
        }

        val endAt = timer.endAtEpochMillis
        if (endAt != null && endAt > System.currentTimeMillis()) {
            // Early: put the wake-up back rather than ringing ahead of time.
            repository.rescheduleRunning()
        } else {
            repository.finish()
        }
    }

    companion object {
        /** Only this app's own `PendingIntent` sends these, so it is not exported. */
        const val ACTION_FIRE: String = "com.pj95.clockwise.action.FIRE_TIMER"
        const val ACTION_PAUSE: String = "com.pj95.clockwise.action.PAUSE_TIMER"
        const val ACTION_RESUME: String = "com.pj95.clockwise.action.RESUME_TIMER"
        const val ACTION_CANCEL: String = "com.pj95.clockwise.action.CANCEL_TIMER"
        const val ACTION_DISMISS: String = "com.pj95.clockwise.action.DISMISS_TIMER"

        private const val TAG = "TimerReceiver"

        private val HANDLED_ACTIONS = setOf(
            ACTION_FIRE,
            ACTION_PAUSE,
            ACTION_RESUME,
            ACTION_CANCEL,
            ACTION_DISMISS,
        )
    }
}
