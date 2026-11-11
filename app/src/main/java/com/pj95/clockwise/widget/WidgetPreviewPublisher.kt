package com.pj95.clockwise.widget

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.setWidgetPreviews
import kotlinx.coroutines.CancellationException

/**
 * Publishes the generated widget-picker previews introduced by Glance 1.2.
 *
 * Android 15+ can render a live preview of a Glance widget instead of showing a
 * static image. There is no callback that asks for one, so the app publishes it
 * itself — here, once per widget-design version, on the first launch after an
 * upgrade.
 *
 * Two platform constraints shape this class:
 *
 *  - `setWidgetPreviews` only exists on API 35+. Older devices keep the static
 *    `previewImage` declared in the provider info, which is why that fallback
 *    still has to look like the card.
 *  - The call is rate-limited by the system (about twice an hour), so a failed
 *    or rejected call must not record success. Leaving the marker alone means
 *    the next launch tries again; recording it after a rate-limit would leave
 *    the old preview in place until the design changed again.
 *
 * The marker lives in a tiny private `SharedPreferences` file rather than the
 * Proto DataStore: it is process bookkeeping, not a user preference, and adding
 * it to the user schema would mean a migration for a value nobody can change.
 */
internal class WidgetPreviewPublisher(
    private val sdkInt: Int,
    private val publishedVersion: () -> Int,
    private val markPublished: (Int) -> Unit,
    private val publish: suspend () -> Int,
    private val onError: (Throwable) -> Unit = {},
) {

    /**
     * Publishes the previews when this device can show them and this design has
     * not been published yet.
     *
     * Every failure is non-fatal: a widget picker without a generated preview is
     * still a working widget, and the next launch will try again.
     */
    suspend fun publishIfNeeded(designVersion: Int = DESIGN_VERSION) {
        if (sdkInt < MIN_SDK) return
        if (publishedVersion() >= designVersion) return

        val result = try {
            publish()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            onError(error)
            return
        }

        if (result == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS) {
            markPublished(designVersion)
        }
    }

    companion object {
        /**
         * Bump this whenever the widget's design changes enough that the picker
         * preview should be regenerated. It is not the app's version name: the
         * preview only needs republishing when it would look different.
         */
        const val DESIGN_VERSION: Int = 1

        /** Android 15, the first release with generated widget previews. */
        internal const val MIN_SDK: Int = Build.VERSION_CODES.VANILLA_ICE_CREAM

        private const val PREFS_NAME = "widget_previews"
        private const val KEY_VERSION = "published_design_version"
        private const val TAG = "WidgetPreviewPublisher"

        fun create(context: Context): WidgetPreviewPublisher {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return WidgetPreviewPublisher(
                sdkInt = Build.VERSION.SDK_INT,
                publishedVersion = { prefs.getInt(KEY_VERSION, 0) },
                markPublished = { version ->
                    prefs.edit().putInt(KEY_VERSION, version).apply()
                },
                publish = {
                    GlanceAppWidgetManager(context)
                        .setWidgetPreviews<ClockwiseWidgetReceiver>()
                },
                onError = { error ->
                    Log.w(TAG, "Widget previews could not be published", error)
                },
            )
        }
    }
}
