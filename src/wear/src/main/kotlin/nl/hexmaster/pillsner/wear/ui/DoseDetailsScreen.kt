package nl.hexmaster.pillsner.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import java.time.Instant
import java.util.Locale
import nl.hexmaster.pillsner.wear.R
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme
import nl.hexmaster.pillsner.wear.ui.theme.WearDimens

/** Stable tags for the watch's dose details screen, for semantics tests. */
object DoseDetailsTestTags {
    const val TITLE = "wear_details_title"
    const val AMOUNT = "wear_details_amount"
    const val DUE = "wear_details_due"
    const val DEFAULT_DOSE = "wear_details_default_dose"
    const val SCHEDULE = "wear_details_schedule"
    const val STOCK = "wear_details_stock"
    const val UNAVAILABLE = "wear_details_unavailable"
}

/**
 * Everything the watch knows about one dose and the medicine behind it
 * (`wear-day-overview` design D5 and D6).
 *
 * Read-only, without exception: there is no field, button or gesture here that changes a medicine,
 * a schedule, a stock level or the dose itself. Every line was written out by the phone, so it
 * reads in the phone app's language exactly as it does there.
 */
@Composable
fun DoseDetailsScreen(entry: WatchDoseEntry, locale: Locale, modifier: Modifier = Modifier) {
    LocalizedContent(locale) {
        val columnState = rememberTransformingLazyColumnState()

        AppScaffold(modifier = modifier) {
            ScreenScaffold(scrollState = columnState) { contentPadding ->
                TransformingLazyColumn(
                    state = columnState,
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(WearDimens.betweenCards),
                ) {
                    item {
                        ListHeader(modifier = Modifier.testTag(DoseDetailsTestTags.TITLE)) {
                            Text(entry.name, textAlign = TextAlign.Center)
                        }
                    }
                    item {
                        DetailRow(
                            label = stringResource(R.string.details_this_dose),
                            value = entry.amountText,
                            tag = DoseDetailsTestTags.AMOUNT,
                        )
                    }
                    item {
                        DetailRow(
                            label = stringResource(R.string.details_due),
                            value = entry.timeText(),
                            tag = DoseDetailsTestTags.DUE,
                        )
                    }
                    entry.defaultDoseText?.let { defaultDose ->
                        item {
                            DetailRow(
                                label = stringResource(R.string.details_default_dose),
                                value = defaultDose,
                                tag = DoseDetailsTestTags.DEFAULT_DOSE,
                            )
                        }
                    }
                    if (entry.scheduleLines.isNotEmpty()) {
                        item {
                            DetailRow(
                                label = stringResource(R.string.details_schedule),
                                value = entry.scheduleLines.joinToString("\n"),
                                tag = DoseDetailsTestTags.SCHEDULE,
                            )
                        }
                    }
                    entry.stockText?.let { stock ->
                        item {
                            DetailRow(
                                label = stringResource(R.string.details_stock),
                                value = stock,
                                tag = DoseDetailsTestTags.STOCK,
                            )
                        }
                    }
                    if (!entry.hasDetails) {
                        item {
                            Text(
                                text = stringResource(R.string.details_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = WearDimens.screenEdge)
                                    .testTag(DoseDetailsTestTags.UNAVAILABLE),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A label and what it says, read as one item: "Schedule, 400 mg twice a day". */
@Composable
private fun DetailRow(label: String, value: String, tag: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WearDimens.cardPaddingHorizontal)
            .semantics(mergeDescendants = true) {}
            .testTag(tag),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val previewEntry = WatchDoseEntry(
    doseId = 1,
    name = "Ibuprofen",
    amountText = "400 mg",
    scheduledAt = Instant.parse("2026-09-13T08:00:00Z"),
    isOverdue = false,
    isTomorrow = false,
    defaultDoseText = "400 mg",
    scheduleLines = listOf("400 mg twice a day", "200 mg as needed"),
    stockText = "24 tablets",
)

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
private fun DoseDetailsPreview() {
    PillsnerWearTheme { DoseDetailsScreen(previewEntry, Locale.ENGLISH) }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun DoseDetailsWithoutDetailsPreview() {
    PillsnerWearTheme {
        DoseDetailsScreen(
            previewEntry.copy(defaultDoseText = null, scheduleLines = emptyList(), stockText = null),
            Locale.ENGLISH,
        )
    }
}
