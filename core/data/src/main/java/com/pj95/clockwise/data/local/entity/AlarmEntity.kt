package com.pj95.clockwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One alarm, stored in Room.
 *
 * Alarms are user data, not a cache, so they live in the same database the app
 * already migrates explicitly. The row is intentionally flat — hour, minute,
 * label, repeat mask, sound — because everything the scheduler needs has to be
 * readable in a process that may have started only to fire the alarm.
 *
 * [repeatDays] is a bitmask (see `domain.alarm.RepeatDays`), `0` meaning "once".
 * Storing seven booleans, or a serialised string, would make the common query
 * ("which days does this repeat on?") a parsing problem in a receiver.
 */
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 0-23, in the device's own zone. */
    val hour: Int,
    /** 0-59. */
    val minute: Int,
    /** Free text; empty means the screen shows a neutral "Alarm". */
    val label: String = "",
    val enabled: Boolean = true,
    /** Monday is bit 0 … Sunday is bit 6; `0` is a one-shot alarm. */
    val repeatDays: Int = 0,
    /**
     * The one calendar day a scheduled alarm may ring on, as an epoch day.
     *
     * Null means "follow the repeat rule". A value pins the alarm to a single
     * date, and the receiver clears it after the ring.
     */
    val scheduledDateEpochDay: Long? = null,
    /** Whether the phone vibrates while this alarm rings. */
    val vibrate: Boolean = true,
    /**
     * The chosen alarm sound URI, or `""` for the device default.
     *
     * A string rather than a resolved resource: the URI is what the notification
     * channel needs, and it survives the sound list changing between builds.
     */
    val soundUri: String = "",
    /**
     * How often the alarm may be snoozed, as `SnoozePolicy.ordinal`.
     *
     * An `Int` rather than an enum with a `TypeConverter`: the value is written
     * by one build and read by the next, and an ordinal column cannot fail to
     * convert at runtime.
     */
    val snoozePolicy: Int = 0,
    /** Snoozes taken in the current ring cycle; reset on a natural fire. */
    val snoozesUsed: Int = 0,
    /**
     * When the current snooze is due, or `null` when the alarm is not snoozed.
     *
     * The receiver compares this with the fire instant to tell a snooze re-fire
     * from a natural one. Without it, re-arming a snoozed alarm would look like
     * a fresh morning and hand the user another snooze.
     */
    val snoozedAtEpochMillis: Long? = null,
    /**
     * The user's chosen position in the alarm list.
     *
     * Alarms used to be ordered by time of day. v4.1.0 lets a long-press drag
     * put the alarm that matters first, so the stored position becomes the
     * order. A migration backfills existing rows by time, which is exactly the
     * list the user had before the gesture existed.
     */
    val sortOrder: Int = 0,
)
