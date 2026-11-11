package com.pj95.clockwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pj95.clockwise.data.local.entity.SolarDayEntity

/**
 * Read/write access to the solar-day cache.
 *
 * The cache is deliberately small: one row per city per day the user actually
 * looks at. Rows are replaced rather than appended, so asking for the same day
 * again cannot grow the table.
 */
@Dao
interface SolarDao {

    /** The cached row for [cityId] on [dateEpochDay], or `null` when absent. */
    @Query(
        "SELECT * FROM solar_days WHERE cityId = :cityId AND dateEpochDay = :dateEpochDay LIMIT 1",
    )
    suspend fun find(cityId: String, dateEpochDay: Long): SolarDayEntity?

    /** Inserts or replaces one cached day. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: SolarDayEntity)

    /** Drops rows older than [beforeEpochDay], used to bound the cache. */
    @Query("DELETE FROM solar_days WHERE dateEpochDay < :beforeEpochDay")
    suspend fun deleteOlderThan(beforeEpochDay: Long): Int

    /** Empties the cache; tests and a future "clear cache" action use this. */
    @Query("DELETE FROM solar_days")
    suspend fun clear(): Int

    /** Number of cached days. */
    @Query("SELECT COUNT(*) FROM solar_days")
    suspend fun count(): Int
}
