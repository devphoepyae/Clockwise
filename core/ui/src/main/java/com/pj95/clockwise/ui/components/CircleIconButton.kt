package com.pj95.clockwise.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.ui.theme.ClockwiseColors

/**
 * Smallest comfortable touch target, in dp (spec section 9).
 *
 * The accessibility guidelines put the floor at 48 dp. Controls that are drawn
 * smaller than that — the 44 dp circular buttons — keep their drawn size but are
 * given this much room to be hit, so the look of the design and the size of the
 * target stop being the same number.
 */
val MinTouchTarget = 48.dp

/**
 * Circular, icon-only control used by the bottom navigation and the "add city"
 * action.
 *
 * When [selected] is non-null the control is a toggle and exposes selection
 * semantics to assistive technology; when it is `null` the control is a plain
 * button.
 *
 * The circle is drawn at [size] but the node that carries the click and the
 * label is never smaller than [MinTouchTarget], so assistive technology sees one
 * control with one target rather than a ring with dead space around it.
 *
 * @param contentDescription accessibility label — icon-only controls must
 *   always have one (spec section 9)
 */
@Composable
fun CircleIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val isSelected = selected == true
    val resolvedContainer = containerColor
        ?: if (isSelected) ClockwiseColors.Primary else ClockwiseColors.Card
    val resolvedContent = contentColor
        ?: if (isSelected) ClockwiseColors.PrimaryForeground else ClockwiseColors.Foreground
    val borderColor = if (isSelected) resolvedContainer else ClockwiseColors.Border

    val interaction = if (selected == null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier.selectable(
            selected = isSelected,
            role = Role.Tab,
            onClick = onClick,
        )
    }

    Box(
        modifier = modifier
            // The interaction sits on this box, not on the drawn circle inside
            // it: the touch target is therefore this box's size, which is at
            // least MinTouchTarget even when the circle is smaller.
            .sizeIn(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
            .then(interaction),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(resolvedContainer)
                .border(width = 1.dp, color = borderColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = resolvedContent,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
