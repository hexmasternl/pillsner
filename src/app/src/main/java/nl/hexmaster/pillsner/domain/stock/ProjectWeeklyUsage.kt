package nl.hexmaster.pillsner.domain.stock

import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.scheduling.DoseGenerator

/** The window a low-stock check projects usage over (`medicine-stock-tracking`). */
private const val PROJECTION_DAYS = 7L

/**
 * Projects how much of [Medication] its schedules call for over the 7 calendar days starting from a
 * given date, in a given zone, by summing what [DoseGenerator] would produce for that window
 * (`medicine-stock-tracking`'s "Weekly usage projection" requirement) — the same schedule-expansion
 * logic the rest of the app already relies on, rather than a second rate calculation.
 *
 * Only doses whose amount shares the medicine's default dose unit are counted, since stock is
 * tracked in that one unit; a schedule set up in a different unit is a pathological edge case this
 * projection cannot usefully add to a stock total, so it is left out rather than mixed in.
 */
class ProjectWeeklyUsage(private val doseGenerator: DoseGenerator = DoseGenerator()) {

    /**
     * The projected usage, or null for a medication with no schedules (as-needed) or one whose
     * window happens to produce nothing to count — both are exempt from the low-stock check.
     */
    fun forMedication(medication: Medication, from: LocalDate, zone: ZoneId): Quantity? {
        if (medication.schedules.isEmpty()) return null

        val window = from..from.plusDays(PROJECTION_DAYS - 1)
        val unit = medication.defaultDose.unit
        val total = doseGenerator.plan(medication, window, zone)
            .filter { it.amount.unit == unit }
            .fold(BigDecimal.ZERO) { sum, dose -> sum + dose.amount.value }

        return if (total > BigDecimal.ZERO) Quantity(total, unit) else null
    }
}
