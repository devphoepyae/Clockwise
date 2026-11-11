package com.pj95.clockwise.widget

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The widget's minute ticker: what it arms, when it stops, and the arithmetic
 * that keeps a clock from ever landing in the minute it is already showing.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WidgetTickerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarms: AlarmManager = context.getSystemService(AlarmManager::class.java)

    @After
    fun tearDown() {
        cancelWidgetTick(context)
    }

    @Test
    fun aWidgetOnTheHomeScreenIsTickedOnTheNextMinute() {
        scheduleWidgetTick(context, widgetsExist = true, isInteractive = true)

        val scheduled = shadowOf(alarms).peekNextScheduledAlarm()
        assertNotNull(scheduled)
        assertEquals(AlarmManager.RTC, scheduled!!.type)
        assertEquals("the trigger must sit exactly on a minute", 0L, scheduled.triggerAtTime % 60_000L)

        val delay = scheduled.triggerAtTime - System.currentTimeMillis()
        assertTrue("expected a tick within the next minute, got $delay ms", delay in 1..60_000L)
    }

    @Test
    fun aScreenOffWidgetBacksOffToTheBackgroundCadence() {
        scheduleWidgetTick(context, widgetsExist = true, isInteractive = false)

        val scheduled = shadowOf(alarms).peekNextScheduledAlarm()
        assertNotNull(scheduled)
        assertEquals(AlarmManager.RTC, scheduled!!.type)
        assertEquals(
            "a screen-off tick must land on a background interval",
            0L,
            scheduled.triggerAtTime % (BACKGROUND_TICK_MINUTES * 60_000L),
        )

        val delay = scheduled.triggerAtTime - System.currentTimeMillis()
        assertTrue(
            "expected a tick within ${BACKGROUND_TICK_MINUTES * 60_000L} ms, got $delay ms",
            delay in 1..(BACKGROUND_TICK_MINUTES * 60_000L),
        )
    }

    @Test
    fun theLastWidgetLeavingStopsTheTicker() {
        scheduleWidgetTick(context, widgetsExist = true)
        assertNotNull(shadowOf(alarms).peekNextScheduledAlarm())

        scheduleWidgetTick(context, widgetsExist = false)

        // Nothing left to draw: the device is not woken every minute for an
        // empty home screen.
        assertNull(shadowOf(alarms).peekNextScheduledAlarm())
    }

    @Test
    fun noWidgetOnTheHomeScreenMeansNoScheduledTick() {
        assertFalse("the test device must start with no widgets", hasHomeScreenWidgets(context))

        scheduleWidgetTick(context)

        assertNull(shadowOf(alarms).peekNextScheduledAlarm())
    }

    @Test
    fun theMinuteBoundaryIsAlwaysAheadAndAlwaysOnTheMinute() {
        // Mid-minute rounds up.
        assertEquals(60_000L, nextMinuteBoundary(59_999L))
        // Exactly on a boundary still moves forward, so a tick that arrives in
        // the minute it was scheduled for does not re-arm for the same minute.
        assertEquals(120_000L, nextMinuteBoundary(60_000L))
        assertEquals(120_000L, nextMinuteBoundary(60_001L))
        // And from a clean epoch it lands on the very next minute.
        assertEquals(60_000L, nextMinuteBoundary(0L))
        assertEquals(1_704_067_200_000L, nextMinuteBoundary(1_704_067_199_999L))
    }

    @Test
    fun theBackgroundBoundaryIsAlwaysAWholeIntervalAhead() {
        val period = BACKGROUND_TICK_MINUTES * 60_000L
        // 00:59.999 rounds to the next minute and then up to 05:00.
        assertEquals(period, nextBackgroundBoundary(59_999L))
        // Exactly on a five-minute boundary still moves to the next one.
        assertEquals(period * 2, nextBackgroundBoundary(period))
        // One second before the boundary lands on it, not after it.
        assertEquals(period, nextBackgroundBoundary(period - 1_000L))
        // And the result is always a whole minute and a multiple of the period.
        listOf(0L, 1L, 61_000L, 299_999L, 300_001L, 1_704_067_199_999L).forEach { now ->
            val next = nextBackgroundBoundary(now)
            assertEquals(0L, next % 60_000L)
            assertEquals(0L, next % period)
            assertTrue("$now -> $next must be in the future", next > now)
        }
    }
}
