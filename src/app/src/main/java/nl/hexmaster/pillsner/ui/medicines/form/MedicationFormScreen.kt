package nl.hexmaster.pillsner.ui.medicines.form

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.time.LocalDate
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.BatchExpiryState
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.domain.model.StockBatchId
import nl.hexmaster.pillsner.domain.stock.StockState
import nl.hexmaster.pillsner.domain.validation.MedicationFieldError
import nl.hexmaster.pillsner.ui.medicines.QuantityFormatter
import nl.hexmaster.pillsner.ui.medicines.labelRes
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
    const val OVERFLOW = "medication_form_overflow"
    const val USAGE_HISTORY = "medication_form_usage_history"
    const val STOCK_HEADER = "medication_form_stock_header"
    const val STOCK_NONE = "medication_form_stock_none"
    const val STOCK_ADD = "medication_form_stock_add"
    const val STOCK_ROW = "medication_form_stock_row"
    const val STOCK_ROW_REMOVE = "medication_form_stock_row_remove"
    const val STOCK_REMOVE_CONFIRM = "medication_form_stock_remove_confirm"
    const val STOCK_REMOVE_CANCEL = "medication_form_stock_remove_cancel"
    const val STOCK_LOW_NOTE = "medication_form_stock_low_note"
    const val STOCK_EXPIRY_NOTE = "medication_form_stock_expiry_note"
    const val ADD_STOCK_QUANTITY = "add_stock_quantity"
    const val ADD_STOCK_STRENGTH = "add_stock_strength"
    const val ADD_STOCK_EXPIRY = "add_stock_expiry"
    const val ADD_STOCK_SAVE = "add_stock_save"
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
    onOpenUsageHistory: () -> Unit = {},
    onAddStockClicked: () -> Unit = {},
    onAddStockDismissed: () -> Unit = {},
    onStockQuantityTextChange: (String) -> Unit = {},
    onStockUnitChange: (DoseUnit) -> Unit = {},
    onStockStrengthTextChange: (String) -> Unit = {},
    onStockExpiryDateChange: (LocalDate) -> Unit = {},
    onSaveStockBatch: () -> Unit = {},
    onRemoveStockBatchClicked: (StockBatchId) -> Unit = {},
    onRemoveStockBatchCancelled: () -> Unit = {},
    onRemoveStockBatchConfirmed: () -> Unit = {},
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
                actions = {
                    // Only on a saved medicine: an unsaved one has no history to look at.
                    if (uiState.showsActiveSwitch) {
                        OverflowMenu(onOpenUsageHistory = onOpenUsageHistory)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
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
                    errorMessage = uiState.doseError.messageOrNull(uiState.showErrors)
                        ?: uiState.doseUnitError.messageOrNull(show = true),
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

                if (uiState.showsActiveSwitch) {
                    StockSection(
                        batches = uiState.stockBatches,
                        stockState = uiState.stockState,
                        defaultDoseUnit = uiState.doseUnit,
                        onAddStock = onAddStockClicked,
                        onRemoveStock = onRemoveStockBatchClicked,
                    )
                }
            }
        }
    }

    if (uiState.showDiscardDialog) {
        DiscardDialog(onDiscard = onDiscard, onKeepEditing = onKeepEditing)
    }

    uiState.addStockState?.let { addStockState ->
        AddStockDialog(
            state = addStockState,
            onQuantityChange = onStockQuantityTextChange,
            onUnitChange = onStockUnitChange,
            onStrengthChange = onStockStrengthTextChange,
            onExpiryChange = onStockExpiryDateChange,
            onSave = onSaveStockBatch,
            onDismiss = onAddStockDismissed,
        )
    }

    if (uiState.pendingStockRemoval != null) {
        RemoveStockBatchDialog(
            onConfirm = onRemoveStockBatchConfirmed,
            onCancel = onRemoveStockBatchCancelled,
        )
    }
}

/**
 * Confirms a stock batch's manual removal (`medicine-stock-tracking`'s "Removing a stock batch"
 * requirement): unlike automatic consumption, this is a deliberate, irreversible user action, so
 * it goes through the same destructive-confirmation pattern as discarding unsaved edits.
 */
@Composable
private fun RemoveStockBatchDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.medicine_stock_remove_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.medicine_stock_remove_body),
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag(MedicationFormTestTags.STOCK_REMOVE_CONFIRM)) {
                Text(stringResource(R.string.action_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag(MedicationFormTestTags.STOCK_REMOVE_CANCEL)) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * The Stock section (`medicine-stock-tracking`): the medicine's batches ordered by expiry date,
 * each with its remaining amount, an inline note of the medicine's current low-stock and/or
 * expiry-at-use state, and an "Add stock" button. Shown only in edit mode, since an unsaved
 * medicine cannot hold stock.
 */
@Composable
private fun StockSection(
    batches: List<StockBatchRowState>,
    stockState: StockState?,
    defaultDoseUnit: DoseUnit,
    onAddStock: () -> Unit,
    onRemoveStock: (StockBatchId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val quantityFormatter = remember(context) { QuantityFormatter(context) }
    val dateFormatter = rememberDateFormatter()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.medicine_stock_header),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .semantics { heading() }
                .testTag(MedicationFormTestTags.STOCK_HEADER),
        )

        if (stockState?.isLow == true) {
            Text(
                text = stringResource(R.string.medicine_stock_low_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag(MedicationFormTestTags.STOCK_LOW_NOTE),
            )
        }
        when (stockState?.nearestExpiry) {
            BatchExpiryState.APPROACHING -> Text(
                text = stringResource(R.string.medicine_stock_expiring_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag(MedicationFormTestTags.STOCK_EXPIRY_NOTE),
            )
            BatchExpiryState.PAST -> Text(
                text = stringResource(R.string.medicine_stock_expired_note),
                style = MaterialTheme.typography.bodyMedium,
                // Not an error role: design system 2.4 reserves red for overdue/missed doses,
                // missing permissions, destructive confirmations, stock at zero and validation
                // errors. An expired batch is distinguished by wording alone, not colour.
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag(MedicationFormTestTags.STOCK_EXPIRY_NOTE),
            )
            else -> Unit
        }

        if (batches.isEmpty()) {
            Text(
                text = stringResource(R.string.medicine_stock_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(MedicationFormTestTags.STOCK_NONE),
            )
        } else {
            batches.forEach { batch ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(
                                R.string.medicine_stock_batch_row,
                                quantityFormatter.format(batch.remaining, batch.unit),
                                dateFormatter.format(batch.expiryDate),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    supportingContent = {
                        // Only shown once a batch's unit differs from the dose's own — a strength
                        // of 1 means there is nothing to convert and so nothing worth stating.
                        if (batch.strengthPerUnit.compareTo(java.math.BigDecimal.ONE) != 0) {
                            Text(
                                text = stringResource(
                                    R.string.medicine_stock_batch_row_strength,
                                    quantityFormatter.format(batch.strengthPerUnit, defaultDoseUnit),
                                    stringResource(batch.unit.labelRes()),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                    trailingContent = {
                        IconButton(
                            onClick = { onRemoveStock(batch.id) },
                            modifier = Modifier
                                .sizeIn(minWidth = Sizes.minTouchTarget, minHeight = Sizes.minTouchTarget)
                                .testTag(MedicationFormTestTags.STOCK_ROW_REMOVE),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.medicine_stock_remove_content_description),
                                modifier = Modifier.size(Sizes.iconDefault),
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(MedicationFormTestTags.STOCK_ROW),
                )
            }
        }

        FilledTonalButton(
            onClick = onAddStock,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Sizes.minTouchTarget)
                .testTag(MedicationFormTestTags.STOCK_ADD),
        ) {
            Text(stringResource(R.string.medicine_stock_add))
        }
    }
}

/**
 * The Add stock form: a quantity with its own unit (defaulting to the medicine's dose unit but
 * freely changeable), a strength field that appears only once the unit is changed away from that
 * default, and a required expiry date (`medicine-stock-tracking`'s "Add stock form" and "Stock unit
 * conversion" requirements).
 */
@Composable
private fun AddStockDialog(
    state: AddStockUiState,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (DoseUnit) -> Unit,
    onStrengthChange: (String) -> Unit,
    onExpiryChange: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.medicine_stock_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                QuantityField(
                    label = stringResource(R.string.medicine_stock_field_quantity),
                    amount = state.quantityText,
                    unit = state.unit,
                    onAmountChange = onQuantityChange,
                    onUnitChange = onUnitChange,
                    errorMessage = state.quantityError.stockErrorText(state.showErrors),
                    testTagPrefix = MedicationFormTestTags.ADD_STOCK_QUANTITY,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.needsStrength) {
                    OutlinedTextField(
                        value = state.strengthText,
                        onValueChange = onStrengthChange,
                        label = { Text(stringResource(R.string.medicine_stock_field_strength)) },
                        suffix = {
                            Text(
                                stringResource(
                                    R.string.medicine_stock_strength_suffix,
                                    stringResource(state.defaultDoseUnit.labelRes()),
                                    stringResource(state.unit.labelRes()),
                                ),
                            )
                        },
                        isError = state.showErrors && state.strengthError != null,
                        supportingText = state.strengthError.strengthErrorText(state.showErrors)?.let { message ->
                            { Text(message, style = MaterialTheme.typography.bodySmall) }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(MedicationFormTestTags.ADD_STOCK_STRENGTH),
                    )
                }
                DateField(
                    label = stringResource(R.string.medicine_stock_field_expiry),
                    date = state.expiryDate,
                    onDateChange = onExpiryChange,
                    testTag = MedicationFormTestTags.ADD_STOCK_EXPIRY,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.expiryPastWarning) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = null,
                            modifier = Modifier.size(Sizes.iconDefault),
                        )
                        Text(
                            stringResource(R.string.medicine_stock_expiry_past_warning),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.testTag(MedicationFormTestTags.ADD_STOCK_SAVE),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun MedicationFieldError?.stockErrorText(show: Boolean): String? {
    if (!show || this == null) return null
    return stringResource(
        when (this) {
            MedicationFieldError.DOSE_REQUIRED -> R.string.medicine_stock_error_quantity_required
            MedicationFieldError.DOSE_NOT_A_NUMBER -> R.string.medicine_stock_error_quantity_not_a_number
            MedicationFieldError.DOSE_NOT_POSITIVE -> R.string.medicine_stock_error_quantity_not_positive
            else -> return null
        },
    )
}

@Composable
private fun MedicationFieldError?.strengthErrorText(show: Boolean): String? {
    if (!show || this == null) return null
    return stringResource(
        when (this) {
            MedicationFieldError.DOSE_REQUIRED -> R.string.medicine_stock_error_strength_required
            MedicationFieldError.DOSE_NOT_A_NUMBER -> R.string.medicine_stock_error_strength_not_a_number
            MedicationFieldError.DOSE_NOT_POSITIVE -> R.string.medicine_stock_error_strength_not_positive
            else -> return null
        },
    )
}

@Composable
private fun rememberDateFormatter(): java.time.format.DateTimeFormatter {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return remember(locale) {
        java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(locale)
    }
}

private fun java.time.format.DateTimeFormatter.format(date: LocalDate): String = date.format(this)

/**
 * The details screen's secondary actions (design D8). Exactly one item, and nothing destructive
 * will ever join it: the medicine-details spec forbids a delete, remove or archive action anywhere
 * on this screen, its menus included.
 */
@Composable
private fun OverflowMenu(onOpenUsageHistory: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .sizeIn(minWidth = Sizes.minTouchTarget, minHeight = Sizes.minTouchTarget)
                .testTag(MedicationFormTestTags.OVERFLOW),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.action_more_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.usage_history_menu_item)) },
                onClick = {
                    expanded = false
                    onOpenUsageHistory()
                },
                modifier = Modifier.testTag(MedicationFormTestTags.USAGE_HISTORY),
            )
        }
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
            MedicationFieldError.DOSE_UNIT_LOCKED_BY_STOCK -> R.string.medicine_error_dose_unit_locked_by_stock
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

@PreviewLightDark
@Preview(name = "Large font", fontScale = 2f)
@Composable
private fun MedicationFormStockSectionPreview() {
    PillsnerTheme {
        MedicationFormScreen(
            uiState = MedicationFormUiState(
                mode = MedicationFormMode.Edit(nl.hexmaster.pillsner.domain.model.MedicationId(1)),
                name = "Metoprolol",
                doseText = "40",
                doseUnit = DoseUnit.MILLIGRAM,
                usedSince = LocalDate.of(2026, 9, 13),
                stockBatches = listOf(
                    StockBatchRowState(
                        id = StockBatchId(1),
                        remaining = java.math.BigDecimal("10"),
                        unit = DoseUnit.TABLET,
                        strengthPerUnit = java.math.BigDecimal("20"),
                        expiryDate = LocalDate.of(2026, 10, 1),
                    ),
                    StockBatchRowState(
                        id = StockBatchId(2),
                        remaining = java.math.BigDecimal("300"),
                        unit = DoseUnit.MILLIGRAM,
                        strengthPerUnit = java.math.BigDecimal.ONE,
                        expiryDate = LocalDate.of(2027, 1, 1),
                    ),
                ),
                stockState = StockState(isLow = true, nearestExpiry = BatchExpiryState.APPROACHING),
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
private fun AddStockDialogPreview() {
    PillsnerTheme {
        AddStockDialog(
            state = AddStockUiState(
                quantityText = "20",
                defaultDoseUnit = DoseUnit.MILLIGRAM,
                unit = DoseUnit.TABLET,
                strengthText = "20",
                expiryDate = LocalDate.of(2027, 1, 1),
            ),
            onQuantityChange = {},
            onUnitChange = {},
            onStrengthChange = {},
            onExpiryChange = {},
            onSave = {},
            onDismiss = {},
        )
    }
}
