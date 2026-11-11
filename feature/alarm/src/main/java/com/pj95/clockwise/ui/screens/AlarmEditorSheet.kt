package com.pj95.clockwise.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.alarm.AlarmSoundCatalog
import com.pj95.clockwise.alarm.AlarmSoundOption
import com.pj95.clockwise.alarm.rememberAlarmSoundCatalog
import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.model.alarm.Alarm
import com.pj95.clockwise.model.alarm.RepeatDays
import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.ui.components.AlarmTimeDisplay
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.components.ClockwiseTimePicker
import com.pj95.clockwise.ui.components.MinTouchTarget
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** The time a brand-new alarm starts on, so the sheet opens on a plausible value. */
private const val DEFAULT_HOUR = 7
private const val DEFAULT_MINUTE = 0

/** Test tags for the editor's sound section. */
object AlarmEditorTestTags {
    const val SoundRow = "alarmSoundRow"
    const val SoundDialog = "alarmSoundDialog"
    const val SoundDialogDone = "alarmSoundDialogDone"

    fun soundOption(uri: String): String = "alarmSoundOption_$uri"
    fun previewButton(uri: String): String = "alarmSoundPreview_$uri"
}

/**
 * The alarm editor, presented as a bottom sheet.
 *
 * One sheet does both jobs: [alarm] `null` creates a new alarm, a stored alarm
 * edits it. That keeps the time picker, the repeat row, the sound and the label
 * in exactly one place, and means a new alarm is created with the same gestures
 * as an existing one is changed.
 *
 * [theme] chooses the time input. Material You asks for the platform's own clock
 * dial, because that theme is the one that advertises itself as the modern,
 * system-native look; every other theme keeps the app's wheel, which needs no
 * explanation and cannot produce an invalid time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorSheet(
    alarm: Alarm?,
    use24Hour: Boolean,
    onSave: (Alarm) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    theme: AppTheme = AppTheme.Default,
    soundCatalog: AlarmSoundCatalog = rememberAlarmSoundCatalog(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ClockwiseColors.Background,
        contentColor = ClockwiseColors.Foreground,
        scrimColor = Color(0x66000000),
        modifier = modifier,
    ) {
        AlarmEditorContent(
            alarm = alarm,
            use24Hour = use24Hour,
            onSave = onSave,
            onDelete = onDelete,
            onDismiss = onDismiss,
            theme = theme,
            soundCatalog = soundCatalog,
        )
    }
}

/**
 * The sheet body. Kept separate from the sheet host so it can be rendered
 * directly in previews and screenshot tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorContent(
    alarm: Alarm?,
    use24Hour: Boolean,
    onSave: (Alarm) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    theme: AppTheme = AppTheme.Default,
    soundCatalog: AlarmSoundCatalog = rememberAlarmSoundCatalog(),
) {
    // The picker reports changes through callbacks, so the hour and minute the
    // sheet saves are held here. One source of truth for the whole editor.
    var hour by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.hour ?: DEFAULT_HOUR) }
    var minute by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.minute ?: DEFAULT_MINUTE) }
    var label by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.label.orEmpty()) }
    var repeatDays by rememberSaveable(alarm?.id) {
        mutableStateOf(alarm?.repeatDays ?: RepeatDays.NONE)
    }
    var soundUri by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.soundUri.orEmpty()) }
    var vibrate by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.vibrate ?: true) }
    // A scheduled date and a repeat rule are mutually exclusive: a scheduled
    // alarm is pinned to one day, so choosing either clears the other.
    var scheduledDateEpochDay by rememberSaveable(alarm?.id) {
        mutableStateOf(alarm?.scheduledDateEpochDay)
    }
    var showSoundPicker by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    // The device's tones are a cursor read, not a cheap lookup; the list is
    // fetched once per sheet rather than on every recomposition.
    val soundOptions = remember(soundCatalog) {
        runCatching { soundCatalog.options() }.getOrDefault(emptyList())
    }

    // A date that has already gone cannot ring, so Save is withheld rather than
    // storing an alarm that will never fire. Today still counts as valid.
    val scheduleInPast = scheduledDateEpochDay?.let { it < LocalDate.now().toEpochDay() } == true
    val canSave = !scheduleInPast

    // A preview is a ringtone, and a ringtone outliving the sheet would keep
    // playing with no visible way to stop it.
    DisposableEffect(soundCatalog) {
        onDispose { soundCatalog.stopPreview() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    if (alarm == null) {
                        R.string.alarm_editor_new_title
                    } else {
                        R.string.alarm_editor_edit_title
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                color = ClockwiseColors.Foreground,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            CircleIconButton(
                iconRes = R.drawable.ic_close,
                contentDescription = stringResource(R.string.alarm_editor_close),
                onClick = onDismiss,
                size = 40.dp,
                iconSize = 18.dp,
            )
        }

        Spacer(Modifier.height(16.dp))

        SectionLabel(stringResource(R.string.alarm_editor_time_label))
        Spacer(Modifier.height(8.dp))

        // The time as the card and the ringing screen will show it: digits large,
        // "AM"/"PM" small beside them. Above the picker rather than below it, so
        // the number being changed and the number being read are the same one.
        AlarmTimeDisplay(
            clock = CityTimeFormatter.clockText(LocalTime.of(hour, minute), use24Hour),
            timeStyle = ClockwiseTextStyles.AlarmClock,
            periodStyle = ClockwiseTextStyles.AlarmClockPeriod,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(12.dp))

        // The same Google-Clock picker in every theme: a dial that cannot make an
        // invalid time, with a keypad one tap away for direct entry.
        ClockwiseTimePicker(
            hour = hour,
            minute = minute,
            use24Hour = use24Hour,
            onHourChange = { hour = it },
            onMinuteChange = { minute = it },
        )

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.alarm_editor_repeat_label),
                style = ClockwiseTextStyles.SectionLabel,
                color = ClockwiseColors.MutedForeground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (scheduledDateEpochDay != null) {
                    stringResource(R.string.alarm_repeat_once)
                } else {
                    repeatSummary(repeatDays)
                },
                style = ClockwiseTextStyles.SectionCount,
                color = ClockwiseColors.MutedForeground,
            )
        }

        Spacer(Modifier.height(8.dp))

        RepeatDayRow(
            mask = repeatDays,
            onDayToggle = { day, selected ->
                repeatDays = RepeatDays.with(repeatDays, day, selected)
                // Picking a repeat rule means the alarm is no longer pinned to
                // one date; the schedule row below goes back to "Schedule alarm".
                if (selected) scheduledDateEpochDay = null
            },
        )

        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.alarm_editor_schedule_label))
        Spacer(Modifier.height(8.dp))
        ScheduleRow(
            dateEpochDay = scheduledDateEpochDay,
            onClick = { showDatePicker = true },
            onClear = { scheduledDateEpochDay = null },
        )

        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.alarm_editor_sound_label))
        Spacer(Modifier.height(8.dp))

        SoundRow(
            title = soundTitle(soundOptions, soundUri),
            onClick = { showSoundPicker = true },
        )

        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.alarm_editor_vibrate_label))
        Spacer(Modifier.height(8.dp))
        VibrateRow(
            checked = vibrate,
            onCheckedChange = { vibrate = it },
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = label,
            onValueChange = { label = it.take(MAX_LABEL_LENGTH) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = stringResource(R.string.alarm_editor_label_placeholder),
                    color = ClockwiseColors.MutedForeground,
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.alarm_editor_label_field),
                    color = ClockwiseColors.MutedForeground,
                )
            },
            singleLine = true,
            shape = ClockwiseShapes.medium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ClockwiseColors.Foreground,
                unfocusedTextColor = ClockwiseColors.Foreground,
                focusedContainerColor = ClockwiseColors.Card,
                unfocusedContainerColor = ClockwiseColors.Card,
                cursorColor = ClockwiseColors.Foreground,
                focusedBorderColor = ClockwiseColors.Foreground,
                unfocusedBorderColor = ClockwiseColors.Border,
            ),
        )

        if (scheduleInPast) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.alarm_schedule_in_past),
                style = ClockwiseTextStyles.CityRegion,
                color = ClockwiseColors.MapAccent,
            )
        }

        Spacer(Modifier.height(20.dp))

        SaveAlarmButton(
            enabled = canSave,
            onClick = {
                val edited = alarm ?: Alarm(hour = hour, minute = minute)
                onSave(
                    edited.copy(
                        hour = hour,
                        minute = minute,
                        label = label.trim(),
                        repeatDays = repeatDays,
                        scheduledDateEpochDay = scheduledDateEpochDay,
                        vibrate = vibrate,
                        soundUri = soundUri,
                    ),
                )
            },
        )

        if (alarm != null) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(ClockwiseShapes.small)
                    .clickable(
                        onClickLabel = stringResource(R.string.alarm_editor_delete),
                        role = Role.Button,
                        onClick = { onDelete(alarm.id) },
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.alarm_editor_delete),
                    style = ClockwiseTextStyles.SectionLabel,
                    color = ClockwiseColors.MapAccent,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showSoundPicker) {
        SoundPickerDialog(
            options = soundOptions,
            selectedUri = soundUri,
            onSelect = {
                soundUri = it
                showSoundPicker = false
                soundCatalog.stopPreview()
            },
            onDismiss = {
                showSoundPicker = false
                soundCatalog.stopPreview()
            },
            onPreview = soundCatalog::preview,
            onStopPreview = soundCatalog::stopPreview,
        )
    }

    if (showDatePicker) {
        ScheduleDateDialog(
            initialEpochDay = scheduledDateEpochDay,
            onConfirm = { epochDay ->
                scheduledDateEpochDay = epochDay
                // A pinned date replaces the repeat rule; the two cannot both
                // describe when the alarm rings.
                repeatDays = RepeatDays.NONE
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/**
 * The date the alarm is pinned to.
 *
 * Material's calendar dialog works in UTC milliseconds, so the conversion is
 * explicit in both directions. The chosen day is stored as an epoch day, which
 * is what [Alarm.scheduledDateEpochDay] and the scheduler read.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDateDialog(
    initialEpochDay: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialEpochDay?.let {
            LocalDate.ofEpochDay(it)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    onConfirm(
                        Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toEpochDay(),
                    )
                },
            ) {
                Text(stringResource(R.string.alarm_schedule_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.alarm_schedule_cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/** The small all-caps label above a group of controls. */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = ClockwiseTextStyles.SectionLabel,
        color = ClockwiseColors.MutedForeground,
        modifier = modifier,
    )
}

/**
 * The one-off date row: a calendar icon, the chosen day (or an invitation to
 * pick one), and a clear action once a date is set.
 *
 * A scheduled alarm rings once on that day and then retires itself, so this row
 * is the alternative to the repeat strip rather than an addition to it.
 */
@Composable
private fun ScheduleRow(
    dateEpochDay: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .clickable(
                onClickLabel = stringResource(R.string.a11y_alarm_schedule),
                role = Role.Button,
                onClick = onClick,
            )
            .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = dateEpochDay?.let { scheduledDateSummary(it) }
                ?: stringResource(R.string.alarm_schedule_action),
            style = ClockwiseTextStyles.CityName,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.weight(1f),
        )
        if (dateEpochDay != null) {
            Text(
                text = stringResource(R.string.alarm_schedule_clear),
                style = ClockwiseTextStyles.SectionLabel,
                color = ClockwiseColors.MutedForeground,
                modifier = Modifier
                    .clip(ClockwiseShapes.small)
                    .clickable(
                        onClickLabel = stringResource(R.string.a11y_clear_schedule),
                        role = Role.Button,
                        onClick = onClear,
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** The whole-row vibration switch, mirroring the settings sheet's rows. */
@Composable
private fun VibrateRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .clickable(
                onClickLabel = stringResource(R.string.alarm_editor_vibrate_label),
                role = Role.Switch,
            ) { onCheckedChange(!checked) }
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_vibrate),
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.alarm_editor_vibrate_hint),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            // The row owns the interaction; the knob is its illustration.
            onCheckedChange = null,
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

/**
 * The one control that opens the sound list: the chosen sound's name, and a
 * chevron saying another list is behind it.
 *
 * It is a single target with the whole row clickable, so choosing a sound is one
 * tap on something the height of a finger, not a tap on the word only.
 */
@Composable
private fun SoundRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .clickable(
                onClickLabel = stringResource(R.string.alarm_sound_dialog_title),
                role = Role.Button,
                onClick = onClick,
            )
            .testTag(AlarmEditorTestTags.SoundRow)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = ClockwiseTextStyles.CityName,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Icon(
            painter = painterResource(R.drawable.ic_expand_more),
            // The row already says what it does; the arrow is decoration.
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * The sound list.
 *
 * A short, scrollable list rather than a spinner: each row previews on its own
 * button, so a person can hear a tone before committing to it, which is the one
 * thing a dropdown cannot do. Selecting closes the list, because the choice is
 * the only thing left to decide.
 */
@Composable
private fun SoundPickerDialog(
    options: List<AlarmSoundOption>,
    selectedUri: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    onPreview: (String) -> Unit,
    onStopPreview: () -> Unit,
) {
    var previewingUri by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.testTag(AlarmEditorTestTags.SoundDialog),
            shape = ClockwiseShapes.large,
            color = ClockwiseColors.Background,
            contentColor = ClockwiseColors.Foreground,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.alarm_sound_dialog_title),
                    style = ClockwiseTextStyles.CityName,
                    color = ClockwiseColors.Foreground,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.alarm_sound_dialog_support),
                    style = ClockwiseTextStyles.CityRegion,
                    color = ClockwiseColors.MutedForeground,
                )
                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .selectableGroup(),
                ) {
                    items(options, key = { it.uri }) { option ->
                        SoundOptionRow(
                            option = option,
                            isSelected = option.uri == selectedUri,
                            isPreviewing = option.uri == previewingUri,
                            onSelect = { onSelect(option.uri) },
                            onPreview = {
                                if (previewingUri == option.uri) {
                                    onStopPreview()
                                    previewingUri = null
                                } else {
                                    onPreview(option.uri)
                                    previewingUri = option.uri
                                }
                            },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinTouchTarget)
                        .clip(ClockwiseShapes.small)
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                onStopPreview()
                                onDismiss()
                            },
                        )
                        .testTag(AlarmEditorTestTags.SoundDialogDone),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.alarm_sound_dialog_done),
                        style = ClockwiseTextStyles.SectionLabel,
                        color = ClockwiseColors.Foreground,
                    )
                }
            }
        }
    }
}

/**
 * One sound: pick it anywhere on the row, or preview it from the button on the
 * right.
 *
 * The preview is a separate control on purpose. Making the whole row play would
 * mean selecting a sound also made noise, and both actions would be reachable
 * only by trial.
 */
@Composable
private fun SoundOptionRow(
    option: AlarmSoundOption,
    isSelected: Boolean,
    isPreviewing: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MinTouchTarget)
                .clip(ClockwiseShapes.small)
                .selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = onSelect,
                )
                .testTag(AlarmEditorTestTags.soundOption(option.uri))
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = option.title,
                style = ClockwiseTextStyles.CityName,
                color = ClockwiseColors.Foreground,
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = stringResource(
                        R.string.a11y_alarm_sound_selected,
                        option.title,
                    ),
                    tint = ClockwiseColors.Foreground,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .heightIn(min = MinTouchTarget)
                .clip(ClockwiseShapes.small)
                .clickable(
                    onClickLabel = stringResource(
                        if (isPreviewing) R.string.alarm_sound_stop else R.string.alarm_sound_preview,
                    ),
                    role = Role.Button,
                    onClick = onPreview,
                )
                .testTag(AlarmEditorTestTags.previewButton(option.uri))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(
                    if (isPreviewing) R.string.alarm_sound_stop else R.string.alarm_sound_preview,
                ),
                style = ClockwiseTextStyles.SectionLabel,
                color = ClockwiseColors.MutedForeground,
            )
        }
    }
}

/** The name of [soundUri], falling back to the default's name if it is gone. */
@Composable
private fun soundTitle(options: List<AlarmSoundOption>, soundUri: String): String =
    options.firstOrNull { it.uri == soundUri }?.title
        ?: stringResource(R.string.alarm_sound_default)

/**
 * Seven day toggles, Monday first.
 *
 * A 360 dp screen cannot give seven separate 48 dp-wide targets in one row, so
 * the targets are edge to edge and the row is 48 dp tall: the whole strip is
 * reachable, with no dead gap between one day and the next. Each chip is a
 * checkbox for a screen reader and announces the full day name.
 */
@Composable
private fun RepeatDayRow(
    mask: Int,
    onDayToggle: (DayOfWeek, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DayOfWeek.values().forEach { day ->
            val selected = RepeatDays.includes(mask, day)
            DayChip(
                day = day,
                selected = selected,
                onToggle = { onDayToggle(day, !selected) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DayChip(
    day: DayOfWeek,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayName = stringResource(R.string.a11y_alarm_day, shortDayName(day))
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(ClockwiseShapes.small)
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .semantics { contentDescription = dayName },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) ClockwiseColors.Primary else ClockwiseColors.Card)
                .border(
                    width = 1.dp,
                    color = if (selected) ClockwiseColors.Primary else ClockwiseColors.Border,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = narrowDayName(day),
                style = ClockwiseTextStyles.SegmentLabel,
                color = if (selected) {
                    ClockwiseColors.PrimaryForeground
                } else {
                    ClockwiseColors.MutedForeground
                },
            )
        }
    }
}

@Composable
private fun SaveAlarmButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(ClockwiseShapes.medium)
            .background(
                if (enabled) ClockwiseColors.Primary else ClockwiseColors.Secondary,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.alarm_editor_save),
            style = ClockwiseTextStyles.SectionLabel,
            color = if (enabled) {
                ClockwiseColors.PrimaryForeground
            } else {
                ClockwiseColors.MutedForeground
            },
        )
    }
}

/** A label is a line on a card, not a note; this keeps it one line. */
private const val MAX_LABEL_LENGTH = 40
