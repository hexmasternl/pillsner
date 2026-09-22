package nl.hexmaster.pillsner.domain.stock

import java.math.BigDecimal
import java.math.RoundingMode
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.repository.BatchRemainingUpdate

/** The result of deducting a dose amount from a medicine's stock, first-expiry-first-out. */
data class StockConsumption(
    val updates: List<BatchRemainingUpdate>,
    val consumed: BigDecimal,
)

/**
 * Decimal places kept when dividing a dose amount by a batch's strength to find how much of that
 * batch's own unit it is worth. Six places is far finer than any real tablet strength or dose
 * needs; a dose that does not divide evenly by a batch's strength (a 25 mg dose against 20 mg
 * tablets, say) can leave a negligible remainder below this precision uncollected rather than
 * spilling a near-zero amount into a further batch (`medicine-stock-tracking` design, "A dose
 * amount that does not divide evenly by a batch's strength").
 */
private const val CONVERSION_SCALE = 6

/**
 * Deducts [doseAmount] — expressed in the medicine's default dose unit — from [batches], always
 * drawing from the batch expiring soonest first, then the next soonest, breaking a tie by
 * [StockBatch.addedAt] (`medicine-stock-tracking`'s "First-expiry-first-out consumption"
 * requirement).
 *
 * Each batch's own share of the amount still owed is found by dividing that amount by the batch's
 * [StockBatch.strengthPerUnit] (`medicine-stock-tracking`'s "Stock unit conversion" requirement):
 * dividing a 40 mg dose still owed by a 20 mg-per-tablet batch asks for 2 tablets. What the batch
 * actually gives up — floored at what it holds — is converted back to the medicine's default dose
 * unit before moving to the next batch. A batch whose unit already matches the dose unit has a
 * strength of exactly 1, so this conversion is a no-op for it: the common case is plain
 * subtraction in one shared unit. A batch already at zero is skipped, and none is ever taken below
 * zero.
 *
 * If the batches hold less than [doseAmount] in total, every batch ends at zero and the deduction
 * floors there rather than going negative — [StockConsumption.consumed] reports what was actually
 * taken, in the medicine's default dose unit, which can be less than [doseAmount].
 *
 * Pure: a batch not drawn from at all is simply absent from [StockConsumption.updates].
 */
fun consumeFefo(batches: List<StockBatch>, doseAmount: BigDecimal): StockConsumption {
    var owedInDoseUnits = doseAmount
    val updates = mutableListOf<BatchRemainingUpdate>()
    var consumedInDoseUnits = BigDecimal.ZERO

    batches
        .filter { it.remaining > BigDecimal.ZERO }
        .sortedWith(compareBy({ it.expiryDate }, { it.addedAt }))
        .forEach { batch ->
            if (owedInDoseUnits <= BigDecimal.ZERO) return@forEach
            // A strength of exactly 1 needs no real conversion — skip the division entirely so the
            // common case (unit matches the dose) stays plain subtraction with no extra decimal
            // places introduced, rather than a division artifact like "18.000000" for "18".
            val noConversionNeeded = batch.strengthPerUnit.compareTo(BigDecimal.ONE) == 0
            val owedInBatchUnits = if (noConversionNeeded) {
                owedInDoseUnits
            } else {
                // Stripped so a dose that happens to divide evenly (the common case: tablet
                // strengths are chosen to divide common doses cleanly) reads as "2", not
                // "2.000000" — a fractional result like 1.25 keeps exactly the digits it needs.
                owedInDoseUnits.divide(batch.strengthPerUnit, CONVERSION_SCALE, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
            }
            val takenInBatchUnits = batch.remaining.min(owedInBatchUnits)
            val takenInDoseUnits = if (noConversionNeeded) takenInBatchUnits else takenInBatchUnits * batch.strengthPerUnit
            updates += BatchRemainingUpdate(batch.id, batch.remaining - takenInBatchUnits)
            consumedInDoseUnits += takenInDoseUnits
            owedInDoseUnits -= takenInDoseUnits
        }

    return StockConsumption(updates, consumedInDoseUnits)
}
