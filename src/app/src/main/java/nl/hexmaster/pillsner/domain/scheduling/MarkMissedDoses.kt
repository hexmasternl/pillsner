package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import java.time.Duration
import java.time.Instant
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.repository.DoseRepository

/**
 * Decides when an unanswered dose has lapsed, and records it as missed (design D4).
 *
 * A dose lapses when the next dose of the same medicine falls due, or 24 hours after its own
 * moment, whichever comes first. The first rule stops the app asking about this morning's tablet
 * once this evening's is due; the second stops a weekly medicine sitting unanswered for a week.
 *
 * Only doses the user never answered become missed. A skipped dose was a decision and stays
 * skipped.
 */
class MarkMissedDoses(
    private val doseRepository: DoseRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /**
     * Marks every pending dose whose moment has lapsed as missed.
     *
     * @return the doses that were marked, so their notifications can be taken down.
     */
    suspend operator fun invoke(): List<Dose> {
        val now = clock.instant()
        val lapsed = doseRepository.pending().filter { !lapseAt(it).isAfter(now) }
        lapsed.forEach { dose ->
            doseRepository.recordIntake(dose.id, IntakeOutcome.MISSED, lapseAt(dose))
        }
        return lapsed
    }

    /** The moment [dose] stops being worth asking about. */
    suspend fun lapseAt(dose: Dose): Instant {
        val cap = dose.scheduledAt.plus(MAX_PENDING)
        val next = dose.medicationId?.let {
            doseRepository.nextScheduledAtAfter(it, dose.scheduledAt)
        }
        return if (next != null && next.isBefore(cap)) next else cap
    }

    companion object {
        /** However rare the medicine, the app stops asking about one dose after a day. */
        val MAX_PENDING: Duration = Duration.ofHours(24)
    }
}
