package com.pj95.clockwise.domain.map

import com.pj95.clockwise.model.map.GeoPoint
import java.time.Instant
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** The point on Earth where the Sun is directly overhead. */
data class SubsolarPoint(
    /** Solar declination in degrees; positive in northern summer. */
    val latitude: Double,
    /** Longitude in degrees within `(-180, 180]`. */
    val longitude: Double,
)

/**
 * Low-precision solar position computed on device from the system clock.
 *
 * This is the standard first-order NOAA approximation (good to roughly 0.01°),
 * far more accurate than a schematic map needs, and it keeps the app free of any
 * network or map-provider dependency.
 */
object SolarPosition {

    private const val MILLIS_PER_DAY = 86_400_000.0
    private const val JULIAN_UNIX_EPOCH = 2440587.5
    private const val J2000 = 2451545.0

    /**
     * The terminator is degenerate exactly at the equinoxes, when the Sun sits
     * over the equator. Holding the declination just off zero keeps the curve
     * continuous and unambiguous all year round.
     */
    private const val MIN_DECLINATION_DEGREES = 0.5

    private val minDeclination: Double = Math.toRadians(MIN_DECLINATION_DEGREES)

    private fun Double.rad(): Double = Math.toRadians(this)

    private fun Double.deg(): Double = Math.toDegrees(this)

    /** Position of the Sun's zenith at [instant]. */
    fun subsolarPoint(instant: Instant): SubsolarPoint {
        val julianDate = instant.toEpochMilli() / MILLIS_PER_DAY + JULIAN_UNIX_EPOCH
        val days = julianDate - J2000

        val meanLongitude = 280.460 + 0.9856474 * days
        val meanAnomaly = (357.528 + 0.9856003 * days).rad()
        val eclipticLongitude = (
            meanLongitude + 1.915 * sin(meanAnomaly) + 0.020 * sin(2.0 * meanAnomaly)
            ).rad()
        val obliquity = (23.439 - 0.0000004 * days).rad()

        val declination = asin(sin(obliquity) * sin(eclipticLongitude))
        val rightAscension = atan2(
            cos(obliquity) * sin(eclipticLongitude),
            cos(eclipticLongitude),
        )
        // Greenwich mean sidereal time. The rate is in hours per day, so it is
        // scaled to degrees (15° per hour) before being subtracted.
        val siderealTimeDegrees = (18.697374558 + 24.06570982441908 * days) * 15.0

        return SubsolarPoint(
            latitude = declination.deg(),
            longitude = normalizeLongitude(rightAscension.deg() - siderealTimeDegrees),
        )
    }

    /**
     * Latitude at which the Sun crosses the horizon along [longitude].
     *
     * Sun elevation is `sin(h) = sin(φ)sin(δ) + cos(φ)cos(δ)cos(H)`, so the
     * horizon `sin(h) = 0` gives `tan(φ) = -cos(Δλ)·cos(δ) / sin(δ)`.
     *
     * The inverse is a plain `atan`, not `atan2`: the terminator latitude only
     * ever lies within ±90°, and `atan2` would pick the antipodal branch.
     */
    fun terminatorLatitude(longitude: Double, subsolar: SubsolarPoint): Double {
        val declination = subsolar.latitude.rad()
        val hourAngle = normalizeLongitude(longitude - subsolar.longitude).rad()
        val sinDeclination = sin(declination)
        val stableSinDeclination = when {
            abs(sinDeclination) >= sin(minDeclination) -> sinDeclination
            declination < 0.0 -> -sin(minDeclination)
            else -> sin(minDeclination)
        }
        val tangent = -cos(hourAngle) * cos(declination) / stableSinDeclination
        return atan(tangent).deg()
    }

    /** True when the Sun is above the horizon at [point]. */
    fun isDaylight(point: GeoPoint, subsolar: SubsolarPoint): Boolean {
        val latitude = point.latitude.rad()
        val declination = subsolar.latitude.rad()
        val hourAngle = (point.longitude - subsolar.longitude).rad()
        return sin(latitude) * sin(declination) +
            cos(latitude) * cos(declination) * cos(hourAngle) > 0.0
    }

    /** Wraps degrees into `(-180, 180]`. */
    fun normalizeLongitude(degrees: Double): Double {
        val wrapped = (degrees + 180.0).mod(360.0)
        return if (wrapped <= 0.0) wrapped + 180.0 else wrapped - 180.0
    }
}
