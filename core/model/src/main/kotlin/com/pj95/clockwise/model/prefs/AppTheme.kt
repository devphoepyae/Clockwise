package com.pj95.clockwise.model.prefs

/**
 * One of the four themes the app ships (spec section 7).
 *
 * Each entry is a complete palette rather than a light/dark switch, so the
 * choice names an exact look and nothing is resolved against the device
 * setting. [id] is the number that is persisted and quoted in the design, and
 * it is part of the stored format: re-ordering this enum would repaint every
 * existing install, so the ids are written out explicitly.
 *
 * True Dark (`1`) and High Contrast (`4`) were retired in 3.1.0 to keep the
 * picker to a short, deliberate list. Their ids are deliberately left unused:
 * a store that still names one reads as [LIGHT_MONOCHROME] through
 * [fromId], and a new theme must never be given a retired number.
 *
 * Kept separate from the generated [SavedAppTheme] so the rest of the app never
 * depends on a generated type.
 */
enum class AppTheme(val id: Int) {
    /** Dark text on a soft off-white surface. The default. */
    LIGHT_MONOCHROME(0),

    /** Warm brown text on soft paper. */
    WARM_SEPIA(2),

    /** Slate text on a muted blue-grey surface. */
    COOL_SLATE(3),

    /**
     * Colours derived from the device wallpaper on Android 12+.
     *
     * The other three are exact palettes and ignore the device setting; Material
     * You is the opt-in exception, and the only theme that can be dark. On older
     * devices, or when the platform supplies no dynamic scheme, it falls back to
     * the system light/dark palette.
     */
    MATERIAL_YOU(5),
    ;

    companion object {
        /** Used when the user has never made a choice. */
        val Default: AppTheme = LIGHT_MONOCHROME

        /**
         * The theme carrying [id], or [Default] when nothing matches.
         *
         * Retired ids (`1`, `4`) fall through to the default, which is what a
         * store written by an older build should read as now that the two dark
         * palettes are gone.
         */
        fun fromId(id: Int): AppTheme = entries.firstOrNull { it.id == id } ?: Default
    }
}
