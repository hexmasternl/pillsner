package nl.hexmaster.pillsner.ui.medicines.form

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme

/** Test tags for the discard confirmation. */
object DiscardDialogTestTags {
    const val DIALOG = "discard_dialog"
    const val DISCARD = "discard_dialog_discard"
    const val KEEP = "discard_dialog_keep"
}

/**
 * Asks before throwing away a half-filled medicine (docs/design-system.md section 8.12).
 *
 * Neither action is red: dropping an unsaved draft is not one of the destructive cases the design
 * system reserves the error colour for.
 */
@Composable
fun DiscardDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = {
            Text(
                text = stringResource(R.string.medicine_discard_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.medicine_discard_body),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            TextButton(onClick = onDiscard, modifier = Modifier.testTag(DiscardDialogTestTags.DISCARD)) {
                Text(stringResource(R.string.action_discard))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing, modifier = Modifier.testTag(DiscardDialogTestTags.KEEP)) {
                Text(stringResource(R.string.action_keep_editing))
            }
        },
        shape = MaterialTheme.shapes.large,
        modifier = modifier.testTag(DiscardDialogTestTags.DIALOG),
    )
}

@PreviewLightDark
@Composable
private fun DiscardDialogPreview() {
    PillsnerTheme { DiscardDialog(onDiscard = {}, onKeepEditing = {}) }
}
