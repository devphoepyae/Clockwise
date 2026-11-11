package com.pj95.clockwise.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.pj95.clockwise.ClockwiseApp

/**
 * The widget's entry point: the component the launcher and the platform talk to.
 *
 * Besides the callbacks Glance needs, this receiver owns the minute ticker's
 * lifetime — armed when the first copy of the widget is added, re-armed on every
 * platform update (which is also how it survives a reboot), and cancelled when
 * the last copy is removed. It also starts [WidgetMirror], so a widget added
 * while the app is not running still tracks later changes to the saved cities,
 * and stops it when the last copy is gone.
 */
class ClockwiseWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = ClockwiseWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        startMirror(context)
        startTicker(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // The platform asks for an update on every reboot and every resize, and
        // alarms do not survive a reboot: re-arming here is what puts the ticker
        // back after one.
        startMirror(context)
        startTicker(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // The classic card is gone, so its saved-cities mirror has nothing to
        // feed. The minute ticker is shared with the Studio styles, so it only
        // stops when no widget of any style remains.
        stopMirror(context)
        if (!hasHomeScreenWidgets(context)) stopTicker(context)
    }

    private fun startMirror(context: Context) {
        (context.applicationContext as? ClockwiseApp)?.startWidgetMirror()
    }

    private fun stopMirror(context: Context) {
        (context.applicationContext as? ClockwiseApp)?.stopWidgetMirror()
    }

    private fun startTicker(context: Context) {
        val app = context.applicationContext as? ClockwiseApp
        if (app != null) app.startWidgetTicker() else scheduleWidgetTick(context)
    }

    private fun stopTicker(context: Context) {
        val app = context.applicationContext as? ClockwiseApp
        if (app != null) app.stopWidgetTicker() else cancelWidgetTick(context)
    }
}
