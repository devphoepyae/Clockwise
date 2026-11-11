package com.pj95.clockwise.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.common.alarm.clockText
import com.pj95.clockwise.common.alarm.timeText
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.model.alarm.RepeatDays
import com.pj95.clockwise.ui.AlarmUiState
import com.pj95.clockwise.ui.StopwatchUiState
import com.pj95.clockwise.ui.TimerUiState
import com.pj95.clockwise.ui.components.AlarmTimeDisplay
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.components.MinTouchTarget
import com.pj95.clockwise.ui.components.SwipeToDeleteBox
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Test tags for the alarm tab. */
object AlarmTestTags {
    const val Screen = "alarmScreen"
    const val AddButton = "alarmAddButton"
    const val AddFab = "alarmAddFab"
    const val AddRow = "alarmAddRow"
    const val TimerButton = "alarmTimerButton"
    const val StopwatchButton = "alarmStopwatchButton"
    const val TimerBackButton = "alarmTimerBackButton"
    const val ExactNotice = "alarmExactNotice"
    const val AlarmsSection = "alarmSectionAlarms"
    const val TimerSection = "alarmSectionTimer"

    fun alarm(id: Long): String = "alarmCard_$id"
}

/** The three sections of the Alarms tab. */
enum class AlarmSection { Alarms, Timer, Stopwatch }

/**
 * "Alarms" — the saved alarm list and the countdown timer.
 *
 * The two share a tab because both answer "what will interrupt me next?", but
 * only one of them is on screen at a time. v4.0.0 said so with an `ALARMS ·
 * TIMER` switch; v4.1.0 says it with a single hourglass in the header instead.
 * The list is the destination, the timer is the detour: it is opened from the
 * hourglass and left with the close button or system Back, so the alarm screen
 * stays the calm thing the user sees when they open the tab.
 *
 * An alarm is one compact card. The time is the anchor, the label and repeat
 * rule share a line beneath it, and the switch is the only control that writes
 * without opening the editor. Adding is next to the list it adds to rather than
 * in the header, which now belongs to the timer.
 *
 * @param openTimer true when a timer notification asked for the countdown; the
 *   screen switches to it once and reports back through [onTimerOpened] so a
 *   recomposition cannot replay the request.
 */
@Composable
fun AlarmScreen(
    state: AlarmUiState,
    use24Hour: Boolean,
    onAddClick: () -> Unit,
    onAlarmClick: (Alarm) -> Unit,
    onToggleAlarm: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onAllowExactAlarms: () -> Unit = {},
    timerState: TimerUiState = TimerUiState(),
    onStartTimer: (Long) -> Unit = {},
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    onExtendTimer: (Long) -> Unit = {},
    onRestartTimer: () -> Unit = {},
    openTimer: Boolean = false,
    onTimerOpened: () -> Unit = {},
    /** Removes an alarm after a swipe or the accessibility delete action. */
    onDeleteAlarm: (Alarm) -> Unit = {},
    /** Persists the order produced by a long-press drag. */
    onReorderAlarms: (List<Long>) -> Unit = {},
    stopwatchState: StopwatchUiState = StopwatchUiState(),
    onToggleStopwatch: () -> Unit = {},
    onResetStopwatch: () -> Unit = {},
) {
    var section by rememberSaveable { mutableStateOf(AlarmSection.Alarms) }

    LaunchedEffect(openTimer) {
        if (!openTimer) return@LaunchedEffect
        section = AlarmSection.Timer
        onTimerOpened()
    }

    // The timer and stopwatch are detours, not destinations: Back belongs to the
    // alarm list.
    BackHandler(enabled = section != AlarmSection.Alarms) {
        section = AlarmSection.Alarms
    }

    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val savedOrder = state.alarms.map { it.id }

    // Same contract as the city list: the cards rearrange locally while the
    // finger is down, and Room receives one order when it lifts.
    var localOrder by remember { mutableStateOf(savedOrder) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(savedOrder) {
        if (draggingId == null) localOrder = savedOrder
    }

    val alarmsById = state.alarms.associateBy { it.id }
    val displayedAlarms = localOrder.mapNotNull(alarmsById::get) +
        state.alarms.filterNot { it.id in localOrder }

    fun moveAlarm(alarmId: Long, targetIndex: Int) {
        val ids = displayedAlarms.map { it.id }.toMutableList()
        val from = ids.indexOf(alarmId)
        if (from < 0 || targetIndex !in ids.indices || from == targetIndex) return
        ids.add(targetIndex, ids.removeAt(from))
        onReorderAlarms(ids)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(AlarmTestTags.Screen)
            .background(ClockwiseColors.Background),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // The bottom padding clears the floating add button, so the last
            // card is never trapped underneath it.
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 16.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AlarmHeader(
                    section = section,
                    onTimerClick = { section = AlarmSection.Timer },
                    onStopwatchClick = { section = AlarmSection.Stopwatch },
                    onBackClick = { section = AlarmSection.Alarms },
                )
            }

            when (section) {
                AlarmSection.Timer -> {
                    item {
                        TimerPanel(
                            state = timerState,
                            onStart = onStartTimer,
                            onPause = onPauseTimer,
                            onResume = onResumeTimer,
                            onReset = onResetTimer,
                            onAllowExactAlarms = onAllowExactAlarms,
                            onExtend = onExtendTimer,
                            onRestart = onRestartTimer,
                        )
                    }
                    item { AlarmFootnote(text = stringResource(R.string.timer_footnote)) }
                }

                AlarmSection.Stopwatch -> {
                    item {
                        StopwatchPanel(
                            state = stopwatchState,
                            onToggle = onToggleStopwatch,
                            onReset = onResetStopwatch,
                        )
                    }
                    item { AlarmFootnote(text = stringResource(R.string.stopwatch_footnote)) }
                }

                AlarmSection.Alarms -> {
                    if (!state.exactAlarmsAllowed) {
                        item { ExactAlarmNotice(onAllow = onAllowExactAlarms) }
                    }

                    when {
                        state.isLoading -> item { AlarmLoadingState() }

                        state.alarms.isEmpty() -> item { AlarmEmptyState(onAddClick = onAddClick) }

                        else -> {
                            items(items = displayedAlarms, key = { it.id }) { alarm ->
                    val index = displayedAlarms.indexOfFirst { it.id == alarm.id }
                    val isDragging = alarm.id == draggingId
                    val canDrag = displayedAlarms.size > 1

                    val reorderModifier = if (canDrag) {
                        Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffset else 0f
                            }
                            .pointerInput(alarm.id, displayedAlarms.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = alarm.id
                                        dragOffset = 0f
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, amount ->
                                        if (draggingId != alarm.id) {
                                            return@detectDragGesturesAfterLongPress
                                        }
                                        change.consume()
                                        dragOffset += amount.y

                                        val info = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == alarm.id }
                                            ?: return@detectDragGesturesAfterLongPress
                                        val center = info.offset + info.size / 2f + dragOffset
                                        val target = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull {
                                                center >= it.offset && center <= it.offset + it.size
                                            }
                                            ?: return@detectDragGesturesAfterLongPress
                                        val targetId = target.key as? Long
                                            ?: return@detectDragGesturesAfterLongPress
                                        if (targetId == alarm.id) {
                                            return@detectDragGesturesAfterLongPress
                                        }

                                        val from = localOrder.indexOf(alarm.id)
                                        val to = localOrder.indexOf(targetId)
                                        if (from < 0 || to < 0 || from == to) {
                                            return@detectDragGesturesAfterLongPress
                                        }

                                        // Keep the card under the finger after the
                                        // list has rearranged beneath it.
                                        val visualTop = info.offset + dragOffset
                                        localOrder = localOrder.toMutableList().apply {
                                            add(to, removeAt(from))
                                        }
                                        dragOffset = visualTop - target.offset
                                    },
                                    onDragEnd = {
                                        if (draggingId != alarm.id) {
                                            return@detectDragGesturesAfterLongPress
                                        }
                                        val finalOrder = localOrder
                                        draggingId = null
                                        dragOffset = 0f
                                        if (finalOrder != savedOrder) {
                                            onReorderAlarms(finalOrder)
                                        }
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragOffset = 0f
                                        localOrder = savedOrder
                                    },
                                )
                            }
                    } else {
                        Modifier
                    }

                    SwipeToDeleteBox(onDelete = { onDeleteAlarm(alarm) }) {
                        AlarmCard(
                            alarm = alarm,
                            use24Hour = use24Hour,
                            onClick = { onAlarmClick(alarm) },
                            onToggle = { onToggleAlarm(alarm.id, it) },
                            modifier = reorderModifier,
                            onMoveUp = if (index > 0) {
                                { moveAlarm(alarm.id, index - 1) }
                            } else {
                                null
                            },
                            onMoveDown = if (index < displayedAlarms.lastIndex) {
                                { moveAlarm(alarm.id, index + 1) }
                            } else {
                                null
                            },
                            onDelete = { onDeleteAlarm(alarm) },
                        )
                    }
                            }
                        }
                    }

                    item { AlarmFootnote(text = stringResource(R.string.alarms_footnote)) }
                }
            }
        }

        // The add action floats over the list rather than sitting in a header or
        // a bottom bar: it is the one thing always reachable, and it belongs to
        // the list it adds to. Hidden in the timer and stopwatch detours, where
        // there is no alarm to add.
        if (section == AlarmSection.Alarms) {
            AddAlarmFab(
                onClick = onAddClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
            )
        }
    }
}

/** The floating add button, sized and placed like the platform's own FAB. */
@Composable
private fun AddAlarmFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.a11y_add_alarm)
    Box(
        modifier = modifier
            .size(56.dp)
            .testTag(AlarmTestTags.AddFab)
            .clip(CircleShape)
            .background(ClockwiseColors.Primary)
            .clickable(
                onClickLabel = description,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = ClockwiseColors.PrimaryForeground,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Eyebrow, title and the header actions.
 *
 * On the alarm list the two icon buttons open the timer and the stopwatch; in
 * either detour a single close button returns to the list. Keeping the actions
 * to icons is what makes the header read as a title rather than a toolbar.
 */
@Composable
private fun AlarmHeader(
    section: AlarmSection,
    onTimerClick: () -> Unit,
    onStopwatchClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    when (section) {
                        AlarmSection.Alarms -> R.string.alarms_eyebrow
                        AlarmSection.Timer -> R.string.timer_eyebrow
                        AlarmSection.Stopwatch -> R.string.stopwatch_eyebrow
                    },
                ),
                style = ClockwiseTextStyles.Eyebrow,
                color = ClockwiseColors.MutedForeground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(
                    when (section) {
                        AlarmSection.Alarms -> R.string.alarms_title
                        AlarmSection.Timer -> R.string.timer_title
                        AlarmSection.Stopwatch -> R.string.stopwatch_title
                    },
                ),
                style = ClockwiseTextStyles.ScreenTitle,
                color = ClockwiseColors.Foreground,
                modifier = Modifier.semantics { heading() },
            )
        }

        if (section == AlarmSection.Alarms) {
            CircleIconButton(
                iconRes = R.drawable.ic_hourglass,
                contentDescription = stringResource(R.string.timer_title),
                onClick = onTimerClick,
                size = 44.dp,
                iconSize = 20.dp,
                modifier = Modifier.testTag(AlarmTestTags.TimerButton),
            )
            Spacer(Modifier.width(4.dp))
            CircleIconButton(
                iconRes = R.drawable.ic_stopwatch,
                contentDescription = stringResource(R.string.stopwatch_title),
                onClick = onStopwatchClick,
                size = 44.dp,
                iconSize = 20.dp,
                modifier = Modifier.testTag(AlarmTestTags.StopwatchButton),
            )
        } else {
            CircleIconButton(
                iconRes = R.drawable.ic_close,
                contentDescription = stringResource(R.string.timer_close),
                onClick = onBackClick,
                size = 44.dp,
                iconSize = 20.dp,
                modifier = Modifier.testTag(AlarmTestTags.TimerBackButton),
            )
        }
    }
}

/**
 * One alarm, in two lines.
 *
 * The time is the anchor, as it is on a city card, with "AM"/"PM" set small
 * beside it. The label and the repeat rule share the second line because they
 * answer the same question — what is this and when does it happen — and one
 * line each would make every card twice as tall for no extra meaning. The
 * switch is the only control on the card, and the whole card opens the editor.
 */
@Composable
fun AlarmCard(
    alarm: Alarm,
    use24Hour: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /** Accessibility reordering; `null` when the card is already at that end. */
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    /** Accessibility equivalent of the horizontal swipe. */
    onDelete: (() -> Unit)? = null,
) {
    val contentColor =
        if (alarm.enabled) ClockwiseColors.Foreground else ClockwiseColors.MutedForeground
    val timeText = alarm.timeText(use24Hour)
    val editLabel = stringResource(R.string.a11y_edit_alarm, timeText)
    val toggleDescription = stringResource(R.string.a11y_alarm_toggle, timeText)
    val moveUpLabel = stringResource(R.string.a11y_move_alarm_up)
    val moveDownLabel = stringResource(R.string.a11y_move_alarm_down)
    val deleteLabel = stringResource(R.string.a11y_delete_alarm)
    val schedule = alarm.scheduledDateEpochDay?.let { scheduledDateSummary(it) }
    val supporting = listOf(
        alarm.label,
        schedule?.let { stringResource(R.string.alarm_scheduled_on, it) }
            ?: repeatSummary(alarm.repeatDays),
    )
        .filter { it.isNotBlank() }
        .joinToString(SUPPORTING_SEPARATOR)

    val cardActions = buildList {
        if (onMoveUp != null) {
            add(CustomAccessibilityAction(moveUpLabel) { onMoveUp(); true })
        }
        if (onMoveDown != null) {
            add(CustomAccessibilityAction(moveDownLabel) { onMoveDown(); true })
        }
        if (onDelete != null) {
            add(CustomAccessibilityAction(deleteLabel) { onDelete(); true })
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(AlarmTestTags.alarm(alarm.id))
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .semantics {
                if (cardActions.isNotEmpty()) customActions = cardActions
            }
            .clickable(onClickLabel = editLabel, onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AlarmTimeDisplay(
                clock = alarm.clockText(use24Hour),
                timeStyle = ClockwiseTextStyles.AlarmClock,
                periodStyle = ClockwiseTextStyles.AlarmClockPeriod,
                color = contentColor,
            )

            if (supporting.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = supporting,
                    style = ClockwiseTextStyles.CityRegion,
                    color = ClockwiseColors.MutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // The custom actions carry the meaning; the grip is the visual hint
        // that this card can be held and moved, matching the city list.
        if (onMoveUp != null || onMoveDown != null) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = null,
                tint = ClockwiseColors.MutedForeground.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
        }

        Switch(
            checked = alarm.enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.semantics {
                contentDescription = toggleDescription
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = ClockwiseColors.PrimaryForeground,
                checkedTrackColor = ClockwiseColors.Primary,
                uncheckedThumbColor = ClockwiseColors.MutedForeground,
                uncheckedTrackColor = ClockwiseColors.Secondary,
                uncheckedBorderColor = ClockwiseColors.Border,
            ),
        )
    }
}

@Composable
private fun AlarmLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.alarms_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = ClockwiseColors.MutedForeground,
        )
    }
}

@Composable
private fun AlarmEmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_alarm),
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.alarms_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = ClockwiseColors.Foreground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.alarms_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = ClockwiseColors.MutedForeground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .clip(ClockwiseShapes.medium)
                .clickable(onClick = onAddClick, role = Role.Button)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ClockwiseColors.Card)
                    .border(1.dp, ClockwiseColors.Border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = ClockwiseColors.Foreground,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.alarms_empty_action),
                style = ClockwiseTextStyles.CityName,
                color = ClockwiseColors.Foreground,
            )
        }
    }
}

/**
 * Shown only when the platform refuses exact alarms: Android 12/12L without the
 * "Alarms & reminders" grant. Deliberately worded as a consequence rather than
 * an error, and offered once at the top of the list.
 */
@Composable
private fun ExactAlarmNotice(onAllow: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AlarmTestTags.ExactNotice)
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Accent)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .clickable(onClick = onAllow)
            .heightIn(min = MinTouchTarget)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_alarm),
            contentDescription = null,
            tint = ClockwiseColors.Foreground,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.alarms_exact_hint),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.alarms_exact_action),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.Foreground,
        )
    }
}

@Composable
private fun AlarmFootnote(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = ClockwiseTextStyles.DstCaption,
            color = ClockwiseColors.MutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * "Once", "Every day", "Weekdays", "Weekends", or the short names of the chosen
 * days. The three named masks are spelled out because they are what people
 * actually pick; anything else is read as the days themselves.
 */
@Composable
fun repeatSummary(mask: Int): String = when (mask) {
    RepeatDays.NONE -> stringResource(R.string.alarm_repeat_once)
    RepeatDays.EVERY_DAY -> stringResource(R.string.alarm_repeat_every_day)
    RepeatDays.WEEKDAYS -> stringResource(R.string.alarm_repeat_weekdays)
    RepeatDays.WEEKEND -> stringResource(R.string.alarm_repeat_weekends)
    else -> RepeatDays.days(mask).joinToString(", ") { shortDayName(it) }
}

/** The three-letter day name the platform uses in the current locale. */
fun shortDayName(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.SHORT, Locale.getDefault())

/**
 * "Wed, 12 Nov" — the single day a scheduled alarm is pinned to.
 *
 * The card shows this instead of the repeat rule, because a scheduled alarm has
 * no repeat: it rings once, on that date, and then retires itself.
 */
fun scheduledDateSummary(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))

/** The one-letter day name used inside the repeat chips. */
fun narrowDayName(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.NARROW, Locale.getDefault())

/** What separates a card's label from its repeat rule. */
private const val SUPPORTING_SEPARATOR = " · "
