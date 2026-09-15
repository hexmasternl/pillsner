package nl.hexmaster.pillsner.ui.dose

import java.time.Instant
import nl.hexmaster.pillsner.domain.intake.DoseTiming
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.ui.home.FormattedDoseTime
import nl.hexmaster.pillsner.ui.theme.IntakeStatus

/**
 * What the dose detail screen is showing (design D6).
 *
 * A sealed interface rather than one class with a flag, because three of the four are genuinely
 * different screens. The version with nullable fields makes it possible to render answer buttons
 * for a dose that has already been answered, which is the one thing this screen must never do.
 */
sealed interface DoseDetailUiState {

    /** The first emission has not arrived. The app bar and an empty surface, so nothing flashes. */
    data object Loading : DoseDetailUiState

    /**
     * The dose is pending and the user can answer it.
     *
     * @property timing whether the dose is due in an hour or more, overdue by an hour or more, or
     *   neither. It changes what the screen says and nothing about what it lets the user do.
     */
    data class Answerable(
        val medicationName: String,
        val amount: Quantity,
        val time: FormattedDoseTime,
        val status: IntakeStatus,
        val timing: DoseTiming,
    ) : DoseDetailUiState

    /**
     * The dose has an outcome, so there is nothing left to answer.
     *
     * Reached by answering here, and also by answering from the notification shade or a watch
     * while this screen is open — which is why the screen reads the dose as a stream.
     *
     * @property recordedAt when the outcome was settled; only [IntakeOutcome.TAKEN] shows it, since
     *   that is the moment adherence is measured against.
     */
    data class Settled(
        val medicationName: String,
        val amount: Quantity,
        val time: FormattedDoseTime,
        val status: IntakeStatus,
        val outcome: IntakeOutcome,
        val recordedAt: Instant,
    ) : DoseDetailUiState

    /** The dose is not there any more: a refresh withdrew it after the user changed its medicine. */
    data object Gone : DoseDetailUiState
}

/** A one-shot instruction to the screen; never part of the state, so it cannot be replayed. */
sealed interface DoseDetailEffect {
    /** The dose has been answered, or the user asked to leave. */
    data object Close : DoseDetailEffect
}

/**
 * How one dose reads on this screen (docs/design-system.md section 2.3).
 *
 * The same mapping the Welcome tile uses, so the status the user tapped is the status they land on:
 * an answered dose shows its outcome, a postponed one is snoozed, one past its moment and still
 * unanswered is overdue, and everything else is simply due.
 */
fun Dose.detailStatus(now: Instant): IntakeStatus = when (intake?.outcome) {
    IntakeOutcome.TAKEN -> IntakeStatus.Taken
    IntakeOutcome.SKIPPED -> IntakeStatus.Skipped
    IntakeOutcome.MISSED -> IntakeStatus.Missed
    null -> when {
        snoozedUntil != null -> IntakeStatus.Snoozed
        scheduledAt.isBefore(now) -> IntakeStatus.Overdue
        else -> IntakeStatus.Due
    }
}
