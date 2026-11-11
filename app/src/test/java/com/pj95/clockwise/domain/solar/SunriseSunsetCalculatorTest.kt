package com.pj95.clockwise.domain.solar

import com.pj95.clockwise.model.solar.SolarDayKind
import com.pj95.clockwise.model.map.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Checks sunrise and sunset against published values for real cities.
 *
 * Each fixture is an independent reference (timeanddate.com / NOAA), converted
 * to UTC by hand. A tolerance of three minutes absorbs the difference between
 * the low-precision NOAA equations and the published almanac values while still
 * catching a wrong longitude sign, a wrong date anchor or a DST mistake.
 */
class SunriseSunsetCalculatorTest {

    private val newYork = GeoPoint(longitude = -74.0060, latitude = 40.7128)
    private val london = GeoPoint(longitude = -0.1278, latitude = 51.5074)
    private val tokyo = GeoPoint(longitude = 139.6503, latitude = 35.6762)
    private val sydney = GeoPoint(longitude = 151.2093, latitude = -33.8688)
    private val singapore = GeoPoint(longitude = 103.8198, latitude = 1.3521)
    private val tromso = GeoPoint(longitude = 18.9553, latitude = 69.6492)

    private fun assertInstantClose(expected: String, actual: Instant?, toleranceMinutes: Long, message: String) {
        val actualInstant = requireNotNull(actual) { "$message: expected an instant" }
        val expectedInstant = Instant.parse(expected)
        val deltaMinutes = abs(Duration.between(expectedInstant, actualInstant).toMinutes())
        assertTrue(
            "$message (expected $expected ± $toleranceMinutes min, was $actualInstant)",
            deltaMinutes <= toleranceMinutes,
        )
    }

    @Test
    fun newYork_summerSolsticeMatchesTheAlmanac() {
        val events = SunriseSunsetCalculator.events(
            date = LocalDate.of(2024, 6, 20),
            point = newYork,
            zone = ZoneId.of("America/New_York"),
        )

        assertEquals(SolarDayKind.NORMAL, events.kind)
        assertInstantClose("2024-06-20T09:24:00Z", events.sunrise, 3, "New York sunrise")
        assertInstantClose("2024-06-21T00:31:00Z", events.sunset, 3, "New York sunset")
        assertInstantClose("2024-06-20T16:57:00Z", events.solarNoon, 3, "New York solar noon")
        assertTrue("New York June day lasts about 15 hours", events.dayLength in Duration.ofHours(15)..Duration.ofHours(15) + Duration.ofMinutes(10))
    }

    @Test
    fun london_summerSolsticeMatchesTheAlmanac() {
        val events = SunriseSunsetCalculator.events(
            date = LocalDate.of(2024, 6, 20),
            point = london,
            zone = ZoneId.of("Europe/London"),
        )

        assertInstantClose("2024-06-20T03:43:00Z", events.sunrise, 3, "London sunrise")
        assertInstantClose("2024-06-20T20:21:00Z", events.sunset, 3, "London sunset")
        assertInstantClose("2024-06-20T12:02:00Z", events.solarNoon, 3, "London solar noon")
    }

    @Test
    fun tokyo_summerSolsticeKeepsTheEventsOnTheRequestedLocalDate() {
        val date = LocalDate.of(2024, 6, 20)
        val zone = ZoneId.of("Asia/Tokyo")
        val events = SunriseSunsetCalculator.events(date, tokyo, zone)

        assertInstantClose("2024-06-19T19:25:00Z", events.sunrise, 3, "Tokyo sunrise")
        assertInstantClose("2024-06-20T10:00:00Z", events.sunset, 3, "Tokyo sunset")
        assertEquals(date, events.sunriseLocal()?.toLocalDate())
        assertEquals(date, events.sunsetLocal()?.toLocalDate())
    }

    @Test
    fun sydney_winterSolsticeCrossesTheUtcDateLine() {
        val date = LocalDate.of(2024, 6, 20)
        val zone = ZoneId.of("Australia/Sydney")
        val events = SunriseSunsetCalculator.events(date, sydney, zone)

        assertInstantClose("2024-06-19T21:00:00Z", events.sunrise, 3, "Sydney sunrise")
        assertInstantClose("2024-06-20T06:54:00Z", events.sunset, 3, "Sydney sunset")
        assertEquals(date, events.sunriseLocal()?.toLocalDate())
        assertEquals(date, events.sunsetLocal()?.toLocalDate())
    }

    @Test
    fun singapore_equinoxMatchesTheAlmanac() {
        val events = SunriseSunsetCalculator.events(
            date = LocalDate.of(2024, 3, 20),
            point = singapore,
            zone = ZoneId.of("Asia/Singapore"),
        )

        assertInstantClose("2024-03-19T23:10:00Z", events.sunrise, 3, "Singapore sunrise")
        assertInstantClose("2024-03-20T11:16:00Z", events.sunset, 3, "Singapore sunset")
    }

    @Test
    fun tromso_hasMidnightSunAtTheJuneSolstice() {
        val events = SunriseSunsetCalculator.events(
            date = LocalDate.of(2024, 6, 20),
            point = tromso,
            zone = ZoneId.of("Europe/Oslo"),
        )

        assertEquals(SolarDayKind.POLAR_DAY, events.kind)
        assertTrue(events.isPolar)
        assertNull(events.sunrise)
        assertNull(events.sunset)
        assertEquals(Duration.ofDays(1), events.dayLength)
    }

    @Test
    fun tromso_hasPolarNightAtTheDecemberSolstice() {
        val events = SunriseSunsetCalculator.events(
            date = LocalDate.of(2024, 12, 21),
            point = tromso,
            zone = ZoneId.of("Europe/Oslo"),
        )

        assertEquals(SolarDayKind.POLAR_NIGHT, events.kind)
        assertTrue(events.isPolar)
        assertNull(events.sunrise)
        assertNull(events.sunset)
        assertEquals(Duration.ZERO, events.dayLength)
    }

    @Test
    fun normalDaysAlwaysHaveSunriseBeforeSunsetAndNoonBetweenThem() {
        val fixtures = listOf(
            Triple(LocalDate.of(2024, 6, 20), newYork, "America/New_York"),
            Triple(LocalDate.of(2024, 6, 20), london, "Europe/London"),
            Triple(LocalDate.of(2024, 6, 20), tokyo, "Asia/Tokyo"),
            Triple(LocalDate.of(2024, 6, 20), sydney, "Australia/Sydney"),
            Triple(LocalDate.of(2024, 3, 20), singapore, "Asia/Singapore"),
        )

        fixtures.forEach { (date, point, zoneId) ->
            val events = SunriseSunsetCalculator.events(date, point, ZoneId.of(zoneId))
            val sunrise = events.sunrise!!
            val sunset = events.sunset!!
            assertEquals("$zoneId sunrise must precede sunset", SolarDayKind.NORMAL, events.kind)
            assertTrue("$zoneId sunrise must precede noon", sunrise < events.solarNoon)
            assertTrue("$zoneId noon must precede sunset", events.solarNoon < sunset)
            assertEquals("$zoneId day length", Duration.between(sunrise, sunset), events.dayLength)
        }
    }

    @Test
    fun leapDayIsCalculatedNormally() {
        val events = SunriseSunsetCalculator.events(
            date = LocalDate.of(2024, 2, 29),
            point = newYork,
            zone = ZoneId.of("America/New_York"),
        )

        assertEquals(SolarDayKind.NORMAL, events.kind)
        assertTrue("Leap day has a sunrise", events.sunrise != null)
        assertTrue("Leap day has a sunset", events.sunset != null)
    }

    @Test
    fun daylightSavingChangeDoesNotMoveTheLocalDate() {
        // 10 March 2024 is the US spring-forward day; sunrise is after the
        // change, so an implementation that hard-codes a summer offset fails.
        val date = LocalDate.of(2024, 3, 10)
        val zone = ZoneId.of("America/New_York")
        val events = SunriseSunsetCalculator.events(date, newYork, zone)

        assertEquals(SolarDayKind.NORMAL, events.kind)
        assertEquals(date, events.sunriseLocal()?.toLocalDate())
        assertEquals(date, events.sunsetLocal()?.toLocalDate())
        assertTrue("Sunrise is in the 07:00 hour after the clock change", events.sunriseTime()!!.hour == 7)
    }
}
