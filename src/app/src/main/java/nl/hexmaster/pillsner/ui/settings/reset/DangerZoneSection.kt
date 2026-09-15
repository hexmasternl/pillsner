package nl.hexmaster.pillsner.ui.settings.reset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Stable tags for the danger zone, for semantics tests. */
object DangerZoneTestTags {
    const val HEADING = "danger_zone_heading"
    const val BUTTON = "danger_zone_reset"
}

/**
 * The last section of Settings, and the only way to empty the app (design D6).
 *
 * Red is one of the few places `docs/design-system.md` section 2.4 allows it — a destructive
 * action — and it is never the only signal: a divider marks the boundary, the heading names it,
 * and a line of explanation says what the button does before the button is offered.
 *
 * The section is last because a destructive action should take a deliberate scroll to reach, and
 * because "here is your app, and here is how to empty it" is the honest reading order. The button
 * performs nothing; it opens the confirmation, which is the whole point of the section.
 */
@Composable
fun DangerZoneSection(
    onResetTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        HorizontalDivider(Modifier.padding(bottom = Spacing.md))

        Text(
            text = stringResource(R.string.reset_header),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .semantics { heading() }
                .testTag(DangerZoneTestTags.HEADING),
        )

        Text(
            text = stringResource(R.string.reset_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = onResetTapped,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.primaryActionHeight)
                .padding(top = Spacing.sm)
                .testTag(DangerZoneTestTags.BUTTON),
        ) {
            Text(stringResource(R.string.reset_action))
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun DangerZoneSectionPreview() {
    PillsnerTheme {
        Surface {
            DangerZoneSection(onResetTapped = {}, modifier = Modifier.padding(Spacing.lg))
        }
    }
}
