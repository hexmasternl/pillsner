package nl.hexmaster.pillsner.data.db

import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.NewMedication
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule

/**
 * Rows to domain and back (design D3).
 *
 * Reading is where the storage layer's nullable columns are turned back into a closed set of
 * shapes. A row that does not satisfy its kind's invariants is a bug or a corrupt database, so it
 * fails loudly and names the row rather than producing a schedule that is only half valid.
 */
fun MedicationWithSchedules.toDomain(): Medication = Medication(
    id = MedicationId(medication.id),
    name = medication.name,
    defaultDose = Quantity(medication.defaultDoseValue, DoseUnit.valueOf(medication.defaultDoseUnit)),
    usedSince = medication.usedSince,
    useUntil = medication.useUntil,
    prescribedBy = Prescriber.valueOf(medication.prescribedBy),
    schedules = schedules.sortedBy { it.position }.map { it.toDomain() },
    isActive = medication.isActive,
)

fun ScheduleEntity.toDomain(): Schedule {
    val amount = Quantity(amountValue, DoseUnit.valueOf(amountUnit))
    return when (kind) {
        ScheduleKind.EVERY_N_DAYS -> Schedule.EveryNDays(
            amount = amount,
            intervalDays = required(intervalDays, "interval_days"),
            times = required(times, "times"),
        )

        ScheduleKind.ON_WEEKDAYS -> Schedule.OnWeekdays(
            amount = amount,
            days = required(days, "days"),
            times = required(times, "times"),
        )

        ScheduleKind.EVERY_N_HOURS -> Schedule.EveryNHours(
            amount = amount,
            intervalHours = required(intervalHours, "interval_hours"),
            firstDoseAt = required(firstDoseAt, "first_dose_at"),
        )

        else -> error("Schedule row $id has unknown kind '$kind'")
    }
}

/** Builds the rows for [medication]; [medicationId] is 0 until the medication row is inserted. */
fun NewMedication.toEntity(): MedicationEntity = MedicationEntity(
    name = name,
    defaultDoseValue = defaultDose.value,
    defaultDoseUnit = defaultDose.unit.name,
    usedSince = usedSince,
    useUntil = useUntil,
    prescribedBy = prescribedBy.name,
    isActive = true,
)

fun Medication.toEntity(): MedicationEntity = MedicationEntity(
    id = id.value,
    name = name,
    defaultDoseValue = defaultDose.value,
    defaultDoseUnit = defaultDose.unit.name,
    usedSince = usedSince,
    useUntil = useUntil,
    prescribedBy = prescribedBy.name,
    isActive = isActive,
)

/** @param medicationId 0 while the medication row has not been inserted yet; the DAO fills it in. */
fun Schedule.toEntity(medicationId: Long, position: Int): ScheduleEntity = when (this) {
    is Schedule.EveryNDays -> baseEntity(medicationId, position, ScheduleKind.EVERY_N_DAYS).copy(
        intervalDays = intervalDays,
        times = times,
    )

    is Schedule.OnWeekdays -> baseEntity(medicationId, position, ScheduleKind.ON_WEEKDAYS).copy(
        days = days,
        times = times,
    )

    is Schedule.EveryNHours -> baseEntity(medicationId, position, ScheduleKind.EVERY_N_HOURS).copy(
        intervalHours = intervalHours,
        firstDoseAt = firstDoseAt,
    )
}

private fun Schedule.baseEntity(medicationId: Long, position: Int, kind: String) = ScheduleEntity(
    medicationId = medicationId,
    position = position,
    kind = kind,
    amountValue = amount.value,
    amountUnit = amount.unit.name,
)

private fun <T : Any> ScheduleEntity.required(value: T?, column: String): T =
    checkNotNull(value) { "Schedule row $id of kind '$kind' has no $column" }
