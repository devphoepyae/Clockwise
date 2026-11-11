package com.pj95.clockwise.ui.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.ui.mapping.WIDGET_MAX_ROWS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The row budget is the responsive half of the widget: the launcher gives a
 * height, and the card decides how many cities fit and whether one of them has
 * to give up its row to the `+N more` line.
 *
 * These are pure numbers, so they are tested without a composition. The values
 * are pinned to the declared sizes, which is what makes a 4×4 widget show the
 * five default cities and a 3×2 one show a single city.
 */
class WidgetGeometryTest {

    @Test
    fun theFiveDeclaredSizesCoverTheDefaultAndTheResizedCards() {
        assertEquals(5, WIDGET_SIZES.size)
        assertTrue(
            "the provider's 4x4 default must be one of the composed sizes",
            DpSize(250.dp, 250.dp) in WIDGET_SIZES,
        )
        assertTrue(
            "the compact 2x2 card must be declared",
            DpSize(180.dp, 110.dp) in WIDGET_SIZES,
        )
        assertTrue(
            "the compact 3x2 card must be declared",
            DpSize(220.dp, 110.dp) in WIDGET_SIZES,
        )
        assertTrue(
            "the wide 5x4 card must be declared",
            DpSize(320.dp, 250.dp) in WIDGET_SIZES,
        )
    }

    @Test
    fun theSmallestCardStillShowsOneCity() {
        // The 2x2 card is the smallest size the provider allows the user to
        // resize to; it is a glance at one city rather than an empty square.
        assertEquals(1, widgetRowBudget(availableHeightDp = 110, savedCityCount = 5))
    }

    @Test
    fun theDefaultFourByFourSizeShowsAllFiveDefaultCities() {
        // 250 dp of height, the 4x4 default: six rows fit, so five cities need
        // no counting line at all.
        assertEquals(6, widgetRowBudget(availableHeightDp = 250, savedCityCount = 5))
    }

    @Test
    fun theCompactCardShowsOneCityAndCountsTheRest() {
        // 3x2 is a glance, not a list: one city plus "+4 more".
        assertEquals(1, widgetRowBudget(availableHeightDp = 110, savedCityCount = 5))
    }

    @Test
    fun aFourByThreeCardShowsThreeCities() {
        assertEquals(3, widgetRowBudget(availableHeightDp = 180, savedCityCount = 5))
    }

    @Test
    fun theCountLineCostsOneCitysRow() {
        // 195 dp fits four cities on its own, but not four plus "+N more".
        assertEquals(4, widgetRowBudget(availableHeightDp = 195, savedCityCount = 4))
        assertEquals(3, widgetRowBudget(availableHeightDp = 195, savedCityCount = 8))
    }

    @Test
    fun aStretchedCardStillStopsAtTheDesignCeiling() {
        assertEquals(WIDGET_MAX_ROWS, widgetRowBudget(availableHeightDp = 2_000, savedCityCount = 20))
    }

    @Test
    fun aCardWithNoReportedSizeFallsBackToTheCeiling() {
        assertEquals(WIDGET_MAX_ROWS, widgetRowBudget(availableHeightDp = 0, savedCityCount = 5))
        assertEquals(WIDGET_MAX_ROWS, widgetRowBudget(availableHeightDp = -1, savedCityCount = 5))
    }

    @Test
    fun aCrampedCardStillShowsOneCityRatherThanNothing() {
        assertEquals(1, widgetRowBudget(availableHeightDp = 20, savedCityCount = 5))
    }

    @Test
    fun aLargerFontShrinksTheRowBudget() {
        val normal = widgetRowBudget(availableHeightDp = 250, savedCityCount = 5)
        val large = widgetRowBudget(availableHeightDp = 250, savedCityCount = 5, fontScale = 2f)

        assertEquals(6, normal)
        assertTrue("a 2x font must fit fewer rows, got $large", large < normal)
        assertTrue("at least one city must still fit", large >= 1)
    }

    @Test
    fun theRowHeightGrowsWithTheFontAndNeverDropsBelowTheTapTarget() {
        assertEquals(28f, WidgetGeometry.rowHeight(fontScale = 1f), 0.001f)
        assertTrue(WidgetGeometry.rowHeight(fontScale = 2f) > 28f)
        assertEquals(32f, WidgetGeometry.headerHeight(fontScale = 1f), 0.001f)
        assertTrue(WidgetGeometry.headerHeight(fontScale = 2f) > 32f)
    }
}
