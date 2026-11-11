package com.pj95.clockwise.ui

import androidx.compose.runtime.Immutable
import com.pj95.clockwise.model.solar.SolarDayKind
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.ui.components.CityCardModel
import java.time.Instant
import java.time.LocalDate

/**
 * The Sun's behaviour in the selected city on the date the map is showing.
 *
 * The text is already formatted in the app-wide 12h/24h setting, so the screen
 * only has to choose the wording. [date] is the city's own civil date, which can
 * differ from the device's date when the map travels across the date line.
 */
@Immutable
data class MapSolarSummary(
    val cityId: String,
    val date: LocalDate,
    /** Sunrise as a local clock reading, or `null` on a polar day/night. */
    val sunriseText: String?,
    /** Sunset as a local clock reading, or `null` on a polar day/night. */
    val sunsetText: String?,
    val kind: SolarDayKind,
)

/**
 * Everything the world map renders.
 *
 * The map can show either live time or a chosen minute of the device's current
 * day. It therefore carries its own city rows, built at [instant], rather than
 * reusing the live rows from [ClockwiseUiState] — otherwise the markers would
 * still read "now" while the terminator had moved.
 *
 * @param isLive true while the map follows the clock; false during time travel
 * @param instant the absolute instant the map, markers and cards are drawn at
 * @param minuteOfDay the slider position, in the device's zone
 * @param deviceTimeText the device's time at [instant], e.g. "16:40"
 * @param deviceDateText the device's date at [instant], e.g. "Sat, 20 Jan"
 * @param solarDate the selected city's civil date at [instant], or `null` when
 *   nothing is saved; used to load [solar]
 * @param solar the selected city's solar events, or `null` while loading or
 *   when the active city changed and the old result no longer applies
 */
@Immutable
data class MapUiState(
    val isLive: Boolean = true,
    val instant: Instant = Instant.EPOCH,
    val minuteOfDay: MinuteOfDay = MinuteOfDay.Noon,
    val deviceTimeText: String = "",
    val deviceDateText: String = "",
    val deviceOffsetLabel: String = "",
    val solarCityId: String? = null,
    val solarDate: LocalDate? = null,
    val cities: List<CityCardModel> = emptyList(),
    val solar: MapSolarSummary? = null,
) {
    /** The active city, falling back to the first saved one like the screens do. */
    val activeCity: CityCardModel?
        get() = cities.firstOrNull { it.isActive } ?: cities.firstOrNull()
}
