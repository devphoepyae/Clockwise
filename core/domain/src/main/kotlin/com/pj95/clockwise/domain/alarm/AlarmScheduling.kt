package com.pj95.clockwise.domain.alarm

import com.pj95.clockwise.model.alarm.Alarm
import java.time.Instant

/**
 * The platform's alarm clock, seen through the app's own vocabulary.
 *
 * Exists as an interface for one reason: the repository that saves an alarm must
 * also arm it, and a unit test must be able to observe that without a device.
 * [AndroidAlarmScheduler] is the only production implementation.
 */
interface AlarmScheduling {

    /**
     * Arms the next occurrence of [alarm], or disarms it when it is switched
     * off or has no future occurrence. Safe to call repeatedly: each alarm has
     * one pending trigger, replaced rather than duplicated.
     */
    fun schedule(alarm: Alarm)

    /** Removes any pending trigger for [alarmId]. */
    fun cancel(alarmId: Long)

    /**
     * Arms one snooze trigger for [alarmId] at [triggerAt].
     *
     * Deliberately separate from [schedule]: a snooze is a one-off instant, not
     * the alarm's next wall-clock occurrence, and it must replace the pending
     * natural trigger rather than stack beside it.
     */
    fun scheduleSnooze(alarmId: Long, triggerAt: Instant)

    /**
     * True when the platform will honour an exact trigger.
     *
     * Android 12 and 12L require the user to grant "Alarms & reminders"; from
     * Android 13 an alarm-clock app declaring `USE_EXACT_ALARM` gets it
     * automatically. When this is false the scheduler falls back to an inexact
     * wake-up rather than dropping the alarm.
     */
    fun canScheduleExact(): Boolean
}
