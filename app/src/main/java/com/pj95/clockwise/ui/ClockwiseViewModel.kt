package com.pj95.clockwise.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.model.prefs.ThemeMode
import com.pj95.clockwise.di.appContainer
import com.pj95.clockwise.data.repository.ClockRepository
import com.pj95.clockwise.di.appContainer
import com.pj95.clockwise.data.repository.PrefsError
import com.pj95.clockwise.di.appContainer
import com.pj95.clockwise.data.repository.SolarRepository
import com.pj95.clockwise.model.solar.SolarEvents
import com.pj95.clockwise.ui.mapping.buildClockwiseUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Drives every clock screen.
 *
 * The state is the combination of the persisted preferences and the once-a-
 * second ticker. `SharingStarted.WhileSubscribed` means the ticker only runs
 * while a screen is actually observing it, so nothing ticks in the background.
 */
class ClockwiseViewModel(
    private val repository: ClockRepository,
    ticker: Flow<Instant>,
    private val deviceZoneId: () -> ZoneId = ZoneId::systemDefault,
    private val solarRepository: SolarRepository? = null,
) : ViewModel() {

    val uiState: StateFlow<ClockwiseUiState> = combine(
        repository.preferences,
        ticker,
    ) { preferences, now ->
        buildClockwiseUiState(
            savedCityIds = preferences.savedCityIds,
            activeCityId = preferences.activeCityId,
            use24Hour = preferences.use24Hour,
            now = now,
            deviceZoneId = deviceZoneId(),
            theme = preferences.theme,
            themeMode = preferences.themeMode,
            cityLabels = preferences.cityLabels,
            onboardingCompleted = preferences.onboardingCompleted,
            compareWithLocalTime = preferences.compareWithLocalTime,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ClockwiseUiState(),
    )

    /** Storage failures for the UI to surface as a non-blocking message. */
    val errors: SharedFlow<PrefsError> = repository.errors

    /**
     * The device's own timezone: the reference every difference is measured
     * against, and the zone the meeting planner's slot is read in.
     */
    val deviceZone: ZoneId get() = deviceZoneId()

    fun addCity(cityId: String) {
        viewModelScope.launch { repository.addCity(cityId) }
    }

    fun removeCity(cityId: String) {
        viewModelScope.launch { repository.removeCity(cityId) }
    }

    /** Restores a removed city to its previous position and active state. */
    fun restoreCity(cityId: String, orderedIds: List<String>, activeCityId: String?) {
        viewModelScope.launch { repository.restoreCity(cityId, orderedIds, activeCityId) }
    }

    fun selectCity(cityId: String) {
        viewModelScope.launch { repository.selectCity(cityId) }
    }

    /** Persists a drag-and-drop order; Room owns the stored sequence. */
    fun reorderCities(orderedIds: List<String>) {
        viewModelScope.launch { repository.reorderCities(orderedIds) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.completeOnboarding() }
    }

    fun restartOnboarding() {
        viewModelScope.launch { repository.restartOnboarding() }
    }

    fun setUse24Hour(use24Hour: Boolean) {
        viewModelScope.launch { repository.setUse24Hour(use24Hour) }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    /** Stores Clockwise's light/dark choice. */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    /** Shows or hides the "compared with your local time" line on every clock. */
    fun setCompareWithLocalTime(compare: Boolean) {
        viewModelScope.launch { repository.setCompareWithLocalTime(compare) }
    }

    /** Stores the user's own label for a city, or clears it when [label] is null. */
    fun setCityLabel(cityId: String, label: String?) {
        viewModelScope.launch { repository.setCityLabel(cityId, label) }
    }

    /**
     * Sunrise and sunset for [cityId] on [date], read through the cached solar
     * repository. Returns `null` when the city is unknown or the engine fails;
     * the map simply omits the solar line in that case rather than taking the
     * screen down.
     */
    suspend fun solarEvents(cityId: String, date: LocalDate): SolarEvents? {
        val solar = solarRepository ?: return null
        val city = CityDirectory.find(cityId) ?: return null
        return try {
            solar.events(city, date)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        /** Keeps the ticker alive across configuration changes. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("Application is not available")
                ClockwiseViewModel(
                    repository = app.appContainer.clockRepository,
                    ticker = app.appContainer.secondTicker.ticker(),
                    solarRepository = app.appContainer.solarRepository,
                )
            }
        }
    }
}
