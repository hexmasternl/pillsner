package nl.hexmaster.pillsner.domain.repository

import kotlinx.coroutines.flow.Flow
import nl.hexmaster.pillsner.domain.model.MedicationId

/**
 * Which medicines have a stock warning waiting to be shown (`medicine-stock-tracking`'s "Combined
 * warning presentation" requirement).
 *
 * Only ever holds a marker per medicine, never the warning's content: the content is evaluated
 * fresh, against current stock, whenever it is finally shown (see `EvaluateStockWarning`), so a take
 * answered while the app was backgrounded is never replayed as stale data, and any number of takes
 * for the same medicine collapse into the one flag a `Set` already gives for free.
 */
interface StockWarningQueue {

    /** Every medicine with a pending check, re-emitting on change. */
    fun observePending(): Flow<Set<MedicationId>>

    /** Flags [medicationId] for a check the next time pending warnings are evaluated and shown. */
    suspend fun enqueue(medicationId: MedicationId)

    /** Clears the flag for every id in [medicationIds]: the user has seen and answered its warning. */
    suspend fun clear(medicationIds: Set<MedicationId>)
}
