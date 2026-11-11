package com.pj95.clockwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes

/**
 * Wraps a list card in the horizontal swipe-to-delete gesture.
 *
 * Only a right-to-left swipe deletes: the same card also owns a long-press
 * vertical drag for reordering, and offering delete in both directions would
 * make the first movement of either gesture ambiguous. The delete is committed
 * on the swipe itself; the caller is expected to offer Undo rather than ask
 * "are you sure?", which is the pattern the rest of the app follows.
 *
 * The red wash behind the card is drawn only while the card moves, so a settled
 * list looks exactly like the list without the gesture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteBox(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(ClockwiseShapes.medium)
                    .background(ClockwiseColors.MapAccent)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    // The swipe is announced by the row's own "Delete" custom
                    // action; this icon is the visual confirmation, not a
                    // second control for a screen reader to land on.
                    contentDescription = null,
                    tint = ClockwiseColors.PrimaryForeground,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        content = content,
    )
}
