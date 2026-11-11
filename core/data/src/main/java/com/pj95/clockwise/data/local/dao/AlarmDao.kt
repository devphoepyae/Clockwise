package com.pj95.clockwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pj95.clockwise.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to the user's alarms.
 *
 * The table is small (a person has a handful of alarms), so reads are
 * unrestricted and the list is observed as one flow: toggling a switch repaints
 * the card, the scheduler and the ringing screen from the same row.
 *
 * Ordering is by the user's chosen position ([AlarmEntity.sortOrder]), with the
 * time of day as a stable tie-break. That keeps the list meaningful before the
 * user has dragged anything — the migration backfills the position by time —
 * and afterwards it is the order they arranged, not the clock's.
 */
@Dao
abstract class AlarmDao {

    @Query(
        "SELECT * FROM alarms " +
            "ORDER BY sortOrder ASC, hour ASC, minute ASC, id ASC",
    )
    abstract fun observeAll(): Flow<List<AlarmEntity>>

    @Query(
        "SELECT * FROM alarms " +
            "ORDER BY sortOrder ASC, hour ASC, minute ASC, id ASC",
    )
    abstract suspend fun getAll(): List<AlarmEntity>

    @Query(
        "SELECT * FROM alarms WHERE enabled = 1 " +
            "ORDER BY sortOrder ASC, hour ASC, minute ASC",
    )
    abstract suspend fun getEnabled(): List<AlarmEntity>

    /** The highest stored position, or -1 when the table is empty. */
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM alarms")
    abstract suspend fun maxSortOrder(): Int

    @Query("UPDATE alarms SET sortOrder = :sortOrder WHERE id = :id")
    abstract suspend fun setSortOrder(id: Long, sortOrder: Int)

    /**
     * Writes a whole new order in one transaction.
     *
     * One statement per row rather than a table rebuild: the list is a handful
     * of rows, and keeping the ids in place means a trigger armed for an alarm
     * stays armed while its card moves.
     */
    @Transaction
    open suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    @Query("SELECT * FROM alarms WHERE id = :id")
    abstract suspend fun getById(id: Long): AlarmEntity?

    @Insert
    abstract suspend fun insert(alarm: AlarmEntity): Long

    @Update
    abstract suspend fun update(alarm: AlarmEntity)

    @Query("UPDATE alarms SET enabled = :enabled WHERE id = :id")
    abstract suspend fun setEnabled(id: Long, enabled: Boolean)

    /**
     * Records a snooze and the instant it is due.
     *
     * One statement rather than a read-modify-write, so two taps on Snooze
     * cannot both read the same count and each decide they are the first.
     */
    @Query(
        "UPDATE alarms SET snoozesUsed = snoozesUsed + 1, " +
            "snoozedAtEpochMillis = :snoozedAt WHERE id = :id",
    )
    abstract suspend fun markSnoozed(id: Long, snoozedAt: Long)

    /** Ends the ring cycle: the next natural fire starts with a clean count. */
    @Query("UPDATE alarms SET snoozesUsed = 0, snoozedAtEpochMillis = NULL WHERE id = :id")
    abstract suspend fun resetSnoozeCycle(id: Long)

    /**
     * Retires a scheduled one-off after it has rung.
     *
     * The date is cleared as well as the switch, so the card stops claiming a
     * schedule that can never come round again.
     */
    @Query(
        "UPDATE alarms SET enabled = 0, scheduledDateEpochDay = NULL, " +
            "snoozesUsed = 0, snoozedAtEpochMillis = NULL WHERE id = :id",
    )
    abstract suspend fun completeScheduled(id: Long)

    /**
     * Forgets that the alarm was snoozed, keeping the count.
     *
     * Called when the snooze itself fires: that ring belongs to the same cycle
     * as the one it was snoozed from.
     */
    @Query("UPDATE alarms SET snoozedAtEpochMillis = NULL WHERE id = :id")
    abstract suspend fun clearSnoozeMarker(id: Long)

    @Query("DELETE FROM alarms WHERE id = :id")
    abstract suspend fun deleteById(id: Long)
}
