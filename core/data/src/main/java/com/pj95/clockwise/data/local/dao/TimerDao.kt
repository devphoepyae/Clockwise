package com.pj95.clockwise.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.pj95.clockwise.data.local.entity.TimerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to the single stored timer.
 *
 * Observation is a flow of one nullable row, so the timer screen and a receiver
 * started by `AlarmManager` read the same source of truth. There is no `insert`
 * versus `update` distinction: the row's key is fixed, and every write replaces
 * it.
 */
@Dao
abstract class TimerDao {

    @Query("SELECT * FROM timer WHERE id = ${TimerEntity.SINGLETON_ID}")
    abstract fun observe(): Flow<TimerEntity?>

    @Query("SELECT * FROM timer WHERE id = ${TimerEntity.SINGLETON_ID}")
    abstract suspend fun get(): TimerEntity?

    @Upsert
    abstract suspend fun upsert(timer: TimerEntity)

    @Query("DELETE FROM timer WHERE id = ${TimerEntity.SINGLETON_ID}")
    abstract suspend fun clear()
}
