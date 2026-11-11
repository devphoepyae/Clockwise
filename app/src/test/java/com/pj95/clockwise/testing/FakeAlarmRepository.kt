package com.pj95.clockwise.testing

import com.pj95.clockwise.data.repository.AlarmRepository
import com.pj95.clockwise.model.alarm.Alarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * An in-memory [AlarmRepository] for screens that only need a list to render.
 *
 * It deliberately does not touch `AlarmManager` or Room: the shell tests are
 * about navigation and layout, and the repository's own scheduling behaviour is
 * covered by `RoomAlarmRepositoryTest` with a recording scheduler.
 */
class FakeAlarmRepository(
    initial: List<Alarm> = emptyList(),
    private val exactAlarmsAllowed: Boolean = true,
) : AlarmRepository {

    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override val alarms: StateFlow<List<Alarm>> = state.asStateFlow()

    override fun canScheduleExact(): Boolean = exactAlarmsAllowed

    override suspend fun get(alarmId: Long): Alarm? =
        state.value.firstOrNull { it.id == alarmId }

    override suspend fun save(alarm: Alarm): Long {
        val stored = if (alarm.id == Alarm.NEW) {
            val position = (state.value.maxOfOrNull { it.sortOrder } ?: -1) + 1
            alarm.copy(id = nextId++, sortOrder = position)
        } else {
            alarm
        }
        state.update { current ->
            (current.filterNot { it.id == stored.id } + stored).sortedWith(ALARM_ORDER)
        }
        return stored.id
    }

    override suspend fun delete(alarmId: Long) {
        state.update { current -> current.filterNot { it.id == alarmId } }
    }

    override suspend fun restore(alarm: Alarm) {
        state.update { current ->
            (current.filterNot { it.id == alarm.id } + alarm).sortedWith(ALARM_ORDER)
        }
    }

    override suspend fun reorder(alarmIds: List<Long>) {
        state.update { current ->
            val byId = current.associateBy { it.id }
            val reordered = alarmIds.mapNotNull(byId::get)
                .mapIndexed { index, alarm -> alarm.copy(sortOrder = index) }
            val rest = current.filterNot { it.id in alarmIds }
                .mapIndexed { index, alarm -> alarm.copy(sortOrder = reordered.size + index) }
            reordered + rest
        }
    }

    override suspend fun setEnabled(alarmId: Long, enabled: Boolean) {
        state.update { current ->
            current.map { if (it.id == alarmId) it.copy(enabled = enabled) else it }
        }
    }

    override suspend fun rearm(alarmId: Long) = Unit

    override suspend fun rescheduleAll() = Unit

    override suspend fun snooze(alarmId: Long, minutes: Int) {
        val due = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        state.update { current ->
            current.map {
                if (it.id == alarmId) {
                    it.copy(
                        snoozesUsed = it.snoozesUsed + 1,
                        snoozedAtEpochMillis = due,
                    )
                } else {
                    it
                }
            }
        }
    }

    override suspend fun consumeSnooze(alarmId: Long) {
        state.update { current ->
            current.map {
                if (it.id == alarmId) it.copy(snoozedAtEpochMillis = null) else it
            }
        }
    }

    override suspend fun resetSnoozeCycle(alarmId: Long) {
        state.update { current ->
            current.map {
                if (it.id == alarmId) {
                    it.copy(snoozesUsed = 0, snoozedAtEpochMillis = null)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun completeScheduled(alarmId: Long) {
        state.update { current ->
            current.map {
                if (it.id == alarmId) {
                    it.copy(
                        enabled = false,
                        scheduledDateEpochDay = null,
                        snoozesUsed = 0,
                        snoozedAtEpochMillis = null,
                    )
                } else {
                    it
                }
            }
        }
    }

    private companion object {
        /** Mirrors the DAO's ORDER BY: chosen position first, then time. */
        val ALARM_ORDER = compareBy<Alarm>(
            { it.sortOrder },
            { it.hour },
            { it.minute },
            { it.id },
        )
    }
}
