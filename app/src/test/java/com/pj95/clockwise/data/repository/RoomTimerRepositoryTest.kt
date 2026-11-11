package com.pj95.clockwise.data.repository

import com.pj95.clockwise.data.local.dao.TimerDao
import com.pj95.clockwise.data.local.entity.TimerEntity
import com.pj95.clockwise.model.timer.CountdownTimer
import com.pj95.clockwise.domain.timer.TimerAlerting
import com.pj95.clockwise.domain.timer.TimerScheduling
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The timer repository is the seam between "stored", "armed" and "announced".
 * Every write is checked three times: the row changed, the wake-up changed and
 * the notification was told.
 */
class RoomTimerRepositoryTest {

    private val dao = InMemoryTimerDao()
    private val scheduler = RecordingTimerScheduler()
    private val alerts = RecordingTimerAlerting()
    private val clock = MutableClock(Instant.parse("2026-10-28T06:00:00Z"))
    private val repository = RoomTimerRepository(
        dao = dao,
        scheduling = scheduler,
        alerting = alerts,
        clock = clock,
    )

    @Test
    fun startingStoresTheEndInstantAndArmsIt() = runBlocking {
        val timer = repository.start(300_000)

        assertEquals(300_000, timer.durationMillis)
        assertEquals(clock.millis() + 300_000, timer.endAtEpochMillis)
        assertTrue(timer.running)
        assertEquals(Instant.ofEpochMilli(timer.endAtEpochMillis!!), scheduler.scheduled)
        assertEquals(listOf(300_000L), alerts.runningRemaining)
        assertEquals(300_000, dao.get()!!.remainingMillis)
    }

    @Test
    fun aDurationShorterThanOneSecondIsRaisedToTheMinimum() = runBlocking {
        val timer = repository.start(0)

        assertEquals(CountdownTimer.MIN_DURATION_MILLIS, timer.durationMillis)
        assertEquals(CountdownTimer.MIN_DURATION_MILLIS, timer.remainingMillis)
    }

    @Test
    fun pausingFreezesTheRemainderAndDisarmsTheWakeUp() = runBlocking {
        repository.start(300_000)
        clock.instant = clock.instant.plusSeconds(100)

        repository.pause()

        val stored = dao.get()!!
        assertEquals(200_000, stored.remainingMillis)
        assertNull(stored.endAtEpochMillis)
        assertFalse(stored.running)
        assertEquals(1, scheduler.cancelled)
        assertTrue(alerts.runningCancelled)
    }

    @Test
    fun resumingCountsFromTheFrozenRemainder() = runBlocking {
        repository.start(300_000)
        clock.instant = clock.instant.plusSeconds(100)
        repository.pause()
        scheduler.scheduled = null
        alerts.runningRemaining.clear()
        clock.instant = clock.instant.plusSeconds(500)

        repository.resume()

        val stored = dao.get()!!
        assertEquals(clock.millis() + 200_000, stored.endAtEpochMillis)
        assertTrue(stored.running)
        assertEquals(Instant.ofEpochMilli(stored.endAtEpochMillis!!), scheduler.scheduled)
        assertEquals(listOf(200_000L), alerts.runningRemaining)
    }

    @Test
    fun finishingMarksZeroAndRingsTheFinishedNotification() = runBlocking {
        repository.start(60_000)
        clock.instant = clock.instant.plusSeconds(60)

        repository.finish()

        val stored = dao.get()!!
        assertEquals(0, stored.remainingMillis)
        assertFalse(stored.running)
        assertEquals(1, scheduler.cancelled)
        assertTrue(alerts.runningCancelled)
        assertEquals(1, alerts.finished.size)
        assertEquals(60_000, alerts.finished.single().durationMillis)
    }

    @Test
    fun resettingClearsTheRowAndBothNotifications() = runBlocking {
        repository.start(60_000)

        repository.reset()

        assertNull(dao.get())
        assertEquals(1, scheduler.cancelled)
        assertTrue(alerts.runningCancelled)
        assertTrue(alerts.finishedCancelled)
    }

    @Test
    fun restoringAFutureTimerRearmsItWithoutChangingItsEnd() = runBlocking {
        repository.start(300_000)
        val endAt = dao.get()!!.endAtEpochMillis
        scheduler.scheduled = null
        alerts.runningRemaining.clear()

        repository.rescheduleRunning()

        assertEquals(Instant.ofEpochMilli(endAt!!), scheduler.scheduled)
        assertEquals(endAt, dao.get()!!.endAtEpochMillis)
        assertEquals(1, alerts.runningRemaining.size)
    }

    @Test
    fun restoringAMissedTimerRingsImmediately() = runBlocking {
        repository.start(60_000)
        clock.instant = clock.instant.plusSeconds(90)

        repository.rescheduleRunning()

        assertEquals(1, alerts.finished.size)
        assertEquals(0, dao.get()!!.remainingMillis)
        assertTrue(scheduler.cancelled >= 1)
    }

    @Test
    fun aPausedTimerIsNotRestoredOrRearmed() = runBlocking {
        repository.start(300_000)
        repository.pause()
        scheduler.scheduled = null
        alerts.runningRemaining.clear()

        repository.rescheduleRunning()

        assertNull(scheduler.scheduled)
        assertTrue(alerts.runningRemaining.isEmpty())
    }

    @Test
    fun theObservableTimerIsTheStoredRow() = runBlocking {
        assertNull(repository.timer.first())

        repository.start(90_000)

        assertEquals(90_000L, repository.timer.first()?.durationMillis)
    }
}

/** A DAO with the same contract as the Room one, held in a flow. */
private class InMemoryTimerDao : TimerDao() {

    private val row = MutableStateFlow<TimerEntity?>(null)

    override fun observe(): Flow<TimerEntity?> = row.map { it }

    override suspend fun get(): TimerEntity? = row.value

    override suspend fun upsert(timer: TimerEntity) {
        row.value = timer.copy(id = TimerEntity.SINGLETON_ID)
    }

    override suspend fun clear() {
        row.value = null
    }
}

private class RecordingTimerScheduler : TimerScheduling {

    var scheduled: Instant? = null
    var cancelled: Int = 0
    var exact: Boolean = true

    override fun schedule(at: Instant) {
        scheduled = at
    }

    override fun cancel() {
        scheduled = null
        cancelled += 1
    }

    override fun canScheduleExact(): Boolean = exact
}

private class RecordingTimerAlerting : TimerAlerting {

    val runningRemaining = mutableListOf<Long>()
    val finished = mutableListOf<CountdownTimer>()
    var runningCancelled = false
    var finishedCancelled = false

    override fun showRunning(timer: CountdownTimer, remainingMillis: Long) {
        runningRemaining += remainingMillis
    }

    override fun cancelRunning() {
        runningCancelled = true
    }

    override fun showFinished(timer: CountdownTimer) {
        finished += timer
    }

    override fun cancelFinished() {
        finishedCancelled = true
    }
}

/** A clock the test can move between suspend calls. */
private class MutableClock(var instant: Instant) : Clock() {
    override fun instant(): Instant = instant
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
}
