package com.pj95.clockwise.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.alarm.AlarmReadiness
import com.pj95.clockwise.model.prefs.ThemeMode
import com.pj95.clockwise.ui.components.MinTouchTarget
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles

/** Test tags for the settings sheet. */
object SettingsTestTags {
    const val ThemeMode = "settingsThemeMode"
    const val Use24Hour = "settingsUse24Hour"
    const val CompareToggle = "settingsCompareToggle"
    const val Legal = "settingsLegal"
    const val Permissions = "settingsPermissions"
}

/** The name of a theme mode, as shown in the picker. */
@StringRes
internal fun themeModeNameRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_mode_system
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
}

/**
 * Clockwise's settings sheet, in exactly the order the product asks for:
 * theme mode, 24-hour format, comparison, and the expandable Legal & Support
 * group.
 *
 * The Chronos gallery is deliberately not here: since v1.0.1 it has its own
 * header shortcut on the Clocks screen, which is where the home-screen clocks
 * are relevant. Keeping the sheet to these four groups is what makes it feel
 * like the rest of the app: one calm list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    use24Hour: Boolean,
    onUse24HourChange: (Boolean) -> Unit,
    compareWithLocalTime: Boolean,
    onCompareWithLocalTimeChange: (Boolean) -> Unit,
    onShowIntro: () -> Unit = {},
    onSendFeedback: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    readiness: AlarmReadiness = AlarmReadiness(),
    onOpenNotificationSettings: () -> Unit = {},
    onOpenExactAlarmSettings: () -> Unit = {},
    onOpenFullScreenSettings: () -> Unit = {},
    onOpenBatterySettings: () -> Unit = {},
    onOpenDndSettings: () -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ClockwiseColors.Background,
        contentColor = ClockwiseColors.Foreground,
        scrimColor = Color(0x66000000),
        modifier = modifier,
    ) {
        SettingsContent(
            themeMode = themeMode,
            onThemeModeSelected = onThemeModeSelected,
            use24Hour = use24Hour,
            onUse24HourChange = onUse24HourChange,
            compareWithLocalTime = compareWithLocalTime,
            onCompareWithLocalTimeChange = onCompareWithLocalTimeChange,
            onShowIntro = onShowIntro,
            onSendFeedback = onSendFeedback,
            readiness = readiness,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onOpenExactAlarmSettings = onOpenExactAlarmSettings,
            onOpenFullScreenSettings = onOpenFullScreenSettings,
            onOpenBatterySettings = onOpenBatterySettings,
            onOpenDndSettings = onOpenDndSettings,
        )
    }
}

/**
 * The sheet body. Kept separate from the sheet host so it can be rendered
 * directly in previews and screenshot tests.
 */
@Composable
internal fun SettingsContent(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    use24Hour: Boolean,
    onUse24HourChange: (Boolean) -> Unit,
    onShowIntro: () -> Unit = {},
    onSendFeedback: () -> Unit = {},
    modifier: Modifier = Modifier,
    compareWithLocalTime: Boolean = false,
    onCompareWithLocalTimeChange: (Boolean) -> Unit = {},
    readiness: AlarmReadiness = AlarmReadiness(),
    onOpenNotificationSettings: () -> Unit = {},
    onOpenExactAlarmSettings: () -> Unit = {},
    onOpenFullScreenSettings: () -> Unit = {},
    onOpenBatterySettings: () -> Unit = {},
    onOpenDndSettings: () -> Unit = {},
) {
    // The Legal & Support group is an accordion: the policy, the guide, the
    // terms, the licences and the version all live behind one heading, and the
    // sheet stays one calm list at rest.
    var legalExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.settings_eyebrow),
            style = ClockwiseTextStyles.Eyebrow,
            color = ClockwiseColors.MutedForeground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_title),
            style = ClockwiseTextStyles.ScreenTitle,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.semantics { heading() },
        )

        SettingsDivider()

        // 1 · Theme mode.
        SettingsHeading(stringResource(R.string.settings_theme_mode_label))
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ClockwiseShapes.medium)
                .background(ClockwiseColors.Accent)
                .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
                .padding(4.dp)
                .testTag(SettingsTestTags.ThemeMode)
                .selectableGroup(),
        ) {
            ThemeMode.entries.forEach { mode ->
                ThemeModeRow(
                    mode = mode,
                    isSelected = mode == themeMode,
                    onSelect = { onThemeModeSelected(mode) },
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_theme_mode_hint),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
        )

        SettingsDivider()

        // 2 · 24-hour format.
        SettingsHeading(stringResource(R.string.settings_format_label))
        Spacer(Modifier.height(10.dp))
        ToggleRow(
            title = stringResource(R.string.settings_24_hour),
            checked = use24Hour,
            onCheckedChange = onUse24HourChange,
            testTag = SettingsTestTags.Use24Hour,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_24_hour_hint),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
        )

        SettingsDivider()

        // 3 · Compare with local, off unless asked for.
        SettingsHeading(stringResource(R.string.settings_comparison_label))
        Spacer(Modifier.height(10.dp))
        ToggleRow(
            title = stringResource(R.string.settings_compare_local_time),
            checked = compareWithLocalTime,
            onCheckedChange = onCompareWithLocalTimeChange,
            testTag = SettingsTestTags.CompareToggle,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_compare_local_time_hint),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
        )

        SettingsDivider()

        // 4 · Alarms & permissions. Every row is a status plus a deep link: the
        // reason a permission matters is clearest next to whether it is on, and
        // the user can fix one without hunting through system settings.
        SettingsHeading(stringResource(R.string.settings_permissions_label))
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_permissions_hint),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
        )
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SettingsTestTags.Permissions),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PermissionRow(
                title = stringResource(R.string.permission_notifications),
                granted = readiness.notificationsAllowed,
                onOpen = onOpenNotificationSettings,
            )
            PermissionRow(
                title = stringResource(R.string.permission_exact_alarms),
                granted = readiness.exactAlarmsAllowed,
                onOpen = onOpenExactAlarmSettings,
            )
            PermissionRow(
                title = stringResource(R.string.permission_full_screen),
                granted = readiness.fullScreenIntentAllowed,
                onOpen = onOpenFullScreenSettings,
            )
            PermissionRow(
                title = stringResource(R.string.permission_battery),
                granted = readiness.batteryOptimizationIgnored,
                onOpen = onOpenBatterySettings,
            )
            PermissionRow(
                title = stringResource(R.string.permission_dnd),
                granted = readiness.dndBypassAllowed,
                onOpen = onOpenDndSettings,
            )
        }

        SettingsDivider()

        Spacer(Modifier.height(10.dp))

        // 4 · Legal & Support, as an expandable group. The old leaf row is gone:
        // the privacy policy, the app guide, the terms, the licences and the
        // version belong to one heading, and nothing in here opens a browser.
        SettingsAccordion(
            title = stringResource(R.string.settings_legal_title),
            support = stringResource(R.string.settings_legal_support),
            expanded = legalExpanded,
            onToggle = { legalExpanded = !legalExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SettingsTestTags.Legal),
        )

        if (legalExpanded) {
            LegalSupportContent(
                onShowIntro = onShowIntro,
                onSendFeedback = onSendFeedback,
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

/**
 * The expandable "Legal & Support" heading: a settings-style row that grows
 * into the group's content when tapped. The chevron rotates, but the row itself
 * is plain tappable semantics, so a screen reader announces the group.
 */
@Composable
private fun SettingsAccordion(
    title: String,
    support: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(220),
        label = "legalAccordionChevron",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .clickable(role = Role.Button, onClick = onToggle)
            .padding(vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = ClockwiseTextStyles.CityName,
                    color = ClockwiseColors.Foreground,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = support,
                    style = ClockwiseTextStyles.CityRegion,
                    color = ClockwiseColors.MutedForeground,
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = null,
                tint = ClockwiseColors.MutedForeground,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation.value),
            )
        }
    }
}

/**
 * The group's body, revealed by the accordion. Everything here is local copy:
 * the privacy text, the guide, the terms, the licences and the version, plus the
 * two actions that do not need a browser — replay the intro and send feedback.
 */
@Composable
private fun LegalSupportContent(
    onShowIntro: () -> Unit,
    onSendFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
    ) {
        LegalTextSection(
            title = stringResource(R.string.legal_privacy_title),
            body = stringResource(R.string.legal_privacy_body),
        )
        LegalDivider()
        LegalTextSection(
            title = stringResource(R.string.legal_guide_title),
            body = stringResource(R.string.legal_guide_body),
        )
        LegalDivider()
        LegalTextSection(
            title = stringResource(R.string.legal_terms_title),
            body = stringResource(R.string.legal_terms_body),
        )
        LegalDivider()
        LegalTextSection(
            title = stringResource(R.string.legal_licenses_title),
            body = stringResource(R.string.legal_licenses_body),
        )

        Spacer(Modifier.height(20.dp))

        LegalTextSection(
            title = stringResource(R.string.legal_about_title),
            body = stringResource(R.string.about_version_title) + "\n" +
                stringResource(R.string.about_version_body) + "\n\n" +
                stringResource(R.string.legal_developer_name),
        )

        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onSendFeedback,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("settingsSendFeedback"),
            shape = ClockwiseShapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = ClockwiseColors.Primary,
                contentColor = ClockwiseColors.PrimaryForeground,
            ),
        ) {
            Text(
                text = stringResource(R.string.legal_send_feedback),
                style = ClockwiseTextStyles.CityName,
            )
        }
        TextButton(
            onClick = onShowIntro,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.legal_show_intro),
                style = ClockwiseTextStyles.CityRegion,
            )
        }
    }
}

/** One titled paragraph inside the accordion. */
@Composable
private fun LegalTextSection(
    title: String,
    body: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = ClockwiseTextStyles.CityName,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
        )
    }
}

/** Hairline between accordion sections, in the sheet's own border colour. */
@Composable
private fun LegalDivider() {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = ClockwiseColors.Border)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SettingsHeading(text: String) {
    Text(
        text = text,
        style = ClockwiseTextStyles.SectionLabel,
        color = ClockwiseColors.MutedForeground,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun SettingsDivider() {
    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = ClockwiseColors.Border)
    Spacer(Modifier.height(20.dp))
}

/**
 * One permission the alarm experience depends on: its name, whether it is
 * granted, and a tap that opens the system screen where it can be changed.
 *
 * A status rather than a switch: none of these can be toggled in-app, and
 * pretending otherwise would make the row lie.
 */
@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .clickable(
                onClickLabel = title,
                role = Role.Button,
                onClick = onOpen,
            )
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = ClockwiseTextStyles.CityName,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(
                if (granted) R.string.permission_allowed else R.string.permission_needs_attention,
            ),
            style = ClockwiseTextStyles.SectionLabel,
            color = if (granted) ClockwiseColors.MutedForeground else ClockwiseColors.MapAccent,
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    support: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(ClockwiseShapes.small)
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ClockwiseTextStyles.CityName,
                color = ClockwiseColors.Foreground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = support,
                style = ClockwiseTextStyles.CityRegion,
                color = ClockwiseColors.MutedForeground,
            )
        }
    }
}

/**
 * A whole-row switch: the label and the control are announced as one thing, so
 * a screen reader user does not have to aim at the knob.
 */
@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(ClockwiseShapes.medium)
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = ClockwiseTextStyles.CityName,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            // The row owns the interaction; the knob is its illustration.
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ClockwiseColors.PrimaryForeground,
                checkedTrackColor = ClockwiseColors.Primary,
                uncheckedThumbColor = ClockwiseColors.MutedForeground,
                uncheckedTrackColor = ClockwiseColors.Secondary,
                uncheckedBorderColor = ClockwiseColors.Border,
            ),
        )
    }
}

/**
 * One theme mode in the picker.
 *
 * The row carries radio-button semantics with its own selected state, so a
 * screen reader announces the mode it would apply and whether it is in force.
 */
@Composable
private fun ThemeModeRow(
    mode: ThemeMode,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(ClockwiseShapes.small)
            .background(if (isSelected) ClockwiseColors.Primary else Color.Transparent)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(themeModeNameRes(mode)),
            style = ClockwiseTextStyles.CityName,
            color = if (isSelected) {
                ClockwiseColors.PrimaryForeground
            } else {
                ClockwiseColors.Foreground
            },
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Settings sheet 360dp",
    widthDp = 360,
    heightDp = 800,
    showBackground = true,
)
@Composable
private fun SettingsContentPreview() {
    com.pj95.clockwise.ui.theme.ClockwiseTheme {
        Column(verticalArrangement = Arrangement.Center) {
            SettingsContent(
                themeMode = ThemeMode.SYSTEM,
                onThemeModeSelected = {},
                use24Hour = true,
                onUse24HourChange = {},
            )
        }
    }
}
