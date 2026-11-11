package com.pj95.clockwise.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.assertHasRunCallbackClickAction
import androidx.glance.appwidget.testing.unit.assertHasStartActivityClickAction as assertHasIntentStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasStartActivityClickAction
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasTextEqualTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.MainActivity
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.ui.WidgetUiState
import com.pj95.clockwise.ui.mapping.buildWidgetUiState
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.NightClockwisePalette
import com.pj95.clockwise.ui.theme.LightClockwisePalette
import com.pj95.clockwise.widget.RefreshWidgetAction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * What the widget actually draws.
 *
 * Glance composables compile to `RemoteViews`, so they cannot be screenshotted
 * the way the app's screens can (spec section 12.3). Glance's own unit harness
 * composes the content against a test context instead, which is what makes these
 * assertions — every city, its time, the marker, the overflow line — real checks
 * of the widget rather than of its inputs.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class ClockwiseWidgetTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun resetTheme() {
        RuntimeEnvironment.setQualifiers("+notnight")
    }

    private fun state(
        savedCityIds: List<String> = CityDirectory.defaultSavedCityIds,
        labels: Map<String, String> = emptyMap(),
        use24Hour: Boolean = true,
    ): WidgetUiState = buildWidgetUiState(
        savedCityIds = savedCityIds,
        cityLabels = labels,
        use24Hour = use24Hour,
        now = PreviewSamples.referenceInstant,
        deviceZoneId = PreviewSamples.sampleDeviceZoneId,
    )

    /** Draws [state] at [size], the way the launcher would, and runs [block]. */
    private fun widgetTest(
        state: WidgetUiState,
        size: DpSize = DpSize(250.dp, 250.dp),
        block: GlanceAppWidgetUnitTest.() -> Unit,
    ) = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(size)
        provideComposable { ClockwiseWidgetContent(state) }
        block()
    }

    @Test
    fun everySavedCityIsListedWithItsOwnLocalTime() = widgetTest(state()) {
        onNode(hasTextEqualTo("Tokyo")).assertExists()
        onNode(hasTextEqualTo("01:40")).assertExists()
        onNode(hasTextEqualTo("Sydney")).assertExists()
        onNode(hasTextEqualTo("03:40")).assertExists()
        onNode(hasTextEqualTo("Los Angeles")).assertExists()
        onNode(hasTextEqualTo("08:40")).assertExists()
        onNode(hasTextEqualTo("New York")).assertExists()
        onNode(hasTextEqualTo("11:40")).assertExists()
        onNode(hasTextEqualTo("London")).assertExists()
        onNode(hasTextEqualTo("16:40")).assertExists()
    }

    @Test
    fun theHeaderCarriesTheDeviceClockEveryRowIsReadAgainst() = widgetTest(state()) {
        onNode(hasTextEqualTo(context.getString(R.string.widget_title))).assertExists()
        onNode(hasTextEqualTo("23:10")).assertExists()
    }

    @Test
    fun theAppWideFormatReachesTheWidget() = widgetTest(state(use24Hour = false)) {
        onNode(hasTextEqualTo("1:40 AM")).assertExists()
        onNode(hasTextEqualTo("4:40 PM")).assertExists()
        onNode(hasTextEqualTo("11:10 PM")).assertExists()
    }

    @Test
    fun aLabelSavedInTheAppAppearsOnTheWidget() =
        widgetTest(state(labels = mapOf("tokyo" to "Head Office"))) {
            onNode(hasTextEqualTo("Tokyo (Head Office)")).assertExists()
        }

    @Test
    fun theWeekdayIsShownOnlyForCitiesThatHaveRolledOverToAnotherDay() =
        widgetTest(state(savedCityIds = listOf("tokyo", "london"))) {
            // Tokyo is already into Sunday while the device is on Saturday...
            onNode(hasTextEqualTo("Sun")).assertExists()

            // ...and London, which shares the device's day, is not labelled: the
            // weekday would be noise on every row.
            onNode(hasTextEqualTo("Sat")).assertDoesNotExist()
        }

    @Test
    fun theSunAndMoonSayWhichHalfOfTheDayEachCityIsIn() =
        widgetTest(state(savedCityIds = listOf("tokyo", "london"))) {
            // 01:40 in Tokyo, 16:40 in London, at the same instant.
            onNode(hasContentDescriptionEqualTo(context.getString(R.string.a11y_nighttime)))
                .assertExists()
            onNode(hasContentDescriptionEqualTo(context.getString(R.string.a11y_daytime)))
                .assertExists()
        }

    @Test
    fun citiesThatDoNotFitAreCountedRatherThanDropped() {
        val eightCities = listOf(
            "tokyo",
            "sydney",
            "los-angeles",
            "new-york",
            "london",
            "paris",
            "dubai",
            "mumbai",
        )

        widgetTest(state(savedCityIds = eightCities)) {
            // Five rows fit beside the count line, three are counted.
            onNode(hasTextEqualTo("London")).assertExists()
            onNode(hasTextEqualTo("Paris")).assertDoesNotExist()
            onNode(hasTextEqualTo("Dubai")).assertDoesNotExist()
            onNode(hasTextEqualTo("+3 more cities")).assertExists()
        }
    }

    @Test
    fun aFullWidgetHasNothingToCount() = widgetTest(state()) {
        onNode(hasTextEqualTo("+2 more cities")).assertDoesNotExist()
        onNode(hasTextEqualTo("+3 more cities")).assertDoesNotExist()
    }

    @Test
    fun theCompactCardShowsOneCityAndCountsTheRest() =
        widgetTest(state(), size = DpSize(220.dp, 110.dp)) {
            onNode(hasTextEqualTo("Tokyo")).assertExists()
            onNode(hasTextEqualTo("Sydney")).assertDoesNotExist()
            onNode(hasTextEqualTo("+4 more cities")).assertExists()
        }

    @Test
    fun anEmptyWidgetSaysSoRatherThanLookingBroken() =
        widgetTest(state(savedCityIds = emptyList())) {
            onNode(hasTextEqualTo(context.getString(R.string.widget_empty))).assertExists()
            // The device's own clock still anchors the header.
            onNode(hasTextEqualTo("23:10")).assertExists()
        }

    @Test
    fun tappingTheHeaderOpensTheApp() = widgetTest(state()) {
        onNode(hasTestTag(WidgetTestTags.HEADER))
            .assertHasStartActivityClickAction<MainActivity>()
    }

    @Test
    fun tappingACityOpensThatCitysDetailScreen() = widgetTest(state()) {
        for (cityId in CityDirectory.defaultSavedCityIds) {
            onNode(hasTestTag(WidgetTestTags.row(cityId)))
                .assertHasIntentStartActivityClickAction(cityDetailIntent(context, cityId))
        }
    }

    @Test
    fun tappingTheOverflowLineOpensTheApp() {
        val eightCities = CityDirectory.defaultSavedCityIds + listOf("paris", "dubai", "mumbai")

        widgetTest(state(savedCityIds = eightCities)) {
            onNode(hasTestTag(WidgetTestTags.MORE))
                .assertHasStartActivityClickAction<MainActivity>()
        }
    }

    @Test
    fun theRefreshGlyphRedrawsWithoutOpeningTheApp() = widgetTest(state()) {
        onNode(hasTestTag(WidgetTestTags.REFRESH))
            .assertHasRunCallbackClickAction<RefreshWidgetAction>()
    }

    @Test
    fun theRowIntentCarriesTheCityAndStaysInTheApp() {
        val intent = cityDetailIntent(context, "tokyo")

        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals("tokyo", intent.getStringExtra(MainActivity.EXTRA_CITY_ID))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
    }

    @Test
    fun theWidgetFollowsTheSystemThemeRatherThanTheAppsStoredChoice() {
        val provider = DayNightColorProvider(
            day = LightClockwisePalette.card,
            night = NightClockwisePalette.card,
        )

        RuntimeEnvironment.setQualifiers("+notnight")
        assertEquals(LightClockwisePalette.card, provider.getColor(context))

        RuntimeEnvironment.setQualifiers("+night")
        assertEquals(NightClockwisePalette.card, provider.getColor(context))
    }
}
