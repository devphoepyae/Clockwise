package com.pj95.clockwise.ui.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.ui.mapping.WIDGET_MAX_ROWS
import kotlin.math.max

/**
 * The widget's geometry, in one place, because two callers need the same
 * numbers: the drawing, and [widgetRowBudget], which works out how many cities
 * fit in the space the launcher has given the card.
 *
 * The font sizes are here for the same reason. A row's height is derived from
 * the size of the text inside it, so the budget would drift away from the
 * drawing if the two ever stated those numbers separately.
 */
internal object WidgetGeometry {

    /** The card's corner radius, matching the app's own cards. */
    val CornerRadius = 16.dp

    val HorizontalPadding = 14.dp
    val VerticalPadding = 10.dp

    /** The header: a title line, the device clock and the refresh action. */
    const val HeaderSizeSp = 10
    val MinHeaderHeight = 32.dp
    val GapAboveRule = 5.dp
    val RuleHeight = 1.dp
    val GapBelowRule = 5.dp

    /** One city row: a full-width tap target, then a hairline of air below. */
    const val TimeSizeSp = 14
    const val NameSizeSp = 12
    const val WeekdaySizeSp = 9
    val MinRowHeight = 28.dp
    val RowSpacing = 2.dp

    /** The `+N more` line, drawn only when cities had to be counted. */
    const val CountSizeSp = 10
    val GapAboveCount = 2.dp

    /** The refresh glyph and the area that receives the tap. */
    val RefreshIconSize = 16.dp
    val RefreshTouchSize = 24.dp

    /**
     * A line of text's height as a multiple of its font size.
     *
     * Roboto's own metrics are about 1.17 em and a `TextView` adds font padding
     * on top, so 1.3 is the honest round-up: it errs towards a *smaller* budget,
     * which costs a little unused space at the bottom of the card and never a
     * row cut off by the launcher.
     */
    const val LineHeightFactor = 1.3f

    /** A row is at least 28 dp, and grows when the user's font does. */
    fun rowHeight(fontScale: Float): Float =
        max(MinRowHeight.value, TimeSizeSp * LineHeightFactor * fontScale + 8f)

    /** The header is at least 32 dp, and grows when the user's font does. */
    fun headerHeight(fontScale: Float): Float =
        max(MinHeaderHeight.value, HeaderSizeSp * LineHeightFactor * fontScale + 12f)

    /** The `+N more` line, including the gap that separates it from the rows. */
    fun countHeight(fontScale: Float): Float =
        GapAboveCount.value + CountSizeSp * LineHeightFactor * fontScale
}

/**
 * The sizes the widget declares to the launcher.
 *
 * Glance runs the composition once for every size, and the host picks the best
 * fit. Five breakpoints cover the range the provider allows: a compact 2x2 card,
 * the 3x2 and 4x3 cards, the 4x4 default, and a wider 5x4 card. Every layout is
 * the same design — only the row budget, and therefore how many cities are
 * listed before the "+N more" line, changes.
 *
 * The 4x4 size is the default footprint declared by the provider info, so a
 * freshly added widget shows all five default cities. `minResizeWidth` and
 * `minResizeHeight` in the same file are what let the user shrink it down to
 * the compact sizes here; without those two attributes the provider's default
 * footprint would also be its floor and the smaller buckets would be dead code.
 */
internal val WIDGET_SIZES: Set<DpSize> = setOf(
    DpSize(180.dp, 110.dp),
    DpSize(220.dp, 110.dp),
    DpSize(250.dp, 180.dp),
    DpSize(250.dp, 250.dp),
    DpSize(320.dp, 250.dp),
)

/**
 * How many city rows fit in a card [availableHeightDp] tall.
 *
 * The launcher decides a cell's size, so the budget is taken from the size the
 * launcher has given *this* widget rather than from a constant. That is what
 * makes the card responsive: the same widget shows one city at its compact
 * size, three at 4x3, and up to six once it is stretched.
 *
 * [savedCityCount] is how many cities the app holds. It matters because the
 * `+N more` line is part of the budget: when it will be drawn, one city has to
 * give up its row to it. Over-counting is safe — an id the directory no longer
 * knows reserves a row that is then simply left empty — so the caller can pass
 * the saved list as it stands.
 *
 * A launcher that reports nothing ([availableHeightDp] of zero or less) falls
 * back to [WIDGET_MAX_ROWS], the same answer as before this existed.
 */
internal fun widgetRowBudget(
    availableHeightDp: Int,
    savedCityCount: Int,
    fontScale: Float = 1f,
): Int {
    if (availableHeightDp <= 0) return WIDGET_MAX_ROWS

    val rows = rowsThatFit(availableHeightDp, fontScale, withCountLine = false)

    return if (savedCityCount <= rows) {
        rows
    } else {
        rowsThatFit(availableHeightDp, fontScale, withCountLine = true)
    }
}

/** The chrome every card carries, whether or not it draws a count line. */
private fun rowsThatFit(heightDp: Int, fontScale: Float, withCountLine: Boolean): Int {
    val chrome = WidgetGeometry.VerticalPadding.value * 2 +
        WidgetGeometry.headerHeight(fontScale) +
        (WidgetGeometry.GapAboveRule + WidgetGeometry.RuleHeight + WidgetGeometry.GapBelowRule).value +
        (if (withCountLine) WidgetGeometry.countHeight(fontScale) else 0f)

    val rowPitch = WidgetGeometry.rowHeight(fontScale) + WidgetGeometry.RowSpacing.value

    // The last row does not need the gap that follows it.
    val rows = ((heightDp - chrome + WidgetGeometry.RowSpacing.value) / rowPitch).toInt()

    // At least one city, so a cramped card still says something; at most the
    // design's own ceiling, so a stretched one stays a glance and not a list.
    return rows.coerceIn(1, WIDGET_MAX_ROWS)
}
