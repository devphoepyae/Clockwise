package com.pj95.clockwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The local licence state for one widget style.
 *
 * Room, not Play Billing, is what the UI and the home-screen widget read. A
 * billing query refreshes this row when the Play Store is reachable; when it is
 * not, the last known state keeps working. [trialStartTimestamp] is `null` until
 * the user explicitly starts the three-day trial, so merely opening the gallery
 * never begins a countdown.
 *
 * @param widgetId stable style id, e.g. `free_slate_multifunction`
 * @param category `FREE`, `MATERIAL_YOU` or `AMBIENT`
 * @param isUnlocked true after a verified purchase
 * @param trialStartTimestamp wall-clock millis when the trial began, or `null`
 */
@Entity(tableName = "widget_licenses")
data class WidgetLicenseEntity(
    @PrimaryKey val widgetId: String,
    val category: String,
    val isUnlocked: Boolean = false,
    val trialStartTimestamp: Long? = null,
)
