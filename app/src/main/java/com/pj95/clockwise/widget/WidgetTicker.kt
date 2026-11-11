package com.pj95.clockwise.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.pj95.clockwise.ClockwiseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** The one action [WidgetTickerReceiver] acts on. */
const val WIDGET_TICK_ACTION: String = "com.pj95.clockwise.action.WIDGET_TICK"

private const val TICK_REQUEST_CODE = 0x5743
private const val MILLIS_PER_MINUTE = 60_000L
private const val TAG = "WidgetTicker"

/**
 * How often the card is redrawn while the screen is off.
 *
 * Nobody is looking at a home-screen widget on a locked screen, so the exact
 * minute is not worth a wake-up. Five minutes keeps the card roughly current
 * and lets the platform batch the alarm with other work; `ACTION_SCREEN_ON`
 * redraws immediately and puts the minute ticker back, so the stale interval is
 * never visible.
 */
internal const val BACKGROUND_TICK_MINUTES: Long = 5L

/**
 * The widget's minute ticker.
 *
 * `RemoteViews` cannot tick on their own, so the widget is redrawn by an alarm
 * that re-arms itself only while at least one copy of the widget is on a home
 * screen.
 *
 * The cadence follows the screen. While the device is interactive the alarm
 * sits on the next minute boundary, which is what keeps the displayed minute
 * truthful. While the screen is off it backs off to [BACKGROUND_TICK_MINUTES]
 * and uses `set` rather than `setAndAllowWhileIdle`, so it is inexact and can be
 * batched or deferred by Doze. `WidgetTickerReceiver` and the screen-state
 * receiver in [ClockwiseApp] put the minute cadence back the moment the screen
 * comes on.
 *
 * [`AlarmManager.setAndAllowWhileIdle`] rather than `setExactAndAllowWhileIdle`,
 * deliberately: the exact variant needs the `SCHEDULE_EXACT_ALARM` special
 * permission on API 31+, and a clock widget is not worth asking the user for it.
 * The trade is that while the device is asleep in Doze the platform throttles the
 * alarm to roughly once every nine minutes, so a widget left alone on a sleeping
 * device can show a stale minute — and catches up as soon as it wakes, is looked
 * at, or is refreshed by hand. The trade is recorded in the specification
 * (section 5.11).
 */
fun scheduleWidgetTick(
    context: Context,
    // Injected so both decisions can be exercised: the presence check is the
    // only part that needs a real widget host, and the interactive check is the
    // only part that needs a real power manager.
    widgetsExist: Boolean = hasHomeScreenWidgets(context),
    isInteractive: Boolean = isDeviceInteractive(context),
) {
    val alarms = context.getSystemService(AlarmManager::class.java) ?: return

    if (!widgetsExist) {
        // Nothing to draw: stop the ticker rather than waking the device for
        // an empty screen.
        alarms.cancel(tickIntent(context))
        return
    }

    val now = System.currentTimeMillis()
    if (isInteractive) {
        alarms.setAndAllowWhileIdle(
            AlarmManager.RTC,
            nextMinuteBoundary(now),
            tickIntent(context),
        )
    } else {
        // Inexact while nobody is looking: the platform may batch this with
        // other alarms, and a home-screen card that is five minutes stale
        // behind a locked screen cannot be seen.
        alarms.set(
            AlarmManager.RTC,
            nextBackgroundBoundary(now),
            tickIntent(context),
        )
    }
}

/**
 * The next whole minute after [nowMillis].
 *
 * A clock that ran to the minute it was already in would be a minute behind, so
 * the boundary is always strictly ahead — including when the tick arrives
 * exactly on one.
 */
internal fun nextMinuteBoundary(nowMillis: Long): Long =
    nowMillis - (nowMillis % MILLIS_PER_MINUTE) + MILLIS_PER_MINUTE

/**
 * The next [BACKGROUND_TICK_MINUTES]-aligned boundary after [nowMillis].
 *
 * Always strictly in the future, and always a multiple of the background
 * interval, so repeated screen-off ticks cannot drift into a faster cadence.
 */
internal fun nextBackgroundBoundary(nowMillis: Long): Long {
    val period = BACKGROUND_TICK_MINUTES * MILLIS_PER_MINUTE
    val nextMinute = nextMinuteBoundary(nowMillis)
    val remainder = nextMinute % period
    return if (remainder == 0L) nextMinute else nextMinute + (period - remainder)
}

/** True while the screen is on and the device is not asleep. */
fun isDeviceInteractive(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isInteractive ?: true

/** Stops the ticker; called when the last widget is removed. */
fun cancelWidgetTick(context: Context) {
    context.getSystemService(AlarmManager::class.java)?.cancel(tickIntent(context))
}

/** True while at least one copy of any home-screen widget is on a home screen. */
fun hasHomeScreenWidgets(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    return allWidgetReceivers.any { receiver ->
        manager.getAppWidgetIds(ComponentName(context, receiver)).isNotEmpty()
    }
}

/**
 * Every provider the app ships. Kept as one list so the presence check and the
 * minute ticker cannot forget a style added later.
 */
private val allWidgetReceivers: List<Class<out android.appwidget.AppWidgetProvider>> = listOf(
    ClockwiseWidgetReceiver::class.java,
    FreeSlateWidgetReceiver::class.java,
    Dial24hWidgetReceiver::class.java,
    GlobalClocksWidgetReceiver::class.java,
    AmbientGridWidgetReceiver::class.java,
)

/**
 * Redraws every widget style once.
 *
 * `updateAll` is per provider class, so each style is asked separately. A style
 * with no copies on a home screen is a cheap no-op.
 */
suspend fun updateAllWidgets(context: Context) {
    ClockwiseWidget().updateAll(context)
    ChronosWidget.all.forEach { widget -> widget.updateAll(context) }
}

private fun tickIntent(context: Context): PendingIntent =
    PendingIntent.getBroadcast(
        context,
        TICK_REQUEST_CODE,
        Intent(context, WidgetTickerReceiver::class.java).setAction(WIDGET_TICK_ACTION),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

/**
 * Redraws the widget once, then arms the next tick.
 *
 * Besides the minute alarm, this receiver also listens for the system's own
 * clock changes. A time-zone or manual-time change rewrites every time on the
 * card, and waiting for the next minute would leave the wrong hours on screen.
 *
 * `updateAll` talks to the widget host, so it is a suspending call that must not
 * run on the main thread: `goAsync` keeps the broadcast alive until it finishes.
 */
class WidgetTickerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WIDGET_TICK_ACTION,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            -> Unit
            else -> return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.Default).launch {
            try {
                updateAllWidgets(appContext)
                // A tick also means the process is awake and the widget is
                // wanted, so the saved-cities mirror is worth having too.
                (appContext as? ClockwiseApp)?.startWidgetMirror()
            } catch (error: Throwable) {
                Log.w(TAG, "Widget redraw failed; the next tick is still armed", error)
            } finally {
                pendingResult.finish()
                // Re-armed after the redraw, so a slow redraw cannot make the
                // next tick arrive early and double-up.
                (appContext as? ClockwiseApp)?.startWidgetTicker()
                    ?: scheduleWidgetTick(appContext)
            }
        }
    }
}
