package com.pj95.clockwise.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One pinned home-screen widget instance.
 *
 * [appWidgetId] is the launcher's instance id from [android.app.AppWidgetManager],
 * not a style id. Several copies of the same style therefore get separate rows,
 * and each can name its own three cities.
 *
 * @param appWidgetId the platform's widget instance id
 * @param widgetType the stable style id this instance was created from
 * @param city1 first configured city id
 * @param city2 second configured city id
 * @param city3 third configured city id
 * @param isPremium true when the instance was pinned from a premium style
 */
@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
    @PrimaryKey val appWidgetId: Int,
    val widgetType: String,
    val city1: String = "Yangon",
    val city2: String = "London",
    val city3: String = "Tokyo",
    val isPremium: Boolean = false,
)
