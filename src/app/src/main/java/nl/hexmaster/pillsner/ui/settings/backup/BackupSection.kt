package nl.hexmaster.pillsner.ui.settings.backup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Stable tags for the backup section, for semantics tests. */
object BackupSectionTestTags {
    const val HEADING = "backup_heading"
    const val EXPORT = "backup_export"
    const val IMPORT = "backup_import"
    const val PROGRESS = "backup_progress"
}

/**
 * The Your data section of Settings: export and import (`data-backup-restore`).
 *
 * Placed directly above the danger zone and styled as an ordinary section, not a dangerous one:
 * exporting changes nothing, and importing has its own confirmation, which is where the red lives.
 * Both rows only start a flow; while one runs they are disabled and a labelled progress bar says
 * what is happening, so a slow phone never looks frozen.
 */
@Composable
fun BackupSection(
    state: BackupUiState,
    onExportTapped: () -> Unit,
    onImportTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.backup_header),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .semantics { heading() }
                .testTag(BackupSectionTestTags.HEADING),
        )

        Text(
            text = stringResource(R.string.backup_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column {
            ActionRow(
                title = stringResource(R.string.backup_export_title),
                summary = stringResource(R.string.backup_export_summary),
                enabled = state.actionsEnabled,
                onClick = onExportTapped,
                testTag = BackupSectionTestTags.EXPORT,
            )
            ActionRow(
                title = stringResource(R.string.backup_import_title),
                summary = stringResource(R.string.backup_import_summary),
                enabled = state.actionsEnabled,
                onClick = onImportTapped,
                testTag = BackupSectionTestTags.IMPORT,
            )
        }

        state.work?.let { work -> WorkInProgress(work) }
    }
}

@Composable
private fun ActionRow(title: String, summary: String, enabled: Boolean, onClick: () -> Unit, testTag: String) {
    // Disabled rows read in onSurfaceVariant rather than at reduced alpha (design system 8.2).
    val textColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        headlineContent = { Text(text = title, style = MaterialTheme.typography.titleSmall) },
        supportingContent = { Text(summary) },
        colors = ListItemDefaults.colors(headlineColor = textColor),
        modifier = Modifier
            .heightIn(min = Sizes.minTouchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag(testTag),
    )
}

@Composable
private fun WorkInProgress(work: BackupWork) {
    val label = stringResource(
        when (work) {
            BackupWork.EXPORTING -> R.string.backup_progress_exporting
            BackupWork.READING -> R.string.backup_progress_reading
            BackupWork.IMPORTING -> R.string.backup_progress_importing
        },
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier
            .padding(horizontal = Spacing.lg)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .testTag(BackupSectionTestTags.PROGRESS),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}

@PreviewLightDark
@Composable
private fun BackupSectionPreview() {
    PillsnerTheme {
        Surface {
            BackupSection(BackupUiState(), onExportTapped = {}, onImportTapped = {}, modifier = Modifier.padding(Spacing.lg))
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun BackupSectionWorkingPreview() {
    PillsnerTheme {
        Surface {
            BackupSection(
                BackupUiState(work = BackupWork.EXPORTING),
                onExportTapped = {},
                onImportTapped = {},
                modifier = Modifier.padding(Spacing.lg),
            )
        }
    }
}

/**
 * The snackbar text for each [BackupMessage], resolved while composing so the collector that shows
 * them needs no context. Each failure says what happened and that nothing on the device changed.
 */
@Composable
fun backupMessageTexts(): (BackupMessage) -> String {
    val texts = BackupMessage.entries.associateWith { message ->
        stringResource(
            when (message) {
                BackupMessage.EXPORT_DONE -> R.string.backup_message_export_done
                BackupMessage.EXPORT_CANCELLED -> R.string.backup_message_export_cancelled
                BackupMessage.EXPORT_FAILED -> R.string.backup_message_export_failed
                BackupMessage.IMPORT_DONE -> R.string.backup_message_import_done
                BackupMessage.IMPORT_CANCELLED -> R.string.backup_message_import_cancelled
                BackupMessage.IMPORT_FAILED -> R.string.backup_message_import_failed
                BackupMessage.NOT_A_BACKUP -> R.string.backup_message_not_a_backup
                BackupMessage.NEWER_VERSION -> R.string.backup_message_newer_version
                BackupMessage.READ_FAILED -> R.string.backup_message_read_failed
            },
        )
    }
    return texts::getValue
}
