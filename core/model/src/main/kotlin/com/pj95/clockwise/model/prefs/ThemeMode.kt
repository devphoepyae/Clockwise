package com.pj95.clockwise.model.prefs

/**
 * The app's light/dark choice (Clockwise 1.0).
 *
 * Clockwise paints with Material You's dynamic colour framework. The wallpaper
 * supplies the hue; this enum decides only whether the light or the dark half
 * of that scheme is used, or whether the device's own setting should decide.
 *
 * Stored by ordinal through a dedicated proto field, so adding a mode must
 * append rather than re-order. [SYSTEM] is the default because a fresh install
 * should look like the rest of the phone.
 */
enum class ThemeMode {
    /** Follow the device's light/dark setting. */
    SYSTEM,

    /** Always paint the light dynamic scheme. */
    LIGHT,

    /** Always paint the dark dynamic scheme. */
    DARK,
    ;

    companion object {
        /** Used when the user has never made a choice. */
        val Default: ThemeMode = SYSTEM

        /** The mode stored as [ordinal], or [Default] for an unknown value. */
        fun fromOrdinal(ordinal: Int): ThemeMode =
            entries.firstOrNull { it.ordinal == ordinal } ?: Default
    }
}
