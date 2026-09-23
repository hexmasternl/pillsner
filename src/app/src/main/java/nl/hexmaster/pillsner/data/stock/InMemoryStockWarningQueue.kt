package nl.hexmaster.pillsner.data.stock

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.repository.StockWarningQueue

/** A [StockWarningQueue] held in memory, for tests and previews. */
class InMemoryStockWarningQueue(initial: Map<MedicationId, LocalDate?> = emptyMap()) : StockWarningQueue {

    private val pending = MutableStateFlow(initial)

    override fun observePending(): Flow<Map<MedicationId, LocalDate?>> = pending.asStateFlow()

    override suspend fun enqueue(medicationId: MedicationId, drawnBatchExpiry: LocalDate?) {
        pending.update { it + (medicationId to drawnBatchExpiry) }
    }

    override suspend fun clear(medicationIds: Set<MedicationId>) {
        pending.update { it - medicationIds }
    }
}
