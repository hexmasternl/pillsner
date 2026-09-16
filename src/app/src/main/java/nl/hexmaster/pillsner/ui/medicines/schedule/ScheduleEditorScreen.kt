package nl.hexmaster.pillsner.ui.medicines.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.time.LocalTime
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.domain.validation.ScheduleDraftError
import nl.hexmaster.pillsner.domain.validation.SchedulePattern
import nl.hexmaster.pillsner.ui.medicines.form.QuantityField
import nl.hexmaster.pillsner.ui.medicines.form.ScheduleDraft
import nl.hexmaster.pillsner.ui.medicines.form.ScheduleEditorUiState
import nl.hexmaster.pillsner.ui.medicines.rememberScheduleDescriptionFormatter
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing
import nl.hexmaster.pillsner.ui.theme.TabularNumbers
import nl.hexmaster.pillsner.ui.theme.tileContainerColor

/** Test tags for semantics tests of the schedule editor. */
object ScheduleEditorTestTags {
    const val TITLE = "schedule_editor_title"
    const val BACK = "schedule_editor_back"
    const val AMOUNT_PREFIX = "schedule_editor_amount_"
    const val PATTERN_PREFIX = "schedule_editor_pattern_"
    const val INTERVAL_LABEL = "schedule_editor_interval_label"
    const val INTERVAL_MINUS = "schedule_editor_interval_minus"
    const val INTERVAL_PLUS = "schedule_editor_interval_plus"
    const val HOUR_INTERVAL = "schedule_editor_hour_interval"
    const val FIRST_DOSE = "schedule_editor_first_dose"
    const val DAILY_TIMES = "schedule_editor_daily_times"
    const val DUPLICATE_TIME = "schedule_editor_duplicate_time"
    const val ALL_SEVEN_HINT = "schedule_editor_all_seven_hint"
    const val PREVIEW = "schedule_editor_preview"
    const val DONE = "schedule_editor_done"
}

/**
 * The schedule editor (design D6): how much, and one of three ways of saying when, with a live
 * preview of the line the overview will show.
 *
 * Done is the one positive action, pinned full width at the bottom like Save on the form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorScreen(
    uiState: ScheduleEditorUiState,
    onAmountTextChange: (String) -> Unit,
    onAmountUnitChange: (DoseUnit) -> Unit,
    onPatternChange: (SchedulePattern) -> Unit,
    onIntervalDaysChange: (Int) -> Unit,
    onIntervalHoursChange: (Int) -> Unit,
    onFirstDoseAtChange: (LocalTime) -> Unit,
    onDayToggled: (java.time.DayOfWeek) -> Unit,
    onTimeAdded: (LocalTime) -> Unit,
    onTimeRemoved: (LocalTime) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = rememberScheduleDescriptionFormatter()
    val timeFormatter = rememberTimeFormatter()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.isNew) R.string.schedule_editor_add_title else R.string.schedule_editor_edit_title,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag(ScheduleEditorTestTags.TITLE),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(ScheduleEditorTestTags.BACK)) {
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
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg)
                        .heightIn(min = Sizes.primaryActionHeight)
                        .imePadding()
                        .testTag(ScheduleEditorTestTags.DONE),
                ) {
                    Text(stringResource(R.string.action_done))
                }
            }
        },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = Spacing.contentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screenEdge, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                QuantityField(
                    label = stringResource(R.string.schedule_field_amount),
                    amount = uiState.amountText,
                    unit = uiState.amountUnit,
                    onAmountChange = onAmountTextChange,
                    onUnitChange = onAmountUnitChange,
                    testTagPrefix = ScheduleEditorTestTags.AMOUNT_PREFIX,
                    modifier = Modifier.fillMaxWidth(),
                )

                SingleChoiceSegmentedButtonRow(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                ) {
                    SchedulePattern.entries.forEachIndexed { index, pattern ->
                        SegmentedButton(
                            selected = uiState.pattern == pattern,
                            onClick = { onPatternChange(pattern) },
                            shape = SegmentedButtonDefaults.itemShape(index, SchedulePattern.entries.size),
                            modifier = Modifier
                                .fillMaxHeight()
                                .testTag(ScheduleEditorTestTags.PATTERN_PREFIX + pattern.name),
                        ) {
                            Text(
                                text = stringResource(pattern.labelRes()),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                when (uiState.pattern) {
                    SchedulePattern.EVERY_N_DAYS -> {
                        IntervalStepper(
                            intervalDays = uiState.intervalDays,
                            onIntervalDaysChange = onIntervalDaysChange,
                        )
                        TimesList(
                            times = uiState.times,
                            onTimeAdded = onTimeAdded,
                            onTimeRemoved = onTimeRemoved,
                        )
                        DuplicateTimeMessage(uiState.duplicateTimeRejected)
                    }

                    SchedulePattern.ON_WEEKDAYS -> {
                        WeekdayChips(selected = uiState.days, onDayToggled = onDayToggled)
                        if (uiState.showsAllSevenDaysHint) {
                            Text(
                                text = stringResource(R.string.schedule_all_seven_days_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag(ScheduleEditorTestTags.ALL_SEVEN_HINT),
                            )
                        }
                        TimesList(
                            times = uiState.times,
                            onTimeAdded = onTimeAdded,
                            onTimeRemoved = onTimeRemoved,
                        )
                        DuplicateTimeMessage(uiState.duplicateTimeRejected)
                    }

                    SchedulePattern.EVERY_N_HOURS -> {
                        HourIntervalPicker(
                            intervalHours = uiState.intervalHours,
                            onIntervalHoursChange = onIntervalHoursChange,
                        )
                        FirstDoseField(
                            firstDoseAt = uiState.firstDoseAt,
                            onFirstDoseAtChange = onFirstDoseAtChange,
                        )
                        Text(
                            text = stringResource(
                                R.string.schedule_daily_times,
                                uiState.dailyDoseTimes.joinToString(stringResource(R.string.list_separator)) {
                                    it.format(timeFormatter)
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = TabularNumbers),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag(ScheduleEditorTestTags.DAILY_TIMES),
                        )
                    }
                }

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = tileContainerColor()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ScheduleEditorTestTags.PREVIEW),
                ) {
                    val error = uiState.firstError
                    Row(
                        Modifier.padding(Spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (error != null) {
                            Icon(
                                painter = painterResource(R.drawable.ic_error_filled),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(Sizes.iconDefault),
                            )
                            Text(
                                text = stringResource(error.messageRes()),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            val summary = uiState.preview
                            val amount = uiState.previewAmount
                            Text(
                                text = if (summary != null && amount != null) {
                                    formatter.describe(summary, amount)
                                } else {
                                    stringResource(R.string.schedule_preview_placeholder)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Says so when a time the user picked is already in the list (spec: Duplicate time). */
@Composable
private fun DuplicateTimeMessage(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    Text(
        text = stringResource(R.string.schedule_duplicate_time),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier.testTag(ScheduleEditorTestTags.DUPLICATE_TIME),
    )
}

/** The every-N-days interval, as plus and minus buttons around a label that reads in words. */
@Composable
private fun IntervalStepper(
    intervalDays: Int,
    onIntervalDaysChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onIntervalDaysChange(intervalDays - 1) },
            enabled = intervalDays > 1,
            modifier = Modifier
                .sizeIn(minWidth = Sizes.minTouchTarget, minHeight = Sizes.minTouchTarget)
                .testTag(ScheduleEditorTestTags.INTERVAL_MINUS),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_remove),
                contentDescription = stringResource(R.string.schedule_interval_decrease),
            )
        }
        Text(
            text = when (intervalDays) {
                1 -> stringResource(R.string.schedule_interval_every_day)
                2 -> stringResource(R.string.schedule_interval_every_other_day)
                else -> stringResource(R.string.schedule_interval_every_n_days, intervalDays)
            },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .testTag(ScheduleEditorTestTags.INTERVAL_LABEL),
        )
        IconButton(
            onClick = { onIntervalDaysChange(intervalDays + 1) },
            enabled = intervalDays < Schedule.MAX_INTERVAL_DAYS,
            modifier = Modifier
                .sizeIn(minWidth = Sizes.minTouchTarget, minHeight = Sizes.minTouchTarget)
                .testTag(ScheduleEditorTestTags.INTERVAL_PLUS),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = stringResource(R.string.schedule_interval_increase),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourIntervalPicker(
    intervalHours: Int,
    onIntervalHoursChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = stringResource(R.string.schedule_hours_value, intervalHours),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.schedule_field_interval_hours)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag(ScheduleEditorTestTags.HOUR_INTERVAL),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ScheduleDraft.HOUR_INTERVALS.forEach { hours ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.schedule_hours_value, hours)) },
                    onClick = {
                        onIntervalHoursChange(hours)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FirstDoseField(
    firstDoseAt: LocalTime,
    onFirstDoseAtChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val timeFormatter = rememberTimeFormatter()

    OutlinedTextField(
        value = firstDoseAt.format(timeFormatter),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.schedule_field_first_dose)) },
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = modifier
            .fillMaxWidth()
            .testTag(ScheduleEditorTestTags.FIRST_DOSE),
    )
    androidx.compose.material3.TextButton(
        onClick = { pickerOpen = true },
        modifier = Modifier.testTag(ScheduleEditorTestTags.FIRST_DOSE + "_pick"),
    ) {
        Text(stringResource(R.string.schedule_pick_time))
    }

    if (pickerOpen) {
        TimePickerDialog(
            initial = firstDoseAt,
            onConfirm = {
                onFirstDoseAtChange(it)
                pickerOpen = false
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

private fun SchedulePattern.labelRes(): Int = when (this) {
    SchedulePattern.EVERY_N_DAYS -> R.string.schedule_pattern_every_n_days
    SchedulePattern.ON_WEEKDAYS -> R.string.schedule_pattern_weekdays
    SchedulePattern.EVERY_N_HOURS -> R.string.schedule_pattern_every_n_hours
}

private fun ScheduleDraftError.messageRes(): Int = when (this) {
    ScheduleDraftError.AMOUNT_REQUIRED -> R.string.medicine_error_dose_required
    ScheduleDraftError.AMOUNT_NOT_A_NUMBER -> R.string.medicine_error_dose_not_a_number
    ScheduleDraftError.AMOUNT_NOT_POSITIVE -> R.string.medicine_error_dose_not_positive
    ScheduleDraftError.TIMES_REQUIRED -> R.string.schedule_error_times_required
    ScheduleDraftError.DAYS_REQUIRED -> R.string.schedule_error_days_required
    ScheduleDraftError.ALL_SEVEN_DAYS -> R.string.schedule_error_all_seven_days
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun ScheduleEditorScreenPreview() {
    PillsnerTheme {
        ScheduleEditorScreen(
            uiState = ScheduleEditorUiState(
                amountText = "40",
                amountUnit = DoseUnit.MILLIGRAM,
                pattern = SchedulePattern.EVERY_N_HOURS,
                intervalHours = 12,
                firstDoseAt = LocalTime.of(8, 0),
                dailyDoseTimes = listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                preview = ScheduleSummary.EveryNHours(12),
                previewAmount = Quantity.of("40", DoseUnit.MILLIGRAM),
            ),
            onAmountTextChange = {},
            onAmountUnitChange = {},
            onPatternChange = {},
            onIntervalDaysChange = {},
            onIntervalHoursChange = {},
            onFirstDoseAtChange = {},
            onDayToggled = {},
            onTimeAdded = {},
            onTimeRemoved = {},
            onDone = {},
            onBack = {},
        )
    }
}
