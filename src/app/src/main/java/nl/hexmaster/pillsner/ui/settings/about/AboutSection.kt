package nl.hexmaster.pillsner.ui.settings.about

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
import nl.hexmaster.pillsner.domain.model.AppInfo
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes

/** Stable tags for the About section's row, for semantics tests. */
object AboutSectionTestTags {
    const val ABOUT_ROW = "about_row"
}

/**
 * The About section of the Settings screen (app-about-screen design D4): one row that says which
 * version is installed and opens the About screen.
 *
 * The version is in the supporting text rather than only behind the row, so someone filing a bug
 * report reads it without opening anything. The row takes the same shape as "Change PIN" above it,
 * so Settings still reads as one list.
 *
 * @param appInfo what the build says about itself; only the version is shown here.
 * @param onAboutTapped opens the About screen.
 */
@Composable
fun AboutSection(
    appInfo: AppInfo,
    onAboutTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.about_header),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )

        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.about_row_title),
                    style = MaterialTheme.typography.titleSmall,
                )
            },
            supportingContent = {
                Text(
                    stringResource(
                        R.string.about_version_summary,
                        appInfo.versionName,
                        appInfo.versionCode,
                    ),
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
                .clickable(role = Role.Button, onClick = onAboutTapped)
                .testTag(AboutSectionTestTags.ABOUT_ROW),
        )
    }
}

@PreviewLightDark
@Composable
private fun AboutSectionPreview() {
    PillsnerTheme {
        AboutSection(appInfo = PreviewAppInfo, onAboutTapped = {})
    }
}

@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun AboutSectionLargeFontPreview() {
    PillsnerTheme {
        AboutSection(appInfo = PreviewAppInfo, onAboutTapped = {})
    }
}
