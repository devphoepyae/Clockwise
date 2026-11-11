package com.pj95.clockwise.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pj95.clockwise.data.local.dao.AlarmDao
import com.pj95.clockwise.data.local.dao.CityDao
import com.pj95.clockwise.data.local.dao.SavedCityDao
import com.pj95.clockwise.data.local.dao.SolarDao
import com.pj95.clockwise.data.local.dao.TimerDao
import com.pj95.clockwise.data.local.dao.WidgetDao
import com.pj95.clockwise.data.local.entity.AlarmEntity
import com.pj95.clockwise.data.local.entity.CityEntity
import com.pj95.clockwise.data.local.entity.SavedCityEntity
import com.pj95.clockwise.data.local.entity.SolarDayEntity
import com.pj95.clockwise.data.local.entity.TimerEntity
import com.pj95.clockwise.data.local.entity.WidgetConfigEntity
import com.pj95.clockwise.data.local.entity.WidgetLicenseEntity

/**
 * The app's local relational database.
 *
 * Version 1 held the city catalog. Version 2 adds the solar-day cache. Version
 * 3 adds the saved-city order used by drag-and-drop. Version 4 adds the user's
 * alarms. Version 5 adds the per-alarm sound. Version 6 adds the single stored
 * countdown timer. Version 7 adds the per-alarm snooze policy and its cycle
 * counter. Version 8 adds the user's chosen alarm order. Version 9 adds the
 * Chronos Studio licence and widget-instance tables, plus the optional city
 * landmark used by the detail screen's ambient watermark. Version 10 adds the
 * one-off scheduled date and the per-alarm vibration choice. Every migration is
 * explicit and reviewable because destructive fallback would throw away the
 * user's catalog, the order they arranged it in and the alarms they rely on to
 * wake up.
 */
@Database(
    entities = [
        CityEntity::class,
        SolarDayEntity::class,
        SavedCityEntity::class,
        AlarmEntity::class,
        TimerEntity::class,
        WidgetLicenseEntity::class,
        WidgetConfigEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
abstract class ClockwiseDatabase : RoomDatabase() {

    abstract fun cityDao(): CityDao

    abstract fun solarDao(): SolarDao

    abstract fun savedCityDao(): SavedCityDao

    abstract fun alarmDao(): AlarmDao

    abstract fun timerDao(): TimerDao

    abstract fun widgetDao(): WidgetDao

    companion object {
        /** File name chosen once; renaming it would look like data loss. */
        const val NAME = "clockwise.db"

        /** Adds the solar-day cache table without touching the city catalog. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `solar_days` (
                        `cityId` TEXT NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `kind` TEXT NOT NULL,
                        `sunriseEpochMilli` INTEGER,
                        `sunsetEpochMilli` INTEGER,
                        `solarNoonEpochMilli` INTEGER NOT NULL,
                        `dayLengthSeconds` INTEGER NOT NULL,
                        `calculatedAtEpochMilli` INTEGER NOT NULL,
                        PRIMARY KEY(`cityId`, `dateEpochDay`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_solar_days_dateEpochDay` " +
                        "ON `solar_days` (`dateEpochDay`)",
                )
            }
        }

        /**
         * Adds the saved-city order table.
         *
         * The table starts empty on purpose. `ClockRepository` seeds it from
         * the saved-city list the existing Proto DataStore already holds, so an
         * upgrade keeps both the membership and the order the user had.
         */
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_cities` (
                        `cityId` TEXT NOT NULL,
                        `position` INTEGER NOT NULL,
                        PRIMARY KEY(`cityId`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_saved_cities_position` " +
                        "ON `saved_cities` (`position`)",
                )
            }
        }

        /**
         * Adds the alarms table.
         *
         * The table starts empty: a fresh install has no alarms, and an upgrade
         * simply gains the new tab with nothing in it. No rows are derived from
         * anything else, so there is nothing to backfill.
         */
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `alarms` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `hour` INTEGER NOT NULL,
                        `minute` INTEGER NOT NULL,
                        `label` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `repeatDays` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Adds the per-alarm sound.
         *
         * Every existing alarm keeps the device's own alarm sound, which is what
         * it has been ringing with all along, so the column defaults to the empty
         * string that means exactly that.
         */
        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `alarms` ADD COLUMN `soundUri` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        /**
         * Adds the single timer row's table.
         *
         * It starts empty: an upgrade simply gains a timer tab that is ready to
         * start, and no half-counted timer is invented from the upgrade moment.
         */
        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `timer` (
                        `id` INTEGER NOT NULL,
                        `durationMillis` INTEGER NOT NULL,
                        `remainingMillis` INTEGER NOT NULL,
                        `endAtEpochMillis` INTEGER,
                        `running` INTEGER NOT NULL,
                        `label` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Adds the snooze policy and its per-cycle counter.
         *
         * Every existing alarm becomes snoozable without limit, which is exactly
         * how it behaved in v4.0.0. Nobody's morning routine changes because of
         * an upgrade, and the counter starts at zero because no ring is in
         * progress at the moment the migration runs.
         */
        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `alarms` ADD COLUMN `snoozePolicy` INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE `alarms` ADD COLUMN `snoozesUsed` INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE `alarms` ADD COLUMN `snoozedAtEpochMillis` INTEGER",
                )
            }
        }

        /**
         * Adds the user's chosen alarm order.
         *
         * Before v4.1.0 the list was sorted by time of day, so that is the
         * position every existing alarm receives. The correlated count assigns
         * 0, 1, 2 … in exactly the order the user saw, which means the upgrade
         * itself is invisible and the first drag starts from the familiar list.
         */
        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `alarms` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    """
                    UPDATE `alarms` SET `sortOrder` = (
                        SELECT COUNT(*) FROM `alarms` AS `earlier`
                        WHERE `earlier`.`hour` < `alarms`.`hour`
                           OR (`earlier`.`hour` = `alarms`.`hour`
                               AND `earlier`.`minute` < `alarms`.`minute`)
                           OR (`earlier`.`hour` = `alarms`.`hour`
                               AND `earlier`.`minute` = `alarms`.`minute`
                               AND `earlier`.`id` < `alarms`.`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Adds Chronos Studio's local state and the city landmark column.
         *
         * The licence table starts empty and is seeded idempotently by
         * `WidgetLicenseRepository`; doing it there rather than in SQL keeps the
         * style catalogue in one place and means a later style can be added
         * without another migration. The city column is nullable, so every
         * existing row keeps working and simply falls back to the ambient
         * pattern until the seeder writes its built-in landmark.
         */
        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `cities` ADD COLUMN `landmarkType` TEXT",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `widget_licenses` (
                        `widgetId` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `isUnlocked` INTEGER NOT NULL,
                        `trialStartTimestamp` INTEGER,
                        PRIMARY KEY(`widgetId`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `widget_configs` (
                        `appWidgetId` INTEGER NOT NULL,
                        `widgetType` TEXT NOT NULL,
                        `city1` TEXT NOT NULL,
                        `city2` TEXT NOT NULL,
                        `city3` TEXT NOT NULL,
                        `isPremium` INTEGER NOT NULL,
                        PRIMARY KEY(`appWidgetId`)
                    )
                    """.trimIndent(),
                )
            }
        }

        /**
         * Adds the one-off scheduled date and the vibration switch.
         *
         * Every existing alarm keeps ringing exactly as before: the date is
         * null, so the repeat rule decides, and vibration defaults to on, which
         * is what the ringing service has always done.
         */
        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `alarms` ADD COLUMN `scheduledDateEpochDay` INTEGER",
                )
                db.execSQL(
                    "ALTER TABLE `alarms` ADD COLUMN `vibrate` INTEGER NOT NULL DEFAULT 1",
                )
            }
        }
    }
}
