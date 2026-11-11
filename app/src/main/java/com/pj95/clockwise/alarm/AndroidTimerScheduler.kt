package com.pj95.clockwise.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pj95.clockwise.domain.timer.TimerScheduling
import java.time.Instant

/**
 * Arms the countdown timer with [AlarmManager].
 *
 * A timer uses `setExactAndAllowWhileIdle` rather than `setAlarmClock`: it has
 * to fire on time even in Doze, but it should not put an alarm-clock icon in the
 * status bar or be listed as tomorrow's wake-up. There is one trigger, with a
 * fixed request code, so every re-arm replaces the previous one.
 *
 * When the platform refuses exact alarms the timer falls back to
 * `setAndAllowWhileIdle`; a few seconds late still beats a timer that never
 * rings.
 */
class AndroidTimerScheduler(
    private val context: Context,
) : TimerScheduling {

    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(at: Instant) {
        val fire = fireIntent()
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                at.toEpochMilli(),
                fire,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                at.toEpochMilli(),
                fire,
            )
        }
    }

    override fun cancel() {
        alarmManager.cancel(fireIntent())
    }

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun fireIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, TimerReceiver::class.java)
            .setAction(TimerReceiver.ACTION_FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        /** Distinct from alarm request codes, which are row ids. */
        const val REQUEST_CODE = 90_001
    }
}
