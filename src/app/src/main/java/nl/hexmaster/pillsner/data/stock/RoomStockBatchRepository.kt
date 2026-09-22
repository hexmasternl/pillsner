package nl.hexmaster.pillsner.data.stock

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.pillsner.data.db.StockBatchDao
import nl.hexmaster.pillsner.data.db.StockBatchEntity
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.model.StockBatchId
import nl.hexmaster.pillsner.domain.repository.BatchRemainingUpdate
import nl.hexmaster.pillsner.domain.repository.StockBatchRepository

/** The wired [StockBatchRepository]: every stock batch, kept on the device by Room. */
class RoomStockBatchRepository(
    private val dao: StockBatchDao,
) : StockBatchRepository {

    override fun observeBatches(medicationId: MedicationId): Flow<List<StockBatch>> =
        dao.observeBatches(medicationId.value).map { rows -> rows.map { it.toDomain() } }

    override suspend fun batches(medicationId: MedicationId): List<StockBatch> =
        dao.batches(medicationId.value).map { it.toDomain() }

    override suspend fun addBatch(
        medicationId: MedicationId,
        amount: Quantity,
        strengthPerUnit: BigDecimal,
        expiryDate: LocalDate,
        addedAt: Instant,
    ) {
        dao.insert(
            StockBatchEntity(
                medicationId = medicationId.value,
                remainingValue = amount.value,
                remainingUnit = amount.unit.name,
                strengthPerUnit = strengthPerUnit,
                expiryDate = expiryDate,
                addedAt = addedAt,
            ),
        )
    }

    override suspend fun applyConsumption(updates: List<BatchRemainingUpdate>) {
        dao.applyConsumption(updates.map { StockBatchDao.RemainingUpdate(it.batchId.value, it.remaining) })
    }

    override suspend fun removeBatch(batchId: StockBatchId) {
        dao.delete(batchId.value)
    }
}

private fun StockBatchEntity.toDomain(): StockBatch = StockBatch(
    id = StockBatchId(id),
    medicationId = MedicationId(medicationId),
    remaining = remainingValue,
    unit = DoseUnit.valueOf(remainingUnit),
    strengthPerUnit = strengthPerUnit,
    expiryDate = expiryDate,
    addedAt = addedAt,
)
