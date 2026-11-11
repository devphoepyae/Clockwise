package com.pj95.clockwise

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import com.pj95.clockwise.alarm.AlarmNotifications
import com.pj95.clockwise.alarm.TimerNotifications
import com.pj95.clockwise.data.repository.SolarCachePolicy
import com.pj95.clockwise.di.AppContainer
import com.pj95.clockwise.di.AppContainerProvider
import com.pj95.clockwise.di.DefaultAppContainer
import com.pj95.clockwise.shortcuts.CityShortcuts
import com.pj95.clockwise.shortcuts.ShortcutMirror
import com.pj95.clockwise.widget.WidgetMirror
import com.pj95.clockwise.widget.WidgetPreviewPublisher
import com.pj95.clockwise.widget.ClockwiseWidget
import com.pj95.clockwise.widget.cancelWidgetTick
import com.pj95.clockwise.widget.hasHomeScreenWidgets
import com.pj95.clockwise.widget.scheduleWidgetTick
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Owns the single [AppContainer] instance for the process and the application
 * scope used for process-lifetime maintenance. The container is created eagerly
 * in [onCreate] so it is ready before any Activity or ViewModel asks for a
 * dependency.
 *
 * The city catalog is seeded off the main thread. Screens keep using the
 * bundled directory until Room is ready, so the first frame never waits on the
 * database.
 *
 * The widget's process-lifetime work lives here too: [startWidgetMirror] keeps a
 * home-screen copy in step with the saved cities, and the generated picker
 * preview is published once per widget-design version on Android 15+.
 *
 * Maintenance launched here is best-effort by design. The scope has a
 * [CoroutineExceptionHandler] instead of the default one, so a failure in a
 * background task (seeding, pruning, publishing a preview) is logged rather
 * than taking down a process the user is looking at. A debug build additionally
 * installs log-only [StrictMode] policies, so a stray main-thread disk read or
 * leaked resource shows up in logcat during development without changing
 * release behaviour.
 */
class ClockwiseApp : Application(), AppContainerProvider {

    override lateinit var container: AppContainer

    private val applicationScope = CoroutineScope(
        SupervisorJob() +
            Dispatchers.IO +
            CoroutineExceptionHandler { _, error ->
                Log.e(TAG, "Unhandled error in an application-scope task", error)
            },
    )
    private var widgetMirrorJob: Job? = null
    private var shortcutMirrorJob: Job? = null
    private var screenReceiverRegistered = false

    /**
     * The screen-state listener the widget ticker needs.
     *
     * `ACTION_SCREEN_ON` cannot be delivered to a manifest receiver, so it is
     * registered here only while a widget exists. Screen-on redraws immediately
     * (the visible minute may have gone stale while the device slept) and puts
     * the minute cadence back; screen-off lets the ticker fall back to the
     * battery-friendly interval.
     */
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    applicationScope.launch {
                        runCatching { ClockwiseWidget().updateAll(applicationContext) }
                            .onFailure { error ->
                                Log.w(TAG, "Widget redraw on screen-on failed", error)
                            }
                    }
                    startWidgetTicker()
                }
                Intent.ACTION_SCREEN_OFF -> scheduleWidgetTick(applicationContext)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        enableDebugStrictMode()
        container = DefaultAppContainer(applicationContext)

        // The channel has to exist before the first alarm can ring, and creating
        // it here means a process that starts only to ring an alarm already has
        // one. It is silent: the ringing service owns the audio.
        AlarmNotifications.ensureRingingChannel(this)
        TimerNotifications.ensureChannels(this)

        // `AlarmManager` keeps its triggers across a process death, but not
        // reliably across an app update or a force-stop. Re-arming on every
        // process start is cheap — a handful of rows — and closes that gap.
        applicationScope.launch {
            runCatching { container.alarmRepository.rescheduleAll() }
                .onFailure { error ->
                    Log.w(TAG, "Alarms could not be re-armed at process start", error)
                }
        }

        // A running timer is restored the same way. An end instant already in
        // the past rings now rather than being lost with the process.
        applicationScope.launch {
            runCatching { container.timerRepository.rescheduleRunning() }
                .onFailure { error ->
                    Log.w(TAG, "Timer could not be restored at process start", error)
                }
        }

        applicationScope.launch {
            runCatching { container.cityRepository.ensureSeeded() }
                .onFailure { error ->
                    Log.w(TAG, "City catalog could not be seeded; using bundled data", error)
                }
        }

        // The Studio catalogue is seeded once per process. It is idempotent, so
        // an existing purchase or running trial is never overwritten.
        applicationScope.launch {
            runCatching { container.widgetLicenseRepository.ensureSeeded() }
                .onFailure { error ->
                    Log.w(TAG, "Studio licences could not be seeded", error)
                }
        }

        // Solar rows are a cache, not user data, and were previously kept
        // forever. Pruning once per launch bounds the table without a periodic
        // worker: if the app is never opened, the cache cannot grow either.
        applicationScope.launch {
            runCatching {
                container.solarRepository.pruneBefore(
                    SolarCachePolicy.cutoff(container.clock),
                )
            }.onFailure { error ->
                Log.w(TAG, "Solar cache could not be pruned; the next launch will retry", error)
            }
        }

        // A process can start because the launcher or its minute alarm asked for
        // a widget update, with no Activity involved. Restoring the two things a
        // widget needs at that point — the saved-cities mirror and the minute
        // ticker — makes a reboot or a low-memory kill recover on its own.
        if (hasHomeScreenWidgets(this)) {
            startWidgetMirror()
            startWidgetTicker()
        }

        // The launcher draws its own long-press menu, so the saved cities have
        // to be published to it. The mirror watches only the saved ids and
        // their labels, and replaces the whole dynamic set on every change.
        shortcutMirrorJob = ShortcutMirror(
            scope = applicationScope,
            preferences = container.clockRepository.preferences,
            publish = { savedCityIds, cityLabels ->
                CityShortcuts.publish(this@ClockwiseApp, savedCityIds, cityLabels)
            },
            onError = { error ->
                Log.w(TAG, "Launcher shortcuts could not be updated", error)
            },
        ).start()

        applicationScope.launch {
            WidgetPreviewPublisher.create(this@ClockwiseApp).publishIfNeeded()
        }
    }

    /**
     * Starts the saved-cities mirror if it is not already running.
     *
     * Called by [ClockwiseApp] itself when the process starts with a widget on
     * screen, and by the widget's receiver when the first copy is added. Safe to
     * call repeatedly: the existing job is left alone, so two calls cannot start
     * two collectors over the same store.
     */
    @Synchronized
    fun startWidgetMirror() {
        if (widgetMirrorJob?.isActive == true) return

        widgetMirrorJob = WidgetMirror(
            scope = applicationScope,
            repository = container.clockRepository,
            refresh = { ClockwiseWidget().updateAll(applicationContext) },
            onError = { error ->
                Log.w(TAG, "Widget mirror redraw failed; the next change will retry", error)
            },
        ).start()
    }

    /** Stops the mirror when the last widget copy is removed. */
    @Synchronized
    fun stopWidgetMirror() {
        widgetMirrorJob?.cancel()
        widgetMirrorJob = null
    }

    /**
     * Arms the widget ticker and listens for the screen going on or off.
     *
     * Safe to call repeatedly: the receiver is registered once, and the alarm is
     * simply replaced. Called when the process starts with a widget on screen,
     * when the first widget is added, and after every tick.
     */
    @Synchronized
    fun startWidgetTicker() {
        if (!hasHomeScreenWidgets(this)) {
            stopWidgetTicker()
            return
        }

        if (!screenReceiverRegistered) {
            // Not exported: these are protected system broadcasts, and a
            // receiver registered at runtime must state its visibility on
            // Android 13+ when the app targets that API.
            ContextCompat.registerReceiver(
                this,
                screenStateReceiver,
                IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                },
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            screenReceiverRegistered = true
        }

        scheduleWidgetTick(applicationContext)
    }

    /** Stops the ticker and the screen listener when the last widget is removed. */
    @Synchronized
    fun stopWidgetTicker() {
        if (screenReceiverRegistered) {
            unregisterReceiver(screenStateReceiver)
            screenReceiverRegistered = false
        }
        cancelWidgetTick(applicationContext)
    }

    /**
     * Turns on StrictMode for debug builds only, with logging penalties.
     *
     * The release APK must behave exactly as before, so this is gated on the
     * APK's own debuggable flag rather than on `BuildConfig.DEBUG` alone: the
     * flag is what the platform uses, and it survives library or build-type
     * changes. Every detected violation is logged; nothing throws.
     */
    private fun enableDebugStrictMode() {
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (!debuggable) return

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build(),
        )
    }

    private companion object {
        const val TAG = "ClockwiseApp"
    }
}
