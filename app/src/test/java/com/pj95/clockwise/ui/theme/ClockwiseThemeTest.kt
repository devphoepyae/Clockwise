package com.pj95.clockwise.ui.theme

import com.pj95.clockwise.model.prefs.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The theme-to-palette rule (spec section 8).
 *
 * [paletteFor] is pure, so the whole mapping is checked here without a device, a
 * Robolectric configuration or a render. What each palette *looks* like is
 * [ClockwisePaletteTest]'s business; this file only pins which theme paints
 * which palette.
 */
class ClockwiseThemeTest {

    @Test
    fun everyThemePaintsItsOwnPalette() {
        assertEquals(LightClockwisePalette, paletteFor(AppTheme.LIGHT_MONOCHROME))
        assertEquals(WarmSepiaClockwisePalette, paletteFor(AppTheme.WARM_SEPIA))
        assertEquals(CoolSlateClockwisePalette, paletteFor(AppTheme.COOL_SLATE))
        // The device-independent half of Material You. `rememberClockwisePalette`
        // replaces it with a wallpaper-derived scheme on Android 12+.
        assertEquals(MaterialYouFallbackPalette, paletteFor(AppTheme.MATERIAL_YOU))
    }

    @Test
    fun onlyTheMaterialYouFallbackIsDark() {
        // Everything that keys off "is this a dark palette" — the Material
        // colour scheme's defaults, the status bar icons — has to agree with
        // the three fixed themes, which are all light. Material You's fallback
        // follows the system light/dark setting instead, so the light half is
        // what `paletteFor` returns.
        val darkThemes = AppTheme.entries.filter { paletteFor(it).isDark }

        assertTrue(darkThemes.isEmpty())
        assertTrue(MaterialYouDarkFallbackPalette.isDark)
    }

    @Test
    fun noTwoThemesLookTheSame() {
        AppTheme.entries.forEach { theme ->
            val other = AppTheme.entries.filter { it != theme }
            other.forEach { candidate ->
                assertTrue(
                    "$theme and $candidate share a palette",
                    paletteFor(theme) != paletteFor(candidate),
                )
            }
        }
    }

    @Test
    fun theRetiredThemeIdsAreNeverReused() {
        // The ids are what is persisted, so the numbering is part of the stored
        // format. True Dark (1) and High Contrast (4) were retired in 3.1.0;
        // their numbers are left unused rather than handed to a new theme.
        assertEquals(listOf(0, 2, 3, 5), AppTheme.entries.map { it.id })
        assertEquals(AppTheme.LIGHT_MONOCHROME, AppTheme.fromId(0))
        assertEquals(AppTheme.WARM_SEPIA, AppTheme.fromId(2))
        assertEquals(AppTheme.COOL_SLATE, AppTheme.fromId(3))
        assertEquals(AppTheme.MATERIAL_YOU, AppTheme.fromId(5))
    }

    @Test
    fun aRetiredThemeIdReadsAsTheDefault() {
        // An install that chose True Dark or High Contrast has that choice in
        // its store. The app has no palette to give it, and falling back to the
        // default is better than a crash or an invisible screen.
        assertEquals(AppTheme.Default, AppTheme.fromId(1))
        assertEquals(AppTheme.Default, AppTheme.fromId(4))
    }

    @Test
    fun theDefaultThemeIsLightMonochrome() {
        assertEquals(AppTheme.LIGHT_MONOCHROME, AppTheme.Default)
        assertEquals(0, AppTheme.Default.id)
        // Anything out of range reads as the default rather than throwing: a
        // store written by a newer build must not crash an older one.
        assertEquals(AppTheme.Default, AppTheme.fromId(-1))
        assertEquals(AppTheme.Default, AppTheme.fromId(99))
    }
}
