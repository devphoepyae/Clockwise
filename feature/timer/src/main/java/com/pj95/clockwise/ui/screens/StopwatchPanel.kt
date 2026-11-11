package com.pj95.clockwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ui.StopwatchFormat
import com.pj95.clockwise.ui.StopwatchUiState
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles

/** Test tags for the stopwatch section. */
object StopwatchTestTags {
    const val Display = "stopwatchDisplay"
    const val Toggle = "stopwatchToggle"
    const val Reset = "stopwatchReset"
}

/**
 * The stopwatch: one large elapsed time, a start/pause control and a reset.
 *
 * It is deliberately quieter than the timer. A stopwatch is looked at after the
 * fact, so there is no ring and no progress arc — just the number and the two
 * controls that change it.
 */
@Composable
fun StopwatchPanel(
    state: StopwatchUiState,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val display = StopwatchFormat.clock(state.elapsedMillis)
    val displayDescription = stringResource(R.string.a11y_stopwatch_elapsed, display)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.stopwatch_section_label),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = display,
            style = ClockwiseTextStyles.TimerClock,
            color = ClockwiseColors.Foreground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(StopwatchTestTags.Display)
                .semantics {
                    contentDescription = displayDescription
                    heading()
                },
        )

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .testTag(StopwatchTestTags.Toggle)
                    .clip(ClockwiseShapes.medium)
                    .background(ClockwiseColors.Primary)
                    .border(1.dp, ClockwiseColors.Primary, ClockwiseShapes.medium)
                    .clickable(
                        onClickLabel = stringResource(
                            if (state.running) R.string.stopwatch_pause else R.string.stopwatch_start,
                        ),
                        role = Role.Button,
                        onClick = onToggle,
                    )
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (state.running) R.string.stopwatch_pause else R.string.stopwatch_start,
                    ),
                    style = ClockwiseTextStyles.SectionLabel,
                    color = ClockwiseColors.PrimaryForeground,
                )
            }

            Spacer(Modifier.width(12.dp))

            CircleIconButton(
                iconRes = R.drawable.ic_refresh,
                contentDescription = stringResource(R.string.stopwatch_reset),
                onClick = onReset,
                size = 52.dp,
                iconSize = 22.dp,
                modifier = Modifier.testTag(StopwatchTestTags.Reset),
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.stopwatch_hint),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
