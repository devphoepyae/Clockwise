package com.pj95.clockwise.data.prefs

import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.model.prefs.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the pruning and first-launch seeding rules (spec section 7). */
class UserPreferencesTest {

    @Test
    fun unknownCityIdsArePrunedAndActiveFallsBackToFirstRemaining() {
        val stored = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .addCityIds("atlantis")
            .addCityIds("paris")
            .setActiveCityId("atlantis")
            .build()

        val preferences = stored.toUserPreferences()

        assertEquals(listOf("tokyo", "paris"), preferences.savedCityIds)
        assertEquals("tokyo", preferences.activeCityId)
    }

    @Test
    fun savedOrderIsPreserved() {
        val stored = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("london")
            .addCityIds("tokyo")
            .addCityIds("berlin")
            .setActiveCityId("london")
            .build()

        assertEquals(listOf("london", "tokyo", "berlin"), stored.toUserPreferences().savedCityIds)
    }

    @Test
    fun anInitialisedEmptyListStaysEmpty() {
        val stored = SavedCities.newBuilder().setSeeded(true).build()

        val preferences = stored.toUserPreferences()

        assertTrue(preferences.savedCityIds.isEmpty())
        assertNull(preferences.activeCityId)
    }

    @Test
    fun missingActiveCityFallsBackToFirstSavedCity() {
        val stored = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("berlin")
            .addCityIds("paris")
            .build()

        assertEquals("berlin", stored.toUserPreferences().activeCityId)
    }

    @Test
    fun use24HourIsCarriedThrough() {
        val twentyFour = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setUse24Hour(true)
            .build()
        assertTrue(twentyFour.toUserPreferences().use24Hour)

        val twelve = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setUse24Hour(false)
            .build()
        assertFalse(twelve.toUserPreferences().use24Hour)
    }

    @Test
    fun seedingAppliesDefaultsOnlyWhenUnseeded() {
        val store = SavedCities.getDefaultInstance().seededWithDefaults()

        assertTrue(store.seeded)
        assertEquals(CityDirectory.defaultSavedCityIds, store.cityIdsList)
        assertEquals(CityDirectory.defaultActiveCityId, store.activeCityId)
        // The reference design opens in 24-hour mode.
        assertTrue(store.use24Hour)

        val alreadySeeded = SavedCities.newBuilder().setSeeded(true).build()
        assertEquals(alreadySeeded, alreadySeeded.seededWithDefaults())
    }

    // --- Theme ---------------------------------------------------------------

    @Test
    fun theStoredThemeIsCarriedThrough() {
        fun stored(theme: SavedAppTheme) = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setTheme(theme)
            .build()
            .toUserPreferences()
            .theme

        assertEquals(AppTheme.LIGHT_MONOCHROME, stored(SavedAppTheme.SAVED_APP_THEME_LIGHT_MONOCHROME))
        assertEquals(AppTheme.WARM_SEPIA, stored(SavedAppTheme.SAVED_APP_THEME_WARM_SEPIA))
        assertEquals(AppTheme.COOL_SLATE, stored(SavedAppTheme.SAVED_APP_THEME_COOL_SLATE))
        assertEquals(AppTheme.MATERIAL_YOU, stored(SavedAppTheme.SAVED_APP_THEME_MATERIAL_YOU))
    }

    @Test
    fun aRetiredThemeInTheStoreReadsAsTheDefault() {
        // 3.1.0 removed True Dark and High Contrast. An install that chose one
        // still has that name in its store, and it has to open on something.
        fun stored(theme: SavedAppTheme) = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setTheme(theme)
            .build()
            .toUserPreferences()
            .theme

        assertEquals(AppTheme.Default, stored(SavedAppTheme.SAVED_APP_THEME_TRUE_DARK))
        assertEquals(AppTheme.Default, stored(SavedAppTheme.SAVED_APP_THEME_HIGH_CONTRAST))
    }

    @Test
    fun aStoreWrittenBeforeTheFiveThemesIsMigrated() {
        fun migrated(mode: SavedThemeMode) = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setThemeMode(mode)
            .build()
            .toUserPreferences()
            .theme

        // Light mode was the light monochrome palette, so those users keep it.
        // Dark mode was True Dark, which 3.1.0 retired, and "follow the device"
        // cannot be resolved after the fact; both land on the default rather
        // than guessing a theme the user never chose.
        assertEquals(AppTheme.LIGHT_MONOCHROME, migrated(SavedThemeMode.THEME_MODE_LIGHT))
        assertEquals(AppTheme.Default, migrated(SavedThemeMode.THEME_MODE_DARK))
        assertEquals(AppTheme.LIGHT_MONOCHROME, migrated(SavedThemeMode.THEME_MODE_SYSTEM))
    }

    @Test
    fun aStoreWithNoThemeAtAllReadsAsTheDefault() {
        // Neither field, exactly like a store written before theme support
        // existed.
        val legacy = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setActiveCityId("tokyo")
            .build()

        assertEquals(AppTheme.Default, legacy.toUserPreferences().theme)
        assertFalse(legacy.hasTheme())
    }

    @Test
    fun anExplicitThemeWinsOverTheLegacyField() {
        // The presence bit is what makes this decidable: Light Monochrome is the
        // proto3 zero value, so without `hasTheme()` a stored Light Monochrome
        // would be indistinguishable from "nothing stored" and the stale dark
        // preference would win.
        val stored = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setThemeMode(SavedThemeMode.THEME_MODE_DARK)
            .setTheme(SavedAppTheme.SAVED_APP_THEME_LIGHT_MONOCHROME)
            .build()

        assertTrue(stored.hasTheme())
        assertEquals(AppTheme.LIGHT_MONOCHROME, stored.toUserPreferences().theme)
    }

    @Test
    fun seedingDoesNotOverwriteAStoredThemeChoice() {
        val stored = SavedCities.newBuilder()
            .setTheme(SavedAppTheme.SAVED_APP_THEME_COOL_SLATE)
            .build()

        assertEquals(SavedAppTheme.SAVED_APP_THEME_COOL_SLATE, stored.seededWithDefaults().theme)
    }

    @Test
    fun themeMappingRoundTrips() {
        AppTheme.entries.forEach { theme ->
            assertEquals(theme, theme.toSavedAppTheme().toAppTheme())
        }
    }

    @Test
    fun bothFallbackPreferencesUseTheDefaultTheme() {
        assertEquals(AppTheme.Default, UserPreferences.Default.theme)
        assertEquals(AppTheme.Default, UserPreferences.Empty.theme)
        assertEquals(AppTheme.LIGHT_MONOCHROME, AppTheme.Default)
    }

    // --- City labels ---------------------------------------------------------

    @Test
    fun cityLabelsAreCarriedThrough() {
        val stored = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .putCityLabels("tokyo", "Head Office")
            .build()

        assertEquals(mapOf("tokyo" to "Head Office"), stored.toUserPreferences().cityLabels)
    }

    @Test
    fun aStoreWrittenBeforeLabelsHaveNone() {
        // No label field at all, exactly like a store written by an older build.
        val legacy = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setActiveCityId("tokyo")
            .build()

        assertTrue(legacy.toUserPreferences().cityLabels.isEmpty())
    }

    @Test
    fun labelsForUnknownCityIdsArePruned() {
        val stored = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .putCityLabels("tokyo", "Head Office")
            .putCityLabels("atlantis", "Lost")
            .build()

        assertEquals(mapOf("tokyo" to "Head Office"), stored.toUserPreferences().cityLabels)
    }

    @Test
    fun blankLabelsAreDropped() {
        val stored = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .putCityLabels("tokyo", "   ")
            .build()

        assertTrue(stored.toUserPreferences().cityLabels.isEmpty())
    }

    @Test
    fun aLabelOutlivesRemovingItsCityFromTheList() {
        // Removing a city is not the same as its id leaving the directory, so
        // re-adding the city brings the label back instead of losing it.
        val stored = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .putCityLabels("paris", "Date night")
            .build()

        val preferences = stored.toUserPreferences()

        assertFalse(preferences.savedCityIds.contains("paris"))
        assertEquals("Date night", preferences.cityLabels["paris"])
    }

    @Test
    fun bothFallbackPreferencesCarryNoLabels() {
        assertTrue(UserPreferences.Default.cityLabels.isEmpty())
        assertTrue(UserPreferences.Empty.cityLabels.isEmpty())
    }

    // --- Comparison with the local time --------------------------------------

    @Test
    fun aStoreWrittenBeforeTheComparisonSwitchStaysOff() {
        // Comparison is opt-in, so a store with no field reads as "off" and the
        // user is never shown a line they did not ask for.
        val legacy = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setActiveCityId("tokyo")
            .build()

        assertFalse(legacy.hasCompareWithLocalTime())
        assertFalse(legacy.toUserPreferences().compareWithLocalTime)
    }

    @Test
    fun anExplicitComparisonChoiceIsCarriedThrough() {
        fun stored(value: Boolean) = SavedCities.newBuilder()
            .setSeeded(true)
            .addCityIds("tokyo")
            .setCompareWithLocalTime(value)
            .build()
            .toUserPreferences()
            .compareWithLocalTime

        assertTrue(stored(true))
        // Explicitly off means off: the presence bit is what distinguishes it
        // from a store that predates the field.
        assertFalse(stored(false))
    }

    @Test
    fun bothFallbackPreferencesLeaveComparisonOff() {
        assertFalse(UserPreferences.Default.compareWithLocalTime)
        assertFalse(UserPreferences.Empty.compareWithLocalTime)
    }
}
