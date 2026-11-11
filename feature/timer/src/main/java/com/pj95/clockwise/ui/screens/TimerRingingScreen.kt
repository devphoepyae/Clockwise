package com.pj95.clockwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.timer.CountdownTimer
import com.pj95.clockwise.model.timer.TimerFormat
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles

/**
 * The screen a finished timer shows, over the lock screen.
 *
 * It mirrors the alarm's ringing screen — one line of type and two large
 * choices — because it is read in the same half-awake state. "Start again" uses
 * the stored duration, so a repeating five-minute tea timer is one tap.
 */
@Composable
fun TimerRingingScreen(
    timer: CountdownTimer?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClockwiseColors.Background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.timer_ringing_eyebrow),
            style = ClockwiseTextStyles.Eyebrow,
            color = ClockwiseColors.MutedForeground,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.timer_finished_title),
            style = ClockwiseTextStyles.DetailClock,
            color = ClockwiseColors.Foreground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.height(8.dp))

        if (timer != null) {
            Text(
                text = timer.label.ifBlank {
                    stringResource(R.string.timer_finished_body, TimerFormat.summary(timer.durationMillis))
                },
                style = ClockwiseTextStyles.DetailCityName,
                color = ClockwiseColors.Foreground,
                textAlign = TextAlign.Center,
            )
        } else if (isLoading) {
            Spacer(Modifier.height(28.dp))
        }

        Spacer(Modifier.weight(1f))

        TimerRingingButton(
            text = stringResource(R.string.timer_restart),
            onClick = onRestart,
            filled = false,
        )

        Spacer(Modifier.height(12.dp))

        TimerRingingButton(
            text = stringResource(R.string.timer_dismiss),
            onClick = onDismiss,
            filled = true,
        )
    }
}

@Composable
private fun TimerRingingButton(
    text: String,
    onClick: () -> Unit,
    filled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(ClockwiseShapes.medium)
            .background(if (filled) ClockwiseColors.Primary else ClockwiseColors.Card)
            .border(
                width = 1.dp,
                color = if (filled) ClockwiseColors.Primary else ClockwiseColors.Border,
                shape = ClockwiseShapes.medium,
            )
            .clickable(onClickLabel = text, role = Role.Button, onClick = onClick)
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = ClockwiseTextStyles.SectionLabel,
            color = if (filled) {
                ClockwiseColors.PrimaryForeground
            } else {
                ClockwiseColors.Foreground
            },
        )
    }
}
