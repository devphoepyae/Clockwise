package com.pj95.clockwise.ui.screens

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Behaviour of the city search sheet: filtering, adding and duplicate guard. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class CitySearchContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        query: String,
        savedCityIds: Set<String> = CityDirectory.defaultSavedCityIds.toSet(),
        onAddCity: (String) -> Unit = {},
    ) {
        val state = PreviewSamples.clockState()
        composeRule.setContent {
            FixedClockwiseTheme {
                CitySearchContent(
                    query = query,
                    onQueryChange = {},
                    savedCityIds = savedCityIds,
                    now = state.now,
                    use24Hour = state.use24Hour,
                    onAddCity = onAddCity,
                    onDismiss = {},
                    autoFocus = false,
                )
            }
        }
    }

    @Test
    fun aBlankQueryListsTheWholeDirectory() {
        render(query = "")

        // The directory is longer than the sheet, so scroll to prove that every
        // city is offered, not just the saved ones.
        composeRule.onNodeWithTag(CitySearchTags.Results)
            .performScrollToNode(hasTestTag(CitySearchTags.result("reykjavik")))

        composeRule.onNodeWithTag(CitySearchTags.result("reykjavik")).assertExists()
    }

    @Test
    fun tappingAnUnsavedCityAddsIt() {
        var added: String? = null
        render(query = "Yangon", onAddCity = { added = it })

        composeRule.onNodeWithTag(CitySearchTags.result("yangon")).performClick()

        assertEquals("yangon", added)
    }

    @Test
    fun anAlreadySavedCityIsMarkedAsNotEnabled() {
        render(query = "Tokyo")

        composeRule.onNodeWithTag(CitySearchTags.result("tokyo")).assertIsNotEnabled()
    }

    @Test
    fun tappingAnAlreadySavedCityAddsNothing() {
        var added: String? = null
        render(query = "Tokyo", onAddCity = { added = it })

        composeRule.onNodeWithTag(CitySearchTags.result("tokyo")).performClick()

        assertNull(added)
    }

    @Test
    fun anUnknownQueryShowsTheEmptyMessage() {
        render(query = "atlantis")

        composeRule.onNodeWithText("No cities found").assertExists()
    }
}
