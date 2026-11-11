package com.pj95.clockwise.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.pj95.clockwise.model.prefs.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks down the palettes and the Material 3 colour scheme built from them
 * (spec section 8).
 *
 * The contrast checks are deliberately written as a ratio rather than as a set
 * of hard-coded hex values: they assert the *relationships* the design relies
 * on, so a future token tweak that breaks readability fails here instead of on
 * a screenshot. Every check runs over every palette the app can paint with, so
 * a palette added later cannot quietly be held to a lower bar.
 */
class ClockwisePaletteTest {

    /** Every shipped theme, in the order the design lists them. */
    private val palettes = AppTheme.entries.map(::paletteFor)

    /**
     * Every palette that reaches a screen: the four themes plus the widget's
     * night pair, which is no longer a theme but still paints the home screen
     * on a dark wallpaper.
     */
    private val allPalettes = palettes + NightClockwisePalette

    // --- The agreed values ---------------------------------------------------

    @Test
    fun theLightMonochromePaletteKeepsTheOriginalSpecValues() {
        assertEquals(Color(0xFFF3F3F1), LightClockwisePalette.background)
        assertEquals(Color(0xFFFAFAF8), LightClockwisePalette.card)
        assertEquals(Color(0xFF111111), LightClockwisePalette.foreground)
        assertEquals(Color(0xFF777774), LightClockwisePalette.mutedForeground)
        assertEquals(false, LightClockwisePalette.isDark)
    }

    @Test
    fun theNightPaletteKeepsTheAgreedDarkTokens() {
        // The widget's night half. True Dark used to be a theme; 3.1.0 retired
        // it, but the widget still needs a dark pair to draw against a dark
        // launcher wallpaper.
        assertEquals(Color(0xFF111111), NightClockwisePalette.background)
        assertEquals(Color(0xFF1A1A1A), NightClockwisePalette.card)
        assertEquals(Color(0xFFE1E1DE), NightClockwisePalette.foreground)
        assertEquals(Color(0xFF777774), NightClockwisePalette.mutedForeground)
        assertEquals(true, NightClockwisePalette.isDark)
    }

    @Test
    fun theWarmSepiaPaletteUsesTheAgreedPaperAndInk() {
        assertEquals(Color(0xFFF7F4EF), WarmSepiaClockwisePalette.background)
        assertEquals(Color(0xFF2C2521), WarmSepiaClockwisePalette.foreground)
        assertEquals(false, WarmSepiaClockwisePalette.isDark)
        // The card is a lighter shade of the paper, as it is in the light
        // monochrome theme.
        assertTrue(
            luminance(WarmSepiaClockwisePalette.card) > luminance(WarmSepiaClockwisePalette.background),
        )
    }

    @Test
    fun theCoolSlatePaletteUsesTheAgreedSurfaceAndText() {
        assertEquals(Color(0xFFEEF2F5), CoolSlateClockwisePalette.background)
        assertEquals(Color(0xFF1E293B), CoolSlateClockwisePalette.foreground)
        assertEquals(false, CoolSlateClockwisePalette.isDark)
        assertTrue(
            luminance(CoolSlateClockwisePalette.card) > luminance(CoolSlateClockwisePalette.background),
        )
    }

    @Test
    fun theRetiredPalettesAreGone() {
        // Retiring a theme means deleting the palette, not hiding it: a stray
        // reference would be a screen that can still paint a theme the picker
        // no longer offers.
        val shipped = palettes.toSet()
        assertEquals(4, shipped.size)
        assertTrue(NightClockwisePalette !in shipped)
    }

    // --- The relationships every palette keeps -------------------------------

    @Test
    fun everyFixedPaletteSelectsWithTheInverseOfItsSurface() {
        // The selected city card is always the mirror of the base surface: a
        // black card on the light palettes, a white card on the dark ones.
        // Material You is the exception: its primary is the Material accent,
        // not an inverse surface, which is the whole point of the theme.
        allPalettes.filterNot { it == MaterialYouFallbackPalette }.forEach { palette ->
            assertEquals(palette.foreground, palette.primary)
        }
    }

    @Test
    fun materialYouSelectsWithTheWallpaperAccentRatherThanTheInverseSurface() {
        assertNotEquals(
            MaterialYouFallbackPalette.foreground,
            MaterialYouFallbackPalette.primary,
        )
        assertTrue(
            "the Material You primary must be readable under its own on-colour",
            contrastRatio(
                MaterialYouFallbackPalette.primaryForeground,
                MaterialYouFallbackPalette.primary,
            ) >= 4.5,
        )
    }

    @Test
    fun everyDarkPaletteIsSeparatedFromItsBackgroundBySomething() {
        // A dark palette may not lift the card the way the light ones do, but
        // then the outline has to carry the separation, so no dark palette may
        // have both a background-coloured card and a border that vanishes into
        // it.
        allPalettes.filter { it.isDark }.forEach { palette ->
            val cardReads = cellIsLighterThanTheBackground(palette)
            val outlinesRead = luminance(palette.border) > luminance(palette.background)
            assertTrue(
                "a dark palette's cards are neither lifted nor outlined",
                cardReads || outlinesRead,
            )
        }

        // The night palette lifts the card rather than outlining it.
        assertTrue(cellIsLighterThanTheBackground(NightClockwisePalette))
        assertNotEquals(NightClockwisePalette.card, NightClockwisePalette.background)
    }

    @Test
    fun theFixedPalettesShareTheBrandAccent() {
        // One warm accent for the fixed palettes: it is the app's only
        // non-monochrome element, and it moves with the palette otherwise.
        val fixedAccents = allPalettes
            .filterNot { it == MaterialYouFallbackPalette }
            .map { it.mapAccent }
            .toSet()
        assertEquals(setOf(Color(0xFFBF6B6B)), fixedAccents)

        // Material You takes its accent from the wallpaper, so it is expected
        // to differ — but it must still be a real colour, not black or white.
        assertNotEquals(Color(0xFFBF6B6B), MaterialYouFallbackPalette.mapAccent)
        assertNotEquals(Color(0xFF000000), MaterialYouFallbackPalette.mapAccent)
        assertNotEquals(Color(0xFFFFFFFF), MaterialYouFallbackPalette.mapAccent)
    }

    @Test
    fun theLandmassesReadAgainstTheOceanTheyFloatIn() {
        allPalettes.forEach { palette ->
            assertNotEquals(
                "the landmasses vanish into the ocean",
                palette.card,
                palette.mapLandmass,
            )
        }
    }

    @Test
    fun nightIsAlwaysPaintedDarkerThanTheMapBeneathIt() {
        // Every night wash is a dark-grey/black shade, never a light one: a
        // light wash on a dark map would read as daylight.
        allPalettes.forEach { palette ->
            assertTrue(
                "night must be painted with a dark shade",
                luminance(palette.mapNight.copy(alpha = 1f)) < 0.1,
            )
        }
        // The dark palettes shade harder than the light ones, because their
        // ocean is already nearly black.
        assertTrue(NightClockwisePalette.mapNight.alpha > LightClockwisePalette.mapNight.alpha)
        assertEquals(Color(0xFF111111), LightClockwisePalette.mapNight.copy(alpha = 1f))
        assertEquals(Color(0xFF000000), NightClockwisePalette.mapNight.copy(alpha = 1f))
    }

    @Test
    fun theTerminatorIsVisibleInEveryPalette() {
        // The absence of these checks is how a theme ships with no visible
        // day/night shading at all: an invisible wash still passes "night is
        // dark". The wash is drawn over the landmasses, so that is where it has
        // to change what the eye sees.
        allPalettes.forEach { palette ->
            val shaded = compositeOver(palette.mapNight, palette.mapLandmass)
            assertTrue(
                "the night wash does not change the landmasses",
                luminance(shaded) < luminance(palette.mapLandmass),
            )
        }
    }

    // --- Legibility ----------------------------------------------------------

    @Test
    fun mainTextPassesWcagAaInEveryPalette() {
        allPalettes.forEach { palette ->
            assertContrastAtLeast(4.5, palette.foreground, palette.background, "foreground on background")
            assertContrastAtLeast(4.5, palette.foreground, palette.card, "foreground on card")
            assertContrastAtLeast(4.5, palette.primaryForeground, palette.primary, "text on selected card")
        }
    }

    @Test
    fun supportingTextStaysReadableInEveryPalette() {
        allPalettes.forEach { palette ->
            // The grey behind supporting labels is a spec value and sits a
            // little under AA for body text; it is only ever used for short
            // supporting labels, so the large-text / non-text threshold is the
            // right bar.
            assertContrastAtLeast(3.0, palette.mutedForeground, palette.background, "muted on background")
            assertContrastAtLeast(3.0, palette.mutedForeground, palette.card, "muted on card")
            assertContrastAtLeast(3.0, palette.mutedForeground, palette.accent, "muted on accent")
        }
    }

    @Test
    fun supportingTextOnTheSelectedCardStaysReadable() {
        allPalettes.forEach { palette ->
            // `onPrimaryMuted` is translucent, so it has to be composited over
            // the selected card before it can be measured.
            val composited = compositeOver(palette.onPrimaryMuted, palette.primary)
            assertContrastAtLeast(3.0, composited, palette.primary, "muted on selected card")
        }
    }

    // --- Wiring --------------------------------------------------------------

    @Test
    fun theColourSchemeIsBuiltOnlyFromThePalette() {
        allPalettes.forEach { palette ->
            val allowed = with(palette) {
                setOf(
                    background, foreground, card, primary, primaryForeground, onPrimaryMuted,
                    secondary, mutedForeground, accent, border, mapAccent, mapLandmass, mapNight,
                )
            } + Color(0xFF000000) // the scrim

            palette.toColorScheme().roles().forEach { (role, color) ->
                assertTrue(
                    "$role fell back to $color, which is not in the ${if (palette.isDark) "dark" else "light"} palette",
                    color in allowed,
                )
            }
        }
    }

    @Test
    fun noFixedPaletteFallsBackToAMaterialAccent() {
        // The default Material 3 light accents. None of them may appear in the
        // fixed palettes. Material You is deliberately built from Material
        // roles, so it is the one theme where those colours are the point.
        val materialAccents = setOf(Color(0xFF6750A4), Color(0xFF625B71), Color(0xFF7D5260))
        allPalettes.filterNot { it == MaterialYouFallbackPalette }.forEach { palette ->
            palette.toColorScheme().roles().forEach { (role, color) ->
                assertTrue("$role is a default Material accent", color !in materialAccents)
            }
        }
    }

    @Test
    fun theDarkMaterialYouFallbackPassesTheSameLegibilityBars() {
        val palette = MaterialYouDarkFallbackPalette

        assertTrue(palette.isDark)
        assertContrastAtLeast(4.5, palette.foreground, palette.background, "foreground on background")
        assertContrastAtLeast(4.5, palette.foreground, palette.card, "foreground on card")
        assertContrastAtLeast(4.5, palette.primaryForeground, palette.primary, "text on selected card")
        assertContrastAtLeast(3.0, palette.mutedForeground, palette.background, "muted on background")
        assertContrastAtLeast(
            3.0,
            compositeOver(palette.onPrimaryMuted, palette.primary),
            palette.primary,
            "muted on selected card",
        )
    }

    @Test
    fun aWallpaperSchemeIsMappedRoleForRoleRatherThanRecoloured() {
        // The dynamic scheme is the source of truth on Android 12+. Mapping it
        // by Material role is what makes the app follow the wallpaper instead
        // of only borrowing one accent from it.
        val scheme = lightColorScheme(
            background = Color(0xFFFDF7FF),
            onBackground = Color(0xFF1C1B1E),
            surfaceContainer = Color(0xFFF1ECF4),
            primary = Color(0xFF006A60),
            onPrimary = Color(0xFFFFFFFF),
            surfaceContainerHigh = Color(0xFFECE6EF),
            onSurfaceVariant = Color(0xFF49454E),
            secondaryContainer = Color(0xFFCCE8E2),
            outlineVariant = Color(0xFFBFC8C5),
            tertiary = Color(0xFF4A6267),
            surfaceContainerHighest = Color(0xFFE6E1E9),
        )

        val palette = paletteFromMaterialScheme(scheme)

        assertEquals(scheme.background, palette.background)
        assertEquals(scheme.onBackground, palette.foreground)
        assertEquals(scheme.surfaceContainer, palette.card)
        assertEquals(scheme.primary, palette.primary)
        assertEquals(scheme.onPrimary, palette.primaryForeground)
        assertEquals(scheme.onPrimary.copy(alpha = 0.72f), palette.onPrimaryMuted)
        assertEquals(scheme.surfaceContainerHigh, palette.secondary)
        assertEquals(scheme.onSurfaceVariant, palette.mutedForeground)
        assertEquals(scheme.secondaryContainer, palette.accent)
        assertEquals(scheme.outlineVariant, palette.border)
        assertEquals(scheme.tertiary, palette.mapAccent)
        assertEquals(scheme.surfaceContainerHighest, palette.mapLandmass)
        assertEquals(Color.Black.copy(alpha = 0.18f), palette.mapNight)
        assertFalse(palette.isDark)
    }

    @Test
    fun aDarkWallpaperSchemeIsDetectedAsDarkAndShadesHarder() {
        val scheme = lightColorScheme(
            background = Color(0xFF141218),
            onBackground = Color(0xFFE6E0E9),
            surfaceContainer = Color(0xFF211F26),
            primary = Color(0xFFD0BCFF),
            onPrimary = Color(0xFF381E72),
            surfaceContainerHigh = Color(0xFF2B2930),
            onSurfaceVariant = Color(0xFFCAC4D0),
            secondaryContainer = Color(0xFF4A4458),
            outlineVariant = Color(0xFF938F99),
            tertiary = Color(0xFFEFB8C8),
            surfaceContainerHighest = Color(0xFF36343B),
        )

        val palette = paletteFromMaterialScheme(scheme)

        assertTrue(palette.isDark)
        assertEquals(Color.Black.copy(alpha = 0.45f), palette.mapNight)
    }

    // --- helpers -------------------------------------------------------------

    private fun cellIsLighterThanTheBackground(palette: ClockwisePalette): Boolean =
        luminance(palette.card) > luminance(palette.background)

    private fun ColorScheme.roles(): Map<String, Color> = mapOf(
        "primary" to primary,
        "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer,
        "inversePrimary" to inversePrimary,
        "secondary" to secondary,
        "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary,
        "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer,
        "onTertiaryContainer" to onTertiaryContainer,
        "background" to background,
        "onBackground" to onBackground,
        "surface" to surface,
        "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant,
        "surfaceTint" to surfaceTint,
        "inverseSurface" to inverseSurface,
        "inverseOnSurface" to inverseOnSurface,
        "error" to error,
        "onError" to onError,
        "errorContainer" to errorContainer,
        "onErrorContainer" to onErrorContainer,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "surfaceBright" to surfaceBright,
        "surfaceDim" to surfaceDim,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainerLowest" to surfaceContainerLowest,
    )

    private fun assertContrastAtLeast(minimum: Double, foreground: Color, background: Color, what: String) {
        val ratio = contrastRatio(foreground, background)
        assertTrue("$what is only ${"%.2f".format(ratio)}:1", ratio >= minimum)
    }

    /** Paints [overlay] over [base], the way the canvas actually draws it. */
    private fun compositeOver(overlay: Color, base: Color): Color {
        val alpha = overlay.alpha
        return Color(
            red = overlay.red * alpha + base.red * (1f - alpha),
            green = overlay.green * alpha + base.green * (1f - alpha),
            blue = overlay.blue * alpha + base.blue * (1f - alpha),
        )
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** WCAG relative luminance. */
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }
}
