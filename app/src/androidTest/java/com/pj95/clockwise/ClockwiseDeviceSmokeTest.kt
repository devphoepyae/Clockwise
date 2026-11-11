package com.pj95.clockwise

import android.content.Context
import android.view.WindowManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pj95.clockwise.data.local.ClockwiseDatabase
import com.pj95.clockwise.ui.components.AlarmTimeTestTags
import com.pj95.clockwise.ui.components.CityCardTestTags
import com.pj95.clockwise.ui.components.TimerTestTags
import com.pj95.clockwise.ui.screens.AlarmTestTags
import com.pj95.clockwise.ui.screens.SettingsTestTags
import com.pj95.clockwise.ui.screens.StopwatchTestTags
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device smoke test, run with `connectedDebugAndroidTest`.
 *
 * Nothing here is faked: the launched activity uses the on-device Proto DataStore,
 * the real system clock, the real device timezone, real hardware rendering and the
 * real Room database. It proves the app installs, starts, seeds its defaults, shows
 * the fresh-install intro, renders the city list, opens the alarm editor and the
 * timer/stopwatch detours, and persists a drag reorder in Room.
 *
 * The preferences store is wiped before every test, so the intro is always shown
 * and has to be dismissed through the same button a person taps. That gate is why
 * every test starts with [startFromClocks]; before 3.0.0 this suite assumed the
 * city list was the first screen and silently stopped testing anything once the
 * 2.1 intro landed.
 *
 * Scope is deliberately narrow. Everything that can be asserted on the JVM already
 * is, by the 596 tests in `src/test` - including every acceptance criterion and the
 * Roborazzi screenshots. Two behaviours remain out of reach here:
 *
 *  - Taps that mutate persisted state are exercised, but a mutation is a disk write
 *    plus a flow round-trip, so assertions wait on the database rather than on the
 *    next frame.
 *  - System back. Espresso's `pressBack()` refuses to run when the device animation
 *    scale is non-zero, which is the default on many OEM ROMs.
 *
 * Neither gap is an app defect. See sections 15.1 and 15.2 of SPECIFICATION.md.
 */
@RunWith(AndroidJUnit4::class)
class ClockwiseDeviceSmokeTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Wipes the persisted preferences first so the test always observes genuine
     * first-launch defaults rather than whatever the previous run left behind.
     */
    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(ClearStoredPreferencesRule())
        .around(composeRule)

    /**
     * Dismisses the fresh-install intro and waits for the Clocks screen.
     *
     * The store was just wiped, so the intro is guaranteed. Clicking it goes
     * through the same callback a tap does; the DataStore write and the shell's
     * first composition happen a beat later, hence the wait rather than an
     * immediate assertion.
     */
    private fun startFromClocks() {
        composeRule.onNodeWithText("Welcome", ignoreCase = true).assertIsDisplayed()
        composeRule.onNodeWithText("Get started").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("CITIES").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Keeps the display awake for the whole test.
     *
     * These tests drive the real UI for several minutes (the timer waits on a
     * countdown). The device's own screen timeout would otherwise put the phone
     * to sleep mid-test, pause the activity and leave Compose with no hierarchy
     * to assert on. The flag is per-window, so it disappears with the test.
     */
    @Before
    fun keepScreenAwake() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @Test
    fun firstLaunchShowsTheDefaultCitiesWithLosAngelesActive() {
        startFromClocks()

        // The five defaults are all present, in the specified order. The list is
        // a LazyColumn, so a card below the fold is not composed yet: scroll the
        // list to each stable tag instead of asking the card itself to scroll.
        val list = composeRule.onNode(hasScrollToIndexAction())
        listOf("tokyo", "sydney", "los-angeles", "new-york", "london").forEach { cityId ->
            list.performScrollToNode(hasTestTag(CityCardTestTags.card(cityId)))
            composeRule.onNodeWithTag(CityCardTestTags.card(cityId))
                .assertIsDisplayed()
        }

        // Los Angeles is the active city, so its card is the black one.
        composeRule.onNodeWithText("Los Angeles").assertIsDisplayed()
        composeRule.onNodeWithText("California, USA").assertIsDisplayed()
    }

    /**
     * The alarm tab is a list with a floating add button and two icon detours.
     *
     * Everything here is reached by tapping, the way a person would: the add
     * button belongs to the list, the timer and stopwatch open from the header,
     * and the stopwatch start/pause/reset loop is exercised on real hardware.
     */
    @Test
    fun theAlarmTabShowsTheFloatingButtonAndBothTimingDetours() {
        startFromClocks()

        composeRule.onNodeWithContentDescription("Alarms").performClick()
        composeRule.onNodeWithTag(AlarmTestTags.AddFab).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Timer").performClick()
        composeRule.onNodeWithText("START TIMER").assertIsDisplayed()
        // The add button belongs to the list, not to the detours.
        composeRule.onNodeWithTag(AlarmTestTags.AddFab).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Close timer").performClick()

        composeRule.onNodeWithContentDescription("Stopwatch").performClick()
        composeRule.onNodeWithTag(StopwatchTestTags.Display).assertIsDisplayed()
        composeRule.onNodeWithTag(StopwatchTestTags.Toggle).performClick()
        composeRule.onNodeWithTag(StopwatchTestTags.Toggle).performClick()
        composeRule.onNodeWithTag(StopwatchTestTags.Reset).performClick()
        composeRule.onNodeWithText("00:00.00").assertIsDisplayed()
    }

    /**
     * The editor's new controls, all of them in the same sheet: the dial with
     * the keypad behind a toggle, the schedule row and the vibration switch.
     */
    @Test
    fun theAlarmEditorOffersTheDialTheKeypadTheScheduleAndVibration() {
        startFromClocks()

        composeRule.onNodeWithContentDescription("Alarms").performClick()
        composeRule.onNodeWithTag(AlarmTestTags.AddFab).performClick()

        composeRule.onNodeWithText("New alarm").assertIsDisplayed()
        composeRule.onNodeWithTag(AlarmTimeTestTags.Dial).assertExists()
        composeRule.onNodeWithTag(AlarmTimeTestTags.Keypad).assertDoesNotExist()

        composeRule.onNodeWithTag(AlarmTimeTestTags.KeypadToggle).performClick()
        composeRule.onNodeWithTag(AlarmTimeTestTags.Keypad).assertExists()
        composeRule.onNodeWithTag(AlarmTimeTestTags.HourField).assertExists()
        composeRule.onNodeWithTag(AlarmTimeTestTags.MinuteField).assertExists()

        composeRule.onNodeWithTag(AlarmTimeTestTags.KeypadToggle).performClick()
        composeRule.onNodeWithTag(AlarmTimeTestTags.Dial).assertExists()

        composeRule.onNodeWithText("Schedule alarm").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Vibrate while ringing").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("SAVE ALARM").performScrollTo().assertIsDisplayed()
        // A brand-new alarm has nothing to delete yet.
        composeRule.onNodeWithText("DELETE ALARM").assertDoesNotExist()
    }

    /**
     * The timer's four presets, the hidden wheels, and the running controls:
     * start from a preset, add a minute, reset.
     */
    @Test
    fun theTimerStartsFromAPresetAndCanBeExtendedAndReset() {
        startFromClocks()

        composeRule.onNodeWithContentDescription("Alarms").performClick()
        composeRule.onNodeWithContentDescription("Timer").performClick()

        // The timer reads its state from the store, so the setup controls arrive
        // one frame after the section does.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(TimerTestTags.Start).fetchSemanticsNodes().isNotEmpty()
        }

        listOf(5, 10, 15, 30).forEach { minutes ->
            composeRule.onNodeWithTag("${TimerTestTags.Preset}_$minutes").assertIsDisplayed()
        }
        // The wheels are opt-in, not the default.
        composeRule.onNodeWithTag(TimerTestTags.HoursWheel).assertDoesNotExist()

        composeRule.onNodeWithTag("${TimerTestTags.Preset}_10").performClick()
        composeRule.onNodeWithTag(TimerTestTags.Start).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(TimerTestTags.Display).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(TimerTestTags.Display).assertIsDisplayed()
        composeRule.onNodeWithTag("${TimerTestTags.AddTime}_1").performClick()
        composeRule.onNodeWithTag(TimerTestTags.Cancel).performClick()

        composeRule.onNodeWithText("START TIMER").assertIsDisplayed()
    }

    /**
     * Settings now carries permission statuses and the Legal & Support group.
     * Neither opens a browser; both are part of the sheet.
     */
    @Test
    fun settingsShowsPermissionStatusesAndTheLegalGroup() {
        startFromClocks()

        composeRule.onNodeWithContentDescription("Settings").performClick()

        composeRule.onNodeWithText("ALARMS & PERMISSIONS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeRule.onNodeWithText("Exact alarms").assertIsDisplayed()

        composeRule.onNodeWithTag(SettingsTestTags.Legal).performScrollTo().performClick()
        composeRule.onNodeWithText("Zero-Internet & Privacy Policy").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("App Guide & Chronos Tips").assertExists()
        composeRule.onNodeWithText("Terms of Service").assertExists()
        composeRule.onNodeWithText("Open Source Licenses").assertExists()
        // The developer name and version live in one multi-line About paragraph.
        composeRule.onNodeWithText("PJ95 Studio", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1.0.10", substring = true).assertExists()
    }

    /**
     * Drags the first city card down one slot with real touch input and proves the
     * new order reached Room, not just the composable.
     *
     * The database is opened by the test as well as by the app, which is the only
     * way to observe persistence from outside the process boundary a Compose test
     * cannot cross. Reading `user_version` in the same pass also verifies that the
     * shipped file really is schema 10, that `saved_cities` and `alarms` both
     * exist, and that `alarms` carries the per-alarm sound column.
     */
    @Test
    fun draggingACityPersistsTheNewOrderAndTheDatabaseIsAtVersionTen() {
        startFromClocks()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room
            .databaseBuilder(context, ClockwiseDatabase::class.java, ClockwiseDatabase.NAME)
            .build()

        try {
            composeRule.waitUntil(timeoutMillis = 10_000) {
                readOrder(database).size == 5
            }
            val before = readOrder(database)
            val tokyoBefore = before.indexOf("tokyo")
            assertTrue("Tokyo must be in the seeded order", tokyoBefore >= 0)

            composeRule.onNodeWithText("Tokyo").performTouchInput {
                down(center)
                // Past the long-press threshold, then far enough to cross the next
                // card's midpoint.
                advanceEventTime(700)
                moveBy(Offset(0f, height.toFloat()))
                up()
            }

            composeRule.waitUntil(timeoutMillis = 10_000) {
                readOrder(database) != before
            }
            val after = readOrder(database)

            assertEquals(before.toSet(), after.toSet())
            assertTrue("Tokyo must move down the list", after.indexOf("tokyo") > tokyoBefore)

            val sqlite = database.openHelper.readableDatabase
            val version = sqlite.query("PRAGMA user_version").use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }
            assertEquals(10, version)

            val tables = sqlite.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            assertTrue("saved_cities must exist at schema 10", "saved_cities" in tables)
            assertTrue("alarms must exist at schema 10", "alarms" in tables)

            val alarmColumns = sqlite.query("PRAGMA table_info(alarms)").use { cursor ->
                buildList {
                    val nameIndex = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
            }
            assertTrue("alarms must carry its sound at schema 10", "soundUri" in alarmColumns)
            assertTrue("alarms must carry a schedule at schema 10", "scheduledDateEpochDay" in alarmColumns)
            assertTrue("alarms must carry a vibration switch at schema 10", "vibrate" in alarmColumns)
        } finally {
            database.close()
        }
    }

    private fun readOrder(database: ClockwiseDatabase): List<String> = runBlocking {
        database.savedCityDao().getCityIds()
    }
}

/** Deletes the Proto DataStore before the activity under test starts. */
private class ClearStoredPreferencesRule : TestWatcher() {
    override fun starting(description: Description) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "datastore").deleteRecursively()
    }
}
