package com.pj95.clockwise.data.repository

import androidx.datastore.core.DataStore
import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.model.prefs.ThemeMode
import com.pj95.clockwise.data.prefs.SavedCities
import com.pj95.clockwise.data.prefs.UserPreferences
import com.pj95.clockwise.data.prefs.seededWithDefaults
import com.pj95.clockwise.data.prefs.toSavedAppTheme
import com.pj95.clockwise.data.prefs.toSavedThemeModeSetting
import com.pj95.clockwise.data.prefs.toUserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Single source of truth for saved cities, the active city and the 12h/24h
 * preference, backed by Proto DataStore (spec section 7).
 *
 * Failures never crash the app: reads fall back to the first-launch defaults
 * and writes are dropped, while a [PrefsError] is emitted so the UI can show a
 * non-blocking message.
 */
class ClockRepository(
    private val dataStore: DataStore<SavedCities>,
    /**
     * Room-backed display order. `null` keeps the historical behaviour of using
     * the order in Proto DataStore, which is what unit tests and previews want.
     * The production container always supplies the Room store.
     */
    private val cityOrder: CityOrderStore? = null,
) {

    private val _errors = MutableSharedFlow<PrefsError>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val errors: SharedFlow<PrefsError> = _errors.asSharedFlow()

    /**
     * Streams the current preferences. The first collection also seeds the
     * first-launch defaults and reconciles the Room order with the saved-city
     * list, so the first value already reflects both.
     */
    val preferences: Flow<UserPreferences> = flow {
        ensureInitialized()
        synchronizeOrder()
        if (cityOrder == null) {
            emitAll(storedPreferences())
        } else {
            emitAll(
                combine(storedPreferences(), storedOrder()) { preferences, order ->
                    preferences.copy(
                        savedCityIds = mergeOrder(
                            savedIds = preferences.savedCityIds,
                            storedOrder = order,
                        ),
                    )
                },
            )
        }
    }.distinctUntilChanged()

    private fun storedPreferences(): Flow<UserPreferences> =
        dataStore.data
            .map { it.toUserPreferences() }
            .catch { throwable ->
                if (throwable is IOException) {
                    _errors.tryEmit(PrefsError.ReadFailed)
                    emit(UserPreferences.Default)
                } else {
                    throw throwable
                }
            }

    /** A Room failure falls back to the order already in DataStore. */
    private fun storedOrder(): Flow<List<String>> =
        cityOrder?.order?.catch { emit(emptyList()) } ?: flowOf(emptyList())

    /** Seeds the first-launch defaults when the store has never been written. */
    suspend fun ensureInitialized() {
        mutate { it }
    }

    /** Adds [cityId] and makes it active. Already-saved cities are ignored. */
    suspend fun addCity(cityId: String) {
        mutate { current ->
            if (current.cityIdsList.contains(cityId)) {
                current
            } else {
                current.toBuilder()
                    .addCityIds(cityId)
                    .setActiveCityId(cityId)
                    .build()
            }
        }
        // Also repair a missing Room row for a city the store already held.
        synchronizeOrder()
    }

    /**
     * Removes [cityId]. When the active city is removed the first remaining city
     * becomes active, or no city when the list becomes empty.
     */
    suspend fun removeCity(cityId: String) {
        mutate { current ->
            val remaining = current.cityIdsList.filterNot { it == cityId }
            val activeId =
                if (current.activeCityId == cityId) remaining.firstOrNull().orEmpty()
                else current.activeCityId
            current.toBuilder()
                .clearCityIds()
                .addAllCityIds(remaining)
                .setActiveCityId(activeId)
                .build()
        }
        updateOrder { it.remove(cityId) }
        synchronizeOrder()
    }

    /**
     * Puts a removed city back, in the position and active state it had.
     *
     * Undo has to restore the list the user was looking at, not append the city
     * to the end and forget that it was the one being read. [orderedIds] is the
     * order from before the removal and [activeCityId] the city that was active
     * then; both are sanitised against what is actually saved now.
     */
    suspend fun restoreCity(
        cityId: String,
        orderedIds: List<String>,
        activeCityId: String?,
    ) {
        ensureInitialized()
        val current = try {
            dataStore.data.first().toUserPreferences()
        } catch (throwable: IOException) {
            _errors.tryEmit(PrefsError.ReadFailed)
            return
        }
        val withCity = if (cityId in current.savedCityIds) {
            current.savedCityIds
        } else {
            current.savedCityIds + cityId
        }
        val restored = sanitizeOrder(current = withCity, requested = orderedIds)
        val active = activeCityId?.takeIf { it in restored }
            ?: current.activeCityId.takeIf { it in restored }
            ?: restored.firstOrNull().orEmpty()

        mutate { stored ->
            stored.toBuilder()
                .clearCityIds()
                .addAllCityIds(restored)
                .setActiveCityId(active)
                .build()
        }
        updateOrder { it.replaceAll(restored) }
    }

    /**
     * Stores a new display order.
     *
     * [orderedIds] is user input, so it is sanitised: only currently saved ids
     * are accepted, duplicates are collapsed, and any saved id the caller left
     * out is appended in its previous position rather than silently dropped.
     * Room is the display-order authority; the DataStore list is updated in the
     * same call so the stored proto and the widget's fallback cannot disagree.
     */
    suspend fun reorderCities(orderedIds: List<String>) {
        ensureInitialized()
        val current = try {
            dataStore.data.first().toUserPreferences().savedCityIds
        } catch (throwable: IOException) {
            _errors.tryEmit(PrefsError.ReadFailed)
            return
        }
        val sanitized = sanitizeOrder(current = current, requested = orderedIds)
        if (sanitized == current) return

        mutate { stored ->
            stored.toBuilder()
                .clearCityIds()
                .addAllCityIds(sanitized)
                .build()
        }
        updateOrder { it.replaceAll(sanitized) }
    }

    /** Marks [cityId] as the active city. Unknown ids are ignored. */
    suspend fun selectCity(cityId: String) {
        mutate { current ->
            if (current.cityIdsList.contains(cityId)) {
                current.toBuilder().setActiveCityId(cityId).build()
            } else {
                current
            }
        }
    }

    /** Stores the 12h/24h display preference. */
    suspend fun setUse24Hour(use24Hour: Boolean) {
        mutate { current -> current.toBuilder().setUse24Hour(use24Hour).build() }
    }

    /**
     * Stores whether global clocks say how far they are from the device's time.
     *
     * Written explicitly both ways, so the stored value is the user's decision
     * rather than "whatever the absence of the field means".
     */
    suspend fun setCompareWithLocalTime(compare: Boolean) {
        mutate { current -> current.toBuilder().setCompareWithLocalTime(compare).build() }
    }

    /**
     * Stores the chosen theme.
     *
     * The legacy light/dark field is cleared in the same write so a store that
     * predates the five themes converges on the new field instead of carrying
     * two answers that can disagree.
     */
    suspend fun setTheme(theme: AppTheme) {
        mutate { current ->
            current.toBuilder()
                .setTheme(theme.toSavedAppTheme())
                .clearThemeMode()
                .build()
        }
    }

    /**
     * Stores Clockwise's light/dark choice for the dynamic-colour shell.
     *
     * Written explicitly both ways so the stored value is the user's decision
     * rather than "whatever the absence of the field means".
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        mutate { current ->
            current.toBuilder()
                .setThemeModeSetting(mode.toSavedThemeModeSetting())
                .build()
        }
    }

    /**
     * Stores a personal label ("Head Office") for [cityId].
     *
     * A `null` or blank label clears it, so the dialog's "remove" action and an
     * empty text field both end in the same state. Labels are keyed by city id,
     * so they survive reordering and are unaffected by changes to other cities.
     */
    suspend fun setCityLabel(cityId: String, label: String?) {
        val trimmed = label?.trim().orEmpty()
        mutate { current ->
            current.toBuilder()
                .apply {
                    if (trimmed.isEmpty()) {
                        removeCityLabels(cityId)
                    } else {
                        putCityLabels(cityId, trimmed)
                    }
                }
                .build()
        }
    }

    /** Marks the first-launch intro as seen. */
    suspend fun completeOnboarding() {
        mutate { current -> current.toBuilder().setOnboardingCompleted(true).build() }
    }

    /** Shows the intro again from the settings sheet. */
    suspend fun restartOnboarding() {
        mutate { current -> current.toBuilder().setOnboardingCompleted(false).build() }
    }

    /**
     * Seeds Room from DataStore on first use and repairs drift afterwards.
     *
     * A store upgraded from v2 has an empty Room table but an ordered
     * `city_ids` list, so that list is copied over. A later change made to one
     * side is reconciled by keeping the Room order, pruning ids DataStore no
     * longer holds, and appending newly saved ids.
     */
    private suspend fun synchronizeOrder() {
        val store = cityOrder ?: return
        try {
            val savedIds = dataStore.data.first().toUserPreferences().savedCityIds
            val roomOrder = store.getOrder()
            val reconciled = mergeOrder(savedIds = savedIds, storedOrder = roomOrder)
            if (reconciled != roomOrder) store.replaceAll(reconciled)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // A Room failure is not fatal: [storedOrder] emits an empty list and
            // the DataStore order is used until the next attempt succeeds.
        }
    }

    /** Runs a Room write, reporting rather than throwing a storage failure. */
    private suspend fun updateOrder(
        block: suspend (CityOrderStore) -> Unit,
    ) {
        val store = cityOrder ?: return
        try {
            block(store)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            _errors.tryEmit(PrefsError.WriteFailed)
        }
    }

    private suspend fun mutate(transform: (SavedCities) -> SavedCities) {
        try {
            dataStore.updateData { current -> transform(current.seededWithDefaults()) }
        } catch (throwable: IOException) {
            _errors.tryEmit(PrefsError.WriteFailed)
        }
    }

    private companion object {
        /**
         * Room wins when it has an order; ids it is missing are appended in
         * DataStore order so an add is visible before its Room row lands.
         */
        fun mergeOrder(savedIds: List<String>, storedOrder: List<String>): List<String> {
            if (storedOrder.isEmpty()) return savedIds
            val saved = savedIds.toSet()
            val ordered = storedOrder.filter { it in saved }.distinct()
            return ordered + savedIds.filter { it !in ordered }
        }

        fun sanitizeOrder(current: List<String>, requested: List<String>): List<String> {
            val valid = current.toSet()
            val ordered = LinkedHashSet<String>()
            requested.forEach { id -> if (id in valid) ordered.add(id) }
            current.forEach { id -> ordered.add(id) }
            return ordered.toList()
        }
    }
}
