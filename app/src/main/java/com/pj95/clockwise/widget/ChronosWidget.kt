package com.pj95.clockwise.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.pj95.clockwise.ClockwiseApp
import com.pj95.clockwise.model.City
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.data.repository.DefaultWidgetCityIds
import com.pj95.clockwise.di.appContainer
import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.model.time.MarketSessions
import com.pj95.clockwise.model.widget.WidgetStyleId
import com.pj95.clockwise.ui.widget.ChronosMarketSession
import com.pj95.clockwise.ui.widget.ChronosWidgetCity
import com.pj95.clockwise.ui.widget.ChronosWidgetContent
import com.pj95.clockwise.ui.widget.ChronosWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/**
 * One Chronos Studio home-screen style.
 *
 * The style is a constructor parameter rather than a saved preference: each of
 * the four providers is a separate `AppWidgetProvider`, so the launcher — not a
 * row in the database — decides which style a given copy is. That is what lets
 * a user pin two different styles side by side.
 *
 * Everything the card needs is read here, once per update: the instance's three
 * cities from Room, its local entitlement, and the current instant. Composition
 * then receives a frozen snapshot, so `RemoteViews` never does I/O.
 */
class ChronosWidget(
    val style: WidgetStyleId,
) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // `GlanceAppWidgetManager` is the public way to turn the opaque Glance id
        // back into the launcher's widget id. The cast-based shortcut
        // (`AppWidgetId.appWidgetId`) is a library-internal API in Glance 1.2.
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val state = loadState(context, appWidgetId)
        provideContent {
            ChronosWidgetContent(state)
        }
    }

    private suspend fun loadState(context: Context, appWidgetId: Int): ChronosWidgetState {
        val container = context.appContainer
        val preferences = container.clockRepository.preferences.first()
        val license = container.widgetLicenseRepository.state(style)
        val config = if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            null
        } else {
            container.widgetConfigRepository.get(appWidgetId)
        }

        val now = container.clock.instant()
        val deviceZone = ZoneId.systemDefault()
        val device = CityTimeFormatter.snapshot(deviceZone, now, preferences.use24Hour)
        val cityIds = config?.cityIds ?: DefaultWidgetCityIds
        val cities = CityDirectory.citiesForIds(cityIds).map { it.toWidgetCity(now, preferences.use24Hour, device.weekdayShort) }
        val markets = if (style == WidgetStyleId.FREE_MARKET_SESSIONS) {
            MarketSessions.cityIds.mapNotNull { cityId ->
                val city = CityDirectory.find(cityId) ?: return@mapNotNull null
                val zone = ZoneId.of(city.timeZone)
                val snapshot = CityTimeFormatter.snapshot(zone, now, preferences.use24Hour)
                ChronosMarketSession(
                    id = city.id,
                    name = city.name,
                    timeText = snapshot.timeText,
                    isOpen = MarketSessions.isOpen(zone, now),
                )
            }
        } else {
            emptyList()
        }

        val dark = isDark(context)
        val accent = materialYouAccent(context)
        val usable = license.isUsable

        return ChronosWidgetState(
            style = style,
            deviceTimeText = device.timeText,
            deviceWeekday = device.weekdayShort,
            cities = cities,
            isLocked = !usable,
            accentArgb = accent,
            isDark = dark,
            dialArt = if (usable && style == WidgetStyleId.FREE_DIAL_24H) {
                ChronosWidgetArt.dial(
                    accent = accent,
                    dayFraction = dayFraction(now, deviceZone),
                    isDark = dark,
                )
            } else {
                null
            },
            ambientArt = if (usable && style == WidgetStyleId.PREMIUM_AMBIENT_GRID) {
                ChronosWidgetArt.ambient(accent = accent, isDark = dark)
            } else {
                null
            },
            markets = markets,
        )
    }

    private fun City.toWidgetCity(
        now: Instant,
        use24Hour: Boolean,
        deviceWeekday: String,
    ): ChronosWidgetCity {
        val snapshot = CityTimeFormatter.snapshot(ZoneId.of(timeZone), now, use24Hour)
        return ChronosWidgetCity(
            id = id,
            name = name,
            timeText = snapshot.timeText,
            weekday = snapshot.weekdayShort,
            isAnotherDay = snapshot.weekdayShort != deviceWeekday,
        )
    }

    private fun dayFraction(now: Instant, zone: ZoneId): Float {
        val local = now.atZone(zone)
        val seconds = local.hour * 3_600 + local.minute * 60 + local.second
        return seconds / 86_400f
    }

    private fun isDark(context: Context): Boolean {
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * The wallpaper's accent on Android 12+, and the app's own neutral on older
     * releases. Material You is the point of the premium styles; a fixed purple
     * fallback would undo it on the devices that cannot do better.
     */
    private fun materialYouAccent(context: Context): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { context.getColor(android.R.color.system_accent1_500) }
                .getOrDefault(NEUTRAL_ACCENT)
        } else {
            NEUTRAL_ACCENT
        }

    companion object {
        private const val NEUTRAL_ACCENT = 0xFF4F4F4D.toInt()

        /** Every provider, for the minute ticker and the presence check. */
        val all: List<ChronosWidget>
            get() = WidgetStyleId.entries.map(::ChronosWidget)
    }
}

/**
 * The shared lifecycle of a Studio provider.
 *
 * `onUpdate` is where the minute ticker is re-armed after a reboot or a resize;
 * `onDisabled` stops it only when *no* widget of any style is left, because a
 * user can pin a free and a premium card at the same time.
 */
abstract class StudioWidgetReceiver : GlanceAppWidgetReceiver() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        startTicker(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        startTicker(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val app = context.applicationContext as? ClockwiseApp ?: return
        CoroutineScope(Dispatchers.IO).launch {
            appWidgetIds.forEach { id ->
                runCatching { app.container.widgetConfigRepository.delete(id) }
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        if (!hasHomeScreenWidgets(context)) {
            (context.applicationContext as? ClockwiseApp)?.stopWidgetTicker()
                ?: cancelWidgetTick(context)
        }
    }

    private fun startTicker(context: Context) {
        val app = context.applicationContext as? ClockwiseApp
        if (app != null) app.startWidgetTicker() else scheduleWidgetTick(context)
    }
}

/** Free slate: the device clock plus timer and stopwatch shortcuts. */
class FreeSlateWidgetReceiver : StudioWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget =
        ChronosWidget(WidgetStyleId.FREE_SLATE_MULTIFUNCTION)
}

/** Free 24-hour dial: one turn of the day with the local time in the middle. */
class Dial24hWidgetReceiver : StudioWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget =
        ChronosWidget(WidgetStyleId.FREE_DIAL_24H)
}

/** Free market card: the four financial centres and their open/closed state. */
class MarketSessionsWidgetReceiver : StudioWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget =
        ChronosWidget(WidgetStyleId.FREE_MARKET_SESSIONS)
}

/** Premium global clocks: three configured cities and their local times. */
class GlobalClocksWidgetReceiver : StudioWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget =
        ChronosWidget(WidgetStyleId.PREMIUM_GLOBAL_CLOCKS)
}

/** Premium ambient grid: a quiet 2x2 of configured places. */
class AmbientGridWidgetReceiver : StudioWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget =
        ChronosWidget(WidgetStyleId.PREMIUM_AMBIENT_GRID)
}
