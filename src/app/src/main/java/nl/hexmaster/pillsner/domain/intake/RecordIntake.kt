package nl.hexmaster.pillsner.domain.intake

import java.time.Clock
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.repository.DoseRepository

/**
 * Records what the user answered about a dose: taken, at the moment they said so, or deliberately
 * skipped. Either way the dose stops being pending, so no snooze survives it.
 */
class RecordIntake(
    private val doseRepository: DoseRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    suspend operator fun invoke(id: DoseId, outcome: IntakeOutcome) {
        require(outcome != IntakeOutcome.MISSED) {
            "Missed is not something the user answers; MarkMissedDoses records it"
        }
        doseRepository.recordIntake(id, outcome, clock.instant())
    }
}
