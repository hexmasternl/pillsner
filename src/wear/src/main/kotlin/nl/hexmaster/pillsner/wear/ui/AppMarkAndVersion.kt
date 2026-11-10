package nl.hexmaster.pillsner.wear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import nl.hexmaster.pillsner.wear.R
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme
import nl.hexmaster.pillsner.wear.ui.theme.WearDimens
import nl.hexmaster.pillsner.wear.ui.theme.roundTextInset

/**
 * What every agenda ends with: the product mark and, under it, the installed version, both centred
 * (`fix-watch-agenda-bottom-inset` design D1 to D3).
 *
 * Stacked rather than side by side, because the end of a list is where a round screen is narrowest.
 * It is not an action and offers none. The mark is decorative (design system section 7): the line
 * under it carries the meaning, so a screen reader hears the version once and nothing for the mark.
 */
@Composable
internal fun AppMarkAndVersion(appVersion: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = roundTextInset()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WearDimens.betweenMarkAndVersion),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_pillsner_logo),
            contentDescription = null,
            modifier = Modifier
                .size(WearDimens.markSize)
                .testTag(UpcomingDosesTestTags.MARK),
        )
        Text(
            text = stringResource(R.string.app_version, appVersion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(UpcomingDosesTestTags.VERSION),
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, fontScale = 1.24f)
@Composable
private fun AppMarkAndVersionPreview() {
    PillsnerWearTheme { AppMarkAndVersion(appVersion = "1.2.3") }
}
