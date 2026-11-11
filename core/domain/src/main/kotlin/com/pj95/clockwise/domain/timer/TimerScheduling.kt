package com.pj95.clockwise.domain.timer

import com.pj95.clockwise.model.timer.CountdownTimer
import com.pj95.clockwise.model.timer.TimerFormat
import java.time.Instant

/**
 * The platform's wake-up mechanism, seen through the app's own vocabulary.
 *
 * Exists for the same reason as `AlarmScheduling`: the repository that stores a
 * timer must also arm it, and a unit test has to observe that without a device.
 * `AndroidTimerScheduler` is the only production implementation.
 */
interface TimerScheduling {

    /**
     * Arms a wake-up at [at]. Safe to call repeatedly: there is one timer, so
     * each call replaces the previous trigger rather than adding another.
     */
    fun schedule(at: Instant)

    /** Removes any pending wake-up. */
    fun cancel()

    /** True when the platform will honour an exact trigger. */
    fun canScheduleExact(): Boolean
}
