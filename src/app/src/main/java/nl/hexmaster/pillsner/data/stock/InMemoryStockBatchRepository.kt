package nl.hexmaster.pillsner.data.stock

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.model.StockBatchId
import nl.hexmaster.pillsner.domain.repository.BatchRemainingUpdate
import nl.hexmaster.pillsner.domain.repository.StockBatchRepository

/**
 * A [StockBatchRepository] held in memory. The app itself is wired with the Room-backed one; this
 * is what tests and Compose previews use, and it behaves the same from the outside.
 */
class InMemoryStockBatchRepository(
    initial: List<StockBatch> = emptyList(),
) : StockBatchRepository {

    private val batchesFlow = MutableStateFlow(initial)
    private val nextId = AtomicLong(initial.maxOfOrNull { it.id.value }?.plus(1) ?: 1L)

    override fun observeBatches(medicationId: MedicationId): Flow<List<StockBatch>> =
        batchesFlow.map { all -> all.filter { it.medicationId == medicationId } }

    override suspend fun batches(medicationId: MedicationId): List<StockBatch> =
        batchesFlow.value.filter { it.medicationId == medicationId }

    override suspend fun addBatch(
        medicationId: MedicationId,
        amount: Quantity,
        strengthPerUnit: BigDecimal,
        expiryDate: LocalDate,
        addedAt: Instant,
    ) {
        batchesFlow.update {
            it + StockBatch(
                id = StockBatchId(nextId.getAndIncrement()),
                medicationId = medicationId,
                remaining = amount.value,
                unit = amount.unit,
                strengthPerUnit = strengthPerUnit,
                expiryDate = expiryDate,
                addedAt = addedAt,
            )
        }
    }

    override suspend fun applyConsumption(updates: List<BatchRemainingUpdate>) {
        val byId = updates.associateBy { it.batchId }
        batchesFlow.update { all ->
            all.map { batch -> byId[batch.id]?.let { batch.copy(remaining = it.remaining) } ?: batch }
        }
    }

    override suspend fun removeBatch(batchId: StockBatchId) {
        batchesFlow.update { all -> all.filterNot { it.id == batchId } }
    }
}
