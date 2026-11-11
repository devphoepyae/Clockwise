package com.pj95.clockwise.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.model.alarm.RepeatDays
import com.pj95.clockwise.ui.theme.AlarmRingingPalette
import com.pj95.clockwise.ui.theme.AlarmRingingTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The two gestures on the ringing screen are the whole interaction, so they are
 * checked directly: a swipe up anywhere snoozes, and the bottom slide still
 * stops. Both run the same callbacks the activity wires to the repository.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class AlarmRingingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        onSnooze: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            AlarmRingingTheme(palette = AlarmRingingPalette) {
                AlarmRingingScreen(
                    alarm = Alarm(
                        id = 1,
                        hour = 6,
                        minute = 30,
                        label = "Morning run",
                        repeatDays = RepeatDays.WEEKDAYS,
                    ),
                    use24Hour = true,
                    isLoading = false,
                    onDismiss = onDismiss,
                    onSnooze = onSnooze,
                )
            }
        }
    }

    @Test
    fun swipingUpAnywhereSnoozesTheAlarm() {
        var snoozed = false
        render(onSnooze = { snoozed = true })

        composeRule.onRoot().performTouchInput { swipeUp() }

        composeRule.runOnIdle { assertTrue(snoozed) }
    }

    @Test
    fun theBottomSlideStillStopsTheAlarm() {
        var dismissed = false
        render(onDismiss = { dismissed = true })

        composeRule.onNodeWithContentDescription("Stop the alarm")
            .performTouchInput { swipeRight() }

        composeRule.runOnIdle { assertTrue(dismissed) }
    }
}
