package com.pj95.clockwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.pj95.clockwise.data.local.entity.SavedCityEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to the saved-city display order.
 *
 * The table is tiny (the app caps the list at the cities a person actually
 * follows), so every read is unrestricted. The observable list is what the
 * screen and the widget consume; [replaceAll] is the atomic write every reorder
 * and membership change goes through.
 */
@Dao
abstract class SavedCityDao {

    /** The saved order, observed so a reorder repaints every collector at once. */
    @Query("SELECT * FROM saved_cities ORDER BY position ASC, cityId ASC")
    abstract fun observeAll(): Flow<List<SavedCityEntity>>

    /** One-shot order read, used to seed and reconcile the table. */
    @Query("SELECT * FROM saved_cities ORDER BY position ASC, cityId ASC")
    abstract suspend fun getAll(): List<SavedCityEntity>

    /** Ids only, in display order; cheaper than materialising every row. */
    @Query("SELECT cityId FROM saved_cities ORDER BY position ASC, cityId ASC")
    abstract suspend fun getCityIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertAll(rows: List<SavedCityEntity>)

    @Query("DELETE FROM saved_cities")
    abstract suspend fun clear()

    /**
     * Replaces the whole order in one transaction.
     *
     * The delete and the insert must not be observable separately: a collector
     * that saw the empty intermediate state would briefly draw an empty list
     * and the widget could redraw with every city missing.
     */
    @Transaction
    open suspend fun replaceAll(cityIds: List<String>) {
        clear()
        if (cityIds.isNotEmpty()) {
            upsertAll(cityIds.mapIndexed { index, id -> SavedCityEntity(cityId = id, position = index) })
        }
    }

    /** Appends a new city without disturbing the existing order. */
    @Transaction
    open suspend fun appendIfMissing(cityId: String) {
        val current = getCityIds()
        if (cityId !in current) replaceAll(current + cityId)
    }

    /** Removes a city and closes the gap in the positions. */
    @Transaction
    open suspend fun remove(cityId: String) {
        val current = getCityIds()
        if (cityId in current) replaceAll(current.filterNot { it == cityId })
    }
}
