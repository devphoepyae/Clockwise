package com.pj95.clockwise.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pj95.clockwise.di.appContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * Everything the stopwatch section renders.
 *
 * [elapsedMillis] is derived from a start instant plus the time accumulated
 * before the last pause, so a dropped frame cannot make the stopwatch run slow.
 */
data class StopwatchUiState(
    val elapsedMillis: Long = 0,
    val running: Boolean = false,
) {
    val isIdle: Boolean get() = !running && elapsedMillis == 0L
}

/**
 * An in-memory stopwatch.
 *
 * A stopwatch is a live measurement, not user data: there is nothing worth
 * restoring after the process dies, and persisting it would make a timer the
 * user forgot about reappear days later. The state survives a section switch
 * because the view model outlives the composable.
 */
class StopwatchViewModel(
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val mutableState = MutableStateFlow(StopwatchUiState())
    val uiState: StateFlow<StopwatchUiState> = mutableState.asStateFlow()

    private var accumulatedMillis = 0L
    private var startedAtMillis = 0L
    private var ticker: Job? = null

    /** Starts from zero, or resumes from the frozen elapsed time. */
    fun start() {
        if (mutableState.value.running) return
        startedAtMillis = clock.millis()
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                mutableState.value = StopwatchUiState(
                    elapsedMillis = accumulatedMillis + (clock.millis() - startedAtMillis),
                    running = true,
                )
                delay(TICK_MILLIS)
            }
        }
    }

    fun pause() {
        if (!mutableState.value.running) return
        accumulatedMillis += clock.millis() - startedAtMillis
        ticker?.cancel()
        ticker = null
        mutableState.value = StopwatchUiState(
            elapsedMillis = accumulatedMillis,
            running = false,
        )
    }

    fun toggle() {
        if (mutableState.value.running) pause() else start()
    }

    fun reset() {
        ticker?.cancel()
        ticker = null
        accumulatedMillis = 0L
        mutableState.value = StopwatchUiState()
    }

    companion object {
        /** Fast enough that the hundredths look alive without burning battery. */
        private const val TICK_MILLIS = 31L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("Application is not available")
                StopwatchViewModel(clock = app.appContainer.clock)
            }
        }
    }
}

/** Formats elapsed time as `MM:SS.cc`, adding hours only when needed. */
object StopwatchFormat {
    fun clock(elapsedMillis: Long): String {
        val total = elapsedMillis.coerceAtLeast(0)
        val centis = (total / 10) % 100
        val totalSeconds = total / 1_000
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centis)
        } else {
            "%02d:%02d.%02d".format(minutes, seconds, centis)
        }
    }
}
