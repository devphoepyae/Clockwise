package com.pj95.clockwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.model.City
import com.pj95.clockwise.model.CityDirectory
import com.pj95.clockwise.common.time.CityTimeFormatter
import com.pj95.clockwise.ui.components.CircleIconButton
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.ClockwiseTextStyles
import java.time.Instant
import java.time.ZoneId

/** Test tags so UI tests can address search results without matching text. */
internal object CitySearchTags {
    const val Results = "citySearchResults"

    fun result(cityId: String): String = "citySearchResult_$cityId"
}

/**
 * City search presented as a bottom sheet (spec section 4.2).
 *
 * Matches on city name, region, country or IANA timezone, shows the current
 * local time and offset for each result, and marks cities that are already
 * saved so they cannot be added twice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchSheet(
    now: Instant,
    use24Hour: Boolean,
    savedCityIds: Set<String>,
    onAddCity: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = ClockwiseColors.Background,
        contentColor = ClockwiseColors.Foreground,
        scrimColor = Color(0x66000000),
        modifier = modifier,
    ) {
        CitySearchContent(
            query = query,
            onQueryChange = { query = it },
            savedCityIds = savedCityIds,
            now = now,
            use24Hour = use24Hour,
            onAddCity = onAddCity,
            onDismiss = onDismiss,
        )
    }
}

/**
 * The sheet body. Kept separate from the sheet host so it can be rendered
 * directly in previews and screenshot tests.
 */
@Composable
internal fun CitySearchContent(
    query: String,
    onQueryChange: (String) -> Unit,
    savedCityIds: Set<String>,
    now: Instant,
    use24Hour: Boolean,
    onAddCity: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
) {
    val results = remember(query) { CityDirectory.search(query) }
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.search_title),
                style = MaterialTheme.typography.titleLarge,
                color = ClockwiseColors.Foreground,
                modifier = Modifier.weight(1f),
            )
            CircleIconButton(
                iconRes = R.drawable.ic_close,
                contentDescription = stringResource(R.string.search_close),
                onClick = onDismiss,
                size = 40.dp,
                iconSize = 18.dp,
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    color = ClockwiseColors.MutedForeground,
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = ClockwiseColors.MutedForeground,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.search_clear),
                            tint = ClockwiseColors.MutedForeground,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            singleLine = true,
            shape = ClockwiseShapes.medium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ClockwiseColors.Foreground,
                unfocusedTextColor = ClockwiseColors.Foreground,
                focusedContainerColor = ClockwiseColors.Card,
                unfocusedContainerColor = ClockwiseColors.Card,
                cursorColor = ClockwiseColors.Foreground,
                focusedBorderColor = ClockwiseColors.Foreground,
                unfocusedBorderColor = ClockwiseColors.Border,
            ),
        )

        Spacer(Modifier.height(16.dp))

        if (results.isEmpty()) {
            NoSearchResults()
        } else {
            LazyColumn(
                modifier = Modifier
                    .testTag(CitySearchTags.Results)
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items = results, key = { it.id }) { city ->
                    SearchResultRow(
                        city = city,
                        now = now,
                        use24Hour = use24Hour,
                        isSaved = city.id in savedCityIds,
                        onAdd = { onAddCity(city.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
private fun SearchResultRow(
    city: City,
    now: Instant,
    use24Hour: Boolean,
    isSaved: Boolean,
    onAdd: () -> Unit,
) {
    val time = remember(city.id, now, use24Hour) {
        CityTimeFormatter.snapshot(ZoneId.of(city.timeZone), now, use24Hour)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CitySearchTags.result(city.id))
            .clip(ClockwiseShapes.medium)
            .clickable(
                enabled = !isSaved,
                onClickLabel = stringResource(R.string.a11y_add_named_city, city.name),
                onClick = onAdd,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
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

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = time.timeText,
                style = ClockwiseTextStyles.SearchResultMeta,
                color = ClockwiseColors.Foreground,
            )
            Text(
                text = time.offsetLabel,
                style = ClockwiseTextStyles.SearchResultMeta,
                color = ClockwiseColors.MutedForeground,
            )
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (isSaved) ClockwiseColors.Primary else ClockwiseColors.Card,
                )
                .border(
                    width = 1.dp,
                    color = if (isSaved) ClockwiseColors.Primary else ClockwiseColors.Border,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (isSaved) R.drawable.ic_check else R.drawable.ic_add,
                ),
                contentDescription = if (isSaved) {
                    stringResource(R.string.a11y_city_already_added, city.name)
                } else {
                    null
                },
                tint = if (isSaved) {
                    ClockwiseColors.PrimaryForeground
                } else {
                    ClockwiseColors.Foreground
                },
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun NoSearchResults() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = ClockwiseColors.MutedForeground,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.search_no_results_title),
            style = MaterialTheme.typography.titleMedium,
            color = ClockwiseColors.Foreground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.search_no_results_hint),
            style = MaterialTheme.typography.bodySmall,
            color = ClockwiseColors.MutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}
