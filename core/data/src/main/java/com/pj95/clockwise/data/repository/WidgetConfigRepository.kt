package com.pj95.clockwise.data.repository

import com.pj95.clockwise.data.local.dao.WidgetDao
import com.pj95.clockwise.data.local.entity.WidgetConfigEntity
import com.pj95.clockwise.model.widget.WidgetStyleId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * One pinned widget's configuration.
 *
 * @param appWidgetId the launcher's instance id
 * @param style the style the instance was pinned from
 * @param cityIds exactly three city ids, in display order
 */
data class WidgetConfig(
    val appWidgetId: Int,
    val style: WidgetStyleId,
    val cityIds: List<String>,
    val isPremium: Boolean,
)

/**
 * Reads and writes the three-city configuration behind each pinned widget.
 *
 * The provider reads this table directly, so a widget on the home screen does
 * not depend on the app being open. Missing rows fall back to the style's
 * defaults rather than rendering an empty card.
 */
class WidgetConfigRepository(
    private val dao: WidgetDao,
) {

    val configs: Flow<List<WidgetConfig>> = dao.observeConfigs().map { rows ->
        rows.mapNotNull { it.toDomain() }
    }

    suspend fun get(appWidgetId: Int): WidgetConfig? =
        dao.getConfig(appWidgetId)?.toDomain()

    suspend fun save(config: WidgetConfig) {
        dao.upsertConfig(config.toEntity())
    }

    suspend fun delete(appWidgetId: Int) {
        dao.deleteConfig(appWidgetId)
    }
}

/** Default cities for a new instance, in the order the card reads them. */
val DefaultWidgetCityIds: List<String> = listOf("yangon", "london", "tokyo")

fun WidgetConfigEntity.toDomain(): WidgetConfig? {
    val style = WidgetStyleId.fromId(widgetType) ?: return null
    return WidgetConfig(
        appWidgetId = appWidgetId,
        style = style,
        cityIds = listOf(city1, city2, city3),
        isPremium = isPremium,
    )
}

fun WidgetConfig.toEntity(): WidgetConfigEntity = WidgetConfigEntity(
    appWidgetId = appWidgetId,
    widgetType = style.id,
    city1 = cityIds.getOrElse(0) { DefaultWidgetCityIds[0] },
    city2 = cityIds.getOrElse(1) { DefaultWidgetCityIds[1] },
    city3 = cityIds.getOrElse(2) { DefaultWidgetCityIds[2] },
    isPremium = isPremium,
)
