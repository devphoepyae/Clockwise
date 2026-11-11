package com.pj95.clockwise.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The user's saved-city order, persisted in Room.
 *
 * Membership is still tracked by Proto DataStore so an existing install keeps
 * its saved cities when it upgrades. This table owns the *display order*: one
 * row per saved city, ordered by [position]. Keeping the order here makes a
 * drag-and-drop reorder a normal relational write and gives the list a single,
 * observable source of truth for "what order are the cities in?".
 *
 * [position] is deliberately not unique. SQLite cannot express "unique per
 * table" without a partial index, and a transient duplicate during a reorder
 * must never make the database reject an otherwise valid write. The repository
 * always writes a dense 0-based order.
 */
@Entity(
    tableName = "saved_cities",
    indices = [Index("position")],
)
data class SavedCityEntity(
    @PrimaryKey val cityId: String,
    val position: Int,
)
