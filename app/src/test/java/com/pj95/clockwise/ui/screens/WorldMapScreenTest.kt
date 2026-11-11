package com.pj95.clockwise.ui.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.model.solar.SolarDayKind
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.ui.MapSolarSummary
import com.pj95.clockwise.ui.MapUiState
import com.pj95.clockwise.ui.components.WorldMapTestTags
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behaviour of the world map screen: marker selection, the route to the city
 * detail screen, the empty state, and the accessibility contract.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class WorldMapScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        map: MapUiState = PreviewSamples.mapState(),
        onSelectCity: (String) -> Unit = {},
        onOpenCity: (String) -> Unit = {},
        onFindCityClick: () -> Unit = {},
        onTimeChange: (MinuteOfDay) -> Unit = {},
        onGoLive: () -> Unit = {},
    ) {
        composeRule.setContent {
            FixedClockwiseTheme {
                WorldMapScreen(
                    map = map,
                    onSelectCity = onSelectCity,
                    onOpenCity = onOpenCity,
                    onAddClick = {},
                    onFindCityClick = onFindCityClick,
                    onTimeChange = onTimeChange,
                    onGoLive = onGoLive,
                )
            }
        }
    }

    @Test
    fun thereIsAMarkerForEverySavedCity() {
        render()

        listOf("tokyo", "sydney", "los-angeles", "new-york", "london").forEach { cityId ->
            composeRule.onNodeWithTag(WorldMapTestTags.marker(cityId)).assertExists()
        }
    }

    @Test
    fun tappingAMarkerSelectsThatCity() {
        var selected: String? = null
        render(onSelectCity = { selected = it })

        composeRule.onNodeWithTag(WorldMapTestTags.marker("tokyo")).performClick()

        assertEquals("tokyo", selected)
    }

    @Test
    fun theSelectedMarkerIsTheActiveCity() {
        render(PreviewSamples.mapState(activeCityId = "new-york"))

        composeRule.onNodeWithTag(WorldMapTestTags.marker("new-york")).assertIsSelected()
    }

    @Test
    fun tappingACityPillSelectsThatCity() {
        var selected: String? = null
        render(onSelectCity = { selected = it })

        composeRule.onNodeWithTag(WorldMapTestTags.cityPill("sydney")).performClick()

        assertEquals("sydney", selected)
    }

    @Test
    fun tappingTheClockPanelOpensTheActiveCityDetail() {
        var opened: String? = null
        render(onOpenCity = { opened = it })

        composeRule.onNodeWithTag(WorldMapTestTags.SELECTED_CITY_PANEL).performClick()

        assertEquals("los-angeles", opened)
    }

    @Test
    fun everyMarkerCarriesADescriptionWithItsNameTimeAndOffset() {
        render()

        // Los Angeles is active at the reference instant: 08:40, UTC-8.
        composeRule
            .onNodeWithContentDescription("Los Angeles, 08:40, UTC-8, selected city")
            .assertExists()
        composeRule
            .onNodeWithContentDescription("Tokyo, 01:40, UTC+9")
            .assertExists()
    }

    @Test
    fun theMapItselfHasADescriptionForScreenReaders() {
        render()

        composeRule.onNodeWithTag(WorldMapTestTags.MAP_CANVAS, useUnmergedTree = true)
            .assertExists()
        composeRule
            .onNodeWithContentDescription("Schematic world map. UTC offset ruler.", substring = true)
            .assertExists()
    }

    @Test
    fun anEmptyCityListShowsAPromptInsteadOfMarkers() {
        var findCityTapped = false
        render(
            map = PreviewSamples.emptyMapState(),
            onFindCityClick = { findCityTapped = true },
        )

        composeRule.onNodeWithText("No cities on the map").assertIsDisplayed()
        composeRule.onNodeWithTag(WorldMapTestTags.marker("tokyo")).assertDoesNotExist()

        composeRule.onNodeWithText("Find a city").performClick()

        assertEquals(true, findCityTapped)
    }

    @Test
    fun anEmptyCityListNeverCallsBackWithACity() {
        var selected: String? = null
        render(map = PreviewSamples.emptyMapState(), onSelectCity = { selected = it })

        composeRule.onNodeWithText("No cities on the map").assertIsDisplayed()

        assertNull(selected)
    }

    @Test
    fun selectingFromTheStripIsOfferedAsASingleSelectionGroup() {
        render()

        // The active pill reports itself selected; the others do not.
        composeRule.onNodeWithTag(WorldMapTestTags.cityPill("los-angeles")).assertIsSelected()
    }

    @Test
    fun theMapStartsLiveAndShowsTheDevicesTime() {
        render()

        composeRule.onNodeWithText("LIVE").assertIsDisplayed()
        // The reference instant is 23:10 where the sample device is (UTC+6:30).
        composeRule.onNodeWithText("23:10").assertIsDisplayed()
        composeRule.onNodeWithText("Sat, 20 Jan · UTC+6:30").assertIsDisplayed()
        composeRule.onNodeWithText("NOW").assertDoesNotExist()
    }

    @Test
    fun timeTravelMovesEveryClockToTheChosenMinute() {
        render(map = PreviewSamples.mapState(travelMinute = MinuteOfDay.of(3, 0)))

        composeRule.onNodeWithText("TIME TRAVEL").assertIsDisplayed()
        composeRule.onNodeWithText("03:00").assertIsDisplayed()
        // 03:00 in Yangon is still the previous afternoon in California.
        composeRule.onNodeWithText("12:30").assertIsDisplayed()
        // Other cities keep their markers, and those markers carry the
        // travelled time too rather than the live clock.
        composeRule
            .onNodeWithContentDescription("Tokyo, 05:30, UTC+9")
            .assertExists()
    }

    @Test
    fun nowReturnsTheMapToLiveTime() {
        var wentLive = false
        render(
            map = PreviewSamples.mapState(travelMinute = MinuteOfDay.of(3, 0)),
            onGoLive = { wentLive = true },
        )

        composeRule.onNodeWithText("NOW").performClick()

        assertEquals(true, wentLive)
    }

    @Test
    fun theTimeSliderOffersTheWholeDayToAssistiveTechnology() {
        var chosen: MinuteOfDay? = null
        render(onTimeChange = { chosen = it })

        composeRule
            .onNodeWithContentDescription("Map time of day")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(540f) }

        assertEquals(MinuteOfDay.of(9, 0), chosen)
    }

    @Test
    fun theSelectedCityShowsItsSunriseAndSunset() {
        render()

        composeRule.onNodeWithText("SUNRISE", substring = true).assertExists()
        composeRule.onNodeWithText("SUNSET", substring = true).assertExists()
        composeRule
            .onNodeWithContentDescription("Sunrise", substring = true)
            .assertExists()
    }

    @Test
    fun aPolarDayReplacesTheMissingSunsetWithAnExplicitLabel() {
        val solar = MapSolarSummary(
            cityId = "los-angeles",
            date = java.time.LocalDate.of(2024, 6, 20),
            sunriseText = null,
            sunsetText = null,
            kind = SolarDayKind.POLAR_DAY,
        )
        render(map = PreviewSamples.mapState().copy(solar = solar))

        composeRule.onNodeWithText("MIDNIGHT SUN · NO SUNSET").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(
                "Midnight sun in Los Angeles: the sun does not set",
            )
            .assertExists()
    }

    @Test
    fun aPolarNightReplacesTheMissingSunriseWithAnExplicitLabel() {
        val solar = MapSolarSummary(
            cityId = "los-angeles",
            date = java.time.LocalDate.of(2024, 12, 21),
            sunriseText = null,
            sunsetText = null,
            kind = SolarDayKind.POLAR_NIGHT,
        )
        render(map = PreviewSamples.mapState().copy(solar = solar))

        composeRule.onNodeWithText("POLAR NIGHT · NO SUNRISE").assertIsDisplayed()
    }
}
