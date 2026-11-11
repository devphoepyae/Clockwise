package com.pj95.clockwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme

/** One titled paragraph on an information screen. */
data class InfoSection(val title: String, val body: String)

/**
 * The shared shape of Privacy, Help and Chronos Studio.
 *
 * All three are local screens rather than a WebView: there is no network
 * permission to load a hosted policy with, and text that ships in the APK
 * cannot quietly change after the user has read it.
 */
@Composable
fun InfoScreen(
    eyebrow: String,
    title: String,
    sections: List<InfoSection>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    footnote: String? = null,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ClockwiseColors.Background),
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 12.dp,
            bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    iconRes = R.drawable.ic_back,
                    contentDescription = stringResource(R.string.detail_back),
                    onClick = onBack,
                    size = 44.dp,
                    iconSize = 20.dp,
                )
            }
        }

        item {
            Column {
                Text(
                    text = eyebrow,
                    style = ClockwiseTextStyles.Eyebrow,
                    color = ClockwiseColors.MutedForeground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    style = ClockwiseTextStyles.ScreenTitle,
                    color = ClockwiseColors.Foreground,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }

        item {
            HorizontalDivider(
                color = ClockwiseColors.Border,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        items(sections) { section ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = section.title,
                    style = ClockwiseTextStyles.CityName,
                    color = ClockwiseColors.Foreground,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = section.body,
                    style = ClockwiseTextStyles.CityRegion,
                    color = ClockwiseColors.MutedForeground,
                )
            }
        }

        if (footnote != null) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = footnote,
                    style = ClockwiseTextStyles.DstCaption,
                    color = ClockwiseColors.MutedForeground,
                )
            }
        }
    }
}

/** Zero-collection policy, in the app and in full. */
@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    InfoScreen(
        eyebrow = stringResource(R.string.privacy_eyebrow),
        title = stringResource(R.string.privacy_title),
        sections = listOf(
            InfoSection(
                stringResource(R.string.privacy_offline_title),
                stringResource(R.string.privacy_offline_body),
            ),
            InfoSection(
                stringResource(R.string.privacy_stored_title),
                stringResource(R.string.privacy_stored_body),
            ),
            InfoSection(
                stringResource(R.string.privacy_ads_title),
                stringResource(R.string.privacy_ads_body),
            ),
            InfoSection(
                stringResource(R.string.privacy_feedback_title),
                stringResource(R.string.privacy_feedback_body),
            ),
            InfoSection(
                stringResource(R.string.privacy_control_title),
                stringResource(R.string.privacy_control_body),
            ),
        ),
        onBack = onBack,
        modifier = modifier,
        footnote = stringResource(R.string.privacy_footnote),
    )
}

/** The platform settings an alarm depends on, explained in one place. */
@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    InfoScreen(
        eyebrow = stringResource(R.string.help_eyebrow),
        title = stringResource(R.string.help_title),
        sections = listOf(
            InfoSection(
                stringResource(R.string.help_alarms_title),
                stringResource(R.string.help_alarms_body),
            ),
            InfoSection(
                stringResource(R.string.help_exact_title),
                stringResource(R.string.help_exact_body),
            ),
            InfoSection(
                stringResource(R.string.help_notifications_title),
                stringResource(R.string.help_notifications_body),
            ),
            InfoSection(
                stringResource(R.string.help_dnd_title),
                stringResource(R.string.help_dnd_body),
            ),
            InfoSection(
                stringResource(R.string.help_battery_title),
                stringResource(R.string.help_battery_body),
            ),
            InfoSection(
                stringResource(R.string.help_screensaver_title),
                stringResource(R.string.help_screensaver_body),
            ),
        ),
        onBack = onBack,
        modifier = modifier,
        footnote = stringResource(R.string.help_footnote),
    )
}

@Preview(name = "Privacy 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun PrivacyScreenPreview() {
    FixedClockwiseTheme {
        PrivacyScreen(onBack = {})
    }
}
