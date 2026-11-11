package com.pj95.clockwise.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.data.repository.DefaultWidgetCityIds
import com.pj95.clockwise.model.widget.LicenseState
import com.pj95.clockwise.model.widget.WidgetStyleId
import com.pj95.clockwise.ui.StudioMessage
import com.pj95.clockwise.ui.StudioPinnedWidget
import com.pj95.clockwise.ui.StudioStyleItem
import com.pj95.clockwise.ui.StudioViewModel
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.components.WidgetStylePreview
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles

/**
 * The Chronos Studio gallery.
 *
 * One scrolling screen, in the order a decision is made: what is already on the
 * home screen, what else is available, then the single purchase that unlocks
 * the premium half. Nothing here reads Play directly — the cards render Room's
 * licence rows, so a device with no connection shows exactly the same thing as
 * one that has just restored a purchase.
 */
@Composable
fun StudioScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudioViewModel = viewModel(factory = StudioViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.messages.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) { context.findActivityOrNull() }

    var editing by remember { mutableStateOf<StudioPinnedWidget?>(null) }

    // Billing outcomes arrive as a state field; they are turned into the
    // gallery's own message vocabulary once, then consumed.
    LaunchedEffect(state.billing.notice) {
        state.billing.notice?.let(viewModel::noteBillingNotice)
    }

    LaunchedEffect(message) {
        val pending = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(pending.textRes()))
        viewModel.clearMessage()
    }

    Box(modifier = modifier.fillMaxSize().background(ClockwiseColors.Background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
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
                        text = stringResource(R.string.studio_eyebrow),
                        style = ClockwiseTextStyles.Eyebrow,
                        color = ClockwiseColors.MutedForeground,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.studio_title),
                        style = ClockwiseTextStyles.ScreenTitle,
                        color = ClockwiseColors.Foreground,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.studio_support),
                        style = ClockwiseTextStyles.CityRegion,
                        color = ClockwiseColors.MutedForeground,
                    )
                }
            }

            if (state.pinned.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.studio_section_pinned)) }
                items(state.pinned, key = { "pinned-${it.appWidgetId}" }) { pinned ->
                    PinnedWidgetRow(
                        widget = pinned,
                        onEdit = { editing = pinned },
                    )
                }
            }

            item { SectionLabel(stringResource(R.string.studio_section_gallery)) }

            items(state.styles, key = { "style-${it.style.id}" }) { item ->
                StyleCard(
                    item = item,
                    now = state.now,
                    use24Hour = state.use24Hour,
                    onPin = { viewModel.pin(item.style) },
                    onStartTrial = { viewModel.startTrial(item.style) },
                )
            }

            item {
                PurchaseCard(
                    allUnlocked = state.styles
                        .filter { it.style.isPremium }
                        .all { it.license is LicenseState.Unlocked },
                    priceText = state.billing.priceText,
                    isWorking = state.billing.isWorking,
                    isUnavailable = state.billing.isUnavailable,
                    onPurchase = { activity?.let(viewModel::purchase) },
                    onRestore = viewModel::restore,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }

    editing?.let { pinned ->
        CitiesDialog(
            initialCityIds = pinned.cityIds,
            onDismiss = { editing = null },
            onSave = { cityIds ->
                viewModel.updatePinnedCities(pinned.appWidgetId, pinned.style, cityIds)
                editing = null
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = ClockwiseTextStyles.SectionLabel,
        color = ClockwiseColors.MutedForeground,
        modifier = Modifier
            .padding(top = 12.dp)
            .semantics { heading() },
    )
}

@Composable
private fun StyleCard(
    item: StudioStyleItem,
    now: java.time.Instant,
    use24Hour: Boolean,
    onPin: () -> Unit,
    onStartTrial: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, RoundedCornerShape(24.dp))
            .padding(14.dp),
    ) {
        WidgetStylePreview(
            style = item.style,
            now = now,
            use24Hour = use24Hour,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.style.nameRes()),
                    style = ClockwiseTextStyles.CityName,
                    color = ClockwiseColors.Foreground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(item.style.bodyRes()),
                    style = ClockwiseTextStyles.CityRegion,
                    color = ClockwiseColors.MutedForeground,
                )
            }
            Spacer(Modifier.size(10.dp))
            StatusBadge(item.license)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = item.statusText(),
            style = ClockwiseTextStyles.DstCaption,
            color = if (item.license.isUsable) {
                ClockwiseColors.MutedForeground
            } else {
                ClockwiseColors.Primary
            },
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.license.isUsable) {
                Button(
                    onClick = onPin,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClockwiseColors.Primary,
                        contentColor = ClockwiseColors.PrimaryForeground,
                    ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.studio_action_pin),
                        style = ClockwiseTextStyles.SegmentLabel,
                    )
                }
            } else if (item.canStartTrial) {
                Button(
                    onClick = onStartTrial,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClockwiseColors.Primary,
                        contentColor = ClockwiseColors.PrimaryForeground,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.studio_action_trial),
                        style = ClockwiseTextStyles.SegmentLabel,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(license: LicenseState) {
    val (label, accent) = when (license) {
        LicenseState.Free -> R.string.studio_badge_free to false
        LicenseState.Unlocked -> R.string.studio_badge_unlocked to true
        is LicenseState.TrialActive -> R.string.studio_badge_trial to true
        LicenseState.TrialExpired -> R.string.studio_badge_locked to false
        LicenseState.Locked -> R.string.studio_badge_locked to false
    }

    Text(
        text = stringResource(label),
        style = ClockwiseTextStyles.SectionLabel,
        color = if (accent) ClockwiseColors.Primary else ClockwiseColors.MutedForeground,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(
                if (accent) {
                    ClockwiseColors.Primary.copy(alpha = 0.12f)
                } else {
                    ClockwiseColors.Background
                },
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun StudioStyleItem.statusText(): String = when (val license = license) {
    LicenseState.Free -> stringResource(R.string.studio_status_free)
    LicenseState.Unlocked -> stringResource(R.string.studio_status_unlocked)
    LicenseState.TrialExpired -> stringResource(R.string.studio_status_trial_expired)
    LicenseState.Locked -> stringResource(R.string.studio_status_locked)
    is LicenseState.TrialActive -> stringResource(
        R.string.studio_status_trial,
        formatRemaining(license.remainingMillis),
    )
}

@Composable
private fun PinnedWidgetRow(widget: StudioPinnedWidget, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(widget.style.nameRes()),
                style = ClockwiseTextStyles.CityName,
                color = ClockwiseColors.Foreground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = widget.cityNames.joinToString(" · "),
                style = ClockwiseTextStyles.CityRegion,
                color = ClockwiseColors.MutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(
                text = stringResource(R.string.studio_action_edit_cities),
                style = ClockwiseTextStyles.SegmentLabel,
                color = ClockwiseColors.Primary,
            )
        }
    }
}

@Composable
private fun PurchaseCard(
    allUnlocked: Boolean,
    priceText: String?,
    isWorking: Boolean,
    isUnavailable: Boolean,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ClockwiseColors.Card)
            .border(1.dp, ClockwiseColors.Border, RoundedCornerShape(24.dp))
            .padding(18.dp),
    ) {
        Text(
            text = stringResource(R.string.studio_section_unlock),
            style = ClockwiseTextStyles.SectionLabel,
            color = ClockwiseColors.MutedForeground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.studio_purchase_title),
            style = ClockwiseTextStyles.CityName,
            color = ClockwiseColors.Foreground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.studio_purchase_body),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
        )

        Spacer(Modifier.height(14.dp))

        if (allUnlocked) {
            Text(
                text = stringResource(R.string.studio_purchase_owned),
                style = ClockwiseTextStyles.SegmentLabel,
                color = ClockwiseColors.Primary,
            )
        } else {
            Button(
                onClick = onPurchase,
                enabled = !isWorking,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClockwiseColors.Primary,
                    contentColor = ClockwiseColors.PrimaryForeground,
                ),
            ) {
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ClockwiseColors.PrimaryForeground,
                    )
                } else {
                    Text(
                        text = if (priceText != null) {
                            stringResource(R.string.studio_purchase_button, priceText)
                        } else {
                            stringResource(R.string.studio_purchase_button_unknown_price)
                        },
                        style = ClockwiseTextStyles.SegmentLabel,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            TextButton(
                onClick = onRestore,
                enabled = !isWorking,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.studio_restore),
                    style = ClockwiseTextStyles.SegmentLabel,
                    color = ClockwiseColors.MutedForeground,
                )
            }
        }

        if (isUnavailable) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.studio_billing_unavailable),
                style = ClockwiseTextStyles.DstCaption,
                color = ClockwiseColors.MutedForeground,
            )
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = ClockwiseColors.Border)
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.studio_offline_body),
            style = ClockwiseTextStyles.DstCaption,
            color = ClockwiseColors.MutedForeground,
        )
    }
}

/**
 * The three-city editor for a widget that is already on the home screen.
 *
 * The same three slots as the launcher's configure screen, but in a dialog: the
 * user came here from the gallery, so a full screen would throw away the list
 * they were looking at. Picking swaps the dialog body rather than stacking a
 * second window, which keeps Back predictable.
 */
@Composable
private fun CitiesDialog(
    initialCityIds: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var cityIds by remember(initialCityIds) {
        mutableStateOf(
            List(3) { index -> initialCityIds.getOrElse(index) { DefaultWidgetCityIds[index] } },
        )
    }
    var picking by remember { mutableStateOf<Int?>(null) }

    val slot = picking
    if (slot == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.studio_cities_title),
                    style = ClockwiseTextStyles.CityName,
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.studio_cities_body),
                        style = ClockwiseTextStyles.CityRegion,
                        color = ClockwiseColors.MutedForeground,
                    )
                    Spacer(Modifier.height(12.dp))
                    cityIds.forEachIndexed { index, cityId ->
                        val city = CityDirectory.find(cityId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { picking = index }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = (index + 1).toString(),
                                style = ClockwiseTextStyles.SectionLabel,
                                color = ClockwiseColors.MutedForeground,
                                modifier = Modifier.size(24.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = city?.name ?: cityId,
                                    style = ClockwiseTextStyles.SearchResultName,
                                )
                                Text(
                                    text = city?.region.orEmpty(),
                                    style = ClockwiseTextStyles.SearchResultRegion,
                                    color = ClockwiseColors.MutedForeground,
                                )
                            }
                            Text(
                                text = stringResource(R.string.widget_config_change),
                                style = ClockwiseTextStyles.SectionLabel,
                                color = ClockwiseColors.MutedForeground,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onSave(cityIds) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.studio_cities_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.studio_cities_cancel))
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(
                    onClick = { picking = null },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(text = stringResource(R.string.widget_config_cancel))
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(CityDirectory.all, key = { it.id }) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable {
                                    cityIds = cityIds.toMutableList().also { it[slot] = city.id }
                                    picking = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = city.name,
                                    style = ClockwiseTextStyles.SearchResultName,
                                )
                                Text(
                                    text = city.region,
                                    style = ClockwiseTextStyles.SearchResultRegion,
                                    color = ClockwiseColors.MutedForeground,
                                )
                            }
                            if (cityIds.getOrNull(slot) == city.id) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = ClockwiseColors.Primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}

private fun StudioMessage.textRes(): Int = when (this) {
    StudioMessage.TRIAL_STARTED -> R.string.studio_msg_trial_started
    StudioMessage.TRIAL_ALREADY_USED -> R.string.studio_msg_trial_used
    StudioMessage.PIN_REQUESTED -> R.string.studio_msg_pin_requested
    StudioMessage.PIN_UNAVAILABLE -> R.string.studio_msg_pin_unavailable
    StudioMessage.CITIES_SAVED -> R.string.studio_msg_cities_saved
    StudioMessage.PURCHASE_COMPLETE -> R.string.studio_msg_purchase_complete
    StudioMessage.RESTORED -> R.string.studio_msg_restored
    StudioMessage.NOTHING_TO_RESTORE -> R.string.studio_msg_nothing_to_restore
    StudioMessage.PURCHASE_CANCELLED -> R.string.studio_msg_purchase_cancelled
    StudioMessage.PURCHASE_FAILED -> R.string.studio_msg_purchase_failed
    StudioMessage.BILLING_UNAVAILABLE -> R.string.studio_msg_billing_unavailable
}

private fun WidgetStyleId.nameRes(): Int = when (this) {
    WidgetStyleId.FREE_SLATE_MULTIFUNCTION -> R.string.studio_style_slate
    WidgetStyleId.FREE_DIAL_24H -> R.string.studio_style_dial
    WidgetStyleId.FREE_MARKET_SESSIONS -> R.string.studio_style_market_sessions
    WidgetStyleId.PREMIUM_GLOBAL_CLOCKS -> R.string.studio_style_global_clocks
    WidgetStyleId.PREMIUM_AMBIENT_GRID -> R.string.studio_style_ambient_grid
}

private fun WidgetStyleId.bodyRes(): Int = when (this) {
    WidgetStyleId.FREE_SLATE_MULTIFUNCTION -> R.string.studio_style_slate_body
    WidgetStyleId.FREE_DIAL_24H -> R.string.studio_style_dial_body
    WidgetStyleId.FREE_MARKET_SESSIONS -> R.string.studio_style_market_sessions_body
    WidgetStyleId.PREMIUM_GLOBAL_CLOCKS -> R.string.studio_style_global_clocks_body
    WidgetStyleId.PREMIUM_AMBIENT_GRID -> R.string.studio_style_ambient_grid_body
}

/** "2d 5h", "3h 20m", "12m" — never a raw millisecond count. */
private fun formatRemaining(remainingMillis: Long): String {
    val totalMinutes = (remainingMillis / 60_000L).coerceAtLeast(0L)
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes % (24L * 60L)) / 60L
    val minutes = totalMinutes % 60L
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}
