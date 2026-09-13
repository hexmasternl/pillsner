package nl.hexmaster.pillsner.domain.model

import java.time.LocalDate

/** Identifies one medication. Stable for the life of the medication; Room assigns it. */
@JvmInline
value class MedicationId(val value: Long)

/**
 * Something the user takes, together with the rules that say when it is due.
 *
 * @property id identity of the medication.
 * @property name the medication's display name, exactly as the user entered it. Never blank.
 * @property defaultDose the amount a dose is unless a schedule says otherwise. Also the amount of
 *   an as-needed medication.
 * @property usedSince the day the user started, or starts, taking it. Also the anchor day that
 *   [Schedule.EveryNDays] counts its intervals from.
 * @property useUntil the last day it is taken, or null when it is open-ended.
 * @property prescribedBy who put the user on it.
 * @property schedules the rules that produce doses, in the order the user added them. An empty
 *   list means the medication is taken as needed.
 * @property isActive whether this medication currently produces doses. An inactive medication is
 *   kept for reference and history: the overview lists it under Inactive and the dose generator
 *   ignores it. It is never a deletion.
 * @throws IllegalArgumentException when the name is blank or [useUntil] is before [usedSince].
 */
data class Medication(
    val id: MedicationId,
    val name: String,
    val defaultDose: Quantity,
    val usedSince: LocalDate,
    val useUntil: LocalDate?,
    val prescribedBy: Prescriber,
    val schedules: List<Schedule>,
    val isActive: Boolean,
) {
    init {
        requireValidMedicationFields(name, usedSince, useUntil)
    }
}

/**
 * A medication the user has filled in but that has not been stored yet, so it has no identifier.
 * New medications are always saved as active.
 *
 * @throws IllegalArgumentException on the same rules as [Medication].
 */
data class NewMedication(
    val name: String,
    val defaultDose: Quantity,
    val usedSince: LocalDate,
    val useUntil: LocalDate?,
    val prescribedBy: Prescriber,
    val schedules: List<Schedule>,
) {
    init {
        requireValidMedicationFields(name, usedSince, useUntil)
    }
}

private fun requireValidMedicationFields(name: String, usedSince: LocalDate, useUntil: LocalDate?) {
    require(name.isNotBlank()) { "A medication needs a name" }
    require(useUntil == null || !useUntil.isBefore(usedSince)) {
        "A medication's use-until date cannot be before its used-since date"
    }
}
