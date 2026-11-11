package com.pj95.clockwise.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.ui.PlannerUiState
import com.pj95.clockwise.ui.components.DaySliderTags
import com.pj95.clockwise.ui.mapping.buildPlannerUiState
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The meeting planner's behaviour: what it shows, and what the slider reports
 * back when it is touched.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class MeetingPlannerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var lastSlot: MinuteOfDay? = null
    private var lastSource: String? = null
    private var sourceWasChanged = false

    private fun render(planner: PlannerUiState = PreviewSamples.plannerState()) {
        lastSlot = null
        lastSource = null
        sourceWasChanged = false
        composeRule.setContent {
            FixedClockwiseTheme(theme = AppTheme.LIGHT_MONOCHROME) {
                MeetingPlannerScreen(
                    planner = planner,
                    onMinuteChange = { lastSlot = it },
                    onSourceChange = {
                        lastSource = it
                        sourceWasChanged = true
                    },
                    onBack = {},
                    onFindCityClick = {},
                )
            }
        }
    }

    @Test
    fun theChosenSlotAndEveryCitysTimeAreOnScreen() {
        render(PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(9, 0)))

        composeRule.onNodeWithText("YOUR LOCAL TIME").assertIsDisplayed()
        // The same 09:00 slot, read in each saved city.
        composeRule.onNodeWithText("11:30").assertIsDisplayed()
        composeRule.onNodeWithText("13:30").assertIsDisplayed()
        composeRule.onNodeWithText("18:30").assertIsDisplayed()
        composeRule.onNodeWithText("21:30").assertIsDisplayed()
        composeRule.onNodeWithText("02:30").assertIsDisplayed()
    }

    @Test
    fun theLabelsTheUserSetAreUsedInTheRows() {
        render(PreviewSamples.plannerState(labels = true))

        composeRule.onNodeWithText("Tokyo (Head Office)").assertIsDisplayed()
        composeRule.onNodeWithText("New York (HQ)").assertIsDisplayed()
    }

    @Test
    fun theWorkingDayIsExplainedRatherThanAppliedSilently() {
        render()

        composeRule.onNodeWithText("WITHIN 09:00–18:00").assertIsDisplayed()
        composeRule.onNodeWithText("OUTSIDE").assertIsDisplayed()
    }

    @Test
    fun touchingTheSliderMidwayReportsMidday() {
        render(PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(9, 0)))

        val node = composeRule.onNodeWithTag(DaySliderTags.TRACK).fetchSemanticsNode()
        val inset = with(composeRule.density) { 10.dp.toPx() }
        val span = node.size.width - 2 * inset
        // Halfway along the track is midday, give or take the thumb's inset.
        composeRule.onNodeWithTag(DaySliderTags.TRACK).performTouchInput {
            click(Offset(inset + span / 2f, center.y))
        }

        assertEquals(MinuteOfDay.of(12, 0), lastSlot)
    }

    @Test
    fun touchingTheFarRightOfTheSliderStopsAtTheLastSlotOfTheDay() {
        render(PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(9, 0)))

        val node = composeRule.onNodeWithTag(DaySliderTags.TRACK).fetchSemanticsNode()
        composeRule.onNodeWithTag(DaySliderTags.TRACK).performTouchInput {
            click(Offset(node.size.width.toFloat() - 1f, center.y))
        }

        // 23:45, not a roll-over into tomorrow.
        assertEquals(MinuteOfDay(MinuteOfDay.LAST_GRID_VALUE), lastSlot)
    }

    @Test
    fun theSliderOffersItsWholeDayToOneFingerAndToTalkBack() {
        render(PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(9, 0)))

        val slider = composeRule.onNodeWithTag(DaySliderTags.TRACK)
        slider.assertRangeInfoEquals(
            ProgressBarRangeInfo(
                current = MinuteOfDay.of(9, 0).value.toFloat(),
                range = 0f..MinuteOfDay.LAST_GRID_VALUE.toFloat(),
                steps = MinuteOfDay.GRID_STEPS,
            ),
        )
        // Read out as a time, because "38 percent" would tell a screen-reader
        // user nothing about when the meeting is.
        slider.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "09:00"),
        )

        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(1035f) }
        assertEquals(MinuteOfDay.of(17, 15), lastSlot)
    }

    @Test
    fun anEmptyListExplainsItselfInsteadOfShowingAnEmptyDay() {
        render(PreviewSamples.emptyPlannerState())

        composeRule.onNodeWithText("No cities to plan for").assertIsDisplayed()
        composeRule.onNodeWithTag(DaySliderTags.TRACK).assertIsDisplayed()
        assertEquals(null, lastSlot)
        assertNotNull(composeRule.onNodeWithTag(DaySliderTags.TRACK).fetchSemanticsNode())
    }

    // ------------------------------------------------- source city converter

    @Test
    fun theSourcePickerStartsOnTheDeviceAndListsEverySavedCity() {
        render()

        composeRule.onNodeWithText("FROM").assertIsDisplayed()
        composeRule.onNodeWithTag(PlannerTestTags.sourceChip(null)).assertIsSelected()
        composeRule.onNodeWithText("LOCAL").assertIsDisplayed()

        listOf("tokyo", "sydney", "los-angeles", "new-york", "london").forEach { cityId ->
            composeRule.onNodeWithTag(PlannerTestTags.sourceChip(cityId)).assertExists()
        }
        composeRule.onNodeWithTag(PlannerTestTags.sourceChip("tokyo")).assertIsNotSelected()
    }

    @Test
    fun choosingACityTurnsTheHeadlineIntoATimeInThatCity() {
        render(PreviewSamples.plannerState(sourceCityId = "tokyo"))

        composeRule.onNodeWithText("TIME IN TOKYO").assertIsDisplayed()
        composeRule.onNodeWithTag(PlannerTestTags.sourceChip("tokyo")).assertIsSelected()
        composeRule.onNodeWithTag(PlannerTestTags.sourceChip(null)).assertIsNotSelected()
        composeRule
            .onNodeWithContentDescription("Show times in Tokyo")
            .assertIsSelected()
    }

    @Test
    fun tappingASourceChipReportsTheCityToConvertFrom() {
        render()

        composeRule.onNodeWithTag(PlannerTestTags.sourceChip("tokyo")).performClick()

        assertTrue(sourceWasChanged)
        assertEquals("tokyo", lastSource)
    }

    @Test
    fun tappingTheDeviceChipReportsNoSource() {
        render(PreviewSamples.plannerState(sourceCityId = "tokyo"))

        composeRule.onNodeWithTag(PlannerTestTags.sourceChip(null)).performClick()

        assertTrue(sourceWasChanged)
        assertEquals(null, lastSource)
    }

    // ------------------------------------------------- best overlap

    @Test
    fun theRecommendationIsAButtonThatMovesTheSliderToIt() {
        render(PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(9, 0)))

        // Three cities are at work at 06:30; that is the closest they get to
        // their collective noon.
        composeRule.onNodeWithText("BEST 06:30").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Best time 06:30: 3 cities within working hours")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PlannerTestTags.BEST_ACTION).performClick()

        assertEquals(MinuteOfDay.of(6, 30), lastSlot)
    }

    @Test
    fun theRecommendationFollowsTheAppWideClockFormat() {
        render(
            PreviewSamples.plannerState(
                minuteOfDay = MinuteOfDay.of(9, 0),
                use24Hour = false,
            ),
        )

        composeRule.onNodeWithText("BEST 6:30 AM").assertIsDisplayed()
    }

    @Test
    fun twoCitiesWithNoSharedDaySaySoInsteadOfOfferingAFalseRecommendation() {
        render(
            buildPlannerUiState(
                state = PreviewSamples.clockState(savedCityIds = listOf("tokyo", "new-york")),
                minuteOfDay = MinuteOfDay.of(9, 0),
                deviceZoneId = PreviewSamples.sampleDeviceZoneId,
            ),
        )

        composeRule.onNodeWithText("NO OVERLAP").assertIsDisplayed()
        composeRule.onNodeWithTag(PlannerTestTags.BEST_ACTION).assertHasNoClickAction()
        composeRule
            .onNodeWithContentDescription("No time puts two or more cities within working hours")
            .assertIsDisplayed()
    }

    @Test
    fun anEmptyPlannerHidesTheConverterAndTheRecommendation() {
        render(PreviewSamples.emptyPlannerState())

        composeRule.onNodeWithText("FROM").assertDoesNotExist()
        composeRule.onNodeWithTag(PlannerTestTags.BEST_ACTION).assertDoesNotExist()
        composeRule.onNodeWithText("YOUR LOCAL TIME").assertIsDisplayed()
    }
}
