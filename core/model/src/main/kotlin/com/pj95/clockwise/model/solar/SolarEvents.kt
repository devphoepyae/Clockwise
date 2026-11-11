package com.pj95.clockwise.model.solar

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** What the Sun does on one local date at one place. */
enum class SolarDayKind {
    /** The Sun rises and sets; [SolarEvents.sunrise] and [SolarEvents.sunset] are present. */
    NORMAL,

    /** The Sun never sets, e.g. a polar summer. Both event instants are `null`. */
    POLAR_DAY,

    /** The Sun never rises, e.g. a polar winter. Both event instants are `null`. */
    POLAR_NIGHT,
}

/**
 * Sunrise, sunset and solar noon for one place on one local date.
 *
 * Instants are absolute, so they stay correct across DST changes; [zone] is
 * retained only to render them as local wall-clock times. A polar day or night
 * has no sunrise or sunset, which is represented explicitly by [kind] rather
 * than by pretending an event happened at midnight.
 *
 * @param date the local date the events belong to
 * @param zone the IANA zone used to interpret [date]
 * @param solarNoon the instant the Sun crosses the local meridian
 * @param dayLength the time between sunrise and sunset; 24 hours on a polar
 *   day and zero on a polar night
 */
data class SolarEvents(
    val date: LocalDate,
    val zone: ZoneId,
    val kind: SolarDayKind,
    val sunrise: Instant?,
    val sunset: Instant?,
    val solarNoon: Instant,
    val dayLength: Duration,
) {

    /** True when the Sun does not cross the horizon on this date. */
    val isPolar: Boolean get() = kind != SolarDayKind.NORMAL

    /** Sunrise in [zone], or `null` on a polar day/night. */
    fun sunriseLocal(): ZonedDateTime? = sunrise?.atZone(zone)

    /** Sunset in [zone], or `null` on a polar day/night. */
    fun sunsetLocal(): ZonedDateTime? = sunset?.atZone(zone)

    /** Solar noon in [zone]. */
    fun solarNoonLocal(): ZonedDateTime = solarNoon.atZone(zone)

    /** Sunrise as a wall-clock time, or `null` on a polar day/night. */
    fun sunriseTime(): LocalTime? = sunriseLocal()?.toLocalTime()

    /** Sunset as a wall-clock time, or `null` on a polar day/night. */
    fun sunsetTime(): LocalTime? = sunsetLocal()?.toLocalTime()
}
