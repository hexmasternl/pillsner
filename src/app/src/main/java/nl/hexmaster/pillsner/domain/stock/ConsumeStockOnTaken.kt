package nl.hexmaster.pillsner.domain.stock

import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.repository.StockBatchRepository
import nl.hexmaster.pillsner.domain.repository.StockWarningQueue

/**
 * What happens to a medicine's stock when one of its doses is recorded taken
 * (`medicine-stock-tracking`'s "First-expiry-first-out consumption" requirement).
 *
 * The one integration point every surface that can answer a dose taken shares: the dose detail
 * screen, the reminder notification, and a bridged wearable answer all funnel through
 * [nl.hexmaster.pillsner.domain.intake.AnswerDose], which calls this for every taken outcome. A
 * medicine with no stock batches, or whose dose amount does not share its stock's unit, is left
 * completely untouched.
 */
class ConsumeStockOnTaken(
    private val stockBatchRepository: StockBatchRepository,
    private val medicationRepository: MedicationRepository,
    private val stockWarningQueue: StockWarningQueue,
    private val evaluateStockWarning: EvaluateStockWarning,
) {
    suspend operator fun invoke(medicationId: MedicationId?, amount: Quantity) {
        if (medicationId == null) return
        val medication = medicationRepository.get(medicationId) ?: return
        // Stock is recorded in the medicine's own default dose unit (see AddStockBatch); a dose
        // whose schedule was somehow set up in a different unit cannot be deducted from it.
        if (amount.unit != medication.defaultDose.unit) return

        val batches = stockBatchRepository.batches(medicationId)
        if (batches.isEmpty()) return

        val consumption = consumeFefo(batches, amount.value)
        stockBatchRepository.applyConsumption(consumption.updates)

        if (evaluateStockWarning(medicationId) != null) {
            stockWarningQueue.enqueue(medicationId)
        }
    }
}
