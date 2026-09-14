package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.repository.DoseRepository

/**
 * The pending doses that should have a reminder showing right now: a dose whose moment has come
 * and that the user has not been told about yet, or one whose snooze has run out.
 *
 * A dose the user has already been reminded about is not returned again, so a clock moved backwards
 * cannot make the app re-announce doses the user has seen.
 */
class DueDoses(
    private val doseRepository: DoseRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    suspend operator fun invoke(): List<Dose> {
        val now = clock.instant()
        return doseRepository.pending().filter { dose ->
            val snoozeElapsed = dose.snoozedUntil?.let { !it.isAfter(now) } == true
            val newlyDue = dose.firstRemindedAt == null && !dose.scheduledAt.isAfter(now)
            snoozeElapsed || newlyDue
        }
    }
}
