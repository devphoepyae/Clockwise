package com.pj95.clockwise.data.repository

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * The retention policy for the Room-backed solar-day cache.
 *
 * The cache is an optimisation, not user data, and its key includes the date:
 * without a bound it would grow by one row per city per day the app is used.
 * Thirty days is deliberately generous — the map only travels through the
 * current day and the detail screen only asks for today, so anything older is
 * never read by the current UI — while keeping the table small enough that a
 * cache lookup stays cheap on a low-memory device.
 *
 * The cutoff is computed from the *device's* local date, not UTC, because the
 * user's "today" is the date the UI asks about. A UTC cutoff would prune a day
 * early or late for every zone east or west of Greenwich.
 */
object SolarCachePolicy {

    /** How many days of solar rows to keep behind the device's local today. */
    const val RETENTION_DAYS: Long = 30L

    /**
     * The oldest date that should remain in the cache, given [clock] and the
     * device's [zoneId].
     */
    fun cutoff(
        clock: Clock,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): LocalDate = LocalDate.now(clock.withZone(zoneId)).minusDays(RETENTION_DAYS)
}
