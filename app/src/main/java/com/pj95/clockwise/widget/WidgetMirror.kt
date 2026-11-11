package com.pj95.clockwise.widget

import com.pj95.clockwise.data.repository.ClockRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the home-screen widget in step with the app's saved cities.
 *
 * The minute ticker alone would leave a newly added city invisible for up to a
 * minute, so the same DataStore stream the screens use also drives a redraw:
 * add Tokyo in the app and the widget has it before the app is closed.
 *
 * Only the three inputs the widget actually depends on are watched — the saved
 * ids, their labels and the 12h/24h choice — so an unrelated change (the theme,
 * the active city) does not redraw a home screen for nothing.
 *
 * @param refresh the redraw itself, injected so this can be tested without a
 *   widget host
 * @param onError called when a redraw fails; injected so the failure is reported
 *   rather than swallowed. A single failed redraw must not end the mirror.
 */
class WidgetMirror(
    private val scope: CoroutineScope,
    private val repository: ClockRepository,
    private val refresh: suspend () -> Unit,
    private val onError: (Throwable) -> Unit,
) {

    fun start(): Job = scope.launch {
        repository.preferences
            .map { preferences ->
                Inputs(
                    savedCityIds = preferences.savedCityIds,
                    cityLabels = preferences.cityLabels,
                    use24Hour = preferences.use24Hour,
                )
            }
            .distinctUntilChanged()
            .collect {
                try {
                    refresh()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    onError(error)
                }
            }
    }

    private data class Inputs(
        val savedCityIds: List<String>,
        val cityLabels: Map<String, String>,
        val use24Hour: Boolean,
    )
}
