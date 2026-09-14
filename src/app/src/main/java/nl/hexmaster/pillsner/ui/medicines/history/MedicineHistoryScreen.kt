package nl.hexmaster.pillsner.ui.medicines.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.UsageHistory
import nl.hexmaster.pillsner.domain.model.UsagePeriod
import nl.hexmaster.pillsner.ui.home.EmptyState
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import nl.hexmaster.pillsner.ui.theme.tileContainerColor

/** Stable tags for the usage history screen's content, for semantics tests. */
object MedicineHistoryTestTags {
    const val TITLE = "medicine_history_title"
    const val BACK = "medicine_history_back"
    const val MEDICINE_NAME = "medicine_history_medicine_name"
    const val LOADING = "medicine_history_loading"
    const val PERIOD_PREFIX = "medicine_history_period_"
    const val ADHERENCE = "medicine_history_adherence"
    const val SCHEDULED = "medicine_history_scheduled"
    const val TAKEN = "medicine_history_taken"
    const val CHART_HEADER = "medicine_history_chart_header"
    const val RECORDS_START = "medicine_history_records_start"
    const val EMPTY = "medicine_history_empty"
}

/**
 * One medicine's record over a chosen period (app-medicine-usage-history design D6).
 *
 * Read-only by construction: the only control besides back is the period selector, and nothing on
 * the screen records, changes or removes anything. The medicine's name sits in the content rather
 * than in the app bar, so a long name wraps instead of being truncated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineHistoryScreen(
    uiState: MedicineHistoryUiState,
    onPeriodSelected: (UsagePeriod) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.usage_history_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag(MedicineHistoryTestTags.TITLE),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(MedicineHistoryTestTags.BACK)) {
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
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(Modifier.testTag(MedicineHistoryTestTags.LOADING))
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = Spacing.contentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenEdge, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            ) {
                Text(
                    text = uiState.medicineName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .semantics { heading() }
                        .testTag(MedicineHistoryTestTags.MEDICINE_NAME),
                )

                PeriodSelector(selected = uiState.period, onPeriodSelected = onPeriodSelected)

                val history = uiState.history
                if (history == null || history.isEmpty) {
                    EmptyState(
                        icon = R.drawable.ic_history,
                        title = stringResource(R.string.usage_history_empty_title),
                        hint = stringResource(R.string.usage_history_empty_hint),
                        modifier = Modifier.testTag(MedicineHistoryTestTags.EMPTY),
                    )
                } else {
                    SummaryCard(history)
                    UsageBreakdown(history)
                    ChartCard(history)
                }
            }
        }
    }
}

/** The three periods as a single-choice row; the selection is the window everything is read over. */
@Composable
private fun PeriodSelector(
    selected: UsagePeriod,
    onPeriodSelected: (UsagePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        UsagePeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onPeriodSelected(period) },
                shape = SegmentedButtonDefaults.itemShape(index, UsagePeriod.entries.size),
                modifier = Modifier
                    .heightIn(min = Sizes.minTouchTarget)
                    .testTag(MedicineHistoryTestTags.PERIOD_PREFIX + period.name),
            ) {
                Text(
                    text = stringResource(period.labelRes()),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** The headline figure and the two counts it is made of (design D6 item 3). */
@Composable
private fun SummaryCard(history: UsageHistory, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val percentFormat = remember(locale) { NumberFormat.getPercentInstance(locale) }
    val adherence = history.adherencePercent

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tileContainerColor()),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            if (adherence != null) {
                Text(
                    text = percentFormat.format(adherence / 100.0),
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag(MedicineHistoryTestTags.ADHERENCE),
                )
                Text(
                    text = stringResource(R.string.usage_history_adherence_caption),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                CountColumn(
                    value = history.scheduled,
                    label = stringResource(R.string.usage_history_scheduled_label),
                    testTag = MedicineHistoryTestTags.SCHEDULED,
                )
                CountColumn(
                    value = history.taken,
                    label = stringResource(R.string.usage_history_taken_label),
                    testTag = MedicineHistoryTestTags.TAKEN,
                )
            }
        }
    }
}

/** One figure with its label beneath it, read as one thing. */
@Composable
private fun CountColumn(value: Int, label: String, testTag: String, modifier: Modifier = Modifier) {
    Column(
        modifier.testTag(testTag),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The chart, its header naming the bucketing, and the records-start note when there is one. */
@Composable
private fun ChartCard(history: UsageHistory, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormat = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tileContainerColor()),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(
                    if (history.period.bucketsByWeek) {
                        R.string.usage_history_by_week
                    } else {
                        R.string.usage_history_by_day
                    },
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .semantics { heading() }
                    .testTag(MedicineHistoryTestTags.CHART_HEADER),
            )

            UsageChart(history)

            history.recordsStartOn?.let { start ->
                Text(
                    text = stringResource(R.string.usage_history_records_start, start.format(dateFormat)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(MedicineHistoryTestTags.RECORDS_START),
                )
            }
        }
    }
}

/** The user-facing label of a period. */
fun UsagePeriod.labelRes(): Int = when (this) {
    UsagePeriod.WEEK -> R.string.usage_period_week
    UsagePeriod.MONTH -> R.string.usage_period_month
    UsagePeriod.THREE_MONTHS -> R.string.usage_period_three_months
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun MedicineHistoryFullWeekPreview() {
    PillsnerTheme {
        MedicineHistoryScreen(
            uiState = MedicineHistoryUiState(
                medicineName = "Metoprolol",
                period = UsagePeriod.WEEK,
                history = UsageHistoryPreviewData.fullWeek(),
                isLoading = false,
            ),
            onPeriodSelected = {},
            onBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MedicineHistorySparseThreeMonthsPreview() {
    PillsnerTheme {
        MedicineHistoryScreen(
            uiState = MedicineHistoryUiState(
                medicineName = "Colecalciferol",
                period = UsagePeriod.THREE_MONTHS,
                history = UsageHistoryPreviewData.sparseThreeMonths(),
                isLoading = false,
            ),
            onPeriodSelected = {},
            onBack = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun MedicineHistoryEmptyPreview() {
    PillsnerTheme {
        MedicineHistoryScreen(
            uiState = MedicineHistoryUiState(
                medicineName = "Paracetamol",
                period = UsagePeriod.WEEK,
                history = UsageHistoryPreviewData.empty(),
                isLoading = false,
            ),
            onPeriodSelected = {},
            onBack = {},
        )
    }
}
