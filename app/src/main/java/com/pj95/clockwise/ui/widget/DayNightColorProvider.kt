package com.pj95.clockwise.ui.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * A [ColorProvider] with a light and a dark value.
 *
 * Glance offers day/night only for *resources* (`ColorProvider(@ColorRes)`), not
 * for two literal colours, and the widget has to paint with the app's own
 * palette tokens — so the resolution happens here, against the configuration the
 * widget is being drawn for. It is re-read on every redraw, and the launcher
 * re-inflates a widget when the system theme changes, so a flip to dark mode
 * repaints the widget without the app being opened.
 */
internal class DayNightColorProvider(
    private val day: Color,
    private val night: Color,
) : ColorProvider {

    override fun getColor(context: Context): Color =
        if (isNightMode(context)) night else day
}

/** True when the device is currently in dark mode. */
internal fun isNightMode(context: Context): Boolean =
    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES
