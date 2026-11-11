package com.pj95.clockwise.ui.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity as startActivityWithIntent
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pj95.clockwise.MainActivity
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.widget.WidgetStyleId

/** One city line on a Studio widget. */
data class ChronosWidgetCity(
    val id: String,
    val name: String,
    val timeText: String,
    val weekday: String,
    val isAnotherDay: Boolean,
)

/** One financial centre on the market-session card. */
data class ChronosMarketSession(
    val id: String,
    val name: String,
    val timeText: String,
    val isOpen: Boolean,
)

/**
 * A frozen snapshot of everything a Studio widget draws.
 *
 * Composition does no I/O and no clock reads: the provider loads this once and
 * Glance renders it, exactly like the classic widget's [com.pj95.clockwise.ui.WidgetUiState].
 */
data class ChronosWidgetState(
    val style: WidgetStyleId,
    val deviceTimeText: String,
    val deviceWeekday: String,
    val cities: List<ChronosWidgetCity>,
    val isLocked: Boolean,
    val accentArgb: Int,
    val isDark: Boolean,
    val dialArt: Bitmap? = null,
    val ambientArt: Bitmap? = null,
    val markets: List<ChronosMarketSession> = emptyList(),
)

/**
 * The four Chronos Studio cards, drawn with Glance.
 *
 * Each style is a different answer to "what belongs on a home screen": the free
 * slate is a clock with two shortcuts, the 24-hour dial is a single day at a
 * glance, the global-clocks card is three cities in a list, and the ambient grid
 * is a quiet 2x2 of places. They share one card shell, one type scale and one
 * accent, so switching styles changes the composition rather than the app.
 */
@Composable
@GlanceComposable
fun ChronosWidgetContent(
    state: ChronosWidgetState,
    modifier: GlanceModifier = GlanceModifier,
) {
    val shell = modifier
        .fillMaxSize()
        .appWidgetBackground()
        .background(ImageProvider(R.drawable.widget_surface))
        .cornerRadius(24.dp)

    when {
        state.isLocked -> LockedCard(state, shell)
        state.style == WidgetStyleId.FREE_SLATE_MULTIFUNCTION -> SlateCard(state, shell)
        state.style == WidgetStyleId.FREE_DIAL_24H -> DialCard(state, shell)
        state.style == WidgetStyleId.FREE_MARKET_SESSIONS -> MarketSessionsCard(state, shell)
        state.style == WidgetStyleId.PREMIUM_GLOBAL_CLOCKS -> GlobalClocksCard(state, shell)
        else -> AmbientGridCard(state, shell)
    }
}

@Composable
@GlanceComposable
private fun LockedCard(state: ChronosWidgetState, modifier: GlanceModifier) {
    val context = LocalContext.current
    val accent = ColorProvider(Color(state.accentArgb))

    Column(
        modifier = modifier
            .padding(20.dp)
            .clickable(startActivityWithIntent(studioIntent(context))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_lock),
            contentDescription = null,
            modifier = GlanceModifier.size(22.dp),
        )
        Spacer(GlanceModifier.height(8.dp))
        Text(
            text = context.getString(R.string.studio_widget_locked_eyebrow),
            style = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = ChronosWidgetColors.Supporting,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = context.getString(R.string.studio_widget_locked_title),
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ChronosWidgetColors.Content,
            ),
            maxLines = 2,
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = context.getString(R.string.studio_widget_locked_action),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = accent,
            ),
            maxLines = 1,
        )
    }
}

@Composable
@GlanceComposable
private fun SlateCard(state: ChronosWidgetState, modifier: GlanceModifier) {
    val context = LocalContext.current
    val compact = LocalSize.current.height < 150.dp

    Column(modifier = modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.studio_widget_slate_title),
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChronosWidgetColors.Supporting,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = state.deviceWeekday.uppercase(),
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = ChronosWidgetColors.Supporting,
                ),
                maxLines = 1,
            )
        }

        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = state.deviceTimeText,
            style = TextStyle(
                fontSize = if (compact) 38.sp else 52.sp,
                fontWeight = FontWeight.Bold,
                color = ChronosWidgetColors.Content,
            ),
            maxLines = 1,
        )

        Spacer(GlanceModifier.defaultWeight())

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            ShortcutPill(
                iconRes = R.drawable.ic_timer,
                label = context.getString(R.string.studio_widget_timer),
                intent = timerIntent(context),
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(GlanceModifier.width(8.dp))
            ShortcutPill(
                iconRes = R.drawable.ic_stopwatch,
                label = context.getString(R.string.studio_widget_stopwatch),
                intent = timerIntent(context),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

@Composable
@GlanceComposable
private fun ShortcutPill(
    iconRes: Int,
    label: String,
    intent: Intent,
    modifier: GlanceModifier,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .background(ChronosWidgetColors.Pill)
            .cornerRadius(20.dp)
            .clickable(startActivityWithIntent(intent))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = null,
            modifier = GlanceModifier.size(16.dp),
        )
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = label,
            style = TextStyle(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = ChronosWidgetColors.Content,
            ),
            maxLines = 1,
        )
    }
}

@Composable
@GlanceComposable
private fun DialCard(state: ChronosWidgetState, modifier: GlanceModifier) {
    Box(modifier = modifier) {
        state.dialArt?.let { art ->
            Image(
                provider = ImageProvider(art),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = state.deviceTimeText,
                style = TextStyle(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChronosWidgetColors.Content,
                ),
                maxLines = 1,
            )
            Text(
                text = state.deviceWeekday.uppercase(),
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = ChronosWidgetColors.Supporting,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.defaultWeight())

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                state.cities.take(2).forEach { city ->
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = city.name,
                            style = TextStyle(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = ChronosWidgetColors.Supporting,
                            ),
                            maxLines = 1,
                        )
                        Text(
                            text = city.timeText,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = ChronosWidgetColors.Content,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@GlanceComposable
private fun MarketSessionsCard(state: ChronosWidgetState, modifier: GlanceModifier) {
    val context = LocalContext.current

    Column(modifier = modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.studio_widget_market_title),
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChronosWidgetColors.Supporting,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = state.deviceTimeText,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = ChronosWidgetColors.Content,
                ),
                maxLines = 1,
            )
        }

        Spacer(GlanceModifier.height(6.dp))
        Spacer(
            GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ChronosWidgetColors.Hairline),
        )
        Spacer(GlanceModifier.height(2.dp))

        state.markets.forEach { market ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable(startActivityWithIntent(cityDetailIntent(context, market.id))),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(8.dp)
                        .background(
                            if (market.isOpen) {
                                ChronosWidgetColors.MarketOpen
                            } else {
                                ChronosWidgetColors.Supporting
                            },
                        )
                        .cornerRadius(4.dp),
                ) { }
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = market.name,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ChronosWidgetColors.Content,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = context.getString(
                        if (market.isOpen) R.string.market_status_open
                        else R.string.market_status_closed,
                    ),
                    style = TextStyle(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (market.isOpen) {
                            ChronosWidgetColors.MarketOpen
                        } else {
                            ChronosWidgetColors.Supporting
                        },
                    ),
                    maxLines = 1,
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = market.timeText,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosWidgetColors.Content,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
@GlanceComposable
private fun GlobalClocksCard(state: ChronosWidgetState, modifier: GlanceModifier) {
    val context = LocalContext.current

    Column(modifier = modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.studio_widget_world_title),
                style = TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChronosWidgetColors.Supporting,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = state.deviceTimeText,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = ChronosWidgetColors.Content,
                ),
                maxLines = 1,
            )
        }

        Spacer(GlanceModifier.height(8.dp))
        Spacer(
            GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ChronosWidgetColors.Hairline),
        )
        Spacer(GlanceModifier.height(4.dp))

        state.cities.take(3).forEach { city ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable(startActivityWithIntent(cityDetailIntent(context, city.id))),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = city.name,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = ChronosWidgetColors.Content,
                        ),
                        maxLines = 1,
                    )
                    if (city.isAnotherDay) {
                        Text(
                            text = city.weekday.uppercase(),
                            style = TextStyle(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Medium,
                                color = ChronosWidgetColors.Supporting,
                            ),
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = city.timeText,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosWidgetColors.Content,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
@GlanceComposable
private fun AmbientGridCard(state: ChronosWidgetState, modifier: GlanceModifier) {
    val context = LocalContext.current

    Box(modifier = modifier) {
        state.ambientArt?.let { art ->
            Image(
                provider = ImageProvider(art),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }

        Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = context.getString(R.string.studio_widget_ambient_title),
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChronosWidgetColors.Supporting,
                    ),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Text(
                    text = state.deviceTimeText,
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = ChronosWidgetColors.Supporting,
                    ),
                    maxLines = 1,
                )
            }

            Spacer(GlanceModifier.defaultWeight())

            state.cities.chunked(2).take(2).forEachIndexed { index, pair ->
                if (index > 0) Spacer(GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    pair.forEach { city ->
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .clickable(
                                    startActivityWithIntent(cityDetailIntent(context, city.id)),
                                ),
                        ) {
                            Text(
                                text = city.name,
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ChronosWidgetColors.Supporting,
                                ),
                                maxLines = 1,
                            )
                            Text(
                                text = city.timeText,
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChronosWidgetColors.Content,
                                ),
                                maxLines = 1,
                            )
                        }
                    }
                    // Keeps a three-city configuration on a four-cell grid even,
                    // rather than stretching the last tile across the row.
                    if (pair.size == 1) Spacer(GlanceModifier.defaultWeight())
                }
            }
        }
    }
}

/** Opens the countdown timer inside the Alarms tab. */
internal fun timerIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_TIMER, true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

/** Opens the Studio gallery from a locked widget. */
internal fun studioIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_STUDIO, true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

/**
 * The card's own tokens.
 *
 * A widget belongs to the launcher, so these are day/night pairs resolved from
 * the device's configuration rather than the app's stored theme.
 */
private object ChronosWidgetColors {
    val Content = DayNightColorProvider(day = Color(0xFF121212), night = Color(0xFFF4F4F2))
    val Supporting = DayNightColorProvider(day = Color(0xFF5F5F5D), night = Color(0xFFB8B8B4))
    val Hairline = DayNightColorProvider(day = Color(0x1F121212), night = Color(0x33F4F4F2))
    val Pill = DayNightColorProvider(day = Color(0x14121212), night = Color(0x1FF4F4F2))
    val MarketOpen = DayNightColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF66BB6A))
}
