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
    suspend operator fun invoke(medicationId: MedicationId): StockWarning? {
        val medication = medicationRepository.get(medicationId) ?: return null
        val batches = stockBatchRepository.batches(medicationId)
        if (batches.isEmpty()) return null

        val state = stockState(batches, medication, LocalDate.now(clock), clock.zone, projectWeeklyUsage)
        val lowStock = state.isLow && medication.lowStockAcknowledgement == null
        if (!lowStock && state.nearestExpiry == BatchExpiryState.NONE) return null

        return StockWarning(
            medicationId = medicationId,
            medicationName = medication.name,
            lowStock = lowStock,
            expiryState = state.nearestExpiry,
        )
    }
}
