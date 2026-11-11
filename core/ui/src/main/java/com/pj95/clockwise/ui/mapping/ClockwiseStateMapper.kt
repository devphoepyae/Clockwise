package com.pj95.clockwise.ui.mapping

import com.pj95.clockwise.model.City
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.model.prefs.ThemeMode
import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.model.time.MarketSessions
import com.pj95.clockwise.model.time.TimeDifference
import com.pj95.clockwise.ui.DeviceLocalTime
import com.pj95.clockwise.ui.ClockwiseUiState
import com.pj95.clockwise.ui.components.CityCardModel
import java.time.Instant
import java.time.ZoneId

/**
 * Builds the renderable screen state from saved ids and the current instant.
 *
 * Pure and side-effect free so it can be exercised directly by tests and used
 * to produce deterministic preview/screenshot data.
 *
 * @param cityLabels the user's own labels keyed by city id; a missing key means
 *   "no label"
 * @param compareWithLocalTime false hides the difference line on every card,
 *   which is what the settings switch turns off
 */
fun buildClockwiseUiState(
    savedCityIds: List<String>,
    activeCityId: String?,
    use24Hour: Boolean,
    now: Instant,
    deviceZoneId: ZoneId,
    theme: AppTheme = AppTheme.Default,
    themeMode: ThemeMode = ThemeMode.Default,
    cityLabels: Map<String, String> = emptyMap(),
    onboardingCompleted: Boolean = true,
    compareWithLocalTime: Boolean = false,
): ClockwiseUiState {
    val cities = CityDirectory.citiesForIds(savedCityIds).map { city ->
        city.toCard(
            now = now,
            use24Hour = use24Hour,
            activeCityId = activeCityId,
            label = cityLabels[city.id]?.takeIf { it.isNotBlank() },
            deviceZoneId = deviceZoneId,
        )
    }

    // The FOREX strip is fixed: the four centres are shown even when they are
    // not in the saved list, so the shortcut never shrinks after a deletion.
    val marketCities = MarketSessions.cityIds.mapNotNull { id ->
        cities.firstOrNull { it.id == id }
            ?: CityDirectory.find(id)?.toCard(
                now = now,
                use24Hour = use24Hour,
                activeCityId = activeCityId,
                label = cityLabels[id]?.takeIf { it.isNotBlank() },
                deviceZoneId = deviceZoneId,
            )
    }

    val deviceTime = CityTimeFormatter.snapshot(
        zoneId = deviceZoneId,
        instant = now,
        use24Hour = use24Hour,
    )

    return ClockwiseUiState(
        isLoading = false,
        now = now,
        use24Hour = use24Hour,
        theme = theme,
        themeMode = themeMode,
        activeCityId = activeCityId,
        cities = cities,
        marketCities = marketCities,
        deviceLocal = DeviceLocalTime(
            zoneId = deviceZoneId.id,
            offsetLabel = deviceTime.offsetLabel,
            timeText = deviceTime.timeText,
        ),
        onboardingCompleted = onboardingCompleted,
        compareWithLocalTime = compareWithLocalTime,
    )
}

/**
 * One renderable card. Every card compares with the device's own time, which is
 * the reference the detail screen also spells out; comparing against the active
 * city instead would always read "Same time" there, because opening a city makes
 * it active (spec section 4.5).
 */
private fun City.toCard(
    now: Instant,
    use24Hour: Boolean,
    activeCityId: String?,
    label: String?,
    deviceZoneId: ZoneId,
): CityCardModel {
    val zoneId = ZoneId.of(timeZone)
    val time = CityTimeFormatter.snapshot(
        zoneId = zoneId,
        instant = now,
        use24Hour = use24Hour,
    )
    return CityCardModel(
        id = id,
        offsetLabel = time.offsetLabel,
        weekdayLabel = time.weekdayShort,
        name = name,
        region = region,
        timeText = time.timeText,
        isDaytime = time.isDaytime,
        isActive = id == activeCityId,
        label = label,
        difference = TimeDifference.between(zoneId, deviceZoneId, now),
        latitude = latitude,
        longitude = longitude,
    )
}
