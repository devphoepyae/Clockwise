package com.pj95.clockwise.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.common.alarm.clockText
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.ui.components.AlarmTimeDisplay
import com.pj95.clockwise.ui.theme.LocalClockwisePalette
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwisePalette
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The screen a ringing alarm shows, over the lock screen.
 *
 * Deliberately almost empty. At 06:30 the user is half awake and the phone may
 * be face-up on a bedside table, so the screen carries exactly three things: the
 * label they wrote, the time it is, and the two ways out of the ring.
 *
 * The label is the hero rather than the clock because it is the only part that
 * says *why* the phone is ringing: "Medication" and "Morning run" are different
 * instructions, and 06:30 is the same either way. Its size follows the screen
 * width, so a long label on a small phone still fits without shrinking into
 * something unreadable.
 *
 * Stopping is a slide rather than a tap, because a half-asleep hand cannot
 * dismiss an alarm by brushing the screen. Snoozing is the opposite gesture,
 * available anywhere on the screen: a deliberate swipe up buys ten more
 * minutes. The service also silences itself after five minutes, so the screen
 * explains that rather than leaving the user guessing.
 *
 * @param palette the fixed navy-and-gold scheme the background is painted
 *   with. A parameter so screenshots and previews can pin the colours; the
 *   activity passes [com.pj95.clockwise.ui.theme.AlarmRingingPalette].
 */
@Composable
fun AlarmRingingScreen(
    alarm: Alarm?,
    use24Hour: Boolean,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onSnooze: () -> Unit = {},
    palette: ClockwisePalette = LocalClockwisePalette.current,
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val snoozeLabel = stringResource(R.string.a11y_snooze_alarm)

    val pull = remember { Animatable(0f) }
    val thresholdPx = with(density) { SNOOZE_PULL_THRESHOLD.toPx() }
    val maxPullPx = with(density) { SNOOZE_MAX_PULL.toPx() }
    val snoozeProgress = if (thresholdPx > 0f) {
        (-pull.value / thresholdPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    CompositionLocalProvider(LocalClockwisePalette provides palette) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(ringingWash(palette))
                // The whole screen is the snooze target, not a small button. The
                // vertical detector only claims vertical drags, so the horizontal
                // slide below still owns its own gesture.
                .pointerInput(onSnooze, thresholdPx) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (pull.value <= -thresholdPx) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSnooze()
                            }
                            scope.launch { pull.animateTo(0f, tween(SNAP_BACK_MILLIS)) }
                        },
                        onDragCancel = {
                            scope.launch { pull.animateTo(0f, tween(SNAP_BACK_MILLIS)) }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                pull.snapTo(
                                    (pull.value + dragAmount).coerceIn(-maxPullPx, 0f),
                                )
                            }
                        },
                    )
                }
                // TalkBack cannot reliably perform a swipe-up, so the same action
                // is offered explicitly rather than leaving the alarm unstoppable
                // from a screen reader.
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(snoozeLabel) {
                            onSnooze()
                            true
                        },
                    )
                },
        ) {
            // A soft gold glow behind the label. One palette colour, fading to
            // nothing, so the screen has depth without a picture or a pattern.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                palette.primary.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            // The title scales with the width but never past a comfortable
            // reading size: 30 sp on a narrow phone, 46 sp on a tablet.
            val titleSp = (maxWidth.value * TITLE_WIDTH_FRACTION)
                .coerceIn(MIN_TITLE_SP, MAX_TITLE_SP)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .graphicsLayer {
                        // A little resistance while the finger travels, so the
                        // gesture feels connected without moving the screen away.
                        translationY = pull.value * PULL_CONTENT_FRACTION
                    }
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))

                if (alarm != null) {
                    Text(
                        text = alarm.label.ifBlank {
                            stringResource(R.string.alarm_default_label)
                        },
                        style = ClockwiseTextStyles.RingingLabel.copy(
                            fontSize = titleSp.sp,
                            lineHeight = (titleSp * TITLE_LINE_HEIGHT_FACTOR).sp,
                        ),
                        color = ClockwiseColors.Foreground,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )

                    Spacer(Modifier.height(14.dp))

                    AlarmTimeDisplay(
                        clock = alarm.clockText(use24Hour),
                        timeStyle = ClockwiseTextStyles.RingingTime,
                        periodStyle = ClockwiseTextStyles.RingingTimePeriod,
                        color = ClockwiseColors.MutedForeground,
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.alarm_ringing_auto_silence),
                        style = ClockwiseTextStyles.CityRegion,
                        color = ClockwiseColors.MutedForeground,
                        textAlign = TextAlign.Center,
                    )
                } else if (isLoading) {
                    // A beat while the row is read; nothing to tap yet.
                    Spacer(Modifier.height(64.dp))
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = stringResource(R.string.alarm_swipe_to_snooze),
                    style = ClockwiseTextStyles.SectionLabel,
                    color = ClockwiseColors.Primary.copy(
                        alpha = HINT_RESTING_ALPHA + HINT_ACTIVE_ALPHA * snoozeProgress,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(12.dp))

                SlideToStop(onStop = onDismiss)
            }
        }
    }
}

/**
 * The screen's stop control: drag the thumb to the far edge to end the alarm.
 *
 * The gesture is a horizontal drag rather than a tap because a tap is what a
 * sleeping hand does by accident. Releasing before the end animates the thumb
 * back, so a half-drag visibly does nothing. TalkBack users get a single
 * "Stop the alarm" action instead: a screen reader cannot reliably drag, and
 * the alternative would be an alarm they cannot silence.
 */
@Composable
private fun SlideToStop(
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stopLabel = stringResource(R.string.a11y_stop_alarm)
    val hint = stringResource(R.string.alarm_slide_to_stop)
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val density = LocalDensity.current

    var trackWidthPx by remember { mutableFloatStateOf(0f) }

    val thumbSize = 56.dp
    val paddingPx = with(density) { 4.dp.toPx() }
    val thumbPx = with(density) { thumbSize.toPx() }
    val maxOffset = (trackWidthPx - thumbPx - paddingPx * 2f).coerceAtLeast(0f)
    val progress = if (maxOffset > 0f) (offset.value / maxOffset).coerceIn(0f, 1f) else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(maxOffset) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (maxOffset > 0f && offset.value >= maxOffset * COMMIT_FRACTION) {
                            onStop()
                        } else {
                            scope.launch { offset.animateTo(0f, tween(SNAP_BACK_MILLIS)) }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offset.animateTo(0f, tween(SNAP_BACK_MILLIS)) }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offset.snapTo((offset.value + dragAmount).coerceIn(0f, maxOffset))
                        }
                    },
                )
            }
            .semantics {
                contentDescription = stopLabel
                onClick(label = stopLabel) {
                    onStop()
                    true
                }
            },
    ) {
        // The trail behind the thumb: the further it travels, the more of the
        // track is filled, so progress is legible without a numeric readout.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { (offset.value + thumbPx + paddingPx * 2f).toDp() })
                .background(ClockwiseColors.Primary.copy(alpha = 0.16f)),
        )

        Text(
            text = hint,
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground.copy(alpha = 1f - progress),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 72.dp),
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .padding(start = 4.dp)
                .size(thumbSize)
                .clip(ClockwiseShapes.small)
                .background(ClockwiseColors.Primary),
            contentAlignment = Alignment.Center,
        ) {
            // A stop glyph rather than an arrow: it says what the gesture does,
            // and it needs no mirrored asset in a right-to-left layout.
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(ClockwiseShapes.extraSmall)
                    .background(ClockwiseColors.PrimaryForeground),
            )
        }
    }
}

/**
 * The screen's wallpaper wash.
 *
 * The primary colour at a third opacity over the background rather than the
 * container colour alone: a dark dynamic scheme's containers are very close to
 * its background, and the wash has to be visible without becoming a colour
 * block. On the ringing palette it is a quiet navy-to-gold gradient.
 */
private fun ringingWash(palette: ClockwisePalette): Brush = Brush.verticalGradient(
    colors = listOf(
        palette.primary.copy(alpha = 0.34f).compositeOver(palette.background),
        palette.background,
    ),
)

/** The title's share of the screen width. */
private const val TITLE_WIDTH_FRACTION = 0.105f
private const val MIN_TITLE_SP = 30f
private const val MAX_TITLE_SP = 46f
private const val TITLE_LINE_HEIGHT_FACTOR = 1.15f

/** How far the content follows the finger while snoozing. */
private const val PULL_CONTENT_FRACTION = 0.18f

/** The gold hint is visible at rest and brightens as the swipe commits. */
private const val HINT_RESTING_ALPHA = 0.55f
private const val HINT_ACTIVE_ALPHA = 0.45f

/** How far the finger has to travel up before releasing snoozes. */
private val SNOOZE_PULL_THRESHOLD = 72.dp

/** A hard cap so the content cannot be dragged off screen. */
private val SNOOZE_MAX_PULL = 200.dp

/** How far the thumb has to travel before releasing stops the alarm. */
private const val COMMIT_FRACTION = 0.82f

private const val SNAP_BACK_MILLIS = 220
