package nl.hexmaster.pillsner.domain.scheduling

import java.time.Instant

/**
 * What kind of moment the app is waking for. It decides one thing: which alarm tier to use.
 *
 * Nothing else may be read from it, and nothing identifying may be added to it — a wake schedule
 * is mirrored into storage that can be read before the phone is unlocked (design D8).
 */
enum class WakeKind {

    /**
     * Something the user will see: a dose falling due, a snooze running out, a reminder asking
     * again. These go on the alarm-clock tier, the one tier the platform and the vendors respect.
     */
    REMINDER,

    /**
     * Bookkeeping the user never sees: a dose lapsing, the daily roll of the planning window. A
     * few minutes of drift costs nothing here, and it should not put an alarm icon in the status
     * bar.
     */
    HOUSEKEEPING,
}

/**
 * One moment the app has to wake for.
 *
 * Identity is the moment and the kind, never the dose: two doses due in the same minute are one
 * wake, and the wake works out from the database what is due once it runs.
 */
data class WakeMoment(val at: Instant, val kind: WakeKind)

/** Everything the app currently has to wake for. */
typealias WakeSchedule = Set<WakeMoment>
