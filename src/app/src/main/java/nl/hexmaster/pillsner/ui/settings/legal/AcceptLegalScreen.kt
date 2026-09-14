package nl.hexmaster.pillsner.ui.settings.legal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.legal.CurrentLegalDocuments
import nl.hexmaster.pillsner.domain.legal.LegalDocument
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Stable tags for the acceptance screen, for semantics tests. */
object AcceptLegalTestTags {
    const val BODY = "legal_accept_body"
    const val TERMS_LINK = "legal_accept_terms_link"
    const val ACCEPT = "legal_accept_button"
    const val SCROLL_HINT = "legal_accept_scroll_hint"
}

/**
 * The gate in front of adding the first medicine (design D6): the Disclaimer in full, the Terms of
 * Service one tap away, and one action that accepts both.
 *
 * The accept action stays disabled until the disclaimer has been scrolled to its end, because a
 * user who accepts text that was never on screen has not been told anything, which is the whole
 * reason this screen exists. A disclaimer that fits without scrolling is already at its end, so on
 * a large screen the action is live at once.
 *
 * There is no decline action. The back arrow and system back are the decline, and they return to
 * Medicines with nothing recorded and nothing added.
 *
 * @param scrollState hoisted so the position survives a visit to the Terms of Service and a
 * rotation: the user comes back to the paragraph they left.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcceptLegalScreen(
    disclaimer: LegalDocument,
    onBack: () -> Unit,
    onReadTerms: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
) {
    // Content shorter than the viewport never scrolls, so maxValue is 0 and it counts as read.
    val readToEnd by remember { derivedStateOf { scrollState.value >= scrollState.maxValue } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.legal_accept_title),
                        style = MaterialTheme.typography.titleLarge,
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

            Column(Modifier.widthIn(max = Spacing.contentMaxWidth)) {
                // The document scrolls; the action below it does not, so it stays reachable at any
                // font scale without the user having to find the end of the text first.
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = sidePadding)
                        .padding(top = Spacing.lg, bottom = Spacing.xl),
                ) {
                    LegalDocumentBody(
                        document = disclaimer,
                        modifier = Modifier.testTag(AcceptLegalTestTags.BODY),
                    )

                    Spacer(Modifier.height(Spacing.lg))

                    TextButton(
                        onClick = onReadTerms,
                        modifier = Modifier
                            .heightIn(min = Sizes.minTouchTarget)
                            .testTag(AcceptLegalTestTags.TERMS_LINK),
                    ) {
                        Text(stringResource(R.string.legal_accept_terms_link))
                    }
                }

                HorizontalDivider()

                AcceptAction(
                    enabled = readToEnd,
                    onAccept = onAccept,
                    modifier = Modifier.padding(
                        start = sidePadding,
                        end = sidePadding,
                        top = Spacing.lg,
                        bottom = Spacing.lg,
                    ),
                )
            }
        }
    }
}

/**
 * The one action, with the reason it is not yet available.
 *
 * The line beneath it is a live region, so a screen reader user hears the action come alive as they
 * reach the end of the text rather than having to move focus back to find out. The same sentence is
 * the button's own state description while it is disabled, so focusing it explains itself too.
 */
@Composable
private fun AcceptAction(
    enabled: Boolean,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hint = stringResource(R.string.legal_accept_scroll_hint)
    val nowEnabled = stringResource(R.string.legal_accept_now_enabled)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Button(
            onClick = onAccept,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.primaryActionHeight)
                .semantics { if (!enabled) stateDescription = hint }
                .testTag(AcceptLegalTestTags.ACCEPT),
        ) {
            Text(stringResource(R.string.legal_accept_button))
        }

        Text(
            text = if (enabled) nowEnabled else hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .semantics { liveRegion = LiveRegionMode.Polite }
                .testTag(AcceptLegalTestTags.SCROLL_HINT),
        )
    }
}

@PreviewLightDark
@Composable
private fun AcceptLegalScreenPreview() {
    PillsnerTheme {
        Surface {
            AcceptLegalScreen(
                disclaimer = CurrentLegalDocuments.disclaimer,
                onBack = {},
                onReadTerms = {},
                onAccept = {},
            )
        }
    }
}

@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun AcceptLegalScreenLargeFontPreview() {
    PillsnerTheme {
        Surface {
            AcceptLegalScreen(
                disclaimer = CurrentLegalDocuments.disclaimer,
                onBack = {},
                onReadTerms = {},
                onAccept = {},
            )
        }
    }
}
