package nl.hexmaster.pillsner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.domain.model.LowStockAcknowledgement
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.StockWarning
import nl.hexmaster.pillsner.domain.repository.MedicationRepository
import nl.hexmaster.pillsner.domain.repository.StockWarningQueue
import nl.hexmaster.pillsner.domain.stock.EvaluateStockWarning

/**
 * The stock warning waiting to be shown, for the whole app rather than one screen
 * (`medicine-stock-tracking`'s "Combined warning presentation" requirement).
 *
 * Held above navigation, so a take answered from the notification while the user is on any
 * destination — Medicines, Settings, a medicine's history — shows its warning there and then, not
 * only once they return to Home.
 *
 * @param evaluateStockWarning turns a flagged medicine into the warning to show, evaluated fresh.
 * @param medicationRepository only used to record "I ordered new"; reads go through [evaluateStockWarning].
 */
class StockWarningViewModel(
    private val stockWarningQueue: StockWarningQueue,
    private val evaluateStockWarning: EvaluateStockWarning,
    private val medicationRepository: MedicationRepository,
) : ViewModel() {

    /**
     * The next stock warning to show, or null when none is pending. Re-evaluated every time the
     * pending set changes, so a warning flagged while the app was backgrounded is never replayed as
     * stale data.
     */
    val warning: StateFlow<StockWarning?> = stockWarningQueue.observePending()
        .map { pending -> firstStockWarning(pending) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** The first pending medicine that still warrants a warning, evaluated one at a time. */
    private suspend fun firstStockWarning(pending: Map<MedicationId, LocalDate?>): StockWarning? {
        for ((medicationId, drawnBatchExpiry) in pending) {
            evaluateStockWarning(medicationId, drawnBatchExpiry)?.let { return it }
        }
        return null
    }

    /** The user tapped "OK": the warning is dismissed, but left free to return on the next take. */
    fun onAcknowledged(medicationId: MedicationId) {
        viewModelScope.launch { stockWarningQueue.clear(setOf(medicationId)) }
    }

    /** The user tapped "I ordered new": suppressed until a new stock batch is added. */
    fun onOrderedNew(medicationId: MedicationId) {
        viewModelScope.launch {
            medicationRepository.setLowStockAcknowledgement(medicationId, LowStockAcknowledgement.ACKNOWLEDGED_ORDERED)
            stockWarningQueue.clear(setOf(medicationId))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
