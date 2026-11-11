package com.pj95.clockwise.domain.time

import com.pj95.clockwise.model.time.MarketSessions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * The offline FOREX session rule: 09:00–18:00 local, Monday to Friday.
 *
 * The DST cases are the point of the test — the rule reads the city's own
 * [ZoneId], so the same UTC instant can be open in one half of the year and
 * closed in the other.
 */
class MarketSessionsTest {

    private val london = ZoneId.of("Europe/London")
    private val newYork = ZoneId.of("America/New_York")
    private val tokyo = ZoneId.of("Asia/Tokyo")

    @Test
    fun theFourCentresAreInTradingOrder() {
        assertEquals(
            listOf("sydney", "tokyo", "london", "new-york"),
            MarketSessions.cityIds,
        )
    }

    @Test
    fun aWeekdayMidSessionIsOpen() {
        // Monday 2024-01-15, 10:30 in London (GMT).
        assertTrue(MarketSessions.isOpen(london, Instant.parse("2024-01-15T10:30:00Z")))
    }

    @Test
    fun beforeTheOpenAndAtTheCloseAreClosed() {
        // 08:59 London, and exactly 18:00 London.
        assertFalse(MarketSessions.isOpen(london, Instant.parse("2024-01-15T08:59:00Z")))
        assertFalse(MarketSessions.isOpen(london, Instant.parse("2024-01-15T18:00:00Z")))
    }

    @Test
    fun theOpenBoundaryIsInclusive() {
        // 09:00 in New York (EST) on a Monday.
        assertTrue(MarketSessions.isOpen(newYork, Instant.parse("2024-01-15T14:00:00Z")))
    }

    @Test
    fun theWeekendIsClosedAllDay() {
        // Saturday 2024-01-13, midday in Tokyo.
        assertFalse(MarketSessions.isOpen(tokyo, Instant.parse("2024-01-13T03:00:00Z")))
    }

    @Test
    fun daylightSavingMovesTheSessionWithTheCity() {
        // 08:30 UTC is 08:30 GMT in January (closed) but 09:30 BST in July (open).
        assertFalse(MarketSessions.isOpen(london, Instant.parse("2024-01-15T08:30:00Z")))
        assertTrue(MarketSessions.isOpen(london, Instant.parse("2024-07-15T08:30:00Z")))
    }

    @Test
    fun aSessionCanDifferAcrossCentresAtTheSameInstant() {
        // Tuesday 2024-01-16, 00:00 UTC: New York has closed for the day
        // (Monday 19:00), while Tokyo has just opened (Tuesday 09:00).
        val instant = Instant.parse("2024-01-16T00:00:00Z")
        assertFalse(MarketSessions.isOpen(newYork, instant))
        assertTrue(MarketSessions.isOpen(tokyo, instant))
    }
}
