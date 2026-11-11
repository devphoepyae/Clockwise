package com.pj95.clockwise.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.domain.map.MapProjectionMode
import com.pj95.clockwise.model.solar.SolarDayKind
import com.pj95.clockwise.model.time.MinuteOfDay
import com.pj95.clockwise.ui.MapSolarSummary
import com.pj95.clockwise.ui.MapUiState
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.components.CityCardModel
import com.pj95.clockwise.ui.components.DaySlider
import com.pj95.clockwise.ui.components.MinTouchTarget
import com.pj95.clockwise.ui.components.WorldMapCanvas
import com.pj95.clockwise.ui.components.WorldMapTestTags
import com.pj95.clockwise.ui.components.timeDifferenceLabel
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles

/**
 * "World Time" — the schematic world map (spec sections 4.4 and 5.14).
 *
 * Shows the saved cities as markers, highlights the active one with a red
 * meridian and diamond, and surfaces the selected city's clock and solar day.
 * The time-of-day slider moves the map through the device's current day: the
 * Sun marker and the night shading move while the cities stay put. Tapping a
 * marker selects that city; tapping the clock panel opens its detail screen.
 *
 * The screen is stateless. The route owns the travel minute, so leaving the map
 * and returning can restore it; `NOW` hands the map back to the live clock.
 */
@Composable
fun WorldMapScreen(
    map: MapUiState,
    onSelectCity: (String) -> Unit,
    onOpenCity: (String) -> Unit,
    onAddClick: () -> Unit,
    onFindCityClick: () -> Unit,
    onTimeChange: (MinuteOfDay) -> Unit,
    onGoLive: () -> Unit,
    modifier: Modifier = Modifier,
    /** False hides the selected city's comparison line, matching Settings. */
    showDifference: Boolean = true,
) {
    val activeCity = map.activeCity
    var projection by rememberSaveable { mutableStateOf(MapProjectionMode.FLAT) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClockwiseColors.Background)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        if (activeCity != null) {
            MapCityStrip(
                cities = map.cities,
                activeCityId = activeCity.id,
                onSelectCity = onSelectCity,
                onAddClick = onAddClick,
            )

            Spacer(Modifier.height(16.dp))

            MapSelectedCityPanel(
                city = activeCity,
                solar = map.solar,
                onOpenCity = { onOpenCity(activeCity.id) },
                showDifference = showDifference,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        } else {
            MapHeader(
                onAddClick = onAddClick,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        MapProjectionToggle(
            projection = projection,
            onProjectionChange = { projection = it },
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(8.dp))

        WorldMapCanvas(
            cities = map.cities,
            now = map.instant,
            onCityClick = { onSelectCity(it.id) },
            projection = projection,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .aspectRatio(if (projection == MapProjectionMode.GLOBE) 1f else 1.7f)
                .testTag(WorldMapTestTags.MAP_CANVAS),
        )

        Spacer(Modifier.height(12.dp))

        if (map.cities.isEmpty()) {
            MapEmptyState(onFindCityClick = onFindCityClick)
        } else {
            if (map.cities.size > 1) {
                MapComparisonTray(
                    cities = map.cities,
                    activeCityId = activeCity?.id,
                    onSelectCity = onSelectCity,
                )
                Spacer(Modifier.height(10.dp))
            }
            MapTimeTravel(
                map = map,
                onMinuteChange = onTimeChange,
                onGoLive = onGoLive,
            )
            Spacer(Modifier.height(8.dp))
            MapLegend()
            Spacer(Modifier.height(8.dp))
            MapCaption()
        }
    }
}

/**
 * The 2D / 3D switch. A radio group rather than a toggle so a screen reader
 * hears "flat map, selected" and "globe" instead of an ambiguous switch state.
 */
@Composable
private fun MapProjectionToggle(
    projection: MapProjectionMode,
    onProjectionChange: (MapProjectionMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.map_view_label),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .clip(ClockwiseShapes.small)
                .background(ClockwiseColors.Card)
                .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.small)
                .padding(2.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ProjectionOption(
                label = stringResource(R.string.map_view_flat),
                description = stringResource(R.string.a11y_map_view_flat),
                selected = projection == MapProjectionMode.FLAT,
                onClick = { onProjectionChange(MapProjectionMode.FLAT) },
                testTag = WorldMapTestTags.PROJECTION_FLAT,
            )
            ProjectionOption(
                label = stringResource(R.string.map_view_globe),
                description = stringResource(R.string.a11y_map_view_globe),
                selected = projection == MapProjectionMode.GLOBE,
                onClick = { onProjectionChange(MapProjectionMode.GLOBE) },
                testTag = WorldMapTestTags.PROJECTION_GLOBE,
            )
        }
    }
}

@Composable
private fun ProjectionOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Box(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .testTag(testTag)
            .clip(ClockwiseShapes.small)
            .background(if (selected) ClockwiseColors.Primary else ClockwiseColors.Card)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = description }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ClockwiseTextStyles.SectionLabel,
            color = if (selected) ClockwiseColors.PrimaryForeground else ClockwiseColors.Foreground,
        )
    }
}

/**
 * A horizontally scrolling tray of every saved city, so two places can be
 * compared without changing the active marker. Each card reports its difference
 * from the device's own clock, matching the city list.
 */
@Composable
private fun MapComparisonTray(
    cities: List<CityCardModel>,
    activeCityId: String?,
    onSelectCity: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .testTag(WorldMapTestTags.COMPARISON_TRAY),
    ) {
        Text(
            text = stringResource(R.string.map_compare_label),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(cities, key = { it.id }) { city ->
                ComparisonCard(
                    city = city,
                    selected = city.id == activeCityId,
                    onClick = { onSelectCity(city.id) },
                )
            }
        }
    }
}

@Composable
private fun ComparisonCard(
    city: CityCardModel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val difference = timeDifferenceLabel(city.difference)
    val description = stringResource(
        R.string.a11y_map_compare_city,
        city.name,
        city.timeText,
        difference,
    )
    val container = if (selected) ClockwiseColors.Primary else ClockwiseColors.Card
    val content = if (selected) {
        ClockwiseColors.PrimaryForeground
    } else {
        ClockwiseColors.Foreground
    }
    val muted = if (selected) ClockwiseColors.OnPrimaryMuted else ClockwiseColors.MutedForeground

    Column(
        modifier = Modifier
            .widthIn(min = 112.dp)
            .testTag(WorldMapTestTags.comparisonCard(city.id))
            .clip(ClockwiseShapes.small)
            .background(container)
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
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = city.displayName.uppercase(),
            style = ClockwiseTextStyles.MapStripLabel,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // The card is announced as one label; the fragments below must not
            // appear as separate nodes or they read as duplicates.
            modifier = Modifier.clearAndSetSemantics { },
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = city.timeText,
            style = ClockwiseTextStyles.CityName,
            color = content,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Text(
            text = difference,
            style = ClockwiseTextStyles.MapCityDifference,
            color = muted,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { },
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MapHeader(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.map_eyebrow),
                style = ClockwiseTextStyles.Eyebrow,
                color = ClockwiseColors.MutedForeground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.map_title),
                style = ClockwiseTextStyles.ScreenTitle,
                color = ClockwiseColors.Foreground,
            )
        }
        CircleIconButton(
            iconRes = R.drawable.ic_add,
            contentDescription = stringResource(R.string.a11y_add_city),
            onClick = onAddClick,
            size = 44.dp,
            iconSize = 20.dp,
        )
    }
}

/**
 * Horizontal strip of the saved cities. Behaves like a radio group: exactly one
 * city is active, and the whole strip is exposed to assistive technology as a
 * single selection group.
 */
@Composable
private fun MapCityStrip(
    cities: List<CityCardModel>,
    activeCityId: String?,
    onSelectCity: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Keep the active city on screen: the strip is scrollable and the active
    // city may be the last one.
    LaunchedEffect(activeCityId, cities.size) {
        val index = cities.indexOfFirst { it.id == activeCityId }
        if (index >= 0) listState.scrollToItem(index)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            items(items = cities, key = { it.id }) { city ->
                CityStripPill(
                    city = city,
                    onClick = { onSelectCity(city.id) },
                    modifier = Modifier.testTag(WorldMapTestTags.cityPill(city.id)),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        CircleIconButton(
            iconRes = R.drawable.ic_add,
            contentDescription = stringResource(R.string.a11y_add_city),
            onClick = onAddClick,
            size = 44.dp,
            iconSize = 20.dp,
        )
    }
}

@Composable
private fun CityStripPill(
    city: CityCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = if (city.isActive) ClockwiseColors.Primary else ClockwiseColors.Card
    val content = if (city.isActive) {
        ClockwiseColors.PrimaryForeground
    } else {
        ClockwiseColors.Foreground
    }

    Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(ClockwiseShapes.small)
            .background(container)
            .border(
                width = 1.dp,
                color = if (city.isActive) ClockwiseColors.Primary else ClockwiseColors.Border,
                shape = ClockwiseShapes.small,
            )
            .selectable(
                selected = city.isActive,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = city.displayName.uppercase(),
            style = ClockwiseTextStyles.MapStripLabel,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The selected city's live clock. Tapping it opens the city detail screen.
 */
@Composable
private fun MapSelectedCityPanel(
    city: CityCardModel,
    solar: MapSolarSummary?,
    onOpenCity: () -> Unit,
    modifier: Modifier = Modifier,
    showDifference: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(WorldMapTestTags.SELECTED_CITY_PANEL)
            .clip(ClockwiseShapes.medium)
            .clickable(
                onClickLabel = stringResource(R.string.a11y_open_city_detail, city.name),
                onClick = onOpenCity,
            )
            .padding(vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (city.isDaytime) R.drawable.ic_sun else R.drawable.ic_moon,
                        ),
                        contentDescription = stringResource(
                            if (city.isDaytime) R.string.a11y_daytime else R.string.a11y_nighttime,
                        ),
                        tint = ClockwiseColors.MutedForeground,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "${city.offsetLabel} · ${city.weekdayLabel}",
                        style = ClockwiseTextStyles.MapCityOffset,
                        color = ClockwiseColors.MutedForeground,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = city.timeText,
                    style = ClockwiseTextStyles.MapCityTime,
                    color = ClockwiseColors.Foreground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = city.displayName,
                    style = ClockwiseTextStyles.MapCityName,
                    color = ClockwiseColors.Foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = city.region,
                    style = ClockwiseTextStyles.MapCityRegion,
                    color = ClockwiseColors.MutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showDifference) {
                    Text(
                        text = timeDifferenceLabel(city.difference),
                        style = ClockwiseTextStyles.MapCityDifference,
                        color = ClockwiseColors.MutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.map_view_details),
                    style = ClockwiseTextStyles.SectionLabel,
                    color = ClockwiseColors.MutedForeground,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    tint = ClockwiseColors.MutedForeground,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(-90f),
                )
            }
        }

        // The solar line gets the panel's full width: inside the city column it
        // would be squeezed by "View details" and ellipsised at 360 dp.
        if (solar != null) {
            Spacer(Modifier.height(6.dp))
            SolarLine(cityName = city.name, solar = solar)
        }
    }
}

/**
 * The selected city's sunrise and sunset on the date the map is showing.
 *
 * Merged into one node: "sunrise 06:47, sunset 16:52 in Los Angeles" is the
 * useful sentence, not three disconnected fragments.
 */
@Composable
private fun SolarLine(
    cityName: String,
    solar: MapSolarSummary,
) {
    val text = when (solar.kind) {
        SolarDayKind.NORMAL -> stringResource(
            R.string.map_solar_normal,
            solar.sunriseText ?: "—",
            solar.sunsetText ?: "—",
        )
        SolarDayKind.POLAR_DAY -> stringResource(R.string.map_solar_polar_day)
        SolarDayKind.POLAR_NIGHT -> stringResource(R.string.map_solar_polar_night)
    }
    val description = when (solar.kind) {
        SolarDayKind.NORMAL -> stringResource(
            R.string.a11y_map_solar,
            solar.sunriseText ?: "—",
            solar.sunsetText ?: "—",
            cityName,
        )
        SolarDayKind.POLAR_DAY -> stringResource(R.string.a11y_map_solar_polar_day, cityName)
        SolarDayKind.POLAR_NIGHT -> stringResource(R.string.a11y_map_solar_polar_night, cityName)
    }

    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(
                if (solar.kind == SolarDayKind.POLAR_NIGHT) R.drawable.ic_moon else R.drawable.ic_sun,
            ),
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = text,
            style = ClockwiseTextStyles.MapCityDifference,
            color = ClockwiseColors.MutedForeground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The map's time-of-day control.
 *
 * The label states whether the map is live or travelling, so the difference is
 * never carried by colour alone. The slider spans the device's current day and
 * only snaps when the user drags it; while live, the thumb sits at the exact
 * minute and the clock keeps ticking behind it.
 */
@Composable
private fun MapTimeTravel(
    map: MapUiState,
    onMinuteChange: (MinuteOfDay) -> Unit,
    onGoLive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Row(
            modifier = Modifier.heightIn(min = MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (map.isLive) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ClockwiseColors.Foreground),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.map_live),
                    style = ClockwiseTextStyles.SectionLabel,
                    color = ClockwiseColors.Foreground,
                )
            } else {
                Text(
                    text = stringResource(R.string.map_time_travel),
                    style = ClockwiseTextStyles.SectionLabel,
                    color = ClockwiseColors.Foreground,
                )
            }

            Spacer(Modifier.weight(1f))

            if (!map.isLive) {
                NowAction(onGoLive = onGoLive)
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = map.deviceTimeText,
                style = ClockwiseTextStyles.DetailLocalTime,
                color = ClockwiseColors.Foreground,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    R.string.map_device_meta,
                    map.deviceDateText,
                    map.deviceOffsetLabel,
                ),
                style = ClockwiseTextStyles.MapCityOffset,
                color = ClockwiseColors.MutedForeground,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }

        Spacer(Modifier.height(2.dp))

        DaySlider(
            minuteOfDay = map.minuteOfDay,
            readout = map.deviceTimeText,
            onMinuteChange = onMinuteChange,
            contentDescription = stringResource(R.string.a11y_map_time_slider),
            showWorkingDay = false,
        )
    }
}

/** "NOW": hands the map back to the live clock. */
@Composable
private fun NowAction(
    onGoLive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .widthIn(min = MinTouchTarget)
            .heightIn(min = MinTouchTarget)
            .clip(ClockwiseShapes.small)
            .clickable(
                onClickLabel = stringResource(R.string.map_now_action),
                role = Role.Button,
                onClick = onGoLive,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.map_now),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.Foreground,
        )
    }
}

@Composable
private fun MapCaption() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.map_caption),
            style = ClockwiseTextStyles.MapCaption,
            color = ClockwiseColors.MutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Key for the map: what the red diamond, the dark dot and the shading mean.
 * Text labels rather than colour alone, so the meaning does not depend on
 * colour perception.
 */
@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(labelRes = R.string.map_legend_selected) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .rotate(45f)
                    .background(ClockwiseColors.MapAccent),
            )
        }
        LegendSpacer()
        LegendItem(labelRes = R.string.map_legend_saved) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(ClockwiseColors.Foreground),
            )
        }
        LegendSpacer()
        LegendItem(labelRes = R.string.map_legend_night) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    // The wash is drawn over the map, not over the panel it is
                    // listed on, so on the darkest themes the swatch would
                    // otherwise be a black dot on a black background.
                    .background(ClockwiseColors.MapNight)
                    .border(1.dp, ClockwiseColors.Border, CircleShape),
            )
        }
    }
}

@Composable
private fun LegendSpacer() {
    Spacer(Modifier.width(16.dp))
}

@Composable
private fun LegendItem(
    labelRes: Int,
    swatch: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        swatch()
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(labelRes),
            style = ClockwiseTextStyles.MapCaption,
            color = ClockwiseColors.MutedForeground,
            maxLines = 1,
        )
    }
}

@Composable
private fun MapEmptyState(onFindCityClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.map_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = ClockwiseColors.Foreground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.map_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = ClockwiseColors.MutedForeground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(ClockwiseShapes.medium)
                .background(ClockwiseColors.Card)
                .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
                .clickable(
                    onClickLabel = stringResource(R.string.clocks_empty_action),
                    role = Role.Button,
                    onClick = onFindCityClick,
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(ClockwiseColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = ClockwiseColors.Foreground,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.map_empty_action),
                style = ClockwiseTextStyles.CityName,
                color = ClockwiseColors.Foreground,
            )
        }
    }
}
