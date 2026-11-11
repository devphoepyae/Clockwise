package com.pj95.clockwise.domain.time

import com.pj95.clockwise.common.time.CityTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * The time engine is pure, so it is exercised directly with fixed instants.
 * Offsets are never hard-coded in the app — these tests pin the behaviour that
 * `java.time` is expected to produce.
 */
class CityTimeFormatterTest {

    /** 20 Jan 2024, 16:40 UTC — northern winter, southern summer. */
    private val winter: Instant = Instant.parse("2024-01-20T16:40:00Z")

    /** 20 Jul 2024, 16:40 UTC — northern summer, southern winter. */
    private val summer: Instant = Instant.parse("2024-07-20T16:40:00Z")

    private fun zone(id: String): ZoneId = ZoneId.of(id)

    // --- UTC offsets ---------------------------------------------------------

    @Test
    fun offsetsMatchReferenceValuesInWinter() {
        assertEquals("UTC+9", CityTimeFormatter.offsetLabel(zone("Asia/Tokyo"), winter))
        assertEquals("UTC+11", CityTimeFormatter.offsetLabel(zone("Australia/Sydney"), winter))
        assertEquals("UTC-8", CityTimeFormatter.offsetLabel(zone("America/Los_Angeles"), winter))
        assertEquals("UTC-5", CityTimeFormatter.offsetLabel(zone("America/New_York"), winter))
        assertEquals("UTC+0", CityTimeFormatter.offsetLabel(zone("Europe/London"), winter))
    }

    @Test
    fun offsetsFollowDaylightSavingRules() {
        // Northern hemisphere moves forward, Sydney moves back.
        assertEquals("UTC-7", CityTimeFormatter.offsetLabel(zone("America/Los_Angeles"), summer))
        assertEquals("UTC-4", CityTimeFormatter.offsetLabel(zone("America/New_York"), summer))
        assertEquals("UTC+1", CityTimeFormatter.offsetLabel(zone("Europe/London"), summer))
        assertEquals("UTC+10", CityTimeFormatter.offsetLabel(zone("Australia/Sydney"), summer))
    }

    @Test
    fun halfHourOffsetsIncludeMinutes() {
        assertEquals("UTC+5:30", CityTimeFormatter.offsetLabel(zone("Asia/Kolkata"), winter))
        assertEquals("UTC+6:30", CityTimeFormatter.offsetLabel(zone("Asia/Yangon"), winter))
    }

    // --- Time text -----------------------------------------------------------

    @Test
    fun twentyFourHourTimeUsesLeadingZero() {
        assertEquals("01:40", CityTimeFormatter.snapshot(zone("Asia/Tokyo"), winter, true).timeText)
        assertEquals("03:40", CityTimeFormatter.snapshot(zone("Australia/Sydney"), winter, true).timeText)
        assertEquals("08:40", CityTimeFormatter.snapshot(zone("America/Los_Angeles"), winter, true).timeText)
        assertEquals("11:40", CityTimeFormatter.snapshot(zone("America/New_York"), winter, true).timeText)
        assertEquals("16:40", CityTimeFormatter.snapshot(zone("Europe/London"), winter, true).timeText)
    }

    @Test
    fun twelveHourTimeCarriesPeriod() {
        assertEquals("1:40 AM", CityTimeFormatter.snapshot(zone("Asia/Tokyo"), winter, false).timeText)
        assertEquals("8:40 AM", CityTimeFormatter.snapshot(zone("America/Los_Angeles"), winter, false).timeText)
        assertEquals("4:40 PM", CityTimeFormatter.snapshot(zone("Europe/London"), winter, false).timeText)
    }

    @Test
    fun midnightAndNoonRenderCorrectlyInTwelveHourMode() {
        // Tokyo is UTC+9: local 00:30 and 12:30 on 21 Jan 2024.
        val midnight = Instant.parse("2024-01-20T15:30:00Z")
        val noon = Instant.parse("2024-01-21T03:30:00Z")
        val tokyo = zone("Asia/Tokyo")

        val midnightTime = CityTimeFormatter.snapshot(tokyo, midnight, false)
        assertEquals("12", midnightTime.hourText)
        assertEquals("30", midnightTime.minuteText)
        assertEquals("AM", midnightTime.periodText)
        assertEquals("12:30 AM", midnightTime.timeText)

        val noonTime = CityTimeFormatter.snapshot(tokyo, noon, false)
        assertEquals("12:30 PM", noonTime.timeText)
        assertEquals("12", noonTime.hourText)
        assertEquals("30", noonTime.minuteText)
        assertEquals("PM", noonTime.periodText)
    }

    @Test
    fun twentyFourHourModeHasNoPeriod() {
        val time = CityTimeFormatter.snapshot(zone("Asia/Tokyo"), winter, true)
        assertEquals("01", time.hourText)
        assertEquals("40", time.minuteText)
        assertEquals(null, time.periodText)
    }

    // --- Weekday and date ----------------------------------------------------

    @Test
    fun weekdayAndDateComeFromTheCityLocalDate() {
        val tokyo = CityTimeFormatter.snapshot(zone("Asia/Tokyo"), winter, true)
        // 16:40 UTC + 9h rolls over to the next day.
        assertEquals("Sun", tokyo.weekdayShort)
        assertEquals("21 Jan", tokyo.dateText)

        val losAngeles = CityTimeFormatter.snapshot(zone("America/Los_Angeles"), winter, true)
        assertEquals("Sat", losAngeles.weekdayShort)
        assertEquals("20 Jan", losAngeles.dateText)
    }

    // --- Day / night ---------------------------------------------------------

    @Test
    fun dayNightSwitchesAtSixAndEighteen() {
        val yangon = zone("Asia/Yangon") // UTC+6:30

        assertFalse("05:59 must be night", CityTimeFormatter.isDaytime(yangon, Instant.parse("2024-01-19T23:29:00Z")))
        assertTrue("06:00 must be day", CityTimeFormatter.isDaytime(yangon, Instant.parse("2024-01-19T23:30:00Z")))
        assertTrue("17:59 must be day", CityTimeFormatter.isDaytime(yangon, Instant.parse("2024-01-20T11:29:00Z")))
        assertFalse("18:00 must be night", CityTimeFormatter.isDaytime(yangon, Instant.parse("2024-01-20T11:30:00Z")))
    }
}
