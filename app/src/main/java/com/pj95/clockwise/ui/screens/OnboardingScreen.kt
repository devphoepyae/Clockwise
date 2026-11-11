package com.pj95.clockwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme

/** Stable handles for the onboarding tests and screenshot render. */
object OnboardingTags {
    const val SCREEN = "onboarding-screen"
    const val GET_STARTED = "onboarding-get-started"
}

/**
 * The first-launch intro.
 *
 * Deliberately one short page in the spirit of Google Tasks' intro: a large
 * quiet mark, one sentence about what the app is for, three concrete things it
 * does, and a single primary action. There is no pager and no account step —
 * the app works entirely offline, so there is nothing to sign into.
 *
 * The screen is scrollable because the copy and the button must both survive a
 * large system font; nothing here is pinned to a fixed height.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ClockwiseColors.Background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 32.dp)
            .testTag(OnboardingTags.SCREEN),
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(ClockwiseColors.Accent)
                .border(1.dp, ClockwiseColors.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_globe),
                contentDescription = null,
                tint = ClockwiseColors.Foreground,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboarding_eyebrow),
            style = ClockwiseTextStyles.Eyebrow,
            color = ClockwiseColors.MutedForeground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_title),
            style = ClockwiseTextStyles.ScreenTitle,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_support),
            style = ClockwiseTextStyles.DetailCityRegion,
            color = ClockwiseColors.MutedForeground,
        )

        Spacer(Modifier.height(36.dp))

        OnboardingFeature(
            iconRes = R.drawable.ic_clock,
            title = stringResource(R.string.onboarding_feature_time_title),
            body = stringResource(R.string.onboarding_feature_time_body),
        )
        Spacer(Modifier.height(20.dp))
        OnboardingFeature(
            iconRes = R.drawable.ic_globe,
            title = stringResource(R.string.onboarding_feature_plan_title),
            body = stringResource(R.string.onboarding_feature_plan_body),
        )
        Spacer(Modifier.height(20.dp))
        OnboardingFeature(
            iconRes = R.drawable.ic_check,
            title = stringResource(R.string.onboarding_feature_private_title),
            body = stringResource(R.string.onboarding_feature_private_body),
        )

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(OnboardingTags.GET_STARTED),
            shape = ClockwiseShapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = ClockwiseColors.Primary,
                contentColor = ClockwiseColors.PrimaryForeground,
            ),
        ) {
            Text(
                text = stringResource(R.string.onboarding_action),
                style = ClockwiseTextStyles.CityName,
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.onboarding_footnote),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OnboardingFeature(
    iconRes: Int,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(ClockwiseColors.Card)
                .border(1.dp, ClockwiseColors.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = ClockwiseColors.Foreground,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ClockwiseTextStyles.CityName,
                color = ClockwiseColors.Foreground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = ClockwiseTextStyles.CityRegion,
                color = ClockwiseColors.MutedForeground,
            )
        }
    }
}

@Preview(name = "Onboarding 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    FixedClockwiseTheme {
        OnboardingScreen(onDone = {})
    }
}
