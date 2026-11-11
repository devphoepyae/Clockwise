package com.pj95.clockwise.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import kotlin.math.abs

/** Height of one number in a wheel. */
private val WheelItemHeight = 44.dp

/** Height of the visible wheel: four numbers, the middle one selected. */
private val WheelHeight = 176.dp

/**
 * A scrolling hour/minute picker in the shape people already know from alarm
 * clocks: two wheels, a colon, and a centre line that marks the chosen number.
 *
 * There is no keypad on purpose. A wheel can only produce a valid time, needs no
 * "did I type 7 or 17?" reading, and is driven by one gesture. Values are
 * reported as they pass the centre line, so the caller's state always matches
 * what is on screen; the parent only has to keep the 0-23 hour it already
 * stores.
 *
 * When the app is in Material You the editor uses the platform's dial instead —
 * see `AlarmEditorContent`.
 *
 * @param hour 0-23, in 24-hour form
 * @param minute 0-59
 */
@Composable
fun WheelTimePicker(
    hour: Int,
    minute: Int,
    use24Hour: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hourValues = remember(use24Hour) {
        if (use24Hour) (0..23).toList() else (1..12).toList()
    }
    val isPm = hour >= 12
    val displayHour = if (use24Hour) hour else if (hour % 12 == 0) 12 else hour % 12

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Wheel(
                values = hourValues,
                selectedValue = displayHour,
                label = stringResource(R.string.a11y_alarm_hour_wheel),
                format = { value -> if (use24Hour) twoDigits(value) else value.toString() },
                onSelected = { value ->
                    onHourChange(if (use24Hour) value else toTwentyFourHour(value, isPm))
                },
                testTag = AlarmTimeTestTags.HourWheel,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = ":",
                style = ClockwiseTextStyles.WheelNumber,
                color = ClockwiseColors.MutedForeground,
                modifier = Modifier.padding(horizontal = 2.dp),
            )

            Wheel(
                values = (0..59).toList(),
                selectedValue = minute,
                label = stringResource(R.string.a11y_alarm_minute_wheel),
                format = ::twoDigits,
                onSelected = onMinuteChange,
                testTag = AlarmTimeTestTags.MinuteWheel,
                modifier = Modifier.weight(1f),
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
                onValueSelected = { pm -> onHourChange(toTwentyFourHour(displayHour, pm)) },
            )
        }
    }
}

/**
 * Test tags for the time pickers, so a UI test can assert which one the current
 * theme puts on screen.
 */
object AlarmTimeTestTags {
    const val HourWheel = "alarmHourWheel"
    const val MinuteWheel = "alarmMinuteWheel"
    const val Dial = "alarmTimeDial"
    const val Keypad = "alarmTimeKeypad"
    const val KeypadToggle = "alarmTimeKeypadToggle"
    const val HourField = "alarmTimeHourField"
    const val MinuteField = "alarmTimeMinuteField"
}

/**
 * One wheel: a lazily built, snapping column of numbers with the selected one
 * held in the middle.
 *
 * The list is padded by half the missing height top and bottom, which is what
 * lets the first and last numbers reach the centre line instead of stopping at
 * the edge.
 */
@Composable
fun Wheel(
    values: List<Int>,
    selectedValue: Int,
    label: String,
    format: (Int) -> String,
    onSelected: (Int) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val initialIndex = remember { values.indexOf(selectedValue).coerceAtLeast(0) }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(state)

    // Which number is under the centre line. Read from the layout rather than
    // from the index, because a fling passes through several items and only the
    // one at the line is the new value.
    val centeredIndex by remember(state) {
        derivedStateOf {
            val layout = state.layoutInfo
            val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            layout.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2 - center) }
                ?.index
                ?: initialIndex
        }
    }

    LaunchedEffect(centeredIndex) {
        val value = values.getOrNull(centeredIndex) ?: return@LaunchedEffect
        if (value != selectedValue) onSelected(value)
    }

    Box(
        modifier = modifier
            .height(WheelHeight)
            .testTag(testTag)
            // One label for the column: the numbers inside are decorative once
            // the value is announced, and fifty separate nodes are not a control.
            .semantics { contentDescription = label },
    ) {
        LazyColumn(
            state = state,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = (WheelHeight - WheelItemHeight) / 2),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            itemsIndexed(values) { index, value ->
                WheelNumber(
                    text = format(value),
                    isCentered = index == centeredIndex,
                )
            }
        }

        // Fade the numbers that are leaving, so the wheel does not look cut off.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(WheelItemHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(ClockwiseColors.Background, Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(WheelItemHeight)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, ClockwiseColors.Background),
                    ),
                ),
        )

        // The selection line, drawn over the wheel and under nothing: it only
        // touches the edges of the centred number.
        Column(modifier = Modifier.align(Alignment.Center).fillMaxWidth()) {
            HorizontalDivider(color = ClockwiseColors.Border)
            Spacer(Modifier.height(WheelItemHeight - 2.dp))
            HorizontalDivider(color = ClockwiseColors.Border)
        }
    }
}

@Composable
private fun WheelNumber(text: String, isCentered: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isCentered) 1f else 0.72f,
        label = "wheelScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isCentered) 1f else 0.35f,
        label = "wheelAlpha",
    )
    Box(
        modifier = Modifier
            .height(WheelItemHeight)
            .width(72.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = ClockwiseTextStyles.WheelNumber,
            color = ClockwiseColors.Foreground,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha),
        )
    }
}

fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

/** 12-hour value plus AM/PM as the 0-23 hour the app stores. */
private fun toTwentyFourHour(hour12: Int, isPm: Boolean): Int =
    (hour12 % 12) + if (isPm) 12 else 0
