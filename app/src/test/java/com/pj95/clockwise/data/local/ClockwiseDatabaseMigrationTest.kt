package com.pj95.clockwise.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Schema upgrades: the alarms table (3 → 4) and the per-alarm sound (4 → 5).
 *
 * Each step is a path every existing install takes, so the tests create a real
 * file at the older version, migrate it, and let Room validate the result
 * against the exported schema of the newer one.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ClockwiseDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClockwiseDatabase::class.java,
    )

    @Test
    fun migration3To4KeepsSavedCitiesAndCreatesTheAlarmsTable() {
        helper.createDatabase(TEST_DATABASE, 3).apply {
            execSQL("INSERT INTO saved_cities (cityId, position) VALUES ('tokyo', 0)")
            execSQL("INSERT INTO saved_cities (cityId, position) VALUES ('london', 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            ClockwiseDatabase.MIGRATION_3_4,
        )

        try {
            db.query("SELECT cityId FROM saved_cities ORDER BY position").use { cursor ->
                val ids = buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
                assertEquals(listOf("tokyo", "london"), ids)
            }

            // The new table exists and stores a complete row.
            db.execSQL(
                "INSERT INTO alarms (hour, minute, label, enabled, repeatDays) " +
                    "VALUES (6, 30, 'Morning run', 1, 5)",
            )
            db.query(
                "SELECT id, hour, minute, label, enabled, repeatDays FROM alarms",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getLong(0) > 0)
                assertEquals(6, cursor.getInt(1))
                assertEquals(30, cursor.getInt(2))
                assertEquals("Morning run", cursor.getString(3))
                assertEquals(1, cursor.getInt(4))
                assertEquals(5, cursor.getInt(5))
            }

            // The catalog and solar tables are untouched by this migration.
            val tables = db.query(
                "SELECT name FROM sqlite_master WHERE type='table'",
            ).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            assertTrue("cities" in tables)
            assertTrue("solar_days" in tables)
            assertTrue("saved_cities" in tables)
            assertTrue("alarms" in tables)
        } finally {
            db.close()
        }
    }

    @Test
    fun theUpgradedDatabaseIsAtVersionFour() {
        helper.createDatabase(TEST_DATABASE, 3).close()

        val db = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            4,
            true,
            ClockwiseDatabase.MIGRATION_3_4,
        )

        try {
            val version = db.query("PRAGMA user_version").use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
            assertEquals(4, version)
        } finally {
            db.close()
        }
    }

    /**
     * Schema 4 → 5: the sound column is added and every existing alarm keeps
     * ringing with the device's own alarm sound, which is what `""` means.
     */
    @Test
    fun migration4To5GivesEveryAlarmTheDefaultSound() {
        helper.createDatabase(TEST_DATABASE, 4).apply {
            execSQL(
                "INSERT INTO alarms (hour, minute, label, enabled, repeatDays) " +
                    "VALUES (6, 30, 'Morning run', 1, 5)",
            )
            execSQL(
                "INSERT INTO alarms (hour, minute, label, enabled, repeatDays) " +
                    "VALUES (22, 0, 'Wind down', 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            5,
            true,
            ClockwiseDatabase.MIGRATION_4_5,
        )

        try {
            db.query(
                "SELECT hour, minute, label, soundUri FROM alarms ORDER BY hour",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                // The earlier alarm first, with its own values intact...
                assertEquals(6, cursor.getInt(0))
                assertEquals(30, cursor.getInt(1))
                assertEquals("Morning run", cursor.getString(2))
                // ...and the default sound, not a null and not a missing row.
                assertEquals("", cursor.getString(3))
                assertTrue(cursor.moveToNext())
                assertEquals(22, cursor.getInt(0))
                assertEquals("", cursor.getString(3))
            }

            val version = db.query("PRAGMA user_version").use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
            assertEquals(5, version)
        } finally {
            db.close()
        }
    }

    /**
     * Schema 5 → 6: the single timer table is added, and every alarm keeps its
     * sound and schedule. The timer starts empty rather than half-counted.
     */
    @Test
    fun migration5To6AddsAnEmptyTimerTable() {
        helper.createDatabase(TEST_DATABASE, 5).apply {
            execSQL(
                "INSERT INTO alarms (hour, minute, label, enabled, repeatDays, soundUri) " +
                    "VALUES (6, 30, 'Morning run', 1, 5, 'content://tones/bright')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            ClockwiseDatabase.MIGRATION_5_6,
        )

        try {
            db.query("SELECT COUNT(*) FROM timer").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }

            db.execSQL(
                "INSERT INTO timer (id, durationMillis, remainingMillis, endAtEpochMillis, running, label) " +
                    "VALUES (1, 300000, 120000, NULL, 0, 'Tea')",
            )
            db.query(
                "SELECT durationMillis, remainingMillis, endAtEpochMillis, running, label FROM timer",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(300_000, cursor.getLong(0))
                assertEquals(120_000, cursor.getLong(1))
                assertTrue(cursor.isNull(2))
                assertEquals(0, cursor.getInt(3))
                assertEquals("Tea", cursor.getString(4))
            }

            // The alarm and its chosen sound survived the upgrade.
            db.query("SELECT label, soundUri FROM alarms").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Morning run", cursor.getString(0))
                assertEquals("content://tones/bright", cursor.getString(1))
            }

            val version = db.query("PRAGMA user_version").use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
            assertEquals(6, version)
        } finally {
            db.close()
        }
    }

    /**
     * Schema 6 → 7: the snooze policy and its per-cycle counter. Every existing
     * alarm keeps unlimited snoozing, which is how v4.0.0 behaved, and no ring is
     * in progress when the upgrade runs.
     */
    @Test
    fun migration6To7AddsTheSnoozeColumnsWithPermissiveDefaults() {
        helper.createDatabase(TEST_DATABASE, 6).apply {
            execSQL(
                "INSERT INTO alarms (hour, minute, label, enabled, repeatDays, soundUri) " +
                    "VALUES (6, 30, 'Morning run', 1, 5, 'content://tones/bright')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            7,
            true,
            ClockwiseDatabase.MIGRATION_6_7,
        )

        try {
            db.query(
                "SELECT label, snoozePolicy, snoozesUsed, snoozedAtEpochMillis FROM alarms",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Morning run", cursor.getString(0))
                // 0 is SnoozePolicy.ALLOWED: nobody's morning routine changes.
                assertEquals(0, cursor.getInt(1))
                assertEquals(0, cursor.getInt(2))
                assertTrue(cursor.isNull(3))
            }

            val version = db.query("PRAGMA user_version").use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
            assertEquals(7, version)
        } finally {
            db.close()
        }
    }

    /**
     * Schema 7 → 8: the alarm order column is added and backfilled by time of
     * day, so the list an existing user opens after the upgrade is the list they
     * left, even though it is now stored as a position rather than a sort rule.
     */
    @Test
    fun migration7To8BackfillsTheOrderByTimeOfDay() {
        helper.createDatabase(TEST_DATABASE, 7).apply {
            execSQL(
                "INSERT INTO alarms " +
                    "(id, hour, minute, label, enabled, repeatDays, soundUri, snoozePolicy, snoozesUsed) " +
                    "VALUES (3, 18, 45, 'Evening', 1, 0, '', 0, 0)",
            )
            execSQL(
                "INSERT INTO alarms " +
                    "(id, hour, minute, label, enabled, repeatDays, soundUri, snoozePolicy, snoozesUsed) " +
                    "VALUES (1, 6, 30, 'Morning run', 1, 5, 'content://tones/bright', 1, 2)",
            )
            execSQL(
                "INSERT INTO alarms " +
                    "(id, hour, minute, label, enabled, repeatDays, soundUri, snoozePolicy, snoozesUsed) " +
                    "VALUES (2, 7, 0, 'Stand-up', 1, 0, '', 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            8,
            true,
            ClockwiseDatabase.MIGRATION_7_8,
        )

        try {
            db.query("SELECT id, sortOrder, label FROM alarms ORDER BY sortOrder")
                .use { cursor ->
                    val rows = buildList {
                        while (cursor.moveToNext()) {
                            add(Triple(cursor.getLong(0), cursor.getInt(1), cursor.getString(2)))
                        }
                    }
                    assertEquals(
                        listOf(
                            Triple(1L, 0, "Morning run"),
                            Triple(2L, 1, "Stand-up"),
                            Triple(3L, 2, "Evening"),
                        ),
                        rows,
                    )
                }

            // The snooze state written by schema 7 survives the upgrade.
            db.query("SELECT snoozePolicy, snoozesUsed FROM alarms WHERE id = 1")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(1, cursor.getInt(0))
                    assertEquals(2, cursor.getInt(1))
                }

            val version = db.query("PRAGMA user_version").use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
            assertEquals(8, version)
        } finally {
            db.close()
        }
    }

    /**
     * Schema 8 → 9: Chronos Studio arrives. The city catalogue keeps every row
     * and gains a nullable landmark, the two widget tables start empty, and the
     * alarms the user relies on are untouched.
     */
    @Test
    fun migration8To9AddsStudioTablesAndKeepsTheCatalogue() {
        helper.createDatabase(TEST_DATABASE, 8).apply {
            execSQL(
                "INSERT INTO cities " +
                    "(id, nameEn, regionEn, countryEn, timeZone, latitude, longitude, isBuiltIn, sortOrder) " +
                    "VALUES ('tokyo', 'Tokyo', 'Tokyo', 'Japan', 'Asia/Tokyo', 35.6762, 139.6503, 1, 0)",
            )
            execSQL(
                "INSERT INTO saved_cities (cityId, position) VALUES ('tokyo', 0)",
            )
            execSQL(
                "INSERT INTO alarms " +
                    "(id, hour, minute, label, enabled, repeatDays, soundUri, snoozePolicy, snoozesUsed, sortOrder) " +
                    "VALUES (1, 6, 30, 'Morning run', 1, 5, '', 0, 0, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            9,
            true,
            ClockwiseDatabase.MIGRATION_8_9,
        )

        try {
            // The catalogue survives, and the new landmark starts as NULL so the
            // ambient fallback is used until the seeder writes a real one.
            db.query(
                "SELECT nameEn, timeZone, landmarkType FROM cities WHERE id = 'tokyo'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Tokyo", cursor.getString(0))
                assertEquals("Asia/Tokyo", cursor.getString(1))
                assertTrue(cursor.isNull(2))
            }

            // Both Studio tables exist and accept a complete row.
            db.execSQL(
                "INSERT INTO widget_licenses " +
                    "(widgetId, category, isUnlocked, trialStartTimestamp) " +
                    "VALUES ('ambient_grid', 'PREMIUM', 0, 1700000000000)",
            )
            db.query(
                "SELECT category, isUnlocked, trialStartTimestamp FROM widget_licenses",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("PREMIUM", cursor.getString(0))
                assertEquals(0, cursor.getInt(1))
                assertEquals(1_700_000_000_000L, cursor.getLong(2))
            }

            db.execSQL(
                "INSERT INTO widget_configs " +
                    "(appWidgetId, widgetType, city1, city2, city3, isPremium) " +
                    "VALUES (42, 'GLOBAL_CLOCKS', 'tokyo', 'london', '', 1)",
            )
            db.query(
                "SELECT widgetType, city1, city2, city3, isPremium FROM widget_configs",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("GLOBAL_CLOCKS", cursor.getString(0))
                assertEquals("tokyo", cursor.getString(1))
                assertEquals("london", cursor.getString(2))
                assertEquals("", cursor.getString(3))
                assertEquals(1, cursor.getInt(4))
            }

            // The alarm, its sound and its order are exactly as they were.
            db.query("SELECT label, soundUri, sortOrder FROM alarms WHERE id = 1")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("Morning run", cursor.getString(0))
                    assertEquals("", cursor.getString(1))
                    assertEquals(0, cursor.getInt(2))
                }

            val version = db.query("PRAGMA user_version").use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
            assertEquals(9, version)
        } finally {
            db.close()
        }
    }

    /**
     * v9 → v10 adds the one-off scheduled date and the vibration switch.
     *
     * Existing alarms keep their behaviour: no date (so the repeat rule still
     * decides) and vibration on, which is what the ringing service did before
     * the switch existed.
     */
    @Test
    fun migration9To10AddsScheduleAndVibrationWithoutChangingExistingAlarms() {
        helper.createDatabase(TEST_DATABASE, 9).apply {
            execSQL(
                "INSERT INTO alarms " +
                    "(id, hour, minute, label, enabled, repeatDays, soundUri, snoozePolicy, snoozesUsed, sortOrder) " +
                    "VALUES (1, 6, 30, 'Morning run', 1, 5, 'content://tones/bright', 1, 2, 0)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            10,
            true,
            ClockwiseDatabase.MIGRATION_9_10,
        )

        try {
            db.query(
                "SELECT label, repeatDays, scheduledDateEpochDay, vibrate, soundUri " +
                    "FROM alarms WHERE id = 1",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Morning run", cursor.getString(0))
                assertEquals(5, cursor.getInt(1))
                assertTrue("an existing alarm has no pinned date", cursor.isNull(2))
                assertEquals("existing alarms keep vibrating", 1, cursor.getInt(3))
                assertEquals("content://tones/bright", cursor.getString(4))
            }

            // The new columns accept a scheduled, non-vibrating alarm.
            db.execSQL(
                "UPDATE alarms SET scheduledDateEpochDay = 20733, vibrate = 0 WHERE id = 1",
            )
            db.query("SELECT scheduledDateEpochDay, vibrate FROM alarms WHERE id = 1")
                .use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(20733L, cursor.getLong(0))
                    assertEquals(0, cursor.getInt(1))
                }

            val version = db.query("PRAGMA user_version").use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
            assertEquals(10, version)
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "clockwise-migration-test.db"
    }
}
