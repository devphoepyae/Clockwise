package com.pj95.clockwise.screensaver

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pj95.clockwise.ClockwiseApp
import com.pj95.clockwise.data.prefs.UserPreferences
import com.pj95.clockwise.ui.screens.ScreenSaverScreen
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import java.time.Instant

/**
 * Full-screen desk clock.
 *
 * A separate Activity rather than a dialog so it covers the system bars and owns
 * the window flags that keep the display awake. It is launched only from
 * Settings, never exported, and a tap or Back finishes it.
 *
 * `FLAG_KEEP_SCREEN_ON` is used instead of a wake lock: it is scoped to the
 * visible window and is released by the platform the moment the activity
 * stops, so a forgotten screen saver cannot hold the CPU awake.
 */
class ScreenSaverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        goImmersive()

        val container = (application as ClockwiseApp).container

        setContent {
            val preferences by container.clockRepository.preferences
                .collectAsStateWithLifecycle(initialValue = UserPreferences.Default)
            val now by container.secondTicker.ticker()
                .collectAsStateWithLifecycle(initialValue = Instant.now())

            FixedClockwiseTheme(theme = preferences.theme) {
                ScreenSaverScreen(
                    now = now,
                    use24Hour = preferences.use24Hour,
                    onExit = ::finish,
                )
            }
        }
    }

    /** Hides every system bar and keeps the display on while visible. */
    private fun goImmersive() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // The compat controller rather than `window.insetsController`: the same
        // behaviour on every API level, and its constants are the ones this
        // code can name without a version check.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
