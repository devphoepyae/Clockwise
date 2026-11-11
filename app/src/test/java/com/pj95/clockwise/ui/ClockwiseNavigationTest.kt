package com.pj95.clockwise.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.data.repository.ClockRepository
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.testing.FakeAlarmRepository
import com.pj95.clockwise.testing.FakeSavedCitiesDataStore
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.ZoneId

/**
 * Navigation and back behaviour across the app shell: the bottom tabs and the
 * return trip from the city detail screen — including the phase 2 destinations
 * that hang off the Clocks screen: the meeting planner and the settings sheet.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class ClockwiseNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun renderShell(alarms: List<Alarm> = emptyList()) {
        val viewModel = ClockwiseViewModel(
            repository = ClockRepository(FakeSavedCitiesDataStore()),
            ticker = flowOf(PreviewSamples.referenceInstant),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )
        composeRule.setContent {
            FixedClockwiseTheme {
                ClockwiseRoot(
                    viewModel = viewModel,
                    alarmViewModel = AlarmViewModel(FakeAlarmRepository(alarms)),
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Los Angeles").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** The shell with nothing saved, for the planner's own empty state. */
    private fun renderEmptyShell() {
        val repository = ClockRepository(FakeSavedCitiesDataStore())
        runBlocking {
            CityDirectory.defaultSavedCityIds.forEach { repository.removeCity(it) }
        }
        val viewModel = ClockwiseViewModel(
            repository = repository,
            ticker = flowOf(PreviewSamples.referenceInstant),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )
        composeRule.setContent {
            FixedClockwiseTheme {
                ClockwiseRoot(
                    viewModel = viewModel,
                    alarmViewModel = AlarmViewModel(FakeAlarmRepository()),
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("No cities yet").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * The phase 2 planner is part of the main flow: it opens from the list and
     * the back action returns to it rather than to the app's start destination.
     */
    @Test
    fun theClocksScreenOpensTheMeetingPlannerAndBackReturnsToTheList() {
        renderShell()

        composeRule.onNodeWithText("PLAN A MEETING").performClick()
        composeRule.onNodeWithText("YOUR CITIES AT THIS TIME").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("CITIES").assertIsDisplayed()
    }

    /** With nothing saved the planner hands the user back to the city search. */
    @Test
    fun thePlannerWithNothingSavedOffersTheCitySearch() {
        renderEmptyShell()

        composeRule.onNodeWithText("PLAN A MEETING").performClick()
        composeRule.onNodeWithText("No cities to plan for").assertIsDisplayed()

        composeRule.onNodeWithText("Find a city").performClick()

        composeRule.onNodeWithText("Add a city").assertIsDisplayed()
    }

    /**
     * The settings sheet hangs off the Clocks screen's gear action, and reports
     * which theme mode is in force.
     */
    @Test
    fun theSettingsActionOpensTheSettingsSheet() {
        renderShell()

        composeRule.onNodeWithContentDescription("Settings").performClick()

        composeRule.onNodeWithText("THEME MODE").assertIsDisplayed()
        composeRule.onNodeWithText("System").assertIsSelected()
    }

    /**
     * A swipe is committed immediately, and the shared snackbar is the safety
     * net: Undo puts the same city back in the same place rather than asking
     * for confirmation before the gesture is allowed to work.
     */
    @Test
    fun swipingACityAwayOffersUndoAndRestoresIt() {
        renderShell()

        composeRule.onNodeWithText("Tokyo").performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("City removed").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("UNDO").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Tokyo").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Tokyo").assertIsDisplayed()
        composeRule.onNodeWithText("City removed").assertDoesNotExist()
    }

    @Test
    fun swipingAnAlarmAwayOffersUndoAndRestoresIt() {
        renderShell(alarms = PreviewSamples.alarmState().alarms)

        composeRule.onNodeWithContentDescription("Alarms").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("06:30").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("06:30").performTouchInput { swipeLeft() }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Alarm deleted").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("UNDO").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("06:30").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("06:30").assertIsDisplayed()
    }

    /**
     * The shell has exactly two destinations: alarms and clocks. The world map
     * that once sat between them is gone, so it is not offered anywhere.
     */
    @Test
    fun theBottomBarOffersOnlyAlarmsAndClocks() {
        renderShell()

        composeRule.onNodeWithContentDescription("Alarms").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clocks").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("World map").assertDoesNotExist()
    }

    @Test
    fun theClocksTabReturnsToTheCityList() {
        renderShell()

        composeRule.onNodeWithContentDescription("Alarms").performClick()
        composeRule.onNodeWithContentDescription("Clocks").performClick()

        composeRule.onNodeWithText("CITIES").assertIsDisplayed()
    }

    @Test
    fun aCityFromTheListOpensTheDetailScreenAndBackReturnsToTheList() {
        renderShell()

        composeRule.onNodeWithText("Tokyo").performClick()
        composeRule.onNodeWithText("YOUR LOCAL TIME").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.onNodeWithText("CITIES").assertIsDisplayed()
    }

    @Test
    fun theAddRowOpensTheCitySearchFromTheClocksList() {
        renderShell()

        // v1.0.1 moved the header's action to the gallery, so adding a city
        // starts from the list's own ADD CITY row.
        composeRule.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText("ADD CITY"))
        composeRule.onNodeWithText("ADD CITY").performClick()

        composeRule.onNodeWithText("Add a city").assertIsDisplayed()
    }

    /**
     * The Clocks header's widget icon is the one tap into Chronos. The gallery
     * must open from the list, not from a settings row.
     */
    @Test
    fun theHeaderWidgetActionOpensTheChronosGallery() {
        renderShell()

        composeRule.onNodeWithContentDescription("Widget gallery").performClick()

        composeRule.onNodeWithText("Chronos gallery").assertIsDisplayed()
    }

    /**
     * The left-hand tab is the alarm list now. It opens from the bar, keeps the
     * shell's header language, and shows the stored alarms.
     */
    @Test
    fun theAlarmTabOpensTheAlarmList() {
        renderShell(alarms = PreviewSamples.alarmState().alarms)

        composeRule.onNodeWithContentDescription("Alarms").performClick()

        composeRule.onNodeWithText("ALWAYS ON TIME").assertIsDisplayed()
        composeRule.onNodeWithText("06:30").assertIsDisplayed()
        composeRule.onNodeWithText("Morning run", substring = true).assertIsDisplayed()

        // The Clocks destination is disposed once the alarm tab is on screen.
        composeRule.onNodeWithText("CITIES").assertDoesNotExist()
    }

    /** Tapping an alarm card opens the same editor that creates a new one. */
    @Test
    fun anAlarmCardOpensTheEditor() {
        renderShell(alarms = PreviewSamples.alarmState().alarms)

        composeRule.onNodeWithContentDescription("Alarms").performClick()
        composeRule.onNodeWithText("06:30").performClick()

        composeRule.onNodeWithText("Edit alarm").assertIsDisplayed()
        composeRule.onNodeWithText("SAVE ALARM").performScrollTo().assertIsDisplayed()
    }

    /** The add action opens a blank editor rather than an existing alarm. */
    @Test
    fun theAddAlarmActionOpensABlankEditor() {
        renderShell()

        composeRule.onNodeWithContentDescription("Alarms").performClick()
        composeRule.onNodeWithText("No alarms yet").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add alarm").performClick()

        composeRule.onNodeWithText("New alarm").assertIsDisplayed()
    }
}
