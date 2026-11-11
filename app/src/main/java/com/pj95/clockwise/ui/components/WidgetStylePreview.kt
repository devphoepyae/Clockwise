package com.pj95.clockwise.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.data.repository.DefaultWidgetCityIds
import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.model.time.MarketSessions
import com.pj95.clockwise.model.widget.WidgetStyleId
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin

/**
 * A live Compose preview of one widget style.
 *
 * The gallery cannot reuse the Glance content: Glance compiles to `RemoteViews`,
 * not Compose, and a `RemoteViews` cannot be embedded in a scrolling screen.
 * This is the same design drawn natively, with the real times and the real
 * Material You accent, so what the user sees in the gallery is what the card
 * will look like on their home screen.
 */
@Composable
fun WidgetStylePreview(
    style: WidgetStyleId,
    now: Instant,
    use24Hour: Boolean,
    modifier: Modifier = Modifier,
) {
    val aspect = when (style) {
        WidgetStyleId.FREE_DIAL_24H,
        WidgetStyleId.PREMIUM_AMBIENT_GRID,
        -> 1.05f
        else -> 1.5f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(RoundedCornerShape(24.dp))
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, RoundedCornerShape(24.dp)),
    ) {
        val compact = maxHeight < 160.dp
        when (style) {
            WidgetStyleId.FREE_SLATE_MULTIFUNCTION -> SlatePreview(now, use24Hour, compact)
            WidgetStyleId.FREE_DIAL_24H -> DialPreview(now, use24Hour)
            WidgetStyleId.FREE_MARKET_SESSIONS -> MarketSessionsPreview(now, use24Hour, compact)
            WidgetStyleId.PREMIUM_GLOBAL_CLOCKS -> GlobalClocksPreview(now, use24Hour, compact)
            WidgetStyleId.PREMIUM_AMBIENT_GRID -> AmbientGridPreview(now, use24Hour)
        }
    }
}

@Composable
private fun SlatePreview(now: Instant, use24Hour: Boolean, compact: Boolean) {
    val zoned = now.atZone(ZoneId.systemDefault())
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.studio_widget_slate_title),
                style = ClockwiseTextStyles.Eyebrow,
                color = ClockwiseColors.MutedForeground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = zoned.dayOfWeek.name.take(3),
                style = ClockwiseTextStyles.Eyebrow,
                color = ClockwiseColors.MutedForeground,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = CityTimeFormatter.timeText(zoned.toLocalTime(), use24Hour),
            style = ClockwiseTextStyles.DetailClock.copy(fontSize = if (compact) 34.sp else 42.sp),
            color = ClockwiseColors.Foreground,
        )
        Text(
            text = zoned.toLocalDate().toString(),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
        )
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PreviewPill(R.drawable.ic_timer, stringResource(R.string.studio_widget_timer))
            PreviewPill(R.drawable.ic_stopwatch, stringResource(R.string.studio_widget_stopwatch))
        }
    }
}

@Composable
private fun PreviewPill(iconRes: Int, label: String) {
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(ClockwiseColors.Background)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = ClockwiseTextStyles.DstCaption,
            color = ClockwiseColors.Foreground,
        )
    }
}

@Composable
private fun DialPreview(now: Instant, use24Hour: Boolean) {
    val zoned = now.atZone(ZoneId.systemDefault())
    val fraction = (
        zoned.hour * 3_600 + zoned.minute * 60 + zoned.second
        ) / 86_400f
    val accent = MaterialTheme.colorScheme.primary
    val cities = CityDirectory.citiesForIds(DefaultWidgetCityIds).take(2)

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.42f
            val center = Offset(size.width / 2f, size.height * 0.42f)
            drawCircle(
                color = accent.copy(alpha = 0.18f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
            drawCircle(
                color = accent.copy(alpha = 0.10f),
                radius = radius * 0.78f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = fraction * 360f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = 3.dp.toPx()),
            )
            val angle = Math.toRadians((fraction * 360f - 90f).toDouble())
            drawLine(
                color = accent,
                start = center,
                end = Offset(
                    center.x + (radius * 0.72f * cos(angle)).toFloat(),
                    center.y + (radius * 0.72f * sin(angle)).toFloat(),
                ),
                strokeWidth = 2.dp.toPx(),
            )
            drawCircle(color = accent, radius = 4.dp.toPx(), center = center)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                text = CityTimeFormatter.timeText(zoned.toLocalTime(), use24Hour),
                style = ClockwiseTextStyles.CityTime,
                color = ClockwiseColors.Foreground,
            )
            Text(
                text = zoned.dayOfWeek.name.take(3),
                style = ClockwiseTextStyles.Eyebrow,
                color = ClockwiseColors.MutedForeground,
            )
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                cities.forEach { city ->
                    val time = CityTimeFormatter.snapshot(
                        ZoneId.of(city.timeZone),
                        now,
                        use24Hour,
                    )
                    Column {
                        Text(
                            text = city.name,
                            style = ClockwiseTextStyles.DstCaption,
                            color = ClockwiseColors.MutedForeground,
                        )
                        Text(
                            text = time.timeText,
                            style = ClockwiseTextStyles.CityWeekday,
                            color = ClockwiseColors.Foreground,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalClocksPreview(now: Instant, use24Hour: Boolean, compact: Boolean) {
    val deviceZone = ZoneId.systemDefault()
    val deviceTime = CityTimeFormatter.snapshot(deviceZone, now, use24Hour)
    val cities = CityDirectory.citiesForIds(DefaultWidgetCityIds)

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.studio_widget_world_title),
                style = ClockwiseTextStyles.Eyebrow,
                color = ClockwiseColors.MutedForeground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = deviceTime.timeText,
                style = ClockwiseTextStyles.CityWeekday,
                color = ClockwiseColors.Foreground,
            )
        }
        Spacer(Modifier.height(6.dp))
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ClockwiseColors.Border),
        )
        Spacer(Modifier.height(2.dp))
        cities.forEach { city ->
            val time = CityTimeFormatter.snapshot(ZoneId.of(city.timeZone), now, use24Hour)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (compact) 2.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = city.name,
                    style = ClockwiseTextStyles.CityName.copy(fontSize = 13.sp),
                    color = ClockwiseColors.Foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = time.timeText,
                    style = ClockwiseTextStyles.CityTime.copy(fontSize = if (compact) 17.sp else 20.sp),
                    color = ClockwiseColors.Foreground,
                )
            }
        }
    }
}

/**
 * The FOREX card, drawn exactly as the Glance widget draws it: one row per
 * financial centre, an open/closed dot computed from that centre's own zone,
 * and the local time on the right.
 */
@Composable
private fun MarketSessionsPreview(now: Instant, use24Hour: Boolean, compact: Boolean) {
    val open = Color(0xFF2E7D32)
    val closed = ClockwiseColors.MutedForeground
    val deviceTime = CityTimeFormatter.snapshot(ZoneId.systemDefault(), now, use24Hour)

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.studio_widget_market_title),
                style = ClockwiseTextStyles.Eyebrow,
                color = ClockwiseColors.MutedForeground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = deviceTime.timeText,
                style = ClockwiseTextStyles.CityWeekday,
                color = ClockwiseColors.Foreground,
            )
        }
        Spacer(Modifier.height(6.dp))
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ClockwiseColors.Border),
        )
        Spacer(Modifier.height(2.dp))
        MarketSessions.cityIds.forEach { id ->
            val city = CityDirectory.find(id) ?: return@forEach
            val zone = ZoneId.of(city.timeZone)
            val running = MarketSessions.isOpen(zone, now)
            val time = CityTimeFormatter.snapshot(zone, now, use24Hour)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (compact) 2.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (running) open else closed),
                ) { }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = city.name,
                    style = ClockwiseTextStyles.CityName.copy(fontSize = 13.sp),
                    color = ClockwiseColors.Foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        if (running) R.string.market_status_open
                        else R.string.market_status_closed,
                    ),
                    style = ClockwiseTextStyles.DstCaption,
                    color = if (running) open else closed,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = time.timeText,
                    style = ClockwiseTextStyles.CityTime.copy(fontSize = if (compact) 17.sp else 20.sp),
                    color = ClockwiseColors.Foreground,
                )
            }
        }
    }
}

@Composable
private fun AmbientGridPreview(now: Instant, use24Hour: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val cities = CityDirectory.citiesForIds(DefaultWidgetCityIds)
    val deviceTime = CityTimeFormatter.snapshot(ZoneId.systemDefault(), now, use24Hour)

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val ringCenter = Offset(size.width * 0.84f, size.height * 0.16f)
            listOf(0.52f, 0.38f, 0.24f).forEachIndexed { index, fraction ->
                drawCircle(
                    color = accent.copy(alpha = 0.10f - index * 0.02f),
                    radius = size.minDimension * fraction,
                    center = ringCenter,
                    style = Stroke(width = (2f - index * 0.4f).dp.toPx()),
                )
            }
            drawArc(
                color = accent.copy(alpha = 0.16f),
                startAngle = 200f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(-size.width * 0.2f, size.height * 0.56f),
                size = Size(size.width * 0.7f, size.height * 0.7f),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.studio_widget_ambient_title),
                    style = ClockwiseTextStyles.Eyebrow,
                    color = ClockwiseColors.MutedForeground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = deviceTime.timeText,
                    style = ClockwiseTextStyles.DstCaption,
                    color = ClockwiseColors.MutedForeground,
                )
            }
            Spacer(Modifier.weight(1f))
            cities.chunked(2).forEachIndexed { index, pair ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    pair.forEach { city ->
                        val time = CityTimeFormatter.snapshot(
                            ZoneId.of(city.timeZone),
                            now,
                            use24Hour,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = city.name,
                                style = ClockwiseTextStyles.DstCaption,
                                color = ClockwiseColors.MutedForeground,
                                maxLines = 1,
                            )
                            Text(
                                text = time.timeText,
                                style = ClockwiseTextStyles.CityName.copy(fontSize = 19.sp),
                                color = ClockwiseColors.Foreground,
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
