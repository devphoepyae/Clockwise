package com.pj95.clockwise.domain.time

import com.pj95.clockwise.model.time.TimeDifference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * Covers the signed minute arithmetic behind the time difference indicator.
 *
 * Reference instants are the ones `PreviewSamples` uses: 2024-01-20 (northern
 * winter, southern summer) and 2024-07-20 (the other way round).
 */
class TimeDifferenceTest {

    private val winter = Instant.parse("2024-01-20T16:40:00Z")
    private val summer = Instant.parse("2024-07-20T04:40:00Z")

    private fun diff(cityZone: String, referenceZone: String, at: Instant = winter) =
        TimeDifference.between(ZoneId.of(cityZone), ZoneId.of(referenceZone), at)

    @Test
    fun aZoneAheadIsPositive() {
        val difference = diff("Asia/Tokyo", "Europe/London")

        assertEquals(540, difference.minutes)
        assertTrue(difference.isAhead)
        assertFalse(difference.isSameTime)
        assertEquals(9, difference.hours)
        assertEquals(0, difference.minutesPart)
    }

    @Test
    fun aZoneBehindIsNegative() {
        val difference = diff("America/New_York", "Asia/Yangon")

        assertEquals(-690, difference.minutes)
        assertFalse(difference.isAhead)
        assertEquals(11, difference.hours)
        assertEquals(30, difference.minutesPart)
    }

    @Test
    fun halfAndQuarterHourZonesKeepTheirMinutes() {
        // Bangkok (UTC+7) against Yangon (UTC+6:30).
        assertEquals(30, diff("Asia/Bangkok", "Asia/Yangon").minutes)
        // Kolkata (UTC+5:30) against Yangon.
        assertEquals(-60, diff("Asia/Kolkata", "Asia/Yangon").minutes)
        // Kathmandu (UTC+5:45) against Kolkata (UTC+5:30): fifteen minutes, not
        // zero and not an hour.
        assertEquals(15, diff("Asia/Kathmandu", "Asia/Kolkata").minutes)
        // Eucla (UTC+8:45) against Yangon.
        assertEquals(135, diff("Australia/Eucla", "Asia/Yangon").minutes)
    }

    @Test
    fun theSameZoneIsAlwaysTheSameTime() {
        val difference = diff("Asia/Yangon", "Asia/Yangon")

        assertEquals(TimeDifference.None, difference)
        assertEquals(0, difference.minutes)
        assertTrue(difference.isSameTime)
        assertEquals(0, difference.hours)
        assertEquals(0, difference.minutesPart)
    }

    @Test
    fun twoDifferentZonesCanReadTheSameTime() {
        // Lagos has no daylight saving; Paris does.
        assertEquals(0, diff("Africa/Lagos", "Europe/Paris").minutes)
        assertTrue(diff("Africa/Lagos", "Europe/Paris").isSameTime)
        assertEquals(-60, diff("Africa/Lagos", "Europe/Paris", summer).minutes)
    }

    @Test
    fun daylightSavingMovesTheDifferenceRatherThanBeingHardCoded() {
        // London goes from UTC+0 to UTC+1, so it closes on Tokyo by an hour.
        assertEquals(540, diff("Asia/Tokyo", "Europe/London", winter).minutes)
        assertEquals(480, diff("Asia/Tokyo", "Europe/London", summer).minutes)

        // Sydney moves the other way and opens the gap to Yangon.
        assertEquals(270, diff("Australia/Sydney", "Asia/Yangon", winter).minutes)
        assertEquals(210, diff("Australia/Sydney", "Asia/Yangon", summer).minutes)

        // Los Angeles and New York swap relative to UTC but not to each other.
        assertEquals(-480, diff("America/Los_Angeles", "UTC", winter).minutes)
        assertEquals(-420, diff("America/Los_Angeles", "UTC", summer).minutes)
    }
}
