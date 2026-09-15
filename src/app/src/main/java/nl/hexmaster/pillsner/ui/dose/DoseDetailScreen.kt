package nl.hexmaster.pillsner.ui.dose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.intake.DoseAnswer
import nl.hexmaster.pillsner.domain.intake.DoseTiming
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.ui.home.DayLabel
import nl.hexmaster.pillsner.ui.home.EmptyState
import nl.hexmaster.pillsner.ui.home.FormattedDoseTime
import nl.hexmaster.pillsner.ui.home.IntakeStatusChip
import nl.hexmaster.pillsner.ui.medicines.rememberQuantityFormatter
import nl.hexmaster.pillsner.ui.theme.IntakeStatus
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import nl.hexmaster.pillsner.ui.theme.TabularNumbers

/** Stable tags for the dose detail screen's controls, for semantics tests. */
object DoseDetailTestTags {
    const val TITLE = "dose_detail_title"
    const val BACK = "dose_detail_back"
    const val TIME = "dose_detail_time"
    const val NAME = "dose_detail_name"
    const val TAKEN = "dose_detail_taken"
    const val SNOOZE = "dose_detail_snooze"
    const val SKIP = "dose_detail_skip"
    const val CLOSE = "dose_detail_close"
    const val OUTCOME = "dose_detail_outcome"
    const val GONE = "dose_detail_gone"
}

/**
 * One dose, with the three answers the notification offers (design D5, and design system 8.8).
 *
 * Reached by tapping a dose on the Welcome screen, and from nowhere else — the notification still
 * opens Home. The answers read in the same fixed order as the notification's, from the same
 * strings, so the gesture is the same wherever the user gives it.
 *
 * The screen records nothing by itself: every answer is an explicit tap, and Close sits in the
 * bottom third where a one-handed user reaches it, so a mis-tap on a tile costs one tap to undo.
 *
 * @param onAnswer called with the answer the user gave; the screen does not wait for it.
 * @param onClose leaves the screen; the back arrow, the Close button and system back all do this.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseDetailScreen(
    uiState: DoseDetailUiState,
    onAnswer: (DoseAnswer) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dose_detail_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag(DoseDetailTestTags.TITLE),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag(DoseDetailTestTags.BACK)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { contentPadding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isWide = maxWidth >= Spacing.contentMaxWidth
            val sidePadding = if (isWide) Spacing.screenEdgeWide else Spacing.screenEdge

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = Spacing.contentMaxWidth)
                    .padding(horizontal = sidePadding)
                    .padding(top = Spacing.xl, bottom = Spacing.lg),
            ) {
                // The content scrolls; Close does not, so it stays reachable without scrolling
                // past the answers at any font scale.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    when (uiState) {
                        DoseDetailUiState.Loading -> Unit

                        is DoseDetailUiState.Answerable -> {
                            DoseHeading(uiState.medicationName, uiState.amount, uiState.time, uiState.status)
                            DoseTimingBanner(uiState.timing)
                            Answers(onAnswer)
                        }

                        is DoseDetailUiState.Settled -> {
                            DoseHeading(uiState.medicationName, uiState.amount, uiState.time, uiState.status)
                            Text(
                                text = uiState.outcomeText(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.testTag(DoseDetailTestTags.OUTCOME),
                            )
                        }

                        DoseDetailUiState.Gone -> EmptyState(
                            icon = R.drawable.ic_do_not_disturb_on,
                            title = stringResource(R.string.dose_detail_gone_title),
                            hint = stringResource(R.string.dose_detail_gone_hint),
                            modifier = Modifier.testTag(DoseDetailTestTags.GONE),
                        )
                    }
                }

                TextButton(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.minTouchTarget)
                        .padding(top = Spacing.md)
                        .testTag(DoseDetailTestTags.CLOSE),
                ) {
                    Text(stringResource(R.string.dose_detail_close))
                }
            }
        }
    }
}

/** The time, the day when it is not today, the medicine, the amount and the state it is in. */
@Composable
private fun DoseHeading(
    medicationName: String,
    amount: Quantity,
    time: FormattedDoseTime,
    status: IntakeStatus,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = time.time,
            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = TabularNumbers),
            modifier = Modifier.testTag(DoseDetailTestTags.TIME),
        )
        time.day?.let { day ->
            Text(
                text = day.text(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = medicationName,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.testTag(DoseDetailTestTags.NAME),
        )
        Text(
            text = rememberQuantityFormatter().format(amount),
            style = MaterialTheme.typography.bodyLarge,
        )
        IntakeStatusChip(status)
    }
}

/**
 * The three answers, in the order they always appear, from the strings the notification uses.
 *
 * Never disabled: the timing banner above may say the dose is early or overdue, and that changes
 * what the screen says, not what it lets the user do.
 */
@Composable
private fun Answers(onAnswer: (DoseAnswer) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Button(
            onClick = { onAnswer(DoseAnswer.TAKEN) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.primaryActionHeight)
                .testTag(DoseDetailTestTags.TAKEN),
        ) {
            Text(stringResource(R.string.reminder_action_took_it))
        }
        FilledTonalButton(
            onClick = { onAnswer(DoseAnswer.SNOOZE) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.minTouchTarget)
                .testTag(DoseDetailTestTags.SNOOZE),
        ) {
            Text(stringResource(R.string.reminder_action_not_yet))
        }
        OutlinedButton(
            onClick = { onAnswer(DoseAnswer.SKIP) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.minTouchTarget)
                .testTag(DoseDetailTestTags.SKIP),
        ) {
            Text(stringResource(R.string.reminder_action_not_going_to))
        }
    }
}

/** What was recorded, and for a dose that was taken, when. */
@Composable
private fun DoseDetailUiState.Settled.outcomeText(): String = when (outcome) {
    IntakeOutcome.TAKEN -> stringResource(R.string.dose_detail_taken_at, recordedAt.asTimeOfDay())
    IntakeOutcome.SKIPPED -> stringResource(R.string.dose_detail_skipped)
    IntakeOutcome.MISSED -> stringResource(R.string.dose_detail_missed)
}

/**
 * The recorded moment as a time of day, in the reader's own locale and zone.
 *
 * The locale comes from the configuration rather than from `Locale.getDefault()`, so changing the
 * app's language recomposes this line instead of leaving it in the old one.
 */
@Composable
private fun Instant.asTimeOfDay(): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)
            .withZone(ZoneId.systemDefault())
    }
    return formatter.format(this)
}

/** Resolves a [DayLabel] to display text; only Tomorrow needs a string resource. */
@Composable
private fun DayLabel.text(): String = when (this) {
    DayLabel.Tomorrow -> stringResource(R.string.day_tomorrow)
    is DayLabel.Text -> value
}

// --- Previews ------------------------------------------------------------------------------

private val previewNow: Instant = Instant.parse("2026-09-14T08:00:00Z")
private val previewAmount = Quantity.of("40", DoseUnit.MILLIGRAM)

private fun previewAnswerable(timing: DoseTiming, status: IntakeStatus = IntakeStatus.Due) =
    DoseDetailUiState.Answerable(
        medicationName = "Ibuprofen",
        amount = previewAmount,
        time = FormattedDoseTime("08:00", null),
        status = status,
        timing = timing,
    )

private fun previewSettled(outcome: IntakeOutcome, status: IntakeStatus) = DoseDetailUiState.Settled(
    medicationName = "Ibuprofen",
    amount = previewAmount,
    time = FormattedDoseTime("08:00", DayLabel.Tomorrow),
    status = status,
    outcome = outcome,
    recordedAt = previewNow,
)

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun DoseDetailOnTimePreview() {
    PillsnerTheme {
        Surface { DoseDetailScreen(previewAnswerable(DoseTiming.ON_TIME), {}, {}) }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun DoseDetailEarlyPreview() {
    PillsnerTheme {
        Surface { DoseDetailScreen(previewAnswerable(DoseTiming.EARLY), {}, {}) }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun DoseDetailLatePreview() {
    PillsnerTheme {
        Surface {
            DoseDetailScreen(previewAnswerable(DoseTiming.LATE, IntakeStatus.Overdue), {}, {})
        }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun DoseDetailTakenPreview() {
    PillsnerTheme {
        Surface { DoseDetailScreen(previewSettled(IntakeOutcome.TAKEN, IntakeStatus.Taken), {}, {}) }
    }
}

@PreviewLightDark
@Composable
private fun DoseDetailSkippedPreview() {
    PillsnerTheme {
        Surface { DoseDetailScreen(previewSettled(IntakeOutcome.SKIPPED, IntakeStatus.Skipped), {}, {}) }
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun DoseDetailGonePreview() {
    PillsnerTheme {
        Surface { DoseDetailScreen(DoseDetailUiState.Gone, {}, {}) }
    }
}

