package com.pj95.clockwise.domain.timer

import com.pj95.clockwise.model.timer.CountdownTimer
/**
 * The notification side of a timer, as the repository needs it.
 *
 * Kept as an interface so the stored/armed/announced sequence can be tested
 * without a `NotificationManager`, in the same way [TimerScheduling] keeps
 * `AlarmManager` out of the repository test.
 */
interface TimerAlerting {

    /** Shows or refreshes the quiet countdown for a running timer. */
    fun showRunning(timer: CountdownTimer, remainingMillis: Long)

    /** Removes the countdown notification. */
    fun cancelRunning()

    /** Rings the "time is up" notification. */
    fun showFinished(timer: CountdownTimer)

    /** Removes a "time is up" notification left on the shade. */
    fun cancelFinished()
}
