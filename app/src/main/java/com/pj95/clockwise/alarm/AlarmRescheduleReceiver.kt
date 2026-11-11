package com.pj95.clockwise.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.pj95.clockwise.ClockwiseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Puts every enabled alarm back into `AlarmManager` after a reboot or a clock
 * change.
 *
 * `AlarmManager` forgets its schedule when the device restarts, and a manual
 * time change or a timezone change moves what "07:30" means. Both are events the
 * system broadcasts; re-arming here is what makes an alarm survive the night
 * rather than only the session that created it.
 */
class AlarmRescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return

        val app = context.applicationContext as? ClockwiseApp ?: return
        val pending = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.alarmRepository.rescheduleAll()
                app.container.timerRepository.rescheduleRunning()
            } catch (error: Exception) {
                Log.e(TAG, "Alarms could not be re-armed after ${intent.action}", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReschedule"

        private val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
