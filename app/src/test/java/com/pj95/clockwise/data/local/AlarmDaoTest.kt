package com.pj95.clockwise.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.data.local.dao.AlarmDao
import com.pj95.clockwise.data.local.entity.AlarmEntity
import com.pj95.clockwise.model.alarm.RepeatDays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The alarm table: rows survive a round trip unchanged, the observable list is
 * ordered by time of day, and the two queries the scheduler relies on —
 * "enabled only" and "by id" — return what they say.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AlarmDaoTest {

    private lateinit var database: ClockwiseDatabase
    private lateinit var dao: AlarmDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ClockwiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.alarmDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun anEmptyTableReadsAsAnEmptyList() = runBlocking {
        assertTrue(dao.getAll().isEmpty())
        assertTrue(dao.observeAll().first().isEmpty())
        assertTrue(dao.getEnabled().isEmpty())
    }

    @Test
    fun anInsertedAlarmRoundTripsIncludingItsRepeatMask() = runBlocking {
        val id = dao.insert(
            AlarmEntity(
                hour = 6,
                minute = 30,
                label = "Morning run",
                enabled = true,
                repeatDays = RepeatDays.WEEKDAYS,
            ),
        )

        assertEquals(
            AlarmEntity(
                id = id,
                hour = 6,
                minute = 30,
                label = "Morning run",
                enabled = true,
                repeatDays = RepeatDays.WEEKDAYS,
            ),
            dao.getById(id),
        )
    }

    @Test
    fun theObservableListIsOrderedByTimeOfDayNotByInsertion() = runBlocking {
        dao.insert(AlarmEntity(hour = 18, minute = 45))
        dao.insert(AlarmEntity(hour = 6, minute = 30))
        dao.insert(AlarmEntity(hour = 7, minute = 0))

        assertEquals(
            listOf(6 to 30, 7 to 0, 18 to 45),
            dao.observeAll().first().map { it.hour to it.minute },
        )
    }

    @Test
    fun theChosenOrderWinsOverTheTimeOfDay() = runBlocking {
        val evening = dao.insert(AlarmEntity(hour = 18, minute = 0, sortOrder = 0))
        val morning = dao.insert(AlarmEntity(hour = 6, minute = 0, sortOrder = 1))

        assertEquals(listOf(evening, morning), dao.observeAll().first().map { it.id })
    }

    @Test
    fun reorderingRewritesEveryPositionInTheGivenSequence() = runBlocking {
        val first = dao.insert(AlarmEntity(hour = 6, minute = 0))
        val second = dao.insert(AlarmEntity(hour = 7, minute = 0))
        val third = dao.insert(AlarmEntity(hour = 8, minute = 0))

        dao.reorder(listOf(third, first, second))

        assertEquals(listOf(third, first, second), dao.getAll().map { it.id })
        assertEquals(listOf(0, 1, 2), dao.getAll().map { it.sortOrder })
    }

    @Test
    fun theHighestPositionStartsAtMinusOneAndTracksTheLastCard() = runBlocking {
        assertEquals(-1, dao.maxSortOrder())

        dao.insert(AlarmEntity(hour = 6, minute = 0))
        dao.insert(AlarmEntity(hour = 7, minute = 0, sortOrder = 4))

        assertEquals(4, dao.maxSortOrder())
    }

    @Test
    fun onlyEnabledAlarmsAreReturnedForRearming() = runBlocking {
        dao.insert(AlarmEntity(hour = 6, minute = 0, enabled = true))
        dao.insert(AlarmEntity(hour = 7, minute = 0, enabled = false))
        dao.insert(AlarmEntity(hour = 8, minute = 0, enabled = true))

        assertEquals(listOf(6 to 0, 8 to 0), dao.getEnabled().map { it.hour to it.minute })
    }

    @Test
    fun setEnabledWritesOnlyTheFlag() = runBlocking {
        val id = dao.insert(AlarmEntity(hour = 7, minute = 15, label = "Stand-up"))

        dao.setEnabled(id, false)

        val stored = dao.getById(id)!!
        assertEquals(false, stored.enabled)
        assertEquals(7, stored.hour)
        assertEquals(15, stored.minute)
        assertEquals("Stand-up", stored.label)
    }

    @Test
    fun deletingRemovesTheRowAndLeavesTheRest() = runBlocking {
        val keep = dao.insert(AlarmEntity(hour = 7, minute = 0))
        val remove = dao.insert(AlarmEntity(hour = 8, minute = 0))

        dao.deleteById(remove)

        assertNull(dao.getById(remove))
        assertEquals(listOf(keep), dao.getAll().map { it.id })
    }

    @Test
    fun updatingReplacesTheStoredValues() = runBlocking {
        val id = dao.insert(AlarmEntity(hour = 7, minute = 0, label = "Old"))

        dao.update(
            AlarmEntity(
                id = id,
                hour = 9,
                minute = 45,
                label = "New",
                enabled = false,
                repeatDays = RepeatDays.WEEKEND,
            ),
        )

        assertEquals(
            AlarmEntity(
                id = id,
                hour = 9,
                minute = 45,
                label = "New",
                enabled = false,
                repeatDays = RepeatDays.WEEKEND,
            ),
            dao.getById(id),
        )
    }

    @Test
    fun idsAutoincrementAndAreNeverReused() = runBlocking {
        val first = dao.insert(AlarmEntity(hour = 7, minute = 0))
        val second = dao.insert(AlarmEntity(hour = 8, minute = 0))

        dao.deleteById(first)
        val third = dao.insert(AlarmEntity(hour = 9, minute = 0))

        assertTrue(second > first)
        assertTrue(third > second)
    }

    @Test
    fun aSnoozeIsRecordedWithoutTouchingTheAlarmsOwnTime() = runBlocking {
        val id = dao.insert(
            AlarmEntity(hour = 7, minute = 0, repeatDays = RepeatDays.WEEKDAYS),
        )

        dao.markSnoozed(id, snoozedAt = 1_700_000_000_000L)

        val row = dao.getById(id)!!
        assertEquals(7, row.hour)
        assertEquals(0, row.minute)
        assertEquals(RepeatDays.WEEKDAYS, row.repeatDays)
        assertEquals(1, row.snoozesUsed)
        assertEquals(1_700_000_000_000L, row.snoozedAtEpochMillis)
    }

    @Test
    fun theCycleResetClearsBothTheCounterAndTheMarker() = runBlocking {
        val id = dao.insert(AlarmEntity(hour = 7, minute = 0))
        dao.markSnoozed(id, snoozedAt = 1_700_000_000_000L)

        dao.resetSnoozeCycle(id)

        val row = dao.getById(id)!!
        assertEquals(0, row.snoozesUsed)
        assertNull(row.snoozedAtEpochMillis)
    }

    @Test
    fun clearingTheMarkerKeepsTheCount() = runBlocking {
        val id = dao.insert(AlarmEntity(hour = 7, minute = 0))
        dao.markSnoozed(id, snoozedAt = 1_700_000_000_000L)

        dao.clearSnoozeMarker(id)

        val row = dao.getById(id)!!
        assertEquals(1, row.snoozesUsed)
        assertNull(row.snoozedAtEpochMillis)
    }
}
