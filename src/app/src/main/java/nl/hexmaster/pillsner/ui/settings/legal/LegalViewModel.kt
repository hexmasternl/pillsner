package nl.hexmaster.pillsner.ui.settings.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.domain.legal.CurrentLegalDocuments
import nl.hexmaster.pillsner.domain.legal.IsLegalAccepted
import nl.hexmaster.pillsner.domain.legal.LegalDocuments
import nl.hexmaster.pillsner.domain.repository.LegalRepository

/**
 * What the Legal section says about acceptance, and what the add-medicine gate reads.
 *
 * Three states and no more, because there are only three things the user can be told: they have
 * never accepted, they accepted what is current, or they accepted something that has since changed.
 */
sealed interface LegalAcceptanceState {

    /** No record: the next medicine added asks for acceptance. */
    data object NeverAccepted : LegalAcceptanceState

    /** Both documents accepted at their current versions, on [on]. */
    data class Accepted(val on: LocalDate) : LegalAcceptanceState

    /** Accepted on [on], but at least one document has been revised since. */
    data class RevisedSince(val on: LocalDate) : LegalAcceptanceState
}

/**
 * The state of the legal documents (design D8): what Settings reports, and the one flag the
 * add-medicine gate asks for.
 *
 * @property acceptance what the Legal section's status line says.
 * @property accepted whether the current documents are accepted. Read at the moment the add button
 *   is tapped, so a revision that lands while the app is open is honoured straight away.
 */
data class LegalUiState(
    val acceptance: LegalAcceptanceState = LegalAcceptanceState.NeverAccepted,
    val accepted: Boolean = false,
)

/**
 * The legal documents' acceptance state, shared by Settings and the add-medicine gate (design D5).
 *
 * It holds no document text: the screens read [CurrentLegalDocuments] directly, because the
 * documents are constant for the life of the process and there is nothing to load.
 *
 * @param zone the time zone the acceptance moment is shown in; the device's own by default.
 */
class LegalViewModel(
    private val repository: LegalRepository,
    private val isLegalAccepted: IsLegalAccepted,
    private val documents: LegalDocuments = CurrentLegalDocuments,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    val uiState: StateFlow<LegalUiState> = repository.observeAcceptance()
        .map { acceptance ->
            val accepted = isLegalAccepted.covers(acceptance)
            val on = acceptance?.acceptedAt?.atZone(zone)?.toLocalDate()
            LegalUiState(
                acceptance = when {
                    on == null -> LegalAcceptanceState.NeverAccepted
                    accepted -> LegalAcceptanceState.Accepted(on)
                    else -> LegalAcceptanceState.RevisedSince(on)
                },
                accepted = accepted,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_TIMEOUT_MILLIS),
            initialValue = LegalUiState(),
        )

    /** Records that the user accepted both documents, at the versions this build ships. */
    fun accept() {
        viewModelScope.launch {
            repository.accept(
                disclaimerVersion = documents.disclaimer.version,
                termsVersion = documents.terms.version,
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
