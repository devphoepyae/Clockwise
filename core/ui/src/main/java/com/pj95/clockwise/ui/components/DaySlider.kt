package com.pj95.clockwise.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.domain.time.WORKING_DAY_END_HOUR
import com.pj95.clockwise.domain.time.WORKING_DAY_START_HOUR
import com.pj95.clockwise.ui.theme.LocalClockwisePalette
import com.pj95.clockwise.ui.theme.ClockwisePalette
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import kotlin.math.roundToInt

/** Test tags so UI tests can address the control without matching its labels. */
object DaySliderTags {
    const val TRACK = "daySliderTrack"
}

/** Minutes in a day; the ruler's scale, which is longer than the slider's range. */
private const val MINUTES_PER_DAY = 24 * 60

/** Half the thumb, so the thumb never hangs over either end of the track. */
private val ThumbRadius = 10.dp

/**
 * The meeting planner's 24-hour slider.
 *
 * A day laid out left to right with a draggable marker on it: tap anywhere to
 * jump, or drag to sweep. The band between [WORKING_DAY_START_HOUR] and
 * [WORKING_DAY_END_HOUR] is shaded, so the usable part of the day is visible
 * before anything is dragged.
 *
 * Hand-drawn rather than built on Material's `Slider` for two reasons: the
 * shaded working-day band and the hour ruler have to line up with the marker
 * exactly, and Material's slider would bring its own track geometry, colours
 * and tick marks — the Material default look this project does not use. The
 * accessibility contract is the same one Material's slider implements:
 * `progressBarRangeInfo` plus a `setProgress` action, which is what makes
 * TalkBack able to adjust it without touching the screen.
 *
 * @param readout the chosen slot as the user's clock format writes it, e.g.
 *   "09:00" or "9:00 AM"; spoken as the control's state, because "45 percent"
 *   would be useless for a time of day
 * @param showWorkingDay true for the planner, which shades the working day;
 *   false for the map's time travel, where the band would be meaningless
 */
@Composable
fun DaySlider(
    minuteOfDay: MinuteOfDay,
    readout: String,
    onMinuteChange: (MinuteOfDay) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    showWorkingDay: Boolean = true,
) {
    val palette = LocalClockwisePalette.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = remember(palette) {
        ClockwiseTextStyles.PlannerAxisLabel.copy(color = palette.mutedForeground)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(DaySliderTags.TRACK)
            .pointerInput(Unit) {
                val inset = ThumbRadius.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onMinuteChange(slotAt(down.position.x, size.width.toFloat(), inset))
                    down.consume()

                    // Follow the finger for as long as it is down: a slider
                    // should answer on the first touch, not after a slop.
                    while (true) {
                        val change = awaitPointerEvent().changes
                            .firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        onMinuteChange(slotAt(change.position.x, size.width.toFloat(), inset))
                        change.consume()
                    }
                }
            }
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                stateDescription = readout
                progressBarRangeInfo = ProgressBarRangeInfo(
                    // Time travel can sit at 23:59, after the planner's last
                    // 15-minute slot; clamping keeps the range valid while the
                    // thumb is still drawn at the exact minute.
                    current = minuteOfDay.value
                        .coerceIn(MinuteOfDay.MIN_VALUE, MinuteOfDay.LAST_GRID_VALUE)
                        .toFloat(),
                    range = 0f..MinuteOfDay.LAST_GRID_VALUE.toFloat(),
                    steps = MinuteOfDay.GRID_STEPS,
                )
                setProgress { target ->
                    onMinuteChange(
                        MinuteOfDay(target.roundToInt().coerceIn(0, MinuteOfDay.LAST_GRID_VALUE))
                            .snappedToGrid(),
                    )
                    true
                }
            },
    ) {
        drawDayTrack(
            minuteOfDay = minuteOfDay,
            textMeasurer = textMeasurer,
            labelStyle = labelStyle,
            palette = palette,
            showWorkingDay = showWorkingDay,
        )
    }
}

/**
 * The slot under [x], snapped to the planner's grid.
 *
 * The scale runs the full 24 hours, so the last slot the slider can reach
 * (23:45) sits just short of the right-hand end rather than being flattened
 * onto it.
 */
private fun slotAt(x: Float, width: Float, inset: Float): MinuteOfDay {
    val span = (width - 2f * inset).coerceAtLeast(1f)
    val fraction = ((x - inset) / span).coerceIn(0f, 1f)
    return MinuteOfDay((fraction * MinuteOfDay.LAST_GRID_VALUE).roundToInt()).snappedToGrid()
}

private fun DrawScope.drawDayTrack(
    minuteOfDay: MinuteOfDay,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    palette: ClockwisePalette,
    showWorkingDay: Boolean,
) {
    val inset = ThumbRadius.toPx()
    val left = inset
    val right = size.width - inset
    val span = (right - left).coerceAtLeast(1f)

    fun xOf(minute: Int): Float = left + (minute.toFloat() / MINUTES_PER_DAY) * span

    val trackTop = 6.dp.toPx()
    val trackHeight = 12.dp.toPx()
    val corner = trackHeight / 2f
    val centreY = trackTop + trackHeight / 2f

    drawRoundRect(
        color = palette.card,
        topLeft = Offset(left, trackTop),
        size = Size(span, trackHeight),
        cornerRadius = CornerRadius(corner, corner),
    )

    // The working day, shaded. A pill *inside* the track rather than a fill of
    // it, so it cannot be mistaken for the slider's own active range when the
    // thumb happens to rest on one of its ends. Drawn before the ticks so the
    // hour ruler stays continuous right across it. The map's time travel has no
    // working-day meaning, so it draws the plain track.
    if (showWorkingDay) {
        val bandLeft = xOf(WORKING_DAY_START_HOUR * 60)
        val bandRight = xOf(WORKING_DAY_END_HOUR * 60)
        val bandInset = 2.dp.toPx()
        drawRoundRect(
            color = palette.accent,
            topLeft = Offset(bandLeft, trackTop + bandInset),
            size = Size(bandRight - bandLeft, trackHeight - 2f * bandInset),
            cornerRadius = CornerRadius(bandInset, bandInset),
        )
    }

    for (hour in 0..24) {
        val x = xOf(hour * 60)
        // One step darker than the band in the light theme and one step lighter
        // in the dark one, so the hour ruler reads the same in both.
        drawLine(
            color = palette.border,
            start = Offset(x, trackTop + 3.dp.toPx()),
            end = Offset(x, trackTop + trackHeight - 3.dp.toPx()),
            strokeWidth = 1f,
        )
    }

    drawRoundRect(
        color = palette.border,
        topLeft = Offset(left, trackTop),
        size = Size(span, trackHeight),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 1f),
    )

    val thumbX = xOf(minuteOfDay.value)
    drawCircle(color = palette.background, radius = ThumbRadius.toPx(), center = Offset(thumbX, centreY))
    drawCircle(
        color = palette.primary,
        radius = ThumbRadius.toPx() - 2.dp.toPx(),
        center = Offset(thumbX, centreY),
    )

    val labelTop = trackTop + trackHeight + 6.dp.toPx()
    for (hour in 0..24 step 6) {
        val layout = textMeasurer.measure(hour.toString().padStart(2, '0'), labelStyle)
        val x = xOf(hour * 60)
        // Centre each label on its tick; the ends still fit, because the track
        // is inset by the thumb's radius.
        val labelLeft = (x - layout.size.width / 2f)
            .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
        drawText(textLayoutResult = layout, topLeft = Offset(labelLeft, labelTop))
    }
}
