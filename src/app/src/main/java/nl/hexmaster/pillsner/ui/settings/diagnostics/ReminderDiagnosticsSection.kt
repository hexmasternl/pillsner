package nl.hexmaster.pillsner.ui.settings.diagnostics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes

/** Stable tags for the Reminders section's row, for semantics tests. */
object ReminderDiagnosticsSectionTestTags {
    const val LOG_ROW = "reminder_diagnostics_row"
}

/**
 * The Reminders section of the Settings screen: one row that opens the delivery log.
 *
 * It takes the same shape as the About row beneath it, so Settings still reads as one list. The
 * supporting text says what the log is for, because a person who has just missed a dose is the
 * one looking for it.
 *
 * @param onLogTapped opens the delivery log.
 */
@Composable
fun ReminderDiagnosticsSection(
    onLogTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.reminder_diagnostics_header),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.reminder_diagnostics_row_title),
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            supportingContent = {
                Text(stringResource(R.string.reminder_diagnostics_row_summary))
            },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.iconDefault),
                )
            },
            modifier = Modifier
                .heightIn(min = Sizes.minTouchTarget)
                .clickable(role = Role.Button, onClick = onLogTapped)
                .testTag(ReminderDiagnosticsSectionTestTags.LOG_ROW),
        )
    }
}

@PreviewLightDark
@Composable
private fun ReminderDiagnosticsSectionPreview() {
    PillsnerTheme {
        ReminderDiagnosticsSection(onLogTapped = {})
    }
}

@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ReminderDiagnosticsSectionLargeFontPreview() {
    PillsnerTheme {
        ReminderDiagnosticsSection(onLogTapped = {})
    }
}
