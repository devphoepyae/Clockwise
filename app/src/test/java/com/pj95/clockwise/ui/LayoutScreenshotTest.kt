package com.pj95.clockwise.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.model.prefs.ThemeMode
import com.pj95.clockwise.data.repository.ClockRepository
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.model.alarm.RepeatDays
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.model.timer.CountdownTimer
import com.pj95.clockwise.testing.FakeAlarmRepository
import com.pj95.clockwise.testing.FakeSavedCitiesDataStore
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.screens.AlarmEditorContent
import com.pj95.clockwise.ui.screens.AlarmRingingScreen
import com.pj95.clockwise.ui.screens.AlarmScreen
import com.pj95.clockwise.ui.screens.CityDetailScreen
import com.pj95.clockwise.ui.screens.CityLabelDialog
import com.pj95.clockwise.ui.screens.CitySearchContent
import com.pj95.clockwise.ui.screens.ClocksScreen
import com.pj95.clockwise.ui.screens.MeetingPlannerScreen
import com.pj95.clockwise.ui.screens.OnboardingScreen
import com.pj95.clockwise.ui.screens.PreviewAlarmSoundCatalog
import com.pj95.clockwise.ui.screens.PrivacyScreen
import com.pj95.clockwise.ui.screens.ScreenSaverScreen
import com.pj95.clockwise.ui.screens.SettingsContent
import com.pj95.clockwise.ui.screens.StudioScreen
import com.pj95.clockwise.ui.screens.TimerPanel
import com.pj95.clockwise.ui.screens.WorldMapScreen
import com.pj95.clockwise.ui.theme.AlarmRingingPalette
import com.pj95.clockwise.ui.theme.AlarmRingingTheme
import com.pj95.clockwise.ui.theme.ClockwiseTheme
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneId

/**
 * JVM screenshot tests for layout verification (spec section 10).
 *
 * Every screen is rendered at 360 dp wide portrait with a fixed instant, so the
 * PNGs in `app/build/screenshots` are reviewable without an emulator and never
 * drift between runs.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class LayoutScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * The whole app shell — navigation plus the live city list — driven by an
     * in-memory store and a single-emission ticker so the render is stable.
     */
    @Test
    fun appShell_360dp() {
        val viewModel = ClockwiseViewModel(
            repository = ClockRepository(FakeSavedCitiesDataStore()),
            ticker = flowOf(PreviewSamples.referenceInstant),
            deviceZoneId = { ZoneId.of("Asia/Yangon") },
        )

        composeRule.setContent {
            FixedClockwiseTheme {
                ClockwiseRoot(
                    viewModel = viewModel,
                    alarmViewModel = AlarmViewModel(
                        FakeAlarmRepository(PreviewSamples.alarmState().alarms),
                    ),
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Los Angeles").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/app_shell_360dp.png")
    }

    @Test
    fun clocksScreen_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme {
                ClocksScreen(
                    state = PreviewSamples.clockState(),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_360dp.png")
    }

    /**
     * The Clocks screen at Android's "Large" font size (1.5x), which is the
     * step the accessibility pass needs a render of: times, labels, headings
     * and the difference line all grow, so clipping or collisions show up in
     * one PNG. The widget estimates its own row heights from the same scale.
     */
    @Test
    fun clocksScreenLargeFont_360dp() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density,
                    fontScale = 1.5f,
                ),
            ) {
                FixedClockwiseTheme {
                    ClocksScreen(
                        state = PreviewSamples.clockState(),
                        onCityClick = {},
                        onAddClick = {},
                        onFindCityClick = {},
                        onSettingsClick = {},
                        onPlanMeetingClick = {},
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_large_font_360dp.png")
    }

    @Test
    fun clocksScreenEmpty_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme {
                ClocksScreen(
                    state = PreviewSamples.emptyState(),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_empty_360dp.png")
    }

    @Test
    fun cityDetail_360dp() {
        val state = PreviewSamples.clockState(use24Hour = true)
        composeRule.setContent {
            FixedClockwiseTheme {
                CityDetailScreen(
                    city = CityDirectory.find(CityDirectory.defaultActiveCityId)!!,
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
        composeRule.onRoot().captureRoboImage("build/screenshots/city_detail_360dp.png")
    }

    @Test
    fun cityDetailTwelveHour_360dp() {
        val state = PreviewSamples.clockState(use24Hour = false)
        composeRule.setContent {
            FixedClockwiseTheme {
                CityDetailScreen(
                    city = CityDirectory.find(CityDirectory.defaultActiveCityId)!!,
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
        composeRule.onRoot().captureRoboImage("build/screenshots/city_detail_12h_360dp.png")
    }

    /**
     * The detail screen for a city the user has labelled: the label sits under
     * the name, the edit action follows it, and the difference names the
     * reference it is measured against.
     */
    @Test
    fun cityDetailLabelled_360dp() {
        val state = PreviewSamples.labelledState()
        composeRule.setContent {
            FixedClockwiseTheme {
                CityDetailScreen(
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
        composeRule.onRoot().captureRoboImage("build/screenshots/city_detail_labelled_360dp.png")
    }

    /**
     * The map screen with the default five cities at the winter reference
     * instant: southern-hemisphere night shading, Los Angeles active.
     */
    @Test
    fun worldMap_360dp() {
        renderMap(PreviewSamples.mapState())
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_360dp.png")
    }

    /**
     * The same screen six months later, so the terminator is on the other side
     * of the equator and the shading clearly moves.
     */
    @Test
    fun worldMapSummer_360dp() {
        renderMap(PreviewSamples.mapState(now = PreviewSamples.summerInstant))
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_summer_360dp.png")
    }

    /**
     * Time travel to 03:00 in the device's zone. The Sun marker has moved to
     * the opposite meridian and the selected city reads mid-afternoon, so the
     * live/time-travel distinction is visible in one render.
     */
    @Test
    fun worldMapTimeTravel_360dp() {
        renderMap(PreviewSamples.mapState(travelMinute = MinuteOfDay.of(3, 0)))
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_time_travel_360dp.png")
    }

    @Test
    fun worldMapEmpty_360dp() {
        renderMap(PreviewSamples.emptyMapState())
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_empty_360dp.png")
    }

    /**
     * Every city selected in turn, so the active marker and meridian are shown
     * at a range of longitudes (including the far east and the far west).
     */
    @Test
    fun worldMapActiveCityMoves_360dp() {
        renderMap(
            PreviewSamples.mapState(
                savedCityIds = listOf("tokyo", "london", "honolulu"),
                activeCityId = "london",
            ),
        )
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_london_360dp.png")
    }

    private fun renderMap(map: MapUiState, theme: AppTheme = AppTheme.LIGHT_MONOCHROME) {
        composeRule.setContent {
            FixedClockwiseTheme(theme = theme) {
                WorldMapScreen(
                    map = map,
                    onSelectCity = {},
                    onOpenCity = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onTimeChange = {},
                    onGoLive = {},
                )
            }
        }
    }

    // ---- Second palette ----
    //
    // The same screens rendered through Cool Slate as well as Light Monochrome,
    // so a token that reads on off-white but not on the slate surface is visible
    // in the same review pass. (True Dark and High Contrast were retired in
    // 3.1.0; the list itself is rendered in Cool Slate by
    // `clocksScreenCoolSlate_360dp`.)

    @Test
    fun cityDetailCoolSlate_360dp() {
        val state = PreviewSamples.clockState(use24Hour = true)
        composeRule.setContent {
            FixedClockwiseTheme(theme = AppTheme.COOL_SLATE) {
                CityDetailScreen(
                    city = CityDirectory.find(CityDirectory.defaultActiveCityId)!!,
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
        composeRule.onRoot().captureRoboImage("build/screenshots/city_detail_cool_slate_360dp.png")
    }

    @Test
    fun worldMapCoolSlate_360dp() {
        renderMap(PreviewSamples.mapState(), AppTheme.COOL_SLATE)
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_cool_slate_360dp.png")
    }

    // ---- Labels and time differences ----

    /**
     * The list with labels and the full spread of differences: whole hours,
     * half hours (this device sits on UTC+6:30) and "Same time" for a city in
     * the device's own zone.
     */
    @Test
    fun clocksScreenLabelled_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme {
                ClocksScreen(
                    state = PreviewSamples.labelledState(),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_labelled_360dp.png")
    }

    /** The same list on a device in the app's own directory, so "Same time" shows. */
    @Test
    fun mapScreenLabelled_360dp() {
        renderMap(PreviewSamples.labelledMapState())
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_labelled_360dp.png")
    }

    /**
     * The labelled list on a second palette: labels, differences and the
     * sun/moon icons in one review pass, so a token that only reads on the
     * off-white surface is visible.
     */
    @Test
    fun clocksScreenLabelledCoolSlate_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme(theme = AppTheme.COOL_SLATE) {
                ClocksScreen(
                    state = PreviewSamples.labelledState(),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_labelled_cool_slate_360dp.png")
    }

    /**
     * The list with the comparison switch off: the difference line is gone and
     * the city names keep the width it used to take.
     */
    @Test
    fun clocksScreenComparisonOff_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme(theme = AppTheme.WARM_SEPIA) {
                ClocksScreen(
                    state = PreviewSamples.labelledState().copy(compareWithLocalTime = false),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_comparison_off_360dp.png")
    }

    /**
     * The list in 12-hour form. The app-wide switch has to reach every surface,
     * so the list is rendered in it as well as the detail screen.
     */
    @Test
    fun clocksScreenTwelveHour_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme {
                ClocksScreen(
                    state = PreviewSamples.clockState(use24Hour = false),
                    onCityClick = {},
                    onAddClick = {},
                    onFindCityClick = {},
                    onSettingsClick = {},
                    onPlanMeetingClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_12h_360dp.png")
    }

    // ---- Meeting planner ----

    /**
     * The planner at 09:00 local, with labels, so one render shows every state
     * the rows can be in: cities still at work, cities long finished, and a city
     * a day behind.
     */
    @Test
    fun meetingPlanner_360dp() {
        renderPlanner(PreviewSamples.plannerState(labels = true))
        composeRule.onRoot().captureRoboImage("build/screenshots/planner_screen_360dp.png")
    }

    @Test
    fun meetingPlannerCoolSlate_360dp() {
        renderPlanner(
            planner = PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(18, 30), labels = true),
            theme = AppTheme.COOL_SLATE,
        )
        composeRule.onRoot().captureRoboImage("build/screenshots/planner_screen_cool_slate_360dp.png")
    }

    @Test
    fun meetingPlannerEmpty_360dp() {
        renderPlanner(PreviewSamples.emptyPlannerState())
        composeRule.onRoot().captureRoboImage("build/screenshots/planner_empty_360dp.png")
    }

    /**
     * The planner in 12-hour form: the slider's readout, the rows and the
     * reference line all follow the app-wide format.
     */
    @Test
    fun meetingPlannerTwelveHour_360dp() {
        renderPlanner(
            PreviewSamples.plannerState(
                minuteOfDay = MinuteOfDay.of(9, 0),
                labels = true,
                use24Hour = false,
            ),
        )
        composeRule.onRoot().captureRoboImage("build/screenshots/planner_screen_12h_360dp.png")
    }

    /**
     * The converter: 12:00 is read in Tokyo rather than on the device, so the
     * source chip is selected, the headline names Tokyo, and the recommendation
     * points at the morning slot where Asia and California overlap.
     */
    @Test
    fun meetingPlannerSource_360dp() {
        renderPlanner(
            PreviewSamples.plannerState(
                minuteOfDay = MinuteOfDay.of(12, 0),
                sourceCityId = "tokyo",
            ),
        )
        composeRule.onRoot().captureRoboImage("build/screenshots/planner_screen_source_360dp.png")
    }

    private fun renderPlanner(
        planner: PlannerUiState,
        theme: AppTheme = AppTheme.LIGHT_MONOCHROME,
    ) {
        composeRule.setContent {
            FixedClockwiseTheme(theme = theme) {
                MeetingPlannerScreen(
                    planner = planner,
                    onMinuteChange = {},
                    onSourceChange = {},
                    onBack = {},
                    onFindCityClick = {},
                )
            }
        }
    }

    /**
     * The label edit dialog, drawn over the Clocks screen it was opened from so
     * the dim behind the card reads the way it does in the app.
     */
    @Test
    fun cityLabelDialog_360dp() {
        renderLabelDialog(AppTheme.LIGHT_MONOCHROME)
        composeRule.onRoot().captureRoboImage("build/screenshots/city_label_dialog_360dp.png")
    }

    /** The same dialog on a second palette, so the card and the scrim compare. */
    @Test
    fun cityLabelDialogCoolSlate_360dp() {
        renderLabelDialog(AppTheme.COOL_SLATE)
        composeRule.onRoot().captureRoboImage("build/screenshots/city_label_dialog_cool_slate_360dp.png")
    }

    private fun renderLabelDialog(theme: AppTheme) {
        composeRule.setContent {
            FixedClockwiseTheme(theme = theme) {
                Box(Modifier.fillMaxSize()) {
                    ClocksScreen(
                        state = PreviewSamples.clockState(),
                        onCityClick = {},
                        onAddClick = {},
                        onFindCityClick = {},
                        onSettingsClick = {},
                        onPlanMeetingClick = {},
                    )
                    CityLabelDialog(
                        cityName = "Tokyo",
                        currentLabel = "Head Office",
                        onSave = {},
                        onDismiss = {},
                    )
                }
            }
        }
    }

    // ---- Settings ----

    @Test
    fun settingsSheet_360dp() {
        renderSettings(ThemeMode.SYSTEM)
        composeRule.onRoot().captureRoboImage("build/screenshots/settings_sheet_360dp.png")
    }

    @Test
    fun settingsSheetDark_360dp() {
        renderSettings(ThemeMode.DARK)
        composeRule.onRoot().captureRoboImage("build/screenshots/settings_sheet_dark_360dp.png")
    }

    /** The sheet with the comparison switch on, the only other state it has. */
    @Test
    fun settingsSheetComparisonOn_360dp() {
        renderSettings(ThemeMode.LIGHT, compareWithLocalTime = true)
        composeRule.onRoot().captureRoboImage("build/screenshots/settings_sheet_comparison_on_360dp.png")
    }

    private fun renderSettings(
        themeMode: ThemeMode,
        compareWithLocalTime: Boolean = false,
    ) {
        composeRule.setContent {
            ClockwiseTheme(themeMode = themeMode) {
                // Presented the way the app actually shows it: a sheet pinned to
                // the bottom over a dimmed backdrop, so the corner radius and the
                // sheet/backdrop contrast are both visible in the PNG.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ClockwiseColors.Secondary),
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        color = ClockwiseColors.Background,
                        contentColor = ClockwiseColors.Foreground,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    ) {
                        SettingsContent(
                            themeMode = themeMode,
                            onThemeModeSelected = {},
                            use24Hour = true,
                            onUse24HourChange = {},
                            compareWithLocalTime = compareWithLocalTime,
                        )
                    }
                }
            }
        }
    }

    // ---- One render per theme ----
    //
    // Together with `clocks_screen_360dp.png` (Light Monochrome) and
    // `settings_sheet_360dp.png`, these complete a list render and an
    // appearance-sheet render for every theme the app ships. The theme picker is
    // the one screen that has to be reviewed in all of them, because it shows the
    // swatch of every theme at once — including the ones that are not in force.

    @Test
    fun clocksScreenWarmSepia_360dp() {
        renderClocks(AppTheme.WARM_SEPIA)
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_warm_sepia_360dp.png")
    }

    @Test
    fun clocksScreenCoolSlate_360dp() {
        renderClocks(AppTheme.COOL_SLATE)
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_cool_slate_360dp.png")
    }

    /**
     * Material You below Android 12, where there is no wallpaper scheme to read:
     * the render is the static Material 3 light fallback, which is what an older
     * device on light mode actually sees. Pinned to API 30 so the PNG is stable
     * rather than depending on Robolectric's dynamic-colour stub.
     */
    @Test
    @Config(sdk = [30])
    fun clocksScreenMaterialYou_360dp() {
        renderClocks(AppTheme.MATERIAL_YOU)
        composeRule.onRoot().captureRoboImage("build/screenshots/clocks_screen_material_you_360dp.png")
    }

    private fun renderClocks(theme: AppTheme) {
        composeRule.setContent {
            FixedClockwiseTheme(theme = theme) {
                ClocksScreen(
                    state = PreviewSamples.clockState(),
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
    fun settingsSheetLight_360dp() {
        renderSettings(ThemeMode.LIGHT)
        composeRule.onRoot().captureRoboImage("build/screenshots/settings_sheet_light_360dp.png")
    }

    /** The same sheet in dark mode, below Android 12. */
    @Test
    @Config(sdk = [30])
    fun settingsSheetMaterialYou_360dp() {
        renderSettings(ThemeMode.DARK)
        composeRule.onRoot().captureRoboImage("build/screenshots/settings_sheet_material_you_360dp.png")
    }

    // ---- First-launch intro ----

    /**
     * The intro a fresh install sees before the city list. Rendered at the same
     * 360 dp as the rest so the copy, the three feature rows and the single
     * primary action are reviewed together.
     */
    @Test
    fun onboarding_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme {
                OnboardingScreen(onDone = {})
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/onboarding_360dp.png")
    }

    /**
     * The map in Warm Sepia: the second palette where the terminator has to be
     * visible over a soft paper surface, so the day/night shading is reviewed on
     * something other than the default light theme.
     */
    @Test
    fun worldMapWarmSepia_360dp() {
        renderMap(PreviewSamples.mapState(), AppTheme.WARM_SEPIA)
        composeRule.onRoot().captureRoboImage("build/screenshots/map_screen_warm_sepia_360dp.png")
    }

    @Test
    fun searchSheet_360dp() {
        renderSearchSheet(query = "")
        composeRule.onRoot().captureRoboImage("build/screenshots/search_sheet_360dp.png")
    }

    @Test
    fun searchSheetNoResults_360dp() {
        renderSearchSheet(query = "atlantis")
        composeRule.onRoot().captureRoboImage("build/screenshots/search_sheet_empty_360dp.png")
    }

    private fun renderSearchSheet(query: String) {        val state = PreviewSamples.clockState()
        composeRule.setContent {
            FixedClockwiseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ClockwiseColors.Background,
                ) {
                    CitySearchContent(
                        query = query,
                        onQueryChange = {},
                        savedCityIds = CityDirectory.defaultSavedCityIds.toSet(),
                        now = state.now,
                        use24Hour = state.use24Hour,
                        onAddCity = {},
                        onDismiss = {},
                        autoFocus = false,
                    )
                }
            }
        }
    }

    // ---- Alarms ----

    /**
     * The alarm tab with the three sample alarms: a repeating weekday alarm, an
     * every-day alarm and a switched-off one-shot, so one PNG shows the active
     * dot, the quiet disabled card and the switch states together.
     */
    @Test
    fun alarmsScreen_360dp() {
        renderAlarms(PreviewSamples.alarmState())
        composeRule.onRoot().captureRoboImage("build/screenshots/alarms_screen_360dp.png")
    }

    @Test
    fun alarmsScreenCoolSlate_360dp() {
        renderAlarms(PreviewSamples.alarmState(), AppTheme.COOL_SLATE)
        composeRule.onRoot().captureRoboImage("build/screenshots/alarms_screen_cool_slate_360dp.png")
    }

    @Test
    fun alarmsScreenEmpty_360dp() {
        renderAlarms(AlarmUiState(isLoading = false))
        composeRule.onRoot().captureRoboImage("build/screenshots/alarms_empty_360dp.png")
    }

    /** The exact-alarm notice, shown only on Android 12/12L without the grant. */
    @Test
    fun alarmsScreenExactNotice_360dp() {
        renderAlarms(
            AlarmUiState(
                isLoading = false,
                alarms = PreviewSamples.alarmState().alarms,
                exactAlarmsAllowed = false,
            ),
        )
        composeRule.onRoot().captureRoboImage("build/screenshots/alarms_exact_notice_360dp.png")
    }

    private fun renderAlarms(
        state: AlarmUiState,
        theme: AppTheme = AppTheme.LIGHT_MONOCHROME,
    ) {
        composeRule.setContent {
            FixedClockwiseTheme(theme = theme) {
                AlarmScreen(
                    state = state,
                    use24Hour = true,
                    onAddClick = {},
                    onAlarmClick = {},
                    onToggleAlarm = { _, _ -> },
                )
            }
        }
    }

    /**
     * The sheet that creates an alarm, opened on its default 07:00 with the
     * wheel pickers that every non-Material-You theme uses.
     */
    @Test
    fun alarmEditorNew_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ClockwiseColors.Background,
                ) {
                    AlarmEditorContent(
                        alarm = null,
                        use24Hour = true,
                        onSave = {},
                        onDelete = {},
                        onDismiss = {},
                        soundCatalog = PreviewAlarmSoundCatalog(),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/alarm_editor_new_360dp.png")
    }

    /**
     * The same sheet under Material You, where the platform's clock dial replaces
     * the wheels. Pinned to API 30 so the dynamic scheme is the static fallback
     * and the PNG is stable.
     */
    @Test
    @Config(sdk = [30])
    fun alarmEditorDial_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme(theme = AppTheme.MATERIAL_YOU) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ClockwiseColors.Background,
                ) {
                    AlarmEditorContent(
                        alarm = null,
                        use24Hour = true,
                        onSave = {},
                        onDelete = {},
                        onDismiss = {},
                        theme = AppTheme.MATERIAL_YOU,
                        soundCatalog = PreviewAlarmSoundCatalog(),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/alarm_editor_dial_360dp.png")
    }

    /** The same sheet for a stored alarm, so the delete action is in the render. */
    @Test
    fun alarmEditorExisting_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ClockwiseColors.Background,
                ) {
                    AlarmEditorContent(
                        alarm = Alarm(
                            id = 1,
                            hour = 6,
                            minute = 30,
                            label = "Morning run",
                            repeatDays = RepeatDays.WEEKDAYS,
                            soundUri = "preview://bright",
                        ),
                        use24Hour = true,
                        onSave = {},
                        onDelete = {},
                        onDismiss = {},
                        soundCatalog = PreviewAlarmSoundCatalog(),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/alarm_editor_existing_360dp.png")
    }

    /** The ring itself: the label as hero, the time beneath it, two large actions. */
    @Test
    fun alarmRinging_360dp() {
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
                    onDismiss = {},
                    onSnooze = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/alarm_ringing_360dp.png")
    }

    /** A long label: still one screen, still one way to stop it. */
    @Test
    fun alarmRingingLongLabel_360dp() {
        composeRule.setContent {
            AlarmRingingTheme(palette = AlarmRingingPalette) {
                AlarmRingingScreen(
                    alarm = Alarm(
                        id = 2,
                        hour = 22,
                        minute = 0,
                        label = "Evening medication and stretch",
                    ),
                    use24Hour = true,
                    isLoading = false,
                    onDismiss = {},
                    onSnooze = {},
                )
            }
        }
        composeRule.onRoot()
            .captureRoboImage("build/screenshots/alarm_ringing_critical_360dp.png")
    }

    /** The editor offering to make a medication alarm unsnoozable. */
    @Test
    fun alarmEditorCriticalHint_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ClockwiseColors.Background,
                ) {
                    AlarmEditorContent(
                        alarm = Alarm(
                            id = 3,
                            hour = 22,
                            minute = 0,
                            label = "Medication",
                            repeatDays = RepeatDays.EVERY_DAY,
                        ),
                        use24Hour = true,
                        onSave = {},
                        onDelete = {},
                        onDismiss = {},
                        soundCatalog = PreviewAlarmSoundCatalog(),
                    )
                }
            }
        }
        composeRule.onRoot()
            .captureRoboImage("build/screenshots/alarm_editor_critical_360dp.png")
    }

    // ---- Phase 1: timer, screen saver and the local information screens ----

    /**
     * The timer half of the Alarms tab in its setup state: three wheels reading
     * 00:05:00, the preset row, and one primary action. Rendered on its own so
     * the picker is reviewed at full height rather than under the alarm header.
     */
    @Test
    fun timerSetup_360dp() {
        renderTimer(TimerUiState(isLoading = false))
        composeRule.onRoot().captureRoboImage("build/screenshots/timer_setup_360dp.png")
    }

    /** The same panel counting down, which is the largest type in the app. */
    @Test
    fun timerRunning_360dp() {
        renderTimer(
            TimerUiState(
                isLoading = false,
                timer = CountdownTimer(
                    durationMillis = 300_000,
                    remainingMillis = 125_000,
                    endAtEpochMillis = 1_000_000,
                    running = true,
                ),
                remainingMillis = 125_000,
            ),
        )
        composeRule.onRoot().captureRoboImage("build/screenshots/timer_running_360dp.png")
    }

    private fun renderTimer(state: TimerUiState) {
        composeRule.setContent {
            FixedClockwiseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ClockwiseColors.Background,
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        TimerPanel(
                            state = state,
                            onStart = {},
                            onPause = {},
                            onResume = {},
                            onReset = {},
                        )
                    }
                }
            }
        }
    }

    /**
     * The desk clock: black, no chrome, one time and one date. The only render
     * in the set with a deliberately fixed palette, because a screen saver that
     * followed the app theme would not be one.
     */
    @Test
    fun screenSaver_360dp() {
        composeRule.setContent {
            ScreenSaverScreen(
                now = PreviewSamples.referenceInstant,
                use24Hour = true,
                onExit = {},
            )
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/screen_saver_360dp.png")
    }

    /** The privacy policy, which has to state zero collection without a WebView. */
    @Test
    fun privacyPolicy_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme { PrivacyScreen(onBack = {}) }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/privacy_policy_360dp.png")
    }

    /** The Chronos Studio gallery: free styles, trials, and the all-access unlock. */
    @Test
    fun chronosStudioGallery_360dp() {
        composeRule.setContent {
            FixedClockwiseTheme { StudioScreen(onBack = {}) }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/chronos_studio_360dp.png")
    }
}
