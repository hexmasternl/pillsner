package nl.hexmaster.pillsner.ui.settings.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import nl.hexmaster.pillsner.data.reminders.ReminderDeliveryLog

/** The delivery log for the diagnostics screen, newest first, re-read whenever it changes. */
class ReminderDiagnosticsViewModel(
    private val log: ReminderDeliveryLog,
) : ViewModel() {

    val entries: StateFlow<List<ReminderDeliveryLog.Entry>> = log.observe()
        .map { it.asReversed() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    /** The whole log as text, oldest first, for the clipboard. */
    suspend fun asText(): String = log.asText()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
