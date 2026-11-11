package com.pj95.clockwise.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.model.timer.CountdownTimer
import com.pj95.clockwise.ui.TimerUiState
import com.pj95.clockwise.ui.components.TimerTestTags
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The timer half of the Alarms tab: setup starts the chosen duration, a running
 * timer can be paused, a paused one resumed, and a finished one cleared.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class TimerPanelTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        state: TimerUiState = TimerUiState(isLoading = false),
        onStart: (Long) -> Unit = {},
        onPause: () -> Unit = {},
        onResume: () -> Unit = {},
        onReset: () -> Unit = {},
        onAllowExactAlarms: () -> Unit = {},
        onExtend: (Long) -> Unit = {},
    ) {
        composeRule.setContent {
            FixedClockwiseTheme {
                TimerPanel(
                    state = state,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onReset = onReset,
                    onAllowExactAlarms = onAllowExactAlarms,
                    onExtend = onExtend,
                )
            }
        }
    }

    @Test
    fun setupHidesThePickerUntilTheKeyboardButtonAsksForIt() {
        render()

        composeRule.onNodeWithTag(TimerTestTags.DurationDisplay).assertIsDisplayed()
        composeRule.onNodeWithTag(TimerTestTags.HoursWheel).assertDoesNotExist()
        composeRule.onNodeWithTag(TimerTestTags.MinutesWheel).assertDoesNotExist()

        composeRule.onNodeWithTag(TimerTestTags.PickerToggle).performClick()

        composeRule.onNodeWithTag(TimerTestTags.HoursWheel).assertIsDisplayed()
        composeRule.onNodeWithTag(TimerTestTags.MinutesWheel).assertIsDisplayed()
        composeRule.onNodeWithTag(TimerTestTags.SecondsWheel).assertIsDisplayed()
    }

    @Test
    fun setupStartsTheDefaultFiveMinutes() {
        var started: Long? = null
        render(onStart = { started = it })

        composeRule.onNodeWithTag(TimerTestTags.Start).performClick()

        assertEquals(300_000L, started)
    }

    @Test
    fun thePresetsAreFiveTenFifteenAndThirtyOnly() {
        var started: Long? = null
        render(onStart = { started = it })

        listOf(5, 10, 15, 30).forEach { minutes ->
            composeRule.onNodeWithTag("${TimerTestTags.Preset}_$minutes").assertIsDisplayed()
        }
        composeRule.onNodeWithTag("${TimerTestTags.Preset}_1").assertDoesNotExist()

        composeRule.onNodeWithTag("${TimerTestTags.Preset}_30").performClick()
        composeRule.onNodeWithTag(TimerTestTags.Start).performClick()

        assertEquals(1_800_000L, started)
    }

    @Test
    fun theScreenOffHintSitsUnderStart() {
        render()

        composeRule.onNodeWithTag(TimerTestTags.ScreenOffHint).assertIsDisplayed()
        composeRule.onNodeWithText(
            "ဖုန်း screen ပိတ်ထားလည်း Timer ဆက်လက် အလုပ်လုပ်ပါမည်",
        ).assertIsDisplayed()
    }

    @Test
    fun aRunningTimerShowsTheCountdownAndPauses() {
        var paused = false
        render(
            state = TimerUiState(
                isLoading = false,
                timer = runningTimer(remainingMillis = 65_000),
                remainingMillis = 65_000,
            ),
            onPause = { paused = true },
        )

        composeRule.onNodeWithText("01:05").assertIsDisplayed()
        composeRule.onNodeWithText("REMAINING").assertIsDisplayed()

        composeRule.onNodeWithTag(TimerTestTags.Pause).performClick()

        assertTrue(paused)
    }

    @Test
    fun aRunningTimerCanTakeOneMoreMinuteAndBeReset() {
        var extended: Long? = null
        var reset = false
        render(
            state = TimerUiState(
                isLoading = false,
                timer = runningTimer(remainingMillis = 65_000),
                remainingMillis = 65_000,
            ),
            onExtend = { extended = it },
            onReset = { reset = true },
        )

        composeRule.onNodeWithTag("${TimerTestTags.AddTime}_1").performClick()
        assertEquals(60_000L, extended)

        composeRule.onNodeWithTag(TimerTestTags.Cancel).performClick()
        assertTrue(reset)
    }

    @Test
    fun aPausedTimerOffersResumeInsteadOfPause() {        var resumed = false
        render(
            state = TimerUiState(
                isLoading = false,
                timer = CountdownTimer(
                    durationMillis = 300_000,
                    remainingMillis = 125_000,
                    endAtEpochMillis = null,
                    running = false,
                ),
                remainingMillis = 125_000,
            ),
            onResume = { resumed = true },
        )

        composeRule.onNodeWithText("02:05").assertIsDisplayed()
        composeRule.onNodeWithText("PAUSED").assertIsDisplayed()
        composeRule.onNodeWithTag(TimerTestTags.Resume).performClick()

        assertTrue(resumed)
    }

    @Test
    fun aFinishedTimerExplainsItselfAndOffersANewOne() {
        var reset = false
        render(
            state = TimerUiState(
                isLoading = false,
                timer = CountdownTimer(
                    durationMillis = 300_000,
                    remainingMillis = 0,
                    endAtEpochMillis = null,
                    running = false,
                ),
                remainingMillis = 0,
            ),
            onReset = { reset = true },
        )

        composeRule.onNodeWithText("Time's up").assertIsDisplayed()
        composeRule.onNodeWithTag(TimerTestTags.Reset).performClick()

        assertTrue(reset)
    }

    @Test
    fun theExactAlarmNoticeIsOnlyShownWhenThePlatformRefusesExactTriggers() {
        var allowed = false
        render(
            state = TimerUiState(isLoading = false, exactAlarmsAllowed = false),
            onAllowExactAlarms = { allowed = true },
        )

        composeRule.onNodeWithText("Allow exact alarms so the timer rings on time.")
            .assertIsDisplayed()
            .performClick()

        assertTrue(allowed)
    }

    private fun runningTimer(remainingMillis: Long): CountdownTimer = CountdownTimer(
        durationMillis = 300_000,
        remainingMillis = remainingMillis,
        endAtEpochMillis = 1_000_000,
        running = true,
    )
}
