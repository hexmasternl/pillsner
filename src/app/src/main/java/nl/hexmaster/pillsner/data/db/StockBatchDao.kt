package nl.hexmaster.pillsner.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

/** Reads and writes of stock batches (`medicine-stock-tracking`). */
@Dao
interface StockBatchDao {

    @Query(
        "SELECT * FROM stock_batches WHERE medication_id = :medicationId ORDER BY expiry_date ASC, added_at ASC",
    )
    fun observeBatches(medicationId: Long): Flow<List<StockBatchEntity>>

    @Query(
        "SELECT * FROM stock_batches WHERE medication_id = :medicationId ORDER BY expiry_date ASC, added_at ASC",
    )
    suspend fun batches(medicationId: Long): List<StockBatchEntity>

    @Insert
    suspend fun insert(batch: StockBatchEntity)

    @Query("UPDATE stock_batches SET remaining_value = :remaining WHERE id = :id")
    suspend fun updateRemaining(id: Long, remaining: BigDecimal)

    /** [medicine-stock-tracking]'s "Removing a stock batch" requirement. */
    @Query("DELETE FROM stock_batches WHERE id = :id")
    suspend fun delete(id: Long)

    /** One batch's new remaining amount, for [applyConsumption]. */
    data class RemainingUpdate(val id: Long, val remaining: BigDecimal)

    /**
     * [updateRemaining] for every row of [updates], in one transaction instead of one commit per
     * batch, mirroring `DoseDao.applyReminderOutcomes`'s batching pattern.
     */
    @Transaction
    suspend fun applyConsumption(updates: List<RemainingUpdate>) {
        updates.forEach { updateRemaining(it.id, it.remaining) }
    }
}
