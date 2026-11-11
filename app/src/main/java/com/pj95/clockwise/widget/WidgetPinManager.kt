package com.pj95.clockwise.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.pj95.clockwise.ClockwiseApp
import com.pj95.clockwise.data.repository.DefaultWidgetCityIds
import com.pj95.clockwise.data.repository.WidgetConfig
import com.pj95.clockwise.model.widget.WidgetStyleId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Pins one Studio style straight onto the home screen.
 *
 * The launcher owns the confirmation dialog: `requestPinGlanceAppWidget` shows
 * it, and only calls our [WidgetPinResultReceiver] once the user has agreed.
 * That is the whole reason there is no "add widget" instructions screen in the
 * gallery — on a launcher that supports pinning, one tap is the flow.
 *
 * Pinning is only available on API 26+, which is the app's minimum, so the
 * caller can treat `false` as "this launcher declined" rather than "unsupported".
 */
object WidgetPinManager {

    /**
     * Asks the current launcher to pin [style].
     *
     * @return true when the launcher accepted the request. The widget is not
     *   added yet at that point; the success callback writes its configuration.
     */
    suspend fun pin(context: Context, style: WidgetStyleId): Boolean {
        val callback = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + style.ordinal,
            Intent(context, WidgetPinResultReceiver::class.java)
                .setAction(WidgetPinResultReceiver.ACTION_PINNED)
                .putExtra(WidgetPinResultReceiver.EXTRA_STYLE, style.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return when (style) {
            WidgetStyleId.FREE_SLATE_MULTIFUNCTION -> request(
                context,
                FreeSlateWidgetReceiver::class.java,
                style,
                callback,
            )
            WidgetStyleId.FREE_DIAL_24H -> request(
                context,
                Dial24hWidgetReceiver::class.java,
                style,
                callback,
            )
            WidgetStyleId.FREE_MARKET_SESSIONS -> request(
                context,
                MarketSessionsWidgetReceiver::class.java,
                style,
                callback,
            )
            WidgetStyleId.PREMIUM_GLOBAL_CLOCKS -> request(
                context,
                GlobalClocksWidgetReceiver::class.java,
                style,
                callback,
            )
            WidgetStyleId.PREMIUM_AMBIENT_GRID -> request(
                context,
                AmbientGridWidgetReceiver::class.java,
                style,
                callback,
            )
        }
    }

    private suspend fun <R : GlanceAppWidgetReceiver> request(
        context: Context,
        receiver: Class<R>,
        style: WidgetStyleId,
        callback: PendingIntent,
    ): Boolean = GlanceAppWidgetManager(context).requestPinGlanceAppWidget(
        receiver = receiver,
        preview = ChronosWidget(style),
        previewState = null,
        successCallback = callback,
    )

    private const val REQUEST_CODE_BASE = 0x5700
}

/**
 * The launcher's "pinned" confirmation.
 *
 * It receives the new instance id, writes the style's default three cities if
 * the configuration activity has not already written a row, and asks the new
 * widget to draw itself. Without this row a pinned card would still render its
 * defaults, but the gallery would not be able to offer city editing until the
 * user opened the app again.
 */
class WidgetPinResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PINNED) return

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val style = WidgetStyleId.fromId(intent.getStringExtra(EXTRA_STYLE).orEmpty())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || style == null) return

        val app = context.applicationContext as? ClockwiseApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existing = app.container.widgetConfigRepository.get(appWidgetId)
                if (existing == null) {
                    app.container.widgetConfigRepository.save(
                        WidgetConfig(
                            appWidgetId = appWidgetId,
                            style = style,
                            cityIds = DefaultWidgetCityIds,
                            isPremium = style.isPremium,
                        ),
                    )
                }
                ChronosWidget(style).update(
                    context,
                    GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId),
                )
            } finally {
                pending.finish()
                app.startWidgetTicker()
            }
        }
    }

    companion object {
        const val ACTION_PINNED: String = "com.pj95.clockwise.action.WIDGET_PINNED"
        const val EXTRA_STYLE: String = "com.pj95.clockwise.extra.PINNED_STYLE"
    }
}
