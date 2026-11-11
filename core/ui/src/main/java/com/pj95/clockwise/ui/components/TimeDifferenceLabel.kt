package com.pj95.clockwise.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.time.TimeDifference

/**
 * Renders a [TimeDifference] the way the stage brief asks for it:
 *
 * ```
 * Same time
 * +2 hrs ahead
 * -5 hrs behind
 * +5 hrs 30 mins ahead
 * ```
 *
 * The sign carries the direction and the words repeat it, so the value reads
 * correctly whether it is skimmed or heard. Half-hour zones are common
 * (UTC+5:30, UTC+6:30), so the minutes part is always shown when non-zero
 * rather than being rounded away.
 */
@Composable
@ReadOnlyComposable
fun timeDifferenceLabel(difference: TimeDifference): String {
    if (difference.isSameTime) return stringResource(R.string.time_diff_same)

    val amount = when {
        difference.hours > 0 && difference.minutesPart > 0 -> stringResource(
            R.string.time_diff_hours_minutes,
            difference.hours,
            difference.minutesPart,
        )

        difference.hours > 0 -> stringResource(R.string.time_diff_hours, difference.hours)

        else -> stringResource(R.string.time_diff_minutes, difference.minutesPart)
    }

    val signed = if (difference.isAhead) "+$amount" else "-$amount"
    return stringResource(
        if (difference.isAhead) R.string.time_diff_ahead else R.string.time_diff_behind,
        signed,
    )
}
