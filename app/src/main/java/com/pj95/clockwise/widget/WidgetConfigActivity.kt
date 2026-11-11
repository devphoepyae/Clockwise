package com.pj95.clockwise.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ClockwiseApp
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.data.repository.DefaultWidgetCityIds
import com.pj95.clockwise.data.repository.WidgetConfig
import com.pj95.clockwise.model.widget.WidgetStyleId
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme
import kotlinx.coroutines.launch

/**
 * The three-city chooser the launcher opens when a Studio widget is added.
 *
 * A configuration activity is the platform's own answer to "which cities?"
 * and it runs before the widget is placed, so a new card never flashes the
 * defaults first. The same screen is not used for editing later: the gallery's
 * "Manage" section owns that, because a user who already has the widget does not
 * need to go through the launcher again.
 */
class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val style = resolveStyle(appWidgetId)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || style == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val container = (application as ClockwiseApp).container
        setContent {
            FixedClockwiseTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    style = style,
                    initialCityIds = DefaultWidgetCityIds,
                    load = { container.widgetConfigRepository.get(appWidgetId)?.cityIds },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onSave = { cityIds ->
                        lifecycleScope.launch {
                            container.widgetConfigRepository.save(
                                WidgetConfig(
                                    appWidgetId = appWidgetId,
                                    style = style,
                                    cityIds = cityIds,
                                    isPremium = style.isPremium,
                                ),
                            )
                            runCatching {
                                ChronosWidget(style).update(
                                    this@WidgetConfigActivity,
                                    GlanceAppWidgetManager(this@WidgetConfigActivity)
                                        .getGlanceIdBy(appWidgetId),
                                )
                            }
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                            )
                            finish()
                        }
                    },
                )
            }
        }
    }

    /**
     * Which provider this instance belongs to.
     *
     * The platform's configure Intent names the widget id but not the provider,
     * so the provider is read back from `AppWidgetManager`. That keeps one
     * activity — and one screen — for all four styles.
     */
    private fun resolveStyle(appWidgetId: Int): WidgetStyleId? {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return null
        val provider = AppWidgetManager.getInstance(this)
            .getAppWidgetInfo(appWidgetId)
            ?.provider
            ?.className
            ?: return null
        return when {
            provider.endsWith(FreeSlateWidgetReceiver::class.java.simpleName) ->
                WidgetStyleId.FREE_SLATE_MULTIFUNCTION
            provider.endsWith(Dial24hWidgetReceiver::class.java.simpleName) ->
                WidgetStyleId.FREE_DIAL_24H
            provider.endsWith(MarketSessionsWidgetReceiver::class.java.simpleName) ->
                WidgetStyleId.FREE_MARKET_SESSIONS
            provider.endsWith(GlobalClocksWidgetReceiver::class.java.simpleName) ->
                WidgetStyleId.PREMIUM_GLOBAL_CLOCKS
            provider.endsWith(AmbientGridWidgetReceiver::class.java.simpleName) ->
                WidgetStyleId.PREMIUM_AMBIENT_GRID
            else -> null
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    appWidgetId: Int,
    style: WidgetStyleId,
    initialCityIds: List<String>,
    load: suspend () -> List<String>?,
    onCancel: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var cityIds by remember(appWidgetId) { mutableStateOf(initialCityIds) }
    var picking by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(appWidgetId) {
        load()?.takeIf { it.size == 3 }?.let { cityIds = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClockwiseColors.Background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                iconRes = R.drawable.ic_close,
                contentDescription = stringResource(R.string.widget_config_cancel),
                onClick = onCancel,
                size = 44.dp,
                iconSize = 20.dp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = style.displayName(),
                style = ClockwiseTextStyles.Eyebrow,
                color = ClockwiseColors.MutedForeground,
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.widget_config_title),
            style = ClockwiseTextStyles.ScreenTitle,
            color = ClockwiseColors.Foreground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.widget_config_body),
            style = ClockwiseTextStyles.CityRegion,
            color = ClockwiseColors.MutedForeground,
        )

        Spacer(Modifier.height(24.dp))

        cityIds.forEachIndexed { index, cityId ->
            CitySlot(
                index = index,
                cityId = cityId,
                onClick = { picking = index },
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onSave(cityIds) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ClockwiseColors.Primary,
                contentColor = ClockwiseColors.PrimaryForeground,
            ),
        ) {
            Text(
                text = stringResource(R.string.widget_config_save),
                style = ClockwiseTextStyles.SectionLabel,
            )
        }
    }

    picking?.let { index ->
        CityPickerDialog(
            currentId = cityIds[index],
            onPick = { picked ->
                cityIds = cityIds.toMutableList().also { it[index] = picked }
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun CitySlot(index: Int, cityId: String, onClick: () -> Unit) {
    val city = CityDirectory.find(cityId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClockwiseColors.Card)
            .clickable(onClickLabel = stringResource(R.string.widget_config_change), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (index + 1).toString(),
                style = ClockwiseTextStyles.SectionLabel,
                color = ClockwiseColors.MutedForeground,
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = city?.name ?: cityId,
                style = ClockwiseTextStyles.CityName,
                color = ClockwiseColors.Foreground,
            )
            Text(
                text = city?.region.orEmpty(),
                style = ClockwiseTextStyles.CityRegion,
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

@Composable
private fun CityPickerDialog(
    currentId: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
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
                            .clickable { onPick(city.id) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = city.name,
                                style = ClockwiseTextStyles.SearchResultName,
                                color = ClockwiseColors.Foreground,
                            )
                            Text(
                                text = city.region,
                                style = ClockwiseTextStyles.SearchResultRegion,
                                color = ClockwiseColors.MutedForeground,
                            )
                        }
                        if (city.id == currentId) {
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

@Composable
private fun WidgetStyleId.displayName(): String = stringResource(
    when (this) {
        WidgetStyleId.FREE_SLATE_MULTIFUNCTION -> R.string.studio_style_slate
        WidgetStyleId.FREE_DIAL_24H -> R.string.studio_style_dial
        WidgetStyleId.FREE_MARKET_SESSIONS -> R.string.studio_style_market_sessions
        WidgetStyleId.PREMIUM_GLOBAL_CLOCKS -> R.string.studio_style_global_clocks
        WidgetStyleId.PREMIUM_AMBIENT_GRID -> R.string.studio_style_ambient_grid
    },
)
