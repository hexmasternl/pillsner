package nl.hexmaster.pillsner.ui.settings.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.domain.reset.EraseAllData

/**
 * The gate in front of the one irreversible thing this app can do (design D8).
 *
 * Every guard is here rather than in the dialog, so the dialog stays a pure function of the state
 * it is given and a configuration change cannot walk around any of them. Cancelling, dismissing and
 * tapping outside are one path, and all three erase nothing.
 */
class ResetViewModel(
    private val eraseAllData: EraseAllData,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetUiState())
    val uiState: StateFlow<ResetUiState> = _uiState.asStateFlow()

    // A channel, not a shared flow: the snackbar must arrive once, and must not come back on a
    // configuration change as though the user had erased everything twice.
    private val _effects = Channel<ResetEffect>(Channel.BUFFERED)
    val effects: Flow<ResetEffect> = _effects.receiveAsFlow()

    /** Opens the confirmation. It performs nothing by itself; that is the point of the section. */
    fun onResetTapped() {
        _uiState.update { it.copy(dialogVisible = true, confirmationAccepted = false) }
    }

    /** Cancel, system back and a tap outside all land here, and all three erase nothing. */
    fun onDismiss() {
        _uiState.update { it.copy(dialogVisible = false, confirmationAccepted = false) }
    }

    fun onConfirmationToggled(accepted: Boolean) {
        _uiState.update { it.copy(confirmationAccepted = accepted) }
    }

    /**
     * Erases everything — but only when the box is ticked and nothing is already running.
     *
     * The guard is what makes a double tap, or a call from a state restored around the dialog,
     * erase once or not at all.
     */
    fun onConfirmed() {
        if (!_uiState.value.canConfirm) return
        _uiState.update { it.copy(inProgress = true) }

        viewModelScope.launch {
            try {
                eraseAllData()
                _effects.send(ResetEffect.Erased)
            } finally {
                // The dialog closes and the tick goes, so reopening always starts from unticked.
                _uiState.value = ResetUiState()
            }
        }
    }
}
