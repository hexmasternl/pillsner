package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import java.time.Instant
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.repository.DoseRepository

/**
 * The pending doses that should have a reminder showing right now: a dose whose moment has come and
 * that the user has not been told about yet, one whose snooze has run out, and one that has been
 * told about, not answered, and is due to be asked again (design D5).
 *
 * A dose the user has already been reminded about is never returned because its own moment came
 * round again, so a clock moved backwards cannot make the app re-announce doses the user has seen.
 * Only the repeat rule, which counts forward from the last posting, brings such a dose back.
 */
class DueDoses(
    private val doseRepository: DoseRepository,
    private val markMissedDoses: MarkMissedDoses,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    suspend operator fun invoke(): List<Dose> {
        val now = clock.instant()
        return doseRepository.pending().filter { dose ->
            val snoozeElapsed = dose.snoozedUntil?.let { !it.isAfter(now) } == true
            val newlyDue = dose.firstRemindedAt == null && !dose.scheduledAt.isAfter(now)
            snoozeElapsed || newlyDue || repeatIsDue(dose, now)
        }
    }

    private suspend fun repeatIsDue(dose: Dose, now: Instant): Boolean {
        val repeatAt = ReminderRepeats.nextRepeatAt(dose, markMissedDoses.lapseAt(dose)) ?: return false
        return !repeatAt.isAfter(now)
    }
}
