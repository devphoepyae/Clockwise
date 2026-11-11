package com.pj95.clockwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles

/** One choice in a [ClockwiseSegmentedControl]. */
data class SegmentedOption<T>(val value: T, val label: String)
/**
 * Small monochrome segmented control, e.g. "12h | 24h".
 *
 * Exposes radio-button selection semantics so assistive technology announces
 * which option is active (spec section 9).
 *
 * Each option's target is at least [MinTouchTarget] tall, while the pill that is
 * drawn stays the height the label plus its padding asks for — the segmented
 * control is the smallest control in the app, so it is the one most worth
 * padding out to a comfortable size.
 */
@Composable
fun <T> ClockwiseSegmentedControl(
    options: List<SegmentedOption<T>>,
    selectedValue: T,
    onValueSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Accent)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .padding(3.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        options.forEach { option ->
            val isSelected = option.value == selectedValue
            Box(
                modifier = Modifier
                    .heightIn(min = MinTouchTarget)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onValueSelected(option.value) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clip(ClockwiseShapes.small)
                        .background(
                            if (isSelected) ClockwiseColors.Primary else Color.Transparent,
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.label,
                        style = ClockwiseTextStyles.SegmentLabel,
                        color = if (isSelected) {
                            ClockwiseColors.PrimaryForeground
                        } else {
                            ClockwiseColors.MutedForeground
                        },
                    )
                }
            }
        }
    }
}
