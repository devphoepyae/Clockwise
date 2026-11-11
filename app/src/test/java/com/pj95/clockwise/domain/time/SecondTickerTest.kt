package com.pj95.clockwise.domain.time

import com.pj95.clockwise.common.time.SecondTicker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Verifies the ticker stays aligned to wall-clock second boundaries. */
@OptIn(ExperimentalCoroutinesApi::class)
class SecondTickerTest {

    /** A clock driven by virtual time instead of the system clock. */
    private fun TestScope.virtualClock(): Clock = object : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(testScheduler.currentTime)
    }

    @Test
    fun emitsOncePerSecond() = runTest {
        val received = mutableListOf<Long>()
        val job = launch {
            SecondTicker(virtualClock()).ticker().collect { received += it.toEpochMilli() }
        }

        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        job.cancel()

        assertEquals(listOf(0L, 1_000L, 2_000L, 3_000L), received)
    }

    @Test
    fun alignsToTheNextSecondBoundaryWhenStartingMidSecond() = runTest {
        advanceTimeBy(1_250)

        val received = mutableListOf<Long>()
        val job = launch {
            SecondTicker(virtualClock()).ticker().collect { received += it.toEpochMilli() }
        }

        runCurrent()
        advanceTimeBy(1_800)
        runCurrent()
        job.cancel()

        // First tick immediately (1250), then on the 2000 and 3000 boundaries.
        assertEquals(listOf(1_250L, 2_000L, 3_000L), received)
    }
}
