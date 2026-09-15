package nl.hexmaster.pillsner.domain.intake

import java.time.Duration
import java.time.Instant

/** Whether a dose is being answered well before its moment, around it, or well after (design D4). */
enum class DoseTiming {
    /** Due in an hour or more. Worth saying, because taking a dose early is a real mistake. */
    EARLY,

    /** Near enough to its moment that nothing needs to be said. */
    ON_TIME,

    /** An hour or more overdue. */
    LATE,
}

/** An hour either way. Deliberately one constant and not a setting. */
val WARNING_MARGIN: Duration = Duration.ofHours(1)

/**
 * Where [now] sits relative to [scheduledAt].
 *
 * The boundaries, stated once so the tests, the spec and the screen agree: exactly one hour before
 * is [DoseTiming.EARLY] and one second later is [DoseTiming.ON_TIME]; exactly one hour after is
 * [DoseTiming.LATE] and one second earlier is [DoseTiming.ON_TIME].
 *
 * Arithmetic on instants, never on wall-clock times, so an hour is an hour across a
 * daylight-saving transition too.
 */
fun doseTiming(scheduledAt: Instant, now: Instant): DoseTiming = when {
    !now.isAfter(scheduledAt.minus(WARNING_MARGIN)) -> DoseTiming.EARLY
    !now.isBefore(scheduledAt.plus(WARNING_MARGIN)) -> DoseTiming.LATE
    else -> DoseTiming.ON_TIME
}
