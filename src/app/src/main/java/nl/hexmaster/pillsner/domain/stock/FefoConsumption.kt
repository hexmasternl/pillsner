package nl.hexmaster.pillsner.domain.stock

import java.math.BigDecimal
import java.math.RoundingMode
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.model.isWholePill
import nl.hexmaster.pillsner.domain.repository.BatchRemainingUpdate

/**
 * The result of deducting a dose amount from a medicine's stock.
 *
 * @property consumed what the deduction took, in the medicine's default dose unit. For a
 *   whole-pill batch this is the full worth of every pill used, so it can be more than the dose: a
 *   40 mg dose drawn from a 500 mg tablet consumes 500 mg (`medicine-stock-tracking`'s "Whole-pill
 *   units" requirement).
 * @property shortfall how much of the dose, in the medicine's default dose unit, the batches could
 *   not cover because they ran out. Zero whenever the dose was covered in full.
 * @property firstDrawn the batch the dose was drawn from first, as it was before the deduction, or
 *   null when no batch had anything left to give. When the exact-fit preference applies, this is
 *   the exact-fit batch: the pill the user actually took. It is what the expiry-at-use warning
 *   classifies (`medicine-stock-tracking`'s "Expiry-at-use warning" requirement). Once a dose
 *   exhausts that batch, it no longer counts as the soonest-expiring batch with stock, so it cannot
 *   be recovered from the batches afterwards.
 */
data class StockConsumption(
    val updates: List<BatchRemainingUpdate>,
    val consumed: BigDecimal,
    val firstDrawn: StockBatch? = null,
    val shortfall: BigDecimal = BigDecimal.ZERO,
)

/**
 * Decimal places kept when dividing a dose amount by the strength of a batch that is *not* counted
 * in whole pills (a batch in grams against a dose in milligrams, say). Six places is far finer than
 * any real dose needs. The division rounds down, never up, so a dose that doesn't divide evenly can
 * leave a negligible remainder below this precision uncollected, but never deducts more than the
 * dose. A batch that covers what is owed settles the dose outright, so that remainder never spills
 * as a near-zero amount into a further batch. Whole-pill batches don't use this: they round up to
 * the next whole pill instead.
 */
private const val CONVERSION_SCALE = 6

/**
 * Deducts [doseAmount], expressed in the medicine's default dose unit, from [batches]
 * (`medicine-stock-tracking`'s "First-expiry-first-out consumption" requirement).
 *
 * Batches are drawn from in order of expiry date, soonest first, with a tie broken by
 * [StockBatch.addedAt]. There is one exception, the exact-fit preference. When the first batch in
 * that order is counted in whole pills and the dose isn't a whole number of its pills, the first
 * batch in the same order that can cover the whole dose without breaking a pill is used instead.
 * That way a 40 mg dose comes from the 40 mg tablets rather than from a 500 mg tablet that would be
 * mostly wasted. When no pill would be broken, expiry order always wins.
 *
 * Each batch's share of what is still owed is that amount divided by the batch's
 * [StockBatch.strengthPerUnit] (`medicine-stock-tracking`'s "Stock unit conversion" requirement).
 * For a whole-pill batch the share rounds **up** to the next whole pill, because a partly used pill
 * can't go back into stock. For any other batch it stays exact. A batch only gives up what it
 * actually has ([StockBatch.usableRemaining]), and none is ever taken below zero. A whole-pill
 * batch's update is written from its usable remaining amount, so a legacy fractional count heals
 * to a whole number here.
 *
 * If the batches hold less than [doseAmount] in total, every batch drawn from ends at zero and the
 * deduction floors there rather than going negative. [StockConsumption.shortfall] reports what
 * couldn't be covered.
 *
 * Pure: a batch not drawn from at all is simply absent from [StockConsumption.updates].
 */
fun consumeFefo(batches: List<StockBatch>, doseAmount: BigDecimal): StockConsumption {
    val candidates = batches
        .filter { it.usableRemaining > BigDecimal.ZERO }
        .sortedWith(compareBy({ it.expiryDate }, { it.addedAt }))
    val drawOrder = exactFitFor(candidates, doseAmount)?.let { listOf(it) } ?: candidates

    var owedInDoseUnits = doseAmount
    val updates = mutableListOf<BatchRemainingUpdate>()
    var consumedInDoseUnits = BigDecimal.ZERO

    for (batch in drawOrder) {
        if (owedInDoseUnits <= BigDecimal.ZERO) break
        val owedInBatchUnits = inBatchUnits(owedInDoseUnits, batch)
        val available = batch.usableRemaining
        val coversWhatIsOwed = available >= owedInBatchUnits
        val takenInBatchUnits = available.min(owedInBatchUnits)
        // A strength of exactly 1 needs no real conversion; skipping the multiplication keeps the
        // common case free of extra decimal places.
        val takenInDoseUnits = if (hasNoConversion(batch)) takenInBatchUnits else takenInBatchUnits * batch.strengthPerUnit
        updates += BatchRemainingUpdate(batch.id, available - takenInBatchUnits)
        consumedInDoseUnits += takenInDoseUnits
        // A batch that covers the dose settles it. That covers a whole pill worth more than was owed,
        // and whatever rounding down left behind in a continuous batch, which is below the
        // conversion precision and not worth drawing from a further batch.
        owedInDoseUnits = if (coversWhatIsOwed) BigDecimal.ZERO else owedInDoseUnits - takenInDoseUnits
    }

    return StockConsumption(
        updates = updates,
        consumed = consumedInDoseUnits,
        firstDrawn = drawOrder.firstOrNull().takeIf { updates.isNotEmpty() },
        shortfall = owedInDoseUnits.max(BigDecimal.ZERO),
    )
}

/**
 * The batch the exact-fit preference picks for [doseAmount], or null when the dose should be drawn
 * first-expiry-first-out. The preference only applies when the first of [candidates] (already in
 * expiry order) would break a pill.
 */
private fun exactFitFor(candidates: List<StockBatch>, doseAmount: BigDecimal): StockBatch? {
    val first = candidates.firstOrNull() ?: return null
    if (!breaksAPill(first, doseAmount)) return null
    return candidates.firstOrNull {
        !breaksAPill(it, doseAmount) && it.usableRemaining >= inBatchUnits(doseAmount, it)
    }
}

/** Whether taking [amountInDoseUnits] from [batch] would use only part of one of its pills. */
private fun breaksAPill(batch: StockBatch, amountInDoseUnits: BigDecimal): Boolean =
    batch.unit.isWholePill && amountInDoseUnits.remainder(batch.strengthPerUnit).signum() != 0

/**
 * [amountInDoseUnits] converted into [batch]'s own unit: rounded up to a whole pill for a
 * whole-pill batch, otherwise exact (to [CONVERSION_SCALE] places, rounded down).
 */
private fun inBatchUnits(amountInDoseUnits: BigDecimal, batch: StockBatch): BigDecimal = when {
    batch.unit.isWholePill -> amountInDoseUnits.divide(batch.strengthPerUnit, 0, RoundingMode.CEILING)
    hasNoConversion(batch) -> amountInDoseUnits
    // Stripped so a dose that divides evenly reads as "2", not "2.000000"; a fractional result
    // such as 1.25 keeps exactly the digits it needs.
    else -> amountInDoseUnits.divide(batch.strengthPerUnit, CONVERSION_SCALE, RoundingMode.DOWN).stripTrailingZeros()
}

private fun hasNoConversion(batch: StockBatch): Boolean = batch.strengthPerUnit.compareTo(BigDecimal.ONE) == 0
