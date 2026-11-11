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
 * Receives the alarm trigger and rings.
 *
 * The broadcast can arrive in a process that has nothing else running, so every
 * piece of work is asynchronous and [goAsync] keeps the process alive until the
 * ringing service has been started. Between the two, the next occurrence is
 * armed: a repeating alarm rolls forward, and a one-shot alarm switches itself
 * off so it cannot ring again tomorrow by accident.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, NO_ALARM)
        if (alarmId == NO_ALARM) return

        val app = context.applicationContext as? ClockwiseApp ?: return
        val pending = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val alarm = app.container.alarmRepository.get(alarmId)
                if (alarm == null) {
                    // Deleted between arming and firing. Nothing to ring, and no
                    // reason to replay a notification for a row that is gone.
                    AlarmNotifications.cancel(context, alarmId)
                    return@launch
                }

                // A marker means this fire is the snooze the user asked for,
                // not a fresh morning. Clearing it keeps the cycle count; a
                // natural fire starts a clean cycle instead.
                if (alarm.snoozedAtEpochMillis != null) {
                    app.container.alarmRepository.consumeSnooze(alarmId)
                } else {
                    app.container.alarmRepository.resetSnoozeCycle(alarmId)
                }

                // The alarm is armed for its next occurrence before the ring is
                // built, so a one-shot alarm cannot ring again tomorrow. A
                // scheduled alarm also retires its date: once that day has
                // fired it has no future occurrence. For a snooze re-fire this
                // simply puts the repeating alarm back on its normal schedule.
                when {
                    alarm.scheduledDateEpochDay != null ->
                        app.container.alarmRepository.completeScheduled(alarmId)
                    alarm.isOneShot ->
                        app.container.alarmRepository.setEnabled(alarmId, false)
                    else -> app.container.alarmRepository.rearm(alarmId)
                }

                AlarmRingingService.ring(context, alarm)
            } catch (error: Exception) {
                Log.e(TAG, "Alarm $alarmId could not be delivered", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Only this app's own `PendingIntent` sends it, so it is not exported. */
        const val ACTION_FIRE: String = "com.pj95.clockwise.action.FIRE_ALARM"

        const val EXTRA_ALARM_ID: String = "com.pj95.clockwise.extra.ALARM_ID"

        private const val NO_ALARM = -1L
        private const val TAG = "AlarmReceiver"
    }
}
