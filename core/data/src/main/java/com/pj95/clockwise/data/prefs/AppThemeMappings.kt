package com.pj95.clockwise.data.prefs

import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.model.prefs.ThemeMode

/**
 * Maps the stored value onto the domain enum.
 *
 * `UNRECOGNIZED`, the zero value and the two retired values all land on the
 * default. The retired ones matter: an install that chose True Dark or High
 * Contrast has those names in its store today, and the app has no palette to
 * give them any more.
 *
 * The mapping lives here rather than next to [AppTheme] so the domain module
 * stays free of generated protobuf types.
 */
fun SavedAppTheme.toAppTheme(): AppTheme = when (this) {
    SavedAppTheme.SAVED_APP_THEME_WARM_SEPIA -> AppTheme.WARM_SEPIA
    SavedAppTheme.SAVED_APP_THEME_COOL_SLATE -> AppTheme.COOL_SLATE
    SavedAppTheme.SAVED_APP_THEME_MATERIAL_YOU -> AppTheme.MATERIAL_YOU
    else -> AppTheme.LIGHT_MONOCHROME
}

/** Maps the domain enum onto the value that gets written to the store. */
fun AppTheme.toSavedAppTheme(): SavedAppTheme = when (this) {
    AppTheme.LIGHT_MONOCHROME -> SavedAppTheme.SAVED_APP_THEME_LIGHT_MONOCHROME
    AppTheme.WARM_SEPIA -> SavedAppTheme.SAVED_APP_THEME_WARM_SEPIA
    AppTheme.COOL_SLATE -> SavedAppTheme.SAVED_APP_THEME_COOL_SLATE
    AppTheme.MATERIAL_YOU -> SavedAppTheme.SAVED_APP_THEME_MATERIAL_YOU
}

/**
 * Migrates the light/dark choice of an older build onto one of the four themes.
 *
 * `LIGHT` was the light monochrome palette, so those users keep what they were
 * looking at. `DARK` was the true-dark palette, which 3.1.0 retired, and
 * `SYSTEM` followed the device; neither can be honoured exactly, so both land on
 * the default — the same theme a store with no choice at all reads as.
 */
fun SavedThemeMode.toMigratedAppTheme(): AppTheme = when (this) {
    SavedThemeMode.THEME_MODE_LIGHT -> AppTheme.LIGHT_MONOCHROME
    else -> AppTheme.Default
}

/** Maps Clockwise's stored light/dark choice onto the domain enum. */
fun SavedThemeModeSetting.toThemeMode(): ThemeMode = when (this) {
    SavedThemeModeSetting.SAVED_THEME_MODE_SETTING_LIGHT -> ThemeMode.LIGHT
    SavedThemeModeSetting.SAVED_THEME_MODE_SETTING_DARK -> ThemeMode.DARK
    else -> ThemeMode.SYSTEM
}

/** Maps the domain enum onto the value written to the store. */
fun ThemeMode.toSavedThemeModeSetting(): SavedThemeModeSetting = when (this) {
    ThemeMode.SYSTEM -> SavedThemeModeSetting.SAVED_THEME_MODE_SETTING_SYSTEM
    ThemeMode.LIGHT -> SavedThemeModeSetting.SAVED_THEME_MODE_SETTING_LIGHT
    ThemeMode.DARK -> SavedThemeModeSetting.SAVED_THEME_MODE_SETTING_DARK
}

/**
 * Reads the light/dark choice, migrating a store written before Clockwise.
 *
 * The old themes were exact palettes rather than a mode: the three fixed ones
 * were all light, and Material You followed the device. They therefore migrate
 * to [ThemeMode.LIGHT] and [ThemeMode.SYSTEM] respectively. The even older
 * `theme_mode` field already meant exactly what this field means, so its dark
 * and light values carry across unchanged.
 */
fun SavedCities.storedThemeMode(): ThemeMode = when {
    hasThemeModeSetting() -> themeModeSetting.toThemeMode()
    hasTheme() && theme == SavedAppTheme.SAVED_APP_THEME_MATERIAL_YOU -> ThemeMode.SYSTEM
    hasTheme() -> ThemeMode.LIGHT
    themeMode == SavedThemeMode.THEME_MODE_DARK -> ThemeMode.DARK
    themeMode == SavedThemeMode.THEME_MODE_LIGHT -> ThemeMode.LIGHT
    else -> ThemeMode.SYSTEM
}
