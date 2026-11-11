package com.pj95.clockwise.ui.components

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerColors
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles

/** Which half of the time the keypad is editing. */
private enum class TimeField { Hour, Minute }

/**
 * Clockwise's Google-Clock-style time picker.
 *
 * Two ways to choose the same time, and one small control between them:
 *
 *  - **The dial.** Material 3's clock face, painted in the app's own palette.
 *    Tapping the hour in the digital preview switches the dial to minutes and
 *    back, exactly as the reference design does, with the platform's built-in
 *    transition.
 *  - **The keypad.** A direct number pad, revealed by the keyboard button at the
 *    bottom-left. It is deliberately hidden by default: the dial cannot produce
 *    an invalid time, and most people never need to type one.
 *
 * The picker owns no persistent state. It reports every change through
 * [onHourChange] (0–23) and [onMinuteChange] (0–59), so the editor's save path
 * and the card behind it always agree with the dial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockwiseTimePicker(
    hour: Int,
    minute: Int,
    use24Hour: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var keyboardMode by rememberSaveable { mutableStateOf(false) }
    var activeField by rememberSaveable { mutableStateOf(TimeField.Hour) }

    val dialState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = use24Hour,
    )

    // The dial is the source while it is visible; the keypad is the source while
    // that is. Both push through the same two callbacks, so there is only one
    // value of "the time" in the editor.
    LaunchedEffect(dialState.hour, dialState.minute) {
        if (dialState.hour != hour) onHourChange(dialState.hour)
        if (dialState.minute != minute) onMinuteChange(dialState.minute)
    }
    LaunchedEffect(hour, minute) {
        if (dialState.hour != hour) dialState.hour = hour
        if (dialState.minute != minute) dialState.minute = minute
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Crossfade(
            targetState = keyboardMode,
            label = "alarmTimePickerMode",
        ) { typing ->
            if (typing) {
                KeypadTimeEntry(
                    hour = hour,
                    minute = minute,
                    use24Hour = use24Hour,
                    activeField = activeField,
                    onActiveFieldChange = { activeField = it },
                    onHourChange = onHourChange,
                    onMinuteChange = onMinuteChange,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TimePicker(
                        state = dialState,
                        modifier = Modifier.testTag(AlarmTimeTestTags.Dial),
                        colors = dialColors(),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PickerModeButton(
                iconRes = if (keyboardMode) R.drawable.ic_clock else R.drawable.ic_keyboard,
                contentDescription = stringResource(
                    if (keyboardMode) R.string.alarm_time_use_dial else R.string.alarm_time_use_keypad,
                ),
                onClick = { keyboardMode = !keyboardMode },
                modifier = Modifier.testTag(AlarmTimeTestTags.KeypadToggle),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    if (keyboardMode) R.string.alarm_time_use_dial else R.string.alarm_time_use_keypad,
                ),
                style = ClockwiseTextStyles.CityRegion,
                color = ClockwiseColors.MutedForeground,
            )
        }
    }
}

/**
 * Direct entry: two large digit fields above the shared number pad.
 *
 * Tapping a field chooses which half the digits replace. The value is built
 * digit by digit the way a phone keypad works: typing 1 then 2 gives 12, and a
 * third digit drops the oldest. Out-of-range values fall back to the digit just
 * pressed instead of being rejected silently.
 */
@Composable
private fun KeypadTimeEntry(
    hour: Int,
    minute: Int,
    use24Hour: Boolean,
    activeField: TimeField,
    onActiveFieldChange: (TimeField) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
) {
    val isPm = hour >= 12
    val displayHour = if (use24Hour) hour else if (hour % 12 == 0) 12 else hour % 12

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AlarmTimeTestTags.Keypad),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            TimeFieldBox(
                text = if (use24Hour) twoDigits(displayHour) else displayHour.toString(),
                active = activeField == TimeField.Hour,
                description = stringResource(R.string.a11y_alarm_hour_wheel),
                onClick = { onActiveFieldChange(TimeField.Hour) },
                modifier = Modifier.testTag(AlarmTimeTestTags.HourField),
            )
            Text(
                text = ":",
                style = ClockwiseTextStyles.WheelNumber,
                color = ClockwiseColors.MutedForeground,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            TimeFieldBox(
                text = twoDigits(minute),
                active = activeField == TimeField.Minute,
                description = stringResource(R.string.a11y_alarm_minute_wheel),
                onClick = { onActiveFieldChange(TimeField.Minute) },
                modifier = Modifier.testTag(AlarmTimeTestTags.MinuteField),
            )
        }

        if (!use24Hour) {
            Spacer(Modifier.height(8.dp))
            ClockwiseSegmentedControl(
                options = listOf(
                    SegmentedOption(value = false, label = stringResource(R.string.alarm_am)),
                    SegmentedOption(value = true, label = stringResource(R.string.alarm_pm)),
                ),
                selectedValue = isPm,
                onValueSelected = { pm ->
                    onHourChange((displayHour % 12) + if (pm) 12 else 0)
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        NumericKeypad(
            onDigit = { digit ->
                when (activeField) {
                    TimeField.Hour -> {
                        val max = if (use24Hour) 23 else 12
                        val next = (displayHour % 10) * 10 + digit
                        val value = if (next > max) digit else next
                        onHourChange(
                            if (use24Hour) value else (value % 12) + if (isPm) 12 else 0,
                        )
                    }

                    TimeField.Minute -> {
                        val next = (minute % 10) * 10 + digit
                        onMinuteChange(if (next > 59) digit else next)
                    }
                }
            },
            onBackspace = {
                when (activeField) {
                    TimeField.Hour -> {
                        val value = displayHour / 10
                        onHourChange(
                            if (use24Hour) value else (value % 12) + if (isPm) 12 else 0,
                        )
                    }

                    TimeField.Minute -> onMinuteChange(minute / 10)
                }
            },
        )
    }
}

@Composable
private fun TimeFieldBox(
    text: String,
    active: Boolean,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 92.dp, height = 64.dp)
            .clip(ClockwiseShapes.medium)
            .background(if (active) ClockwiseColors.Accent else ClockwiseColors.Card)
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) ClockwiseColors.Primary else ClockwiseColors.Border,
                shape = ClockwiseShapes.medium,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = ClockwiseTextStyles.WheelNumber,
            color = ClockwiseColors.Foreground,
        )
    }
}

/**
 * The platform clock face in the app's colours.
 *
 * The dial, its selector and the AM/PM blocks all read from the active palette,
 * so the picker is navy and champagne gold in the dark themes rather than
 * Material's purple default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun dialColors(): TimePickerColors = TimePickerDefaults.colors(
    containerColor = ClockwiseColors.Background,
    clockDialColor = ClockwiseColors.Card,
    clockDialSelectedContentColor = ClockwiseColors.PrimaryForeground,
    clockDialUnselectedContentColor = ClockwiseColors.Foreground,
    selectorColor = ClockwiseColors.Primary,
    periodSelectorBorderColor = ClockwiseColors.Border,
    periodSelectorSelectedContainerColor = ClockwiseColors.Primary,
    periodSelectorSelectedContentColor = ClockwiseColors.PrimaryForeground,
    periodSelectorUnselectedContainerColor = ClockwiseColors.Card,
    periodSelectorUnselectedContentColor = ClockwiseColors.Foreground,
)
