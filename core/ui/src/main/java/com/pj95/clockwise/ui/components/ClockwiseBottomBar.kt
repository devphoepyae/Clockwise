package com.pj95.clockwise.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R

/** The two destinations in the bottom bar. */
enum class ClockwiseTab {
    Alarms,
    Clocks,
}

/**
 * Bottom navigation: Alarms and Clocks.
 *
 * The world-map destination was removed in Clockwise 1.0 to keep the app a pure
 * clock list; search stays on the `+` action on the Clocks screen.
 */
@Composable
fun ClockwiseBottomBar(
    selectedTab: ClockwiseTab,
    onSelectTab: (ClockwiseTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            iconRes = R.drawable.ic_alarm,
            contentDescription = stringResource(R.string.nav_alarms),
            onClick = { onSelectTab(ClockwiseTab.Alarms) },
            selected = selectedTab == ClockwiseTab.Alarms,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        CircleIconButton(
            iconRes = R.drawable.ic_clock,
            contentDescription = stringResource(R.string.nav_clocks),
            onClick = { onSelectTab(ClockwiseTab.Clocks) },
            selected = selectedTab == ClockwiseTab.Clocks,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}
