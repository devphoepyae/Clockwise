package com.pj95.clockwise.domain.solar

import com.pj95.clockwise.model.solar.SolarEphemeris
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

/**
 * Pins the NOAA solar coordinates to known values, so the sunrise/sunset
 * solver cannot drift silently with a refactor.
 */
class SolarEphemerisTest {

    private fun assertClose(expected: Double, actual: Double, tolerance: Double, message: String) {
        assertTrue(
            "$message (expected $expected ± $tolerance, was $actual)",
            abs(expected - actual) <= tolerance,
        )
    }

    @Test
    fun declination_atJuneSolsticeIsNorthOfTheTropicOfCancer() {
        val ephemeris = SolarEphemeris.at(Instant.parse("2024-06-20T20:51:00Z"))
        assertClose(23.44, ephemeris.declinationDegrees, 0.2, "June solstice declination")
    }

    @Test
    fun declination_atDecemberSolsticeIsSouthOfTheTropicOfCapricorn() {
        val ephemeris = SolarEphemeris.at(Instant.parse("2024-12-21T09:21:00Z"))
        assertClose(-23.44, ephemeris.declinationDegrees, 0.2, "December solstice declination")
    }

    @Test
    fun declination_atMarchEquinoxIsNearTheEquator() {
        val ephemeris = SolarEphemeris.at(Instant.parse("2024-03-20T03:06:00Z"))
        assertClose(0.0, ephemeris.declinationDegrees, 0.3, "March equinox declination")
    }

    @Test
    fun equationOfTime_atItsFebruaryMinimumIsAboutMinusFourteenMinutes() {
        val ephemeris = SolarEphemeris.at(Instant.parse("2024-02-11T12:00:00Z"))
        assertClose(-14.24, ephemeris.equationOfTimeMinutes, 0.5, "February equation of time")
    }

    @Test
    fun equationOfTime_atItsNovemberMaximumIsAboutPlusSixteenMinutes() {
        val ephemeris = SolarEphemeris.at(Instant.parse("2024-11-03T12:00:00Z"))
        assertClose(16.47, ephemeris.equationOfTimeMinutes, 0.5, "November equation of time")
    }

    @Test
    fun equationOfTime_isNearZeroAtTheAprilAndSeptemberCrossings() {
        val april = SolarEphemeris.at(Instant.parse("2024-04-15T12:00:00Z"))
        val september = SolarEphemeris.at(Instant.parse("2024-09-01T12:00:00Z"))
        assertClose(0.0, april.equationOfTimeMinutes, 0.7, "April crossing")
        assertClose(0.0, september.equationOfTimeMinutes, 0.7, "September crossing")
    }
}
