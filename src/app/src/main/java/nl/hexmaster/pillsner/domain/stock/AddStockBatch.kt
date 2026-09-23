package nl.hexmaster.pillsner.domain.stock

import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.isWholePill
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.repository.StockBatchRepository
import nl.hexmaster.pillsner.domain.repository.TransactionRunner

/**
 * Records a new stock batch for a medicine, from the Add stock form
 * (`medicine-stock-tracking`'s "Add stock form" requirement).
 *
 * [strengthPerUnit] is how much of the medicine's default dose unit one unit of [amount] is worth
 * (`medicine-stock-tracking`'s "Stock unit conversion" requirement) — required by the form only when
 * [amount]'s unit differs from the medicine's own; when it matches, this always records a strength
 * of exactly 1 regardless of what is passed, since a unit never needs converting against itself and
 * the domain layer should not trust a caller to have already enforced that.
 *
 * Always clears the medicine's low-stock acknowledgement, regardless of whether the new batch
 * actually restores sufficiency: a standing "I ordered new" suppression ends the moment new stock
 * arrives, exactly as the user was told it would. The batch and the cleared acknowledgement are
 * written in one [transactionRunner] transaction, so new stock is never recorded with the old
 * suppression still standing.
 */
class AddStockBatch(
    private val stockBatchRepository: StockBatchRepository,
    private val medicationRepository: MedicationRepository,
    private val transactionRunner: TransactionRunner,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    suspend operator fun invoke(
        medicationId: MedicationId,
        amount: Quantity,
        strengthPerUnit: BigDecimal,
        expiryDate: LocalDate,
    ) = transactionRunner.inTransaction {
        val medication = medicationRepository.get(medicationId)
        val effectiveStrength = if (medication != null && amount.unit == medication.defaultDose.unit) {
            BigDecimal.ONE
        } else {
            strengthPerUnit
        }
        // Checked here, not only on the form: this is the domain's own boundary, and a batch with a
        // non-positive strength breaks every conversion that later divides by it. A Quantity is
        // always greater than zero, but one counted in pills must also be whole
        // (`medicine-stock-tracking`'s "Whole-pill units" requirement).
        require(effectiveStrength > BigDecimal.ZERO) { "A stock batch's strength must be greater than zero" }
        require(!amount.unit.isWholePill || amount.value.stripTrailingZeros().scale() <= 0) {
            "A stock batch counted in tablets or capsules must hold a whole number of them"
        }
        stockBatchRepository.addBatch(medicationId, amount, effectiveStrength, expiryDate, clock.instant())
        medicationRepository.setLowStockAcknowledgement(medicationId, null)
    }
}
