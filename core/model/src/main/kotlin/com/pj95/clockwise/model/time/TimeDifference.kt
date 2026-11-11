package com.pj95.clockwise.model.time

import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

/**
 * How far one zone's local time sits from a reference zone's, in whole minutes.
 *
 * Positive means the zone is *ahead* of the reference. The difference is kept
 * as a signed minute count rather than as whole hours because zones use
 * 30- and 45-minute offsets (UTC+5:30, UTC+6:30, UTC+5:45), and rounding those
 * away would make the indicator wrong for a large part of the world.
 */
@JvmInline
value class TimeDifference(val minutes: Int) {

    /** True when both zones currently read the same clock time. */
    val isSameTime: Boolean get() = minutes == 0

    /** True when this zone is ahead of the reference. */
    val isAhead: Boolean get() = minutes > 0

    /** Whole hours in the difference, never negative. */
    val hours: Int get() = abs(minutes) / 60

    /** Minutes left over after [hours], never negative. */
    val minutesPart: Int get() = abs(minutes) % 60

    companion object {
        /** No difference at all. */
        val None = TimeDifference(0)

        /**
         * The difference between [zoneId] and [referenceZoneId] at [instant].
         *
         * Both offsets come from the zone's own [java.time.ZoneRules], so
         * daylight saving is applied automatically and no offset is ever
         * hard-coded (spec section 6).
         */
        fun between(
            zoneId: ZoneId,
            referenceZoneId: ZoneId,
            instant: Instant,
        ): TimeDifference {
            val seconds = zoneId.rules.getOffset(instant).totalSeconds -
                referenceZoneId.rules.getOffset(instant).totalSeconds
            return TimeDifference(seconds / 60)
        }
    }
}
