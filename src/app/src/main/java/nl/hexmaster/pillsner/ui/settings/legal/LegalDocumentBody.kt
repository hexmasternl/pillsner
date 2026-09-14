package nl.hexmaster.pillsner.ui.settings.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.domain.legal.CurrentLegalDocuments
import nl.hexmaster.pillsner.domain.legal.LegalDocument
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

/**
 * One legal document as headed prose (design D6, D7). Shared by the document screen and the
 * acceptance screen, so the text a user accepts is exactly the text they can read again later.
 *
 * Every heading is a semantic heading, so a screen reader user moves through a long document by
 * section instead of hearing it linearly. Nothing here sets `maxLines`: at the largest font scale
 * the text grows and the caller's scroll takes the strain.
 */
@Composable
fun LegalDocumentBody(
    document: LegalDocument,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        document.sections.forEachIndexed { index, section ->
            if (index > 0) Spacer(Modifier.height(Spacing.xl))

            Text(
                text = stringResource(section.heading.resourceId),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )

            section.paragraphs.forEach { paragraph ->
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = stringResource(paragraph.resourceId),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun LegalDocumentBodyPreview() {
    PillsnerTheme {
        Surface {
            LegalDocumentBody(CurrentLegalDocuments.disclaimer)
        }
    }
}

@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun LegalDocumentBodyLargeFontPreview() {
    PillsnerTheme {
        Surface {
            LegalDocumentBody(CurrentLegalDocuments.terms)
        }
    }
}
