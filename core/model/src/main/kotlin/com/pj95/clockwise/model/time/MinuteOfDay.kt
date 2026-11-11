package com.pj95.clockwise.model.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A time of day on a 24-hour clock, held as minutes since midnight.
 *
 * The meeting planner works in whole minutes rather than in hours because
 * dragging the slider has to land on the *same wall-clock time* in every city,
 * and the cities that use 30- and 45-minute offsets (UTC+5:30, UTC+5:45) would
 * otherwise land half a zone away.
 */
@JvmInline
value class MinuteOfDay(val value: Int) {

    init {
        require(value in MIN_VALUE..MAX_VALUE) { "Minute of day out of range: $value" }
    }

    /** Hour on a 24-hour clock, 0–23. */
    val hours: Int get() = value / 60

    /** Minutes past the hour, 0–59. */
    val minutes: Int get() = value % 60

    /**
     * The nearest value the planner's grid allows.
     *
     * The slider offers 15-minute slots — a meeting is never agreed for 09:07 —
     * and the last slot of the day is 23:45, so the grid never rolls over into
     * the next day.
     */
    fun snappedToGrid(): MinuteOfDay {
        val nearest = (value.toFloat() / STEP_MINUTES).roundToInt() * STEP_MINUTES
        return MinuteOfDay(nearest.coerceIn(MIN_VALUE, LAST_GRID_VALUE))
    }

    /**
     * The instant this time falls on in [zoneId].
     *
     * Both awkward daylight-saving cases come out of [java.time] rather than
     * from arithmetic here: a time inside the spring-forward gap is moved
     * forward by the length of the gap, and a time the clocks pass through
     * twice takes the earlier of the two offsets.
     */
    fun onDate(date: LocalDate, zoneId: ZoneId): Instant =
        date.atTime(hours, minutes).atZone(zoneId).toInstant()

    companion object {
        const val MIN_VALUE = 0

        /** 23:59 — the largest time of day that exists. */
        const val MAX_VALUE = 1439

        /** The planner's granularity: a 15-minute slot. */
        const val STEP_MINUTES = 15

        /** 23:45, the last slot on the grid. */
        const val LAST_GRID_VALUE = MAX_VALUE / STEP_MINUTES * STEP_MINUTES

        /** Number of steps between the first and last slot, as Compose's sliders count them. */
        const val GRID_STEPS = LAST_GRID_VALUE / STEP_MINUTES - 1

        val StartOfDay = MinuteOfDay(MIN_VALUE)
        val Noon = MinuteOfDay(12 * 60)

        fun of(hours: Int, minutes: Int): MinuteOfDay = MinuteOfDay(hours * 60 + minutes)

        /** The wall-clock time of [instant] in [zoneId]. */
        fun ofInstant(instant: Instant, zoneId: ZoneId): MinuteOfDay {
            val time = instant.atZone(zoneId).toLocalTime()
            return of(time.hour, time.minute)
        }
    }
}
