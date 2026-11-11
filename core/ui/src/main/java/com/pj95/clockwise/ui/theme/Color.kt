package com.pj95.clockwise.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * One complete set of app colours (spec section 8).
 *
 * The app ships three fixed palettes — Light Monochrome, Warm Sepia and Cool
 * Slate — plus Material You, which is resolved from the device at composition
 * time. Every screen paints with whichever one is currently provided through
 * [LocalClockwisePalette]. Nothing falls back to a Material default, so the
 * Material 3 purple/blue accent can never appear.
 */
@Immutable
data class ClockwisePalette(
    /** App main surface. */
    val background: Color,
    /** Main text and primary actions. */
    val foreground: Color,
    /** City cards and elevated surfaces. */
    val card: Color,
    /** Selected card and active controls. */
    val primary: Color,
    /** Text and icons on [primary]. */
    val primaryForeground: Color,
    /**
     * Supporting text drawn on top of the selected card. Derived from
     * [primaryForeground] rather than being an independent colour.
     */
    val onPrimaryMuted: Color,
    /** Secondary controls and map grid. */
    val secondary: Color,
    /** Supporting text, offsets and labels. */
    val mutedForeground: Color,
    /** Soft selected surfaces. */
    val accent: Color,
    /** Outlines, separators and the map graticule. */
    val border: Color,
    /** Map meridian, active pin, and the remove (destructive) action. */
    val mapAccent: Color,
    /**
     * The landmasses on the schematic map.
     *
     * Separate from [accent] — which is also the fill of the app's soft pills —
     * because the two pull in opposite directions in a dark palette: the pills
     * need a near-black fill so white text stays readable on them, while the
     * landmasses need to read *against* an ocean that is itself [card].
     */
    val mapLandmass: Color,
    /**
     * The wash drawn over the half of the globe that is in shadow.
     *
     * The alpha lives in the value because darkness has to be painted as
     * darkness in every theme — a light black wash over a light map, a heavier
     * black wash over a dark one. Deriving it from [foreground] would wash a
     * dark map *lighter*, which reads as daylight.
     *
     * A black wash cannot show over a pure-black ocean, which is why High
     * Contrast paints its landmasses light: the shadow stays visible on the
     * continents (see `ClockwisePaletteTest`).
     */
    val mapNight: Color,
    /** True for the dark palettes. */
    val isDark: Boolean,
)

/**
 * Theme 1 — light monochrome. These are the original spec values and are
 * unchanged, so light-mode rendering is byte-for-byte what it was.
 */
val LightClockwisePalette = ClockwisePalette(
    background = Color(0xFFF3F3F1),
    foreground = Color(0xFF111111),
    card = Color(0xFFFAFAF8),
    primary = Color(0xFF111111),
    primaryForeground = Color(0xFFFFFFFF),
    onPrimaryMuted = Color(0x9EFFFFFF),
    secondary = Color(0xFFE8E8E6),
    mutedForeground = Color(0xFF777774),
    accent = Color(0xFFE3E3E0),
    border = Color(0xFFD9D9D6),
    mapAccent = Color(0xFFBF6B6B),
    mapLandmass = Color(0xFFE3E3E0),
    mapNight = Color(0xFF111111).copy(alpha = 0.18f),
    isDark = false,
)

/**
 * The night half of the widget palette.
 *
 * This was the True Dark theme until 3.1.0 retired it. The values are kept
 * because the home-screen widget draws its own day/night pair through
 * `DayNightColorProvider` and has to stay legible on a dark launcher wallpaper;
 * it is no longer reachable from the theme picker.
 *
 *  - [ClockwisePalette.primary] stays the *inverse* of the base surface, so a
 *    selected row is a light surface with dark text.
 *  - [ClockwisePalette.secondary], [ClockwisePalette.accent] and
 *    [ClockwisePalette.border] step up from [ClockwisePalette.card] by the
 *    same kind of increments the light theme uses.
 *  - [ClockwisePalette.mapAccent] is deliberately unchanged: the single warm
 *    accent has enough contrast against every background.
 */
val NightClockwisePalette = ClockwisePalette(
    background = Color(0xFF111111),
    foreground = Color(0xFFE1E1DE),
    card = Color(0xFF1A1A1A),
    primary = Color(0xFFE1E1DE),
    primaryForeground = Color(0xFF111111),
    onPrimaryMuted = Color(0x9E111111),
    secondary = Color(0xFF232323),
    mutedForeground = Color(0xFF777774),
    accent = Color(0xFF262626),
    border = Color(0xFF2E2E2E),
    mapAccent = Color(0xFFBF6B6B),
    mapLandmass = Color(0xFF262626),
    mapNight = Color(0xFF000000).copy(alpha = 0.45f),
    isDark = true,
)

/**
 * The ringing screen's fixed palette: deep navy and champagne gold.
 *
 * v1.0.1 made the alarm screen a brand moment rather than a wallpaper mirror.
 * A ringing alarm is the one screen the user did not navigate to and may meet at
 * 03:00, so it gets its own quiet, luxurious colours: an almost-black navy
 * surface, warm ivory type and a single gold accent for the control that stops
 * the ring. It deliberately ignores both the app's chosen theme and the
 * wallpaper, so the screen is identical on every device.
 */
val AlarmRingingPalette = ClockwisePalette(
    background = Color(0xFF081426),
    foreground = Color(0xFFF4EBDD),
    card = Color(0xFF101F35),
    primary = Color(0xFFC9A86A),
    primaryForeground = Color(0xFF081426),
    onPrimaryMuted = Color(0xB3081426),
    secondary = Color(0xFF18283F),
    mutedForeground = Color(0xFFB8AF9B),
    accent = Color(0xFF1C2C46),
    border = Color(0xFF2A3B55),
    mapAccent = Color(0xFFC9A86A),
    mapLandmass = Color(0xFF1C2C46),
    mapNight = Color(0xFF000000).copy(alpha = 0.45f),
    isDark = true,
)

/**
 * Theme 2 — warm sepia.
 *
 * Built on the light theme's structure with the surface turned to paper and the
 * text to warm ink: the card is a shade lighter than the background, and
 * [ClockwisePalette.secondary], [ClockwisePalette.accent] and
 * [ClockwisePalette.border] step down from the background in the same
 * increments. The warm accent is shared with every other theme so the brand
 * colour does not shift; the night wash is tinted with the ink rather than pure
 * black, so the shadow over the map stays warm.
 */
val WarmSepiaClockwisePalette = ClockwisePalette(
    background = Color(0xFFF7F4EF),
    foreground = Color(0xFF2C2521),
    card = Color(0xFFFCFAF6),
    primary = Color(0xFF2C2521),
    primaryForeground = Color(0xFFFFFFFF),
    onPrimaryMuted = Color(0x9EFFFFFF),
    secondary = Color(0xFFEFEAE2),
    mutedForeground = Color(0xFF7A7065),
    accent = Color(0xFFEDE7DD),
    border = Color(0xFFE2DACE),
    mapAccent = Color(0xFFBF6B6B),
    mapLandmass = Color(0xFFEDE7DD),
    mapNight = Color(0xFF2C2521).copy(alpha = 0.18f),
    isDark = false,
)

/**
 * Theme 3 — cool slate.
 *
 * The same structure again, shifted to a blue-grey: a near-white card on a
 * muted surface, with the supporting greys taken from the slate family so the
 * whole screen stays cool. Supporting text is a little darker than the light
 * theme's muted grey, because a blue-grey background reflects more light.
 */
val CoolSlateClockwisePalette = ClockwisePalette(
    background = Color(0xFFEEF2F5),
    foreground = Color(0xFF1E293B),
    card = Color(0xFFF8FAFC),
    primary = Color(0xFF1E293B),
    primaryForeground = Color(0xFFFFFFFF),
    onPrimaryMuted = Color(0x9EFFFFFF),
    secondary = Color(0xFFE4EAEF),
    mutedForeground = Color(0xFF64748B),
    accent = Color(0xFFDDE5EB),
    border = Color(0xFFCFD9E1),
    mapAccent = Color(0xFFBF6B6B),
    mapLandmass = Color(0xFFDDE5EB),
    mapNight = Color(0xFF1E293B).copy(alpha = 0.18f),
    isDark = false,
)

/**
 * The Material You palette used when the platform cannot supply a dynamic one.
 *
 * On Android 12+ [FixedClockwiseTheme] replaces this with colours generated from
 * the wallpaper. This static set is the same Material 3 baseline palette, so a
 * preview, an older device, or Robolectric still renders a coherent Material
 * You screen instead of falling back to a monochrome theme.
 */
val MaterialYouFallbackPalette = ClockwisePalette(
    background = Color(0xFFFEF7FF),
    foreground = Color(0xFF1D1B20),
    card = Color(0xFFF7F2FA),
    primary = Color(0xFF6750A4),
    primaryForeground = Color(0xFFFFFFFF),
    onPrimaryMuted = Color(0xB3FFFFFF),
    secondary = Color(0xFFECE6F0),
    mutedForeground = Color(0xFF49454F),
    accent = Color(0xFFE8DEF8),
    border = Color(0xFF79747E),
    mapAccent = Color(0xFF7D5260),
    mapLandmass = Color(0xFFE7E0EC),
    mapNight = Color(0xFF000000).copy(alpha = 0.18f),
    isDark = false,
)

/**
 * The dark half of [MaterialYouFallbackPalette].
 *
 * Used on devices below Android 12 when the system is in dark mode. Without
 * it, choosing Material You on an older phone in dark mode would paint a light
 * screen — worse than not offering the theme at all.
 */
val MaterialYouDarkFallbackPalette = ClockwisePalette(
    background = Color(0xFF141218),
    foreground = Color(0xFFE6E0E9),
    card = Color(0xFF211F26),
    primary = Color(0xFFD0BCFF),
    primaryForeground = Color(0xFF381E72),
    onPrimaryMuted = Color(0xB3381E72),
    secondary = Color(0xFF2B2930),
    mutedForeground = Color(0xFFCAC4D0),
    accent = Color(0xFF4A4458),
    border = Color(0xFF938F99),
    mapAccent = Color(0xFFEFB8C8),
    mapLandmass = Color(0xFF36343B),
    mapNight = Color(0xFF000000).copy(alpha = 0.45f),
    isDark = true,
)

/**
 * The palette in effect for the current theme.
 *
 * Provided by [FixedClockwiseTheme]; use [ClockwiseColors] to read it.
 */
val LocalClockwisePalette = staticCompositionLocalOf { LightClockwisePalette }

/**
 * The active palette, read through Material-style composable getters.
 *
 * This mirrors `MaterialTheme.colorScheme`: the properties read the composition
 * local rather than holding constants, so a screen written once paints
 * correctly in every theme. Because they read a composition local they
 * are only accessible from a composable scope — non-composable drawing code
 * receives a [ClockwisePalette] as a parameter instead (see `WorldMapCanvas`).
 */
object ClockwiseColors {
    /** App main surface. */
    val Background: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.background

    /** Main text and primary actions. */
    val Foreground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.foreground

    /** City cards and elevated surfaces. */
    val Card: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.card

    /** Selected card and active controls. */
    val Primary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.primary

    /** Text and icons on [Primary]. */
    val PrimaryForeground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.primaryForeground

    /** Supporting text on the selected card. */
    val OnPrimaryMuted: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.onPrimaryMuted

    /** Secondary controls and map grid. */
    val Secondary: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.secondary

    /** Supporting text, offsets and labels. */
    val MutedForeground: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.mutedForeground

    /** Soft selected surfaces. */
    val Accent: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.accent

    /** Outlines, separators and the map graticule. */
    val Border: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.border

    /** Map meridian, active pin and the destructive action. */
    val MapAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.mapAccent

    /** The continents on the schematic map. */
    val MapLandmass: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.mapLandmass

    /** The wash drawn over the half of the globe in shadow. */
    val MapNight: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalClockwisePalette.current.mapNight
}
