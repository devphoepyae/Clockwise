package com.pj95.clockwise.ui.mapping

import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.ui.WidgetRow
import com.pj95.clockwise.ui.WidgetUiState
import java.time.Instant
import java.time.ZoneId

/**
 * The ceiling on how many cities the widget will ever draw.
 *
 * A widget is a fixed rectangle chosen by the user, not a scrollable list. The
 * responsive layout decides how many rows fit in the height the launcher has
 * given it, but a very tall card still stops at six: this is a glance, not a
 * second city list. Anything beyond the visible budget is counted as
 * "+N more" instead of being dropped silently (spec section 5.11).
 */
const val WIDGET_MAX_ROWS: Int = 6

/**
 * Builds the widget's rendering state from the stored preferences.
 *
 * Runs the same production mapper the Clocks screen uses, so a widget and the
 * app can never disagree about a city's time, then trims it to the widget's
 * shape. Pure, so it is exercised directly by tests.
 */
fun buildWidgetUiState(
    savedCityIds: List<String>,
    use24Hour: Boolean,
    now: Instant,
    deviceZoneId: ZoneId,
    cityLabels: Map<String, String> = emptyMap(),
): WidgetUiState {
    val state = buildClockwiseUiState(
        savedCityIds = savedCityIds,
        // The widget has no active city: nothing on a home screen is selected.
        activeCityId = null,
        use24Hour = use24Hour,
        now = now,
        deviceZoneId = deviceZoneId,
        cityLabels = cityLabels,
    )

    val deviceWeekday = CityTimeFormatter
        .snapshot(zoneId = deviceZoneId, instant = now, use24Hour = use24Hour)
        .weekdayShort

    val rows = state.cities.map { city ->
        WidgetRow(
            id = city.id,
            displayName = city.displayName,
            timeText = city.timeText,
            weekdayLabel = city.weekdayLabel,
            isDaytime = city.isDaytime,
            isAnotherDay = city.weekdayLabel != deviceWeekday,
        )
    }

    return WidgetUiState(
        deviceTimeText = state.deviceLocal?.timeText.orEmpty(),
        rows = rows,
    )
}
