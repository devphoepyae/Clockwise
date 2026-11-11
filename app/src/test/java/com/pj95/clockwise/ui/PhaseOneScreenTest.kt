package com.pj95.clockwise.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.model.prefs.ThemeMode
import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.ui.screens.HelpScreen
import com.pj95.clockwise.ui.screens.PrivacyScreen
import com.pj95.clockwise.ui.screens.ScreenSaverScreen
import com.pj95.clockwise.ui.screens.SettingsContent
import com.pj95.clockwise.ui.screens.SettingsTestTags
import com.pj95.clockwise.ui.screens.StudioScreen
import com.pj95.clockwise.ui.theme.ClockwiseTheme
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/**
 * Phase 1's information architecture: the Settings groups, the three local
 * information screens, and the desk clock. All of it has to work with no mail
 * app, no WebView and no network.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class PhaseOneScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsHasExactlyTheFourItemsInOrder() {
        var theme: ThemeMode? = null
        var use24Hour: Boolean? = null
        var compare: Boolean? = null
        var showIntro = false
        var sendFeedback = false

        composeRule.setContent {
            ClockwiseTheme {
                SettingsContent(
                    themeMode = ThemeMode.SYSTEM,
                    onThemeModeSelected = { theme = it },
                    use24Hour = true,
                    onUse24HourChange = { use24Hour = it },
                    onShowIntro = { showIntro = true },
                    onSendFeedback = { sendFeedback = true },
                    compareWithLocalTime = false,
                    onCompareWithLocalTimeChange = { compare = it },
                )
            }
        }

        listOf("THEME MODE", "TIME FORMAT", "TIME COMPARISON", "Legal & Support")
            .forEach { label ->
                composeRule.onNodeWithText(label).performScrollTo().assertIsDisplayed()
            }

        // The gallery moved to the Clocks header in v1.0.1; it must not also
        // linger as a fifth settings row.
        composeRule.onAllNodesWithText("Chronos gallery").assertCountEquals(0)

        composeRule.onNodeWithText("Dark").performScrollTo().performClick()
        composeRule.onNodeWithText("24-hour format").performScrollTo().performClick()
        composeRule.onNodeWithText("Compare with local time").performScrollTo().performClick()
        composeRule.onNodeWithTag(SettingsTestTags.Legal).performScrollTo().performClick()

        // The Legal & Support group expands into its own content, and the two
        // actions it owns are the ones the group exists for.
        composeRule.onNodeWithText("Zero-Internet & Privacy Policy").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("App Guide & Chronos Tips").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Terms of Service").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Open Source Licenses").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("About & Version Info & Feedback").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Send feedback").performScrollTo().performClick()
        composeRule.onNodeWithText("Show intro").performScrollTo().performClick()

        assertEquals(ThemeMode.DARK, theme)
        assertEquals(false, use24Hour)
        assertEquals(true, compare)
        assertTrue(sendFeedback)
        assertTrue(showIntro)
    }

    @Test
    fun comparisonIsOffByDefault() {
        composeRule.setContent {
            ClockwiseTheme {
                SettingsContent(
                    themeMode = ThemeMode.SYSTEM,
                    onThemeModeSelected = {},
                    use24Hour = true,
                    onUse24HourChange = {},
                    compareWithLocalTime = false,
                )
            }
        }

        composeRule.onNodeWithTag(SettingsTestTags.CompareToggle).assertIsOff()
        composeRule.onAllNodesWithText("APPEARANCE").assertCountEquals(0)
        composeRule.onAllNodesWithText("ALARMS").assertCountEquals(0)
    }

    @Test
    fun thePrivacyPolicyStatesZeroCollectionAndGoesBack() {
        var back = false
        composeRule.setContent {
            FixedClockwiseTheme { PrivacyScreen(onBack = { back = true }) }
        }

        composeRule.onNodeWithText("Privacy policy").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing leaves your phone").assertIsDisplayed()
        composeRule.onNodeWithText("No ads, trackers or analytics").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(back)
    }

    @Test
    fun helpExplainsThePermissionsAndTheScreenSaver() {
        composeRule.setContent {
            FixedClockwiseTheme { HelpScreen(onBack = {}) }
        }

        composeRule.onNodeWithText("Help").assertIsDisplayed()
        composeRule.onNodeWithText("Exact alarms").assertIsDisplayed()
        composeRule.onNodeWithText("Do Not Disturb").assertIsDisplayed()
        composeRule.onNodeWithText("Screen saver").assertIsDisplayed()
    }

    @Test
    fun studioGalleryNamesItselfAndGoesBack() {
        var back = false
        composeRule.setContent {
            FixedClockwiseTheme { StudioScreen(onBack = { back = true }) }
        }

        composeRule.onNodeWithText("Chronos gallery").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Home-screen clocks for the places you follow. " +
                "Every style stays on this device.",
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(back)
    }

    @Test
    fun theScreenSaverShowsTheTimeAndExitsOnATap() {
        val now = Instant.parse("2026-10-28T06:30:00Z")
        val expected = CityTimeFormatter.timeText(
            now.atZone(ZoneId.systemDefault()).toLocalTime(),
            true,
        )
        var exited = false

        composeRule.setContent {
            FixedClockwiseTheme {
                ScreenSaverScreen(
                    now = now,
                    use24Hour = true,
                    onExit = { exited = true },
                )
            }
        }

        composeRule.onNodeWithText(expected).assertIsDisplayed()
        composeRule.onNodeWithText("TAP ANYWHERE TO EXIT").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Screen saver clock. Tap to return to the app.",
        ).performClick()

        assertEquals(true, exited)
    }
}
