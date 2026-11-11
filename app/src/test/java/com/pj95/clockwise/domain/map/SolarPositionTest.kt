package com.pj95.clockwise.domain.map

import com.pj95.clockwise.model.map.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

/**
 * The solar solver drives the map's day/night shading (spec section 4.4).
 *
 * Checks are pinned to real astronomical events rather than to whatever the
 * implementation happens to output, so a regression actually fails.
 */
class SolarPositionTest {

    private val juneSolstice = Instant.parse("2024-06-20T20:51:00Z")
    private val decemberSolstice = Instant.parse("2024-12-21T09:21:00Z")
    private val marchEquinox = Instant.parse("2024-03-20T03:06:00Z")

    private val northPole = GeoPoint(longitude = 0.0, latitude = 90.0)
    private val southPole = GeoPoint(longitude = 0.0, latitude = -90.0)

    private fun assertClose(expected: Double, actual: Double, tolerance: Double, message: String) {
        assertTrue(
            "$message (expected $expected ± $tolerance, was $actual)",
            abs(expected - actual) <= tolerance,
        )
    }

    @Test
    fun subsolarLatitude_atJuneSolsticeSitsOnTheTropicOfCancer() {
        val subsolar = SolarPosition.subsolarPoint(juneSolstice)
        assertClose(23.44, subsolar.latitude, 0.6, "June solstice declination")
    }

    @Test
    fun subsolarLatitude_atDecemberSolsticeSitsOnTheTropicOfCapricorn() {
        val subsolar = SolarPosition.subsolarPoint(decemberSolstice)
        assertClose(-23.44, subsolar.latitude, 0.6, "December solstice declination")
    }

    @Test
    fun subsolarLatitude_atMarchEquinoxSitsOnTheEquator() {
        val subsolar = SolarPosition.subsolarPoint(marchEquinox)
        assertClose(0.0, subsolar.latitude, 0.6, "March equinox declination")
    }

    @Test
    fun subsolarLongitude_atNoonUtcIsNearThePrimeMeridian() {
        val subsolar = SolarPosition.subsolarPoint(Instant.parse("2024-03-20T12:00:00Z"))
        // The equation of time can shift this by up to about 4 degrees.
        assertClose(0.0, subsolar.longitude, 5.0, "Noon UTC subsolar longitude")
    }

    @Test
    fun subsolarLongitude_tracksEarthRotationAtFifteenDegreesPerHour() {
        val noon = SolarPosition.subsolarPoint(Instant.parse("2024-01-20T00:00:00Z"))
        val fourHoursLater = SolarPosition.subsolarPoint(Instant.parse("2024-01-20T04:00:00Z"))
        val drift = SolarPosition.normalizeLongitude(fourHoursLater.longitude - noon.longitude)
        assertClose(-60.0, drift, 0.5, "Four hours of rotation")
    }

    @Test
    fun normalizeLongitude_wrapsDegreesIntoTheHalfOpenRange() {
        assertClose(0.0, SolarPosition.normalizeLongitude(0.0), 1e-9, "0")
        assertClose(179.0, SolarPosition.normalizeLongitude(179.0), 1e-9, "179")
        assertClose(-179.0, SolarPosition.normalizeLongitude(181.0), 1e-9, "181 wraps to -179")
        assertClose(-10.0, SolarPosition.normalizeLongitude(-370.0), 1e-9, "-370 wraps to -10")
        assertTrue(
            "The result must never exceed 180",
            SolarPosition.normalizeLongitude(540.0) <= 180.0,
        )
    }

    @Test
    fun isDaylight_atTheSubsolarPointIsAlwaysTrue() {
        listOf(juneSolstice, decemberSolstice, marchEquinox).forEach { instant ->
            val subsolar = SolarPosition.subsolarPoint(instant)
            val point = GeoPoint(subsolar.longitude, subsolar.latitude)
            assertTrue("Subsolar point must be in daylight at $instant", SolarPosition.isDaylight(point, subsolar))
        }
    }

    @Test
    fun isDaylight_givesThePolesOppositeSeasons() {
        val june = SolarPosition.subsolarPoint(juneSolstice)
        val december = SolarPosition.subsolarPoint(decemberSolstice)

        assertTrue("North pole has midnight sun in June", SolarPosition.isDaylight(northPole, june))
        assertFalse("North pole has polar night in December", SolarPosition.isDaylight(northPole, december))
        assertFalse("South pole has polar night in June", SolarPosition.isDaylight(southPole, june))
        assertTrue("South pole has midnight sun in December", SolarPosition.isDaylight(southPole, december))
    }

    @Test
    fun terminatorLatitude_separatesDayFromNight() {
        listOf(juneSolstice, decemberSolstice).forEach { instant ->
            val subsolar = SolarPosition.subsolarPoint(instant)
            listOf(-120.0, -60.0, 60.0, 120.0).forEach { delta ->
                val longitude = SolarPosition.normalizeLongitude(subsolar.longitude + delta)
                val latitude = SolarPosition.terminatorLatitude(longitude, subsolar)
                assertTrue(
                    "Terminator at Δλ=$delta must be a real latitude, was $latitude",
                    latitude in -90.0..90.0,
                )
                val justNorth = GeoPoint(longitude, latitude + 2.0)
                val justSouth = GeoPoint(longitude, latitude - 2.0)
                assertTrue(
                    "Δλ=$delta: exactly one side of the terminator can be lit",
                    SolarPosition.isDaylight(justNorth, subsolar) !=
                        SolarPosition.isDaylight(justSouth, subsolar),
                )
            }
        }
    }

    @Test
    fun terminatorLatitude_atEquinoxIsZeroNinetyDegreesFromTheSubsolarMeridian() {
        // Around the equinoxes the terminator runs along meridians, so the
        // solver is deliberately held just off the degenerate case.
        val subsolar = SolarPosition.subsolarPoint(marchEquinox)
        listOf(-90.0, 90.0).forEach { delta ->
            val longitude = SolarPosition.normalizeLongitude(subsolar.longitude + delta)
            val latitude = SolarPosition.terminatorLatitude(longitude, subsolar)
            assertClose(0.0, latitude, 1.0, "Equinox terminator at Δλ=$delta")
        }
    }

    @Test
    fun terminatorLatitude_isContinuousAcrossTheAntimeridian() {
        // The map traces the terminator from -180 to +180 and closes the path;
        // the two ends must meet or the night polygon would tear.
        listOf(juneSolstice, decemberSolstice, marchEquinox).forEach { instant ->
            val subsolar = SolarPosition.subsolarPoint(instant)
            val westEdge = SolarPosition.terminatorLatitude(-180.0, subsolar)
            val eastEdge = SolarPosition.terminatorLatitude(180.0, subsolar)
            assertClose(westEdge, eastEdge, 1e-6, "Terminator seam at $instant")
        }
    }

    @Test
    fun terminatorLatitude_staysWithinTheGlobeForEveryLongitude() {
        listOf(juneSolstice, decemberSolstice, marchEquinox).forEach { instant ->
            val subsolar = SolarPosition.subsolarPoint(instant)
            var longitude = -180.0
            while (longitude <= 180.0) {
                val latitude = SolarPosition.terminatorLatitude(longitude, subsolar)
                assertTrue(
                    "Terminator left the globe at $longitude on $instant: $latitude",
                    latitude >= -90.0 && latitude <= 90.0,
                )
                longitude += 5.0
            }
        }
    }
}
