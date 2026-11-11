package com.pj95.clockwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The one active timer, stored in Room.
 *
 * There is exactly one row, always [SINGLETON_ID]. A timer is a single piece of
 * state — "what is counting down right now" — rather than a list, so a fixed
 * primary key makes "replace whatever was there" the natural write and rules
 * out two timers racing each other.
 */
@Entity(tableName = "timer")
data class TimerEntity(
    @PrimaryKey val id: Long = SINGLETON_ID,
    /** What the user chose, kept so "Start again" needs no memory of its own. */
    val durationMillis: Long,
    /** The frozen remainder while paused, or the full duration when running. */
    val remainingMillis: Long,
    /** When it should ring, or `null` while paused. */
    val endAtEpochMillis: Long?,
    val running: Boolean,
    val label: String = "",
) {
    companion object {
        const val SINGLETON_ID: Long = 1L
    }
}
