package nl.hexmaster.pillsner.ui.dose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.intake.DoseTiming
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tag for the timing warning, so a semantics test can say it is or is not there. */
object DoseTimingBannerTestTags {
    const val BANNER = "dose_timing_banner"
}

/**
 * Says so when a dose is being answered well before or well after its moment (design D7).
 *
 * The attention banner's shape (docs/design-system.md section 8.9) without its action button and,
 * for early, without its colours: section 2.4 reserves red for danger, and being early is
 * information, not a problem, so it takes the same blue 2.3 gives a dose that is simply due. Late
 * reuses the `errorContainer` pair 2.3 assigns to Overdue, so this screen agrees with the tile the
 * user tapped to reach it.
 *
 * Both sentences end by saying the dose can still be recorded, because that is the whole point:
 * the warning takes nothing away, and no button is ever disabled by the clock.
 *
 * Renders nothing for [DoseTiming.ON_TIME].
 */
@Composable
fun DoseTimingBanner(
    timing: DoseTiming,
    modifier: Modifier = Modifier,
) {
    val early = timing == DoseTiming.EARLY
    if (timing == DoseTiming.ON_TIME) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DoseTimingBannerTestTags.BANNER),
        shape = MaterialTheme.shapes.large,
        color = if (early) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        contentColor = if (early) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
    ) {
        Row(
            Modifier.padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                painter = painterResource(if (early) R.drawable.ic_info else R.drawable.ic_schedule),
                // Decorative: the sentence beside it says everything the icon does.
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconDefault),
            )
            Text(
                stringResource(if (early) R.string.dose_detail_early else R.string.dose_detail_late),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun DoseTimingBannerPreview() {
    PillsnerTheme {
        Surface {
            Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                DoseTimingBanner(DoseTiming.EARLY)
                DoseTimingBanner(DoseTiming.LATE)
            }
        }
    }
}
