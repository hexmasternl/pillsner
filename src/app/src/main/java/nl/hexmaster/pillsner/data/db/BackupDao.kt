package nl.hexmaster.pillsner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/**
 * The whole-dataset reads and writes a backup needs (`data-backup-restore`), and nothing else.
 *
 * Kept apart from the other DAOs on purpose. The deletes here are the second exception to
 * *Medications are never removed*, after the reset, and living in a DAO that is plainly about
 * backups keeps them out of reach of any later change looking for a way to remove one medicine.
 * Only [nl.hexmaster.pillsner.data.backup.RoomBackupDataStore] calls this, and always inside one
 * transaction.
 */
@Dao
interface BackupDao {

    @Transaction
    @Query("SELECT * FROM medications ORDER BY id ASC")
    suspend fun medications(): List<MedicationWithSchedules>

    @Query("SELECT * FROM stock_batches ORDER BY id ASC")
    suspend fun stockBatches(): List<StockBatchEntity>

    @Query("SELECT * FROM doses ORDER BY scheduled_at ASC, id ASC")
    suspend fun doses(): List<DoseEntity>

    @Query("DELETE FROM doses")
    suspend fun deleteDoses()

    @Query("DELETE FROM stock_batches")
    suspend fun deleteStockBatches()

    @Query("DELETE FROM schedules")
    suspend fun deleteSchedules()

    @Query("DELETE FROM medications")
    suspend fun deleteMedications()

    @Insert
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Insert
    suspend fun insertSchedules(schedules: List<ScheduleEntity>)

    @Insert
    suspend fun insertStockBatches(batches: List<StockBatchEntity>)

    /** Aborts on a conflict, so a backup that breaks the unique dose index fails the whole import. */
    @Insert
    suspend fun insertDoses(doses: List<DoseEntity>)
}
