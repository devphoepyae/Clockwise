package com.pj95.clockwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.prefs.AppTheme
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.domain.time.WORKING_DAY_END_HOUR
import com.pj95.clockwise.domain.time.WORKING_DAY_START_HOUR
import com.pj95.clockwise.ui.PlannerCityRow
import com.pj95.clockwise.ui.PlannerSource
import com.pj95.clockwise.ui.PlannerUiState
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.components.DaySlider
import com.pj95.clockwise.ui.components.MinTouchTarget
import com.pj95.clockwise.ui.components.timeDifferenceLabel
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme

/** Test tags for the planner's converter and recommendation controls. */
object PlannerTestTags {
    const val BEST_ACTION = "planner-best-action"

    fun sourceChip(cityId: String?): String = "planner-source-${cityId ?: "device"}"
}

/**
 * Meeting planner: pick a time of day in one city, see what that is for every
 * saved city, at once.
 *
 * The slider is the whole feature. Dragging it moves the slot the rows are read
 * at, and every row moves with it, so the user can hunt for an hour that lands
 * inside the working day for as many cities as possible. The source chips turn
 * the same control into a converter: the wall-clock reading stays put and the
 * instant moves to the chosen city's zone.
 *
 * @param onMinuteChange called on every slider movement — the rows are derived
 *   from the slot, so the update is live rather than on release
 * @param onSourceChange called with the city id whose zone should express the
 *   slot, or `null` to return to the device's own zone
 */
@Composable
fun MeetingPlannerScreen(
    planner: PlannerUiState,
    onMinuteChange: (MinuteOfDay) -> Unit,
    onSourceChange: (String?) -> Unit,
    onBack: () -> Unit,
    onFindCityClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClockwiseColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        CircleIconButton(
            iconRes = R.drawable.ic_back,
            contentDescription = stringResource(R.string.detail_back),
            onClick = onBack,
            size = 44.dp,
            iconSize = 20.dp,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.planner_eyebrow),
            style = ClockwiseTextStyles.Eyebrow,
            color = ClockwiseColors.MutedForeground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.planner_title),
            style = ClockwiseTextStyles.ScreenTitle,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.height(24.dp))

        if (planner.sources.isNotEmpty()) {
            Text(
                text = stringResource(R.string.planner_source_label),
                style = ClockwiseTextStyles.SectionLabel,
                color = ClockwiseColors.MutedForeground,
            )
            Spacer(Modifier.height(8.dp))
            PlannerSourceRow(
                sources = planner.sources,
                selectedCityId = planner.sourceCityId,
                onSourceChange = onSourceChange,
            )
            Spacer(Modifier.height(20.dp))
        } else {
            // Keep the old rhythm when there is nothing to convert between.
            Spacer(Modifier.height(4.dp))
        }

        Text(
            text = planner.sourceName
                ?.takeIf { planner.sourceCityId != null }
                ?.let { stringResource(R.string.planner_time_in, it.uppercase()) }
                ?: stringResource(R.string.planner_local_label),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = planner.referenceTimeText,
            style = ClockwiseTextStyles.PlannerClock,
            color = ClockwiseColors.Foreground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.planner_reference_meta,
                planner.referenceDateText,
                planner.referenceOffsetLabel,
            ),
            style = ClockwiseTextStyles.DetailLocalZone,
            color = ClockwiseColors.MutedForeground,
        )

        Spacer(Modifier.height(24.dp))

        DaySlider(
            minuteOfDay = planner.minuteOfDay,
            readout = planner.referenceTimeText,
            onMinuteChange = onMinuteChange,
            contentDescription = stringResource(R.string.a11y_meeting_time_slider),
        )

        Spacer(Modifier.height(12.dp))

        WorkingHoursLegend()

        Spacer(Modifier.height(28.dp))

        HorizontalDivider(color = ClockwiseColors.Border)

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.planner_section_label),
                style = ClockwiseTextStyles.SectionLabel,
                color = ClockwiseColors.MutedForeground,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (planner.rows.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                PlannerBestAction(planner = planner, onMinuteChange = onMinuteChange)
            }
        }

        Spacer(Modifier.height(4.dp))

        if (planner.rows.isEmpty()) {
            PlannerEmptyState(onFindCityClick = onFindCityClick)
        } else {
            planner.rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(color = ClockwiseColors.Border)
                }
                PlannerRow(row = row)
            }
        }
    }
}

/**
 * The zones the slot can be expressed in, device first. Horizontally scrolling
 * rather than wrapping so a long city list keeps one predictable row and the
 * heading below it never jumps.
 */
@Composable
private fun PlannerSourceRow(
    sources: List<PlannerSource>,
    selectedCityId: String?,
    onSourceChange: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        sources.forEach { source ->
            PlannerSourceChip(
                source = source,
                selected = source.cityId == selectedCityId,
                onClick = { onSourceChange(source.cityId) },
                modifier = Modifier.testTag(PlannerTestTags.sourceChip(source.cityId)),
            )
        }
    }
}

@Composable
private fun PlannerSourceChip(
    source: PlannerSource,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val device = source.cityId == null
    val label = if (device) {
        stringResource(R.string.planner_source_device)
    } else {
        source.name.uppercase()
    }
    val description = if (device) {
        stringResource(R.string.a11y_planner_source_device)
    } else {
        stringResource(R.string.a11y_planner_source, source.name)
    }

    Box(
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .clip(ClockwiseShapes.small)
            .background(if (selected) ClockwiseColors.Primary else ClockwiseColors.Card)
            .border(
                width = 1.dp,
                color = if (selected) ClockwiseColors.Primary else ClockwiseColors.Border,
                shape = ClockwiseShapes.small,
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ClockwiseTextStyles.PlannerSourceChip,
            color = if (selected) {
                ClockwiseColors.PrimaryForeground
            } else {
                ClockwiseColors.Foreground
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The recommendation, in the section header where it applies. With a slot it is
 * a button that moves the slider; without one it is a plain status, because an
 * unclickable "no overlap" is information rather than an action.
 */
@Composable
private fun PlannerBestAction(
    planner: PlannerUiState,
    onMinuteChange: (MinuteOfDay) -> Unit,
) {
    val best = planner.bestSlot
    if (best == null) {
        val description = stringResource(R.string.a11y_planner_no_overlap)
        Box(
            modifier = Modifier
                .testTag(PlannerTestTags.BEST_ACTION)
                .heightIn(min = MinTouchTarget)
                .clip(ClockwiseShapes.small)
                .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.small)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.planner_no_overlap),
                style = ClockwiseTextStyles.SectionLabel,
                color = ClockwiseColors.MutedForeground,
                maxLines = 1,
            )
        }
        return
    }

    val label = if (planner.bestTimeText.isNotEmpty()) {
        stringResource(R.string.planner_best_time, planner.bestTimeText)
    } else {
        stringResource(R.string.planner_best_fallback)
    }
    val description = stringResource(
        R.string.a11y_planner_best,
        planner.bestTimeText,
        planner.bestWorkingCount,
    )

    Box(
        modifier = Modifier
            .testTag(PlannerTestTags.BEST_ACTION)
            .heightIn(min = MinTouchTarget)
            .clip(ClockwiseShapes.small)
            .border(1.dp, ClockwiseColors.Primary, ClockwiseShapes.small)
            .clickable(
                onClickLabel = stringResource(R.string.a11y_planner_apply_best),
                role = Role.Button,
                onClick = { onMinuteChange(best.minuteOfDay) },
            )
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.Primary,
            maxLines = 1,
        )
    }
}

/**
 * Explains the shaded band and the marker on each row. The working day is the
 * planner's own judgement, so it is stated rather than applied silently.
 */
@Composable
private fun WorkingHoursLegend() {
    val range = stringResource(
        R.string.planner_working_range,
        WORKING_DAY_START_HOUR,
        WORKING_DAY_END_HOUR,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LegendItem(
            filled = true,
            label = stringResource(R.string.planner_legend_working, range),
        )
        LegendItem(
            filled = false,
            label = stringResource(R.string.planner_legend_outside),
        )
    }
}

@Composable
private fun LegendItem(filled: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WorkingHoursMarker(filled = filled)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = ClockwiseTextStyles.DstCaption,
            color = ClockwiseColors.MutedForeground,
        )
    }
}

/** Filled inside the working day, outlined outside it. */
@Composable
private fun WorkingHoursMarker(filled: Boolean) {
    Box(
        modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(if (filled) ClockwiseColors.Primary else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (filled) ClockwiseColors.Primary else ClockwiseColors.MutedForeground,
                shape = CircleShape,
            ),
    )
}

/**
 * One city at the chosen slot. Informational, so it is a single merged node:
 * the time, the weekday and whether it is a civilised hour, in one swipe.
 */
@Composable
private fun PlannerRow(row: PlannerCityRow) {
    val workingLabel = stringResource(R.string.planner_within_hours)
    val outsideLabel = stringResource(R.string.planner_outside_hours)
    val description = stringResource(
        R.string.a11y_planner_row,
        row.displayName,
        // The weekday is part of the reading, not decoration: without it a
        // screen-reader user cannot tell that California is still on Friday.
        "${row.weekdayLabel} ${row.timeText}",
        if (row.isWorkingHours) workingLabel else outsideLabel,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.displayName,
                style = ClockwiseTextStyles.PlannerRowName,
                color = ClockwiseColors.Foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = timeDifferenceLabel(row.difference),
                style = ClockwiseTextStyles.PlannerRowDifference,
                color = ClockwiseColors.MutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = row.weekdayLabel,
            style = ClockwiseTextStyles.CityWeekday,
            color = ClockwiseColors.MutedForeground,
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = row.timeText,
            style = ClockwiseTextStyles.PlannerRowTime,
            // A time outside the working day is drawn quietly rather than
            // hidden: it is still the answer, just not a convenient one.
            color = if (row.isWorkingHours) {
                ClockwiseColors.Foreground
            } else {
                ClockwiseColors.MutedForeground
            },
        )

        Spacer(Modifier.width(10.dp))

        WorkingHoursMarker(filled = row.isWorkingHours)
    }
}

@Composable
private fun PlannerEmptyState(onFindCityClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.planner_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = ClockwiseColors.Foreground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.planner_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = ClockwiseColors.MutedForeground,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .clip(ClockwiseShapes.medium)
                .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
                .clickable(onClick = onFindCityClick, role = Role.Button)
                .heightIn(min = MinTouchTarget)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = ClockwiseColors.Foreground,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.planner_empty_action),
                style = ClockwiseTextStyles.CityName,
                color = ClockwiseColors.Foreground,
            )
        }
    }
}

@Preview(name = "Meeting planner 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun MeetingPlannerScreenPreview() {
    FixedClockwiseTheme {
        MeetingPlannerScreen(
            planner = PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(9, 0)),
            onMinuteChange = {},
            onSourceChange = {},
            onBack = {},
            onFindCityClick = {},
        )
    }
}

@Preview(name = "Meeting planner Cool Slate 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun MeetingPlannerScreenDarkPreview() {
    FixedClockwiseTheme(theme = AppTheme.COOL_SLATE) {
        MeetingPlannerScreen(
            planner = PreviewSamples.plannerState(minuteOfDay = MinuteOfDay.of(18, 30)),
            onMinuteChange = {},
            onSourceChange = {},
            onBack = {},
            onFindCityClick = {},
        )
    }
}

@Preview(name = "Meeting planner empty 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun MeetingPlannerScreenEmptyPreview() {
    FixedClockwiseTheme {
        MeetingPlannerScreen(
            planner = PreviewSamples.emptyPlannerState(),
            onMinuteChange = {},
            onSourceChange = {},
            onBack = {},
            onFindCityClick = {},
        )
    }
}
