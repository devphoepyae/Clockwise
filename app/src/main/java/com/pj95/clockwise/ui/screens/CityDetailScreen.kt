package com.pj95.clockwise.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.City
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.model.time.TimeDifference
import com.pj95.clockwise.ui.DeviceLocalTime
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.components.MinTouchTarget
import com.pj95.clockwise.ui.components.SegmentedOption
import com.pj95.clockwise.ui.components.ClockwiseSegmentedControl
import com.pj95.clockwise.ui.components.timeDifferenceLabel
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import java.time.Instant
import java.time.ZoneId

/**
 * City detail: the city's live clock, the 12h/24h switch, how it compares with
 * the device's own time, and the user's own label for it (spec section 4.3).
 */
@Composable
fun CityDetailScreen(
    city: City,
    now: Instant,
    use24Hour: Boolean,
    deviceLocal: DeviceLocalTime?,
    label: String?,
    onBack: () -> Unit,
    onToggleFormat: (Boolean) -> Unit,
    onEditLabel: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    /** False drops the comparison block, matching the Settings switch. */
    showDifference: Boolean = true,
) {
    val zoneId = remember(city.id) { ZoneId.of(city.timeZone) }

    val time = remember(city.id, now, use24Hour) {
        CityTimeFormatter.snapshot(zoneId = zoneId, instant = now, use24Hour = use24Hour)
    }

    // The device's own zone only leaves the loading state once, but it is keyed
    // so a travelling user gets the new difference as soon as the platform
    // reports it.
    val deviceZoneId = remember(deviceLocal?.zoneId) {
        deviceLocal?.let { ZoneId.of(it.zoneId) }
    }
    val difference = remember(city.id, now, deviceZoneId) {
        deviceZoneId?.let { TimeDifference.between(zoneId, it, now) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClockwiseColors.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                iconRes = R.drawable.ic_back,
                contentDescription = stringResource(R.string.detail_back),
                onClick = onBack,
                size = 44.dp,
                iconSize = 20.dp,
            )
            Spacer(Modifier.weight(1f))
            ClockwiseSegmentedControl(
                options = listOf(
                    SegmentedOption(value = false, label = stringResource(R.string.format_12h)),
                    SegmentedOption(value = true, label = stringResource(R.string.format_24h)),
                ),
                selectedValue = use24Hour,
                onValueSelected = onToggleFormat,
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = time.offsetLabel,
            style = ClockwiseTextStyles.CityOffset,
            color = ClockwiseColors.MutedForeground,
        )
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.Top) {
            Column {
                Text(
                    text = time.hourText,
                    style = ClockwiseTextStyles.DetailClock,
                    color = ClockwiseColors.Foreground,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = time.minuteText,
                        style = ClockwiseTextStyles.DetailClock,
                        color = ClockwiseColors.Foreground,
                    )
                    val periodText = time.periodText
                    if (periodText != null) {
                        Text(
                            text = periodText,
                            style = ClockwiseTextStyles.DetailClockPeriod,
                            color = ClockwiseColors.MutedForeground,
                            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.width(24.dp))

            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = "${time.weekdayShort},",
                    style = ClockwiseTextStyles.DetailDate,
                    color = ClockwiseColors.MutedForeground,
                )
                Text(
                    text = time.dateText,
                    style = ClockwiseTextStyles.DetailDate,
                    color = ClockwiseColors.MutedForeground,
                )
                Spacer(Modifier.height(10.dp))
                Icon(
                    painter = painterResource(
                        if (time.isDaytime) R.drawable.ic_sun else R.drawable.ic_moon,
                    ),
                    contentDescription = stringResource(
                        if (time.isDaytime) R.string.a11y_daytime else R.string.a11y_nighttime,
                    ),
                    tint = ClockwiseColors.MutedForeground,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = city.name,
            style = ClockwiseTextStyles.DetailCityName,
            color = ClockwiseColors.Foreground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = city.region,
            style = ClockwiseTextStyles.DetailCityRegion,
            color = ClockwiseColors.MutedForeground,
        )

        if (label != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.detail_city_label_format, label),
                style = ClockwiseTextStyles.DetailCityLabel,
                color = ClockwiseColors.Foreground,
            )
        }

        Spacer(Modifier.height(14.dp))
        EditLabelAction(cityName = city.name, hasLabel = label != null, onClick = onEditLabel)

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = ClockwiseColors.Border)
        Spacer(Modifier.height(20.dp))

        // The reference is named outright, because "+2 hrs ahead" on its own
        // does not say what it is ahead of.
        if (difference != null && showDifference) {
            Text(
                text = stringResource(R.string.detail_difference_label),
                style = ClockwiseTextStyles.SectionLabel,
                color = ClockwiseColors.MutedForeground,
                // A section header, so screen-reader users can jump between the
                // three blocks of this screen rather than swiping through them.
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = timeDifferenceLabel(difference),
                style = ClockwiseTextStyles.DetailDifference,
                color = ClockwiseColors.Foreground,
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = ClockwiseColors.Border)
            Spacer(Modifier.height(20.dp))
        }

        Text(
            text = stringResource(R.string.detail_local_time_label),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        if (deviceLocal != null) {
            Text(
                text = "${deviceLocal.zoneId} · ${deviceLocal.offsetLabel}",
                style = ClockwiseTextStyles.DetailLocalZone,
                color = ClockwiseColors.MutedForeground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = deviceLocal.timeText,
                style = ClockwiseTextStyles.DetailLocalTime,
                color = ClockwiseColors.Foreground,
            )
        }

        Spacer(Modifier.height(32.dp))

        OutlinedButton(
            onClick = onRemove,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = ClockwiseShapes.medium,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = ClockwiseColors.MapAccent,
            ),
            border = BorderStroke(1.dp, ClockwiseColors.MapAccent),
        ) {
            Text(
                text = stringResource(R.string.detail_remove),
                style = MaterialTheme.typography.labelLarge,
            )
        }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * "ADD LABEL" / "EDIT LABEL": a small text action under the city name, which is
 * where the label itself is shown. Kept as a quiet text row rather than a
 * button so the screen still has one clear primary action.
 */
@Composable
private fun EditLabelAction(
    cityName: String,
    hasLabel: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(ClockwiseShapes.small)
            .clickable(
                onClickLabel = stringResource(R.string.a11y_edit_city_label, cityName),
                role = Role.Button,
                onClick = onClick,
            )
            // The row is a bare text action, so its drawn height is small; the
            // target is padded out to the minimum a finger needs (spec 9).
            .heightIn(min = MinTouchTarget)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_edit),
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(if (hasLabel) R.string.label_edit else R.string.label_add),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
        )
    }
}

@Preview(name = "City detail 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun CityDetailScreenPreview() {
    val state = PreviewSamples.labelledState()
    FixedClockwiseTheme {
        CityDetailScreen(
            city = CityDirectory.find("tokyo")!!,
            now = state.now,
            use24Hour = state.use24Hour,
            deviceLocal = state.deviceLocal,
            label = "Head Office",
            onBack = {},
            onToggleFormat = {},
            onEditLabel = {},
            onRemove = {},
        )
    }
}
