package nl.hexmaster.pillsner.ui.dose

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.domain.intake.AnswerDose
import nl.hexmaster.pillsner.domain.intake.DoseAnswer
import nl.hexmaster.pillsner.domain.intake.WARNING_MARGIN
import nl.hexmaster.pillsner.domain.intake.doseTiming
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.ui.home.UpcomingDoseTimeFormatter

/**
 * One dose, and the three answers (design D2, D4, D6).
 *
 * The dose is read as a stream rather than loaded once, because it can be answered from the
 * notification shade or from a watch while this screen is in the foreground, and what the screen
 * offers must follow. Answering is fire-and-forget: the record is written before the screen closes,
 * and the wake that follows it runs on the coordinator's own scope under its own lock.
 */
class DoseDetailViewModel(
    doseRepository: DoseRepository,
    private val answerDose: AnswerDose,
    savedStateHandle: SavedStateHandle,
    private val clock: Clock = Clock.systemDefaultZone(),
    timeFormatter: UpcomingDoseTimeFormatter = UpcomingDoseTimeFormatter(clock = clock),
) : ViewModel() {

    private val doseId = DoseId(
        checkNotNull(savedStateHandle.get<Long>(DOSE_ID_ARG)) { "The dose detail route always carries a dose" },
    )

    // A channel, not a shared flow: closing must arrive even when it is emitted before the screen
    // has started collecting, and it must never be replayed on a configuration change.
    private val _effects = Channel<DoseDetailEffect>(Channel.BUFFERED)
    val effects: Flow<DoseDetailEffect> = _effects.receiveAsFlow()

    private val doses: Flow<Dose?> = doseRepository.observe(doseId)

    val uiState: StateFlow<DoseDetailUiState> = combine(doses, timingTicks(doses)) { dose, _ ->
        val now = clock.instant()
        when {
            dose == null -> DoseDetailUiState.Gone

            dose.isPending -> DoseDetailUiState.Answerable(
                medicationName = dose.medicationName,
                amount = dose.amount,
                time = timeFormatter.format(dose.scheduledAt),
                status = dose.detailStatus(now),
                timing = doseTiming(dose.scheduledAt, now),
            )

            else -> {
                val intake = checkNotNull(dose.intake) { "A dose that is not pending has an outcome" }
                DoseDetailUiState.Settled(
                    medicationName = dose.medicationName,
                    amount = dose.amount,
                    time = timeFormatter.format(dose.scheduledAt),
                    status = dose.detailStatus(now),
                    outcome = intake.outcome,
                    recordedAt = intake.recordedAt,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DoseDetailUiState.Loading)

    /** Records [answer] and leaves; the list on Home is a stream off the same database. */
    fun onAnswer(answer: DoseAnswer) {
        viewModelScope.launch {
            answerDose(doseId, answer)
            _effects.send(DoseDetailEffect.Close)
        }
    }

    /**
     * Emits once at the start, then once at each early or late boundary the dose still has ahead
     * of it, and never otherwise.
     *
     * A screen left open has to move from "not due yet" to "on time" to "overdue" on its own, and
     * recomposition is not a clock. Rather than tick every second for a change that happens twice,
     * this waits until each boundary is actually reached. Combined with the dose stream that is
     * enough: any change to the dose re-emits too, and every emission recomputes the timing from
     * the instant it happens at.
     */
    private fun timingTicks(doses: Flow<Dose?>): Flow<Unit> = flow {
        emit(Unit)
        val dose = doses.first() ?: return@flow
        listOf(dose.scheduledAt.minus(WARNING_MARGIN), dose.scheduledAt.plus(WARNING_MARGIN))
            .forEach { boundary ->
                if (boundary.isAfter(clock.instant())) {
                    delay(millisUntil(boundary))
                    emit(Unit)
                }
            }
    }

    private fun millisUntil(moment: Instant): Long =
        (moment.toEpochMilli() - clock.instant().toEpochMilli()).coerceAtLeast(0L)

    companion object {
        /** The name `DoseDetail`'s `doseId` property is serialised under in the route. */
        const val DOSE_ID_ARG = "doseId"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
