package nl.hexmaster.pillsner.ui.settings.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Stable tags for the backup dialogs, for semantics tests. */
object BackupDialogTestTags {
    const val EXPORT_DIALOG = "backup_export_dialog"
    const val EXPORT_PASSPHRASE = "backup_export_passphrase"
    const val EXPORT_CONFIRMATION = "backup_export_confirmation"
    const val EXPORT_CONTINUE = "backup_export_continue"
    const val IMPORT_DIALOG = "backup_import_dialog"
    const val IMPORT_PASSPHRASE = "backup_import_passphrase"
    const val IMPORT_OPEN = "backup_import_open"
    const val CONFIRM_DIALOG = "backup_confirm_dialog"
    const val CONFIRM_CHECKBOX = "backup_confirm_checkbox"
    const val CONFIRM_REPLACE = "backup_confirm_replace"
    const val CANCEL = "backup_dialog_cancel"
}

/**
 * Whichever backup dialog [dialog] says is showing, or nothing.
 *
 * Every dialog is a pure function of its state: the typed text and the tick live in the view model,
 * so a rotation neither loses a half-typed passphrase nor leaves a stale tick behind.
 */
@Composable
fun BackupDialogHost(
    dialog: BackupDialog?,
    working: Boolean,
    callbacks: BackupCallbacks,
) {
    when (dialog) {
        null -> Unit
        is BackupDialog.ExportPassphrase -> ExportPassphraseDialog(dialog, callbacks)
        is BackupDialog.ImportPassphrase -> ImportPassphraseDialog(dialog, working, callbacks)
        is BackupDialog.ConfirmImport -> ConfirmImportDialog(dialog, working, callbacks)
    }
}

/** Everything the backup section and its dialogs can tell the view model. */
data class BackupCallbacks(
    val onExportTapped: () -> Unit,
    val onExportPassphraseChange: (String) -> Unit,
    val onExportConfirmationChange: (String) -> Unit,
    val onExportPassphraseConfirmed: () -> Unit,
    val onImportTapped: () -> Unit,
    val onImportPassphraseChange: (String) -> Unit,
    val onImportPassphraseConfirmed: () -> Unit,
    val onImportConfirmationToggled: (Boolean) -> Unit,
    val onImportConfirmed: () -> Unit,
    val onDialogDismissed: () -> Unit,
) {
    companion object {
        val NONE = BackupCallbacks({}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

/**
 * The export passphrase, typed twice. The warning that it cannot be recovered comes before the
 * fields, not after the export, so losing it is a choice the user made knowingly.
 */
@Composable
private fun ExportPassphraseDialog(state: BackupDialog.ExportPassphrase, callbacks: BackupCallbacks) {
    AlertDialog(
        onDismissRequest = callbacks.onDialogDismissed,
        modifier = Modifier.testTag(BackupDialogTestTags.EXPORT_DIALOG),
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.backup_export_dialog_title), style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.backup_export_dialog_body), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.backup_export_dialog_no_recovery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PassphraseField(
                    value = state.passphrase,
                    onValueChange = callbacks.onExportPassphraseChange,
                    label = stringResource(R.string.backup_field_passphrase),
                    imeAction = ImeAction.Next,
                    testTag = BackupDialogTestTags.EXPORT_PASSPHRASE,
                )
                PassphraseField(
                    value = state.confirmation,
                    onValueChange = callbacks.onExportConfirmationChange,
                    label = stringResource(R.string.backup_field_confirmation),
                    error = stringResource(R.string.backup_error_mismatch).takeIf { state.mismatch },
                    imeAction = ImeAction.Done,
                    testTag = BackupDialogTestTags.EXPORT_CONFIRMATION,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = callbacks.onExportPassphraseConfirmed,
                enabled = state.canProceed,
                modifier = Modifier
                    .heightIn(min = Sizes.minTouchTarget)
                    .testTag(BackupDialogTestTags.EXPORT_CONTINUE),
            ) {
                Text(stringResource(R.string.backup_export_dialog_confirm))
            }
        },
        dismissButton = { CancelButton(callbacks.onDialogDismissed, enabled = true) },
    )
}

/** The passphrase for the file already picked; a wrong one stays here, marked, to try again. */
@Composable
private fun ImportPassphraseDialog(state: BackupDialog.ImportPassphrase, working: Boolean, callbacks: BackupCallbacks) {
    AlertDialog(
        onDismissRequest = { if (!working) callbacks.onDialogDismissed() },
        modifier = Modifier.testTag(BackupDialogTestTags.IMPORT_DIALOG),
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.backup_import_dialog_title), style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.backup_import_dialog_body), style = MaterialTheme.typography.bodyLarge)
                PassphraseField(
                    value = state.passphrase,
                    onValueChange = callbacks.onImportPassphraseChange,
                    label = stringResource(R.string.backup_field_passphrase),
                    error = stringResource(R.string.backup_error_wrong_passphrase).takeIf { state.rejected },
                    imeAction = ImeAction.Done,
                    enabled = !working,
                    testTag = BackupDialogTestTags.IMPORT_PASSPHRASE,
                )
                if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = callbacks.onImportPassphraseConfirmed,
                enabled = state.canProceed && !working,
                modifier = Modifier
                    .heightIn(min = Sizes.minTouchTarget)
                    .testTag(BackupDialogTestTags.IMPORT_OPEN),
            ) {
                Text(stringResource(R.string.backup_import_dialog_confirm))
            }
        },
        dismissButton = { CancelButton(callbacks.onDialogDismissed, enabled = !working) },
    )
}

/**
 * The last gate before the replace, built like the reset confirmation: what goes, what stays, a
 * tick that must be set, and the destructive button in red because this one is destructive.
 */
@Composable
private fun ConfirmImportDialog(state: BackupDialog.ConfirmImport, working: Boolean, callbacks: BackupCallbacks) {
    AlertDialog(
        onDismissRequest = { if (!working) callbacks.onDialogDismissed() },
        modifier = Modifier.testTag(BackupDialogTestTags.CONFIRM_DIALOG),
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.backup_confirm_title), style = MaterialTheme.typography.headlineMedium) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.backup_confirm_replaced), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.backup_confirm_kept),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.minTouchTarget)
                        .toggleable(
                            value = state.accepted,
                            enabled = !working,
                            role = Role.Checkbox,
                            onValueChange = callbacks.onImportConfirmationToggled,
                        )
                        .testTag(BackupDialogTestTags.CONFIRM_CHECKBOX),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // Null: the row owns the toggle, so the box is not a second target.
                    Checkbox(checked = state.accepted, onCheckedChange = null)
                    Text(stringResource(R.string.backup_confirm_checkbox), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = callbacks.onImportConfirmed,
                enabled = state.accepted && !working,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier
                    .heightIn(min = Sizes.minTouchTarget)
                    .testTag(BackupDialogTestTags.CONFIRM_REPLACE),
            ) {
                Text(stringResource(R.string.backup_confirm_action))
            }
        },
        dismissButton = { CancelButton(callbacks.onDialogDismissed, enabled = !working) },
    )
}

@Composable
private fun PassphraseField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    testTag: String,
    error: String? = null,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        textStyle = MaterialTheme.typography.bodyLarge,
        isError = error != null,
        supportingText = error?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = imeAction),
        singleLine = true,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}

@Composable
private fun CancelButton(onClick: () -> Unit, enabled: Boolean) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .heightIn(min = Sizes.minTouchTarget)
            .testTag(BackupDialogTestTags.CANCEL),
    ) {
        Text(stringResource(R.string.action_cancel))
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ExportPassphraseDialogMismatchPreview() {
    PillsnerTheme {
        Surface {
            BackupDialogHost(BackupDialog.ExportPassphrase("secret", "secrex"), working = false, callbacks = BackupCallbacks.NONE)
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ImportPassphraseDialogRejectedPreview() {
    PillsnerTheme {
        Surface {
            BackupDialogHost(BackupDialog.ImportPassphrase(rejected = true), working = false, callbacks = BackupCallbacks.NONE)
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ConfirmImportDialogPreview() {
    PillsnerTheme {
        Surface {
            BackupDialogHost(BackupDialog.ConfirmImport(accepted = true), working = false, callbacks = BackupCallbacks.NONE)
        }
    }
}
