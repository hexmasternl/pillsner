package nl.hexmaster.pillsner.domain.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import nl.hexmaster.pillsner.domain.model.MedicationId

/**
 * Which medicines have a stock warning waiting to be shown (`medicine-stock-tracking`'s "Combined
 * warning presentation" requirement).
 *
 * Holds a marker per medicine, never the warning's content: the content is evaluated fresh, against
 * current stock, whenever it is finally shown (see `EvaluateStockWarning`), so a take answered while
 * the app was backgrounded is never replayed as stale data, and any number of takes for the same
 * medicine collapse into one entry. The one fact kept alongside the marker is the expiry date of
 * the batch the latest take drew from first, because that batch may since have been used up and
 * can then no longer be found among the batches with stock left.
 */
interface StockWarningQueue {

    /**
     * Every medicine with a pending check, mapped to the expiry date of the batch its latest take
     * drew from first (null when that is not known), re-emitting on change.
     */
    fun observePending(): Flow<Map<MedicationId, LocalDate?>>

    /**
     * Flags [medicationId] for a check the next time pending warnings are evaluated and shown,
     * replacing whatever [drawnBatchExpiry] an earlier take left.
     */
    suspend fun enqueue(medicationId: MedicationId, drawnBatchExpiry: LocalDate?)

    /** Clears the flag for every id in [medicationIds]: the user has seen and answered its warning. */
    suspend fun clear(medicationIds: Set<MedicationId>)

    /** Clears every pending flag, for resetting the app (`app-reset`). */
    suspend fun clearAll()
}
