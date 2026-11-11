package com.pj95.clockwise.model.timer

/**
 * The one countdown timer the app keeps.
 *
 * A timer is a stored fact rather than a running coroutine: [endAtEpochMillis]
 * is the moment it should ring, so the remaining time is arithmetic that stays
 * correct when the app is killed, the device sleeps or the clock is adjusted.
 * Only a running timer has an end instant; a paused one keeps [remainingMillis]
 * frozen, which is what makes pause/resume lossless.
 *
 * Pure Kotlin with no Android types, so the countdown rule is unit-testable on
 * the JVM.
 */
data class CountdownTimer(
    /** What the user originally chose, so "Start again" is one tap. */
    val durationMillis: Long,
    /** The time left when paused, or the full duration when freshly started. */
    val remainingMillis: Long,
    /** When the timer should ring, or `null` while paused. */
    val endAtEpochMillis: Long?,
    val running: Boolean,
    /** Free text; empty means the screen shows a neutral "Timer". */
    val label: String = "",
) {

    /**
     * The time left at [nowEpochMillis].
     *
     * A running timer is derived from its end instant — never decremented — so
     * a missed tick cannot make it drift. A paused timer reports its frozen
     * remainder.
     */
    fun remainingAt(nowEpochMillis: Long): Long =
        if (running && endAtEpochMillis != null) {
            (endAtEpochMillis - nowEpochMillis).coerceAtLeast(0)
        } else {
            remainingMillis.coerceAtLeast(0)
        }

    /** True once it has run out and is no longer counting. */
    val isFinished: Boolean get() = !running && remainingMillis <= 0

    /** True when it has not been started yet, or has been reset. */
    val isIdle: Boolean get() = remainingMillis <= 0 && !running

    companion object {
        /** The longest timer this app accepts: 99:59:59. */
        const val MAX_DURATION_MILLIS: Long = 99L * 3_600_000 + 59L * 60_000 + 59_000

        /** The shortest timer that can actually ring. */
        const val MIN_DURATION_MILLIS: Long = 1_000
    }
}

/**
 * Formats a countdown for the screen and the notification.
 *
 * Hours are only shown when there are hours left, so a five-minute timer reads
 * "05:00" rather than "0:05:00" and the digits stay large enough to read from
 * across a room.
 */
object TimerFormat {

    fun clock(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis.coerceAtLeast(0) + 999) / 1_000
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    /** "1 hr 30 min", for a finished timer's summary. */
    fun summary(durationMillis: Long): String {
        val totalSeconds = durationMillis.coerceAtLeast(0) / 1_000
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return buildList {
            if (hours > 0) add("$hours hr")
            if (minutes > 0) add("$minutes min")
            if (seconds > 0 && hours == 0L) add("$seconds sec")
        }.joinToString(" ").ifEmpty { "0 sec" }
    }
}
