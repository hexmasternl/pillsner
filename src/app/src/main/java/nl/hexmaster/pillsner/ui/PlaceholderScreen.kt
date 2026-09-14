package nl.hexmaster.pillsner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

/**
 * A destination that has a title and nothing else yet (docs/design-system.md section 8.7):
 * the title in `displayLarge` with heading semantics, so navigation is observable and testable.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    titleTestTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = Spacing.screenEdge)
            .padding(top = Spacing.xxl),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier
                .semantics { heading() }
                .testTag(titleTestTag),
        )
    }
}

@PreviewLightDark
@Composable
private fun PlaceholderScreenPreview() {
    PillsnerTheme { PlaceholderScreen(title = "Medicines", titleTestTag = "preview_title") }
}
