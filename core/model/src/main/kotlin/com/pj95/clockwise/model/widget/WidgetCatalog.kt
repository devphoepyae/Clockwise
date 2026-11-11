package com.pj95.clockwise.model.widget

/**
 * The stable identity of a widget style.
 *
 * [id] is what Room stores and what a pinned widget's provider carries; the
 * enum name is never persisted. Adding a style therefore cannot change the
 * meaning of an existing licence row.
 *
 * The launch catalogue has three free styles and two premium styles. The free
 * trio covers the everyday clock and timer, plus the FOREX market-session
 * card; the premium pair is the Material You global-clocks card and the
 * ambient/grid card.
 */
enum class WidgetStyleId(
    val id: String,
    val tier: WidgetTier,
    val category: WidgetCategory,
) {
    FREE_SLATE_MULTIFUNCTION(
        id = "free_slate_multifunction",
        tier = WidgetTier.FREE,
        category = WidgetCategory.FREE,
    ),
    FREE_DIAL_24H(
        id = "free_dial_24h",
        tier = WidgetTier.FREE,
        category = WidgetCategory.FREE,
    ),
    FREE_MARKET_SESSIONS(
        id = "free_market_sessions",
        tier = WidgetTier.FREE,
        category = WidgetCategory.FREE,
    ),
    PREMIUM_GLOBAL_CLOCKS(
        // Frozen: this string is persisted in widget licence/config rows and
        // in pinned widget provider state. Renaming it would orphan them.
        id = "premium_clockwise",
        tier = WidgetTier.PREMIUM,
        category = WidgetCategory.MATERIAL_YOU,
    ),
    PREMIUM_AMBIENT_GRID(
        id = "premium_ambient_grid",
        tier = WidgetTier.PREMIUM,
        category = WidgetCategory.AMBIENT,
    ),
    ;

    val isPremium: Boolean get() = tier == WidgetTier.PREMIUM

    companion object {
        fun fromId(id: String): WidgetStyleId? = entries.firstOrNull { it.id == id }
    }
}

enum class WidgetTier { FREE, PREMIUM }

enum class WidgetCategory(val storageValue: String) {
    FREE("FREE"),
    MATERIAL_YOU("MATERIAL_YOU"),
    AMBIENT("AMBIENT");

    companion object {
        fun fromStored(value: String?): WidgetCategory =
            entries.firstOrNull { it.storageValue == value } ?: FREE
    }
}
