package nl.hexmaster.pillsner.domain.intake

import java.time.Clock
import java.time.Duration
import java.time.Instant
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.repository.DoseRepository
import nl.hexmaster.pillsner.domain.scheduling.MarkMissedDoses

/**
 * Postpones a reminder without settling the dose (design D4).
 *
 * A snooze is short and bounded: it never carries a dose past the point where it lapses, so
 * answering "Not yet" repeatedly can never keep a dose alive indefinitely or past the next dose of
 * the same medicine.
 */
class SnoozeDose(
    private val doseRepository: DoseRepository,
    private val markMissedDoses: MarkMissedDoses,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    /** @return the moment the reminder will come back, or null when the dose has already lapsed. */
    suspend operator fun invoke(id: DoseId): Instant? {
        val dose = doseRepository.get(id) ?: return null
        if (!dose.isPending) return null

        val lapseAt = markMissedDoses.lapseAt(dose)
        val until = minOf(clock.instant().plus(SNOOZE), lapseAt)
        doseRepository.setSnooze(id, until)
        return until
    }

    companion object {
        /** Fifteen minutes, deliberately not configurable. */
        val SNOOZE: Duration = Duration.ofMinutes(15)
    }
}
