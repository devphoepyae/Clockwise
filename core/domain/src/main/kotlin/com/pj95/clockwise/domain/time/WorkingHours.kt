package com.pj95.clockwise.domain.time

import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.model.time.WorkingHoursOverlap
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Working-hours policy for the meeting planner.
 *
 * Kept in `:core:domain` rather than beside [MinuteOfDay] so the model module
 * stays a dependency-free set of data types.
 */

/** First hour of the working day the planner highlights (inclusive). */
const val WORKING_DAY_START_HOUR = 9

/** Last hour of the working day the planner highlights (exclusive). */
const val WORKING_DAY_END_HOUR = 18

/**
 * True between 09:00 and 17:59 — the window a remote team can plausibly meet
 * in. A judgement call, not a fact, so it is stated once here and shown to the
 * user as a legend rather than applied silently.
 */
fun isWithinWorkingDay(minuteOfDay: MinuteOfDay): Boolean =
    minuteOfDay.hours in WORKING_DAY_START_HOUR until WORKING_DAY_END_HOUR

/** True between 09:00 and 17:59 local time in [zoneId] at [instant]. */
fun isWithinWorkingDay(instant: Instant, zoneId: ZoneId): Boolean =
    isWithinWorkingDay(MinuteOfDay.ofInstant(instant, zoneId))

/**
 * The best slot of a source day for a meeting across [cityZoneIds].
 *
 * The scan is deliberately exhaustive: 96 quarter-hour slots is cheap, and the
 * answer has to hold for half-hour zones and for cities on the other side of
 * the date line, where no closed-form overlap exists.
 *
 * "Best" means the most cities inside 09:00-17:59. Ties are broken by the sum
 * of the squared distances from each working city's local noon — the slot
 * where the people who *can* attend are closest to the middle of their day.
 * A final tie goes to the earlier slot, so the result is deterministic.
 *
 * A slot that suits only one city is not an overlap, so a recommendation is
 * made only when at least two cities are at work. That is also what gives the
 * planner a meaningful "no overlap" state for a pair such as Tokyo and New
 * York, whose working days never meet.
 *
 * @param date the civil date in [sourceZoneId]
 * @param sourceZoneId the zone the slot is expressed in
 * @param cityZoneIds the zones of every city being planned for
 * @return the winning slot, or `null` when fewer than two cities can be at work
 */
fun bestWorkingHoursSlot(
    date: LocalDate,
    sourceZoneId: ZoneId,
    cityZoneIds: List<ZoneId>,
): WorkingHoursOverlap? {
    if (cityZoneIds.isEmpty()) return null

    var best: WorkingHoursOverlap? = null
    var bestComfort = Int.MAX_VALUE

    var value = MinuteOfDay.MIN_VALUE
    while (value <= MinuteOfDay.LAST_GRID_VALUE) {
        val minuteOfDay = MinuteOfDay(value)
        val instant = minuteOfDay.onDate(date, sourceZoneId)

        var working = 0
        var comfort = 0
        cityZoneIds.forEach { zoneId ->
            val local = MinuteOfDay.ofInstant(instant, zoneId)
            if (isWithinWorkingDay(local)) {
                working++
                val distanceFromNoon = abs(local.value - MinuteOfDay.Noon.value)
                comfort += distanceFromNoon * distanceFromNoon
            }
        }

        if (
            best == null ||
            working > best.citiesWithinWorkingHours ||
            (working == best.citiesWithinWorkingHours && comfort < bestComfort)
        ) {
            best = WorkingHoursOverlap(minuteOfDay, working)
            bestComfort = comfort
        }

        value += MinuteOfDay.STEP_MINUTES
    }

    return best?.takeIf { it.citiesWithinWorkingHours >= 2 }
}
