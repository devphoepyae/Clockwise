package com.pj95.clockwise.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The in-app desk clock, shown full screen from Settings.
 *
 * Pure black with no chrome: on an OLED phone this is the least light the
 * screen can emit, and there are no bars or buttons to burn in. The whole
 * surface is the exit target, so a tap anywhere — or Back — returns to the app.
 *
 * The block of type drifts between five positions once a minute. A moving image
 * cannot leave a fixed ghost, which is the point of a screen saver; the travel
 * is small enough that it reads as still.
 */
@Composable
fun ScreenSaverScreen(
    now: Instant,
    use24Hour: Boolean,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoned = remember(now) { now.atZone(ZoneId.systemDefault()) }
    var position by remember { mutableIntStateOf(0) }

    // A minute per position. Long enough to look static, short enough that no
    // pixel holds the same glyph for long.
    LaunchedEffect(Unit) {
        while (true) {
            delay(BURN_IN_INTERVAL_MILLIS)
            position = (position + 1) % BURN_IN_OFFSETS.size
        }
    }

    val (targetX, targetY) = BURN_IN_OFFSETS[position]
    val offsetX by animateDpAsState(targetValue = targetX.dp, label = "screenSaverX")
    val offsetY by animateDpAsState(targetValue = targetY.dp, label = "screenSaverY")

    val exitLabel = stringResource(R.string.screen_saver_exit_hint)
    val screenLabel = stringResource(R.string.a11y_screen_saver)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = exitLabel,
                role = Role.Button,
                onClick = onExit,
            )
            .semantics { contentDescription = screenLabel },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                // The lambda overload defers the dp→px read to layout, so the
                // drift does not re-measure on every animation frame.
                .offset { IntOffset(offsetX.roundToPx(), offsetY.roundToPx()) }
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = CityTimeFormatter.timeText(zoned.toLocalTime(), use24Hour),
                    style = ClockwiseTextStyles.ScreenSaverClock,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = zoned.format(DATE_FORMAT),
                style = ClockwiseTextStyles.ScreenSaverDate,
                color = Color(0xFFB3B3B3),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = exitLabel,
                style = ClockwiseTextStyles.DstCaption,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The five resting places. The centre is included so the first frame is dead
 * centre and the movement begins after a full minute.
 */
private val BURN_IN_OFFSETS = listOf(
    0 to 0,
    10 to 8,
    -10 to 8,
    10 to -8,
    -10 to -8,
)

private const val BURN_IN_INTERVAL_MILLIS = 60_000L

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

@Preview(name = "Screen saver 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ScreenSaverScreenPreview() {
    Box(modifier = Modifier.background(Color.Black)) {
        ScreenSaverScreen(
            now = Instant.parse("2026-10-28T06:30:00Z"),
            use24Hour = true,
            onExit = {},
        )
    }
}
