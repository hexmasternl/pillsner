package nl.hexmaster.pillsner.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.UpcomingDose
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tags for semantics tests of the welcome screen. */
object WelcomeScreenTestTags {
    const val HEADER = "welcome_header"
    const val DOSE_LIST = "welcome_dose_list"
    const val DOSE_TILE = "welcome_dose_tile"
    const val EMPTY_STATE = "welcome_empty_state"
}

/**
 * The start screen: header, the reminder banner when reminders cannot be delivered, then up to five
 * upcoming dose tiles or the empty state (design D7, D8, and app-medicine-alarm D11).
 *
 * The whole screen is one `LazyColumn` so header, banner and tiles scroll together at large font
 * scales. Side padding is `Spacing.screenEdge` on compact widths and `Spacing.screenEdgeWide` from
 * medium width up, and the column is capped at `Spacing.contentMaxWidth` (design system section 5).
 */
@Composable
fun WelcomeScreen(
    uiState: HomeUiState,
    modifier: Modifier = Modifier,
    timeFormatter: UpcomingDoseTimeFormatter = remember { UpcomingDoseTimeFormatter() },
    onOpenReminderSettings: () -> Unit = {},
    onOpenDose: (DoseId) -> Unit = {},
) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.TopCenter,
    ) {
        val isWide = maxWidth >= Spacing.contentMaxWidth
        val sidePadding = if (isWide) Spacing.screenEdgeWide else Spacing.screenEdge

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = Spacing.contentMaxWidth)
                .testTag(WelcomeScreenTestTags.DOSE_LIST),
            contentPadding = PaddingValues(start = sidePadding, end = sidePadding, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item(key = "header") { WelcomeHeader() }

            uiState.reminderProblem?.let { problem ->
                item(key = "banner") {
                    ReminderBanner(
                        message = stringResource(problem.message),
                        actionLabel = stringResource(problem.actionLabel),
                        onOpenSettings = onOpenReminderSettings,
                    )
                }
            }

            when {
                uiState.isLoading -> Unit
                uiState.upcomingDoses.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        icon = R.drawable.ic_medication,
                        title = stringResource(R.string.home_empty_title),
                        hint = stringResource(R.string.home_empty_hint),
                        modifier = Modifier.testTag(WelcomeScreenTestTags.EMPTY_STATE),
                    )
                }
                else -> items(uiState.upcomingDoses, key = { it.doseId.value }) { dose ->
                    DoseTile(
                        dose = dose,
                        time = timeFormatter.format(dose.scheduledAt),
                        status = dose.status(uiState.now),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(WelcomeScreenTestTags.DOSE_TILE),
                        onClick = { onOpenDose(dose.doseId) },
                    )
                }
            }
        }
    }
}

/** What the banner says about each problem, and what its button offers to do about it. */
private val ReminderProblem.message: Int
    get() = when (this) {
        ReminderProblem.NOTIFICATIONS_DENIED -> R.string.reminder_banner_notifications_denied
        ReminderProblem.SILENTLY_MISSED_REMINDER -> R.string.reminder_banner_reminder_missed
        ReminderProblem.INEXACT_ALARMS -> R.string.reminder_banner_inexact_alarms
    }

private val ReminderProblem.actionLabel: Int
    get() = when (this) {
        ReminderProblem.SILENTLY_MISSED_REMINDER -> R.string.reminder_banner_check_background
        else -> R.string.reminder_banner_open_settings
    }

// Preview data: invented names, fixed clock so the day labels are stable.
private val previewNow: Instant = Instant.parse("2026-09-11T10:00:00Z")
private val previewFormatter = UpcomingDoseTimeFormatter(
    zoneId = ZoneOffset.UTC,
    locale = Locale.UK,
    clock = Clock.fixed(previewNow, ZoneOffset.UTC),
)
private val previewDoses = listOf(
    UpcomingDose(DoseId(1), "Ibuprofen", Quantity.of("1", DoseUnit.TABLET), previewNow.plus(Duration.ofHours(2))),
    UpcomingDose(DoseId(2), "Vitamin D", Quantity.of("2", DoseUnit.DROP), previewNow.plus(Duration.ofHours(8))),
    UpcomingDose(
        DoseId(3),
        "Amoxicillin",
        Quantity.of("500", DoseUnit.MILLIGRAM),
        previewNow.plus(Duration.ofHours(22)),
    ),
    UpcomingDose(DoseId(4), "Ibuprofen", Quantity.of("1", DoseUnit.TABLET), previewNow.plus(Duration.ofHours(26))),
    UpcomingDose(
        DoseId(5),
        "Metformin",
        Quantity.of("850", DoseUnit.MILLIGRAM),
        previewNow.plus(Duration.ofHours(50)),
    ),
)

@PreviewLightDark
@Composable
private fun WelcomeScreenEmptyPreview() {
    PillsnerTheme {
        Surface {
            WelcomeScreen(HomeUiState(isLoading = false, now = previewNow), timeFormatter = previewFormatter)
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun WelcomeScreenFiveTilesPreview() {
    PillsnerTheme {
        Surface {
            WelcomeScreen(
                HomeUiState(previewDoses, isLoading = false, now = previewNow),
                timeFormatter = previewFormatter,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WelcomeScreenWithBannerPreview() {
    PillsnerTheme {
        Surface {
            WelcomeScreen(
                HomeUiState(
                    previewDoses.take(2),
                    isLoading = false,
                    notificationsAllowed = false,
                    now = previewNow,
                ),
                timeFormatter = previewFormatter,
            )
        }
    }
}
