package nl.hexmaster.pillsner.ui.medicines.history

import nl.hexmaster.pillsner.domain.model.TimeDeviationHistory
import nl.hexmaster.pillsner.domain.model.UsageHistory
import nl.hexmaster.pillsner.domain.model.UsagePeriod

/**
 * What the usage history screen shows (app-medicine-usage-history design D9). Read-only: there is
 * no event here but the choice of period, because nothing on the screen writes.
 *
 * @property medicineName the name of the medicine whose record this is, empty while it loads.
 * @property history the counts and buckets for [period], or null while the first read is running.
 * @property timeDeviation the timing accuracy record for [period], or null while the first read is
 *   running; its own [TimeDeviationHistory.averageMinutes] being null means no dose was taken in
 *   the period, in which case its chart is omitted.
 */
data class MedicineHistoryUiState(
    val medicineName: String = "",
    val period: UsagePeriod = UsagePeriod.WEEK,
    val history: UsageHistory? = null,
    val timeDeviation: TimeDeviationHistory? = null,
    val isLoading: Boolean = true,
)

/** The one thing that can happen to this screen beyond showing a record. */
sealed interface MedicineHistoryEffect {

    /** The medicine could not be read; the screen closes and says so. */
    data object OpenFailed : MedicineHistoryEffect
}
