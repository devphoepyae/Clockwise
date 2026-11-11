package com.pj95.clockwise.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The solar cache is bounded by a date, and that date has to be the user's
 * local one.
 *
 * The cases below pin the two ways this can go wrong: a UTC cutoff would prune
 * a day early in Yangon (UTC+6:30) and a day late in Los Angeles (UTC-7), so the
 * tests fix one instant and assert the different local cutoffs it produces.
 */
class SolarCachePolicyTest {

    private val yangon = ZoneId.of("Asia/Yangon")
    private val losAngeles = ZoneId.of("America/Los_Angeles")
    private val clock = Clock.fixed(Instant.parse("2026-10-27T09:00:00Z"), ZoneOffset.UTC)

    @Test
    fun retentionIsThirtyDays() {
        assertEquals(30L, SolarCachePolicy.RETENTION_DAYS)
    }

    @Test
    fun cutoffIsThirtyDaysBeforeTheDeviceLocalToday() {
        assertEquals(
            LocalDate.of(2026, 9, 27),
            SolarCachePolicy.cutoff(clock, yangon),
        )
    }

    @Test
    fun cutoffFollowsTheDeviceZoneAcrossUtcMidnight() {
        // 18:30Z is already 01:00 on 28 October in Yangon, while UTC is still
        // on the 27th. The cache should age with the user's calendar.
        val afterUtcMidnight = Clock.fixed(
            Instant.parse("2026-10-27T18:30:00Z"),
            ZoneOffset.UTC,
        )

        assertEquals(
            LocalDate.of(2026, 9, 28),
            SolarCachePolicy.cutoff(afterUtcMidnight, yangon),
        )
        assertEquals(
            LocalDate.of(2026, 9, 27),
            SolarCachePolicy.cutoff(afterUtcMidnight, ZoneOffset.UTC),
        )
    }

    @Test
    fun aZoneBehindUtcKeepsThePreviousLocalDay() {
        // 02:00Z on 28 October is still 19:00 on the 27th in Los Angeles.
        val beforeLocalMidnight = Clock.fixed(
            Instant.parse("2026-10-28T02:00:00Z"),
            ZoneOffset.UTC,
        )

        assertEquals(
            LocalDate.of(2026, 9, 27),
            SolarCachePolicy.cutoff(beforeLocalMidnight, losAngeles),
        )
    }

    @Test
    fun cutoffIsStableThroughoutTheLocalDay() {
        // 01:00Z and 16:00Z are both 27 October in Yangon (07:30 and 22:30),
        // so the cache's oldest day must not move between them.
        val morning = Clock.fixed(Instant.parse("2026-10-27T01:00:00Z"), ZoneOffset.UTC)
        val evening = Clock.fixed(Instant.parse("2026-10-27T16:00:00Z"), ZoneOffset.UTC)

        assertEquals(
            SolarCachePolicy.cutoff(morning, yangon),
            SolarCachePolicy.cutoff(evening, yangon),
        )
    }
}
