package com.pj95.clockwise.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pj95.clockwise.core.ui.R
import com.pj95.clockwise.ui.samples.PreviewSamples
import com.pj95.clockwise.ui.theme.ClockwiseColors
import com.pj95.clockwise.ui.theme.ClockwiseShapes
import com.pj95.clockwise.ui.theme.FixedClockwiseTheme

/** Longest label the dialog accepts, so a label can never swamp the layout. */
internal const val CITY_LABEL_MAX_LENGTH = 40

/** Test tags so UI tests can address the dialog without matching placeholder text. */
internal object CityLabelDialogTags {
    const val FIELD = "cityLabelField"
}

/**
 * The dim behind the dialog. The bottom sheets use the same value; it is
 * deliberately not a palette token, because a scrim is dark in both themes.
 */
private val DialogScrim = Color(0x66000000)

/**
 * Edit dialog for a city's personal label (spec section 4.6).
 *
 * Built as a modal overlay inside the app's own composition rather than as a
 * platform dialog window. That keeps it drawn from the same monochrome palette
 * as everything else — a platform dialog brings its own shape, elevation and
 * button styling — and it means the dialog is covered by the same screenshot
 * and behaviour tests as the rest of the UI. Dismissal is offered three ways:
 * Cancel, the system back gesture, and a tap outside the card.
 *
 * @param onSave the new label, or `null` to clear it
 */
@Composable
fun CityLabelDialog(
    cityName: String,
    currentLabel: String?,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by rememberSaveable(currentLabel) { mutableStateOf(currentLabel.orEmpty()) }

    BackHandler(enabled = true, onBack = onDismiss)

    val dialogTitle = stringResource(R.string.label_dialog_title)
    val fieldDescription = stringResource(R.string.a11y_city_label_field, cityName)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DialogScrim)
            // A tap on the scrim dismisses; the card below swallows its own taps
            // before they ever reach here.
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(ClockwiseShapes.medium)
                .background(ClockwiseColors.Card)
                .border(1.dp, ClockwiseColors.Border, ClockwiseShapes.medium)
                // Tells assistive technology that a new pane is on screen; the
                // dialog is drawn inside this app's composition rather than in a
                // platform dialog window, so nothing else announces it.
                .semantics { paneTitle = dialogTitle }
                // Keeps taps on the card away from the dismiss handler above.
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(24.dp),
        ) {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.titleLarge,
                color = ClockwiseColors.Foreground,
                modifier = Modifier.semantics { heading() },
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.label_dialog_support),
                style = MaterialTheme.typography.bodySmall,
                color = ClockwiseColors.MutedForeground,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = value,
                onValueChange = { entered ->
                    if (entered.length <= CITY_LABEL_MAX_LENGTH) value = entered
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(CityLabelDialogTags.FIELD)
                    // The heading above names the feature; the field has to name
                    // the city, or "edit box" is all a screen-reader user hears.
                    .semantics { contentDescription = fieldDescription },
                placeholder = {
                    Text(
                        text = stringResource(R.string.label_dialog_placeholder),
                        color = ClockwiseColors.MutedForeground,
                    )
                },
                singleLine = true,
                shape = ClockwiseShapes.medium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onSave(value.trim().ifEmpty { null }) },
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ClockwiseColors.Foreground,
                    unfocusedTextColor = ClockwiseColors.Foreground,
                    focusedContainerColor = ClockwiseColors.Background,
                    unfocusedContainerColor = ClockwiseColors.Background,
                    cursorColor = ClockwiseColors.Foreground,
                    focusedBorderColor = ClockwiseColors.Foreground,
                    unfocusedBorderColor = ClockwiseColors.Border,
                ),
            )

            Spacer(Modifier.height(8.dp))

            // Removing sits on its own line: three buttons on one row at 360 dp
            // squeezes Save into two lines.
            if (currentLabel != null) {
                Row {
                    TextButton(
                        onClick = { onSave(null) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = ClockwiseColors.MapAccent,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.label_dialog_remove),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                HorizontalDivider(color = ClockwiseColors.Border)

                Spacer(Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ClockwiseColors.MutedForeground,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.label_dialog_cancel),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { onSave(value.trim().ifEmpty { null }) },
                    shape = ClockwiseShapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClockwiseColors.Primary,
                        contentColor = ClockwiseColors.PrimaryForeground,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.label_dialog_save),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Preview(name = "City label dialog 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun CityLabelDialogPreview() {
    FixedClockwiseTheme {
        Box(Modifier.fillMaxSize()) {
            ClocksScreen(
                state = PreviewSamples.clockState(),
                onCityClick = {},
                onAddClick = {},
                onFindCityClick = {},
                onSettingsClick = {},
                onPlanMeetingClick = {},
            )
            CityLabelDialog(
                cityName = "Tokyo",
                currentLabel = "Head Office",
                onSave = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(name = "City label dialog empty 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun CityLabelDialogEmptyPreview() {
    FixedClockwiseTheme {
        Box(Modifier.fillMaxSize()) {
            ClocksScreen(
                state = PreviewSamples.clockState(),
                onCityClick = {},
                onAddClick = {},
                onFindCityClick = {},
                onSettingsClick = {},
                onPlanMeetingClick = {},
            )
            CityLabelDialog(
                cityName = "Tokyo",
                currentLabel = null,
                onSave = {},
                onDismiss = {},
            )
        }
    }
}
