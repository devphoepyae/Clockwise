package com.pj95.clockwise.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.pj95.clockwise.MainActivity
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.CityDirectory

/**
 * The cities offered as long-press launcher shortcuts.
 *
 * Membership is dynamic on purpose: the app has no city list to hard-code, and
 * a shortcut that outlived the city it named would be a dead end. Only the
 * first few saved cities are published — a long-press menu is a shortlist, not
 * a second copy of the Clocks screen — and only ids the bundled directory still
 * knows, so a catalog change can never leave a dangling shortcut.
 *
 * Publishing needs no permission and no network: it writes to the launcher's
 * own shortcut store through [ShortcutManagerCompat].
 */
object CityShortcuts {

    /** How many saved cities appear in the long-press menu. */
    const val MAX_CITY_SHORTCUTS = 3

    /**
     * The fixed quick actions published from `res/xml/shortcuts.xml`.
     *
     * The launcher's limit counts static and dynamic shortcuts together, so the
     * dynamic budget has to leave room for these or the publish call is rejected
     * on older devices (where the maximum is five).
     */
    const val STATIC_SHORTCUT_COUNT = 3

    /** Namespaced so a dynamic city shortcut cannot collide with a static one. */
    const val ID_PREFIX = "city-"

    /**
     * How many dynamic shortcuts fit when [maxTotal] are allowed per activity.
     *
     * Pure so the subtraction can be tested without a launcher. A launcher that
     * reports no room gets none rather than an exception.
     */
    fun dynamicShortcutLimit(maxTotal: Int): Int =
        (maxTotal - STATIC_SHORTCUT_COUNT).coerceIn(0, MAX_CITY_SHORTCUTS)

    /**
     * The [ShortcutInfoCompat] entries for [savedCityIds], in saved order.
     *
     * Pure apart from resource lookup, so the labels and intents can be checked
     * without a launcher. Duplicate ids are collapsed and unknown ids skipped.
     */
    fun build(
        context: Context,
        savedCityIds: List<String>,
        cityLabels: Map<String, String>,
        limit: Int = MAX_CITY_SHORTCUTS,
    ): List<ShortcutInfoCompat> {
        if (limit <= 0) return emptyList()

        val seen = mutableSetOf<String>()
        return savedCityIds.asSequence()
            .filter { seen.add(it) }
            .mapNotNull { cityId ->
                val city = CityDirectory.find(cityId) ?: return@mapNotNull null
                val displayName = cityLabels[cityId]?.takeIf { it.isNotBlank() } ?: city.name
                cityId to displayName
            }
            .take(limit)
            .mapIndexed { index, (cityId, displayName) ->
                ShortcutInfoCompat.Builder(context, ID_PREFIX + cityId)
                    .setShortLabel(displayName)
                    .setLongLabel(
                        context.getString(R.string.shortcut_city_long, displayName),
                    )
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_city))
                    .setIntent(cityIntent(context, cityId))
                    .setRank(index)
                    .build()
            }
            .toList()
    }

    /**
     * Publishes [savedCityIds] as the app's dynamic shortcuts.
     *
     * Replaces the previous set in one call, so removing a city also removes
     * its shortcut. A launcher that supports no shortcuts, or a platform that
     * refuses the update, is not a failure the user needs to see: the app works
     * without them.
     */
    fun publish(
        context: Context,
        savedCityIds: List<String>,
        cityLabels: Map<String, String>,
    ) {
        val limit = dynamicShortcutLimit(
            ShortcutManagerCompat.getMaxShortcutCountPerActivity(context),
        )
        if (limit <= 0) return

        ShortcutManagerCompat.setDynamicShortcuts(
            context,
            build(
                context = context,
                savedCityIds = savedCityIds,
                cityLabels = cityLabels,
                limit = limit,
            ),
        )
    }

    /** The Intent a city shortcut starts; the same deep link a widget row uses. */
    fun cityIntent(context: Context, cityId: String): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(MainActivity.EXTRA_CITY_ID, cityId)
            // Only a shortcut carries its own id, so the activity can report
            // usage without a widget tap being counted as one.
            .putExtra(MainActivity.EXTRA_SHORTCUT_ID, ID_PREFIX + cityId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}
