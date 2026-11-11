package com.pj95.clockwise.data.repository

import com.pj95.clockwise.data.local.dao.AlarmDao
import com.pj95.clockwise.data.local.entity.AlarmEntity
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.domain.alarm.AlarmScheduling
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant

/**
 * The user's alarms, as the rest of the app sees them.
 *
 * An interface rather than a concrete class because two observers sit on either
 * side of it: the UI needs a flow it can render, and a receiver needs the same
 * rows without a screen. It also lets the alarm screen be unit-tested with a
 * fake store instead of a real database and a real `AlarmManager`.
 */
interface AlarmRepository {

    /** Every stored alarm, ordered by time of day. */
    val alarms: Flow<List<Alarm>>

    /**
     * True when the platform will honour an exact trigger.
     *
     * False means the app is running on Android 12/12L without the "Alarms &
     * reminders" grant; alarms still fire, but inexactly.
     */
    fun canScheduleExact(): Boolean

    suspend fun get(alarmId: Long): Alarm?

    /**
     * Inserts a new alarm (id [Alarm.NEW]) or updates an existing one, then
     * arms or disarms it. Returns the stored id.
     */
    suspend fun save(alarm: Alarm): Long

    suspend fun delete(alarmId: Long)

    /**
     * Re-inserts a deleted alarm with its original id, position and settings,
     * then arms it again when it was enabled.
     *
     * The Undo action has to put back the row the user just removed, not a new
     * alarm that happens to look like it: the id is what the editor, the
     * scheduler and any pending notification already refer to.
     */
    suspend fun restore(alarm: Alarm)

    /** Persists the order the user arranged with a long-press drag. */
    suspend fun reorder(alarmIds: List<Long>)

    suspend fun setEnabled(alarmId: Long, enabled: Boolean)

    /**
     * Re-arms the next occurrence of an enabled alarm without touching the row.
     *
     * Used after a repeat alarm rings: the row is unchanged, but its next ring
     * has to be placed a week (or a day) ahead.
     */
    suspend fun rearm(alarmId: Long)

    /** Re-arms every enabled alarm. Called at process start and after a reboot. */
    suspend fun rescheduleAll()

    /**
     * Snoozes [alarmId] for [minutes] and arms that one trigger.
     *
     * The row records the instant and increments its cycle count so the
     * receiver can tell this re-fire from a natural one. A snooze is allowed
     * even on a one-shot alarm that has already switched itself off: the alarm
     * is still ringing, and the user asked for ten more minutes of it.
     */
    suspend fun snooze(alarmId: Long, minutes: Int)

    /**
     * Forgets the snooze marker after a snoozed alarm fires, keeping its count.
     *
     * The re-fire belongs to the same ring cycle as the alarm it was snoozed
     * from, so the count must survive; only the marker is stale.
     */
    suspend fun consumeSnooze(alarmId: Long)

    /** Ends the ring cycle; the next natural fire starts with a clean count. */
    suspend fun resetSnoozeCycle(alarmId: Long)

    /**
     * Ends a scheduled one-off after it has rung.
     *
     * A scheduled alarm is pinned to one date, so once that date has fired it
     * has no future occurrence. Switching it off and clearing the date keeps
     * the card truthful instead of leaving a "scheduled" alarm that can never
     * ring again.
     */
    suspend fun completeScheduled(alarmId: Long)
}

/**
 * Room-backed implementation.
 *
 * Every write is followed by the matching `AlarmManager` call, in the same
 * coroutine, so a stored alarm and an armed alarm cannot drift apart: enabling
 * a switch arms it before the flow repaints, and deleting a row removes its
 * trigger before the card disappears.
 */
class RoomAlarmRepository(
    private val dao: AlarmDao,
    private val scheduling: AlarmScheduling,
    private val clock: Clock,
) : AlarmRepository {

    override val alarms: Flow<List<Alarm>> =
        dao.observeAll().map { rows -> rows.map(AlarmEntity::toDomain) }

    override fun canScheduleExact(): Boolean = scheduling.canScheduleExact()

    override suspend fun get(alarmId: Long): Alarm? = dao.getById(alarmId)?.toDomain()

    override suspend fun save(alarm: Alarm): Long {
        if (alarm.id == Alarm.NEW) {
            // A new alarm goes to the end of the user's arrangement rather than
            // being slotted by time: once the list is manually ordered, only
            // the user decides where the next one belongs.
            val position = dao.maxSortOrder() + 1
            val id = dao.insert(alarm.copy(sortOrder = position).toEntity())
            val stored = alarm.copy(id = id, sortOrder = position)
            armOrDisarm(stored)
            return id
        }

        dao.update(alarm.toEntity())
        // A row can disappear while the editor is open. An update to nothing is
        // still nothing: do not arm a trigger that would only find a null row.
        val stored = dao.getById(alarm.id)?.toDomain() ?: return alarm.id
        armOrDisarm(stored)
        return stored.id
    }

    private fun armOrDisarm(alarm: Alarm) {
        if (alarm.enabled) scheduling.schedule(alarm) else scheduling.cancel(alarm.id)
    }

    override suspend fun delete(alarmId: Long) {
        dao.deleteById(alarmId)
        scheduling.cancel(alarmId)
    }

    override suspend fun restore(alarm: Alarm) {
        // An explicit id makes this the same row the user deleted. If the alarm
        // was enabled, its trigger has to be rebuilt from the restored row.
        dao.insert(alarm.toEntity())
        if (alarm.enabled) scheduling.schedule(alarm)
    }

    override suspend fun reorder(alarmIds: List<Long>) {
        // Room writes the positions in one transaction. No scheduling call is
        // needed: moving a card never changes when its alarm rings.
        dao.reorder(alarmIds)
    }

    override suspend fun setEnabled(alarmId: Long, enabled: Boolean) {
        dao.setEnabled(alarmId, enabled)
        val alarm = dao.getById(alarmId)?.toDomain() ?: return
        if (enabled) scheduling.schedule(alarm) else scheduling.cancel(alarmId)
    }

    override suspend fun rearm(alarmId: Long) {
        val alarm = dao.getById(alarmId)?.toDomain() ?: return
        if (alarm.enabled) scheduling.schedule(alarm)
    }

    override suspend fun rescheduleAll() {
        val now = clock.millis()
        dao.getAll().forEach { row ->
            val snoozedAt = row.snoozedAtEpochMillis
            when {
                // A pending snooze is the soonest thing the user is waiting for,
                // and it survives a reboot only if it is re-armed here. This
                // includes a one-shot row that switched itself off at the first
                // ring but still owes the user ten more minutes.
                snoozedAt != null && snoozedAt > now ->
                    scheduling.scheduleSnooze(row.id, Instant.ofEpochMilli(snoozedAt))
                // A stale marker on a disabled one-shot row has already rung or
                // was abandoned; arming a past trigger would ring it again.
                row.enabled -> scheduling.schedule(row.toDomain())
            }
        }
    }

    override suspend fun snooze(alarmId: Long, minutes: Int) {
        // A notification action can outlive its row: never arm a trigger that
        // would only find a null alarm when it fires.
        if (dao.getById(alarmId) == null) return

        val triggerAt = clock.instant().plusSeconds(minutes.coerceAtLeast(1) * 60L)
        dao.markSnoozed(alarmId, triggerAt.toEpochMilli())
        scheduling.scheduleSnooze(alarmId, triggerAt)
    }

    override suspend fun consumeSnooze(alarmId: Long) {
        dao.clearSnoozeMarker(alarmId)
    }

    override suspend fun resetSnoozeCycle(alarmId: Long) {
        dao.resetSnoozeCycle(alarmId)
    }

    override suspend fun completeScheduled(alarmId: Long) {
        dao.completeScheduled(alarmId)
        scheduling.cancel(alarmId)
    }
}

/** The database row as the domain value. */
fun AlarmEntity.toDomain(): Alarm = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    label = label,
    enabled = enabled,
    repeatDays = repeatDays,
    scheduledDateEpochDay = scheduledDateEpochDay,
    vibrate = vibrate,
    soundUri = soundUri,
    snoozedAtEpochMillis = snoozedAtEpochMillis,
    snoozesUsed = snoozesUsed,
    sortOrder = sortOrder,
)

/**
 * The domain value as the database row.
 *
 * The v7 snooze columns are written again since v1.0.1: the ringing screen's
 * swipe-up and the notification's Snooze action both need the cycle count and
 * the due instant to survive process death between the ring and the re-fire.
 */
fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    label = label,
    enabled = enabled,
    repeatDays = repeatDays,
    scheduledDateEpochDay = scheduledDateEpochDay,
    vibrate = vibrate,
    soundUri = soundUri,
    snoozesUsed = snoozesUsed,
    snoozedAtEpochMillis = snoozedAtEpochMillis,
    sortOrder = sortOrder,
)
