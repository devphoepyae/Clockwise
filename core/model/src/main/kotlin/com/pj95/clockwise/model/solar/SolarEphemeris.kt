package com.pj95.clockwise.model.solar

import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/**
 * The two solar coordinates a sunrise/sunset solver needs at one instant.
 *
 * This is the NOAA solar-position approximation, accurate to well under a
 * minute for the app's purposes (the app never claims observatory accuracy).
 * It is computed on device from `java.time`, with no network or almanac file.
 *
 * @param declinationDegrees the Sun's declination in degrees; positive in
 *   northern summer, negative in northern winter
 * @param equationOfTimeMinutes the difference between apparent solar time and
 *   mean solar time, in minutes; positive means the sundial is ahead
 */
data class SolarEphemeris(
    val declinationDegrees: Double,
    val equationOfTimeMinutes: Double,
) {

    companion object {

        private const val MILLIS_PER_DAY = 86_400_000.0
        private const val JULIAN_UNIX_EPOCH = 2440587.5
        private const val J2000 = 2451545.0
        private const val DAYS_PER_JULIAN_CENTURY = 36525.0

        /** Solar coordinates at [instant]. */
        fun at(instant: Instant): SolarEphemeris {
            val julianDay = instant.toEpochMilli() / MILLIS_PER_DAY + JULIAN_UNIX_EPOCH
            val t = (julianDay - J2000) / DAYS_PER_JULIAN_CENTURY

            // Geometric mean longitude and anomaly of the Sun, in degrees.
            val meanLongitude = (280.46646 + t * (36000.76983 + 0.0003032 * t)).mod(360.0)
            val meanAnomaly = (357.52911 + t * (35999.05029 - 0.0001537 * t)).mod(360.0)
            val eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)

            val equationOfCenter =
                sin(meanAnomaly.rad()) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
                    sin(2.0 * meanAnomaly.rad()) * (0.019993 - 0.000101 * t) +
                    sin(3.0 * meanAnomaly.rad()) * 0.000289
            val trueLongitude = meanLongitude + equationOfCenter

            // Nutation and aberration correction to the apparent longitude.
            val omega = 125.04 - 1934.136 * t
            val apparentLongitude = trueLongitude - 0.00569 - 0.00478 * sin(omega.rad())

            // Mean obliquity of the ecliptic, then the corrected obliquity.
            val meanObliquity = 23.0 +
                (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
            val obliquity = meanObliquity + 0.00256 * cos(omega.rad())

            val declination = asin(
                sin(obliquity.rad()) * sin(apparentLongitude.rad()),
            ).deg()

            // Equation of time in minutes. `y` is tan(obliquity / 2) squared.
            val y = tan(obliquity.rad() / 2.0).pow(2)
            val equationOfTime = 4.0 * (
                y * sin(2.0 * meanLongitude.rad()) -
                    2.0 * eccentricity * sin(meanAnomaly.rad()) +
                    4.0 * eccentricity * y * sin(meanAnomaly.rad()) * cos(2.0 * meanLongitude.rad()) -
                    0.5 * y * y * sin(4.0 * meanLongitude.rad()) -
                    1.25 * eccentricity * eccentricity * sin(2.0 * meanAnomaly.rad())
                ).deg()

            return SolarEphemeris(
                declinationDegrees = declination,
                equationOfTimeMinutes = equationOfTime,
            )
        }

        private fun Double.rad(): Double = Math.toRadians(this)

        private fun Double.deg(): Double = Math.toDegrees(this)
    }
}
