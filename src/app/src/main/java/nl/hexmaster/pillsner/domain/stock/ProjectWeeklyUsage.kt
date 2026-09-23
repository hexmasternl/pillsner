package nl.hexmaster.pillsner.domain.stock

import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.StockBatch
import nl.hexmaster.pillsner.domain.scheduling.DoseGenerator

/** The window a low-stock check projects usage over (`medicine-stock-tracking`). */
private const val PROJECTION_DAYS = 7L

/**
 * Projects how much of [Medication] its schedules call for over the 7 calendar days starting from a
 * given date, in a given zone (`medicine-stock-tracking`'s "Weekly usage projection" requirement).
 * It uses what [DoseGenerator] would produce for that window, the same schedule-expansion logic the
 * rest of the app already relies on, rather than a second rate calculation.
 *
 * Only doses whose amount shares the medicine's default dose unit are counted, since stock is
 * tracked in that one unit. A schedule set up in a different unit is a pathological edge case this
 * projection can't usefully add to a stock total, so it is left out rather than mixed in.
 */
class ProjectWeeklyUsage(private val doseGenerator: DoseGenerator = DoseGenerator()) {

    /**
     * The projected usage, or null for a medication with no schedules (as-needed) or one whose
     * window happens to produce nothing to count. Both are exempt from the low-stock check.
     *
     * With [batches], each dose is run in order through [consumeFefo] against an in-memory copy of
     * them. So a 40 mg dose drawn from 500 mg tablets counts as the whole 500 mg tablet it uses, and
     * any amount the copy can no longer cover counts at face value. The copy is never written back.
     * Without batches, the projection is the plain sum of the dose amounts.
     */
    fun forMedication(
        medication: Medication,
        from: LocalDate,
        zone: ZoneId,
        batches: List<StockBatch> = emptyList(),
    ): Quantity? {
        if (medication.schedules.isEmpty()) return null

        val window = from..from.plusDays(PROJECTION_DAYS - 1)
        val unit = medication.defaultDose.unit
        val amounts = doseGenerator.plan(medication, window, zone)
            .filter { it.amount.unit == unit }
            .map { it.amount.value }
        val total = if (batches.isEmpty()) {
            amounts.fold(BigDecimal.ZERO, BigDecimal::add)
        } else {
            simulate(amounts, batches)
        }

        return if (total > BigDecimal.ZERO) Quantity(total, unit) else null
    }

    /** The total [amounts] would take from [batches] if each were deducted in turn. */
    private fun simulate(amounts: List<BigDecimal>, batches: List<StockBatch>): BigDecimal {
        var remaining = batches
        var total = BigDecimal.ZERO
        for (amount in amounts) {
            val consumption = consumeFefo(remaining, amount)
            total += consumption.consumed + consumption.shortfall
            val updated = consumption.updates.associate { it.batchId to it.remaining }
            remaining = remaining.map { batch -> updated[batch.id]?.let { batch.copy(remaining = it) } ?: batch }
        }
        return total
    }
}
