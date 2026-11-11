package com.pj95.clockwise.model.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * The four financial centres Clockwise follows, and their offline session rule.
 *
 * A session is the local trading day at each centre: 09:00–18:00, Monday to
 * Friday. Using the city's own [ZoneId] means daylight saving is handled by the
 * platform's tzdb rather than by a fixed UTC offset, and the rule is evaluated
 * without a network call or a holiday feed. Exchange holidays are deliberately
 * not modelled: the app promises a purely offline indicator, not an exchange
 * calendar.
 */
object MarketSessions {

    /** The quick-access strip and the Chronos market card, in this order. */
    val cityIds: List<String> = listOf("sydney", "tokyo", "london", "new-york")

    const val OPEN_HOUR: Int = 9
    const val CLOSE_HOUR: Int = 18

    /** True while [instant] falls inside the local weekday session at [zoneId]. */
    fun isOpen(zoneId: ZoneId, instant: Instant): Boolean {
        val local = instant.atZone(zoneId)
        if (local.dayOfWeek == DayOfWeek.SATURDAY || local.dayOfWeek == DayOfWeek.SUNDAY) {
            return false
        }
        val minuteOfDay = local.hour * 60 + local.minute
        return minuteOfDay >= OPEN_HOUR * 60 && minuteOfDay < CLOSE_HOUR * 60
    }
}
