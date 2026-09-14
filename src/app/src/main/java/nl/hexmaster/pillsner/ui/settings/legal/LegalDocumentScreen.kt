package nl.hexmaster.pillsner.ui.settings.legal

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.legal.CurrentLegalDocuments
import nl.hexmaster.pillsner.domain.legal.LegalDocument
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Stable tags for the legal document screen, for semantics tests. */
object LegalDocumentTestTags {
    const val TITLE = "legal_document_title"
    const val VERSION = "legal_document_version"
    const val BODY = "legal_document_body"
}

/**
 * One legal document, read-only (design D7): a top app bar with the document's title and a back
 * arrow, the version and the date it took effect beneath it, then the document itself.
 *
 * No view model: the content is constant for the life of the process and comes from the route plus
 * [CurrentLegalDocuments]. There is nothing to accept here and nothing to withdraw — acceptance
 * belongs to the moment a medicine is added.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentScreen(
    document: LegalDocument,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(document.title.resourceId),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag(LegalDocumentTestTags.TITLE),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val sidePadding =
                if (maxWidth >= Spacing.contentMaxWidth) Spacing.screenEdgeWide else Spacing.screenEdge

            Column(
                Modifier
                    .fillMaxSize()
                    .widthIn(max = Spacing.contentMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = sidePadding)
                    .padding(top = Spacing.lg, bottom = Spacing.xxl),
            ) {
                Text(
                    text = stringResource(
                        R.string.legal_document_version,
                        document.version,
                        document.effectiveDate.format(rememberLegalDateFormatter()),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(LegalDocumentTestTags.VERSION),
                )

                Spacer(Modifier.height(Spacing.xl))

                LegalDocumentBody(
                    document = document,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(LegalDocumentTestTags.BODY),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun LegalDocumentScreenPreview() {
    PillsnerTheme {
        LegalDocumentScreen(document = CurrentLegalDocuments.disclaimer, onBack = {})
    }
}

@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun LegalDocumentScreenLargeFontPreview() {
    PillsnerTheme {
        LegalDocumentScreen(document = CurrentLegalDocuments.terms, onBack = {})
    }
}
