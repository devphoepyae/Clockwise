package com.pj95.clockwise.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.model.prefs.ThemeMode

/**
 * The palette a theme paints with.
 *
 * Pure, so the four-way mapping is unit-testable without a device or a
 * Robolectric configuration, and so there is exactly one place that decides
 * what a theme looks like.
 */
fun paletteFor(theme: AppTheme): ClockwisePalette = when (theme) {
    AppTheme.LIGHT_MONOCHROME -> LightClockwisePalette
    AppTheme.WARM_SEPIA -> WarmSepiaClockwisePalette
    AppTheme.COOL_SLATE -> CoolSlateClockwisePalette
    // The real palette is generated from the wallpaper in
    // [rememberClockwisePalette]; this is the non-device fallback.
    AppTheme.MATERIAL_YOU -> MaterialYouFallbackPalette
}

/**
 * Resolves [theme] for the current composition.
 *
 * The three fixed themes are exact and device-independent. Material You is the
 * one that reads the platform: on Android 12+ its palette is generated from the
 * wallpaper, and on older devices it follows the system light/dark setting
 * instead of pretending a dynamic scheme exists.
 *
 * Kept separate from [paletteFor] so the fixed mapping stays pure and
 * unit-testable while the one device-dependent choice is explicit.
 */
@Composable
fun rememberClockwisePalette(theme: AppTheme): ClockwisePalette {
    if (theme != AppTheme.MATERIAL_YOU) return paletteFor(theme)
    return rememberMaterialYouPalette(isSystemInDarkTheme())
}

/**
 * Material You's palette in the light or dark half of the wallpaper scheme.
 *
 * Shared by the legacy Material You theme and Clockwise's [ThemeMode] shell so
 * the two can never drift apart. Below Android 12 the static Material 3
 * fallback stands in, because no dynamic scheme exists there.
 */
@Composable
fun rememberMaterialYouPalette(dark: Boolean): ClockwisePalette {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return if (dark) MaterialYouDarkFallbackPalette else MaterialYouFallbackPalette
    }
    val context = LocalContext.current
    val scheme = if (dark) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    return paletteFromMaterialScheme(scheme)
}

/**
 * Maps a Material 3 dynamic scheme onto the app's semantic palette.
 *
 * Every role the app uses is taken from a Material role rather than from a
 * fixed hex, so the result follows the wallpaper's hue and chroma. The night
 * wash stays a dark translucent black: it has to darken the map in both light
 * and dark wallpapers, which a wallpaper colour cannot promise.
 */
fun paletteFromMaterialScheme(scheme: ColorScheme): ClockwisePalette {
    val dark = scheme.background.luminance() < 0.5f
    return ClockwisePalette(
        background = scheme.background,
        foreground = scheme.onBackground,
        card = scheme.surfaceContainer,
        primary = scheme.primary,
        primaryForeground = scheme.onPrimary,
        onPrimaryMuted = scheme.onPrimary.copy(alpha = 0.72f),
        secondary = scheme.surfaceContainerHigh,
        mutedForeground = scheme.onSurfaceVariant,
        accent = scheme.secondaryContainer,
        border = scheme.outlineVariant,
        mapAccent = scheme.tertiary,
        mapLandmass = scheme.surfaceContainerHighest,
        mapNight = Color.Black.copy(alpha = if (dark) 0.45f else 0.18f),
        isDark = dark,
    )
}

/**
 * Material 3 colour scheme with every role pinned to [palette].
 *
 * Every slot is listed explicitly — including the `surfaceContainer*` roles —
 * so no component can fall back to a default Material colour. The defaults of
 * `lightColorScheme()` / `darkColorScheme()` are only a starting point here;
 * nothing is inherited from them.
 */
fun ClockwisePalette.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = primaryForeground,
        primaryContainer = accent,
        onPrimaryContainer = foreground,
        inversePrimary = secondary,

        secondary = secondary,
        onSecondary = foreground,
        secondaryContainer = accent,
        onSecondaryContainer = foreground,

        tertiary = mapAccent,
        onTertiary = primaryForeground,
        tertiaryContainer = accent,
        onTertiaryContainer = foreground,

        background = background,
        onBackground = foreground,

        surface = card,
        onSurface = foreground,
        surfaceVariant = secondary,
        onSurfaceVariant = mutedForeground,

        // Keeps elevation overlays neutral instead of tinting towards purple.
        surfaceTint = foreground,
        inverseSurface = foreground,
        inverseOnSurface = card,

        error = mapAccent,
        onError = primaryForeground,
        errorContainer = accent,
        onErrorContainer = foreground,

        outline = border,
        outlineVariant = border,
        scrim = Color(0xFF000000),

        surfaceBright = card,
        surfaceDim = background,
        surfaceContainer = card,
        surfaceContainerHigh = card,
        surfaceContainerHighest = secondary,
        surfaceContainerLow = card,
        surfaceContainerLowest = primaryForeground,
    )
}

/**
 * Root theme for the app.
 *
 * Provides the palette of [theme] through [LocalClockwisePalette] and wraps
 * content in [MaterialTheme] so Material components inherit the app's colours,
 * Inter typography and 16 dp corner radius while still providing correct
 * accessibility semantics.
 *
 * The device's light/dark setting is deliberately ignored by the three fixed
 * palettes: the user has chosen an exact look, and repainting it because the
 * phone switched at sunset would throw that choice away. Material You is the
 * opt-in exception and follows the device.
 *
 * @param theme the user's choice; defaults to Light Monochrome
 */
@Composable
fun FixedClockwiseTheme(
    theme: AppTheme = AppTheme.Default,
    content: @Composable () -> Unit,
) {
    val palette = rememberClockwisePalette(theme)

    CompositionLocalProvider(LocalClockwisePalette provides palette) {
        // Applied inside the provider so the icons are resolved from the palette
        // that is actually in effect, not from the device setting.
        SystemBarAppearance()
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = ClockwiseTypography,
            shapes = ClockwiseShapes,
            content = content,
        )
    }
}

/**
 * Clockwise's root theme: Material You in the user's chosen light/dark mode.
 *
 * The wallpaper supplies the hue; [themeMode] decides which half of that
 * scheme is painted. [ThemeMode.SYSTEM] follows the device, while an explicit
 * Light or Dark choice ignores sunset changes so the app does not repaint
 * underneath the user.
 */
@Composable
fun ClockwiseTheme(
    themeMode: ThemeMode = ThemeMode.Default,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = rememberMaterialYouPalette(dark)

    CompositionLocalProvider(LocalClockwisePalette provides palette) {
        SystemBarAppearance()
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = ClockwiseTypography,
            shapes = ClockwiseShapes,
            content = content,
        )
    }
}

/**
 * The palette a ringing alarm paints with.
 *
 * Since v1.0.1 this is the fixed [AlarmRingingPalette] — dark navy with
 * champagne gold — rather than the wallpaper's dynamic scheme. The alarm is the
 * app's most important moment, and it should look the same on every phone: the
 * same calm surface, the same single gold control. It also cannot flash a light
 * theme at 03:00, because it is not derived from the user's theme at all.
 */
@Composable
fun rememberRingingPalette(): ClockwisePalette = AlarmRingingPalette

/**
 * Root theme for [com.pj95.clockwise.alarm.AlarmRingingActivity].
 *
 * Separate from [FixedClockwiseTheme] because the two answers differ: this one is
 * not a user preference and not light. [palette] is a parameter rather than a
 * hard call to [rememberRingingPalette] so a screenshot test can pin the exact
 * colours without a wallpaper.
 */
@Composable
fun AlarmRingingTheme(
    palette: ClockwisePalette = rememberRingingPalette(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalClockwisePalette provides palette) {
        SystemBarAppearance()
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = ClockwiseTypography,
            shapes = ClockwiseShapes,
            content = content,
        )
    }
}
