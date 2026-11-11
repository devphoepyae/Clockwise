package com.pj95.clockwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pj95.clockwise.data.local.entity.CityEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to the local city catalog.
 *
 * The catalog is small (currently 22 built-in rows), so every read is
 * unrestricted and the screens can keep working from an in-memory list. Search
 * still runs in SQLite so a future directory with many custom cities does not
 * need a different query path.
 */
@Dao
interface CityDao {

    /** Every city in directory order, observed for future custom-city edits. */
    @Query("SELECT * FROM cities ORDER BY sortOrder ASC, nameEn COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CityEntity>>

    /** One-shot catalog read, used by seeding checks and tests. */
    @Query("SELECT * FROM cities ORDER BY sortOrder ASC, nameEn COLLATE NOCASE ASC")
    suspend fun getAll(): List<CityEntity>

    /** The city with [id], or `null` when it is not in the catalog. */
    @Query("SELECT * FROM cities WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CityEntity?

    /**
     * Inserts or replaces rows in one transaction.
     *
     * Replacement keeps built-in rows current when the bundled directory gains
     * a coordinate fix or a new city; it never touches rows whose ids are not
     * supplied.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cities: List<CityEntity>)

    /** Number of rows currently in the catalog. */
    @Query("SELECT COUNT(*) FROM cities")
    suspend fun count(): Int

    /**
     * Case-insensitive search over the fields the search sheet exposes.
     *
     * SQLite's LIKE is case-insensitive for ASCII by default; the bundled
     * labels and IANA ids are ASCII, so no collation override is needed.
     */
    @Query(
        """
        SELECT * FROM cities
        WHERE nameEn LIKE '%' || :query || '%'
           OR regionEn LIKE '%' || :query || '%'
           OR countryEn LIKE '%' || :query || '%'
           OR timeZone LIKE '%' || :query || '%'
        ORDER BY sortOrder ASC, nameEn COLLATE NOCASE ASC
        """,
    )
    suspend fun search(query: String): List<CityEntity>
}
