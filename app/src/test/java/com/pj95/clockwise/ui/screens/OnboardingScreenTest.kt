package com.pj95.clockwise.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The first-launch intro: one page, one action, and nothing to sign into.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(onDone: () -> Unit = {}) {
        composeRule.setContent {
            FixedClockwiseTheme {
                OnboardingScreen(onDone = onDone)
            }
        }
    }

    @Test
    fun theIntroExplainsTheAppInOnePage() {
        render()

        composeRule.onNodeWithTag(OnboardingTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithText("Every clock, at a glance.").assertIsDisplayed()
        composeRule.onNodeWithText("Clocks that stay right").assertIsDisplayed()
        composeRule.onNodeWithText("Plan across time zones").assertIsDisplayed()
        composeRule.onNodeWithText("Private by design").assertIsDisplayed()
    }

    @Test
    fun thereIsOnePrimaryActionAndItDismissesTheIntro() {
        var done = false
        render(onDone = { done = true })

        composeRule.onNodeWithTag(OnboardingTags.GET_STARTED)
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertTrue(done)
    }

    @Test
    fun theFootnotePointsAtSettingsForReplay() {
        render()

        composeRule
            .onNodeWithText("You can replay this any time from Settings.")
            .assertIsDisplayed()
    }
}
