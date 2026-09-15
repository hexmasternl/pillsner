package nl.hexmaster.pillsner.ui.settings.reset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Stable tags for the confirmation dialog, for semantics tests. */
object ResetDialogTestTags {
    const val DIALOG = "reset_dialog"
    const val CHECKBOX = "reset_dialog_checkbox"
    const val CONFIRM = "reset_dialog_confirm"
    const val CANCEL = "reset_dialog_cancel"
}

/**
 * The confirmation in front of the reset (design D7, and design system 8.12).
 *
 * Two paragraphs, not one: a list of casualties without a list of survivors reads as "everything",
 * and that would be a lie — the language, the app lock and the accepted documents all stay. So the
 * dialog names what goes *and* what does not.
 *
 * The checkbox is the gate. The confirm button is disabled until it is ticked, which is not a
 * validation message — nothing is wrong yet, the user simply has not confirmed. Cancel, system back
 * and a tap outside are one path and all three erase nothing.
 *
 * A pure function of [accepted]: the tick lives in the view model, so a configuration change can
 * neither untick it silently nor leave it ticked over a dialog that was recreated.
 */
@Composable
fun ResetAppDialog(
    accepted: Boolean,
    confirmEnabled: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(ResetDialogTestTags.DIALOG),
        shape = MaterialTheme.shapes.large,
        title = {
            Text(
                text = stringResource(R.string.reset_dialog_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    text = stringResource(R.string.reset_dialog_erased),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.reset_dialog_kept),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.minTouchTarget)
                        // The whole row toggles, so the label is part of the target rather than
                        // text beside a small box.
                        .toggleable(
                            value = accepted,
                            role = Role.Checkbox,
                            onValueChange = onAcceptedChange,
                        )
                        .testTag(ResetDialogTestTags.CHECKBOX),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // Null, not a lambda: the row above owns the toggle, so the box must not be a
                    // second target announcing itself separately.
                    Checkbox(checked = accepted, onCheckedChange = null)
                    Text(
                        text = stringResource(R.string.reset_dialog_checkbox),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier
                    .heightIn(min = Sizes.minTouchTarget)
                    .testTag(ResetDialogTestTags.CONFIRM),
            ) {
                Text(stringResource(R.string.reset_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = Sizes.minTouchTarget)
                    .testTag(ResetDialogTestTags.CANCEL),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun ResetAppDialogUntickedPreview() {
    PillsnerTheme {
        Surface {
            ResetAppDialog(accepted = false, confirmEnabled = false, onAcceptedChange = {}, onConfirm = {}, onDismiss = {})
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ResetAppDialogTickedPreview() {
    PillsnerTheme {
        Surface {
            ResetAppDialog(accepted = true, confirmEnabled = true, onAcceptedChange = {}, onConfirm = {}, onDismiss = {})
        }
    }
}
