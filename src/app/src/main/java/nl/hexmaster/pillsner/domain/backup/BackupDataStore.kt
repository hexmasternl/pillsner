package nl.hexmaster.pillsner.domain.backup

import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.StockBatch

/**
 * The whole stored dataset as it is right now, with the database's own ids still on it.
 *
 * Read in one transaction, so a dose answered while an export is reading cannot appear half
 * recorded.
 */
data class StoredDataset(
    val medications: List<Medication>,
    val stockBatches: List<StockBatch>,
    val doses: List<Dose>,
)

/**
 * The two whole-dataset operations a backup needs (`data-backup-restore` design).
 *
 * The same shape as [nl.hexmaster.pillsner.domain.reset.AppDataEraser]: a domain port with the
 * transaction living behind it, because no pair of repositories can promise "all of it or none of
 * it" across their separate DAOs.
 */
interface BackupDataStore {

    /** Every medication, stock batch and dose, read as one consistent unit. */
    suspend fun readAll(): StoredDataset

    /**
     * Replaces every medication, schedule, stock batch and dose with what [snapshot] describes, in
     * one transaction. Either all of it lands, or the database is left exactly as it was and the
     * failure is thrown.
     */
    suspend fun replaceAll(snapshot: BackupSnapshot)
}
