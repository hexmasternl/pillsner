package nl.hexmaster.pillsner.ui.medicines.form

import androidx.lifecycle.SavedStateHandle
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.validation.SchedulePattern
import nl.hexmaster.pillsner.ui.navigation.MedicationFormGraph

/** Which entrance the user came through, and therefore what Save does. */
sealed interface MedicationFormMode {
    /** A new medicine; Save stores it. */
    data object Add : MedicationFormMode

    /** An existing medicine; Save replaces it. */
    data class Edit(val id: MedicationId) : MedicationFormMode
}

/**
 * Everything the user has filled in on the medicine form so far, including the schedules built in
 * the editor. Amounts stay as the raw text the user typed so a half-typed "2." is never rewritten
 * under their fingers (design D5).
 *
 * @property medicationId the medicine being edited, or null when a new one is being added.
 */
data class MedicationFormDraft(
    val medicationId: Long? = null,
    val name: String = "",
    val doseText: String = "",
    val doseUnit: DoseUnit = DoseUnit.MILLIGRAM,
    val usedSince: LocalDate,
    val useUntil: LocalDate? = null,
    val prescribedBy: Prescriber = Prescriber.GENERAL_PRACTITIONER,
    val schedules: List<Schedule> = emptyList(),
    /** Only editable in [MedicationFormMode.Edit]; a new medicine is always active. */
    val isActive: Boolean = true,
) {
    companion object {
        /** Opens the form on an existing medicine. */
        fun from(medication: Medication, amountText: String) = MedicationFormDraft(
            medicationId = medication.id.value,
            name = medication.name,
            doseText = amountText,
            doseUnit = medication.defaultDose.unit,
            usedSince = medication.usedSince,
            useUntil = medication.useUntil,
            prescribedBy = medication.prescribedBy,
            schedules = medication.schedules,
            isActive = medication.isActive,
        )
    }
}

/**
 * Applies a label-scan's optional guesses to a fresh add-mode draft (medicine-add-label-scan
 * design D3). Only ever called once, at construction, before the draft has anything else in it, so
 * this can freely overwrite [MedicationFormDraft]'s still-default fields; called again later it
 * would silently undo whatever the user had typed since.
 *
 * A blank recognized name or amount counts as nothing recognized, the same as a null one.
 */
fun MedicationFormDraft.seededFromScan(route: MedicationFormGraph): MedicationFormDraft = copy(
    name = route.scannedName?.takeIf(String::isNotBlank) ?: name,
    doseText = route.scannedDoseAmount?.takeIf(String::isNotBlank) ?: doseText,
    doseUnit = route.scannedDoseUnit ?: doseUnit,
)

/** The medicine this draft describes, under [id]. Only valid once the draft passes validation. */
fun MedicationFormDraft.toMedication(id: MedicationId, defaultDose: Quantity) = Medication(
    id = id,
    name = name.trim(),
    defaultDose = defaultDose,
    usedSince = usedSince,
    useUntil = useUntil,
    prescribedBy = prescribedBy,
    schedules = schedules,
    isActive = isActive,
)

/**
 * The schedule being built or edited in the editor.
 *
 * @property index where the schedule sits in [MedicationFormDraft.schedules], or null when it is new.
 */
data class ScheduleDraft(
    val index: Int? = null,
    val amountText: String = "",
    val amountUnit: DoseUnit = DoseUnit.MILLIGRAM,
    val pattern: SchedulePattern = SchedulePattern.EVERY_N_DAYS,
    val intervalDays: Int = 1,
    val intervalHours: Int = DEFAULT_INTERVAL_HOURS,
    val days: Set<DayOfWeek> = emptySet(),
    val times: List<LocalTime> = emptyList(),
    val firstDoseAt: LocalTime = DEFAULT_FIRST_DOSE,
    /** Set once the user has tried to finish the schedule, so an untouched editor shows no errors. */
    val showErrors: Boolean = false,
) {
    companion object {
        const val DEFAULT_INTERVAL_HOURS = 12
        val DEFAULT_FIRST_DOSE: LocalTime = LocalTime.of(8, 0)

        /** The intervals the editor offers for every-N-hours; each divides a day evenly. */
        val HOUR_INTERVALS = listOf(1, 2, 3, 4, 6, 8, 12, 24)

        /** Opens the editor on an existing [schedule]. */
        fun from(index: Int, schedule: Schedule, amountText: String) = when (schedule) {
            is Schedule.EveryNDays -> ScheduleDraft(
                index = index,
                amountText = amountText,
                amountUnit = schedule.amount.unit,
                pattern = SchedulePattern.EVERY_N_DAYS,
                intervalDays = schedule.intervalDays,
                times = schedule.times,
            )

            is Schedule.OnWeekdays -> ScheduleDraft(
                index = index,
                amountText = amountText,
                amountUnit = schedule.amount.unit,
                pattern = SchedulePattern.ON_WEEKDAYS,
                days = schedule.days,
                times = schedule.times,
            )

            is Schedule.EveryNHours -> ScheduleDraft(
                index = index,
                amountText = amountText,
                amountUnit = schedule.amount.unit,
                pattern = SchedulePattern.EVERY_N_HOURS,
                intervalHours = schedule.intervalHours,
                firstDoseAt = schedule.firstDoseAt,
            )
        }
    }
}

/**
 * Builds the [Schedule] this draft describes, or null when it is not valid yet.
 *
 * @param amount the already-parsed amount, or null when the text is not a number.
 */
fun ScheduleDraft.toSchedule(amount: BigDecimal?): Schedule? {
    val quantity = amount?.takeIf { it > BigDecimal.ZERO }?.let { Quantity(it, amountUnit) } ?: return null
    return when (pattern) {
        SchedulePattern.EVERY_N_DAYS -> if (times.isEmpty()) {
            null
        } else {
            Schedule.EveryNDays(quantity, intervalDays, times)
        }

        SchedulePattern.ON_WEEKDAYS -> if (times.isEmpty() || days.isEmpty() || days.size == Schedule.DAYS_IN_WEEK) {
            null
        } else {
            Schedule.OnWeekdays(quantity, days, times)
        }

        SchedulePattern.EVERY_N_HOURS -> Schedule.EveryNHours(quantity, intervalHours, firstDoseAt)
    }
}

/**
 * Saves and restores the draft through a [SavedStateHandle] so it survives rotation and process
 * death while the add flow is on the back stack (design D4).
 *
 * Everything is stored as a string: the handle's bundle cannot hold a `BigDecimal`, a `LocalTime`
 * or a sealed-interface instance, and strings keep the stored form readable in a test.
 */
object DraftSaver {

    private const val KEY_NAME = "draft_name"
    private const val KEY_DOSE_TEXT = "draft_dose_text"
    private const val KEY_DOSE_UNIT = "draft_dose_unit"
    private const val KEY_USED_SINCE = "draft_used_since"
    private const val KEY_USE_UNTIL = "draft_use_until"
    private const val KEY_PRESCRIBED_BY = "draft_prescribed_by"
    private const val KEY_SCHEDULES = "draft_schedules"
    private const val KEY_MEDICATION_ID = "draft_medication_id"
    private const val KEY_IS_ACTIVE = "draft_is_active"

    /** The draft as it was when the form opened, so "has the user changed anything" survives too. */
    private const val INITIAL = "initial_"

    fun save(handle: SavedStateHandle, draft: MedicationFormDraft, prefix: String = "") {
        handle[prefix + KEY_MEDICATION_ID] = draft.medicationId?.toString() ?: ""
        handle[prefix + KEY_NAME] = draft.name
        handle[prefix + KEY_DOSE_TEXT] = draft.doseText
        handle[prefix + KEY_DOSE_UNIT] = draft.doseUnit.name
        handle[prefix + KEY_USED_SINCE] = draft.usedSince.toString()
        handle[prefix + KEY_USE_UNTIL] = draft.useUntil?.toString() ?: ""
        handle[prefix + KEY_PRESCRIBED_BY] = draft.prescribedBy.name
        handle[prefix + KEY_IS_ACTIVE] = draft.isActive
        handle[prefix + KEY_SCHEDULES] = ArrayList(draft.schedules.map(ScheduleCodec::encode))
    }

    /** Saves the draft the form opened with, next to the working one. */
    fun saveInitial(handle: SavedStateHandle, draft: MedicationFormDraft) = save(handle, draft, INITIAL)

    /**
     * The draft the form opened with, or null when nothing was saved. The fallback date is never
     * used: the key it is keyed on is exactly what decides whether anything was saved at all.
     */
    fun restoreInitial(handle: SavedStateHandle): MedicationFormDraft? =
        if (handle.contains(INITIAL + KEY_USED_SINCE)) {
            restore(handle, UNUSED_FALLBACK_DATE, INITIAL)
        } else {
            null
        }

    /** Restores a saved draft, or builds a fresh one starting on [today] when nothing was saved. */
    fun restore(handle: SavedStateHandle, today: LocalDate, prefix: String = ""): MedicationFormDraft {
        val usedSince = handle.get<String>(prefix + KEY_USED_SINCE)
            ?: return MedicationFormDraft(usedSince = today)
        val useUntil = handle.get<String>(prefix + KEY_USE_UNTIL).orEmpty()
        val medicationId = handle.get<String>(prefix + KEY_MEDICATION_ID).orEmpty()
        return MedicationFormDraft(
            medicationId = medicationId.takeIf { it.isNotEmpty() }?.toLong(),
            name = handle.get<String>(prefix + KEY_NAME).orEmpty(),
            doseText = handle.get<String>(prefix + KEY_DOSE_TEXT).orEmpty(),
            doseUnit = handle.get<String>(prefix + KEY_DOSE_UNIT)?.let(DoseUnit::valueOf) ?: DoseUnit.MILLIGRAM,
            usedSince = LocalDate.parse(usedSince),
            useUntil = useUntil.takeIf { it.isNotEmpty() }?.let(LocalDate::parse),
            prescribedBy = handle.get<String>(prefix + KEY_PRESCRIBED_BY)?.let(Prescriber::valueOf)
                ?: Prescriber.GENERAL_PRACTITIONER,
            isActive = handle.get<Boolean>(prefix + KEY_IS_ACTIVE) != false,
            schedules = handle.get<ArrayList<String>>(prefix + KEY_SCHEDULES).orEmpty().map(ScheduleCodec::decode),
        )
    }

    /** Whether a working draft has been saved at all. */
    fun hasSavedDraft(handle: SavedStateHandle): Boolean = handle.contains(KEY_USED_SINCE)

    /** `LocalDate.EPOCH` would be the obvious spelling, but it needs API 34 and minSdk is 26. */
    private val UNUSED_FALLBACK_DATE: LocalDate = LocalDate.of(1970, 1, 1)
}

/** One [Schedule] as a single line of text, for saved state. Never shown to the user. */
object ScheduleCodec {

    private const val FIELD = "|"
    private const val LIST = ","

    fun encode(schedule: Schedule): String {
        val amount = "${schedule.amount.value.toPlainString()}$FIELD${schedule.amount.unit.name}"
        return when (schedule) {
            is Schedule.EveryNDays ->
                listOf("D", amount, schedule.intervalDays.toString(), schedule.times.joinToString(LIST))

            is Schedule.OnWeekdays -> listOf(
                "W",
                amount,
                schedule.days.sorted().joinToString(LIST) { it.name },
                schedule.times.joinToString(LIST),
            )

            is Schedule.EveryNHours ->
                listOf("H", amount, schedule.intervalHours.toString(), schedule.firstDoseAt.toString())
        }.joinToString(FIELD)
    }

    fun decode(value: String): Schedule {
        val parts = value.split(FIELD)
        val amount = Quantity(BigDecimal(parts[1]), DoseUnit.valueOf(parts[2]))
        return when (parts[0]) {
            "D" -> Schedule.EveryNDays(amount, parts[3].toInt(), parts[4].toTimes())
            "W" -> Schedule.OnWeekdays(
                amount,
                parts[3].split(LIST).map(DayOfWeek::valueOf).toSet(),
                parts[4].toTimes(),
            )

            "H" -> Schedule.EveryNHours(amount, parts[3].toInt(), LocalTime.parse(parts[4]))
            else -> error("Unknown encoded schedule kind '${parts[0]}'")
        }
    }

    private fun String.toTimes(): List<LocalTime> =
        split(LIST).filter { it.isNotBlank() }.map(LocalTime::parse)
}
