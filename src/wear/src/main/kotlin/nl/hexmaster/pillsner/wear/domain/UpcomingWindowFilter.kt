package nl.hexmaster.pillsner.wear.domain

import java.time.Duration
import java.time.Instant
import nl.hexmaster.pillsner.shared.wear.SyncedDose

/**
 * Which of the doses the phone sent belong on the wrist right now (design D5). Free of Android
 * imports, so the rule that decides what the user sees is a plain unit test.
 *
 * The phone sends every pending dose it holds — a rolling two-day window — and this keeps the ones
 * scheduled no more than six hours from now. Nothing is dropped on the past side: the phone never
 * sends a dose that has lapsed into missed, so a dose with a time already gone is one the user
 * still has to take, and it belongs at the top of the list.
 */
object UpcomingWindowFilter {

    val WINDOW: Duration = Duration.ofHours(6)

    fun filter(doses: List<SyncedDose>, now: Instant): List<SyncedDose> {
        val until = now.plus(WINDOW)
        return doses
            .filter { !Instant.ofEpochMilli(it.scheduledAtEpochMillis).isAfter(until) }
            .sortedBy { it.scheduledAtEpochMillis }
    }
}
