package nl.hexmaster.pillsner.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.BatchExpiryState
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.StockWarning
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme

/** Test tag for semantics tests of the stock warning dialog. */
object StockWarningDialogTestTags {
    const val DIALOG = "stock_warning_dialog"
}

/**
 * The stock warning shown after a taken dose, when either or both of low stock and an
 * approaching/past batch expiry apply (`medicine-stock-tracking`'s "Combined warning presentation").
 *
 * "I ordered new" is offered only alongside the low-stock message, since it is what suppresses that
 * one; the expiry message, when present, always dismisses with the same "OK" the low-stock message
 * would use on its own, since only using up or replacing that batch changes the underlying fact.
 */
@Composable
fun StockWarningDialog(
    warning: StockWarning,
    onOk: (MedicationId) -> Unit,
    onOrderedNew: (MedicationId) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onOk(warning.medicationId) },
        title = { Text(warning.medicationName) },
        text = {
            Column {
                if (warning.lowStock) {
                    Text(
                        stringResource(R.string.stock_warning_low_stock_message, warning.medicationName),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                when (warning.expiryState) {
                    BatchExpiryState.APPROACHING -> Text(
                        stringResource(R.string.stock_warning_expiring_message, warning.medicationName),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    BatchExpiryState.PAST -> Text(
                        stringResource(R.string.stock_warning_expired_message, warning.medicationName),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    BatchExpiryState.NONE -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onOk(warning.medicationId) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = if (warning.lowStock) {
            {
                TextButton(onClick = { onOrderedNew(warning.medicationId) }) {
                    Text(stringResource(R.string.stock_warning_action_ordered_new))
                }
            }
        } else {
            null
        },
    )
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun StockWarningDialogLowStockPreview() {
    PillsnerTheme {
        StockWarningDialog(
            warning = StockWarning(MedicationId(1), "Metoprolol", lowStock = true, expiryState = BatchExpiryState.NONE),
            onOk = {},
            onOrderedNew = {},
        )
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun StockWarningDialogBothPreview() {
    PillsnerTheme {
        StockWarningDialog(
            warning = StockWarning(
                MedicationId(1),
                "Metoprolol",
                lowStock = true,
                expiryState = BatchExpiryState.APPROACHING,
            ),
            onOk = {},
            onOrderedNew = {},
        )
    }
}
