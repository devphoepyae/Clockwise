package com.pj95.clockwise.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.ui.ClockwiseUiState
import com.pj95.clockwise.ui.components.CityCardModel
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Reordering on the Clocks screen.
 *
 * A drag is exercised through real touch input, and the same reorder is checked
 * through the custom accessibility actions a screen reader uses — the two paths
 * have to end in the same persisted list.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class ClocksScreenReorderTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        state: ClockwiseUiState = PreviewSamples.clockState(),
        onReorderCities: (List<String>) -> Unit = {},
        onDeleteCity: (CityCardModel) -> Unit = {},
    ) {
        composeRule.setContent {
            FixedClockwiseTheme {
                ClocksScreen(
                    state = state,
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                    onReorderCities = onReorderCities,
                    onDeleteCity = onDeleteCity,
                )
            }
        }
    }

    @Test
    fun draggingACityDownPersistsTheNewOrder() {
        var reordered: List<String>? = null
        render(onReorderCities = { reordered = it })

        composeRule.onNodeWithText("Tokyo").performTouchInput {
            down(center)
            // Past the long-press threshold, then far enough to cross the next
            // card's midpoint.
            advanceEventTime(700)
            moveBy(Offset(0f, 220f))
            up()
        }

        val order = reordered
        assertTrue("the drag must persist an order", order != null)
        assertEquals("sydney", order!!.first())
        assertTrue(order.indexOf("tokyo") > 0)
        assertEquals(PreviewSamples.clockState().cities.map { it.id }.toSet(), order.toSet())
    }

    @Test
    fun theMoveDownActionReordersTheSameWayADragWould() {
        var reordered: List<String>? = null
        render(onReorderCities = { reordered = it })

        composeRule.onNodeWithText("Tokyo")
            .performCustomAccessibilityActionWithLabel("Move down")

        val order = reordered!!
        assertEquals(listOf("sydney", "tokyo"), order.take(2))
    }

    @Test
    fun theMoveUpActionOnTheSecondCardMovesItToTheTop() {
        var reordered: List<String>? = null
        render(onReorderCities = { reordered = it })

        composeRule.onNodeWithText("Sydney")
            .performCustomAccessibilityActionWithLabel("Move up")

        assertEquals(listOf("sydney", "tokyo"), reordered!!.take(2))
    }

    @Test
    fun theFirstCardOffersNoMoveUpAndTheLastOffersNoMoveDown() {
        render()

        val first = customActionLabels("Tokyo")
        assertTrue("the first card must not offer Move up", "Move up" !in first)
        assertTrue("the first card must offer Move down", "Move down" in first)

        val last = customActionLabels("London")
        assertTrue("the last card must offer Move up", "Move up" in last)
        assertTrue("the last card must not offer Move down", "Move down" !in last)
    }

    @Test
    fun aSingleCityOffersNoReorderActionsButStillOffersDelete() {
        val state = PreviewSamples.clockState()
        render(
            state = state.copy(
                cities = listOf(state.cities.first()),
                activeCityId = state.cities.first().id,
            ),
        )

        val labels = customActionLabels(state.cities.first().name)
        assertTrue("a lone city cannot move up", "Move up" !in labels)
        assertTrue("a lone city cannot move down", "Move down" !in labels)
        assertTrue("a lone city can still be removed", "Remove city" in labels)
    }

    @Test
    fun swipingACityLeftRemovesIt() {
        var removed: CityCardModel? = null
        render(onDeleteCity = { removed = it })

        composeRule.onNodeWithText("Tokyo").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals("tokyo", removed?.id)
    }

    @Test
    fun theRemoveCityActionDeletesTheSameWayASwipeWould() {
        var removed: CityCardModel? = null
        render(onDeleteCity = { removed = it })

        composeRule.onNodeWithText("Tokyo")
            .performCustomAccessibilityActionWithLabel("Remove city")

        assertEquals("tokyo", removed?.id)
    }

    @Test
    fun theActiveCardIsAnnouncedAsSelected() {
        render()

        // Los Angeles is the active city at the reference instant.
        composeRule.onNodeWithText("Los Angeles").assertIsSelected()
    }

    @Test
    fun everyCardExposesItsCityNameAndTimeToTheScreenReader() {
        render()

        // A screen reader reads the merged card as one node, so the name and the
        // time have to be on that same node rather than only in separate children.
        composeRule
            .onNode(hasText("Tokyo") and hasText("01:40"))
            .assertExists()
    }

    private fun customActionLabels(text: String): List<String> =
        composeRule.onNodeWithText(text)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            .orEmpty()
            .map { it.label }
}
