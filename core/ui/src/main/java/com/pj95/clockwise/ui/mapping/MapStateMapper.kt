package com.pj95.clockwise.ui.mapping

import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.model.solar.SolarEvents
import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.ui.MapSolarSummary
import com.pj95.clockwise.ui.MapUiState
import com.pj95.clockwise.ui.ClockwiseUiState
import java.time.ZoneId

/**
 * Builds the map's state for either live time or a chosen minute of today.
 *
 * Like the other mappers this is pure: the map screen never calculates time,
 * zones or solar events itself, so the whole screen can be exercised at a fixed
 * instant without a device.
 *
 * @param travelMinute the minute of the device's current day to show, or `null`
 *   to follow the clock
 * @param solar a already-loaded solar result for the active city and date; it is
 *   ignored if it belongs to a different city or date, so a slow load from a
 *   previous slider position can never be shown against the wrong day
 */
fun buildMapUiState(
    state: ClockwiseUiState,
    deviceZoneId: ZoneId,
    travelMinute: MinuteOfDay? = null,
    solar: MapSolarSummary? = null,
): MapUiState {
    val today = state.now.atZone(deviceZoneId).toLocalDate()
    val instant = travelMinute?.onDate(today, deviceZoneId) ?: state.now

    // If the stored active id no longer belongs to the saved list, the map (and
    // the rest of the app) falls back to the first city rather than showing no
    // selection.
    val activeCityId = state.activeCityId
        ?.takeIf { active -> state.cities.any { it.id == active } }
        ?: state.cities.firstOrNull()?.id

    val mapClock = buildClockwiseUiState(
        savedCityIds = state.cities.map { it.id },
        activeCityId = activeCityId,
        use24Hour = state.use24Hour,
        now = instant,
        deviceZoneId = deviceZoneId,
        theme = state.theme,
        cityLabels = state.cities
            .mapNotNull { card -> card.label?.let { label -> card.id to label } }
            .toMap(),
    )

    val deviceTime = CityTimeFormatter.snapshot(
        zoneId = deviceZoneId,
        instant = instant,
        use24Hour = state.use24Hour,
    )
    val activeCity = mapClock.cities.firstOrNull { it.isActive } ?: mapClock.cities.firstOrNull()
    val activeDirectoryCity = activeCity?.let { CityDirectory.find(it.id) }
    val solarCityId = activeCity?.id
    val solarDate = activeDirectoryCity?.let { city ->
        instant.atZone(ZoneId.of(city.timeZone)).toLocalDate()
    }

    return MapUiState(
        isLive = travelMinute == null,
        instant = instant,
        minuteOfDay = MinuteOfDay.ofInstant(instant, deviceZoneId),
        deviceTimeText = deviceTime.timeText,
        deviceDateText = "${deviceTime.weekdayShort}, ${deviceTime.dateText}",
        deviceOffsetLabel = deviceTime.offsetLabel,
        solarCityId = solarCityId,
        solarDate = solarDate,
        cities = mapClock.cities,
        solar = solar?.takeIf { it.cityId == solarCityId && it.date == solarDate },
    )
}

/**
 * Formats one calculated solar day for the map.
 *
 * @param events the result from `SunriseSunsetCalculator` / `SolarRepository`
 * @param cityId the city the result belongs to, so a stale summary can be
 *   rejected by [buildMapUiState]
 * @param use24Hour the app-wide clock format
 */
fun buildMapSolarSummary(
    events: SolarEvents,
    cityId: String,
    use24Hour: Boolean,
): MapSolarSummary = MapSolarSummary(
    cityId = cityId,
    date = events.date,
    sunriseText = events.sunriseTime()?.let { time -> CityTimeFormatter.timeText(time, use24Hour) },
    sunsetText = events.sunsetTime()?.let { time -> CityTimeFormatter.timeText(time, use24Hour) },
    kind = events.kind,
)
