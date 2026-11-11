package com.pj95.clockwise.domain.solar

import com.pj95.clockwise.model.solar.SolarDayKind
import com.pj95.clockwise.model.solar.SolarEphemeris
import com.pj95.clockwise.model.solar.SolarEvents
import com.pj95.clockwise.model.map.GeoPoint
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/**
 * Sunrise and sunset from the NOAA solar equations.
 *
 * The calculation is deliberately self-contained: it needs a date, a point and
 * an IANA zone, and nothing else. No network, no almanac, no location service.
 *
 * The Sun is treated as risen when its centre is 0.833° below the horizon,
 * which accounts for atmospheric refraction and the solar disc's radius. This
 * is the usual "sunrise" definition, not the astronomical twilight boundary.
 */
object SunriseSunsetCalculator {

    /** Centre of the Sun below the horizon at apparent sunrise/sunset. */
    private const val ZENITH_DEGREES = 90.833

    private const val MINUTES_PER_DAY = 1440.0
    private const val MINUTES_PER_DEGREE = 4.0
    private const val SECONDS_PER_MINUTE = 60.0

    /** Below this cosine the latitude is treated as a pole for the hour angle. */
    private const val POLE_EPSILON = 1e-12

    /**
     * Events for [date] at [point], interpreted in [zone].
     *
     * The date is a **civil** date in [zone], so "20 June" in Tokyo and
     * "20 June" in Los Angeles are different physical days. The returned
     * instants may fall on the previous or next UTC date, which is normal for
     * places far from Greenwich.
     */
    fun events(
        date: LocalDate,
        point: GeoPoint,
        zone: ZoneId,
    ): SolarEvents {
        val localNoon = date.atTime(LocalTime.NOON).atZone(zone)
        val ephemeris = SolarEphemeris.at(localNoon.toInstant())

        // Solar noon is `720 - 4 * longitude - equationOfTime` minutes after
        // UTC midnight. Anchoring it to local noon (rather than to a UTC date)
        // keeps the result on the requested civil date even across the date
        // line, where the local noon instant can be on a different UTC date.
        val solarNoonUtcMinutes =
            720.0 - MINUTES_PER_DEGREE * point.longitude - ephemeris.equationOfTimeMinutes
        val localNoonUtcMinutes = localNoon.toInstant()
            .atZone(ZoneOffset.UTC)
            .toLocalTime()
            .toSecondOfDay() / 60.0
        val solarNoon = localNoon.toInstant().plusSeconds(
            (
                normalizeMinuteDelta(solarNoonUtcMinutes - localNoonUtcMinutes) *
                    SECONDS_PER_MINUTE
                ).roundToLong(),
        )

        val cosHourAngle = cosineOfHourAngle(point.latitude, ephemeris.declinationDegrees)
        return when {
            cosHourAngle > 1.0 -> polarEvents(SolarDayKind.POLAR_NIGHT, date, zone, solarNoon)
            cosHourAngle < -1.0 -> polarEvents(SolarDayKind.POLAR_DAY, date, zone, solarNoon)
            else -> {
                val hourAngleDegrees = Math.toDegrees(acos(cosHourAngle.coerceIn(-1.0, 1.0)))
                val halfDaySeconds =
                    (MINUTES_PER_DEGREE * hourAngleDegrees * SECONDS_PER_MINUTE).roundToLong()
                val sunrise = solarNoon.minusSeconds(halfDaySeconds)
                val sunset = solarNoon.plusSeconds(halfDaySeconds)
                SolarEvents(
                    date = date,
                    zone = zone,
                    kind = SolarDayKind.NORMAL,
                    sunrise = sunrise,
                    sunset = sunset,
                    solarNoon = solarNoon,
                    dayLength = Duration.between(sunrise, sunset),
                )
            }
        }
    }

    /**
     * `cos(H) = (cos(zenith) - sin(lat)sin(decl)) / (cos(lat)cos(decl))`.
     *
     * Values above 1 mean the Sun never reaches the horizon (polar night);
     * values below -1 mean it never goes below it (polar day). At the poles
     * the denominator vanishes, so the Sun's constant elevation is used
     * instead: it is above the horizon whenever the declination has the same
     * sign as the latitude.
     */
    private fun cosineOfHourAngle(latitudeDegrees: Double, declinationDegrees: Double): Double {
        val latitude = Math.toRadians(latitudeDegrees)
        val declination = Math.toRadians(declinationDegrees)
        val cosLatitude = cos(latitude)
        if (abs(cosLatitude) < POLE_EPSILON) {
            return if (latitude * declination >= 0.0) {
                Double.NEGATIVE_INFINITY
            } else {
                Double.POSITIVE_INFINITY
            }
        }
        return (cos(Math.toRadians(ZENITH_DEGREES)) - sin(latitude) * sin(declination)) /
            (cosLatitude * cos(declination))
    }

    private fun polarEvents(
        kind: SolarDayKind,
        date: LocalDate,
        zone: ZoneId,
        solarNoon: Instant,
    ): SolarEvents = SolarEvents(
        date = date,
        zone = zone,
        kind = kind,
        sunrise = null,
        sunset = null,
        solarNoon = solarNoon,
        dayLength = when (kind) {
            SolarDayKind.POLAR_DAY -> Duration.ofDays(1)
            SolarDayKind.POLAR_NIGHT -> Duration.ZERO
            SolarDayKind.NORMAL -> error("Normal days are built by the caller")
        },
    )

    /**
     * Wraps a minutes-of-day difference into `[-720, 720)`, so a solar noon
     * just before UTC midnight is not mistaken for one almost a day later.
     */
    private fun normalizeMinuteDelta(minutes: Double): Double {
        val wrapped = minutes.mod(MINUTES_PER_DAY)
        return if (wrapped >= MINUTES_PER_DAY / 2.0) wrapped - MINUTES_PER_DAY else wrapped
    }
}
