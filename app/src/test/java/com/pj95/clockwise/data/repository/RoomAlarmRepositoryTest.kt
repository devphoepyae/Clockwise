package com.pj95.clockwise.data.repository

import com.pj95.clockwise.data.local.dao.AlarmDao
import com.pj95.clockwise.data.local.entity.AlarmEntity
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.domain.alarm.AlarmScheduling
import com.pj95.clockwise.model.alarm.RepeatDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * The repository is the seam between "stored" and "armed", so every write is
 * checked twice: the row changed, and `AlarmManager` was told about it.
 *
 * Uses a small in-memory DAO and a recording scheduler rather than Robolectric:
 * the behaviour under test is the ordering of those two effects, which has
 * nothing to do with SQLite or the platform.
 */
class RoomAlarmRepositoryTest {

    private val dao = InMemoryAlarmDao()
    private val scheduler = RecordingScheduler()
    private val now = Instant.parse("2024-01-20T16:40:00Z")
    private val repository = RoomAlarmRepository(
        dao = dao,
        scheduling = scheduler,
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun savingANewAlarmStoresItAndArmsIt() = runBlocking {
        val id = repository.save(
            Alarm(hour = 7, minute = 0, label = "Wake", repeatDays = RepeatDays.WEEKDAYS),
        )

        assertEquals(1L, id)
        assertEquals(
            Alarm(
                id = 1L,
                hour = 7,
                minute = 0,
                label = "Wake",
                enabled = true,
                repeatDays = RepeatDays.WEEKDAYS,
            ),
            dao.getById(1L)?.toDomain(),
        )
        assertEquals(listOf(1L), scheduler.scheduled.map { it.id })
    }

    @Test
    fun savingADisabledAlarmCancelsAnyPendingTrigger() = runBlocking {
        val id = repository.save(Alarm(hour = 7, minute = 0, enabled = false))

        assertFalse(dao.getById(id)!!.enabled)
        assertEquals(listOf(id), scheduler.cancelled)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun savingAnExistingAlarmUpdatesTheRowAndRearmsTheSameId() = runBlocking {
        val id = repository.save(Alarm(hour = 7, minute = 0, label = "Old"))

        repository.save(Alarm(id = id, hour = 9, minute = 30, label = "New"))

        assertEquals(9, dao.getById(id)!!.hour)
        assertEquals(30, dao.getById(id)!!.minute)
        assertEquals("New", dao.getById(id)!!.label)
        assertEquals(listOf(1L, 1L), scheduler.scheduled.map { it.id })
    }

    @Test
    fun disablingAnAlarmCancelsItAndEnablingItArmsItAgain() = runBlocking {
        val id = repository.save(Alarm(hour = 7, minute = 0))
        scheduler.scheduled.clear()

        repository.setEnabled(id, false)
        assertFalse(dao.getById(id)!!.enabled)
        assertEquals(listOf(id), scheduler.cancelled)

        repository.setEnabled(id, true)
        assertTrue(dao.getById(id)!!.enabled)
        assertEquals(listOf(id), scheduler.scheduled.map { it.id })
    }

    @Test
    fun deletingAnAlarmRemovesTheRowAndItsTrigger() = runBlocking {
        val id = repository.save(Alarm(hour = 7, minute = 0))

        repository.delete(id)

        assertNull(dao.getById(id))
        assertEquals(listOf(id), scheduler.cancelled)
    }

    @Test
    fun reorderingRewritesThePositionsWithoutTouchingTheAlarms() = runBlocking {
        val morning = repository.save(Alarm(hour = 7, minute = 0, label = "Morning"))
        val evening = repository.save(Alarm(hour = 19, minute = 0, label = "Evening"))
        scheduler.scheduled.clear()

        repository.reorder(listOf(evening, morning))

        assertEquals(listOf(evening, morning), dao.getAll().map { it.id })
        assertEquals(listOf(0, 1), dao.getAll().map { it.sortOrder })
        assertEquals(7, dao.getById(morning)!!.hour)
        assertEquals(19, dao.getById(evening)!!.hour)
        assertTrue("reordering must not re-arm anything", scheduler.scheduled.isEmpty())
    }

    @Test
    fun restoringADeletedAlarmKeepsItsIdPositionAndArmsItAgain() = runBlocking {
        val id = repository.save(Alarm(hour = 7, minute = 0, label = "Wake"))
        val stored = repository.get(id)!!
        repository.delete(id)
        scheduler.scheduled.clear()

        repository.restore(stored)

        assertEquals(stored, repository.get(id))
        assertEquals(listOf(id), scheduler.scheduled.map { it.id })
    }

    @Test
    fun restoringADisabledAlarmDoesNotArmIt() = runBlocking {
        val id = repository.save(Alarm(hour = 7, minute = 0, enabled = false))
        val stored = repository.get(id)!!
        repository.delete(id)
        scheduler.cancelled.clear()

        repository.restore(stored)

        assertEquals(stored, repository.get(id))
        assertTrue(scheduler.scheduled.isEmpty())
        assertTrue(scheduler.cancelled.isEmpty())
    }

    @Test
    fun savingAnAlarmKeepsItsChosenSound() = runBlocking {
        val id = repository.save(
            Alarm(hour = 7, minute = 0, soundUri = "content://tones/bright"),
        )

        assertEquals("content://tones/bright", dao.getById(id)!!.soundUri)
        assertEquals("content://tones/bright", dao.getById(id)!!.toDomain().soundUri)

        // An alarm that keeps the device default stores an empty string, not a
        // stale copy of whatever the default happened to be on this device.
        val defaultId = repository.save(Alarm(hour = 8, minute = 0))
        assertEquals("", dao.getById(defaultId)!!.soundUri)
    }

    @Test
    fun updatingAnAlarmThatDoesNotExistIsNotAnInsert() = runBlocking {
        // A row deleted while the editor was open: the update affects nothing,
        // and arming a nonexistent alarm would be a phantom trigger.
        repository.save(Alarm(id = 42L, hour = 7, minute = 0))

        assertNull(dao.getById(42L))
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun reschedulingRearmsOnlyEnabledRows() = runBlocking {
        dao.insert(AlarmEntity(hour = 6, minute = 0, enabled = true))
        dao.insert(AlarmEntity(hour = 7, minute = 0, enabled = false))
        dao.insert(AlarmEntity(hour = 8, minute = 0, enabled = true))
        scheduler.scheduled.clear()
        scheduler.cancelled.clear()

        repository.rescheduleAll()

        assertEquals(setOf(1L, 3L), scheduler.scheduled.map { it.id }.toSet())
        assertTrue(
            "Re-arming must not cancel a pending snooze on a disabled row",
            scheduler.cancelled.isEmpty(),
        )
    }

    @Test
    fun reschedulingRestoresAFutureSnoozeInsteadOfTheNaturalTrigger() = runBlocking {
        val due = now.plusSeconds(5 * 60).toEpochMilli()
        val snoozed = dao.insert(
            AlarmEntity(hour = 7, minute = 0, enabled = false, snoozedAtEpochMillis = due),
        )
        val normal = dao.insert(AlarmEntity(hour = 8, minute = 0, enabled = true))
        scheduler.scheduled.clear()
        scheduler.snoozed.clear()

        repository.rescheduleAll()

        assertEquals(listOf(snoozed to Instant.ofEpochMilli(due)), scheduler.snoozed)
        assertEquals(listOf(normal), scheduler.scheduled.map { it.id })
    }

    @Test
    fun snoozingRecordsTheDueInstantAndArmsThatOneTrigger() = runBlocking {
        val id = repository.save(Alarm(hour = 7, minute = 0, label = "Wake"))
        scheduler.scheduled.clear()

        repository.snooze(id, minutes = 10)

        val due = now.plusSeconds(10 * 60).toEpochMilli()
        val row = dao.getById(id)!!
        assertEquals(1, row.snoozesUsed)
        assertEquals(due, row.snoozedAtEpochMillis)
        assertEquals(listOf(id to Instant.ofEpochMilli(due)), scheduler.snoozed)
        assertTrue("a snooze must not re-arm the natural occurrence", scheduler.scheduled.isEmpty())
    }

    @Test
    fun aSnoozedOneShotAlarmCanStillBeSnoozedAfterItSwitchedItselfOff() = runBlocking {
        val id = repository.save(Alarm(hour = 7, minute = 0))
        repository.setEnabled(id, false)

        repository.snooze(id, minutes = 10)

        assertEquals(1, dao.getById(id)!!.snoozesUsed)
        assertEquals(listOf(id), scheduler.snoozed.map { it.first })
    }

    @Test
    fun consumingASnoozeClearsTheMarkerButKeepsTheCycleCount() = runBlocking {
        val id = repository.save(Alarm(hour = 7, minute = 0))
        repository.snooze(id, minutes = 5)

        repository.consumeSnooze(id)

        val row = dao.getById(id)!!
        assertEquals(1, row.snoozesUsed)
        assertNull(row.snoozedAtEpochMillis)
    }

    @Test
    fun resettingTheSnoozeCycleClearsBothTheMarkerAndTheCount() = runBlocking {
        val id = repository.save(Alarm(hour = 7, minute = 0))
        repository.snooze(id, minutes = 5)

        repository.resetSnoozeCycle(id)

        val row = dao.getById(id)!!
        assertEquals(0, row.snoozesUsed)
        assertNull(row.snoozedAtEpochMillis)
    }

    @Test
    fun snoozingADeletedAlarmDoesNotArmAPhantomTrigger() = runBlocking {
        repository.snooze(42L, minutes = 10)

        assertTrue(scheduler.snoozed.isEmpty())
    }

    @Test
    fun theObservableListMapsEveryRowToTheDomainValue() = runBlocking {
        repository.save(Alarm(hour = 18, minute = 45, label = "Evening"))
        repository.save(Alarm(hour = 6, minute = 30, label = "Morning"))

        // New alarms append to the user's arrangement, so the later-created
        // 18:45 card sits first even though its clock time is later.
        assertEquals(
            listOf(18 to 45, 6 to 30),
            repository.alarms.first().map { it.hour to it.minute },
        )
        assertEquals(
            listOf("Evening", "Morning"),
            repository.alarms.first().map { it.label },
        )
    }

    @Test
    fun theExactAlarmAnswerComesFromThePlatformAdapter() {
        assertTrue(repository.canScheduleExact())

        scheduler.exact = false

        assertFalse(repository.canScheduleExact())
    }
}

/** A DAO with the same contract as the Room one, held in a flow. */
private class InMemoryAlarmDao : AlarmDao() {

    private val rows = MutableStateFlow<List<AlarmEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<AlarmEntity>> = rows.map(::ordered)

    override suspend fun getAll(): List<AlarmEntity> = ordered(rows.value)

    override suspend fun getEnabled(): List<AlarmEntity> =
        ordered(rows.value.filter { it.enabled })

    override suspend fun getById(id: Long): AlarmEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun maxSortOrder(): Int =
        rows.value.maxOfOrNull { it.sortOrder } ?: -1

    override suspend fun setSortOrder(id: Long, sortOrder: Int) {
        rows.update { current ->
            current.map { if (it.id == id) it.copy(sortOrder = sortOrder) else it }
        }
    }

    override suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    override suspend fun insert(alarm: AlarmEntity): Long {
        // Room honours an explicit id, which is how Undo puts a deleted row
        // back under the id the rest of the app already refers to.
        val id = if (alarm.id != 0L) alarm.id else nextId++
        if (id >= nextId) nextId = id + 1
        rows.update { it + alarm.copy(id = id) }
        return id
    }

    override suspend fun update(alarm: AlarmEntity) {
        rows.update { current -> current.map { if (it.id == alarm.id) alarm else it } }
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        rows.update { current ->
            current.map { if (it.id == id) it.copy(enabled = enabled) else it }
        }
    }

    override suspend fun markSnoozed(id: Long, snoozedAt: Long) {
        rows.update { current ->
            current.map {
                if (it.id == id) {
                    it.copy(snoozesUsed = it.snoozesUsed + 1, snoozedAtEpochMillis = snoozedAt)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun resetSnoozeCycle(id: Long) {
        rows.update { current ->
            current.map {
                if (it.id == id) it.copy(snoozesUsed = 0, snoozedAtEpochMillis = null) else it
            }
        }
    }

    override suspend fun completeScheduled(id: Long) {
        rows.update { current ->
            current.map {
                if (it.id == id) {
                    it.copy(
                        enabled = false,
                        scheduledDateEpochDay = null,
                        snoozesUsed = 0,
                        snoozedAtEpochMillis = null,
                    )
                } else {
                    it
                }
            }
        }
    }

    override suspend fun clearSnoozeMarker(id: Long) {
        rows.update { current ->
            current.map {
                if (it.id == id) it.copy(snoozedAtEpochMillis = null) else it
            }
        }
    }

    override suspend fun deleteById(id: Long) {
        rows.update { current -> current.filterNot { it.id == id } }
    }

    private fun ordered(list: List<AlarmEntity>): List<AlarmEntity> =
        list.sortedWith(
            compareBy({ it.sortOrder }, { it.hour }, { it.minute }, { it.id }),
        )
}

/** Records every call so the test can assert the ordering of stored vs armed. */
private class RecordingScheduler : AlarmScheduling {

    val scheduled = mutableListOf<Alarm>()
    val cancelled = mutableListOf<Long>()
    val snoozed = mutableListOf<Pair<Long, Instant>>()
    var exact = true

    override fun schedule(alarm: Alarm) {
        scheduled += alarm
    }

    override fun cancel(alarmId: Long) {
        cancelled += alarmId
    }

    override fun scheduleSnooze(alarmId: Long, triggerAt: Instant) {
        snoozed += alarmId to triggerAt
    }

    override fun canScheduleExact(): Boolean = exact
}
