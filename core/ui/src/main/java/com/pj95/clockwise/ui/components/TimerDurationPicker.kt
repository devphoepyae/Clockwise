package com.pj95.clockwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles

/** Test tags for the timer's duration picker and controls. */
object TimerTestTags {
    const val HoursWheel = "timerHoursWheel"
    const val MinutesWheel = "timerMinutesWheel"
    const val SecondsWheel = "timerSecondsWheel"
    const val Start = "timerStart"
    const val Pause = "timerPause"
    const val Resume = "timerResume"
    const val Cancel = "timerCancel"
    const val Reset = "timerReset"
    const val Restart = "timerRestart"
    const val AddTime = "timerAddTime"
    const val Display = "timerDisplay"
    const val Preset = "timerPreset"
    const val PickerToggle = "timerPickerToggle"
    const val DurationDisplay = "timerDurationDisplay"
    const val ScreenOffHint = "timerScreenOffHint"
}

/**
 * Three wheels — hours, minutes, seconds — in the same shape as the alarm time
 * picker.
 *
 * A wheel cannot produce an invalid duration and needs one gesture, which is
 * why there is no keypad. Hours stop at 23: a timer longer than a day is not
 * what this tab is for, and the extra numbers would only make the column harder
 * to land on.
 */
@Composable
fun TimerDurationPicker(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            DurationWheel(
                values = (0..23).toList(),
                selected = hours,
                label = stringResource(R.string.timer_hours),
                onSelected = onHoursChange,
                testTag = TimerTestTags.HoursWheel,
                modifier = Modifier.weight(1f),
            )
            WheelSeparator()
            DurationWheel(
                values = (0..59).toList(),
                selected = minutes,
                label = stringResource(R.string.timer_minutes),
                onSelected = onMinutesChange,
                testTag = TimerTestTags.MinutesWheel,
                modifier = Modifier.weight(1f),
            )
            WheelSeparator()
            DurationWheel(
                values = (0..59).toList(),
                selected = seconds,
                label = stringResource(R.string.timer_seconds),
                onSelected = onSecondsChange,
                testTag = TimerTestTags.SecondsWheel,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WheelCaption(stringResource(R.string.timer_hours_short), Modifier.weight(1f))
            WheelCaption(stringResource(R.string.timer_minutes_short), Modifier.weight(1f))
            WheelCaption(stringResource(R.string.timer_seconds_short), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DurationWheel(
    values: List<Int>,
    selected: Int,
    label: String,
    onSelected: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Wheel(
        values = values,
        selectedValue = selected,
        label = label,
        format = ::twoDigits,
        onSelected = onSelected,
        testTag = testTag,
        modifier = modifier,
    )
}

@Composable
private fun WheelSeparator() {
    Text(
        text = ":",
        style = ClockwiseTextStyles.WheelNumber,
        color = ClockwiseColors.MutedForeground,
        modifier = Modifier.padding(horizontal = 2.dp),
    )
}

@Composable
private fun WheelCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = ClockwiseTextStyles.SectionLabel,
        color = ClockwiseColors.MutedForeground,
        modifier = modifier,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

/**
 * One-tap durations. The four people reach for most, evenly spaced so the row
 * never needs a sideways scroll on a 360 dp screen.
 */
@Composable
fun TimerPresets(
    onPresetSelected: (minutes: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PRESET_MINUTES.forEach { minutes ->
            val label = if (minutes >= MINUTES_PER_HOUR) {
                val hours = minutes / MINUTES_PER_HOUR
                stringResource(R.string.timer_preset_hours, hours)
            } else {
                stringResource(R.string.timer_preset_minutes, minutes)
            }
            Text(
                text = label,
                style = ClockwiseTextStyles.SectionLabel,
                color = ClockwiseColors.Foreground,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${TimerTestTags.Preset}_$minutes")
                    .heightIn(min = 44.dp)
                    .clip(ClockwiseShapes.small)
                    .background(ClockwiseColors.Card)
                    .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.small)
                    .clickable(role = Role.Button) { onPresetSelected(minutes) }
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

private const val MINUTES_PER_HOUR = 60

/** Durations people actually pick, in minutes. */
private val PRESET_MINUTES = listOf(5, 10, 15, 30)
