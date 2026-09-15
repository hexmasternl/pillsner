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
 * @property batteryExempt whether Pillsner is out of battery optimisation, which is what lets it
 *   act on an alarm rather than merely receive one.
 * @property now the moment the state was built, which is what decides whether a dose is overdue.
 */
data class HomeUiState(
    val upcomingDoses: List<UpcomingDose> = emptyList(),
    val isLoading: Boolean = true,
    val notificationsAllowed: Boolean = true,
    val alarmsAreExact: Boolean = true,
    val batteryExempt: Boolean = true,
    val now: Instant = Instant.EPOCH,
) {
    /**
     * The one thing standing between the user and a reliable reminder, or null when nothing is
     * (design D6).
     *
     * Three things can stop a reminder and more than one can be wrong at a time, but the banner
     * holds one message and one button. So they are ordered by how completely each one breaks the
     * promise: denied notifications mean no reminder at all; battery optimisation means a reminder
     * that may never be acted on; inexact alarms mean a reminder that is merely late. Fixing the
     * first reveals the next.
     */
    val reminderProblem: ReminderProblem? get() = when {
        !notificationsAllowed -> ReminderProblem.NOTIFICATIONS_DENIED
        !batteryExempt -> ReminderProblem.BATTERY_OPTIMISED
        !alarmsAreExact -> ReminderProblem.INEXACT_ALARMS
        else -> null
    }

    /** Reminders cannot be delivered as promised, so the Home screen has to say so. */
    val remindersAreUnreliable: Boolean get() = reminderProblem != null
}

/** What the Home banner is reporting, most severe first (design D6). */
enum class ReminderProblem {
    /** Pillsner may not post a notification, so a due dose is never announced. */
    NOTIFICATIONS_DENIED,

    /** The platform may stop Pillsner from running when its alarm goes off. */
    BATTERY_OPTIMISED,

    /** Reminders still arrive, but within a ten-minute window rather than on the minute. */
    INEXACT_ALARMS,
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
