package nl.hexmaster.pillsner.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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

/** Stable tags for the watch screen, for semantics tests. */
object UpcomingDosesTestTags {
    const val HEADER = "wear_header"
    const val EMPTY = "wear_empty"
    const val FOOTER = "wear_footer"
    const val LIST = "wear_list"
}

/**
 * The watch app: what is due in the next six hours, and nothing else (design D6).
 *
 * Read-only by design. The footer is the honest part — a list the phone can no longer correct is
 * still shown, because it is the best answer available, but it says the phone is out of reach so
 * nobody mistakes it for live.
 */
@Composable
fun UpcomingDosesScreen(uiState: WatchUiState, modifier: Modifier = Modifier) {
    LocalizedContent(uiState.locale) {
        val columnState = rememberTransformingLazyColumnState()

        AppScaffold(modifier = modifier) {
            ScreenScaffold(scrollState = columnState) { contentPadding ->
                TransformingLazyColumn(
                    state = columnState,
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(WearDimens.betweenCards),
                    modifier = Modifier.testTag(UpcomingDosesTestTags.LIST),
                ) {
                    item {
                        ListHeader(modifier = Modifier.testTag(UpcomingDosesTestTags.HEADER)) {
                            Text(stringResource(R.string.upcoming_header))
                        }
                    }

                    if (uiState.isEmpty) {
                        item {
                            Text(
                                text = stringResource(R.string.upcoming_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = WearDimens.screenEdge)
                                    .testTag(UpcomingDosesTestTags.EMPTY),
                            )
                        }
                    } else {
                        items(uiState.entries.size, key = { uiState.entries[it].doseId }) { index ->
                            DoseCard(uiState.entries[index])
                        }
                    }

                    if (!uiState.phoneConnected) {
                        item {
                            Text(
                                text = stringResource(
                                    if (uiState.hasData) R.string.phone_not_connected else R.string.never_synced,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = WearDimens.screenEdge, vertical = WearDimens.screenEdge)
                                    .testTag(UpcomingDosesTestTags.FOOTER),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun previewState(
    entries: List<WatchDoseEntry>,
    phoneConnected: Boolean = true,
    hasData: Boolean = true,
) = WatchUiState(entries = entries, phoneConnected = phoneConnected, hasData = hasData, locale = Locale.ENGLISH)

private val previewEntries = listOf(
    WatchDoseEntry(1, "Ibuprofen", "400 mg", Instant.parse("2026-09-13T08:00:00Z"), isOverdue = true, isTomorrow = false),
    WatchDoseEntry(2, "Paracetamol", "2 tablets", Instant.parse("2026-09-13T12:00:00Z"), isOverdue = false, isTomorrow = false),
    WatchDoseEntry(3, "Metformin", "500 mg", Instant.parse("2026-09-14T01:00:00Z"), isOverdue = false, isTomorrow = true),
)

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
private fun UpcomingDosesPreview() {
    PillsnerWearTheme { UpcomingDosesScreen(previewState(previewEntries)) }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun UpcomingDosesEmptyPreview() {
    PillsnerWearTheme { UpcomingDosesScreen(previewState(emptyList())) }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
private fun UpcomingDosesDisconnectedPreview() {
    PillsnerWearTheme {
        UpcomingDosesScreen(previewState(previewEntries, phoneConnected = false))
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun UpcomingDosesNeverSyncedPreview() {
    PillsnerWearTheme {
        UpcomingDosesScreen(previewState(emptyList(), phoneConnected = false, hasData = false))
    }
}
