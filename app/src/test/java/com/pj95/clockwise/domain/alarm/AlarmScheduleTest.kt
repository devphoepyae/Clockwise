package com.pj95.clockwise.domain.alarm

import com.pj95.clockwise.model.alarm.RepeatDays
import com.pj95.clockwise.common.alarm.timeText
import com.pj95.clockwise.model.alarm.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The one calculation that decides whether a phone wakes someone up.
 *
 * Pinned to named zones rather than the machine's default, because the rule is
 * about the wall clock: 07:30 has to read 07:30 on the day it rings, including
 * on the days a timezone shifts.
 */
class AlarmScheduleTest {

    private val yangon: ZoneId = ZoneId.of("Asia/Yangon")

    private fun zoned(text: String, zone: ZoneId = yangon): ZonedDateTime =
        ZonedDateTime.of(java.time.LocalDateTime.parse(text), zone)

    @Test
    fun aOneShotAlarmLaterTodayRingsToday() {
        val alarm = Alarm(hour = 18, minute = 0)

        val next = alarm.nextTrigger(zoned("2024-01-20T07:00"))

        assertEquals(zoned("2024-01-20T18:00").toInstant(), next)
    }

    @Test
    fun aOneShotAlarmWhoseTimeHasPassedRingsTomorrow() {
        val alarm = Alarm(hour = 7, minute = 0)

        val next = alarm.nextTrigger(zoned("2024-01-20T07:00"))

        assertEquals(zoned("2024-01-21T07:00").toInstant(), next)
    }

    @Test
    fun aRepeatingAlarmSkipsTheDaysItDoesNotRepeatOn() {
        // Friday at 08:00; a weekday alarm's next ring is Monday at 07:00.
        val alarm = Alarm(hour = 7, minute = 0, repeatDays = RepeatDays.WEEKDAYS)

        val next = alarm.nextTrigger(zoned("2024-01-19T08:00"))

        assertEquals(zoned("2024-01-22T07:00").toInstant(), next)
        assertEquals(DayOfWeek.MONDAY, next!!.atZone(yangon).dayOfWeek)
    }

    @Test
    fun aRepeatingAlarmOnTheSameDayWaitsForNextWeekOnceItsTimeHasPassed() {
        // Monday 08:00, weekday alarm at 07:00: the next ring is next Monday.
        val alarm = Alarm(hour = 7, minute = 0, repeatDays = RepeatDays.MONDAY)

        val next = alarm.nextTrigger(zoned("2024-01-15T08:00"))

        assertEquals(zoned("2024-01-22T07:00").toInstant(), next)
    }

    @Test
    fun everyDayAlwaysFindsTomorrowAfterTodayHasPassed() {
        val alarm = Alarm(hour = 23, minute = 59, repeatDays = RepeatDays.EVERY_DAY)

        val next = alarm.nextTrigger(zoned("2024-01-20T23:59"))

        assertEquals(zoned("2024-01-21T23:59").toInstant(), next)
    }

    @Test
    fun anEmptyMaskStillFindsTheNextOccurrence() {
        val alarm = Alarm(hour = 3, minute = 0)

        assertTrue(alarm.isOneShot)
        assertEquals(
            zoned("2024-01-21T03:00").toInstant(),
            alarm.nextTrigger(zoned("2024-01-20T03:00")),
        )
    }

    /**
     * Spring forward: 02:30 does not exist in New York on 8 March 2026. The
     * platform moves it to 03:30 EDT, and the alarm must ring at that instant
     * rather than a day later or an hour off.
     */
    @Test
    fun anAlarmInTheSpringForwardGapResolvesToTheShiftedInstant() {
        val newYork = ZoneId.of("America/New_York")
        val alarm = Alarm(hour = 2, minute = 30)

        val next = alarm.nextTrigger(zoned("2026-03-08T00:00", newYork))

        assertEquals(Instant.parse("2026-03-08T07:30:00Z"), next)
        assertEquals(3, next!!.atZone(newYork).hour)
    }

    /**
     * Fall back: 01:30 occurs twice in New York on 1 November 2026. The earlier
     * offset is the one `atZone` chooses, which is the first 01:30 the user sees.
     */
    @Test
    fun anAmbiguousFallBackTimeUsesTheFirstOccurrence() {
        val newYork = ZoneId.of("America/New_York")
        val alarm = Alarm(hour = 1, minute = 30)

        val next = alarm.nextTrigger(zoned("2026-11-01T00:00", newYork))

        assertEquals(Instant.parse("2026-11-01T05:30:00Z"), next)
    }

    @Test
    fun theDayMaskIsCalendarOrderedAndTogglesIndependently() {
        assertEquals(
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
            RepeatDays.days(RepeatDays.WEEKDAYS),
        )
        assertEquals(
            listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            RepeatDays.days(RepeatDays.WEEKEND),
        )
        assertEquals(7, RepeatDays.days(RepeatDays.EVERY_DAY).size)

        val monday = RepeatDays.with(RepeatDays.NONE, DayOfWeek.MONDAY, true)
        assertTrue(RepeatDays.includes(monday, DayOfWeek.MONDAY))
        assertFalse(RepeatDays.includes(monday, DayOfWeek.TUESDAY))

        val cleared = RepeatDays.with(monday, DayOfWeek.MONDAY, false)
        assertEquals(RepeatDays.NONE, cleared)
    }

    @Test
    fun theTimeReadsInTheAppWideFormat() {
        val alarm = Alarm(hour = 7, minute = 5)

        assertEquals("07:05", alarm.timeText(use24Hour = true))
        assertEquals("7:05 AM", alarm.timeText(use24Hour = false))
        assertEquals("12:00 PM", Alarm(hour = 12, minute = 0).timeText(use24Hour = false))
        assertEquals("12:00 AM", Alarm(hour = 0, minute = 0).timeText(use24Hour = false))
    }

    @Test
    fun aOneShotAlarmIsRecognisedByItsEmptyMask() {
        assertTrue(Alarm(hour = 7, minute = 0).isOneShot)
        assertFalse(Alarm(hour = 7, minute = 0, repeatDays = RepeatDays.MONDAY).isOneShot)
    }
}
