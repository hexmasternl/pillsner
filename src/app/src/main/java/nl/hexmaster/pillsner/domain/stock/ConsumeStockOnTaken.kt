package nl.hexmaster.pillsner.domain.stock

import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.repository.StockBatchRepository
import nl.hexmaster.pillsner.domain.repository.StockWarningQueue

/**
 * What one taken dose did to its medicine's stock, carried from the deduction to the warning check.
 *
 * @property firstDrawnExpiry the expiry date of the batch the dose was drawn from first, or null
 *   when nothing was left to draw from.
 */
data class TakenStockEffect(val medicationId: MedicationId, val firstDrawnExpiry: LocalDate?)

/**
 * What happens to a medicine's stock when one of its doses is recorded taken
 * (`medicine-stock-tracking`'s "First-expiry-first-out consumption" requirement).
 *
 * The one integration point every surface that can answer a dose taken shares: the dose detail
 * screen, the reminder notification, and a bridged wearable answer all funnel through
 * [nl.hexmaster.pillsner.domain.intake.AnswerDose], which calls this for every taken outcome. A
 * medicine with no stock batches, or whose dose amount does not share its stock's unit, is left
 * completely untouched.
 *
 * It comes in two halves because they belong on opposite sides of a commit: [deduct] is a
 * read-modify-write of the batches and runs inside the transaction that records the intake;
 * [flagWarning] writes to the warning queue, which is not in the database, so it runs only once
 * that transaction has committed.
 */
class ConsumeStockOnTaken(
    private val stockBatchRepository: StockBatchRepository,
    private val medicationRepository: MedicationRepository,
    private val stockWarningQueue: StockWarningQueue,
    private val evaluateStockWarning: EvaluateStockWarning,
) {
    /**
     * Deducts [amount] from the medicine's stock, first-expiry-first-out. Must run inside the same
     * [nl.hexmaster.pillsner.domain.repository.TransactionRunner] transaction as the intake write,
     * so the batches read here cannot change before the new amounts are written back.
     *
     * @return what the deduction drew from, or null when the medicine is not under stock tracking.
     */
    suspend fun deduct(medicationId: MedicationId?, amount: Quantity): TakenStockEffect? {
        if (medicationId == null) return null
        val medication = medicationRepository.get(medicationId) ?: return null
        // Stock is recorded in the medicine's own default dose unit (see AddStockBatch); a dose
        // whose schedule was somehow set up in a different unit cannot be deducted from it.
        if (amount.unit != medication.defaultDose.unit) return null

        val batches = stockBatchRepository.batches(medicationId)
        if (batches.isEmpty()) return null

        val consumption = consumeFefo(batches, amount.value)
        stockBatchRepository.applyConsumption(consumption.updates)
        return TakenStockEffect(medicationId, consumption.firstDrawn?.expiryDate)
    }

    /** Flags the medicine for a stock warning when [effect] left something worth saying. */
    suspend fun flagWarning(effect: TakenStockEffect) {
        if (evaluateStockWarning(effect.medicationId, effect.firstDrawnExpiry) != null) {
            stockWarningQueue.enqueue(effect.medicationId, effect.firstDrawnExpiry)
        }
    }

    /** [deduct] then [flagWarning], for a caller that has no intake write of its own to join. */
    suspend operator fun invoke(medicationId: MedicationId?, amount: Quantity) {
        deduct(medicationId, amount)?.let { flagWarning(it) }
    }
}
