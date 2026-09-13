package nl.hexmaster.pillsner.ui.medicines.form

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.time.LocalDate
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.domain.validation.MedicationFieldError
import nl.hexmaster.pillsner.ui.medicines.rememberScheduleDescriptionFormatter
import nl.hexmaster.pillsner.ui.theme.PillsnerTheme
import nl.hexmaster.pillsner.ui.theme.Sizes
import nl.hexmaster.pillsner.ui.theme.Spacing

/** Test tags for semantics tests of the Add medicine form. */
object MedicationFormTestTags {
    const val TITLE = "add_medication_title"
    const val BACK = "add_medication_back"
    const val NAME = "add_medication_name"
    const val DOSE_PREFIX = "add_medication_dose_"
    const val USED_SINCE = "add_medication_used_since"
    const val USE_UNTIL = "add_medication_use_until"
    const val PRESCRIBER = "add_medication_prescriber"
    const val SCHEDULES_HEADER = "add_medication_schedules_header"
    const val ADD_SCHEDULE = "add_medication_add_schedule"
    const val NO_SCHEDULES = "add_medication_no_schedules"
    const val SAVE = "add_medication_save"
    const val LOADING = "medication_form_loading"
    const val ACTIVE_SWITCH = "medication_form_active_switch"
}

/**
 * The Add medicine form (design D5): name, default dose, the two dates, the prescriber and the
 * schedules, with one full-width Save pinned above the keyboard.
 *
 * Save is a filled button at the bottom rather than an action in the app bar because it is the
 * screen's single positive action and has to be reachable one-handed (design system 8.4, 8.11).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationFormScreen(
    uiState: MedicationFormUiState,
    onNameChange: (String) -> Unit,
    onDoseTextChange: (String) -> Unit,
    onDoseUnitChange: (DoseUnit) -> Unit,
    onUsedSinceChange: (LocalDate) -> Unit,
    onUseUntilChange: (LocalDate?) -> Unit,
    onPrescriberChange: (Prescriber) -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onAddSchedule: () -> Unit,
    onEditSchedule: (Int) -> Unit,
    onRemoveSchedule: (Int) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val formatter = rememberScheduleDescriptionFormatter()
    var prescriberMenuExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = true, onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (uiState.showsActiveSwitch) {
                                R.string.medication_form_title_details
                            } else {
                                R.string.medication_form_title_add
                            },
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag(MedicationFormTestTags.TITLE),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag(MedicationFormTestTags.BACK)) {
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
                    onClick = onSave,
                    // Always tappable: the spec asks that tapping Save on an invalid form is what
                    // reveals the errors. A disabled button would leave the user with no feedback.
                    enabled = !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg)
                        .heightIn(min = Sizes.primaryActionHeight)
                        .imePadding()
                        .testTag(MedicationFormTestTags.SAVE),
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(Modifier.testTag(MedicationFormTestTags.LOADING))
            }
            return@Scaffold
        }

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
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.medicine_field_name)) },
                    isError = uiState.showErrors && uiState.nameError != null,
                    supportingText = uiState.nameError.supportingText(uiState.showErrors),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(MedicationFormTestTags.NAME),
                )

                QuantityField(
                    label = stringResource(R.string.medicine_field_default_dose),
                    amount = uiState.doseText,
                    unit = uiState.doseUnit,
                    onAmountChange = onDoseTextChange,
                    onUnitChange = onDoseUnitChange,
                    errorMessage = uiState.doseError.messageOrNull(uiState.showErrors),
                    testTagPrefix = MedicationFormTestTags.DOSE_PREFIX,
                    modifier = Modifier.fillMaxWidth(),
                )

                DateField(
                    label = stringResource(R.string.medicine_field_used_since),
                    date = uiState.usedSince,
                    onDateChange = onUsedSinceChange,
                    testTag = MedicationFormTestTags.USED_SINCE,
                    modifier = Modifier.fillMaxWidth(),
                )

                DateField(
                    label = stringResource(R.string.medicine_field_use_until),
                    date = uiState.useUntil,
                    onDateChange = onUseUntilChange,
                    minDate = uiState.usedSince,
                    onClear = { onUseUntilChange(null) },
                    errorMessage = uiState.useUntilError.messageOrNull(uiState.showErrors),
                    testTag = MedicationFormTestTags.USE_UNTIL,
                    modifier = Modifier.fillMaxWidth(),
                )

                ExposedDropdownMenuBox(
                    expanded = prescriberMenuExpanded,
                    onExpandedChange = { prescriberMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = stringResource(uiState.prescribedBy.labelRes()),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.medicine_field_prescribed_by)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(prescriberMenuExpanded) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag(MedicationFormTestTags.PRESCRIBER),
                    )
                    ExposedDropdownMenu(
                        expanded = prescriberMenuExpanded,
                        onDismissRequest = { prescriberMenuExpanded = false },
                    ) {
                        Prescriber.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.labelRes())) },
                                onClick = {
                                    onPrescriberChange(option)
                                    prescriberMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                if (uiState.showsActiveSwitch) {
                    ActiveSwitchRow(isActive = uiState.isActive, onActiveChanged = onActiveChanged)
                }

                Text(
                    text = stringResource(R.string.medicine_schedules_header),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .semantics { heading() }
                        .testTag(MedicationFormTestTags.SCHEDULES_HEADER),
                )

                if (uiState.schedules.isEmpty()) {
                    Text(
                        text = stringResource(R.string.medicine_no_schedules),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(MedicationFormTestTags.NO_SCHEDULES),
                    )
                } else {
                    uiState.schedules.forEach { row ->
                        ScheduleRow(
                            description = formatter.describe(row.summary, row.amount),
                            onEdit = { onEditSchedule(row.index) },
                            onRemove = { onRemoveSchedule(row.index) },
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onAddSchedule,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Sizes.minTouchTarget)
                        .testTag(MedicationFormTestTags.ADD_SCHEDULE),
                ) {
                    Text(stringResource(R.string.medicine_add_schedule))
                }
            }
        }
    }

    if (uiState.showDiscardDialog) {
        DiscardDialog(onDiscard = onDiscard, onKeepEditing = onKeepEditing)
    }
}

/** The supporting text slot, which Material expects as a composable or null. */
@Composable
private fun MedicationFieldError?.supportingText(show: Boolean): (@Composable () -> Unit)? {
    val message = messageOrNull(show) ?: return null
    return { Text(message, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun MedicationFieldError?.messageOrNull(show: Boolean): String? {
    if (!show || this == null) return null
    return stringResource(
        when (this) {
            MedicationFieldError.NAME_REQUIRED -> R.string.medicine_error_name_required
            MedicationFieldError.DOSE_REQUIRED -> R.string.medicine_error_dose_required
            MedicationFieldError.DOSE_NOT_A_NUMBER -> R.string.medicine_error_dose_not_a_number
            MedicationFieldError.DOSE_NOT_POSITIVE -> R.string.medicine_error_dose_not_positive
            MedicationFieldError.USE_UNTIL_BEFORE_USED_SINCE -> R.string.medicine_error_use_until_before_used_since
        },
    )
}

/** The user-facing label of a prescriber. */
fun Prescriber.labelRes(): Int = when (this) {
    Prescriber.GENERAL_PRACTITIONER -> R.string.prescriber_general_practitioner
    Prescriber.SPECIALIST -> R.string.prescriber_specialist
    Prescriber.PHARMACIST -> R.string.prescriber_pharmacist
    Prescriber.SELF -> R.string.prescriber_self
    Prescriber.OTHER -> R.string.prescriber_other
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun MedicationFormScreenPreview() {
    PillsnerTheme {
        MedicationFormScreen(
            uiState = MedicationFormUiState(
                name = "Metoprolol",
                doseText = "40",
                doseUnit = DoseUnit.MILLIGRAM,
                usedSince = LocalDate.of(2026, 9, 13),
                schedules = listOf(
                    ScheduleRowState(
                        index = 0,
                        summary = ScheduleSummary.EveryNHours(12),
                        amount = Quantity.of("40", DoseUnit.MILLIGRAM),
                    ),
                ),
            ),
            onNameChange = {},
            onDoseTextChange = {},
            onDoseUnitChange = {},
            onUsedSinceChange = {},
            onUseUntilChange = {},
            onPrescriberChange = {},
            onActiveChanged = {},
            onAddSchedule = {},
            onEditSchedule = {},
            onRemoveSchedule = {},
            onSave = {},
            onBack = {},
            onDiscard = {},
            onKeepEditing = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun MedicationFormDetailsPreview() {
    PillsnerTheme {
        MedicationFormScreen(
            uiState = MedicationFormUiState(
                mode = MedicationFormMode.Edit(nl.hexmaster.pillsner.domain.model.MedicationId(1)),
                isActive = false,
                name = "Metoprolol",
                doseText = "40",
                doseUnit = DoseUnit.MILLIGRAM,
                usedSince = LocalDate.of(2026, 9, 13),
                schedules = listOf(
                    ScheduleRowState(
                        index = 0,
                        summary = ScheduleSummary.EveryNHours(12),
                        amount = Quantity.of("40", DoseUnit.MILLIGRAM),
                    ),
                ),
            ),
            onNameChange = {},
            onDoseTextChange = {},
            onDoseUnitChange = {},
            onUsedSinceChange = {},
            onUseUntilChange = {},
            onPrescriberChange = {},
            onActiveChanged = {},
            onAddSchedule = {},
            onEditSchedule = {},
            onRemoveSchedule = {},
            onSave = {},
            onBack = {},
            onDiscard = {},
            onKeepEditing = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
