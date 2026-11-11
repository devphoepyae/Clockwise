package com.pj95.clockwise.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ui.theme.ClockwiseTheme

/**
 * About Clockwise.
 *
 * A local screen, like Privacy and Help: there is no WebView and no network
 * permission to load a hosted page with. The version is spelled out here and
 * the privacy note states the offline contract in one sentence.
 */
@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    InfoScreen(
        eyebrow = stringResource(R.string.about_eyebrow),
        title = stringResource(R.string.about_title),
        sections = listOf(
            InfoSection(
                stringResource(R.string.about_version_title),
                stringResource(R.string.about_version_body),
            ),
            InfoSection(
                stringResource(R.string.about_privacy_title),
                stringResource(R.string.about_privacy_body),
            ),
        ),
        onBack = onBack,
        modifier = modifier,
        footnote = stringResource(R.string.about_footnote),
    )
}

@Preview(name = "About 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun AboutScreenPreview() {
    ClockwiseTheme {
        AboutScreen(onBack = {})
    }
}
