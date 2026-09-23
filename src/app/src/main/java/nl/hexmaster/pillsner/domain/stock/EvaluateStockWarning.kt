package nl.hexmaster.pillsner.domain.stock

import java.time.Clock
import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.BatchExpiryState
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.StockWarning
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.repository.StockBatchRepository

/**
 * Turns a medicine id into the stock warning to show, evaluated against its stock and schedules
 * right now — never against whatever triggered the check — so a warning shown later always reflects
 * current state (`medicine-stock-tracking`'s "Combined warning presentation" requirement).
 *
 * Returns null when nothing warrants a warning: the medicine has no stock batches, could not be
 * read, stock is sufficient and nothing is near expiry, or the only reason left was low stock and
 * the user has already suppressed it with "I ordered new".
 */
class EvaluateStockWarning(
    private val medicationRepository: MedicationRepository,
    private val stockBatchRepository: StockBatchRepository,
    private val projectWeeklyUsage: ProjectWeeklyUsage = ProjectWeeklyUsage(),
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    /**
     * @param drawnBatchExpiry the expiry date of the batch the triggering take drew from first
     *   (`medicine-stock-tracking`'s "Expiry-at-use warning" requirement). That batch may be empty
     *   by now, so it is classified directly rather than looked up among the batches with stock
     *   left; the classification itself is still made against today. Null falls back to the
     *   soonest-expiring batch that still has stock.
     */
    suspend operator fun invoke(medicationId: MedicationId, drawnBatchExpiry: LocalDate? = null): StockWarning? {
        val medication = medicationRepository.get(medicationId) ?: return null
        val batches = stockBatchRepository.batches(medicationId)
        if (batches.isEmpty()) return null

        val today = LocalDate.now(clock)
        val state = stockState(batches, medication, today, clock.zone, projectWeeklyUsage)
        val lowStock = state.isLow && medication.lowStockAcknowledgement == null
        val expiryState = drawnBatchExpiry?.let { batchExpiryState(it, today) } ?: state.nearestExpiry
        if (!lowStock && expiryState == BatchExpiryState.NONE) return null

        return StockWarning(
            medicationId = medicationId,
            medicationName = medication.name,
            lowStock = lowStock,
            expiryState = expiryState,
        )
    }
}
