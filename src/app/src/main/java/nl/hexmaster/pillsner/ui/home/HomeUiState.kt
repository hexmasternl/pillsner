package nl.hexmaster.pillsner.ui.home

import java.time.Instant
import nl.hexmaster.pillsner.domain.model.UpcomingDose
import nl.hexmaster.pillsner.ui.theme.IntakeStatus

/**
 * What the welcome screen shows (design D7).
 *
 * @property upcomingDoses at most [HomeViewModel.MAX_UPCOMING_DOSES] doses, soonest first.
 * @property isLoading true until the first list has arrived, so the empty state does not flash
 *   before real data is known.
 * @property notificationsAllowed whether the user has allowed Pillsner to show notifications.
 * @property alarmsAreExact whether the platform lets reminders fire at the exact minute.
 * @property now the moment the state was built, which is what decides whether a dose is overdue.
 */
data class HomeUiState(
    val upcomingDoses: List<UpcomingDose> = emptyList(),
    val isLoading: Boolean = true,
    val notificationsAllowed: Boolean = true,
    val alarmsAreExact: Boolean = true,
    val now: Instant = Instant.EPOCH,
) {
    /** Reminders cannot be delivered as promised, so the Home screen has to say so. */
    val remindersAreUnreliable: Boolean get() = !notificationsAllowed || !alarmsAreExact
}

/**
 * How a dose reads on the Home screen (docs/design-system.md section 2.3). A dose the user has been
 * asked about but has not answered is overdue, which is one of the few places red is allowed; a
 * postponed one is snoozed; everything else is simply due.
 */
fun UpcomingDose.status(now: Instant): IntakeStatus = when {
    snoozedUntil != null -> IntakeStatus.Snoozed
    isOverdue || scheduledAt.isBefore(now) -> IntakeStatus.Overdue
    else -> IntakeStatus.Due
}
