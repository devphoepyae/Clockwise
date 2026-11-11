package com.pj95.clockwise.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.pj95.clockwise.di.appContainer
import com.pj95.clockwise.ui.WidgetUiState
import com.pj95.clockwise.ui.mapping.buildWidgetUiState
import com.pj95.clockwise.ui.widget.WIDGET_SIZES
import com.pj95.clockwise.ui.widget.ClockwiseWidgetContent
import kotlinx.coroutines.flow.first
import java.time.ZoneId

/**
 * The home-screen widget (spec section 5.11).
 *
 * Reads the same Proto DataStore the app writes — through the same repository,
 * so a first collection still seeds the first-launch defaults — and hands one
 * frozen snapshot to [ClockwiseWidgetContent]. Composition itself therefore
 * does no I/O and no clock reads: every redraw is a fresh, consistent minute.
 *
 * Redraws are driven by [scheduleWidgetTick] (once a minute), by [WidgetMirror]
 * (immediately, whenever the saved cities change), and by the card's own
 * refresh action.
 *
 * The card declares a fixed set of responsive sizes. Glance composes the content
 * once for each size, so the same card lists one city when compact and up to six
 * at 4x4. The 4x4 size is the provider's default footprint, which is why a
 * freshly added widget shows every default city.
 */
class ClockwiseWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(WIDGET_SIZES)

    /**
     * Previews use the same responsive set.
     *
     * Without this, Android would render the picker preview at the provider's
     * minimum size and the taller layouts — the ones that show more than one
     * city — would be missing from the picker. The actual widget still chooses
     * its own view from [sizeMode] on the home screen.
     */
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(WIDGET_SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)
        provideContent {
            ClockwiseWidgetContent(state)
        }
    }

    /**
     * The generated picker preview, introduced by Glance 1.2.
     *
     * It is a single composition with no recomposition or effects, so the data
     * is loaded once with `first()` and handed over like any other snapshot.
     * Publishing is rate-limited by the system; [WidgetPreviewPublisher] waits
     * for the app's first launch and only calls it again when the widget's
     * design version changes. Older releases and old Android versions use the
     * static `previewImage` declared in the provider info.
     */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val state = loadState(context)
        provideContent {
            ClockwiseWidgetContent(state)
        }
    }

    private suspend fun loadState(context: Context): WidgetUiState {
        val preferences = context.appContainer.clockRepository.preferences.first()
        return buildWidgetUiState(
            savedCityIds = preferences.savedCityIds,
            cityLabels = preferences.cityLabels,
            use24Hour = preferences.use24Hour,
            now = context.appContainer.clock.instant(),
            // The device's own zone, the same reference the app's cards and the
            // meeting planner use.
            deviceZoneId = ZoneId.systemDefault(),
        )
    }
}
