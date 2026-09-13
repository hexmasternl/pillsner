package nl.hexmaster.pillsner.ui.medicines.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.Dialog
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import nl.hexmaster.pillsner.ui.theme.TabularNumbers

/** Test tags for the times list. */
object TimesListTestTags {
    const val ROW = "times_list_row"
    const val REMOVE = "times_list_remove"
    const val ADD = "times_list_add"
}

/**
 * The clock times a schedule fires at: one row per time, ascending, each removable, with one
 * button that opens the Material time picker. Times are never free text (design system 8.11).
 */
@Composable
fun TimesList(
    times: List<LocalTime>,
    onTimeAdded: (LocalTime) -> Unit,
    onTimeRemoved: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val formatter = rememberTimeFormatter()

    Column(modifier.fillMaxWidth()) {
        times.forEach { time ->
            ListItem(
                headlineContent = {
                    Text(
                        text = time.format(formatter),
                        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TabularNumbers),
                    )
                },
                trailingContent = {
                    IconButton(
                        onClick = { onTimeRemoved(time) },
                        modifier = Modifier
                            .sizeIn(minWidth = Sizes.minTouchTarget, minHeight = Sizes.minTouchTarget)
                            .testTag(TimesListTestTags.REMOVE),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.schedule_remove_time_content_description),
                            modifier = Modifier.size(Sizes.iconDefault),
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TimesListTestTags.ROW),
            )
        }

        FilledTonalButton(
            onClick = { pickerOpen = true },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.minTouchTarget)
                .testTag(TimesListTestTags.ADD),
        ) {
            Text(stringResource(R.string.schedule_add_time))
        }
    }

    if (pickerOpen) {
        TimePickerDialog(
            initial = times.lastOrNull() ?: DEFAULT_TIME,
            onConfirm = {
                onTimeAdded(it)
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

/** Material 3 has no ready-made time picker dialog, so this is the documented composition. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(
            androidx.compose.ui.platform.LocalContext.current,
        ),
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(Spacing.xl)) {
                TimePicker(state = state)
                Column(Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) },
                        modifier = Modifier.align(androidx.compose.ui.Alignment.End),
                    ) {
                        Text(stringResource(R.string.action_ok))
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.End),
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        }
    }
}

/** A time in the device's own short format, so 08:00 or 8:00 AM as the user expects. */
@Composable
fun rememberTimeFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }
}

private val DEFAULT_TIME: LocalTime = LocalTime.of(8, 0)

@PreviewLightDark
@Composable
private fun TimesListPreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg)) {
                TimesList(
                    times = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                    onTimeAdded = {},
                    onTimeRemoved = {},
                )
            }
        }
    }
}
