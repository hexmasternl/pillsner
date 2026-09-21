package nl.hexmaster.pillsner.ui.medicines.form

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
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
) {
    /**
     * Whether the draft would pass validation. The Save button stays tappable regardless, because
     * tapping it is what reveals the errors; this decides whether the tap stores anything.
     */
    val canSave: Boolean
        get() = !isSaving && nameError == null && doseError == null && useUntilError == null

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
