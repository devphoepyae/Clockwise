package com.pj95.clockwise.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * Redraws the widget on demand.
 *
 * This is the card's one Glance action callback: tapping the refresh glyph
 * re-composes this widget at the current minute without opening the app. It is
 * deliberately per-widget — `update(glanceId)` rather than `updateAll` — because
 * the tap belongs to one copy on one home screen. The minute ticker is still the
 * thing that keeps a clock ticking; this is the escape hatch for a widget that
 * was asleep in Doze and is being looked at again.
 */
class RefreshWidgetAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ClockwiseWidget().update(context, glanceId)
    }
}
