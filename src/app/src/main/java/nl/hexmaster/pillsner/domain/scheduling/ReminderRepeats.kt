package nl.hexmaster.pillsner.domain.scheduling

import java.time.Duration
import java.time.Instant
import nl.hexmaster.pillsner.domain.model.Dose

/**
 * When an unanswered reminder asks again (design D5).
 *
 * A reminder used to be announced exactly once, ever. If that one posting landed while the phone
 * was face down, in a pocket or on a call, that was the end of it until the dose lapsed — which is
 * not what "remind reliably" means. So a dose that has been posted and not answered is posted
 * again, four times, a quarter of an hour apart.
 *
 * Bounded deliberately: four repeats is about an hour of asking, after which a reminder has become
 * nagging rather than helpful and the dose simply waits for its lapse moment. Any of the three
 * answers ends the sequence, and "Not yet" starts it over, because the user has acknowledged the
 * dose and the app should not go on counting down against them.
 */
object ReminderRepeats {

    /** The same quarter of an hour a snooze lasts, so the two read as one behaviour. */
    val INTERVAL: Duration = Duration.ofMinutes(15)

    /** After this many asks the app goes quiet and waits for the dose to lapse. */
    const val MAX = 4

    /**
     * The moment [dose] should be asked about again, or null when it should not be.
     *
     * @param lapseAt the moment the dose stops being worth asking about, from [MarkMissedDoses].
     *   A repeat is never posted at or after it: the dose is about to become missed, or its
     *   successor is about to be due, and neither is a moment to ask about this one.
     */
    fun nextRepeatAt(dose: Dose, lapseAt: Instant): Instant? {
        if (!dose.isPending) return null
        // A snoozed dose has its own moment to come back at; the repeats resume from there.
        if (dose.snoozedUntil != null) return null
        if (dose.reminderCount >= MAX) return null
        val lastPosted = dose.lastRemindedAt ?: return null
        val at = lastPosted.plus(INTERVAL)
        return at.takeIf { it.isBefore(lapseAt) }
    }
}
