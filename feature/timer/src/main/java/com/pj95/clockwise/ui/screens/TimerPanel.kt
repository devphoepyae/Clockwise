package com.pj95.clockwise.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.timer.CountdownTimer
import com.pj95.clockwise.model.timer.TimerFormat
import com.pj95.clockwise.ui.TimerUiState
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.components.TimerDurationPicker
import com.pj95.clockwise.ui.components.TimerPresets
import com.pj95.clockwise.ui.components.TimerTestTags
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles

/**
 * "Timer" — the second half of the Alarms tab.
 *
 * Setup and countdown are the same screen rather than two destinations: a timer
 * is a mode of the alarm tab. The countdown is the largest thing on screen while
 * it runs, because it is the only thing worth reading from across a room.
 *
 * The idle screen is deliberately quiet: the chosen duration sits in the middle,
 * four presets sit under it, and the wheels only appear when the keyboard button
 * asks for them. That is the reference layout, and it keeps the Start button
 * above the fold on a 360 dp phone.
 *
 * Every action is a callback: the panel has no repository of its own, so the
 * same composable can be rendered in a preview or a screenshot test.
 */
@Composable
fun TimerPanel(
    state: TimerUiState,
    onStart: (durationMillis: Long) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    onAllowExactAlarms: () -> Unit = {},
    onExtend: (deltaMillis: Long) -> Unit = {},
    onRestart: () -> Unit = {},
) {
    var hours by rememberSaveable { mutableIntStateOf(0) }
    var minutes by rememberSaveable { mutableIntStateOf(5) }
    var seconds by rememberSaveable { mutableIntStateOf(0) }

    val mode = when {
        state.isLoading -> TimerMode.Loading
        state.isFinished -> TimerMode.Finished
        state.isRunning || state.isPaused -> TimerMode.Active
        else -> TimerMode.Setup
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.timer_section_label),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))

        Crossfade(targetState = mode, label = "timerMode") { current ->
            when (current) {
                TimerMode.Loading -> TimerLoading()
                TimerMode.Setup -> TimerSetup(
                    hours = hours,
                    minutes = minutes,
                    seconds = seconds,
                    onHoursChange = { hours = it },
                    onMinutesChange = { minutes = it },
                    onSecondsChange = { seconds = it },
                    onPreset = { presetMinutes ->
                        hours = presetMinutes / 60
                        minutes = presetMinutes % 60
                        seconds = 0
                    },
                    onStart = {
                        val duration = ((hours * 60L + minutes) * 60L + seconds) * 1_000L
                        if (duration > 0) onStart(duration)
                    },
                )

                TimerMode.Active -> TimerActive(
                    state = state,
                    onPause = onPause,
                    onResume = onResume,
                    onReset = onReset,
                    onExtend = onExtend,
                )

                TimerMode.Finished -> TimerFinished(
                    timer = state.timer,
                    onReset = onReset,
                    onRestart = onRestart,
                )
            }
        }

        if (!state.exactAlarmsAllowed && mode != TimerMode.Loading) {
            Spacer(Modifier.height(16.dp))
            ExactTimerNotice(onAllow = onAllowExactAlarms)
        }
    }
}

private enum class TimerMode { Loading, Setup, Active, Finished }

@Composable
private fun TimerSetup(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    onPreset: (Int) -> Unit,
    onStart: () -> Unit,
) {
    // Hidden by default: the duration is readable at a glance and the wheels
    // are one tap away when it needs changing.
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = setupClock(hours, minutes, seconds),
                style = ClockwiseTextStyles.TimerClock,
                color = ClockwiseColors.Foreground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .testTag(TimerTestTags.DurationDisplay)
                    .semantics { heading() },
            )
            Spacer(Modifier.width(8.dp))
            CircleIconButton(
                iconRes = R.drawable.ic_keyboard,
                contentDescription = stringResource(R.string.timer_edit_duration),
                onClick = { showPicker = !showPicker },
                size = 44.dp,
                iconSize = 20.dp,
                modifier = Modifier.testTag(TimerTestTags.PickerToggle),
            )
        }

        AnimatedVisibility(visible = showPicker) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(8.dp))
                TimerDurationPicker(
                    hours = hours,
                    minutes = minutes,
                    seconds = seconds,
                    onHoursChange = onHoursChange,
                    onMinutesChange = onMinutesChange,
                    onSecondsChange = onSecondsChange,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        TimerPresets(onPresetSelected = onPreset)

        Spacer(Modifier.height(24.dp))
        TimerButton(
            text = stringResource(R.string.timer_start),
            onClick = onStart,
            filled = true,
            testTag = TimerTestTags.Start,
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.timer_screen_off_hint),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TimerTestTags.ScreenOffHint),
        )
    }
}

/** "05:00" or "1:05:00" — hours only when the user chose one. */
private fun setupClock(hours: Int, minutes: Int, seconds: Int): String =
    if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }

@Composable
private fun TimerActive(
    state: TimerUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onExtend: (Long) -> Unit,
) {
    val timer = state.timer ?: return
    val status = if (state.isRunning) {
        stringResource(R.string.timer_remaining)
    } else {
        stringResource(R.string.timer_paused)
    }
    val display = TimerFormat.clock(state.remainingMillis)
    val displayDescription = stringResource(R.string.a11y_timer_remaining, display)
    val fraction = if (timer.durationMillis <= 0) {
        0f
    } else {
        (1f - state.remainingMillis.toFloat() / timer.durationMillis.toFloat()).coerceIn(0f, 1f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = status,
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
        )

        Spacer(Modifier.height(10.dp))
        TimerClockRing(
            display = display,
            displayDescription = displayDescription,
            fraction = fraction,
        )

        Spacer(Modifier.height(18.dp))
        ActiveTimerControls(onExtend = onExtend, onReset = onReset)

        Spacer(Modifier.height(18.dp))
        if (state.isRunning) {
            TimerButton(
                text = stringResource(R.string.timer_pause),
                onClick = onPause,
                filled = true,
                testTag = TimerTestTags.Pause,
            )
        } else {
            TimerButton(
                text = stringResource(R.string.timer_resume),
                onClick = onResume,
                filled = true,
                testTag = TimerTestTags.Resume,
            )
        }
    }
}

/**
 * The two controls beside a running countdown: one more minute, and the reset
 * icon that ends this timer. Both sit in a row under the ring, matching the
 * reference layout, and the reset is an icon so it cannot be confused with the
 * filled pause button.
 */
@Composable
private fun ActiveTimerControls(
    onExtend: (Long) -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("${TimerTestTags.AddTime}_1")
                .clip(ClockwiseShapes.medium)
                .background(ClockwiseColors.Card)
                .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
                .clickable(
                    onClickLabel = stringResource(R.string.a11y_timer_add_minutes, 1),
                    role = Role.Button,
                ) { onExtend(60_000L) }
                .padding(horizontal = 22.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.timer_add_one_minute),
                style = ClockwiseTextStyles.SectionLabel,
                color = ClockwiseColors.Foreground,
            )
        }

        Spacer(Modifier.width(16.dp))

        CircleIconButton(
            iconRes = R.drawable.ic_refresh,
            contentDescription = stringResource(R.string.timer_cancel),
            onClick = onReset,
            size = 48.dp,
            iconSize = 22.dp,
            modifier = Modifier.testTag(TimerTestTags.Cancel),
        )
    }
}

@Composable
private fun TimerFinished(
    timer: CountdownTimer?,
    onReset: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.timer_finished_title),
            style = ClockwiseTextStyles.TimerClock,
            color = ClockwiseColors.Foreground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.timer_finished_hint),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (timer != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = TimerFormat.summary(timer.durationMillis),
                style = ClockwiseTextStyles.MapCityOffset,
                color = ClockwiseColors.MutedForeground,
            )
        }
        Spacer(Modifier.height(20.dp))
        TimerButton(
            text = stringResource(R.string.timer_restart),
            onClick = onRestart,
            filled = true,
            testTag = TimerTestTags.Restart,
        )
        Spacer(Modifier.height(10.dp))
        TimerButton(
            text = stringResource(R.string.timer_reset),
            onClick = onReset,
            filled = false,
            testTag = TimerTestTags.Reset,
        )
    }
}

/**
 * The countdown inside a ring that empties as time passes.
 *
 * The arc is drawn by hand for the same reason the old progress line was: the
 * app's track is a single hairline and the fill is exactly the foreground
 * colour, without Material's stop indicator. The sweep is animated over one
 * tick so the quarter-second updates read as a smooth sweep rather than steps.
 */
@Composable
private fun TimerClockRing(
    display: String,
    displayDescription: String,
    fraction: Float,
) {
    val sweep by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 260, easing = LinearEasing),
        label = "timerRing",
    )
    val trackColor = ClockwiseColors.Secondary
    val fillColor = ClockwiseColors.Foreground

    Box(
        modifier = Modifier.size(224.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (sweep > 0f) {
                drawArc(
                    color = fillColor,
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        Text(
            text = display,
            style = ClockwiseTextStyles.TimerClock,
            color = ClockwiseColors.Foreground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .testTag(TimerTestTags.Display)
                .semantics {
                    contentDescription = displayDescription
                    heading()
                },
        )
    }
}

@Composable
private fun TimerButton(
    text: String,
    onClick: () -> Unit,
    filled: Boolean,
    testTag: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(testTag)
            .clip(ClockwiseShapes.medium)
            .background(if (filled) ClockwiseColors.Primary else ClockwiseColors.Card)
            .border(
                width = 1.dp,
                color = if (filled) ClockwiseColors.Primary else ClockwiseColors.Border,
                shape = ClockwiseShapes.medium,
            )
            .clickable(onClickLabel = text, role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = ClockwiseTextStyles.SectionLabel,
            color = if (filled) ClockwiseColors.PrimaryForeground else ClockwiseColors.Foreground,
        )
    }
}

@Composable
private fun TimerLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.timer_loading),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
        )
    }
}

@Composable
private fun ExactTimerNotice(onAllow: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Accent)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .clickable(onClick = onAllow)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.timer_exact_hint),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.weight(1f),
        )
    }
}
