package nl.hexmaster.pillsner.domain.backup

import java.util.UUID
import nl.hexmaster.pillsner.domain.model.MedicationId

/**
 * Reads the whole dataset into a [BackupSnapshot] (`data-backup-restore`).
 *
 * The one thing this adds over a plain read is identity: each medication's Room id is swapped for
 * a fresh export-local id, and every stock batch and dose is pointed at that instead. Nothing is
 * written, and nothing outside the database is read.
 *
 * @param newBackupId makes one export-local id; a random UUID unless a test needs them predictable.
 */
class ExportBackup(
    private val store: BackupDataStore,
    private val newBackupId: () -> String = { UUID.randomUUID().toString() },
) {

    suspend operator fun invoke(): BackupSnapshot {
        val stored = store.readAll()
        val backupIds: Map<MedicationId, String> = stored.medications.associate { it.id to newBackupId() }

        return BackupSnapshot(
            medications = stored.medications.map { medication ->
                BackupMedication(
                    backupId = backupIds.getValue(medication.id),
                    name = medication.name,
                    defaultDose = medication.defaultDose,
                    usedSince = medication.usedSince,
                    useUntil = medication.useUntil,
                    prescribedBy = medication.prescribedBy,
                    schedules = medication.schedules,
                    isActive = medication.isActive,
                    lowStockAcknowledgement = medication.lowStockAcknowledgement,
                )
            },
            stockBatches = stored.stockBatches.map { batch ->
                BackupStockBatch(
                    medicationBackupId = backupIds.getValue(batch.medicationId),
                    remaining = batch.remaining,
                    unit = batch.unit,
                    strengthPerUnit = batch.strengthPerUnit,
                    expiryDate = batch.expiryDate,
                    addedAt = batch.addedAt,
                )
            },
            doses = stored.doses.map { dose ->
                BackupDose(
                    // A dose whose medicine is already gone keeps its null: it is still the user's
                    // record, and it travels as one.
                    medicationBackupId = dose.medicationId?.let(backupIds::getValue),
                    medicationName = dose.medicationName,
                    amount = dose.amount,
                    scheduledAt = dose.scheduledAt,
                    plannedAt = dose.plannedAt,
                    intake = dose.intake,
                    snoozedUntil = dose.snoozedUntil,
                    firstRemindedAt = dose.firstRemindedAt,
                    lastRemindedAt = dose.lastRemindedAt,
                    reminderCount = dose.reminderCount,
                )
            },
        )
    }
}
