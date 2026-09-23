package nl.hexmaster.pillsner.domain.repository

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.model.StockBatchId

/**
 * One batch's new remaining amount after first-expiry-first-out consumption, for
 * [StockBatchRepository.applyConsumption].
 */
data class BatchRemainingUpdate(val batchId: StockBatchId, val remaining: BigDecimal)

/**
 * Storage of a medicine's stock batches (`medicine-stock-tracking`).
 *
 * [applyConsumption] never deletes a batch: one consumed to zero is kept, not removed, so a medicine
 * that has ever had stock recorded never silently reverts to having none. [removeBatch] is the one
 * deliberate exception, gated behind the Stock section's confirmation dialog rather than anything
 * automatic — see `medicine-stock-tracking`'s "Removing a stock batch" requirement. Clearing a
 * medicine's low-stock acknowledgement when new stock arrives is the caller's job (`AddStockBatch`),
 * not this repository's: it touches the `medications` table, not this one.
 */
interface StockBatchRepository {

    /** Every batch of [medicationId], in no guaranteed order, re-emitting on any change. */
    fun observeBatches(medicationId: MedicationId): Flow<List<StockBatch>>

    /** Every batch of [medicationId], as one snapshot rather than a stream. */
    suspend fun batches(medicationId: MedicationId): List<StockBatch>

    /**
     * Adds a new batch of [amount], expiring on [expiryDate], added at [addedAt], to the medicine's
     * stock. [strengthPerUnit] is how much of the medicine's default dose unit one unit of [amount]
     * is worth (`medicine-stock-tracking`'s "Stock unit conversion" requirement) — exactly 1 when
     * [amount]'s unit already matches the medicine's default dose unit.
     */
    suspend fun addBatch(
        medicationId: MedicationId,
        amount: Quantity,
        strengthPerUnit: BigDecimal,
        expiryDate: LocalDate,
        addedAt: Instant,
    )

    /**
     * Applies first-expiry-first-out consumption's result: sets each named batch's remaining amount
     * to what is left of it, in one transaction. A batch not mentioned is untouched.
     */
    suspend fun applyConsumption(updates: List<BatchRemainingUpdate>)

    /**
     * Removes [batchId] from the medicine's stock (`medicine-stock-tracking`'s "Removing a stock
     * batch" requirement). The caller — the Stock section — is responsible for confirming this with
     * the user first; this operation itself does not ask again. Removing an id that does not exist
     * does nothing and raises no error.
     */
    suspend fun removeBatch(batchId: StockBatchId)
}
