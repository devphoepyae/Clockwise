package com.pj95.clockwise.data.repository

import com.pj95.clockwise.data.local.dao.TimerDao
import com.pj95.clockwise.data.local.entity.TimerEntity
import com.pj95.clockwise.model.timer.CountdownTimer
import com.pj95.clockwise.domain.timer.TimerAlerting
import com.pj95.clockwise.domain.timer.TimerScheduling
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

/**
 * The one countdown timer, as the rest of the app sees it.
 *
 * An interface because two observers sit on either side of it: the timer screen
 * renders a flow, and a receiver that may have started the process reads the
 * same row without a screen. It also lets the repository be unit-tested with an
 * in-memory store, a recording scheduler and a fake notification sink.
 */
interface TimerRepository {

    /** The stored timer, or `null` when none has been started. */
    val timer: Flow<CountdownTimer?>

    /** True when the platform will honour an exact wake-up. */
    fun canScheduleExact(): Boolean

    suspend fun get(): CountdownTimer?

    /**
     * Starts (or replaces) the timer for [durationMillis] and arms its wake-up.
     *
     * The duration is clamped to a range that can actually be displayed and
     * heard, so a wheel that lands on 00:00:00 cannot start a timer that is
     * already over.
     */
    suspend fun start(durationMillis: Long): CountdownTimer

    /** Freezes the remaining time, disarms the wake-up and removes the countdown. */
    suspend fun pause()

    /** Continues a paused timer from its frozen remainder. */
    suspend fun resume()

    /**
     * Adds [deltaMillis] to a running or paused timer.
     *
     * The original duration grows with the addition, so the ring keeps showing
     * the whole countdown rather than a remainder larger than its own start.
     * A finished timer has nothing to extend; use [start] to run it again.
     */
    suspend fun extendBy(deltaMillis: Long)

    /** Stops the timer and clears the stored row. */
    suspend fun reset()

    /**
     * Marks the timer as run out and rings the "time is up" notification.
     *
     * Called by the trigger receiver and by process start when a wake-up was
     * missed (a reboot or a force-stop).
     */
    suspend fun finish()

    /**
     * Puts a stored running timer back into `AlarmManager` after a reboot, an
     * update or a clock change. An already-elapsed timer rings immediately
     * rather than disappearing.
     */
    suspend fun rescheduleRunning()
}

/**
 * Room-backed implementation.
 *
 * Every write is followed by the matching `AlarmManager` call and notification
 * update in the same coroutine, so the row, the armed trigger and the shade
 * cannot drift apart.
 */
class RoomTimerRepository(
    private val dao: TimerDao,
    private val scheduling: TimerScheduling,
    private val alerting: TimerAlerting,
    private val clock: Clock,
) : TimerRepository {

    override val timer: Flow<CountdownTimer?> =
        dao.observe().map { it?.toDomain() }

    override fun canScheduleExact(): Boolean = scheduling.canScheduleExact()

    override suspend fun get(): CountdownTimer? = dao.get()?.toDomain()

    override suspend fun start(durationMillis: Long): CountdownTimer {
        val duration = durationMillis.coerceIn(
            CountdownTimer.MIN_DURATION_MILLIS,
            CountdownTimer.MAX_DURATION_MILLIS,
        )
        val now = clock.millis()
        val timer = CountdownTimer(
            durationMillis = duration,
            remainingMillis = duration,
            endAtEpochMillis = now + duration,
            running = true,
        )
        dao.upsert(timer.toEntity())
        scheduling.schedule(Instant.ofEpochMilli(timer.endAtEpochMillis!!))
        alerting.cancelFinished()
        alerting.showRunning(timer, duration)
        return timer
    }

    override suspend fun pause() {
        val timer = dao.get()?.toDomain() ?: return
        if (!timer.running) return

        val remaining = timer.remainingAt(clock.millis())
        if (remaining <= 0) {
            finish()
            return
        }

        val paused = timer.copy(
            remainingMillis = remaining,
            endAtEpochMillis = null,
            running = false,
        )
        dao.upsert(paused.toEntity())
        scheduling.cancel()
        alerting.cancelRunning()
    }

    override suspend fun resume() {
        val timer = dao.get()?.toDomain() ?: return
        if (timer.running) return
        val remaining = timer.remainingMillis
        if (remaining <= 0) return

        val endAt = clock.millis() + remaining
        val running = timer.copy(
            remainingMillis = remaining,
            endAtEpochMillis = endAt,
            running = true,
        )
        dao.upsert(running.toEntity())
        scheduling.schedule(Instant.ofEpochMilli(endAt))
        alerting.cancelFinished()
        alerting.showRunning(running, remaining)
    }

    override suspend fun reset() {
        dao.clear()
        scheduling.cancel()
        alerting.cancelRunning()
        alerting.cancelFinished()
    }

    override suspend fun extendBy(deltaMillis: Long) {
        if (deltaMillis <= 0) return
        val timer = dao.get()?.toDomain() ?: return
        val now = clock.millis()
        val remaining = if (timer.running) timer.remainingAt(now) else timer.remainingMillis
        if (remaining <= 0) return

        // Grow the original duration with the addition so the progress ring
        // still starts at zero; clamp both to the picker's maximum.
        val duration = (timer.durationMillis + deltaMillis)
            .coerceAtMost(CountdownTimer.MAX_DURATION_MILLIS)
        val grownBy = duration - timer.durationMillis
        if (grownBy <= 0) return
        val newRemaining = (remaining + grownBy).coerceAtMost(CountdownTimer.MAX_DURATION_MILLIS)

        val extended = timer.copy(
            durationMillis = duration,
            remainingMillis = newRemaining,
            endAtEpochMillis = if (timer.running) now + newRemaining else null,
        )
        dao.upsert(extended.toEntity())
        if (extended.running) {
            scheduling.schedule(Instant.ofEpochMilli(extended.endAtEpochMillis!!))
            alerting.showRunning(extended, newRemaining)
        }
    }

    override suspend fun finish() {
        val timer = dao.get()?.toDomain() ?: return
        dao.upsert(
            timer.copy(
                remainingMillis = 0,
                endAtEpochMillis = null,
                running = false,
            ).toEntity(),
        )
        scheduling.cancel()
        alerting.cancelRunning()
        alerting.showFinished(timer)
    }

    override suspend fun rescheduleRunning() {
        val timer = dao.get()?.toDomain() ?: return
        if (!timer.running) return

        val endAt = timer.endAtEpochMillis ?: run {
            // A running row without an end instant is inconsistent; treat it as
            // paused with whatever is left rather than arming the wrong moment.
            dao.upsert(timer.copy(running = false).toEntity())
            alerting.cancelRunning()
            return
        }

        if (endAt <= clock.millis()) {
            finish()
            return
        }

        scheduling.schedule(Instant.ofEpochMilli(endAt))
        alerting.showRunning(timer, timer.remainingAt(clock.millis()))
    }
}

/** The database row as the domain value. */
fun TimerEntity.toDomain(): CountdownTimer = CountdownTimer(
    durationMillis = durationMillis,
    remainingMillis = remainingMillis,
    endAtEpochMillis = endAtEpochMillis,
    running = running,
    label = label,
)

/** The domain value as the database row. */
fun CountdownTimer.toEntity(): TimerEntity = TimerEntity(
    durationMillis = durationMillis,
    remainingMillis = remainingMillis,
    endAtEpochMillis = endAtEpochMillis,
    running = running,
    label = label,
)
