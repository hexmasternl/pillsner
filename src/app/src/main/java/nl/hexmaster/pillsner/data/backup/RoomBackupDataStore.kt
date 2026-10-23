package nl.hexmaster.pillsner.data.backup

import androidx.room.withTransaction
import nl.hexmaster.pillsner.data.db.DoseEntity
import nl.hexmaster.pillsner.data.db.MedicationEntity
import nl.hexmaster.pillsner.data.db.PillsnerDatabase
import nl.hexmaster.pillsner.data.db.StockBatchEntity
import nl.hexmaster.pillsner.data.db.toDomain
import nl.hexmaster.pillsner.data.db.toEntity
import nl.hexmaster.pillsner.data.stock.toDomain
import nl.hexmaster.pillsner.data.toDomain
import nl.hexmaster.pillsner.domain.backup.BackupDataStore
import nl.hexmaster.pillsner.domain.backup.BackupMedication
import nl.hexmaster.pillsner.domain.backup.BackupSnapshot
import nl.hexmaster.pillsner.domain.backup.StoredDataset

/**
 * The wired [BackupDataStore]: one Room transaction per operation (`data-backup-restore` design).
 *
 * The replace deletes table by table rather than calling `clearAllTables()`, because that runs its
 * own transaction and cannot be part of this one. Children go before parents, so no foreign key
 * action fires halfway through. Unlike a reset, the auto-increment counters are not set back: every
 * imported row gets an id the old dataset never used, so a notification or armed alarm left over
 * from before the import wakes to a missing row, which is already a no-op, rather than to an
 * unrelated imported dose that happens to reuse its id.
 */
class RoomBackupDataStore(private val database: PillsnerDatabase) : BackupDataStore {

    private val dao = database.backupDao()

    override suspend fun readAll(): StoredDataset = database.withTransaction {
        StoredDataset(
            medications = dao.medications().map { it.toDomain() },
            stockBatches = dao.stockBatches().map { it.toDomain() },
            doses = dao.doses().map { it.toDomain() },
        )
    }

    override suspend fun replaceAll(snapshot: BackupSnapshot) = database.withTransaction {
        dao.deleteDoses()
        dao.deleteStockBatches()
        dao.deleteSchedules()
        dao.deleteMedications()

        // Medications first, in file order, remembering the id Room gives each one: that map is
        // the only way a stock batch or dose finds its medicine again.
        val roomIds = snapshot.medications.associate { medication ->
            val id = dao.insertMedication(medication.toEntity())
            dao.insertSchedules(medication.schedules.mapIndexed { position, schedule -> schedule.toEntity(id, position) })
            medication.backupId to id
        }

        dao.insertStockBatches(
            snapshot.stockBatches.map { batch ->
                StockBatchEntity(
                    medicationId = roomIds.getValue(batch.medicationBackupId),
                    remainingValue = batch.remaining,
                    remainingUnit = batch.unit.name,
                    strengthPerUnit = batch.strengthPerUnit,
                    expiryDate = batch.expiryDate,
                    addedAt = batch.addedAt,
                )
            },
        )

        dao.insertDoses(
            snapshot.doses.map { dose ->
                DoseEntity(
                    medicationId = dose.medicationBackupId?.let(roomIds::getValue),
                    medicationName = dose.medicationName,
                    amountValue = dose.amount.value,
                    amountUnit = dose.amount.unit.name,
                    scheduledAt = dose.scheduledAt,
                    plannedAt = dose.plannedAt,
                    outcome = dose.intake?.outcome?.name,
                    recordedAt = dose.intake?.recordedAt,
                    snoozedUntil = dose.snoozedUntil,
                    firstRemindedAt = dose.firstRemindedAt,
                    lastRemindedAt = dose.lastRemindedAt,
                    reminderCount = dose.reminderCount,
                )
            },
        )
    }

    private fun BackupMedication.toEntity() = MedicationEntity(
        name = name,
        defaultDoseValue = defaultDose.value,
        defaultDoseUnit = defaultDose.unit.name,
        usedSince = usedSince,
        useUntil = useUntil,
        prescribedBy = prescribedBy.name,
        isActive = isActive,
        lowStockAcknowledgement = lowStockAcknowledgement?.name,
    )
}
