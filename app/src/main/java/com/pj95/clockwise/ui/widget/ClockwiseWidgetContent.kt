package com.pj95.clockwise.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity as startActivityWithIntent
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pj95.clockwise.MainActivity
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ui.WidgetRow
import com.pj95.clockwise.ui.WidgetUiState
import com.pj95.clockwise.ui.theme.LightClockwisePalette
import com.pj95.clockwise.ui.theme.NightClockwisePalette
import com.pj95.clockwise.widget.RefreshWidgetAction
import kotlin.math.roundToInt

/** Stable handles for the Glance unit tests; RemoteViews has no resource ids of its own. */
internal object WidgetTestTags {
    const val HEADER = "widget-header"
    const val REFRESH = "widget-refresh"
    const val MORE = "widget-more"

    fun row(cityId: String): String = "widget-row-$cityId"
}

/**
 * The widget's drawing, kept apart from the widget itself so Glance's own unit
 * test harness can compose it directly.
 *
 * Glance composables are not Compose composables: they compile to `RemoteViews`,
 * which is why this cannot be screenshotted the way the app's screens are, and
 * why it is asserted on instead (spec section 12.3).
 *
 * The layout is responsive. Glance provides [LocalSize] from the size the
 * launcher has given this copy; [widgetRowBudget] turns that into a row count,
 * so the same widget lists one city when compact and up to six when stretched.
 * The overflow is counted, never dropped.
 *
 * The widget follows the device's light/dark setting rather than the app's
 * stored choice: a home screen belongs to the launcher, and a widget that
 * ignored the system theme would be the odd one out among its neighbours.
 */
@Composable
@GlanceComposable
fun ClockwiseWidgetContent(
    state: WidgetUiState,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val fontScale = context.resources.configuration.fontScale
    val heightDp = size.height.value.let { if (it.isFinite()) it.roundToInt() else 0 }
    val maxRows = widgetRowBudget(
        availableHeightDp = heightDp,
        savedCityCount = state.rows.size,
        fontScale = fontScale,
    )
    val visibleRows = state.rows.take(maxRows)
    val hiddenCount = state.rows.size - visibleRows.size

    // Two containers rather than one, and the split is the point. The outer box
    // *is* the card: it is the view the launcher treats as the widget's
    // background, so the solid fill and the corner radius belong on it.
    Box(
        modifier = modifier
            .fillMaxSize()
            // Names this view as the widget's background, which is what lets a
            // launcher round its corners and cross-fade it. Without it the
            // background a launcher reserves for a widget can stay transparent
            // and the card appears to float on the wallpaper.
            .appWidgetBackground()
            // A translucent card, never the wallpaper. The drawable carries a
            // 94%-opaque Material 3 surface, rounded corners and a hairline
            // outline, so a busy wallpaper can tint the card without ever
            // making a city time unreadable. It resolves its light/dark fill
            // from the device resource configuration, because a widget belongs
            // to the home screen rather than to the app's stored theme.
            .background(ImageProvider(R.drawable.widget_background))
            .cornerRadius(WidgetGeometry.CornerRadius),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(
                    horizontal = WidgetGeometry.HorizontalPadding,
                    vertical = WidgetGeometry.VerticalPadding,
                ),
        ) {
            WidgetHeader(
                deviceTimeText = state.deviceTimeText,
                fontScale = fontScale,
            )

            Spacer(GlanceModifier.height(WidgetGeometry.GapAboveRule))
            WidgetHairline()
            Spacer(GlanceModifier.height(WidgetGeometry.GapBelowRule))

            if (state.isEmpty) {
                WidgetEmptyState()
            } else {
                visibleRows.forEachIndexed { index, row ->
                    WidgetCityRow(row = row, fontScale = fontScale)
                    if (index != visibleRows.lastIndex) {
                        Spacer(GlanceModifier.height(WidgetGeometry.RowSpacing))
                    }
                }
                if (hiddenCount > 0) {
                    WidgetHiddenCount(hiddenCount = hiddenCount, fontScale = fontScale)
                }

                // Pushes everything up, so a widget the user has stretched taller
                // keeps its content anchored to the top instead of floating in the
                // middle.
                Spacer(GlanceModifier.defaultWeight())
            }
        }
    }
}

@Composable
@GlanceComposable
private fun WidgetHeader(
    deviceTimeText: String,
    fontScale: Float,
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.Vertical.CenterVertically,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(WidgetGeometry.headerHeight(fontScale).dp),
    ) {
        Row(
            verticalAlignment = Alignment.Vertical.CenterVertically,
            modifier = GlanceModifier
                .defaultWeight()
                .height(WidgetGeometry.headerHeight(fontScale).dp)
                // The title block is the way back into the app from the card.
                .clickable(actionStartActivity<MainActivity>())
                .semantics { testTag = WidgetTestTags.HEADER },
        ) {
            Text(
                text = context.getString(R.string.widget_title),
                style = TextStyle(
                    fontSize = WidgetGeometry.HeaderSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    color = WidgetColors.Supporting,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                // The device's own clock, so every row below has something to be
                // relative to. Without it a row reading "01:45" is unanchored.
                text = deviceTimeText,
                style = TextStyle(
                    fontSize = WidgetGeometry.HeaderSizeSp.sp,
                    fontWeight = FontWeight.Medium,
                    color = WidgetColors.Supporting,
                ),
                maxLines = 1,
            )
        }

        Spacer(GlanceModifier.width(4.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .size(WidgetGeometry.RefreshTouchSize)
                // The card is a home-screen surface, so a quiet refresh action
                // belongs here: it redraws without opening the app and is the
                // only thing in the header that is not a way in.
                .clickable(actionRunCallback<RefreshWidgetAction>())
                .semantics { testTag = WidgetTestTags.REFRESH },
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = context.getString(R.string.a11y_widget_refresh),
                colorFilter = ColorFilter.tint(WidgetColors.Action),
                modifier = GlanceModifier.size(WidgetGeometry.RefreshIconSize),
            )
        }
    }
}

/**
 * One city.
 *
 * The whole row is the tap target, so a finger cannot miss between the name and
 * the time, and it opens that city's detail screen rather than dropping the user
 * at the top of the app.
 *
 * The children are left to be announced in reading order rather than merged
 * into a single description: `RemoteViews` has no equivalent of Compose's
 * merged semantics. That still reads correctly, because the weekday is only
 * hidden when the city shares the device's day — so whatever is on screen is
 * also what is spoken.
 */
@Composable
@GlanceComposable
private fun WidgetCityRow(
    row: WidgetRow,
    fontScale: Float,
) {
    val context = LocalContext.current

    Row(
        verticalAlignment = Alignment.Vertical.CenterVertically,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(WidgetGeometry.rowHeight(fontScale).dp)
            .clickable(startActivityWithIntent(cityDetailIntent(context, row.id)))
            .semantics { testTag = WidgetTestTags.row(row.id) },
    ) {
        Image(
            provider = ImageProvider(
                if (row.isDaytime) R.drawable.ic_sun else R.drawable.ic_moon,
            ),
            contentDescription = context.getString(
                if (row.isDaytime) R.string.a11y_daytime else R.string.a11y_nighttime,
            ),
            colorFilter = ColorFilter.tint(WidgetColors.Supporting),
            modifier = GlanceModifier.size(12.dp),
        )

        Spacer(GlanceModifier.width(8.dp))

        Text(
            text = row.displayName,
            style = TextStyle(
                fontSize = WidgetGeometry.NameSizeSp.sp,
                fontWeight = FontWeight.Medium,
                color = WidgetColors.Content,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )

        Spacer(GlanceModifier.width(6.dp))

        if (row.isAnotherDay) {
            Text(
                // Only the exception is labelled: on the day everyone shares,
                // the weekday would be noise.
                text = row.weekdayLabel,
                style = TextStyle(
                    fontSize = WidgetGeometry.WeekdaySizeSp.sp,
                    fontWeight = FontWeight.Medium,
                    color = WidgetColors.Supporting,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(6.dp))
        }

        Text(
            text = row.timeText,
            style = TextStyle(
                fontSize = WidgetGeometry.TimeSizeSp.sp,
                fontWeight = FontWeight.Bold,
                color = WidgetColors.Content,
            ),
            maxLines = 1,
        )
    }
}

@Composable
@GlanceComposable
private fun WidgetEmptyState() {
    val context = LocalContext.current

    Box(
        contentAlignment = Alignment.TopStart,
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Text(
            text = context.getString(R.string.widget_empty),
            style = TextStyle(
                fontSize = WidgetGeometry.CountSizeSp.sp,
                fontWeight = FontWeight.Normal,
                color = WidgetColors.Supporting,
            ),
            maxLines = 2,
        )
    }
}

@Composable
@GlanceComposable
private fun WidgetHiddenCount(
    hiddenCount: Int,
    fontScale: Float,
) {
    val context = LocalContext.current

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(WidgetGeometry.countHeight(fontScale).dp)
            // Counted, not dropped: a widget that silently omitted cities would
            // be lying about what the app holds. Tapping it opens the app.
            .clickable(actionStartActivity<MainActivity>())
            .semantics { testTag = WidgetTestTags.MORE },
    ) {
        Text(
            text = context.resources.getQuantityString(
                R.plurals.widget_more_cities,
                hiddenCount,
                hiddenCount,
            ),
            style = TextStyle(
                fontSize = WidgetGeometry.CountSizeSp.sp,
                fontWeight = FontWeight.Medium,
                color = WidgetColors.Supporting,
            ),
            maxLines = 1,
        )
    }
}

/** A one-pixel rule, standing in for the Material divider the app never uses. */
@Composable
@GlanceComposable
private fun WidgetHairline() {
    Spacer(
        GlanceModifier
            .fillMaxWidth()
            .height(WidgetGeometry.RuleHeight)
            .background(WidgetColors.Hairline),
    )
}

/**
 * The Intent a row's tap starts.
 *
 * `singleTop` plus the extra means a widget tap on a cold start lands on the
 * city's detail screen, and a tap while the app is already open calls
 * `onNewIntent` instead of stacking a second task.
 */
internal fun cityDetailIntent(context: Context, cityId: String): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_CITY_ID, cityId)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

/** The app's own tokens, offered to Glance as a day/night pair. */
private object WidgetColors {
    val Content = dayNight(LightClockwisePalette.foreground, NightClockwisePalette.foreground)
    val Supporting = dayNight(
        LightClockwisePalette.mutedForeground,
        NightClockwisePalette.mutedForeground,
    )
    val Hairline = dayNight(LightClockwisePalette.border, NightClockwisePalette.border)
    val Action = dayNight(LightClockwisePalette.mapAccent, NightClockwisePalette.mapAccent)
}

private fun dayNight(day: Color, night: Color): ColorProvider =
    DayNightColorProvider(day = day, night = night)
