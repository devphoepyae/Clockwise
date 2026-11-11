package com.pj95.clockwise.common.time

import com.pj95.clockwise.model.time.CityTime
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * Turns an IANA timezone plus an instant into the strings the UI shows.
 *
 * All offsets and daylight-saving shifts come from [java.time.ZoneRules] via
 * the zone's rules for that instant — offsets are never hard-coded (spec
 * section 6).
 */
object CityTimeFormatter {

    /** Sun from 06:00, moon from 18:00 (spec section 6). */
    private const val DAY_START_HOUR = 6
    private const val DAY_END_HOUR = 18

    private const val AM = "AM"
    private const val PM = "PM"

    private val dateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    fun snapshot(zoneId: ZoneId, instant: Instant, use24Hour: Boolean): CityTime {
        val zoned = instant.atZone(zoneId)
        val hour24 = zoned.hour
        val minute = zoned.minute

        return CityTime(
            offsetLabel = offsetLabel(zoneId, instant),
            timeText = timeText(zoned.toLocalTime(), use24Hour),
            hourText = hourText(hour24, use24Hour),
            minuteText = twoDigits(minute),
            periodText = periodText(hour24, use24Hour),
            weekdayShort = zoned.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
            dateText = zoned.format(dateFormat),
            isDaytime = isDaytime(zoneId, instant),
        )
    }

    /**
     * A wall-clock time in the app-wide format, e.g. "16:40" or "4:40 PM".
     *
     * Used for solar events, whose times are [LocalTime] values rather than full
     * zone-aware instants but must read exactly like every other clock in the
     * app.
     */
    fun timeText(time: LocalTime, use24Hour: Boolean): String =
        clockText(time, use24Hour).text

    /**
     * The same time split into the parts a screen styles separately.
     *
     * Alarm cards and the ringing screen set the digits large and the "AM"/"PM"
     * small beside them. Splitting the string here — rather than at each call
     * site — keeps the 12-hour rule in one place, and [ClockText.text] still
     * produces exactly the string [timeText] always has, so notifications and
     * tests are unaffected.
     */
    fun clockText(time: LocalTime, use24Hour: Boolean): ClockText = ClockText(
        hourText = hourText(time.hour, use24Hour),
        minuteText = twoDigits(time.minute),
        periodText = periodText(time.hour, use24Hour),
    )

    private fun hourText(hour24: Int, use24Hour: Boolean): String =
        if (use24Hour) {
            twoDigits(hour24)
        } else {
            (if (hour24 % 12 == 0) 12 else hour24 % 12).toString()
        }

    private fun periodText(hour24: Int, use24Hour: Boolean): String? =
        if (use24Hour) null else if (hour24 < 12) AM else PM

    /**
     * The UTC offset in effect at [instant], e.g. "UTC+9", "UTC-8", "UTC+5:30"
     * or "UTC+0". Minutes are only shown when non-zero.
     */
    fun offsetLabel(zoneId: ZoneId, instant: Instant): String {
        val totalMinutes = zoneId.rules.getOffset(instant).totalSeconds / 60
        val sign = if (totalMinutes < 0) "-" else "+"
        val absolute = abs(totalMinutes)
        val hours = absolute / 60
        val minutes = absolute % 60
        return if (minutes == 0) {
            "UTC$sign$hours"
        } else {
            "UTC$sign$hours:${twoDigits(minutes)}"
        }
    }

    /** True between 06:00 and 17:59 local time. */
    fun isDaytime(zoneId: ZoneId, instant: Instant): Boolean {
        val hour = instant.atZone(zoneId).hour
        return hour in DAY_START_HOUR until DAY_END_HOUR
    }

    private fun twoDigits(value: Int): String = if (value < 10) "0$value" else value.toString()
}

/**
 * One wall-clock time, in the pieces the UI renders.
 *
 * [periodText] is `null` in 24-hour mode, which is what lets a screen omit the
 * suffix entirely rather than blanking a placeholder. Pure data, so the split
 * is unit-testable without a device.
 */
data class ClockText(
    val hourText: String,
    val minuteText: String,
    val periodText: String?,
) {
    /** "16:40", or "4:40 PM" in 12-hour mode. */
    val text: String
        get() = if (periodText == null) {
            "$hourText:$minuteText"
        } else {
            "$hourText:$minuteText $periodText"
        }
}
