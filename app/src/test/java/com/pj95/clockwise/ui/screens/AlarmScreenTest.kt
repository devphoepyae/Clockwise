package com.pj95.clockwise.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.model.alarm.RepeatDays
import com.pj95.clockwise.ui.AlarmUiState
import com.pj95.clockwise.ui.TimerUiState
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The alarm tab's contract: one card per alarm, the switch is the only control
 * that writes without opening the editor, and an empty list explains what to do
 * next rather than showing a blank screen.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class AlarmScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        state: AlarmUiState = PreviewSamples.alarmState(),
        use24Hour: Boolean = true,
        onAddClick: () -> Unit = {},
        onAlarmClick: (Alarm) -> Unit = {},
        onToggleAlarm: (Long, Boolean) -> Unit = { _, _ -> },
        onAllowExactAlarms: () -> Unit = {},
        onDeleteAlarm: (Alarm) -> Unit = {},
        onReorderAlarms: (List<Long>) -> Unit = {},
    ) {
        composeRule.setContent {
            FixedClockwiseTheme {
                AlarmScreen(
                    state = state,
                    use24Hour = use24Hour,
                    onAddClick = onAddClick,
                    onAlarmClick = onAlarmClick,
                    onToggleAlarm = onToggleAlarm,
                    onAllowExactAlarms = onAllowExactAlarms,
                    onDeleteAlarm = onDeleteAlarm,
                    onReorderAlarms = onReorderAlarms,
                )
            }
        }
    }

    @Test
    fun everyStoredAlarmRendersItsTimeLabelAndRepeatSummary() {
        render()

        // The list is the screen: there is no count header and no bottom add bar
        // any more, only the floating action button.
        composeRule.onNodeWithTag(AlarmTestTags.AddFab).assertIsDisplayed()
        composeRule.onNodeWithTag(AlarmTestTags.AddRow).assertDoesNotExist()

        composeRule.onNodeWithText("06:30").assertIsDisplayed()
        composeRule.onNodeWithText("Morning run", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Weekdays", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("07:00").assertIsDisplayed()
        composeRule.onNodeWithText("Every day", substring = true).assertIsDisplayed()

        composeRule.onNodeWithText("18:45").assertIsDisplayed()
        composeRule.onNodeWithText("Once", substring = true).assertIsDisplayed()
    }

    @Test
    fun theSwitchReportsTheAlarmsCurrentState() {
        render()

        composeRule.onNodeWithContentDescription("06:30 alarm").assertIsOn()
        composeRule.onNodeWithContentDescription("18:45 alarm").assertIsOff()
    }

    @Test
    fun tappingASwitchReportsTheNewValueWithoutOpeningTheEditor() {
        var toggled: Pair<Long, Boolean>? = null
        var opened: Alarm? = null
        render(
            onAlarmClick = { opened = it },
            onToggleAlarm = { id, enabled -> toggled = id to enabled },
        )

        composeRule.onNodeWithContentDescription("06:30 alarm").performClick()

        assertEquals(1L to false, toggled)
        assertNull(opened)
    }

    @Test
    fun tappingTheCardOpensThatAlarm() {
        var opened: Alarm? = null
        render(onAlarmClick = { opened = it })

        composeRule.onNodeWithText("06:30").performClick()

        assertEquals(1L, opened?.id)
    }

    @Test
    fun theTimeFollowsTheAppWideTwelveHourFormat() {
        render(use24Hour = false)

        composeRule.onNodeWithText("6:30 AM").assertIsDisplayed()
        composeRule.onNodeWithText("6:45 PM").assertIsDisplayed()
    }

    @Test
    fun anEmptyListOffersToCreateTheFirstAlarm() {
        var added = false
        render(state = AlarmUiState(isLoading = false), onAddClick = { added = true })

        composeRule.onNodeWithText("No alarms yet").assertIsDisplayed()

        // Both the empty-state action and the floating button create the first
        // alarm; the floating one is always present.
        composeRule.onNodeWithTag(AlarmTestTags.AddFab).assertIsDisplayed()
        composeRule.onNodeWithText("Add an alarm").performClick()

        assertEquals(true, added)
    }

    @Test
    fun theFloatingButtonCreatesAnAlarm() {
        var added = false
        render(onAddClick = { added = true })

        composeRule.onNodeWithTag(AlarmTestTags.AddFab).performClick()

        assertEquals(true, added)
    }

    @Test
    fun theExactAlarmNoticeAppearsOnlyWhenThePlatformRefusesExactTriggers() {
        var allowed = false
        render(
            state = PreviewSamples.alarmState().copy(exactAlarmsAllowed = false),
            onAllowExactAlarms = { allowed = true },
        )

        composeRule.onNodeWithText("Allow exact alarms so they ring on time.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("ALLOW").performClick()

        assertEquals(true, allowed)
    }

    @Test
    fun theNoticeIsAbsentWhenExactAlarmsAreAllowed() {
        render()

        composeRule.onNodeWithText("Allow exact alarms so they ring on time.")
            .assertDoesNotExist()
    }

    @Test
    fun aCustomRepeatMaskReadsAsTheChosenDays() {
        render(
            state = AlarmUiState(
                isLoading = false,
                alarms = listOf(
                    Alarm(
                        id = 9,
                        hour = 8,
                        minute = 15,
                        repeatDays = RepeatDays.MONDAY or RepeatDays.WEDNESDAY or RepeatDays.FRIDAY,
                    ),
                ),
            ),
        )

        // The day names come from the platform locale (en-US under Robolectric),
        // so this is both the separator and the calendar order.
        composeRule.onNodeWithText("Mon, Wed, Fri", substring = true).assertIsDisplayed()
    }

    @Test
    fun swipingAnAlarmLeftRemovesIt() {
        var removed: Alarm? = null
        render(onDeleteAlarm = { removed = it })

        composeRule.onNodeWithTag(AlarmTestTags.alarm(1)).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(1L, removed?.id)
    }

    @Test
    fun theDeleteAlarmActionDeletesTheSameWayASwipeWould() {
        var removed: Alarm? = null
        render(onDeleteAlarm = { removed = it })

        composeRule.onNodeWithTag(AlarmTestTags.alarm(1))
            .performCustomAccessibilityActionWithLabel("Delete alarm")

        assertEquals(1L, removed?.id)
    }

    @Test
    fun draggingAnAlarmDownPersistsTheNewOrder() {
        var reordered: List<Long>? = null
        render(onReorderAlarms = { reordered = it })

        composeRule.onNodeWithTag(AlarmTestTags.alarm(1)).performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(Offset(0f, 220f))
            up()
        }

        val order = reordered
        assertTrue("the drag must persist an order", order != null)
        assertEquals(listOf(2L, 1L, 3L), order)
    }

    @Test
    fun theMoveDownActionReordersTheSameWayADragWould() {
        var reordered: List<Long>? = null
        render(onReorderAlarms = { reordered = it })

        composeRule.onNodeWithTag(AlarmTestTags.alarm(1))
            .performCustomAccessibilityActionWithLabel("Move down")

        assertEquals(listOf(2L, 1L, 3L), reordered)
    }

    @Test
    fun theFirstAlarmOffersNoMoveUpAndTheLastOffersNoMoveDown() {
        render()

        val first = customActionLabels(AlarmTestTags.alarm(1))
        assertTrue("the first card must not offer Move up", "Move up" !in first)
        assertTrue("the first card must offer Move down", "Move down" in first)

        val last = customActionLabels(AlarmTestTags.alarm(3))
        assertTrue("the last card must offer Move up", "Move up" in last)
        assertTrue("the last card must not offer Move down", "Move down" !in last)
    }

    private fun customActionLabels(tag: String): List<String> =
        composeRule.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            .orEmpty()
            .map { it.label }

    @Test
    fun theHourglassOpensTheTimerAndTheCloseButtonReturnsToTheAlarms() {
        var started: Long? = null
        composeRule.setContent {
            FixedClockwiseTheme {
                AlarmScreen(
                    state = AlarmUiState(isLoading = false),
                    use24Hour = true,
                    onAddClick = {},
                    onAlarmClick = {},
                    onToggleAlarm = { _, _ -> },
                    timerState = TimerUiState(isLoading = false),
                    onStartTimer = { started = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Timer").performClick()

        composeRule.onNodeWithText("START TIMER").assertIsDisplayed()
        composeRule.onNodeWithText("No alarms yet").assertDoesNotExist()

        composeRule.onNodeWithText("START TIMER").performClick()
        assertEquals(300_000L, started)

        // Back belongs to the alarm list, not to the tab.
        composeRule.onNodeWithContentDescription("Close timer").performClick()
        composeRule.onNodeWithTag(AlarmTestTags.AddFab).assertIsDisplayed()
        composeRule.onNodeWithText("No alarms yet").assertIsDisplayed()
    }

    @Test
    fun theStopwatchButtonOpensItsSectionAndTheCloseButtonReturns() {
        composeRule.setContent {
            FixedClockwiseTheme {
                AlarmScreen(
                    state = AlarmUiState(isLoading = false),
                    use24Hour = true,
                    onAddClick = {},
                    onAlarmClick = {},
                    onToggleAlarm = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithTag(AlarmTestTags.StopwatchButton).performClick()

        composeRule.onNodeWithText("STOPWATCH").assertIsDisplayed()
        composeRule.onNodeWithText("00:00.00").assertIsDisplayed()
        // The add button belongs to the alarm list, not to the detours.
        composeRule.onNodeWithTag(AlarmTestTags.AddFab).assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Close timer").performClick()
        composeRule.onNodeWithTag(AlarmTestTags.AddFab).assertIsDisplayed()
    }

    @Test
    fun aScheduledAlarmShowsItsDateInsteadOfARepeatRule() {
        val epochDay = java.time.LocalDate.of(2026, 11, 12).toEpochDay()
        render(
            state = AlarmUiState(
                isLoading = false,
                alarms = listOf(
                    Alarm(id = 5, hour = 9, minute = 0, scheduledDateEpochDay = epochDay),
                ),
            ),
        )

        composeRule.onNodeWithText("Scheduled for", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Once", substring = true).assertDoesNotExist()
    }

    @Test
    fun aTimerRequestOpensTheCountdownOnceAndIsReportedBack() {
        var openedCount = 0
        var open = true
        composeRule.setContent {
            FixedClockwiseTheme {
                AlarmScreen(
                    state = AlarmUiState(isLoading = false),
                    use24Hour = true,
                    onAddClick = {},
                    onAlarmClick = {},
                    onToggleAlarm = { _, _ -> },
                    timerState = TimerUiState(isLoading = false),
                    openTimer = open,
                    onTimerOpened = {
                        openedCount += 1
                        open = false
                    },
                )
            }
        }

        composeRule.onNodeWithText("START TIMER").assertIsDisplayed()
        assertEquals(1, openedCount)
    }
}
