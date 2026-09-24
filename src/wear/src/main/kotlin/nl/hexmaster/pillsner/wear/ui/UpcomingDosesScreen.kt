package nl.hexmaster.pillsner.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import nl.hexmaster.pillsner.wear.domain.AgendaDay
import nl.hexmaster.pillsner.wear.ui.theme.PillsnerWearTheme
import nl.hexmaster.pillsner.wear.ui.theme.WearDimens

/** Stable tags for the watch's agenda screen, for semantics tests. */
object UpcomingDosesTestTags {
    const val DAY_HEADER = "wear_day_header"
    const val TIME_HEADER = "wear_time_header"
    const val EMPTY = "wear_empty"
    const val FOOTER = "wear_footer"
    const val LIST = "wear_list"
}

/**
 * The watch app: what is still to be taken today and tomorrow, grouped by the time it is due
 * (`wear-day-overview` design D1). The list scrolls, by touch and by rotary.
 *
 * Read-only by design: a tap opens the details of the medicine behind a dose and nothing else. The
 * footer is the honest part — a list the phone can no longer correct is still shown, because it is
 * the best answer available, but it says the phone is out of reach so nobody mistakes it for live.
 */
@Composable
fun UpcomingDosesScreen(
    uiState: WatchUiState,
    onDoseClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LocalizedContent(uiState.locale) {
        val columnState = rememberTransformingLazyColumnState()
        val rows = remember(uiState.sections) { uiState.sections.toRows() }

        AppScaffold(modifier = modifier) {
            ScreenScaffold(scrollState = columnState) { contentPadding ->
                TransformingLazyColumn(
                    state = columnState,
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(WearDimens.betweenCards),
                    modifier = Modifier.testTag(UpcomingDosesTestTags.LIST),
                ) {
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
                        items(rows.size, key = { rows[it].key }) { index ->
                            when (val row = rows[index]) {
                                is AgendaRow.Day -> DayHeader(row.day)
                                is AgendaRow.Time -> TimeHeader(row.scheduledAt)
                                is AgendaRow.Dose ->
                                    DoseCard(row.entry, onClick = { onDoseClick(row.entry.doseId) })
                            }
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

@Composable
private fun DayHeader(day: AgendaDay) {
    ListHeader(modifier = Modifier.testTag(UpcomingDosesTestTags.DAY_HEADER)) {
        Text(
            stringResource(
                when (day) {
                    AgendaDay.TODAY -> R.string.agenda_today
                    AgendaDay.TOMORROW -> R.string.agenda_tomorrow
                },
            ),
        )
    }
}

@Composable
private fun TimeHeader(scheduledAt: Instant) {
    Text(
        text = formatTime(scheduledAt, rememberTimeFormatter()),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WearDimens.cardPaddingHorizontal)
            .testTag(UpcomingDosesTestTags.TIME_HEADER),
    )
}

/**
 * The agenda flattened for the lazy column: a day heading, then a time heading, then the doses due
 * at that time, and so on. Each row carries its own key so the column can keep its items across a
 * new payload.
 */
private sealed interface AgendaRow {
    val key: String

    data class Day(val day: AgendaDay) : AgendaRow {
        override val key: String get() = "day-${day.name}"
    }

    data class Time(val day: AgendaDay, val scheduledAt: Instant) : AgendaRow {
        override val key: String get() = "time-${day.name}-${scheduledAt.toEpochMilli()}"
    }

    data class Dose(val entry: WatchDoseEntry) : AgendaRow {
        override val key: String get() = "dose-${entry.doseId}"
    }
}

private fun List<WatchDaySection>.toRows(): List<AgendaRow> = flatMap { section ->
    buildList {
        add(AgendaRow.Day(section.day))
        section.groups.forEach { group ->
            add(AgendaRow.Time(section.day, group.scheduledAt))
            group.doses.forEach { add(AgendaRow.Dose(it)) }
        }
    }
}

private fun previewState(
    sections: List<WatchDaySection>,
    phoneConnected: Boolean = true,
    hasData: Boolean = true,
) = WatchUiState(sections = sections, phoneConnected = phoneConnected, hasData = hasData, locale = Locale.ENGLISH)

private fun previewEntry(
    id: Long,
    name: String,
    amount: String,
    at: String,
    isOverdue: Boolean = false,
    isTomorrow: Boolean = false,
) = WatchDoseEntry(
    doseId = id,
    name = name,
    amountText = amount,
    scheduledAt = Instant.parse(at),
    isOverdue = isOverdue,
    isTomorrow = isTomorrow,
    defaultDoseText = amount,
    scheduleLines = listOf("$amount twice a day"),
    stockText = "24 tablets",
)

private val previewSections = listOf(
    WatchDaySection(
        day = AgendaDay.TODAY,
        groups = listOf(
            WatchTimeGroup(
                scheduledAt = Instant.parse("2026-09-13T08:00:00Z"),
                doses = listOf(
                    previewEntry(1, "Ibuprofen", "400 mg", "2026-09-13T08:00:00Z", isOverdue = true),
                    previewEntry(2, "Metformin", "500 mg", "2026-09-13T08:00:00Z", isOverdue = true),
                ),
            ),
            WatchTimeGroup(
                scheduledAt = Instant.parse("2026-09-13T20:00:00Z"),
                doses = listOf(previewEntry(3, "Paracetamol", "2 tablets", "2026-09-13T20:00:00Z")),
            ),
        ),
    ),
    WatchDaySection(
        day = AgendaDay.TOMORROW,
        groups = listOf(
            WatchTimeGroup(
                scheduledAt = Instant.parse("2026-09-14T08:00:00Z"),
                doses = listOf(
                    previewEntry(4, "Ibuprofen", "400 mg", "2026-09-14T08:00:00Z", isTomorrow = true),
                ),
            ),
        ),
    ),
)

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
private fun UpcomingDosesPreview() {
    PillsnerWearTheme { UpcomingDosesScreen(previewState(previewSections)) }
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
        UpcomingDosesScreen(previewState(previewSections, phoneConnected = false))
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun UpcomingDosesNeverSyncedPreview() {
    PillsnerWearTheme {
        UpcomingDosesScreen(previewState(emptyList(), phoneConnected = false, hasData = false))
    }
}
