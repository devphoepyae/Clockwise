package com.pj95.clockwise.ui

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasStartActivityClickAction
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.MainActivity
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.alarm.AlarmSoundCatalog
import com.pj95.clockwise.alarm.AlarmSoundOption
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.model.prefs.ThemeMode
import com.pj95.clockwise.data.prefs.SavedCities
import com.pj95.clockwise.data.prefs.SavedThemeMode
import com.pj95.clockwise.data.prefs.UserPreferences
import com.pj95.clockwise.data.prefs.toUserPreferences
import com.pj95.clockwise.data.repository.ClockRepository
import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.model.time.TimeDifference
import com.pj95.clockwise.domain.time.WORKING_DAY_END_HOUR
import com.pj95.clockwise.domain.time.WORKING_DAY_START_HOUR
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.model.alarm.RepeatDays
import com.pj95.clockwise.testing.FakeSavedCitiesDataStore
import com.pj95.clockwise.ui.components.AlarmTimeTestTags
import com.pj95.clockwise.ui.components.DaySliderTags
import com.pj95.clockwise.ui.components.MinTouchTarget
import com.pj95.clockwise.ui.components.WorldMapTestTags
import com.pj95.clockwise.ui.components.timeDifferenceLabel
import com.pj95.clockwise.ui.mapping.buildPlannerUiState
import com.pj95.clockwise.ui.mapping.buildWidgetUiState
import com.pj95.clockwise.ui.mapping.buildClockwiseUiState
import com.pj95.clockwise.ui.navigation.Routes
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.screens.CityDetailScreen
import com.pj95.clockwise.ui.screens.CityLabelDialog
import com.pj95.clockwise.ui.screens.CityLabelDialogTags
import com.pj95.clockwise.ui.screens.AlarmEditorContent
import com.pj95.clockwise.ui.screens.AlarmEditorTestTags
import com.pj95.clockwise.ui.screens.ClocksScreen
import com.pj95.clockwise.ui.screens.MeetingPlannerScreen
import com.pj95.clockwise.ui.screens.PlannerTestTags
import com.pj95.clockwise.ui.screens.SettingsContent
import com.pj95.clockwise.ui.screens.SettingsTestTags
import com.pj95.clockwise.ui.screens.WorldMapScreen
import com.pj95.clockwise.ui.theme.CoolSlateClockwisePalette
import com.pj95.clockwise.ui.theme.LightClockwisePalette
import com.pj95.clockwise.ui.theme.LocalClockwisePalette
import com.pj95.clockwise.ui.theme.MaterialYouFallbackPalette
import com.pj95.clockwise.ui.theme.NightClockwisePalette
import com.pj95.clockwise.ui.theme.WarmSepiaClockwisePalette
import com.pj95.clockwise.ui.theme.ClockwisePalette
import com.pj95.clockwise.ui.theme.ClockwiseTheme
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import com.pj95.clockwise.ui.theme.paletteFor
import com.pj95.clockwise.ui.widget.DayNightColorProvider
import com.pj95.clockwise.ui.widget.WidgetTestTags
import com.pj95.clockwise.ui.widget.ClockwiseWidgetContent
import com.pj95.clockwise.ui.widget.widgetRowBudget
import com.pj95.clockwise.widget.WidgetMirror
import com.pj95.clockwise.widget.WidgetTickerReceiver
import com.pj95.clockwise.widget.ClockwiseWidgetReceiver
import com.pj95.clockwise.widget.cancelWidgetTick
import com.pj95.clockwise.widget.scheduleWidgetTick
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import java.time.Instant
import java.time.ZoneId

/**
 * One test per acceptance criterion, each named after the criterion it proves.
 *
 * Logic criteria run against the real repository, formatter, mapper and map
 * domain objects. The behavioural criteria drive the real composables, and the
 * visual criteria have a matching PNG in `app/build/screenshots` produced by
 * [LayoutScreenshotTest] and [com.pj95.clockwise.ui.screens.WorldMapScreenTest].
 *
 * Criteria 1-11 are phase 1 (the clock app). Criterion 12 is the theme system;
 * 13 and 14 are the time difference and the city labels; 15 is the meeting
 * planner; 16 the home-screen widget; 17 the phase 2 integration and
 * accessibility pass; and 18 the 3.1.0 refinements — the comparison switch, the
 * wheel/dial time input and the alarm sound.
 *
 * Reference instants (see `PreviewSamples`): winter 2024-01-20T16:40Z and summer
 * 2024-07-20T16:40Z.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class AcceptanceCriteriaTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val winter = PreviewSamples.referenceInstant
    private val summer = Instant.parse("2024-07-20T16:40:00Z")

    private fun newRepository(
        store: FakeSavedCitiesDataStore = FakeSavedCitiesDataStore(),
    ): ClockRepository = ClockRepository(store)

    private fun snapshot(cityId: String, instant: Instant, use24Hour: Boolean = true) =
        CityTimeFormatter.snapshot(
            zoneId = ZoneId.of(CityDirectory.find(cityId)!!.timeZone),
            instant = instant,
            use24Hour = use24Hour,
        )

    // ---------------------------------------------------------------- 1

    /**
     * Criterion 1 — first launch shows the five default cities in the specified
     * order, with Los Angeles active and a 24-hour clock.
     */
    @Test
    fun criterion01_firstLaunchShowsTheDefaultCitiesInOrderWithLosAngelesActive() = runTest {
        val preferences = newRepository().preferences.first()

        assertEquals(
            listOf("tokyo", "sydney", "los-angeles", "new-york", "london"),
            preferences.savedCityIds,
        )
        assertEquals("los-angeles", preferences.activeCityId)
        assertTrue("The default format is 24-hour", preferences.use24Hour)

        val state = PreviewSamples.clockState()
        assertEquals(preferences.savedCityIds, state.cities.map { it.id })
        assertEquals(listOf("los-angeles"), state.cities.filter { it.isActive }.map { it.id })
    }

    // ---------------------------------------------------------------- 2

    /**
     * Criterion 2 — every time, UTC offset and weekday matches the city's IANA
     * timezone at the current instant.
     */
    @Test
    fun criterion02_everyCityMatchesItsIanaTimezone() {
        val expected = mapOf(
            "tokyo" to Triple("01:40", "UTC+9", "Sun"),
            "sydney" to Triple("03:40", "UTC+11", "Sun"),
            "los-angeles" to Triple("08:40", "UTC-8", "Sat"),
            "new-york" to Triple("11:40", "UTC-5", "Sat"),
            "london" to Triple("16:40", "UTC+0", "Sat"),
        )

        val state = buildClockwiseUiState(
            savedCityIds = CityDirectory.defaultSavedCityIds,
            activeCityId = CityDirectory.defaultActiveCityId,
            use24Hour = true,
            now = winter,
            deviceZoneId = ZoneId.of("Asia/Yangon"),
        )

        assertEquals(expected.size, state.cities.size)
        state.cities.forEach { city ->
            val (time, offset, weekday) = expected.getValue(city.id)
            assertEquals("${city.id} time", time, city.timeText)
            assertEquals("${city.id} offset", offset, city.offsetLabel)
            assertEquals("${city.id} weekday", weekday, city.weekdayLabel)
        }
        // UTC+0 keeps its sign rather than collapsing to "UTC".
        assertEquals("UTC+0", state.cities.single { it.id == "london" }.offsetLabel)
    }

    // ---------------------------------------------------------------- 3

    /**
     * Criterion 3 — daylight saving is picked up from the zone rules, with no
     * offset hard-coded anywhere.
     */
    @Test
    fun criterion03_daylightSavingIsAppliedAutomatically() {
        val winterOffsets = listOf("los-angeles", "new-york", "london", "sydney")
            .associateWith { snapshot(it, winter).offsetLabel }
        val summerOffsets = listOf("los-angeles", "new-york", "london", "sydney")
            .associateWith { snapshot(it, summer).offsetLabel }

        assertEquals("UTC-8", winterOffsets.getValue("los-angeles"))
        assertEquals("UTC-7", summerOffsets.getValue("los-angeles"))
        assertEquals("UTC-5", winterOffsets.getValue("new-york"))
        assertEquals("UTC-4", summerOffsets.getValue("new-york"))
        assertEquals("UTC+0", winterOffsets.getValue("london"))
        assertEquals("UTC+1", summerOffsets.getValue("london"))
        // The southern hemisphere moves the other way.
        assertEquals("UTC+11", winterOffsets.getValue("sydney"))
        assertEquals("UTC+10", summerOffsets.getValue("sydney"))

        // Half-hour zones keep their minutes rather than rounding to the hour.
        assertEquals("UTC+5:30", snapshot("mumbai", winter).offsetLabel)
    }

    // ---------------------------------------------------------------- 4

    /**
     * Criterion 4 — the sun icon covers local 06:00–17:59 and the moon covers
     * 18:00–05:59.
     */
    @Test
    fun criterion04_dayAndNightFollowTheSixToEighteenRule() {
        val utc = ZoneId.of("UTC")
        for (hour in 0..23) {
            val instant = Instant.parse("2024-01-20T%02d:30:00Z".format(hour))
            val expectedDaytime = hour in 6 until 18
            assertEquals(
                "Hour ${hour}:30 should be ${if (expectedDaytime) "day" else "night"}",
                expectedDaytime,
                CityTimeFormatter.isDaytime(utc, instant),
            )
            assertEquals(
                "Hour ${hour}:30 snapshot icon",
                expectedDaytime,
                CityTimeFormatter.snapshot(utc, instant, use24Hour = true).isDaytime,
            )
        }
    }

    // ---------------------------------------------------------------- 5

    /**
     * Criterion 5 — all 22 directory cities are searchable, and adding one twice
     * is a no-op.
     */
    @Test
    fun criterion05_searchCoversTheDirectoryAndRejectsDuplicates() = runTest {
        assertEquals(22, CityDirectory.search("").size)
        assertEquals(
            listOf("tokyo"),
            CityDirectory.search("tokyo").map { it.id },
        )
        assertEquals(listOf("yangon"), CityDirectory.search("Asia/Yangon").map { it.id })

        val repository = newRepository()
        repository.preferences.first()
        repository.addCity("yangon")
        repository.addCity("yangon")

        val preferences = repository.preferences.first()
        assertEquals(1, preferences.savedCityIds.count { it == "yangon" })
        assertEquals("yangon", preferences.activeCityId)
    }

    // ---------------------------------------------------------------- 6

    /**
     * Criterion 6 — selecting a city makes it active and its detail route
     * resolves to that city.
     */
    @Test
    fun criterion06_selectingACityMakesItActiveAndOpensItsDetail() = runTest {
        val repository = newRepository()
        repository.preferences.first()

        repository.selectCity("tokyo")

        val preferences = repository.preferences.first()
        assertEquals("tokyo", preferences.activeCityId)

        val state = buildClockwiseUiState(
            savedCityIds = preferences.savedCityIds,
            activeCityId = preferences.activeCityId,
            use24Hour = preferences.use24Hour,
            now = winter,
            deviceZoneId = ZoneId.of("Asia/Yangon"),
        )
        assertEquals(listOf("tokyo"), state.cities.filter { it.isActive }.map { it.id })

        // The detail destination resolves the id back to the same city.
        assertEquals("city/tokyo", Routes.cityDetail("tokyo"))
        assertEquals("Tokyo", CityDirectory.find("tokyo")?.name)
    }

    // ---------------------------------------------------------------- 7

    /**
     * Criterion 7 — removing works, removing the active city promotes another,
     * and removing everything empties the list.
     */
    @Test
    fun criterion07_removingCitiesHandlesTheActiveCityAndTheEmptyState() = runTest {
        val repository = newRepository()
        repository.preferences.first()

        // Removing an inactive city leaves the active one alone.
        repository.removeCity("london")
        var preferences = repository.preferences.first()
        assertEquals(4, preferences.savedCityIds.size)
        assertEquals("los-angeles", preferences.activeCityId)
        assertFalse(preferences.savedCityIds.contains("london"))

        // Removing the active city promotes the first remaining city.
        repository.removeCity("los-angeles")
        preferences = repository.preferences.first()
        assertEquals(listOf("tokyo", "sydney", "new-york"), preferences.savedCityIds)
        assertEquals("tokyo", preferences.activeCityId)

        // Removing everything empties the list and clears the active city.
        preferences.savedCityIds.toList().forEach { repository.removeCity(it) }
        preferences = repository.preferences.first()
        assertTrue(preferences.savedCityIds.isEmpty())
        assertNull(preferences.activeCityId)

        val state = buildClockwiseUiState(
            savedCityIds = preferences.savedCityIds,
            activeCityId = preferences.activeCityId,
            use24Hour = preferences.use24Hour,
            now = winter,
            deviceZoneId = ZoneId.of("Asia/Yangon"),
        )
        assertTrue(state.cities.isEmpty())
    }

    // ---------------------------------------------------------------- 8

    /**
     * Criterion 8 — the 12h/24h choice changes the display immediately and is
     * stored.
     */
    @Test
    fun criterion08_theTimeFormatSwitchesAndPersists() = runTest {
        val repository = newRepository()
        repository.preferences.first()

        assertEquals("01:40", snapshot("tokyo", winter, use24Hour = true).timeText)

        repository.setUse24Hour(false)
        var preferences = repository.preferences.first()
        assertFalse(preferences.use24Hour)

        val twelveHour = snapshot("tokyo", winter, use24Hour = preferences.use24Hour)
        assertEquals("1:40 AM", twelveHour.timeText)
        assertEquals("1", twelveHour.hourText)
        assertEquals("AM", twelveHour.periodText)

        // The format is app-wide: it applies to every city, not just the active one.
        assertEquals("8:40 AM", snapshot("los-angeles", winter, use24Hour = false).timeText)

        repository.setUse24Hour(true)
        preferences = repository.preferences.first()
        assertTrue(preferences.use24Hour)
        assertEquals("01:40", snapshot("tokyo", winter, use24Hour = preferences.use24Hour).timeText)
    }

    // ---------------------------------------------------------------- 9

    /**
     * Criterion 9 — order, active city and format survive a restart, and ids
     * that are no longer in the directory are dropped.
     */
    @Test
    fun criterion09_savedStateSurvivesRestartAndUnknownIdsAreDropped() = runTest {
        val store = FakeSavedCitiesDataStore()
        val first = ClockRepository(store)
        first.preferences.first()

        first.removeCity("sydney")
        first.addCity("reykjavik")
        first.selectCity("london")
        first.setUse24Hour(false)

        // A second repository over the same store models an app restart.
        val afterRestart = ClockRepository(store).preferences.first()
        assertEquals(listOf("tokyo", "los-angeles", "new-york", "london", "reykjavik"), afterRestart.savedCityIds)
        assertEquals("london", afterRestart.activeCityId)
        assertFalse(afterRestart.use24Hour)
    }

    @Test
    fun criterion09b_unknownIdsAreDroppedAndTheActiveCityIsRepaired() {
        val stored = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .addCityIds("atlantis")
            .addCityIds("new-york")
            .setActiveCityId("atlantis")
            .setUse24Hour(true)
            .build()

        val preferences: UserPreferences = stored.toUserPreferences()

        assertEquals(listOf("tokyo", "new-york"), preferences.savedCityIds)
        assertEquals("tokyo", preferences.activeCityId)
    }

    // ---------------------------------------------------------------- 10

    /**
     * Criterion 10 — the map plots every saved city, highlights the active one
     * with the accent marker and meridian, shades the night side, and the
     * selected-city card routes to the detail screen.
     *
     * Visual proof: `map_screen_360dp.png`, `map_screen_summer_360dp.png`,
     * `map_screen_london_360dp.png`.
     */
    @Test
    fun criterion10_theMapPlotsCitiesAndDrivesSelection() {
        val state = PreviewSamples.clockState()
        val active = state.cities.single { it.isActive }
        assertEquals("los-angeles", active.id)

        // Every saved city carries the coordinates the marker is drawn from.
        state.cities.forEach { city ->
            val directory = CityDirectory.find(city.id)!!
            assertEquals(directory.latitude, city.latitude, 1e-9)
            assertEquals(directory.longitude, city.longitude, 1e-9)
        }

        // The night side is shaded, so the subsolar point is lit and its
        // antipode is not.
        val subsolar = com.pj95.clockwise.domain.map.SolarPosition.subsolarPoint(state.now)
        assertTrue(
            com.pj95.clockwise.domain.map.SolarPosition.isDaylight(
                com.pj95.clockwise.model.map.GeoPoint(subsolar.longitude, subsolar.latitude),
                subsolar,
            ),
        )

        var selected: String? = null
        var opened: String? = null
        composeRule.setContent {
            FixedClockwiseTheme {
                WorldMapScreen(
                    map = PreviewSamples.mapState(),
                    onSelectCity = { selected = it },
                    onOpenCity = { opened = it },
                    onAddClick = {},
                    onFindCityClick = {},
                    onTimeChange = {},
                    onGoLive = {},
                )
            }
        }

        // One marker per saved city, and tapping one changes the active city.
        composeRule.onNodeWithTag(WorldMapTestTags.marker("tokyo")).performClick()
        assertEquals("tokyo", selected)
        composeRule.onNodeWithTag(WorldMapTestTags.marker("london")).performClick()
        assertEquals("london", selected)

        // The selected-city card opens the detail screen.
        composeRule.onNodeWithTag(WorldMapTestTags.SELECTED_CITY_PANEL).performClick()
        assertEquals("los-angeles", opened)
    }

    // ---------------------------------------------------------------- 11

    /**
     * Criterion 11 — the layout holds at 360 dp portrait, and every control
     * exposes an accessibility label.
     *
     * Visual proof: every PNG in `app/build/screenshots` is captured at
     * `w360dp-h800dp`.
     */
    @Test
    fun criterion11_layoutHoldsAt360dpAndControlsAreLabelled() {
        composeRule.setContent {
            FixedClockwiseTheme {
                WorldMapScreen(
                    map = PreviewSamples.mapState(),
                    onSelectCity = {},
                    onOpenCity = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onTimeChange = {},
                    onGoLive = {},
                )
            }
        }

        // The headline content is on screen and not ellipsised away.
        composeRule.onNodeWithText("Los Angeles").assertIsDisplayed()
        composeRule.onNodeWithText("08:40").assertIsDisplayed()
        composeRule.onNodeWithText("California, USA").assertIsDisplayed()

        assertEveryControlIsLabelled()
    }

    @Test
    fun criterion11b_iconOnlyControlsCarryContentDescriptions() {
        composeRule.setContent {
            FixedClockwiseTheme {
                WorldMapScreen(
                    map = PreviewSamples.mapState(),
                    onSelectCity = {},
                    onOpenCity = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onTimeChange = {},
                    onGoLive = {},
                )
            }
        }

        val labelled = composeRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .map { node ->
                val text = node.config.getOrNull(SemanticsProperties.Text)
                    ?.any { it.text.isNotBlank() } == true
                val description = node.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?.any { it.isNotBlank() } == true
                text || description
            }

        assertTrue("Expected to find interactive controls on the map screen", labelled.isNotEmpty())
        assertTrue("Every interactive control must have a text or content description", labelled.all { it })
    }

    private fun assertEveryControlIsLabelled() {
        composeRule
            .onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .forEach { node ->
                val text = node.config.getOrNull(SemanticsProperties.Text)
                    ?.any { it.text.isNotBlank() } == true
                val description = node.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?.any { it.isNotBlank() } == true
                assertTrue(
                    "Every interactive control must have a text or content description",
                    text || description,
                )
            }
    }

    @Test
    fun criterion11c_theEmptyAndErrorStatesAreReachable() {
        // Empty list.
        composeRule.setContent {
            FixedClockwiseTheme {
                WorldMapScreen(
                    map = PreviewSamples.emptyMapState(),
                    onSelectCity = {},
                    onOpenCity = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onTimeChange = {},
                    onGoLive = {},
                )
            }
        }
        composeRule.onNodeWithText("No cities on the map").assertIsDisplayed()
        composeRule.onNodeWithText("Find a city").assertIsDisplayed()

        // Loading state is a distinct, labelled state.
        assertNotNull(PreviewSamples.emptyState())
    }

    // ---------------------------------------------------------------- 12
    // Phase 2 — theme support (extended to the four themes).
    //
    // 12a is persistence and migration, 12b the theme actually reaching the
    // screen, 12c the picker that chooses it.

    /**
     * Criterion 12a — the chosen theme is persisted, survives a restart, and
     * leaves the saved cities untouched.
     *
     * The restart itself is covered by `ClockRepositoryTest.stateSurvivesARestart`;
     * here the *stored message* is encoded and decoded to prove the choice is in
     * the bytes DataStore puts on disk.
     */
    @Test
    fun criterion12a_theChosenThemeIsPersistedAndSurvivesARestart() = runTest {
        val store = FakeSavedCitiesDataStore()
        val repository = ClockRepository(store)

        // Nothing chosen yet, so the first of the four themes applies.
        assertEquals(AppTheme.LIGHT_MONOCHROME, AppTheme.Default)
        assertEquals(AppTheme.Default, repository.preferences.first().theme)

        repository.setTheme(AppTheme.COOL_SLATE)
        assertEquals(AppTheme.COOL_SLATE, repository.preferences.first().theme)

        val before = repository.preferences.first()
        val after = SavedCities.parseFrom(store.data.first().toByteArray()).toUserPreferences()
        assertEquals(AppTheme.COOL_SLATE, after.theme)
        assertEquals(before, after)

        // Changing the theme must not disturb anything else.
        assertEquals(CityDirectory.defaultSavedCityIds, after.savedCityIds)
        assertEquals(CityDirectory.defaultActiveCityId, after.activeCityId)
        assertTrue(after.use24Hour)
    }

    /**
     * Criterion 12a (continued) — a store written by the three-mode build is
     * migrated rather than reset: light *was* the light monochrome palette, so
     * those users keep what they were looking at.
     *
     * `DARK` was True Dark, retired in 3.1.0, and `SYSTEM` followed the device
     * and cannot be resolved after the fact; both land on the default rather
     * than guessing a theme the user never chose.
     */
    @Test
    fun criterion12a_anOlderStoreIsMigratedToATheme() {
        fun themed(mode: SavedThemeMode) = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setThemeMode(mode)
            .build()
            .toUserPreferences()
            .theme

        assertEquals(AppTheme.Default, themed(SavedThemeMode.THEME_MODE_DARK))
        assertEquals(AppTheme.LIGHT_MONOCHROME, themed(SavedThemeMode.THEME_MODE_LIGHT))
        assertEquals(AppTheme.Default, themed(SavedThemeMode.THEME_MODE_SYSTEM))
    }

    /**
     * Criterion 12b — the chosen theme reaches the screens: each of the fixed
     * themes provides its own palette, and a change of theme repaints without
     * recreating anything. Material You resolves against the device, so it is
     * covered by its own test below.
     */
    @Test
    fun criterion12b_theChosenThemePaintsTheScreenWithTheMatchingPalette() {
        var theme by mutableStateOf(AppTheme.LIGHT_MONOCHROME)
        var palette: ClockwisePalette? = null

        composeRule.setContent {
            FixedClockwiseTheme(theme = theme) {
                palette = LocalClockwisePalette.current
            }
        }

        AppTheme.entries.filter { it != AppTheme.MATERIAL_YOU }.forEach { candidate ->
            composeRule.runOnIdle { theme = candidate }
            composeRule.runOnIdle {
                assertEquals(paletteFor(candidate), palette)
                // The four shipped themes are all light surfaces; only a
                // wallpaper scheme can be dark.
                assertEquals(
                    "every fixed theme paints a light surface",
                    false,
                    palette!!.isDark,
                )
            }
        }
    }

    /**
     * Criterion 12b (continued) — below Android 12 there is no wallpaper
     * scheme, so Material You falls back to the Material 3 baseline instead of
     * quietly painting one of the monochrome themes.
     */
    @Test
    @Config(sdk = [30], qualifiers = "w360dp-h800dp-notnight-xhdpi")
    fun criterion12b_materialYouFallsBackToTheStaticPaletteBelowAndroidTwelve() {
        var palette: ClockwisePalette? = null

        composeRule.setContent {
            FixedClockwiseTheme(theme = AppTheme.MATERIAL_YOU) {
                palette = LocalClockwisePalette.current
            }
        }

        composeRule.runOnIdle { assertEquals(MaterialYouFallbackPalette, palette) }
    }

    /**
     * Criterion 12b (continued) — the device's own setting no longer decides
     * anything. The same theme has to paint the same palette on a phone in dark
     * mode as on one in light mode, which is the whole point of choosing an
     * exact palette rather than following the system.
     */
    @Test
    @Config(sdk = [34], qualifiers = "w360dp-h800dp-night-xhdpi")
    fun criterion12b_theDeviceSettingNoLongerPicksThePalette() {
        var palette: ClockwisePalette? = null

        composeRule.setContent {
            FixedClockwiseTheme(theme = AppTheme.WARM_SEPIA) {
                palette = LocalClockwisePalette.current
            }
        }

        composeRule.runOnIdle {
            assertEquals(WarmSepiaClockwisePalette, palette)
            assertEquals(false, palette!!.isDark)
        }
    }

    /**
     * Criterion 12c — the settings sheet offers Clockwise's three theme modes,
     * reports which one is in force, and hands the new choice back to the
     * caller. The retired fixed palettes must not appear.
     */
    @Test
    fun criterion12c_theSettingsSheetOffersEveryThemeModeAndReportsTheSelection() {
        var selected: ThemeMode? = null

        composeRule.setContent {
            ClockwiseTheme {
                SettingsContent(
                    themeMode = ThemeMode.DARK,
                    onThemeModeSelected = { selected = it },
                    use24Hour = true,
                    onUse24HourChange = {},
                )
            }
        }

        composeRule.onNodeWithText("THEME MODE").assertIsDisplayed()
        composeRule.onNodeWithText("System").assertIsDisplayed().assertIsNotSelected()
        composeRule.onNodeWithText("Light").assertIsDisplayed().assertIsNotSelected()
        composeRule.onNodeWithText("Dark").assertIsSelected()
        composeRule.onNodeWithText("Light Monochrome").assertDoesNotExist()
        composeRule.onNodeWithText("Warm Sepia").assertDoesNotExist()

        composeRule.onNodeWithText("Light").performClick()

        assertEquals(ThemeMode.LIGHT, selected)
        assertEveryControlIsLabelled()
    }

    // ---------------------------------------------------------------- 13
    // Phase 2 — the time difference indicator.

    /**
     * Criterion 13a — the difference is the gap between the two zones' offsets
     * at the same instant, in minutes, so half-hour and quarter-hour zones are
     * exact and daylight saving is picked up automatically.
     */
    @Test
    fun criterion13a_theDifferenceIsTheOffsetGapAtThatInstant() {
        val yangon = ZoneId.of("Asia/Yangon")
        fun difference(cityId: String, at: Instant = winter) = TimeDifference.between(
            zoneId = ZoneId.of(CityDirectory.find(cityId)!!.timeZone),
            referenceZoneId = yangon,
            instant = at,
        )

        assertEquals(150, difference("tokyo").minutes) // UTC+9 against UTC+6:30
        assertEquals(270, difference("sydney").minutes) // UTC+11
        assertEquals(-870, difference("los-angeles").minutes) // UTC-8
        assertEquals(-690, difference("new-york").minutes) // UTC-5
        assertEquals(-390, difference("london").minutes) // UTC+0
        assertEquals(-60, difference("mumbai").minutes) // UTC+5:30
        assertEquals(30, difference("bangkok").minutes) // UTC+7
        assertTrue(difference("yangon").isSameTime)

        // London moves to UTC+1 in the northern summer.
        assertEquals(-330, difference("london", summer).minutes)
    }

    /**
     * Criterion 13b — the indicator reads "+N hrs ahead", "-N hrs behind" or
     * "Same time", and keeps the minutes a half-hour zone needs.
     */
    @Test
    fun criterion13b_theDifferenceReadsAheadBehindOrSameTime() {
        val samples = listOf(
            TimeDifference(0),
            TimeDifference(150),
            TimeDifference(-300),
            TimeDifference(330),
            TimeDifference(-30),
            TimeDifference(15),
        )

        composeRule.setContent {
            FixedClockwiseTheme {
                Column { samples.forEach { TimeDifferenceSample(it) } }
            }
        }

        composeRule.onNodeWithText("Same time").assertIsDisplayed()
        composeRule.onNodeWithText("+2 hrs 30 mins ahead").assertIsDisplayed()
        composeRule.onNodeWithText("-5 hrs behind").assertIsDisplayed()
        composeRule.onNodeWithText("+5 hrs 30 mins ahead").assertIsDisplayed()
        composeRule.onNodeWithText("-30 mins behind").assertIsDisplayed()
        composeRule.onNodeWithText("+15 mins ahead").assertIsDisplayed()
    }

    /**
     * Criterion 13c — every city card carries the difference, computed from the
     * device's own time.
     *
     * Visual proof: `clocks_screen_labelled_360dp.png`,
     * `map_screen_labelled_360dp.png`.
     */
    @Test
    fun criterion13c_everyCityCardShowsTheDifferenceFromTheDeviceTime() {
        val state = PreviewSamples.clockState(compareWithLocalTime = true)

        assertEquals(150, state.cities.single { it.id == "tokyo" }.difference.minutes)
        assertEquals(-870, state.cities.single { it.id == "los-angeles" }.difference.minutes)

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

        composeRule.onNodeWithText("+2 hrs 30 mins ahead").assertIsDisplayed()
        composeRule.onNodeWithText("-14 hrs 30 mins behind").assertIsDisplayed()
        composeRule.onNodeWithText("-6 hrs 30 mins behind").assertIsDisplayed()
    }

    /**
     * Criterion 13d — the detail screen shows the difference and names the
     * reference it is measured against, so the number is never ambiguous.
     *
     * Visual proof: `city_detail_360dp.png`, `city_detail_labelled_360dp.png`.
     */
    @Test
    fun criterion13d_theDetailScreenNamesTheReferenceItComparesWith() {
        val state = PreviewSamples.clockState()

        composeRule.setContent {
            FixedClockwiseTheme {
                CityDetailScreen(
                    city = CityDirectory.find("tokyo")!!,
                    now = state.now,
                    use24Hour = state.use24Hour,
                    deviceLocal = state.deviceLocal,
                    label = null,
                    onBack = {},
                    onToggleFormat = {},
                    onEditLabel = {},
                    onRemove = {},
                )
            }
        }

        composeRule.onNodeWithText("COMPARED WITH YOUR LOCAL TIME").assertIsDisplayed()
        composeRule.onNodeWithText("+2 hrs 30 mins ahead").assertIsDisplayed()
        // The device's own zone is on screen too, which is the thing compared with.
        composeRule.onNodeWithText("Asia/Yangon · UTC+6:30").assertIsDisplayed()
    }

    // ---------------------------------------------------------------- 14
    // Phase 2 — custom city labels.

    /**
     * Criterion 14a — a label is stored against the city id, survives a
     * restart, can be changed or cleared, and never disturbs anything else.
     */
    @Test
    fun criterion14a_aCityLabelIsPersistedAndCleared() = runTest {
        val store = FakeSavedCitiesDataStore()
        val repository = ClockRepository(store)
        repository.preferences.first()

        repository.setCityLabel("tokyo", "Head Office")
        assertEquals(mapOf("tokyo" to "Head Office"), repository.preferences.first().cityLabels)

        // The label is in the bytes DataStore actually writes.
        val decoded = SavedCities.parseFrom(store.data.first().toByteArray()).toUserPreferences()
        assertEquals("Head Office", decoded.cityLabels["tokyo"])
        assertEquals(CityDirectory.defaultSavedCityIds, decoded.savedCityIds)
        assertEquals(CityDirectory.defaultActiveCityId, decoded.activeCityId)

        // A label is per city.
        repository.setCityLabel("new-york", "Client calls")
        assertEquals("Head Office", repository.preferences.first().cityLabels["tokyo"])

        // And it can be changed and cleared.
        repository.setCityLabel("tokyo", "HQ")
        assertEquals("HQ", repository.preferences.first().cityLabels["tokyo"])

        repository.setCityLabel("tokyo", null)
        val after = repository.preferences.first()
        assertNull(after.cityLabels["tokyo"])
        assertEquals("Client calls", after.cityLabels["new-york"])
    }

    /**
     * Criterion 14b — the label reaches the screens as the name followed by the
     * label, leaving unlabelled cities alone.
     *
     * Visual proof: `clocks_screen_labelled_360dp.png`,
     * `city_detail_labelled_360dp.png`.
     */
    @Test
    fun criterion14b_theLabelIsShownAfterTheCityName() {
        val state = PreviewSamples.labelledState()

        assertEquals("Tokyo (Head Office)", state.cities.single { it.id == "tokyo" }.displayName)
        assertEquals(
            "New York (HQ)",
            state.cities.single { it.id == "new-york" }.displayName,
        )
        assertEquals("London", state.cities.single { it.id == "london" }.displayName)

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

        composeRule.onNodeWithText("Tokyo (Head Office)").assertIsDisplayed()
        composeRule.onNodeWithText("New York (HQ)").assertIsDisplayed()
        composeRule.onNodeWithText("London").assertIsDisplayed()
    }

    /**
     * Criterion 14c — the whole edit loop, driven through the real app shell: the
     * dialog opens from the detail screen, saves a label, shows it, and removes
     * it again.
     *
     * Visual proof: `city_label_dialog_360dp.png`.
     */
    @Test
    fun criterion14c_theEditDialogSavesAndRemovesALabel() {
        val store = FakeSavedCitiesDataStore()
        val viewModel = ClockwiseViewModel(
            repository = ClockRepository(store),
            ticker = flowOf(winter),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )

        composeRule.setContent {
            FixedClockwiseTheme {
                ClockwiseRoot(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Tokyo").fetchSemanticsNodes().isNotEmpty()
        }

        // Open Tokyo's detail screen, then its label dialog.
        composeRule.onNodeWithText("Tokyo").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("ADD LABEL").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("ADD LABEL").performClick()

        composeRule.onNodeWithText("City label").assertIsDisplayed()
        composeRule.onNodeWithTag(CityLabelDialogTags.FIELD).performTextInput("Head Office")
        composeRule.onNodeWithText("Save").performClick()

        // The label is stored and rendered as "Tokyo (Head Office)".
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("(Head Office)").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("EDIT LABEL").assertIsDisplayed()

        // Re-opening the dialog offers removal, which clears it again.
        composeRule.onNodeWithText("EDIT LABEL").performClick()
        composeRule.onNodeWithText("Remove label").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("ADD LABEL").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("(Head Office)").assertDoesNotExist()
    }

    // ---- Phase 2, stage 3: the meeting planner ----

    /**
     * Criterion 15a — the Clocks screen offers a meeting planner, and opening it
     * puts a 24-hour slider on screen set to the current time.
     *
     * Driven through the real app shell, so this covers the entry row, the route
     * and the control in one pass.
     *
     * Visual proof: `planner_screen_360dp.png`.
     */
    @Test
    fun criterion15a_theClocksScreenOffersAFullDayMeetingPlanner() {
        val store = FakeSavedCitiesDataStore()
        val viewModel = ClockwiseViewModel(
            repository = ClockRepository(store),
            ticker = flowOf(winter),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )

        composeRule.setContent {
            FixedClockwiseTheme {
                ClockwiseRoot(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("PLAN A MEETING").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("PLAN A MEETING").performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Meeting planner").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("YOUR LOCAL TIME").assertIsDisplayed()
        composeRule.onNodeWithText("YOUR CITIES AT THIS TIME").assertIsDisplayed()

        // The slider covers the whole day in quarter hours: 00:00 through 23:45,
        // so the last slot never rolls over into tomorrow.
        val yangon = ZoneId.of("Asia/Yangon")
        val opensOn = MinuteOfDay.ofInstant(winter, yangon).snappedToGrid()
        // 23:10 rounds to the next quarter hour, not down.
        assertEquals(MinuteOfDay.of(23, 15), opensOn)

        val slider = composeRule.onNodeWithTag(DaySliderTags.TRACK)
        slider.assertRangeInfoEquals(
            ProgressBarRangeInfo(
                current = opensOn.value.toFloat(),
                range = 0f..MinuteOfDay.LAST_GRID_VALUE.toFloat(),
                steps = MinuteOfDay.GRID_STEPS,
            ),
        )
        // It opens on the clock, so the planner agrees with the list behind it,
        // and the whole plan is already drawn for that slot.
        slider.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "23:15"),
        )
        composeRule
            .onNodeWithContentDescription("Meeting time in Tokyo: Sun 01:45, outside working hours")
            .assertIsDisplayed()
    }

    /**
     * Criterion 15b — the slot is the single input to every row: sweeping the
     * slider moves all the saved cities at once, each by the same amount, and
     * every row reads that city's own clock at the slot's instant.
     *
     * Checked across all 96 slots of the day, against times computed here rather
     * than taken from the code under test.
     */
    @Test
    fun criterion15b_everyCityMovesWithTheSlot() {
        val state = PreviewSamples.clockState()
        val deviceZone = PreviewSamples.sampleDeviceZoneId
        val referenceDate = state.now.atZone(deviceZone).toLocalDate()

        val slots = (MinuteOfDay.MIN_VALUE..MinuteOfDay.LAST_GRID_VALUE step MinuteOfDay.STEP_MINUTES)
            .map { MinuteOfDay(it) }
        assertEquals("96 quarter-hours in a day", 96, slots.size)

        for (cityId in CityDirectory.defaultSavedCityIds) {
            val zoneId = ZoneId.of(CityDirectory.find(cityId)!!.timeZone)
            var previousMinute: Int? = null

            for (slot in slots) {
                val row = buildPlannerUiState(state, slot, deviceZone)
                    .rows.single { it.id == cityId }

                // The slot is a time of day where the user is; the city's reading
                // is that same instant seen through the city's own zone.
                val local = slot.onDate(referenceDate, deviceZone).atZone(zoneId)
                val minuteOfDay = local.hour * 60 + local.minute

                // The row is this city's clock at the slot's instant.
                assertEquals(
                    "$cityId at ${slot.value}",
                    "%02d:%02d".format(local.hour, local.minute),
                    row.timeText,
                )

                // A quarter-hour step of the slider is a quarter-hour step in
                // every city — nobody is left showing the previous slot and
                // nobody is rounded to the hour.
                previousMinute?.let { previous ->
                    assertEquals(
                        "$cityId stepping from ${slot.value - MinuteOfDay.STEP_MINUTES}",
                        (previous + MinuteOfDay.STEP_MINUTES) % 1440,
                        minuteOfDay,
                    )
                }
                previousMinute = minuteOfDay
            }
        }
    }

    /**
     * Criterion 15c — the slider can be driven by a finger and by a screen
     * reader, and the plan follows the movement live rather than on release.
     *
     * Visual proof: `planner_screen_dark_360dp.png`.
     */
    @Test
    fun criterion15c_theSliderIsDrivableAndThePlanFollowsItLive() {
        var slot by mutableStateOf(MinuteOfDay.of(9, 0))
        val state = PreviewSamples.clockState()

        composeRule.setContent {
            FixedClockwiseTheme {
                MeetingPlannerScreen(
                    planner = buildPlannerUiState(state, slot, PreviewSamples.sampleDeviceZoneId),
                    onMinuteChange = { slot = it },
                    onSourceChange = {},
                    onBack = {},
                    onFindCityClick = {},
                )
            }
        }

        // 09:00 in Yangon: Tokyo is at work, and California is on the previous
        // evening and out of hours.
        composeRule
            .onNodeWithContentDescription("Meeting time in Tokyo: Sat 11:30, within working hours")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(
                "Meeting time in Los Angeles: Fri 18:30, outside working hours",
            )
            .assertIsDisplayed()

        // A touch anywhere on the strip jumps there: halfway along is midday.
        val node = composeRule.onNodeWithTag(DaySliderTags.TRACK).fetchSemanticsNode()
        val inset = with(composeRule.density) { 10.dp.toPx() }
        composeRule.onNodeWithTag(DaySliderTags.TRACK).performTouchInput {
            click(Offset(inset + (node.size.width - 2 * inset) / 2f, center.y))
        }
        assertEquals(MinuteOfDay.of(12, 0), slot)
        composeRule
            .onNodeWithContentDescription("Meeting time in Tokyo: Sat 14:30, within working hours")
            .assertIsDisplayed()

        // And so does TalkBack's adjustable-progress action, which is the only
        // way a screen-reader user can move it.
        composeRule.onNodeWithTag(DaySliderTags.TRACK)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(90f) }
        assertEquals(MinuteOfDay.of(1, 30), slot)
        composeRule
            .onNodeWithContentDescription("Meeting time in Tokyo: Sat 04:00, outside working hours")
            .assertIsDisplayed()
    }

    /**
     * Criterion 15d — the planner says which cities are inside a working day,
     * judges that in each city's own zone, and marks the day each city is on.
     *
     * Visual proof: `planner_screen_360dp.png` (filled marker = at work),
     * `planner_screen_dark_360dp.png`.
     */
    @Test
    fun criterion15d_thePlannerNamesTheWorkingDayInEachCitysOwnZone() {
        val state = PreviewSamples.clockState()

        val morning = buildPlannerUiState(
            state = state,
            minuteOfDay = MinuteOfDay.of(9, 0),
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )
        assertTrue(morning.rows.single { it.id == "tokyo" }.isWorkingHours)
        assertTrue(morning.rows.single { it.id == "sydney" }.isWorkingHours)
        assertFalse(morning.rows.single { it.id == "los-angeles" }.isWorkingHours)
        assertFalse(morning.rows.single { it.id == "new-york" }.isWorkingHours)
        assertFalse(morning.rows.single { it.id == "london" }.isWorkingHours)

        // Same city, same slot, different judgement once the day moves on.
        val evening = buildPlannerUiState(
            state = state,
            minuteOfDay = MinuteOfDay.of(18, 0),
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )
        assertFalse(evening.rows.single { it.id == "tokyo" }.isWorkingHours)
        assertTrue(evening.rows.single { it.id == "london" }.isWorkingHours)

        // Cities behind the date line are marked with their own weekday.
        assertEquals("Sat", morning.rows.single { it.id == "london" }.weekdayLabel)
        assertEquals("Fri", morning.rows.single { it.id == "los-angeles" }.weekdayLabel)

        // The rule is stated on screen, not applied silently.
        composeRule.setContent {
            FixedClockwiseTheme {
                MeetingPlannerScreen(
                    planner = morning,
                    onMinuteChange = {},
                    onSourceChange = {},
                    onBack = {},
                    onFindCityClick = {},
                )
            }
        }
        composeRule.onNodeWithText("WITHIN 09:00–18:00").assertIsDisplayed()
        composeRule.onNodeWithText("OUTSIDE").assertIsDisplayed()
        assertEquals(WORKING_DAY_START_HOUR, 9)
        assertEquals(WORKING_DAY_END_HOUR, 18)
    }

    /**
     * Criterion 15e — the planner is a true converter: the slot can be read in
     * a saved city's zone instead of the device's, and every difference is then
     * measured from that source.
     *
     * Visual proof: `planner_screen_source_360dp.png`.
     */
    @Test
    fun criterion15e_thePlannerConvertsFromAChosenSourceCity() {
        val state = PreviewSamples.clockState()
        val fromTokyo = buildPlannerUiState(
            state = state,
            minuteOfDay = MinuteOfDay.of(9, 0),
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
            sourceCityId = "tokyo",
        )

        assertEquals("tokyo", fromTokyo.sourceCityId)
        assertEquals("Tokyo", fromTokyo.sourceName)
        assertEquals(
            listOf(null, "tokyo", "sydney", "los-angeles", "new-york", "london"),
            fromTokyo.sources.map { it.cityId },
        )

        // 09:00 in Tokyo is still the 20th in California and the 21st in
        // Tokyo itself; the source date follows the source city.
        assertEquals("Sun, 21 Jan", fromTokyo.referenceDateText)
        assertEquals("UTC+9", fromTokyo.referenceOffsetLabel)
        assertEquals("09:00", fromTokyo.rows.single { it.id == "tokyo" }.timeText)
        assertEquals("16:00", fromTokyo.rows.single { it.id == "los-angeles" }.timeText)
        assertEquals("Sat", fromTokyo.rows.single { it.id == "los-angeles" }.weekdayLabel)

        // Differences are from Tokyo now, not from the device.
        assertEquals(TimeDifference(0), fromTokyo.rows.single { it.id == "tokyo" }.difference)
        assertEquals(
            TimeDifference(-1020),
            fromTokyo.rows.single { it.id == "los-angeles" }.difference,
        )

        // And the loop is real: the chip row changes the source, the headline
        // follows it, and switching back to LOCAL restores the device reading.
        var sourceCityId by mutableStateOf<String?>("tokyo")
        composeRule.setContent {
            FixedClockwiseTheme {
                MeetingPlannerScreen(
                    planner = buildPlannerUiState(
                        state = state,
                        minuteOfDay = MinuteOfDay.of(9, 0),
                        deviceZoneId = PreviewSamples.sampleDeviceZoneId,
                        sourceCityId = sourceCityId,
                    ),
                    onMinuteChange = {},
                    onSourceChange = { sourceCityId = it },
                    onBack = {},
                    onFindCityClick = {},
                )
            }
        }

        composeRule.onNodeWithText("TIME IN TOKYO").assertIsDisplayed()
        composeRule.onNodeWithTag(PlannerTestTags.sourceChip("tokyo")).assertIsSelected()
        composeRule
            .onNodeWithContentDescription("Meeting time in Tokyo: Sun 09:00, within working hours")
            .assertIsDisplayed()

        composeRule.onNodeWithTag(PlannerTestTags.sourceChip(null)).performClick()
        assertNull(sourceCityId)
        composeRule.onNodeWithText("YOUR LOCAL TIME").assertIsDisplayed()
        composeRule.onNodeWithTag(PlannerTestTags.sourceChip(null)).assertIsSelected()
        composeRule
            .onNodeWithContentDescription("Meeting time in Tokyo: Sat 11:30, within working hours")
            .assertIsDisplayed()
    }

    /**
     * Criterion 15f — the planner recommends the quarter-hour slot that puts
     * the most cities inside the working day, and the recommendation is a
     * button that moves the whole plan to it.
     *
     * Visual proof: `planner_screen_360dp.png` (BEST action in the header).
     */
    @Test
    fun criterion15f_theBestOverlapIsRecommendedAndAppliedToThePlan() {
        val state = PreviewSamples.clockState()
        val initial = buildPlannerUiState(
            state = state,
            minuteOfDay = MinuteOfDay.of(9, 0),
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )

        // Three cities are at work at 06:30, and no other three-city slot is
        // closer to their collective noon.
        assertEquals(MinuteOfDay.of(6, 30), initial.bestSlot?.minuteOfDay)
        assertEquals(3, initial.bestWorkingCount)

        var slot by mutableStateOf(MinuteOfDay.of(9, 0))
        composeRule.setContent {
            FixedClockwiseTheme {
                MeetingPlannerScreen(
                    planner = buildPlannerUiState(
                        state = state,
                        minuteOfDay = slot,
                        deviceZoneId = PreviewSamples.sampleDeviceZoneId,
                    ),
                    onMinuteChange = { slot = it },
                    onSourceChange = {},
                    onBack = {},
                    onFindCityClick = {},
                )
            }
        }

        composeRule.onNodeWithText("BEST 06:30").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Best time 06:30: 3 cities within working hours")
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PlannerTestTags.BEST_ACTION).performClick()

        assertEquals(MinuteOfDay.of(6, 30), slot)
        // The whole plan has moved, not just the headline: Tokyo's morning and
        // California's previous afternoon are now both inside working hours.
        composeRule
            .onNodeWithContentDescription("Meeting time in Tokyo: Sat 09:00, within working hours")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(
                "Meeting time in Los Angeles: Fri 16:00, within working hours",
            )
            .assertIsDisplayed()
    }

    /**
     * Criterion 15g — when no slot can put two cities at work together, the
     * planner says so instead of inventing a recommendation.
     */
    @Test
    fun criterion15g_noWorkingHoursOverlapIsStatedRatherThanInvented() {
        val state = PreviewSamples.clockState(savedCityIds = listOf("tokyo", "new-york"))
        val planner = buildPlannerUiState(
            state = state,
            minuteOfDay = MinuteOfDay.of(9, 0),
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )

        assertNull(planner.bestSlot)
        assertEquals(0, planner.bestWorkingCount)

        composeRule.setContent {
            FixedClockwiseTheme {
                MeetingPlannerScreen(
                    planner = planner,
                    onMinuteChange = {},
                    onSourceChange = {},
                    onBack = {},
                    onFindCityClick = {},
                )
            }
        }

        composeRule.onNodeWithText("NO OVERLAP").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("No time puts two or more cities within working hours")
            .assertIsDisplayed()
    }

    // ---- Phase 2, stage 4: the home-screen widget ----

    /**
     * Criterion 16a — the widget is declared the way Android requires, and its
     * content comes from the same store and mapper as the screens.
     *
     * The provider info is read back from the resource the launcher reads, not
     * from a copy of the values, so a change to the XML that breaks the default
     * footprint or the backstop fails here.
     */
    @Test
    fun criterion16a_theWidgetIsDeclaredAndReadsTheSavedCitiesFromDataStore() {
        val receiver = ComponentName(context, ClockwiseWidgetReceiver::class.java)
        val info = context.packageManager.getReceiverInfo(
            receiver,
            PackageManager.GET_META_DATA,
        )
        assertTrue("the launcher must be able to reach the provider", info.exported)
        assertEquals(
            R.xml.clockwise_widget_info,
            info.metaData.getInt("android.appwidget.provider"),
        )

        val updateReceiver = context.packageManager.queryBroadcastReceivers(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setPackage(context.packageName),
            0,
        )
        assertTrue(
            "the provider must listen for APPWIDGET_UPDATE",
            updateReceiver.any {
                it.activityInfo?.name == ClockwiseWidgetReceiver::class.java.name
            },
        )

        val ticker = context.packageManager.getReceiverInfo(
            ComponentName(context, WidgetTickerReceiver::class.java),
            0,
        )
        assertFalse("the minute ticker is app-private", ticker.exported)

        val parser = context.resources.getXml(R.xml.clockwise_widget_info)
        while (parser.eventType != XmlPullParser.END_DOCUMENT &&
            parser.eventType != XmlPullParser.START_TAG
        ) {
            parser.next()
        }
        assertEquals("appwidget-provider", parser.name)

        val ns = "http://schemas.android.com/apk/res/android"

        // Compiled dimension attributes are typed values, not integer literals,
        // so they are read back as their string form ("250.0dp") and parsed.
        fun dp(name: String): Float {
            val raw = parser.getAttributeValue(ns, name)
                ?: throw AssertionError("the provider info must declare $name")
            return raw.trimEnd { it.isLetter() }.toFloat()
        }

        assertEquals(250f, dp("minWidth"), 0.01f)
        assertEquals(250f, dp("minHeight"), 0.01f)
        assertEquals(4, parser.getAttributeIntValue(ns, "targetCellWidth", -1))
        assertEquals(4, parser.getAttributeIntValue(ns, "targetCellHeight", -1))
        assertEquals(1_800_000, parser.getAttributeIntValue(ns, "updatePeriodMillis", -1))
        // Flags are compiled to their bitmask: horizontal|vertical, home_screen.
        assertEquals(0x3, parser.getAttributeIntValue(ns, "resizeMode", -1))
        assertEquals(0x1, parser.getAttributeIntValue(ns, "widgetCategory", -1))
        assertEquals(
            R.drawable.widget_preview,
            parser.getAttributeResourceValue(ns, "previewImage", 0),
        )
        assertEquals(
            R.string.widget_description,
            parser.getAttributeResourceValue(ns, "description", 0),
        )
        assertEquals(
            context.resources.getIdentifier(
                "glance_default_loading_layout",
                "layout",
                context.packageName,
            ),
            parser.getAttributeResourceValue(ns, "initialLayout", 0),
        )
        parser.close()

        // The widget and the Clocks screen ask the same repository and run the
        // same mapper, so the same saved cities read identically.
        val preferences = runBlocking {
            ClockRepository(FakeSavedCitiesDataStore()).preferences.first()
        }
        val widget = buildWidgetUiState(
            savedCityIds = preferences.savedCityIds,
            cityLabels = preferences.cityLabels,
            use24Hour = preferences.use24Hour,
            now = winter,
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )
        val clocks = buildClockwiseUiState(
            savedCityIds = preferences.savedCityIds,
            activeCityId = preferences.activeCityId,
            cityLabels = preferences.cityLabels,
            use24Hour = preferences.use24Hour,
            now = winter,
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )

        assertEquals(clocks.cities.map { it.id }, widget.rows.map { it.id })
        assertEquals(clocks.cities.map { it.timeText }, widget.rows.map { it.timeText })
    }

    /**
     * Criterion 16b — every saved city with its own time, in the app-wide
     * format, with labels, date-line days and an honest count past the budget.
     */
    @Test
    fun criterion16b_theWidgetListsTheSavedCitiesWithTheirTimesAndCountsTheRest() {
        val widget = buildWidgetUiState(
            savedCityIds = CityDirectory.defaultSavedCityIds,
            cityLabels = mapOf("tokyo" to "Head Office"),
            use24Hour = true,
            now = winter,
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )

        assertEquals(
            listOf("01:40", "03:40", "08:40", "11:40", "16:40"),
            widget.rows.map { it.timeText },
        )
        assertEquals("Tokyo (Head Office)", widget.rows.first().displayName)
        assertTrue("Tokyo has crossed the date line", widget.rows.first().isAnotherDay)
        assertFalse(widget.rows.last().isAnotherDay)
        assertEquals("23:10", widget.deviceTimeText)

        val twelveHour = buildWidgetUiState(
            savedCityIds = CityDirectory.defaultSavedCityIds,
            use24Hour = false,
            now = winter,
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )
        assertEquals("1:40 AM", twelveHour.rows.first().timeText)

        // Eight cities do not fit beside the count line at 4x4: five rows and
        // three counted, never dropped.
        val eightCities = CityDirectory.defaultSavedCityIds +
            listOf("paris", "dubai", "mumbai")
        val budget = widgetRowBudget(
            availableHeightDp = 250,
            savedCityCount = eightCities.size,
        )
        assertEquals(5, budget)
        assertEquals(
            "+3 more cities",
            context.resources.getQuantityString(
                R.plurals.widget_more_cities,
                eightCities.size - budget,
                eightCities.size - budget,
            ),
        )
    }

    /**
     * Criterion 16c — the card is drawn: the device clock anchors the rows, the
     * cities carry the app's own times and markers, and the header opens the app.
     */
    @Test
    fun criterion16c_theCardDrawsTheCitiesAndOpensTheApp() {
        val state = buildWidgetUiState(
            savedCityIds = CityDirectory.defaultSavedCityIds,
            use24Hour = true,
            now = winter,
            deviceZoneId = PreviewSamples.sampleDeviceZoneId,
        )

        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(DpSize(250.dp, 250.dp))
            provideComposable { ClockwiseWidgetContent(state) }

            onNode(hasTextEqualTo("23:10")).assertExists()
            onNode(hasTextEqualTo("Tokyo")).assertExists()
            onNode(hasTextEqualTo("01:40")).assertExists()
            // Tokyo (01:40 Sun) and Sydney (03:40 Sun) have crossed the date
            // line; both rows carry the weekday, so there are two of them.
            onAllNodes(hasTextEqualTo("Sun")).assertCountEquals(2)
            // Three cities are in daylight and two are not; the marker is per
            // row, so the counts are part of what the card draws.
            onAllNodes(hasContentDescriptionEqualTo(context.getString(R.string.a11y_daytime)))
                .assertCountEquals(3)
            onAllNodes(hasContentDescriptionEqualTo(context.getString(R.string.a11y_nighttime)))
                .assertCountEquals(2)
            onNode(hasTestTag(WidgetTestTags.HEADER))
                .assertHasStartActivityClickAction<MainActivity>()
        }

        // The widget follows the device's light/dark setting, not the app's
        // stored four-theme choice.
        val provider = DayNightColorProvider(
            day = LightClockwisePalette.card,
            night = NightClockwisePalette.card,
        )
        RuntimeEnvironment.setQualifiers("+notnight")
        assertEquals(LightClockwisePalette.card, provider.getColor(context))
        RuntimeEnvironment.setQualifiers("+night")
        assertEquals(NightClockwisePalette.card, provider.getColor(context))
        RuntimeEnvironment.setQualifiers("+notnight")
    }

    /**
     * Criterion 16d — a minute alarm while a widget exists, no alarm when there
     * is none, and an immediate redraw when the saved cities change.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun criterion16d_theWidgetRefreshesWithTheClockAndWithTheSavedCities() = runTest {
        val alarms = context.getSystemService(AlarmManager::class.java)
        scheduleWidgetTick(context, widgetsExist = true)
        val scheduled = shadowOf(alarms).peekNextScheduledAlarm()
        assertNotNull("a widget must arm the next minute", scheduled)
        assertEquals(AlarmManager.RTC, scheduled!!.type)
        assertEquals(0L, scheduled.triggerAtTime % 60_000L)

        scheduleWidgetTick(context, widgetsExist = false)
        assertNull("no widget means no minute alarm", shadowOf(alarms).peekNextScheduledAlarm())
        cancelWidgetTick(context)

        val repository = ClockRepository(FakeSavedCitiesDataStore())
        var redraws = 0
        val mirror = WidgetMirror(
            scope = this,
            repository = repository,
            refresh = { redraws += 1 },
            onError = { throw it },
        )
        val job = mirror.start()
        runCurrent()
        assertEquals(1, redraws)

        repository.addCity("paris")
        runCurrent()
        assertEquals("adding a city redraws before the next minute", 2, redraws)
        job.cancel()
    }

    /**
     * Criterion 16e — a widget row is a deep link: it opens the app on that
     * city's detail screen, not at the top of the list, and the pending id is
     * consumed once so a recomposition cannot replay it.
     *
     * The Intent extra itself is proved in `ClockwiseWidgetTest`; this drives
     * the app-side hand-off through the real shell.
     */
    @Test
    fun criterion16e_aWidgetTapOpensThatCitysDetailScreen() {
        val store = FakeSavedCitiesDataStore()
        val viewModel = ClockwiseViewModel(
            repository = ClockRepository(store),
            ticker = flowOf(winter),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )
        var opened = false

        composeRule.setContent {
            FixedClockwiseTheme {
                ClockwiseRoot(
                    viewModel = viewModel,
                    initialCityId = "tokyo",
                    onInitialCityOpened = { opened = true },
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("YOUR LOCAL TIME").fetchSemanticsNodes().isNotEmpty()
        }

        // The detail screen, not the list: Tokyo is the title and the app has
        // made it the active city.
        composeRule.onNodeWithText("Tokyo").assertIsDisplayed()
        composeRule.onNodeWithText("YOUR LOCAL TIME").assertIsDisplayed()
        assertTrue("the pending id must be consumed", opened)

        // And back leads to the city list, so the deep link did not replace the
        // app's own navigation.
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("CITIES").fetchSemanticsNodes().isNotEmpty()
        }
    }

    // ---- Phase 2, stage 5: integration, polish and verification ----

    /**
     * One of the surfaces phase 2 added. The stage 5 checks render them one at
     * a time in a single composition, because a JVM test may only set content
     * once.
     */
    private enum class Stage5Screen { Clocks, Planner, EmptyPlanner, Settings, LabelDialog, Detail }

    @Composable
    private fun Stage5Surface(surface: Stage5Screen, state: ClockwiseUiState) {
        when (surface) {
            Stage5Screen.Clocks -> ClocksScreen(
                state = state,
                onCityClick = {},
                onAddClick = {},
                onFindCityClick = {},
                onSettingsClick = {},
                onPlanMeetingClick = {},
            )

            Stage5Screen.Planner -> MeetingPlannerScreen(
                planner = PreviewSamples.plannerState(labels = true),
                onMinuteChange = {},
                onSourceChange = {},
                onBack = {},
                onFindCityClick = {},
            )

            Stage5Screen.EmptyPlanner -> MeetingPlannerScreen(
                planner = PreviewSamples.emptyPlannerState(),
                onMinuteChange = {},
                onSourceChange = {},
                onBack = {},
                onFindCityClick = {},
            )

            Stage5Screen.Settings -> SettingsContent(
                themeMode = ThemeMode.SYSTEM,
                onThemeModeSelected = {},
                use24Hour = true,
                onUse24HourChange = {},
            )

            Stage5Screen.LabelDialog -> Box(Modifier.fillMaxSize()) {
                CityLabelDialog(
                    cityName = "Tokyo",
                    currentLabel = "Head Office",
                    onSave = {},
                    onDismiss = {},
                )
            }

            Stage5Screen.Detail -> CityDetailScreen(
                city = CityDirectory.find("tokyo")!!,
                now = state.now,
                use24Hour = state.use24Hour,
                deviceLocal = state.deviceLocal,
                label = "Head Office",
                onBack = {},
                onToggleFormat = {},
                onEditLabel = {},
                onRemove = {},
            )
        }
    }

    /**
     * Criterion 17a — the appearance switch is wired into the running app, not
     * only into the sheet: choosing a mode in the shell repaints the whole shell
     * and is written back through the repository.
     *
     * The composition below is the same wiring `MainActivity` uses — the theme
     * wraps the shell and reads the same view model the sheet writes to — so this
     * is the app's real path rather than a shortcut. Dialogs and sheets are
     * covered by 14c (labels) and 17b (the sheet's own controls).
     */
    @Test
    fun criterion17a_theAppearanceChosenInTheShellRepaintsTheApp() {
        val viewModel = ClockwiseViewModel(
            repository = newRepository(),
            ticker = flowOf(winter),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )
        var palette: ClockwisePalette? = null

        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            ClockwiseTheme(themeMode = state.themeMode) {
                palette = LocalClockwisePalette.current
                ClockwiseRoot(viewModel = viewModel)
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Los Angeles").fetchSemanticsNodes().isNotEmpty()
        }

        // The gear on the Clocks screen opens the sheet, which starts on the
        // stored choice.
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("THEME MODE").assertIsDisplayed()
        composeRule.onNodeWithText("System").assertIsSelected()
        // The action rather than a tap: a bottom sheet is a window of its own,
        // and injected touches do not cross into it under the JVM test renderer.
        composeRule.onNodeWithText("Dark").performSemanticsAction(SemanticsActions.OnClick)

        // The choice reaches the palette the whole shell is drawn with...
        composeRule.waitUntil(timeoutMillis = 10_000) {
            palette?.isDark == true
        }
        // ...and the state every screen reads, which is the repository's.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.themeMode == ThemeMode.DARK
        }
        composeRule.onNodeWithText("Dark").assertIsSelected()
        composeRule.onNodeWithText("System").assertIsNotSelected()

        composeRule.onNodeWithText("Light").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            palette?.isDark == false
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            viewModel.uiState.value.themeMode == ThemeMode.LIGHT
        }
        composeRule.onNodeWithText("Light").assertIsSelected()
        composeRule.onNodeWithText("Dark").assertIsNotSelected()
    }

    /**
     * Criterion 17b — every control phase 2 added is at least 48 dp tall, which
     * is the guideline minimum for something a finger has to hit. The icon-only
     * controls, whose size the app sets, are checked in both directions.
     *
     * The small controls keep their drawn size and pad the *target* out to this;
     * that is what the check measures. Widths of text-sized controls are not
     * asserted, because the JVM test renderer's stub font measures text at close
     * to zero width — the PNGs are the evidence for those.
     */
    @Test
    fun criterion17b_everyPhaseTwoControlMeetsTheTouchTargetGuideline() {
        val state = PreviewSamples.labelledState()
        var surface by mutableStateOf(Stage5Screen.Clocks)

        composeRule.setContent {
            FixedClockwiseTheme {
                Stage5Surface(surface = surface, state = state)
            }
        }

        fun show(next: Stage5Screen) {
            composeRule.runOnIdle { surface = next }
        }

        // The list: the gear, the gallery shortcut, the planner entry, the add row.
        assertIconTargetIsBigEnough("Settings")
        assertIconTargetIsBigEnough("Widget gallery")
        assertTargetIsTallEnough("PLAN A MEETING")
        // The market strip pushes the bottom of the list past the fold, so the
        // lazy list has to be scrolled before the row is composed.
        composeRule.onNode(hasScrollToIndexAction())
            .performScrollToNode(hasText("ADD CITY"))
        assertTargetIsTallEnough("ADD CITY")

        show(Stage5Screen.Planner)
        assertIconTargetIsBigEnough("Back")
        composeRule.onNodeWithTag(DaySliderTags.TRACK)
            .assertHeightIsAtLeast(MinTouchTarget)

        show(Stage5Screen.EmptyPlanner)
        assertTargetIsTallEnough("Find a city")

        // The theme-mode picker: three modes, each a finger-sized target.
        show(Stage5Screen.Settings)
        assertTargetIsTallEnough("System")
        assertTargetIsTallEnough("Light")
        assertTargetIsTallEnough("Dark")

        // The label dialog's field.
        show(Stage5Screen.LabelDialog)
        composeRule.onNodeWithTag(CityLabelDialogTags.FIELD)
            .assertHeightIsAtLeast(MinTouchTarget)

        // The detail screen's label action.
        show(Stage5Screen.Detail)
        assertTargetIsTallEnough("EDIT LABEL")
    }

    private fun assertTargetIsTallEnough(text: String) {
        composeRule.onNodeWithText(text).assertHeightIsAtLeast(MinTouchTarget)
    }

    private fun assertIconTargetIsBigEnough(contentDescription: String) {
        composeRule.onNodeWithContentDescription(contentDescription)
            .assertHeightIsAtLeast(MinTouchTarget)
            .assertWidthIsAtLeast(MinTouchTarget)
    }

    /**
     * Criterion 17c — the phase 2 surfaces are structured for assistive
     * technology: every screen title and section header is a heading, the label
     * dialog names itself when it opens and names the city its field edits, and
     * a city card announces the time difference instead of only drawing it.
     *
     * The planner's slider semantics are criterion 15c.
     */
    @Test
    fun criterion17c_thePhaseTwoSurfacesAreStructuredForAssistiveTechnology() {
        val state = PreviewSamples.labelledState(compareWithLocalTime = true)
        var surface by mutableStateOf(Stage5Screen.Clocks)

        composeRule.setContent {
            FixedClockwiseTheme {
                Stage5Surface(surface = surface, state = state)
            }
        }

        fun show(next: Stage5Screen) {
            composeRule.runOnIdle { surface = next }
        }

        // The difference is spoken with the card, and says what it is measured
        // against.
        composeRule.onNodeWithContentDescription(
            "Compared with your local time: +2 hrs 30 mins ahead",
        ).assertExists()
        assertHeading("Clockwise")
        assertHeading("CITIES")

        show(Stage5Screen.Planner)
        assertHeading("Meeting planner")
        assertHeading("YOUR LOCAL TIME")
        assertHeading("YOUR CITIES AT THIS TIME")

        show(Stage5Screen.Settings)
        assertHeading("Settings")
        assertHeading("THEME MODE")

        show(Stage5Screen.Detail)
        assertHeading("COMPARED WITH YOUR LOCAL TIME")
        assertHeading("YOUR LOCAL TIME")

        // The dialog is drawn in the app's own composition, so it has to
        // announce itself as a pane and name the field: nothing else will.
        show(Stage5Screen.LabelDialog)
        composeRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "City label"))
            .assertExists()
        assertHeading("City label")
        composeRule.onNodeWithContentDescription("Label for Tokyo").assertExists()
    }

    private fun assertHeading(text: String) {
        composeRule.onNodeWithText(text)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    // ---------------------------------------------------------------- 18
    // 3.1.0 — the comparison switch, the time input and the alarm sound.
    //
    // 18a is the Settings switch and what it hides, 18b the wheel/dial rule,
    // 18c the sound picker end to end.

    /**
     * Criterion 18a — "Compared with your local time" is a real switch in
     * Settings, and turning it off removes the difference line from the city
     * cards.
     *
     * The switch reports its own state (rather than only moving a knob) so a
     * screen reader says "on"/"off", which is what the toggleable semantics on
     * the whole row are for.
     */
    @Test
    fun criterion18a_theComparisonSwitchHidesTheDifferenceOnTheCards() {
        var compare by mutableStateOf(true)
        var toggled: Boolean? = null
        val state = PreviewSamples.clockState()

        composeRule.setContent {
            FixedClockwiseTheme {
                // Both halves get an explicit share of the height: the settings
                // sheet scrolls internally and would otherwise take the whole
                // screen, leaving the cards uncomposed.
                Column {
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsContent(
                            themeMode = ThemeMode.SYSTEM,
                            onThemeModeSelected = {},
                            use24Hour = true,
                            onUse24HourChange = {},
                            compareWithLocalTime = compare,
                            onCompareWithLocalTimeChange = {
                                toggled = it
                                compare = it
                            },
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ClocksScreen(
                            state = state.copy(compareWithLocalTime = compare),
                            onCityClick = {},
                            onAddClick = {},
                            onFindCityClick = {},
                            onSettingsClick = {},
                            onPlanMeetingClick = {},
                        )
                    }
                }
            }
        }

        // Every card that has a comparison says what it is measured against.
        fun differences() = composeRule
            .onAllNodesWithContentDescription("Compared with your local time:", substring = true)
            .fetchSemanticsNodes()
        assertTrue("the cards show their difference by default", differences().isNotEmpty())

        composeRule
            .onNodeWithTag(SettingsTestTags.CompareToggle)
            .performScrollTo()
            .assertIsOn()

        composeRule.onNodeWithTag(SettingsTestTags.CompareToggle).performClick()
        composeRule.waitForIdle()

        assertEquals(false, toggled)
        composeRule.onNodeWithTag(SettingsTestTags.CompareToggle).assertIsOff()
        assertTrue("turning the switch off removes every difference line", differences().isEmpty())
    }

    /**
     * Criterion 18b — the alarm time is the Google-Clock dial in every theme,
     * and direct numeric entry is one keyboard tap away.
     *
     * The dial cannot produce an invalid time, so it is the default; the keypad
     * is the fast path for someone who already knows the digits.
     */
    @Test
    fun criterion18b_theDialIsAlwaysTheDefaultAndTheKeypadIsOneTapAway() {
        var theme by mutableStateOf(AppTheme.LIGHT_MONOCHROME)

        composeRule.setContent {
            FixedClockwiseTheme(theme = theme) {
                AlarmEditorContent(
                    alarm = null,
                    use24Hour = true,
                    onSave = {},
                    onDelete = {},
                    onDismiss = {},
                    theme = theme,
                    soundCatalog = RecordingAlarmSoundCatalog(),
                )
            }
        }

        composeRule.onNodeWithTag(AlarmTimeTestTags.Dial).assertExists()
        composeRule.onNodeWithTag(AlarmTimeTestTags.Keypad).assertDoesNotExist()

        composeRule.runOnIdle { theme = AppTheme.MATERIAL_YOU }
        composeRule.waitForIdle()

        // The same picker, whichever theme is in force.
        composeRule.onNodeWithTag(AlarmTimeTestTags.Dial).assertExists()

        composeRule.onNodeWithTag(AlarmTimeTestTags.KeypadToggle).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AlarmTimeTestTags.Keypad).assertExists()
        composeRule.onNodeWithTag(AlarmTimeTestTags.HourField).assertExists()
        composeRule.onNodeWithTag(AlarmTimeTestTags.MinuteField).assertExists()
        composeRule.onNodeWithTag(AlarmTimeTestTags.Dial).assertDoesNotExist()
    }

    /**
     * Criterion 18c — a sound can be previewed, chosen, and saved with the
     * alarm, and the row then names the sound that will ring.
     *
     * The catalog is a fake because there is no ringtone store under the JVM
     * test renderer; what is proved here is the editor's wiring, and the real
     * catalog's fallback rule lives in `AlarmSoundCatalogTest`.
     */
    @Test
    fun criterion18c_aSoundIsPreviewedChosenAndSavedWithTheAlarm() {
        val catalog = RecordingAlarmSoundCatalog()
        var saved: Alarm? = null

        composeRule.setContent {
            FixedClockwiseTheme {
                AlarmEditorContent(
                    alarm = Alarm(id = 1, hour = 7, minute = 0, label = "Wake"),
                    use24Hour = true,
                    onSave = { saved = it },
                    onDelete = {},
                    onDismiss = {},
                    soundCatalog = catalog,
                )
            }
        }

        // The row starts on the default, because the alarm was saved before the
        // picker existed.
        composeRule.onNodeWithTag(AlarmEditorTestTags.SoundRow).performScrollTo().assertExists()
        composeRule.onNodeWithText("Default alarm sound").assertExists()

        composeRule.onNodeWithTag(AlarmEditorTestTags.SoundRow).performClick()
        composeRule.onNodeWithTag(AlarmEditorTestTags.SoundDialog).assertExists()

        composeRule
            .onNodeWithTag(AlarmEditorTestTags.previewButton(BRIGHT_TONE_URI))
            .performClick()
        assertEquals(listOf(BRIGHT_TONE_URI), catalog.previewed)

        composeRule
            .onNodeWithTag(AlarmEditorTestTags.soundOption(BRIGHT_TONE_URI))
            .performClick()
        composeRule.waitForIdle()

        // Choosing closes the list and the row names what will ring.
        composeRule.onNodeWithTag(AlarmEditorTestTags.SoundDialog).assertDoesNotExist()
        composeRule.onNodeWithText("Bright Morning").assertExists()
        // A preview never outlives the list it started in.
        assertTrue(catalog.stopped)

        composeRule.onNodeWithText("SAVE ALARM").performScrollTo().performClick()
        assertEquals(BRIGHT_TONE_URI, saved?.soundUri)
    }

}

/** The URI the fake catalog offers as its first real tone. */
private const val BRIGHT_TONE_URI = "test://bright-morning"

/**
 * A catalog with no device behind it.
 *
 * Records what was asked of it, so a test can say a preview actually happened
 * rather than only that the button was tapped.
 */
private class RecordingAlarmSoundCatalog : AlarmSoundCatalog {
    val options = listOf(
        AlarmSoundOption(uri = "", title = "Default alarm sound"),
        AlarmSoundOption(uri = BRIGHT_TONE_URI, title = "Bright Morning"),
    )
    val previewed = mutableListOf<String>()
    var stopped = false

    override fun options(): List<AlarmSoundOption> = options

    override fun preview(uri: String) {
        previewed += uri
    }

    override fun stopPreview() {
        stopped = true
    }
}

/** Renders one difference so the wording itself can be asserted. */
@Composable
private fun TimeDifferenceSample(difference: TimeDifference) {
    Text(text = timeDifferenceLabel(difference))
}
