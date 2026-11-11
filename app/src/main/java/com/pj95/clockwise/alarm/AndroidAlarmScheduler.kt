package com.pj95.clockwise.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pj95.clockwise.MainActivity
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.domain.alarm.AlarmScheduling
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Arms alarms with [AlarmManager].
 *
 * Two decisions are worth stating:
 *
 *  - **`setAlarmClock`, not `setExact`.** `setAlarmClock` is the only API that
 *    is exact, exempt from Doze, allowed while the app is in the background, and
 *    visible to the platform as a user-facing alarm — it is what makes the
 *    status-bar alarm icon appear and what lets the system treat the app as an
 *    alarm clock rather than as a background job. Anything weaker is a
 *    best-effort reminder, not an alarm.
 *  - **One trigger per alarm id.** The `PendingIntent` request code is the
 *    alarm id, so re-arming an alarm replaces its own trigger and can never
 *    accumulate duplicates. Manual re-arms are therefore always safe.
 *
 * When the platform refuses exact alarms (Android 12/12L without the grant), the
 * alarm is still armed with `setAndAllowWhileIdle`: a few minutes late beats
 * silent, and the screen shows the prompt that makes it exact again.
 */
class AndroidAlarmScheduler(
    private val context: Context,
    private val clock: Clock,
) : AlarmScheduling {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(alarm: Alarm) {
        val next = if (alarm.enabled) {
            alarm.nextTrigger(clock.instant().atZone(ZoneId.systemDefault()))
        } else {
            null
        }
        if (next == null) {
            cancel(alarm.id)
        } else {
            arm(alarm.id, next)
        }
    }

    override fun cancel(alarmId: Long) {
        alarmManager.cancel(fireIntent(alarmId))
    }

    override fun scheduleSnooze(alarmId: Long, triggerAt: Instant) {
        // Same request code as the natural trigger, so the snooze replaces the
        // next occurrence rather than leaving two fires armed for one alarm.
        arm(alarmId, triggerAt)
    }

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * The intent that opens the app when the user taps the alarm icon in the
     * status bar. `setAlarmClock` requires it; without a show intent the
     * platform has nothing to open and the alarm is treated as a plain wake-up.
     */
    private fun showIntent(alarmId: Long): PendingIntent = PendingIntent.getActivity(
        context,
        alarmId.toInt(),
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MainActivity.EXTRA_ALARM_ID, alarmId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * Safe to call repeatedly. The extras are fixed, so the identity is the
     * alarm id alone and a renamed alarm cannot leave an orphaned trigger
     * behind: the label is read from the row when the alarm fires, never carried
     * in the PendingIntent.
     */
    private fun fireIntent(alarmId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        alarmId.toInt(),
        Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_FIRE)
            .putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun arm(alarmId: Long, at: Instant) {
        val fire = fireIntent(alarmId)
        val show = showIntent(alarmId)
        if (canScheduleExact()) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at.toEpochMilli(), show), fire)
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                at.toEpochMilli(),
                fire,
            )
        }
    }
}
