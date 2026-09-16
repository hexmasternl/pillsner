package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import java.time.Instant
import nl.hexmaster.pillsner.domain.model.Dose

/**
 * The pending doses that should have a reminder showing right now: a dose whose moment has come and
 * that the user has not been told about yet, one whose snooze has run out, and one that has been
 * told about, not answered, and is due to be asked again (design D5).
 *
 * A dose the user has already been reminded about is never returned because its own moment came
 * round again, so a clock moved backwards cannot make the app re-announce doses the user has seen.
 * Only the repeat rule, which counts forward from the last posting, brings such a dose back.
 *
 * Works entirely from a [PendingSnapshot] built earlier in the same wake, rather than reading the
 * repository itself, so the wake cycle pays for one pending-dose read instead of one per use case.
 */
class DueDoses(
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    operator fun invoke(snapshot: PendingSnapshot): List<Dose> {
        val now = clock.instant()
        return snapshot.doses.filter { dose ->
            val snoozeElapsed = dose.snoozedUntil?.let { !it.isAfter(now) } == true
            val newlyDue = dose.firstRemindedAt == null && !dose.scheduledAt.isAfter(now)
            snoozeElapsed || newlyDue || repeatIsDue(dose, snapshot, now)
        }
    }

    private fun repeatIsDue(dose: Dose, snapshot: PendingSnapshot, now: Instant): Boolean {
        val repeatAt = ReminderRepeats.nextRepeatAt(dose, snapshot.lapseAt(dose)) ?: return false
        return !repeatAt.isAfter(now)
    }
}
