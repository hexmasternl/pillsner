package nl.hexmaster.pillsner.domain.scheduling

import java.time.Instant
import nl.hexmaster.pillsner.domain.model.Dose

/**
 * The moment of the most recent reminder that went missing in silence among [lapsed], or null when
 * none did (design D2).
 *
 * A reminder went missing in silence when a dose lapsed without the user ever having been told
 * about it, although the app had a window in which to tell them. That is what an alarm the platform
 * did not deliver looks like from inside the app, and it is the only view of it there is:
 * `AlarmManager` will not say what it holds, so the consequence is what can be seen.
 *
 * @param notificationsAllowed whether the app could have posted a notification at all. When it
 *   could not, nothing is reported however many doses lapsed un-reminded: their cause is known and
 *   has its own banner, and recording it here would leave a second, wronger banner behind once the
 *   user granted the permission.
 *
 * The most recent moment wins when several lapsed at once. The banner says a reminder did not
 * arrive, not how many did not.
 */
fun silentlyMissedReminderAmong(lapsed: List<Dose>, notificationsAllowed: Boolean): Instant? {
    if (!notificationsAllowed) return null
    return lapsed
        .filter { it.wasMissedInSilence }
        .maxOfOrNull { checkNotNull(it.intake) { "A dose missed in silence has an outcome" }.recordedAt }
}
