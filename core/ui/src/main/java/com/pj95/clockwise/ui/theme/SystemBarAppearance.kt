package com.pj95.clockwise.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Keeps the system bar icons legible against the app's own palette.
 *
 * `enableEdgeToEdge()` decides light/dark icons from the *device* setting, which
 * cannot be right for a palette the user chose: picking True Dark on a device in
 * light mode would leave dark status bar icons on the app's near-black
 * background.
 *
 * Called from [FixedClockwiseTheme], so the icons follow whatever palette is
 * actually in effect.
 */
@Composable
fun SystemBarAppearance() {
    val view = LocalView.current
    if (view.isInEditMode) return

    val darkTheme = LocalClockwisePalette.current.isDark
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
