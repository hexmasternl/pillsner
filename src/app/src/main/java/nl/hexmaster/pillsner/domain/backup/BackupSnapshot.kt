package nl.hexmaster.pillsner.domain.backup

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.Prescriber
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule

/**
 * Everything a backup carries: the user's whole dataset, detached from the database it came from
 * (`data-backup-restore`).
 *
 * Nothing here holds a Room row id. Those belong to one database instance and mean nothing in
 * another, so every medication gets an export-local [BackupMedication.backupId] instead, and stock
 * batches and doses point at their medication through it. The ids only have to be unique within
 * one snapshot; they are generated fresh on every export.
 *
 * What is deliberately absent is as much a part of the contract as what is present: the app lock,
 * the accepted legal documents and anything about a paired watch stay on the device they belong to.
 *
 * @throws IllegalArgumentException when two medications share a backup id, or a stock batch or dose
 *   points at a backup id no medication in this snapshot has. Such a snapshot cannot be restored
 *   without inventing or dropping data, so it is refused whole.
 */
data class BackupSnapshot(
    val medications: List<BackupMedication>,
    val stockBatches: List<BackupStockBatch>,
    val doses: List<BackupDose>,
) {
    init {
        val ids = medications.map { it.backupId }
        require(ids.size == ids.toSet().size) { "Two medications in a backup share an id" }
        val known = ids.toSet()
        require(stockBatches.all { it.medicationBackupId in known }) {
            "A stock batch in a backup points at a medication the backup does not have"
        }
        require(doses.all { it.medicationBackupId == null || it.medicationBackupId in known }) {
            "A dose in a backup points at a medication the backup does not have"
        }
    }

    companion object {
        val EMPTY = BackupSnapshot(emptyList(), emptyList(), emptyList())
    }
}

/**
 * One medication as a backup carries it: every field the user can see or set, plus [backupId].
 *
 * Validated the same way a stored medication is, so a file that decrypts but describes an
 * impossible medicine is refused rather than written into the database.
 */
data class BackupMedication(
    val backupId: String,
    val name: String,
    val defaultDose: Quantity,
    val usedSince: LocalDate,
    val useUntil: LocalDate?,
    val prescribedBy: Prescriber,
    val schedules: List<Schedule>,
    val isActive: Boolean,
    val lowStockAcknowledgement: LowStockAcknowledgement?,
) {
    init {
        require(backupId.isNotBlank()) { "A medication in a backup needs an id" }
        require(name.isNotBlank()) { "A medication needs a name" }
        require(useUntil == null || !useUntil.isBefore(usedSince)) {
            "A medication's use-until date cannot be before its used-since date"
        }
    }
}

/** One stock batch as a backup carries it (`medicine-stock-tracking`). */
data class BackupStockBatch(
    val medicationBackupId: String,
    val remaining: BigDecimal,
    val unit: DoseUnit,
    val strengthPerUnit: BigDecimal,
    val expiryDate: LocalDate,
    val addedAt: Instant,
) {
    init {
        require(remaining >= BigDecimal.ZERO) { "A stock batch's remaining amount cannot be negative" }
        require(strengthPerUnit > BigDecimal.ZERO) { "A stock batch's strength must be greater than zero" }
    }
}

/**
 * One dose as a backup carries it, answered or not, with all of its reminder bookkeeping.
 *
 * The bookkeeping travels too, not only the outcome: [firstRemindedAt] and [plannedAt] are what
 * tell a reminder the platform dropped from one that was never possible, and restoring a history
 * without them would make old doses read differently than they did before the export.
 *
 * @property medicationBackupId the [BackupMedication.backupId] this dose came from, or null when its
 *   medication was already gone at export time.
 */
data class BackupDose(
    val medicationBackupId: String?,
    val medicationName: String,
    val amount: Quantity,
    val scheduledAt: Instant,
    val plannedAt: Instant,
    val intake: Intake?,
    val snoozedUntil: Instant?,
    val firstRemindedAt: Instant?,
    val lastRemindedAt: Instant?,
    val reminderCount: Int,
) {
    init {
        require(reminderCount >= 0) { "A dose's reminder count cannot be negative" }
    }
}
