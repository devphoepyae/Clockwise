package com.pj95.clockwise.model.alarm

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * The days an alarm repeats on, packed into an `Int`.
 *
 * Monday is bit 0 so the mask reads in calendar order and `WEEKDAYS` is simply
 * the first five bits. `0` means "once": the alarm rings at the next occurrence
 * of its time and switches itself off, which is the behaviour a person expects
 * from a one-shot alarm rather than a hidden repeat.
 *
 * Pure Kotlin with no Android types, so the scheduling rule is unit-testable on
 * the JVM.
 */
object RepeatDays {

    const val NONE: Int = 0
    const val MONDAY: Int = 1
    const val TUESDAY: Int = 1 shl 1
    const val WEDNESDAY: Int = 1 shl 2
    const val THURSDAY: Int = 1 shl 3
    const val FRIDAY: Int = 1 shl 4
    const val SATURDAY: Int = 1 shl 5
    const val SUNDAY: Int = 1 shl 6

    const val WEEKDAYS: Int = MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY
    const val WEEKEND: Int = SATURDAY or SUNDAY
    const val EVERY_DAY: Int = WEEKDAYS or WEEKEND

    /** The single bit for [day]. */
    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    /** True when [mask] repeats on [day]. */
    fun includes(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0

    /** The selected days in calendar order (Monday first). */
    fun days(mask: Int): List<DayOfWeek> =
        DayOfWeek.values().filter { includes(mask, it) }

    /** The mask with [day] switched on or off. */
    fun with(mask: Int, day: DayOfWeek, selected: Boolean): Int =
        if (selected) mask or bit(day) else mask and bit(day).inv()
}

/**
 * An alarm as the app thinks about it.
 *
 * The value is deliberately independent of Room and of the Android alarm clock:
 * [nextTrigger] is the one piece of arithmetic that decides whether a phone
 * wakes someone up, so it is a pure function that tests can pin to a zone and an
 * instant.
 */
data class Alarm(
    val id: Long = NEW,
    /** 0-23, in the device's own zone. */
    val hour: Int,
    /** 0-59. */
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    /** See [RepeatDays]; `0` is a one-shot alarm. */
    val repeatDays: Int = RepeatDays.NONE,
    /**
     * The one calendar day a scheduled alarm may ring on, as an epoch day.
     *
     * `null` means "follow the repeat rule". A value makes the alarm a one-shot
     * pinned to that date: [nextTrigger] considers no other day, and the
     * receiver clears the field once it has rung. Repeat days and a scheduled
     * date are mutually exclusive in the editor.
     */
    val scheduledDateEpochDay: Long? = null,
    /**
     * Whether the phone should also vibrate while this alarm rings.
     *
     * Defaults to true, which is what every alarm did before the switch existed.
     */
    val vibrate: Boolean = true,
    /**
     * The alarm sound, as a content/ringtone URI.
     *
     * Empty means the device's own alarm sound, which is what an alarm set
     * before the picker existed keeps. A stale URI — a sound the user later
     * removed — is handled at ring time by falling back to that default rather
     * than ringing silently.
     */
    val soundUri: String = "",
    /**
     * When the current snooze is due, or `null` when the alarm is not snoozed.
     *
     * The receiver compares this with the fire instant to tell a snooze re-fire
     * from a natural one. Without it, re-arming a snoozed alarm would look like
     * a fresh morning and hand the user another snooze.
     */
    val snoozedAtEpochMillis: Long? = null,
    /** Snoozes taken in the current ring cycle; reset on a natural fire. */
    val snoozesUsed: Int = 0,
    /**
     * The user's chosen position in the alarm list.
     *
     * New alarms are appended; a long-press drag rewrites this for the whole
     * list. Scheduling never reads it — it exists only so the list stays in the
     * order the user arranged.
     */
    val sortOrder: Int = 0,
) {

    /** True when the alarm rings once and then switches itself off. */
    val isOneShot: Boolean get() = repeatDays == RepeatDays.NONE

    /**
     * The next moment this alarm should ring, or `null` when it has no future
     * occurrence.
     *
     * A one-shot alarm looks at today and tomorrow only. A repeating alarm scans
     * the next week, which is enough for every mask: seven consecutive days
     * always contain the weekday it repeats on. The scan is inclusive of today,
     * so an alarm set for later this evening still fires tonight.
     *
     * The candidate is built with [ZonedDateTime.of], not by adding seconds to
     * the current time, because the transition into or out of daylight saving
     * can move the wall clock: an alarm set for 07:30 must read 07:30 on the
     * clock face, not "24 hours after the last ring".
     */
    fun nextTrigger(now: ZonedDateTime): Instant? {
        // A scheduled alarm is pinned to one date. If that moment has passed it
        // has no future occurrence at all, rather than rolling to the next day
        // like a one-shot alarm does.
        scheduledDateEpochDay?.let { epochDay ->
            val candidate = LocalDate.ofEpochDay(epochDay)
                .atTime(hour, minute)
                .atZone(now.zone)
            return if (candidate.isAfter(now)) candidate.toInstant() else null
        }

        val today = now.toLocalDate()
        for (offset in 0..LAST_SEARCH_DAY) {
            val date = today.plusDays(offset.toLong())
            if (!isOneShot && !RepeatDays.includes(repeatDays, date.dayOfWeek)) continue

            val candidate = date.atTime(hour, minute).atZone(now.zone)
            if (candidate.isAfter(now)) return candidate.toInstant()
        }
        return null
    }

    companion object {
        /** The id of an alarm that has not been stored yet. */
        const val NEW: Long = 0L

        /**
         * Days to scan for the next occurrence. Seven covers every repeat mask;
         * one-shot alarms only ever use the first two.
         */
        private const val LAST_SEARCH_DAY = 7
    }
}
