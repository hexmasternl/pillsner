package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.foundation.text.KeyboardOptions
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.ui.medicines.labelRes
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tags for the shared quantity field. */
object QuantityFieldTestTags {
    const val AMOUNT = "quantity_amount"
    const val UNIT = "quantity_unit"
}

/**
 * How much of something: a decimal amount beside its unit (docs/design-system.md section 8.11).
 *
 * The amount stays exactly as typed, so a half-entered "2." is never rewritten; parsing happens in
 * the view model, which knows the locale. Used by both the medicine's default dose and every
 * schedule's own amount.
 *
 * @param label the field's label, already resolved.
 * @param errorMessage the message to show under the field, or null when there is nothing wrong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuantityField(
    label: String,
    amount: String,
    unit: DoseUnit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (DoseUnit) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    testTagPrefix: String = "",
) {
    var unitMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text(label) },
                isError = errorMessage != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTagPrefix + QuantityFieldTestTags.AMOUNT),
            )

            ExposedDropdownMenuBox(
                expanded = unitMenuExpanded,
                onExpandedChange = { unitMenuExpanded = it },
                modifier = Modifier.width(UNIT_FIELD_WIDTH),
            ) {
                OutlinedTextField(
                    value = stringResource(unit.labelRes()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.medicine_field_unit)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitMenuExpanded) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag(testTagPrefix + QuantityFieldTestTags.UNIT),
                )
                ExposedDropdownMenu(
                    expanded = unitMenuExpanded,
                    onDismissRequest = { unitMenuExpanded = false },
                ) {
                    DoseUnit.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.labelRes())) },
                            onClick = {
                                onUnitChange(option)
                                unitMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = Spacing.lg, top = Spacing.xs),
            )
        }
    }
}

/** Wide enough for "capsule" beside the chevron without truncating it. */
private val UNIT_FIELD_WIDTH = Sizes.minTouchTarget * 3

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun QuantityFieldPreview() {
    PillsnerTheme {
        Surface {
            Column(
                Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                QuantityField(
                    label = "Default dose",
                    amount = "40",
                    unit = DoseUnit.MILLIGRAM,
                    onAmountChange = {},
                    onUnitChange = {},
                )
                QuantityField(
                    label = "Default dose",
                    amount = "0",
                    unit = DoseUnit.TABLET,
                    onAmountChange = {},
                    onUnitChange = {},
                    errorMessage = "Enter an amount greater than zero.",
                )
            }
        }
    }
}
