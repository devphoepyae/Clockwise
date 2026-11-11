package com.pj95.clockwise.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * The FOREX quick-access strip on the Clocks screen.
 *
 * Each cell is one semantics node — "city, status, time" — so the assertions
 * also lock the screen-reader announcement.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class MarketSessionsStripTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(now: Instant, savedCityIds: List<String> = CityDirectory.defaultSavedCityIds) {
        val state = PreviewSamples.clockState(now = now, savedCityIds = savedCityIds)
        composeRule.setContent {
            FixedClockwiseTheme {
                ClocksScreen(
                    state = state,
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
    }

    @Test
    fun theFourCentresAreShownInTradingOrder() {
        // Saturday 20 Jan 2024: every exchange is closed.
        render(PreviewSamples.referenceInstant)

        composeRule.onNodeWithText("MARKETS").assertIsDisplayed()
        listOf(
            "Sydney, CLOSED, 03:40",
            "Tokyo, CLOSED, 01:40",
            "London, CLOSED, 16:40",
            "New York, CLOSED, 11:40",
        ).forEach { composeRule.onNodeWithContentDescription(it).assertIsDisplayed() }
    }

    @Test
    fun openAndClosedAreComputedAtTheSameInstant() {
        // Monday 15 Jan 2024, 14:00 UTC: London and New York are open, Tokyo
        // and Sydney are already past their close.
        render(Instant.parse("2024-01-15T14:00:00Z"))

        composeRule.onNodeWithContentDescription("London, OPEN, 14:00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("New York, OPEN, 09:00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tokyo, CLOSED, 23:00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sydney, CLOSED, 01:00").assertIsDisplayed()
    }

    @Test
    fun theStripKeepsAllFourCentresEvenWhenTheyAreNotSaved() {
        // Only Los Angeles is saved: the FOREX shortcut must still show all four.
        render(PreviewSamples.referenceInstant, savedCityIds = listOf("los-angeles"))

        composeRule.onNodeWithText("MARKETS").assertIsDisplayed()
        listOf(
            "Sydney, CLOSED, 03:40",
            "Tokyo, CLOSED, 01:40",
            "London, CLOSED, 16:40",
            "New York, CLOSED, 11:40",
        ).forEach { composeRule.onNodeWithContentDescription(it).assertIsDisplayed() }
    }
}
