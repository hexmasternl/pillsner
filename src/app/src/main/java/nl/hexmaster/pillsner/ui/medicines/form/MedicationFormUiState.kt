package nl.hexmaster.pillsner.ui.medicines.form

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.domain.model.StockBatchId
import nl.hexmaster.pillsner.domain.stock.StockState
import nl.hexmaster.pillsner.domain.validation.MedicationFieldError
import nl.hexmaster.pillsner.domain.validation.ScheduleDraftError
import nl.hexmaster.pillsner.domain.validation.SchedulePattern

/**
 * What the add-medicine form shows. Raw field text and parsed values live side by side so the
 * fields never fight the user's typing, and errors are typed rather than resolved strings so the
 * wording stays in the composable (design D5, D8).
 *
 * @property showErrors false until the user has tried to save, so an untouched form is not covered
 *   in red before they have done anything.
 */
data class MedicationFormUiState(
    val mode: MedicationFormMode = MedicationFormMode.Add,
    val isLoading: Boolean = false,
    val isActive: Boolean = true,
    val name: String = "",
    val doseText: String = "",
    val doseUnit: DoseUnit = DoseUnit.MILLIGRAM,
    val usedSince: LocalDate = FALLBACK_DATE,
    val useUntil: LocalDate? = null,
    val prescribedBy: Prescriber = Prescriber.GENERAL_PRACTITIONER,
    val schedules: List<ScheduleRowState> = emptyList(),
    val nameError: MedicationFieldError? = null,
    val doseError: MedicationFieldError? = null,
    val useUntilError: MedicationFieldError? = null,
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val hasEdits: Boolean = false,
    val showDiscardDialog: Boolean = false,
    /** Only meaningful in edit mode: an unsaved medicine cannot hold stock. */
    val stockBatches: List<StockBatchRowState> = emptyList(),
    /** The medicine's live stock picture, or null when it has no batches (`medicine-stock-tracking`). */
    val stockState: StockState? = null,
    /**
     * The stored default dose unit while the medicine has any stock batch, or null when it has none.
     * Each batch's strength is relative to that unit, so it cannot change until the batches are
     * removed (`medicine-stock-tracking`'s "Stock unit conversion" requirement).
     */
    val lockedDoseUnit: DoseUnit? = null,
    /** Non-null while the Add stock form is open. */
    val addStockState: AddStockUiState? = null,
    /** Non-null while the removal confirmation dialog is open for this batch (`medicine-stock-tracking`). */
    val pendingStockRemoval: StockBatchId? = null,
) {
    /** Set when [doseUnit] has been moved away from [lockedDoseUnit]; shown straight away, not only on save. */
    val doseUnitError: MedicationFieldError?
        get() = MedicationFieldError.DOSE_UNIT_LOCKED_BY_STOCK.takeIf { lockedDoseUnit != null && doseUnit != lockedDoseUnit }

    /**
     * Whether the draft would pass validation. The Save button stays tappable regardless, because
     * tapping it is what reveals the errors; this decides whether the tap stores anything.
     */
    val canSave: Boolean
        get() = !isSaving && nameError == null && doseError == null && doseUnitError == null && useUntilError == null

    /** Only an existing medicine can be started and stopped here; a new one is always active. */
    val showsActiveSwitch: Boolean get() = mode is MedicationFormMode.Edit
}

/** One schedule as the form lists it. */
data class ScheduleRowState(
    val index: Int,
    val summary: ScheduleSummary,
    val amount: Quantity,
)

/**
 * One stock batch as the Stock section lists it, ordered by expiry date ascending.
 *
 * @property strengthPerUnit how much of the medicine's default dose unit one [unit] is worth; 1
 *   when [unit] matches that default dose unit, in which case the row shows no strength at all.
 */
data class StockBatchRowState(
    val id: StockBatchId,
    val remaining: java.math.BigDecimal,
    val unit: DoseUnit,
    val strengthPerUnit: java.math.BigDecimal,
    val expiryDate: LocalDate,
)

/**
 * The Add stock form (`medicine-stock-tracking`).
 *
 * @property defaultDoseUnit the medicine's own default dose unit; [unit] defaults to it and the
 *   strength field appears only once [unit] is changed away from it.
 * @property unit the batch's own unit, chosen from the same fixed list the rest of the app uses.
 * @property strengthText how much of [defaultDoseUnit] one [unit] is worth, e.g. "20" for 20 mg per
 *   tablet; only meaningful, shown and validated when [unit] differs from [defaultDoseUnit].
 * @property quantityError and [strengthError] reuse [MedicationFieldError]'s dose-amount values,
 *   since the rule is identical for both (a decimal greater than zero); the Add stock dialog gives
 *   them their own wording.
 * @property expiryPastWarning advisory only, never blocking: an expiry date already in the past is
 *   still accepted.
 */
data class AddStockUiState(
    val quantityText: String = "",
    val defaultDoseUnit: DoseUnit = DoseUnit.MILLIGRAM,
    val unit: DoseUnit = DoseUnit.MILLIGRAM,
    val strengthText: String = "",
    val expiryDate: LocalDate? = null,
    val showErrors: Boolean = false,
    val quantityError: MedicationFieldError? = null,
    val strengthError: MedicationFieldError? = null,
    val expiryPastWarning: Boolean = false,
    val isSaving: Boolean = false,
) {
    /** Whether the strength field applies at all: only when the batch's unit differs from the dose's. */
    val needsStrength: Boolean get() = unit != defaultDoseUnit

    val canSave: Boolean
        get() = !isSaving &&
            quantityError == null &&
            (!needsStrength || strengthError == null) &&
            expiryDate != null
}

/**
 * What the schedule editor shows. [firstError] is what the preview area displays instead of the
 * description while the draft cannot be saved.
 *
 * @property isNew true when the editor was opened by "Add schedule" rather than from a row.
 * @property duplicateTimeRejected set when the user picked a time that is already in the list; the
 *   screen shows the message and clears it.
 */
data class ScheduleEditorUiState(
    val isNew: Boolean = true,
    val amountText: String = "",
    val amountUnit: DoseUnit = DoseUnit.MILLIGRAM,
    val pattern: SchedulePattern = SchedulePattern.EVERY_N_DAYS,
    val intervalDays: Int = 1,
    val intervalHours: Int = ScheduleDraft.DEFAULT_INTERVAL_HOURS,
    val days: Set<DayOfWeek> = emptySet(),
    val times: List<LocalTime> = emptyList(),
    val firstDoseAt: LocalTime = ScheduleDraft.DEFAULT_FIRST_DOSE,
    val dailyDoseTimes: List<LocalTime> = emptyList(),
    val preview: ScheduleSummary? = null,
    val previewAmount: Quantity? = null,
    val errors: List<ScheduleDraftError> = emptyList(),
    val showErrors: Boolean = false,
    val duplicateTimeRejected: Boolean = false,
) {
    val firstError: ScheduleDraftError? get() = errors.firstOrNull()
    val canSave: Boolean get() = errors.isEmpty()
    val showsAllSevenDaysHint: Boolean
        get() = pattern == SchedulePattern.ON_WEEKDAYS && days.size == DAYS_IN_WEEK

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}

/** Something that happened once and must not be replayed on the next recomposition. */
sealed interface MedicationFormEffect {
    /** The medicine was stored; the flow should close. */
    data object Saved : MedicationFormEffect

    /** Storing failed; the user stays on the form and is told, without naming the medicine. */
    data object SaveFailed : MedicationFormEffect

    /** The medicine could not be opened at all; the flow closes and the overview says so. */
    data object OpenFailed : MedicationFormEffect
}

/**
 * Only ever seen before the view model has published real state. `LocalDate.EPOCH` would be the
 * obvious choice but it needs API 34, and Pillsner supports API 26.
 */
private val FALLBACK_DATE: LocalDate = LocalDate.of(1970, 1, 1)
