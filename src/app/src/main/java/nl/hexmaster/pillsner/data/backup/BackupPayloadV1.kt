package nl.hexmaster.pillsner.data.backup

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import nl.hexmaster.pillsner.domain.backup.BackupDose
import nl.hexmaster.pillsner.domain.backup.BackupMedication
import nl.hexmaster.pillsner.domain.backup.BackupSnapshot
import nl.hexmaster.pillsner.domain.backup.BackupStockBatch
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule

/**
 * Format version 1 of the backup payload: what is inside the encryption, before compression
 * (`data-backup-restore`).
 *
 * Its own types rather than `@Serializable` on the domain models, for the same reason a Room entity
 * is not a domain model: this shape is frozen the day it ships, and the domain is free to move on.
 * A later format gets its own file next to this one; this one is never edited, so a version 1 file
 * stays readable for as long as the app supports it.
 *
 * Every value is stored the way the database stores it: decimals as plain strings so "2.50" never
 * becomes 2.4999, dates, times and instants as ISO text, enums by name.
 */
@Serializable
internal data class PayloadV1(
    val medications: List<MedicationV1>,
    val stockBatches: List<StockBatchV1>,
    val doses: List<DoseV1>,
)

@Serializable
internal data class MedicationV1(
    val id: String,
    val name: String,
    val defaultDoseValue: String,
    val defaultDoseUnit: String,
    val usedSince: String,
    val useUntil: String?,
    val prescribedBy: String,
    val isActive: Boolean,
    val lowStockAcknowledgement: String?,
    val schedules: List<ScheduleV1>,
)

/** One schedule; which of the nullable fields are present follows [kind], as in the database. */
@Serializable
internal data class ScheduleV1(
    val kind: String,
    val amountValue: String,
    val amountUnit: String,
    val intervalDays: Int? = null,
    val intervalHours: Int? = null,
    val days: List<String>? = null,
    val times: List<String>? = null,
    val firstDoseAt: String? = null,
)

@Serializable
internal data class StockBatchV1(
    val medicationId: String,
    val remainingValue: String,
    val remainingUnit: String,
    val strengthPerUnit: String,
    val expiryDate: String,
    val addedAt: String,
)

@Serializable
internal data class DoseV1(
    val medicationId: String?,
    val medicationName: String,
    val amountValue: String,
    val amountUnit: String,
    val scheduledAt: String,
    val plannedAt: String,
    val outcome: String?,
    val recordedAt: String?,
    val snoozedUntil: String?,
    val firstRemindedAt: String?,
    val lastRemindedAt: String?,
    val reminderCount: Int,
)

private object KindV1 {
    const val EVERY_N_DAYS = "EVERY_N_DAYS"
    const val ON_WEEKDAYS = "ON_WEEKDAYS"
    const val EVERY_N_HOURS = "EVERY_N_HOURS"
}

internal fun BackupSnapshot.toPayloadV1() = PayloadV1(
    medications = medications.map { it.toV1() },
    stockBatches = stockBatches.map {
        StockBatchV1(
            medicationId = it.medicationBackupId,
            remainingValue = it.remaining.toPlainString(),
            remainingUnit = it.unit.name,
            strengthPerUnit = it.strengthPerUnit.toPlainString(),
            expiryDate = it.expiryDate.toString(),
            addedAt = it.addedAt.toString(),
        )
    },
    doses = doses.map {
        DoseV1(
            medicationId = it.medicationBackupId,
            medicationName = it.medicationName,
            amountValue = it.amount.value.toPlainString(),
            amountUnit = it.amount.unit.name,
            scheduledAt = it.scheduledAt.toString(),
            plannedAt = it.plannedAt.toString(),
            outcome = it.intake?.outcome?.name,
            recordedAt = it.intake?.recordedAt?.toString(),
            snoozedUntil = it.snoozedUntil?.toString(),
            firstRemindedAt = it.firstRemindedAt?.toString(),
            lastRemindedAt = it.lastRemindedAt?.toString(),
            reminderCount = it.reminderCount,
        )
    },
)

/**
 * Back to the domain, through every constructor's own checks. A payload that decrypts but
 * describes something the app could never have stored throws here, and the caller treats that the
 * same as a damaged file.
 */
internal fun PayloadV1.toSnapshot() = BackupSnapshot(
    medications = medications.map { it.toDomain() },
    stockBatches = stockBatches.map {
        BackupStockBatch(
            medicationBackupId = it.medicationId,
            remaining = BigDecimal(it.remainingValue),
            unit = DoseUnit.valueOf(it.remainingUnit),
            strengthPerUnit = BigDecimal(it.strengthPerUnit),
            expiryDate = LocalDate.parse(it.expiryDate),
            addedAt = Instant.parse(it.addedAt),
        )
    },
    doses = doses.map {
        BackupDose(
            medicationBackupId = it.medicationId,
            medicationName = it.medicationName,
            amount = quantity(it.amountValue, it.amountUnit),
            scheduledAt = Instant.parse(it.scheduledAt),
            plannedAt = Instant.parse(it.plannedAt),
            intake = it.outcome?.let { outcome ->
                Intake(IntakeOutcome.valueOf(outcome), Instant.parse(requireNotNull(it.recordedAt) { "An outcome without a moment" }))
            },
            snoozedUntil = it.snoozedUntil?.let(Instant::parse),
            firstRemindedAt = it.firstRemindedAt?.let(Instant::parse),
            lastRemindedAt = it.lastRemindedAt?.let(Instant::parse),
            reminderCount = it.reminderCount,
        )
    },
)

private fun BackupMedication.toV1() = MedicationV1(
    id = backupId,
    name = name,
    defaultDoseValue = defaultDose.value.toPlainString(),
    defaultDoseUnit = defaultDose.unit.name,
    usedSince = usedSince.toString(),
    useUntil = useUntil?.toString(),
    prescribedBy = prescribedBy.name,
    isActive = isActive,
    lowStockAcknowledgement = lowStockAcknowledgement?.name,
    schedules = schedules.map { it.toV1() },
)

private fun MedicationV1.toDomain() = BackupMedication(
    backupId = id,
    name = name,
    defaultDose = quantity(defaultDoseValue, defaultDoseUnit),
    usedSince = LocalDate.parse(usedSince),
    useUntil = useUntil?.let(LocalDate::parse),
    prescribedBy = Prescriber.valueOf(prescribedBy),
    schedules = schedules.map { it.toDomain() },
    isActive = isActive,
    lowStockAcknowledgement = lowStockAcknowledgement?.let(LowStockAcknowledgement::valueOf),
)

private fun Schedule.toV1(): ScheduleV1 {
    val value = amount.value.toPlainString()
    val unit = amount.unit.name
    return when (this) {
        is Schedule.EveryNDays -> ScheduleV1(KindV1.EVERY_N_DAYS, value, unit, intervalDays = intervalDays, times = times.map(LocalTime::toString))
        is Schedule.OnWeekdays -> ScheduleV1(
            KindV1.ON_WEEKDAYS,
            value,
            unit,
            days = days.sorted().map { it.name },
            times = times.map(LocalTime::toString),
        )
        is Schedule.EveryNHours -> ScheduleV1(KindV1.EVERY_N_HOURS, value, unit, intervalHours = intervalHours, firstDoseAt = firstDoseAt.toString())
    }
}

private fun ScheduleV1.toDomain(): Schedule {
    val amount = quantity(amountValue, amountUnit)
    return when (kind) {
        KindV1.EVERY_N_DAYS -> Schedule.EveryNDays(amount, required(intervalDays), required(times).map(LocalTime::parse))
        KindV1.ON_WEEKDAYS -> Schedule.OnWeekdays(
            amount,
            required(days).map(DayOfWeek::valueOf).toSet(),
            required(times).map(LocalTime::parse),
        )
        KindV1.EVERY_N_HOURS -> Schedule.EveryNHours(amount, required(intervalHours), LocalTime.parse(required(firstDoseAt)))
        else -> throw IllegalArgumentException("Unknown schedule kind")
    }
}

private fun quantity(value: String, unit: String) = Quantity(BigDecimal(value), DoseUnit.valueOf(unit))

private fun <T : Any> required(value: T?): T = requireNotNull(value) { "A schedule is missing a field its kind needs" }
