package com.pj95.clockwise.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pj95.clockwise.di.appContainer
import com.pj95.clockwise.data.repository.AlarmRepository
import com.pj95.clockwise.model.alarm.Alarm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the alarm tab renders. */
data class AlarmUiState(
    val isLoading: Boolean = true,
    val alarms: List<Alarm> = emptyList(),
    /**
     * False only on Android 12/12L without the "Alarms & reminders" grant. The
     * tab then says so, because an alarm that rings a few minutes late is
     * something the user should know about before trusting it.
     */
    val exactAlarmsAllowed: Boolean = true,
)

/**
 * Drives the alarm list.
 *
 * The list is the repository's own flow, so a toggle written by this view model
 * and a ring written by the receiver both surface here without a refresh. Every
 * mutation is fired into [viewModelScope]: the sheet closes immediately and the
 * Room write plus the `AlarmManager` call finish a beat later.
 */
class AlarmViewModel(
    private val repository: AlarmRepository,
) : ViewModel() {

    val uiState: StateFlow<AlarmUiState> = repository.alarms
        .map { alarms ->
            AlarmUiState(
                isLoading = false,
                alarms = alarms,
                exactAlarmsAllowed = repository.canScheduleExact(),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AlarmUiState(),
        )

    /** Inserts or updates [alarm], then arms it when it is enabled. */
    fun save(alarm: Alarm) {
        viewModelScope.launch { repository.save(alarm) }
    }

    fun setEnabled(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(alarmId, enabled) }
    }

    fun delete(alarmId: Long) {
        viewModelScope.launch { repository.delete(alarmId) }
    }

    /** Puts a just-deleted alarm back, with its id and position intact. */
    fun restore(alarm: Alarm) {
        viewModelScope.launch { repository.restore(alarm) }
    }

    /** Persists the order produced by a long-press drag. */
    fun reorder(alarmIds: List<Long>) {
        viewModelScope.launch { repository.reorder(alarmIds) }
    }

    companion object {
        /** Keeps the list alive across configuration changes. */
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    ?: error("Application is not available")
                AlarmViewModel(repository = app.appContainer.alarmRepository)
            }
        }
    }
}
