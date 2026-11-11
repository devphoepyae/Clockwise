package com.pj95.clockwise.common.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Instant

/**
 * Emits the current instant once per second, aligned to the wall-clock second
 * boundary.
 *
 * Because the delay is computed from the current instant rather than being a
 * flat 1000 ms, the clock stays in step with the second hand instead of
 * drifting, and the displayed time never lags by up to a second.
 *
 * The flow is cold: it only ticks while someone collects it. Combined with
 * `collectAsStateWithLifecycle`/`stateIn(WhileSubscribed)` the ticking stops
 * whenever the UI is not visible.
 */
class SecondTicker(
    private val clock: Clock = Clock.systemUTC(),
) {
    fun ticker(): Flow<Instant> = flow {
        while (true) {
            val now = clock.instant()
            emit(now)
            delay(millisUntilNextSecond(now))
        }
    }

    private fun millisUntilNextSecond(instant: Instant): Long {
        val remainder = instant.toEpochMilli().mod(SECOND_MILLIS)
        return if (remainder == 0L) SECOND_MILLIS else SECOND_MILLIS - remainder
    }

    private companion object {
        const val SECOND_MILLIS = 1_000L
    }
}
