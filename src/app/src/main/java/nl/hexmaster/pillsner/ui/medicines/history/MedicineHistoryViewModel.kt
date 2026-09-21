package nl.hexmaster.pillsner.ui.medicines.history

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.domain.history.SummariseUsageHistory
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.UsagePeriod
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.repository.MedicationRepository

/**
 * One medicine's record over a chosen period (app-medicine-usage-history design D9).
 *
 * The window is resolved once per period the user picks and then held still, so an answer recorded
 * from a notification while the screen is open re-emits the counts without the window sliding under
 * the reader mid-glance. Nothing here writes, and nothing here logs the medicine's name or amount.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MedicineHistoryViewModel(
    medicationRepository: MedicationRepository,
    doseRepository: DoseRepository,
    private val summarise: SummariseUsageHistory,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val medicationId = MedicationId(checkNotNull(savedStateHandle.get<Long>(MEDICATION_ID_ARG)) {
        "The usage history route always carries a medicine"
    })

    private val selectedPeriod = MutableStateFlow(
        savedStateHandle.get<String>(PERIOD_KEY)?.let(UsagePeriod::valueOf) ?: UsagePeriod.WEEK,
    )

    private val medicineName = MutableStateFlow("")

    // A channel, not a shared flow: the read can fail before the screen collects, and that message
    // must still arrive rather than be dropped.
    private val _effects = Channel<MedicineHistoryEffect>(Channel.BUFFERED)
    val effects: Flow<MedicineHistoryEffect> = _effects.receiveAsFlow()

    /** The medicine's oldest recorded moment, read once; null until it is known. */
    private val earliestRecordedAt = MutableStateFlow<Instant?>(null)

    val uiState: StateFlow<MedicineHistoryUiState> = combine(
        medicineName,
        earliestRecordedAt,
        selectedPeriod.flatMapLatest { period ->
            // Resolved once per selection and then held still: an answer recorded while the screen
            // is open re-emits the counts, but the window does not slide under the reader.
            val from = period.firstDay(LocalDate.now(clock)).atStartOfDay(clock.zone).toInstant()
            doseRepository.observeHistoryFor(medicationId, from, clock.instant())
        },
        selectedPeriod,
    ) { name, earliest, doses, period ->
        MedicineHistoryUiState(
            medicineName = name,
            period = period,
            history = summarise(period, doses, earliest),
            isLoading = name.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MedicineHistoryUiState(period = selectedPeriod.value),
    )

    init {
        viewModelScope.launch {
            val medication = runCatching { medicationRepository.get(medicationId) }.getOrNull()
            if (medication == null) {
                // Medicines are never removed, so this can only be a defect. The id is safe to
                // log; the name and the amount never are.
                Log.d(TAG, "No medicine with id ${medicationId.value} to show a history for")
                _effects.trySend(MedicineHistoryEffect.OpenFailed)
                return@launch
            }
            // How far the records reach first, then the name: the name is what ends the loading
            // state, so by the time the figures appear the records-start note is already settled.
            earliestRecordedAt.value = runCatching { doseRepository.earliestScheduledAt(medicationId) }.getOrNull()
            medicineName.value = medication.name
        }
    }

    /** The user chose another period; the window is resolved again and everything recomputes. */
    fun onPeriodSelected(period: UsagePeriod) {
        savedStateHandle[PERIOD_KEY] = period.name
        selectedPeriod.value = period
    }

    private companion object {
        const val TAG = "MedicineHistory"
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** The name navigation gives the route argument of the usage history destination. */
        const val MEDICATION_ID_ARG = "medicationId"

        /** Keeps the chosen period across rotation and process death. */
        const val PERIOD_KEY = "usageHistoryPeriod"
    }
}
