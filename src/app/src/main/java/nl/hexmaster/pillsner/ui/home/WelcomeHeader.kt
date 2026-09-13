package nl.hexmaster.pillsner.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/**
 * Logo and app title at the top of the welcome screen (docs/design-system.md sections 3.3, 8.7):
 * Home has no app bar, the title is the screen's single `displayLarge` and it is centred. The mark
 * is a painter parameter so the final artwork replaces the drawable without touching this code.
 */
@Composable
fun WelcomeHeader(
    modifier: Modifier = Modifier,
    logo: Painter = painterResource(R.drawable.ic_pillsner_logo),
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = Spacing.xxl, bottom = Spacing.xl)
            .testTag(WelcomeScreenTestTags.HEADER),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = logo,
            contentDescription = stringResource(R.string.logo_content_description),
            modifier = Modifier.size(Sizes.logoHeader),
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = stringResource(R.string.app_title),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@PreviewLightDark
@Composable
private fun WelcomeHeaderPreview() {
    PillsnerTheme {
        Surface { WelcomeHeader() }
    }
}
