package nl.hexmaster.pillsner.data.wear

import java.math.BigDecimal
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.ScheduleSummary
import nl.hexmaster.pillsner.domain.model.summarize
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.repository.StockBatchRepository
import nl.hexmaster.pillsner.shared.wear.SyncedMedicineDetails

/**
 * What the watch's read-only details screen says about the medicine behind a dose
 * (`wear-day-overview` design D3).
 *
 * Every line is written out here rather than on the watch, for the same reason the amounts are: the
 * phone is the only side that holds the units, the schedule vocabulary and their translations.
 * The formatting itself arrives as lambdas, so this stays a plain object a unit test can drive
 * without resources.
 */
class WearMedicineDetails(
    private val medicationRepository: MedicationRepository,
    private val stockBatchRepository: StockBatchRepository,
    private val amountText: (Quantity) -> String,
    private val stockText: (BigDecimal, DoseUnit) -> String,
    private val scheduleText: (ScheduleSummary, Quantity) -> String,
) {

    /**
     * The details of the medicine with [id], or null when it no longer exists — a dose outlives its
     * medication, and the watch then shows only what the dose itself carries.
     */
    suspend fun forMedication(id: MedicationId): SyncedMedicineDetails? {
        val medication = medicationRepository.get(id) ?: return null
        val batches = stockBatchRepository.batches(id)
        val scheduleLines = if (medication.schedules.isEmpty()) {
            listOf(scheduleText(ScheduleSummary.AsNeeded, medication.defaultDose))
        } else {
            medication.schedules.map { scheduleText(it.summarize(), it.amount) }
        }
        return SyncedMedicineDetails(
            defaultDoseText = amountText(medication.defaultDose),
            scheduleLines = scheduleLines,
            // A medicine that records no stock at all has no stock line, rather than a zero one.
            stockText = batches
                .takeIf { it.isNotEmpty() }
                ?.fold(BigDecimal.ZERO) { sum, batch -> sum + batch.remainingInDoseUnits }
                ?.let { stockText(it, medication.defaultDose.unit) },
        )
    }
}
