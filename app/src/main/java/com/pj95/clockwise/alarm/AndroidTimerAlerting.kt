package com.pj95.clockwise.alarm

import android.content.Context
import com.pj95.clockwise.model.timer.CountdownTimer
import com.pj95.clockwise.domain.timer.TimerAlerting

/** Binds [TimerNotifications] to the application context. */
class AndroidTimerAlerting(
    private val context: Context,
) : TimerAlerting {

    override fun showRunning(timer: CountdownTimer, remainingMillis: Long) {
        TimerNotifications.showRunning(context, timer, remainingMillis)
    }

    override fun cancelRunning() {
        TimerNotifications.cancelRunning(context)
    }

    override fun showFinished(timer: CountdownTimer) {
        TimerNotifications.showFinished(context, timer)
    }

    override fun cancelFinished() {
        TimerNotifications.cancelFinished(context)
    }
}
