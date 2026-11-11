package com.pj95.clockwise

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pj95.clockwise.ui.ClockwiseRoot
import com.pj95.clockwise.ui.ClockwiseViewModel
import com.pj95.clockwise.ui.screens.OnboardingScreen
import com.pj95.clockwise.ui.theme.ClockwiseTheme

/**
 * Single-activity host. Renders the Compose shell using the app's own
 * monochrome theme.
 *
 * The theme is applied here rather than inside [ClockwiseRoot] because it has
 * to wrap the shell: the persisted theme is read from the same view model the
 * shell uses, so the palette is already correct on the first composition of any
 * screen — including the very first frame of a cold start.
 *
 * The activity is `singleTop` because a widget row is a deep link. Tapping a
 * city on the home screen while the app is already open delivers
 * [EXTRA_CITY_ID] through [onNewIntent], and the pending id is handed to
 * [ClockwiseRoot] once, which opens that city's detail and clears it.
 */
class MainActivity : ComponentActivity() {

    private var pendingCityId by mutableStateOf<String?>(null)
    private var pendingShortcut by mutableStateOf<String?>(null)
    private var pendingAlarmId by mutableStateOf<Long?>(null)
    private var pendingTimer by mutableStateOf(false)
    private var pendingStudio by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Only a fresh launch carries a widget tap or a launcher shortcut. A
        // configuration change restores the back stack instead, so replaying
        // the Intent would open the city again after the user had already
        // navigated away.
        if (savedInstanceState == null) {
            pendingCityId = intent?.consumeWidgetCityId()
            pendingShortcut = intent?.consumeShortcut()
            pendingAlarmId = intent?.consumeAlarmId()
            pendingTimer = intent?.consumeTimer() == true
            pendingStudio = intent?.consumeStudio() == true
            intent?.reportShortcutUsage()
        }

        setContent {
            val viewModel: ClockwiseViewModel = viewModel(factory = ClockwiseViewModel.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            ClockwiseTheme(themeMode = state.themeMode) {
                // The intro gates the whole shell, but it lives here rather than
                // inside ClockwiseRoot so the shell stays directly renderable in
                // tests and previews. "Show intro" in Settings flips the same
                // preference, and this branch reacts to it.
                if (!state.isLoading && !state.onboardingCompleted) {
                    OnboardingScreen(onDone = viewModel::completeOnboarding)
                } else {
                    ClockwiseRoot(
                        viewModel = viewModel,
                        initialCityId = pendingCityId,
                        onInitialCityOpened = { pendingCityId = null },
                        initialShortcut = pendingShortcut,
                        onInitialShortcutOpened = { pendingShortcut = null },
                        initialAlarmId = pendingAlarmId,
                        onInitialAlarmOpened = { pendingAlarmId = null },
                        initialTimer = pendingTimer,
                        onInitialTimerOpened = { pendingTimer = false },
                        initialStudio = pendingStudio,
                        onInitialStudioOpened = { pendingStudio = false },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.consumeWidgetCityId()?.let { pendingCityId = it }
        intent.consumeShortcut()?.let { pendingShortcut = it }
        intent.consumeAlarmId()?.let { pendingAlarmId = it }
        if (intent.consumeTimer() == true) pendingTimer = true
        if (intent.consumeStudio() == true) pendingStudio = true
        intent.reportShortcutUsage()
    }

    /**
     * Reads the widget's city id and removes it from the Intent.
     *
     * Removing it is what makes the extra a one-shot deep link: the same Intent
     * object is reused across configuration changes, and without this a rotation
     * would look like a second tap.
     */
    private fun Intent.consumeWidgetCityId(): String? {
        val cityId = getStringExtra(EXTRA_CITY_ID)?.takeIf { it.isNotBlank() }
        removeExtra(EXTRA_CITY_ID)
        return cityId
    }

    /** Reads and consumes a launcher shortcut's destination. */
    private fun Intent.consumeShortcut(): String? {
        val shortcut = getStringExtra(EXTRA_SHORTCUT)
            ?.takeIf { it in SUPPORTED_SHORTCUTS }
        removeExtra(EXTRA_SHORTCUT)
        return shortcut
    }

    /**
     * Reads the alarm whose status-bar icon was tapped and removes it.
     *
     * Consuming it matters for the same reason as the city extra: the same
     * Intent is reused across configuration changes, and a rotation should not
     * send the user back to the alarm tab after they have moved on.
     */
    private fun Intent.consumeAlarmId(): Long? {
        val alarmId = getLongExtra(EXTRA_ALARM_ID, NO_ALARM).takeIf { it != NO_ALARM }
        removeExtra(EXTRA_ALARM_ID)
        return alarmId
    }

    /**
     * Reads and consumes the timer notification tap.
     *
     * A boolean rather than an id: there is one timer, and the tab is what the
     * notification needs to open.
     */
    private fun Intent.consumeTimer(): Boolean {
        val requested = getBooleanExtra(EXTRA_TIMER, false)
        removeExtra(EXTRA_TIMER)
        return requested
    }

    /** Reads and consumes a locked widget's request for the Studio gallery. */
    private fun Intent.consumeStudio(): Boolean {
        val requested = getBooleanExtra(EXTRA_STUDIO, false)
        removeExtra(EXTRA_STUDIO)
        return requested
    }

    /**
     * Tells the launcher that a city shortcut was used.
     *
     * The id is consumed once, like the other deep-link extras. Only city
     * shortcuts set it, so a widget row — which uses the same activity and the
     * same city extra — never reports a shortcut it did not come from. Usage is
     * advisory and the platform ignores it on a launcher that does not care, so
     * a failure here must not stop the tap from opening the city.
     */
    private fun Intent.reportShortcutUsage() {
        val shortcutId = getStringExtra(EXTRA_SHORTCUT_ID)?.takeIf { it.isNotBlank() } ?: return
        removeExtra(EXTRA_SHORTCUT_ID)
        runCatching { ShortcutManagerCompat.reportShortcutUsed(this@MainActivity, shortcutId) }
    }

    companion object {
        /**
         * The city a widget row wants opened.
         *
         * Explicitly namespaced rather than a bare `cityId`, because the activity
         * is exported and another app's Intent must not be mistaken for ours.
         */
        const val EXTRA_CITY_ID: String = "com.pj95.clockwise.extra.CITY_ID"

        /** The quick action a launcher shortcut wants opened. */
        const val EXTRA_SHORTCUT: String = "com.pj95.clockwise.extra.SHORTCUT"

        /**
         * The alarm whose status-bar icon opened the app.
         *
         * The same value is baked into every `AlarmClockInfo` show intent, so
         * tapping the alarm icon lands on the alarm list.
         */
        const val EXTRA_ALARM_ID: String = "com.pj95.clockwise.extra.ALARM_ID"

        /**
         * The dynamic shortcut id behind a city tap, used for usage reporting.
         *
         * Distinct from [EXTRA_SHORTCUT] because that one names a fixed quick
         * action, and distinct from [EXTRA_CITY_ID] because a widget row sends
         * that without coming from a shortcut.
         */
        const val EXTRA_SHORTCUT_ID: String = "com.pj95.clockwise.extra.SHORTCUT_ID"

        /** Set by the timer notification: open the Alarms tab. */
        const val EXTRA_TIMER: String = "com.pj95.clockwise.extra.TIMER"

        /**
         * Set by a locked premium widget: open the Studio gallery directly.
         *
         * A boolean rather than a style id: the gallery is one destination, and
         * the style the user tapped is already visible in the list.
         */
        const val EXTRA_STUDIO: String = "com.pj95.clockwise.extra.STUDIO"

        const val SHORTCUT_ADD_CITY: String = "add-city"
        const val SHORTCUT_PLANNER: String = "planner"

        /** `getLongExtra` default: no alarm was named. */
        private const val NO_ALARM = -1L

        /** Only these values are accepted from the exported activity. */
        private val SUPPORTED_SHORTCUTS = setOf(
            SHORTCUT_ADD_CITY,
            SHORTCUT_PLANNER,
        )
    }
}
