package com.pj95.clockwise.ui

import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.model.prefs.ThemeMode
import com.pj95.clockwise.ui.components.CityCardModel
import java.time.Instant

/** The device's own timezone and current time, shown on the detail screen. */
data class DeviceLocalTime(
    /** IANA id of the device timezone, e.g. "Asia/Yangon". */
    val zoneId: String,
    /** e.g. "UTC+6:30" */
    val offsetLabel: String,
    /** e.g. "16:40" */
    val timeText: String,
)

/**
 * Everything the clock screens render at one point in time.
 *
 * @param now the instant all displayed times were computed from; refreshed once
 *   a second by the ticker
 * @param theme the persisted legacy palette; kept for widgets and older callers
 * @param themeMode Clockwise's light/dark choice; the root theme reads this so
 *   it can be applied before any screen is composed
 * @param compareWithLocalTime whether clocks say how far they are from the
 *   device's own time; off unless the user turns it on
 */
data class ClockwiseUiState(
    val isLoading: Boolean = true,
    val now: Instant = Instant.EPOCH,
    val use24Hour: Boolean = false,
    val theme: AppTheme = AppTheme.Default,
    val themeMode: ThemeMode = ThemeMode.Default,
    val activeCityId: String? = null,
    val cities: List<CityCardModel> = emptyList(),
    /**
     * Sydney, Tokyo, London and New York — always all four, whether or not the
     * user has saved them, because the strip is the FOREX trader's fixed
     * shortcut. A saved city contributes its user label; an unsaved one is
     * built straight from the bundled directory.
     */
    val marketCities: List<CityCardModel> = emptyList(),
    val deviceLocal: DeviceLocalTime? = null,
    /** False only until the first-launch intro has been dismissed. */
    val onboardingCompleted: Boolean = true,
    val compareWithLocalTime: Boolean = false,
)
