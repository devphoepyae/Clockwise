package com.pj95.clockwise.shortcuts

import com.pj95.clockwise.data.prefs.UserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the dynamic launcher shortcuts in step with the saved cities.
 *
 * The long-press menu is drawn by the launcher, so it cannot read the app's
 * store itself. This mirror watches only the two inputs the shortcuts depend on
 * — the saved ids and their labels — and republishes when either changes. A
 * theme change, a time-format change or a reorder (which is visible in the
 * saved ids) costs nothing extra.
 *
 * The publication itself is injected so the update rule is testable without a
 * launcher, and a failure is reported rather than allowed to kill the mirror:
 * shortcuts are a convenience, not a feature the app depends on.
 */
class ShortcutMirror(
    private val scope: CoroutineScope,
    private val preferences: Flow<UserPreferences>,
    private val publish: (savedCityIds: List<String>, cityLabels: Map<String, String>) -> Unit,
    private val onError: (Throwable) -> Unit = {},
) {

    fun start(): Job = scope.launch {
        preferences
            .map { ShortcutSnapshot(it.savedCityIds, it.cityLabels) }
            .distinctUntilChanged()
            .collect { snapshot ->
                try {
                    publish(snapshot.savedCityIds, snapshot.cityLabels)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    onError(error)
                }
            }
    }

    private data class ShortcutSnapshot(
        val savedCityIds: List<String>,
        val cityLabels: Map<String, String>,
    )
}
