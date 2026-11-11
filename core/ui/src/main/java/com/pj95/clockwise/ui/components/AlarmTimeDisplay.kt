package com.pj95.clockwise.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import com.pj95.clockwise.common.time.ClockText

/**
 * A time with its "AM"/"PM" beside it, in one text node.
 *
 * Three screens show an alarm's time — the card, the editor's preview and the
 * ringing screen — and all three want the digits large with a small period
 * tucked against them. Doing that with two `Text`s in a `Row` would look right
 * and read wrong: TalkBack would announce "6:30" and "PM" as separate nodes, and
 * the period would sit on a different baseline.
 *
 * So the period is a [androidx.compose.ui.text.SpanStyle] inside an
 * `AnnotatedString`. One node, one announcement ("6:30 AM"), correct baseline,
 * and still visibly smaller. In 24-hour mode the period is simply absent.
 */
@Composable
fun AlarmTimeDisplay(
    clock: ClockText,
    timeStyle: TextStyle,
    periodStyle: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    periodColor: Color = color,
) {
    val period = clock.periodText
    val text = buildAnnotatedString {
        append(clock.hourText)
        append(":")
        append(clock.minuteText)
        if (period != null) {
            withStyle(periodStyle.asSpanStyle().copy(color = periodColor)) {
                // A space, not padding: the gap has to survive a screen reader
                // reading the string and a screenshot cropping the node.
                append(" ")
                append(period)
            }
        }
    }

    Text(
        text = text,
        style = timeStyle.copy(color = color),
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        softWrap = false,
    )
}

/** `TextStyle` as a span, so a period can borrow a whole named style. */
private fun TextStyle.asSpanStyle() = androidx.compose.ui.text.SpanStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    fontSynthesis = fontSynthesis,
    fontFamily = fontFamily,
    fontFeatureSettings = fontFeatureSettings,
    letterSpacing = letterSpacing,
    baselineShift = baselineShift,
    textGeometricTransform = textGeometricTransform,
    localeList = localeList,
    background = background,
    textDecoration = textDecoration,
    shadow = shadow,
)
