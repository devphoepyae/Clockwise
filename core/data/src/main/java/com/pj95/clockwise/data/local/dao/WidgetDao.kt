package com.pj95.clockwise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pj95.clockwise.data.local.entity.WidgetConfigEntity
import com.pj95.clockwise.data.local.entity.WidgetLicenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Read/write access to Chronos Studio's local licence and widget-instance state.
 *
 * The tables are tiny, so every read is unrestricted and the gallery observes
 * the whole licence table as one flow. Writes are deliberately narrow: a trial
 * can only be started once, and a purchase unlocks every premium style in one
 * statement.
 */
@Dao
interface WidgetDao {

    @Query("SELECT * FROM widget_licenses ORDER BY widgetId ASC")
    fun observeLicenses(): Flow<List<WidgetLicenseEntity>>

    @Query("SELECT * FROM widget_licenses WHERE widgetId = :widgetId LIMIT 1")
    suspend fun getLicense(widgetId: String): WidgetLicenseEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLicenses(licenses: List<WidgetLicenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLicense(license: WidgetLicenseEntity)

    /**
     * Starts the trial only if it has never been started.
     *
     * The `IS NULL` guard is what makes the write idempotent: a double tap, a
     * recomposition or a second device cannot restart the same three days.
     */
    @Query(
        "UPDATE widget_licenses SET trialStartTimestamp = :timestamp " +
            "WHERE widgetId = :widgetId AND trialStartTimestamp IS NULL",
    )
    suspend fun startTrial(widgetId: String, timestamp: Long): Int

    /** A verified purchase unlocks every non-free style. */
    @Query("UPDATE widget_licenses SET isUnlocked = 1 WHERE category != 'FREE'")
    suspend fun unlockAllPremium()

    @Query("SELECT * FROM widget_configs ORDER BY appWidgetId ASC")
    fun observeConfigs(): Flow<List<WidgetConfigEntity>>

    @Query("SELECT * FROM widget_configs WHERE appWidgetId = :appWidgetId LIMIT 1")
    suspend fun getConfig(appWidgetId: Int): WidgetConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: WidgetConfigEntity)

    @Query("DELETE FROM widget_configs WHERE appWidgetId = :appWidgetId")
    suspend fun deleteConfig(appWidgetId: Int)
}
