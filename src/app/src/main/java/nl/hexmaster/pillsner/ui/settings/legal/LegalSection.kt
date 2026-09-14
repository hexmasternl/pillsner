package nl.hexmaster.pillsner.ui.settings.legal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import java.time.LocalDate
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.legal.LegalDocumentId
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Stable tags for the Legal section's rows, for semantics tests. */
object LegalSectionTestTags {
    const val DISCLAIMER_ROW = "legal_disclaimer_row"
    const val TERMS_ROW = "legal_terms_row"
    const val STATUS = "legal_status"
}

/**
 * The Legal section of the Settings screen (design D8): the Disclaimer, the Terms of Service, and
 * one line saying what was accepted and when.
 *
 * The rows are shaped exactly like the Security section's "Change PIN" row, so Settings reads as
 * one list rather than as several screens stacked. The status line is informational only: there is
 * no way to accept here, because acceptance belongs to the moment a medicine is added, and no way
 * to withdraw it, because that would mean losing data the user never asked to lose.
 */
@Composable
fun LegalSection(
    state: LegalAcceptanceState,
    onOpenDocument: (LegalDocumentId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.legal_section_header),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        DocumentRow(
            label = R.string.legal_row_disclaimer,
            testTag = LegalSectionTestTags.DISCLAIMER_ROW,
            onClick = { onOpenDocument(LegalDocumentId.DISCLAIMER) },
        )

        DocumentRow(
            label = R.string.legal_row_terms,
            testTag = LegalSectionTestTags.TERMS_ROW,
            onClick = { onOpenDocument(LegalDocumentId.TERMS) },
        )

        Text(
            text = state.describe(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                .testTag(LegalSectionTestTags.STATUS),
        )
    }
}

/** One document row: its name, a chevron, and the whole row as the target. */
@Composable
private fun DocumentRow(label: Int, testTag: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.titleSmall,
            )
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
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag),
    )
}

/** The one line of status, in the app's own language and date format. */
@Composable
private fun LegalAcceptanceState.describe(): String {
    val formatter = rememberLegalDateFormatter()
    return when (this) {
        LegalAcceptanceState.NeverAccepted -> stringResource(R.string.legal_status_not_accepted)
        is LegalAcceptanceState.Accepted ->
            stringResource(R.string.legal_status_accepted, on.format(formatter))
        is LegalAcceptanceState.RevisedSince ->
            stringResource(R.string.legal_status_revised, on.format(formatter))
    }
}

@PreviewLightDark
@Composable
private fun LegalSectionNeverAcceptedPreview() {
    PillsnerTheme {
        Surface {
            LegalSection(state = LegalAcceptanceState.NeverAccepted, onOpenDocument = {})
        }
    }
}

@PreviewLightDark
@Composable
private fun LegalSectionAcceptedPreview() {
    PillsnerTheme {
        Surface {
            LegalSection(
                state = LegalAcceptanceState.Accepted(LocalDate.of(2026, 9, 14)),
                onOpenDocument = {},
            )
        }
    }
}

@Preview(name = "Revised since, large font", fontScale = 2f)
@Composable
private fun LegalSectionRevisedPreview() {
    PillsnerTheme {
        Surface {
            LegalSection(
                state = LegalAcceptanceState.RevisedSince(LocalDate.of(2026, 9, 14)),
                onOpenDocument = {},
            )
        }
    }
}
