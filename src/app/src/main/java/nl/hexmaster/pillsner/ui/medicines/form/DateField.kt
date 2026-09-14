package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

/**
 * A date the user picks rather than types (docs/design-system.md section 8.11): a read-only field
 * that opens the Material date picker, with an optional clear affordance for a date that may be
 * left empty.
 *
 * @param minDate the earliest date the picker allows, or null for no lower bound.
 * @param onClear when given, the field shows a clear button; used for the optional end date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    onClear: (() -> Unit)? = null,
    errorMessage: String? = null,
    testTag: String = "",
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }

    Column(modifier) {
        OutlinedTextField(
            value = date?.format(formatter).orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = errorMessage != null,
            textStyle = MaterialTheme.typography.bodyLarge,
            trailingIcon = {
                if (onClear != null && date != null) {
                    IconButton(onClick = onClear) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.action_clear),
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
        )

        // A read-only text field does not take taps, so the row carries its own button.
        TextButton(
            onClick = { pickerOpen = true },
            modifier = Modifier.testTag(testTag + PICK_SUFFIX),
        ) {
            Text(stringResource(R.string.action_pick_date))
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = Spacing.lg),
            )
        }
    }

    if (pickerOpen) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).toEpochMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    minDate == null || utcTimeMillis >= minDate.toEpochMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onDateChange(it.toLocalDate()) }
                        pickerOpen = false
                    },
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { pickerOpen = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            shape = MaterialTheme.shapes.large,
        ) {
            DatePicker(state = state)
        }
    }
}

private const val PICK_SUFFIX = "_pick"

/** The picker deals in UTC midnight, which is how Material states a calendar day. */
private fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@PreviewLightDark
@Composable
private fun DateFieldPreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg)) {
                DateField(
                    label = "Used since",
                    date = LocalDate.of(2026, 9, 13),
                    onDateChange = {},
                )
            }
        }
    }
}
