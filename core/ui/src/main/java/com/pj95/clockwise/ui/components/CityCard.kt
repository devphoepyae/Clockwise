package com.pj95.clockwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.time.TimeDifference
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles

/** Test hooks for the city list; the ids match the saved-city ids. */
object CityCardTestTags {
    fun card(cityId: String): String = "cityCard_$cityId"
}

/**
 * Everything a city card needs to render.
 *
 * The clock list and the world map share this model: the list reads the top
 * block, the map uses [latitude]/[longitude] to place the marker and the
 * selected-city card.
 */
@Immutable
data class CityCardModel(
    val id: String,
    /** e.g. "UTC+9" */
    val offsetLabel: String,
    /** e.g. "Thu" */
    val weekdayLabel: String,
    val name: String,
    val region: String,
    /** e.g. "01:40" */
    val timeText: String,
    val isDaytime: Boolean,
    val isActive: Boolean,
    /** The user's own label for this city, e.g. "Head Office"; `null` when unset. */
    val label: String? = null,
    /** How this city's clock compares with the device's own, in whole minutes. */
    val difference: TimeDifference = TimeDifference.None,
    /** Degrees north of the equator; used only to place the map marker. */
    val latitude: Double = 0.0,
    /** Degrees east of the prime meridian; used only to place the map marker. */
    val longitude: Double = 0.0,
) {
    /** "Tokyo (Head Office)" when a label is set, otherwise just the name. */
    val displayName: String get() = label?.let { "$name ($it)" } ?: name
}

/**
 * A single row in the city list.
 *
 * The active city is drawn as a black card with light text; every other city
 * uses the card colour with a hairline border.
 *
 * The bottom-right line compares the city with the device's own time, so the
 * card answers "how far from me is this?" without opening anything. The line is
 * omitted when the user turns that comparison off in Settings.
 */
@Composable
fun CityCard(
    model: CityCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** False hides the difference line; the region stays where it is. */
    showDifference: Boolean = true,
    /**
     * Accessibility reordering. A screen reader cannot drag a card, so the
     * same reorder is offered as two actions on the card itself. `null` when
     * the city is already at that end of the list.
     */
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    /** Accessibility equivalent of the horizontal swipe. */
    onDelete: (() -> Unit)? = null,
) {
    val containerColor = if (model.isActive) ClockwiseColors.Primary else ClockwiseColors.Card
    val contentColor =
        if (model.isActive) ClockwiseColors.PrimaryForeground else ClockwiseColors.Foreground
    val supportingColor =
        if (model.isActive) ClockwiseColors.OnPrimaryMuted else ClockwiseColors.MutedForeground

    val differenceText = timeDifferenceLabel(model.difference)
    val differenceDescription =
        stringResource(R.string.a11y_difference_from_your_time, differenceText)
    val moveUpLabel = stringResource(R.string.a11y_move_city_up)
    val moveDownLabel = stringResource(R.string.a11y_move_city_down)
    val deleteLabel = stringResource(R.string.a11y_remove_city)
    val moveActions = buildList {
        if (onMoveUp != null) {
            add(
                CustomAccessibilityAction(moveUpLabel) {
                    onMoveUp()
                    true
                },
            )
        }
        if (onMoveDown != null) {
            add(
                CustomAccessibilityAction(moveDownLabel) {
                    onMoveDown()
                    true
                },
            )
        }
        if (onDelete != null) {
            add(
                CustomAccessibilityAction(deleteLabel) {
                    onDelete()
                    true
                },
            )
        }
    }
    val reorderable = onMoveUp != null || onMoveDown != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 93.dp)
            .testTag(CityCardTestTags.card(model.id))
            .clip(ClockwiseShapes.medium)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (model.isActive) ClockwiseColors.Primary else ClockwiseColors.Border,
                shape = ClockwiseShapes.medium,
            )
            .semantics {
                selected = model.isActive
                if (moveActions.isNotEmpty()) customActions = moveActions
            }
            .clickable(
                onClickLabel = stringResource(R.string.a11y_open_city_detail, model.name),
                onClick = onClick,
            )
            // 14 dp rather than 18 dp. The difference line makes the card taller
            // than it used to be, and with the min-height clamp the card still
            // measures about 93 dp.
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        // Three independent lines rather than one shared grid: a long label or a
        // long difference then only ever shortens itself, never the city name.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = model.offsetLabel,
                style = ClockwiseTextStyles.CityOffset,
                color = supportingColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (model.isDaytime) R.drawable.ic_sun else R.drawable.ic_moon,
                    ),
                    contentDescription = stringResource(
                        if (model.isDaytime) R.string.a11y_daytime else R.string.a11y_nighttime,
                    ),
                    tint = supportingColor,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = model.weekdayLabel,
                    style = ClockwiseTextStyles.CityWeekday,
                    color = supportingColor,
                )
            }
            if (reorderable) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_drag_handle),
                    // The card's custom actions carry the meaning; the grip is
                    // only the visual hint that the row can be dragged.
                    contentDescription = null,
                    tint = supportingColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(3.dp))

        Row {
            Text(
                text = model.displayName,
                style = ClockwiseTextStyles.CityName,
                color = contentColor,
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = model.timeText,
                style = ClockwiseTextStyles.CityTime,
                color = contentColor,
                modifier = Modifier.alignByBaseline(),
            )
        }

        Spacer(Modifier.height(2.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = model.region,
                style = ClockwiseTextStyles.CityRegion,
                color = supportingColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showDifference) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = differenceText,
                    style = ClockwiseTextStyles.CityDifference,
                    color = supportingColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // Spoken in full, because "+2 hrs ahead" on its own does not
                    // say what it is ahead of.
                    modifier = Modifier.semantics {
                        contentDescription = differenceDescription
                    },
                )
            }
        }
    }
}
