package com.pj95.clockwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles

/** Test tags shared by every direct numeric entry pad. */
object NumericKeypadTestTags {
    const val Backspace = "numericKeypadBackspace"

    fun digit(value: Int): String = "numericKeypad_$value"
}

/**
 * A calm 3 × 4 number pad: 1–9, then a blank cell, 0 and backspace.
 *
 * The alarm editor and the timer both offer direct entry, so the pad lives in
 * one place rather than being drawn twice with slightly different spacing. The
 * blank bottom-left cell keeps 0 under the thumb instead of in a corner, which
 * is where a phone keypad puts it.
 */
@Composable
fun NumericKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    backspaceEnabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
        ).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { digit ->
                    KeypadKey(
                        label = digit.toString(),
                        description = digit.toString(),
                        onClick = { onDigit(digit) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(NumericKeypadTestTags.digit(digit)),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.weight(1f))
            KeypadKey(
                label = "0",
                description = "0",
                onClick = { onDigit(0) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(NumericKeypadTestTags.digit(0)),
            )
            KeypadKey(
                label = null,
                description = stringResource(R.string.keypad_backspace),
                onClick = { if (backspaceEnabled) onBackspace() },
                modifier = Modifier
                    .weight(1f)
                    .testTag(NumericKeypadTestTags.Backspace),
                iconRes = R.drawable.ic_backspace,
                enabled = backspaceEnabled,
            )
        }
    }
}

@Composable
private fun KeypadKey(
    label: String?,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (enabled) {
                    ClockwiseColors.Foreground
                } else {
                    ClockwiseColors.MutedForeground
                },
                modifier = Modifier.size(20.dp),
            )
        } else if (label != null) {
            Text(
                text = label,
                style = ClockwiseTextStyles.CityName,
                color = ClockwiseColors.Foreground,
            )
        }
    }
}

/** A round icon button used beside the keypad for the dial/keyboard switch. */
@Composable
fun PickerModeButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(22.dp),
        )
    }
}
