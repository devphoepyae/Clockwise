package com.pj95.clockwise.ui.screens

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.model.time.MarketSessions
import com.pj95.clockwise.ui.DeviceLocalTime
import com.pj95.clockwise.ui.ClockwiseUiState
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.components.CityCard
import com.pj95.clockwise.ui.components.CityCardModel
import com.pj95.clockwise.ui.components.MinTouchTarget
import com.pj95.clockwise.ui.components.SwipeToDeleteBox
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.LocalClockwisePalette
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import java.time.Instant
import java.time.ZoneId

/**
 * "World Time" — the saved city list (spec section 4.1).
 *
 * Handles the loading, populated and empty states.
 */
@Composable
fun ClocksScreen(
    state: ClockwiseUiState,
    onCityClick: (CityCardModel) -> Unit,
    onAddClick: () -> Unit,
    onFindCityClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlanMeetingClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Opens the Chronos widget gallery from the header shortcut. */
    onWidgetGalleryClick: () -> Unit = {},
    /** Persists a new display order after a drag or an accessibility move. */
    onReorderCities: (List<String>) -> Unit = {},
    /** Removes a city after a swipe or the accessibility delete action. */
    onDeleteCity: (CityCardModel) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val savedOrder = state.cities.map { it.id }

    // The list is arranged locally while a drag is in flight. The persisted
    // state must not receive an order on every pixel of movement; it receives
    // one order, once, when the finger lifts.
    var localOrder by remember { mutableStateOf(savedOrder) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(savedOrder) {
        if (draggingId == null) localOrder = savedOrder
    }

    val citiesById = state.cities.associateBy { it.id }
    val displayedCities = localOrder.mapNotNull(citiesById::get) +
        state.cities.filterNot { it.id in localOrder }

    fun moveCity(cityId: String, targetIndex: Int) {
        val ids = displayedCities.map { it.id }.toMutableList()
        val from = ids.indexOf(cityId)
        if (from < 0 || targetIndex !in ids.indices || from == targetIndex) return
        ids.add(targetIndex, ids.removeAt(from))
        onReorderCities(ids)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(ClockwiseColors.Background),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 16.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ClocksHeader(
                onSettingsClick = onSettingsClick,
                onWidgetGalleryClick = onWidgetGalleryClick,
            )
        }

        item {
            ClocksSectionHeader(
                cityCount = state.cities.size,
                deviceLocal = state.deviceLocal,
            )
        }

        item {
            MarketSessionsStrip(
                cities = state.marketCities,
                now = state.now,
                onCityClick = onCityClick,
            )
        }

        item { PlanMeetingRow(onClick = onPlanMeetingClick) }

        when {
            state.isLoading -> item { LoadingState() }

            state.cities.isEmpty() -> item { EmptyState(onFindCityClick = onFindCityClick) }

            else -> {
                items(items = displayedCities, key = { it.id }) { city ->
                    val index = displayedCities.indexOfFirst { it.id == city.id }
                    val isDragging = city.id == draggingId
                    val canDrag = displayedCities.size > 1

                    val reorderModifier = if (canDrag) {
                        Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffset else 0f
                            }
                            .pointerInput(city.id, displayedCities.size) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = city.id
                                        dragOffset = 0f
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, amount ->
                                        if (draggingId != city.id) return@detectDragGesturesAfterLongPress
                                        change.consume()
                                        dragOffset += amount.y

                                        val info = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { it.key == city.id }
                                            ?: return@detectDragGesturesAfterLongPress
                                        val center = info.offset + info.size / 2f + dragOffset
                                        val target = listState.layoutInfo.visibleItemsInfo
                                            .firstOrNull {
                                                center >= it.offset && center <= it.offset + it.size
                                            }
                                            ?: return@detectDragGesturesAfterLongPress
                                        val targetId = target.key as? String
                                            ?: return@detectDragGesturesAfterLongPress
                                        if (targetId == city.id) return@detectDragGesturesAfterLongPress

                                        val from = localOrder.indexOf(city.id)
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
                                        if (draggingId != city.id) {
                                            return@detectDragGesturesAfterLongPress
                                        }
                                        val finalOrder = localOrder
                                        draggingId = null
                                        dragOffset = 0f
                                        if (finalOrder != savedOrder) {
                                            onReorderCities(finalOrder)
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

                    SwipeToDeleteBox(onDelete = { onDeleteCity(city) }) {
                        CityCard(
                            model = city,
                            onClick = { onCityClick(city) },
                            modifier = reorderModifier,
                            showDifference = state.compareWithLocalTime,
                            onMoveUp = if (index > 0) {
                                { moveCity(city.id, index - 1) }
                            } else {
                                null
                            },
                            onMoveDown = if (index < displayedCities.lastIndex) {
                                { moveCity(city.id, index + 1) }
                            } else {
                                null
                            },
                            onDelete = { onDeleteCity(city) },
                        )
                    }
                }
                item { AddCityRow(onClick = onAddClick) }
            }
        }

        item { DstCaption() }
    }
}

@Composable
private fun ClocksHeader(
    onSettingsClick: () -> Unit,
    onWidgetGalleryClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.clocks_eyebrow),
                style = ClockwiseTextStyles.Eyebrow,
                color = ClockwiseColors.MutedForeground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.clocks_title),
                style = ClockwiseTextStyles.ScreenTitle,
                color = ClockwiseColors.Foreground,
                modifier = Modifier.semantics { heading() },
            )
        }
        CircleIconButton(
            iconRes = R.drawable.ic_settings,
            contentDescription = stringResource(R.string.a11y_settings),
            onClick = onSettingsClick,
            size = 44.dp,
            iconSize = 20.dp,
        )
        Spacer(Modifier.width(8.dp))
        // The header's one creative action: straight into Chronos, where the
        // home-screen clocks live. Adding a city stays on the list's own ADD
        // CITY row, which is where a city action belongs.
        CircleIconButton(
            iconRes = R.drawable.ic_chronos_widget,
            contentDescription = stringResource(R.string.a11y_widget_gallery),
            onClick = onWidgetGalleryClick,
            size = 44.dp,
            iconSize = 20.dp,
        )
    }
}

@Composable
private fun ClocksSectionHeader(cityCount: Int, deviceLocal: DeviceLocalTime?) {
    val count = pluralStringResource(R.plurals.clocks_city_count, cityCount, cityCount)
    val caption = deviceLocal?.let { "$count · ${it.timeText}" } ?: count

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.clocks_section_label),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        Text(
            text = caption,
            style = ClockwiseTextStyles.SectionCount,
            color = ClockwiseColors.MutedForeground,
        )
    }
}

/**
 * The four financial centres, always in one row and always in the same order.
 *
 * This is the FOREX trader's shortcut into the app: the session dot answers
 * "is this market open?" at a glance, and a tap opens the city detail. The
 * status is computed locally from each city's own zone, so it follows daylight
 * saving without a network call.
 */
@Composable
private fun MarketSessionsStrip(
    cities: List<CityCardModel>,
    now: Instant,
    onCityClick: (CityCardModel) -> Unit,
) {
    if (cities.isEmpty()) return
    val palette = LocalClockwisePalette.current
    val openColor = if (palette.isDark) Color(0xFF66BB6A) else Color(0xFF2E7D32)
    val closedColor = ClockwiseColors.MutedForeground

    Column {
        Text(
            text = stringResource(R.string.clocks_markets_label),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cities.forEach { city ->
                val zone = CityDirectory.find(city.id)?.let { ZoneId.of(it.timeZone) }
                val open = zone != null && MarketSessions.isOpen(zone, now)
                val status = stringResource(
                    if (open) R.string.market_status_open
                    else R.string.market_status_closed,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(ClockwiseShapes.medium)
                        .background(ClockwiseColors.Card)
                        .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
                        .clickable(
                            onClickLabel = city.name,
                            onClick = { onCityClick(city) },
                        )
                        // The cell reads as one item, not three fragments. This
                        // also keeps the city card list the only place a bare
                        // city name is exposed to the rest of the screen.
                        .clearAndSetSemantics {
                            contentDescription = "${city.name}, $status, ${city.timeText}"
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (open) openColor else closedColor),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = status,
                            style = ClockwiseTextStyles.DstCaption.copy(letterSpacing = 0.8.sp),
                            color = if (open) openColor else closedColor,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = city.name,
                        style = ClockwiseTextStyles.CityName.copy(fontSize = 12.sp),
                        color = ClockwiseColors.Foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = city.timeText,
                        style = ClockwiseTextStyles.CityRegion,
                        color = ClockwiseColors.MutedForeground,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.clocks_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = ClockwiseColors.MutedForeground,
        )
    }
}

@Composable
private fun EmptyState(onFindCityClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_globe),
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.clocks_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = ClockwiseColors.Foreground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.clocks_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = ClockwiseColors.MutedForeground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .clip(ClockwiseShapes.medium)
                .clickable(onClick = onFindCityClick, role = Role.Button)
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
                text = stringResource(R.string.clocks_empty_action),
                style = ClockwiseTextStyles.CityName,
                color = ClockwiseColors.Foreground,
            )
        }
    }
}

@Composable
private fun AddCityRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ClockwiseShapes.medium)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .clickable(onClick = onClick)
            .heightIn(min = MinTouchTarget)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.clocks_add_city_action),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
        )
    }
}

/**
 * Entry point to the meeting planner.
 *
 * Placed directly under the section header rather than at the foot of the list,
 * because with a full list of cities an action down there would start life off
 * screen. It is deliberately styled like `ADD CITY` so the two read as a pair.
 */
@Composable
private fun PlanMeetingRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ClockwiseShapes.medium)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .clickable(
                onClickLabel = stringResource(R.string.a11y_meeting_planner),
                onClick = onClick,
            )
            .heightIn(min = MinTouchTarget)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_clock),
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.clocks_plan_action),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
        )
    }
}

@Composable
private fun DstCaption() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.clocks_dst_caption),
            style = ClockwiseTextStyles.DstCaption,
            color = ClockwiseColors.MutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(name = "Clocks 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ClocksScreenPreview() {
    FixedClockwiseTheme {
        ClocksScreen(
            state = PreviewSamples.clockState(),
            onCityClick = {},
            onAddClick = {},
            onFindCityClick = {},
            onSettingsClick = {},
            onPlanMeetingClick = {},
        )
    }
}

@Preview(name = "Clocks empty 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ClocksScreenEmptyPreview() {
    FixedClockwiseTheme {
        ClocksScreen(
            state = PreviewSamples.emptyState(),
            onCityClick = {},
            onAddClick = {},
            onFindCityClick = {},
            onSettingsClick = {},
            onPlanMeetingClick = {},
        )
    }
}
