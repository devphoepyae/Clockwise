package com.pj95.clockwise.shortcuts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pj95.clockwise.MainActivity
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.CityDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The dynamic long-press shortcuts: which cities are offered, what they say,
 * and where they point.
 *
 * The launcher itself is not involved — the built [androidx.core.content.pm.ShortcutInfoCompat]
 * entries are inspected directly, so the rules hold on a JVM.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CityShortcutsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun savedCitiesBecomeShortcutsInSavedOrder() {
        val shortcuts = CityShortcuts.build(
            context = context,
            savedCityIds = listOf("tokyo", "london", "sydney"),
            cityLabels = emptyMap(),
        )

        assertEquals(listOf("city-tokyo", "city-london", "city-sydney"), shortcuts.map { it.id })
        assertEquals(listOf("Tokyo", "London", "Sydney"), shortcuts.map { it.shortLabel.toString() })
        assertEquals(listOf(0, 1, 2), shortcuts.map { it.rank })
    }

    @Test
    fun theDefaultListIsLimitedToAHandfulOfCities() {
        val shortcuts = CityShortcuts.build(
            context = context,
            savedCityIds = CityDirectory.defaultSavedCityIds,
            cityLabels = emptyMap(),
        )

        assertEquals(CityShortcuts.MAX_CITY_SHORTCUTS, shortcuts.size)
        assertEquals(
            CityDirectory.defaultSavedCityIds.take(CityShortcuts.MAX_CITY_SHORTCUTS),
            shortcuts.map { it.id.removePrefix(CityShortcuts.ID_PREFIX) },
        )
    }

    @Test
    fun aPersonalLabelIsUsedInTheShortcut() {
        val shortcuts = CityShortcuts.build(
            context = context,
            savedCityIds = listOf("tokyo"),
            cityLabels = mapOf("tokyo" to "Head Office"),
        )

        assertEquals("Head Office", shortcuts.single().shortLabel.toString())
        assertEquals(
            context.getString(R.string.shortcut_city_long, "Head Office"),
            shortcuts.single().longLabel.toString(),
        )
    }

    @Test
    fun aBlankLabelFallsBackToTheCityName() {
        val shortcuts = CityShortcuts.build(
            context = context,
            savedCityIds = listOf("tokyo"),
            cityLabels = mapOf("tokyo" to "   "),
        )

        assertEquals("Tokyo", shortcuts.single().shortLabel.toString())
    }

    @Test
    fun duplicateAndUnknownIdsAreDropped() {
        val shortcuts = CityShortcuts.build(
            context = context,
            savedCityIds = listOf("tokyo", "atlantis", "tokyo", "london"),
            cityLabels = emptyMap(),
        )

        assertEquals(listOf("city-tokyo", "city-london"), shortcuts.map { it.id })
    }

    @Test
    fun aNegativeOrZeroLimitProducesNoShortcuts() {
        assertTrue(CityShortcuts.build(context, listOf("tokyo"), emptyMap(), limit = 0).isEmpty())
        assertTrue(CityShortcuts.build(context, listOf("tokyo"), emptyMap(), limit = -1).isEmpty())
    }

    @Test
    fun theDynamicBudgetLeavesRoomForTheThreeStaticActions() {
        // API 26-29 launchers allow five per activity; the three quick actions
        // in res/xml/shortcuts.xml already use three of them.
        assertEquals(2, CityShortcuts.dynamicShortcutLimit(5))
        assertEquals(0, CityShortcuts.dynamicShortcutLimit(3))
        assertEquals(0, CityShortcuts.dynamicShortcutLimit(0))
        // A launcher with a larger allowance still gets at most three cities,
        // because the long-press menu is a shortlist.
        assertEquals(3, CityShortcuts.dynamicShortcutLimit(6))
        assertEquals(3, CityShortcuts.dynamicShortcutLimit(15))
    }

    @Test
    fun eachShortcutOpensThatCityInTheApp() {
        val shortcut = CityShortcuts.build(
            context = context,
            savedCityIds = listOf("tokyo"),
            cityLabels = emptyMap(),
        ).single()

        val intent = shortcut.intent
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals("tokyo", intent.getStringExtra(MainActivity.EXTRA_CITY_ID))
        // The usage-reporting id is the shortcut's own id, not the city id.
        assertEquals(
            CityShortcuts.ID_PREFIX + "tokyo",
            intent.getStringExtra(MainActivity.EXTRA_SHORTCUT_ID),
        )
        // No stale shortcut action may survive on a city deep link.
        assertNull(intent.getStringExtra(MainActivity.EXTRA_SHORTCUT))
    }

    @Test
    fun theShortcutIntentIsOneShotAndReusesTheRunningApp() {
        val intent = CityShortcuts.cityIntent(context, "london")

        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP != 0)
        assertEquals("london", intent.getStringExtra(MainActivity.EXTRA_CITY_ID))
        assertEquals(
            CityShortcuts.ID_PREFIX + "london",
            intent.getStringExtra(MainActivity.EXTRA_SHORTCUT_ID),
        )
    }
}
