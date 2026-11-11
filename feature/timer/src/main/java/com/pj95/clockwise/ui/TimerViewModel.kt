package com.pj95.clockwise.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pj95.clockwise.di.appContainer
import com.pj95.clockwise.data.repository.TimerRepository
import com.pj95.clockwise.model.timer.CountdownTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * Everything the timer section renders.
 *
 * [remainingMillis] is computed on a quarter-second cadence while the timer
 * runs. Storing a decremented value would drift; storing an end instant and
 * subtracting is exact, and a missed tick only delays the next redraw.
 */
data class TimerUiState(
    val isLoading: Boolean = true,
    val timer: CountdownTimer? = null,
    val remainingMillis: Long = 0,
    val exactAlarmsAllowed: Boolean = true,
) {
    val isRunning: Boolean get() = timer?.running == true

    val isPaused: Boolean get() = timer != null && !timer.running && timer.remainingMillis > 0

    val isFinished: Boolean get() = timer?.isFinished == true

    val hasTimer: Boolean get() = timer != null
}

/**
 * Drives the timer section and owns its quarter-second redraw.
 *
 * The ticker only runs while a timer is counting: with none, or with a paused
 * one, the state is a single value and the coroutine does nothing. The lifetime
 * is tied to `WhileSubscribed`, so it stops with the screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModel(
    private val repository: TimerRepository,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val now: Flow<Long> = flow {
        while (true) {
            emit(clock.millis())
            delay(TICK_MILLIS)
        }
    }

    val uiState: StateFlow<TimerUiState> = repository.timer
        .flatMapLatest { timer ->
            if (timer?.running == true) {
                now.map { instant ->
                    TimerUiState(
                        isLoading = false,
                        timer = timer,
                        remainingMillis = timer.remainingAt(instant),
                        exactAlarmsAllowed = repository.canScheduleExact(),
                    )
                }
            } else {
                flowOf(
                    TimerUiState(
                        isLoading = false,
                        timer = timer,
                        remainingMillis = timer?.remainingMillis ?: 0,
                        exactAlarmsAllowed = repository.canScheduleExact(),
                    ),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TimerUiState(),
        )

    fun start(durationMillis: Long) {
        if (durationMillis <= 0) return
        viewModelScope.launch { repository.start(durationMillis) }
    }

    fun pause() {
        viewModelScope.launch { repository.pause() }
    }

    fun resume() {
        viewModelScope.launch { repository.resume() }
    }

    /** Adds time to the live timer; ignored when there is nothing to extend. */
    fun extendBy(deltaMillis: Long) {
        if (deltaMillis <= 0) return
        viewModelScope.launch { repository.extendBy(deltaMillis) }
    }

    /** Runs the same duration again after it has finished. */
    fun restart() {
        val duration = uiState.value.timer?.durationMillis ?: return
        start(duration)
    }

    fun reset() {
        viewModelScope.launch { repository.reset() }
    }

    companion object {
        private const val TICK_MILLIS = 250L
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("Application is not available")
                TimerViewModel(
                    repository = app.appContainer.timerRepository,
                    clock = app.appContainer.clock,
                )
            }
        }
    }
}
