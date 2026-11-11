package com.pj95.clockwise.domain.timer

import com.pj95.clockwise.model.timer.CountdownTimer
import com.pj95.clockwise.model.timer.TimerFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The countdown rule is arithmetic, not a running coroutine: these tests pin
 * the two properties everything else relies on — a running timer is derived
 * from its end instant, and a paused one keeps its frozen remainder.
 */
class CountdownTimerTest {

    @Test
    fun aRunningTimerDerivesTheRemainderFromItsEndInstant() {
        val timer = CountdownTimer(
            durationMillis = 300_000,
            remainingMillis = 300_000,
            endAtEpochMillis = 1_000_000,
            running = true,
        )

        assertEquals(300_000, timer.remainingAt(700_000))
        assertEquals(60_000, timer.remainingAt(940_000))
    }

    @Test
    fun aPastEndInstantNeverReportsNegativeTime() {
        val timer = CountdownTimer(
            durationMillis = 300_000,
            remainingMillis = 300_000,
            endAtEpochMillis = 1_000_000,
            running = true,
        )

        assertEquals(0, timer.remainingAt(1_500_000))
    }

    @Test
    fun aPausedTimerKeepsItsFrozenRemainder() {
        val timer = CountdownTimer(
            durationMillis = 300_000,
            remainingMillis = 120_000,
            endAtEpochMillis = null,
            running = false,
        )

        assertEquals(120_000, timer.remainingAt(9_999_999))
        assertFalse(timer.isFinished)
    }

    @Test
    fun aStoppedTimerWithNoTimeLeftIsFinished() {
        assertTrue(
            CountdownTimer(
                durationMillis = 60_000,
                remainingMillis = 0,
                endAtEpochMillis = null,
                running = false,
            ).isFinished,
        )
    }

    @Test
    fun theClockShowsHoursOnlyWhenThereAreHoursLeft() {
        assertEquals("00:00", TimerFormat.clock(0))
        assertEquals("00:59", TimerFormat.clock(58_100))
        assertEquals("01:00", TimerFormat.clock(59_100))
        assertEquals("04:59", TimerFormat.clock(298_100))
        assertEquals("1:00:00", TimerFormat.clock(3_600_000))
        assertEquals("1:01:05", TimerFormat.clock(3_665_000))
    }

    @Test
    fun aPartSecondStillReadsAsOneSecond() {
        // A timer that says 00:00 while still counting would look broken.
        assertEquals("00:01", TimerFormat.clock(1))
        assertEquals("00:01", TimerFormat.clock(999))
    }

    @Test
    fun theSummaryReadsInTheUnitsTheUserChose() {
        assertEquals("1 min 30 sec", TimerFormat.summary(90_000))
        assertEquals("1 hr 1 min", TimerFormat.summary(3_660_000))
        assertEquals("45 sec", TimerFormat.summary(45_000))
    }

    @Test
    fun theDurationLimitsCoverOneSecondToOneDay() {
        assertEquals(1_000, CountdownTimer.MIN_DURATION_MILLIS)
        assertTrue(CountdownTimer.MAX_DURATION_MILLIS > 24L * 3_600_000)
    }
}
