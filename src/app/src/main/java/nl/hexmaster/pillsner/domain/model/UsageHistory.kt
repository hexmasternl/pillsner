package nl.hexmaster.pillsner.domain.model

import java.time.LocalDate

/**
 * How far back a usage history looks. A period always ends today; only its first day differs
 * (app-medicine-usage-history design D2).
 */
enum class UsagePeriod {
    /** Today and the six days before it. */
    WEEK,

    /** The day after the same date one month ago, through today. */
    MONTH,

    /** The day after the same date three months ago, through today. */
    THREE_MONTHS,
    ;

    /**
     * The whole local days this period covers, ending on [today].
     *
     * The arithmetic is done on local dates rather than by subtracting a fixed number of hours, so
     * month lengths, leap days and daylight-saving transitions need no special case.
     */
    fun window(today: LocalDate): ClosedRange<LocalDate> = firstDay(today)..today

    /** The first local day of the window ending on [today]. */
    fun firstDay(today: LocalDate): LocalDate = when (this) {
        WEEK -> today.minusDays(6)
        MONTH -> today.minusMonths(1).plusDays(1)
        THREE_MONTHS -> today.minusMonths(3).plusDays(1)
    }

    /** True when this period is shown as one bar per week rather than one per day (design D4). */
    val bucketsByWeek: Boolean get() = this != WEEK
}

/**
 * One column of the usage chart: a single day for [UsagePeriod.WEEK], a week for the longer
 * periods. The counts follow the same rules as [UsageHistory]'s.
 *
 * @property start the first day this bucket covers. For a week bucket that may be later than the
 *   notional start of the week, when the window begins mid-week, so a label never claims days the
 *   window does not hold.
 * @property isWeek whether the bucket spans a week rather than a single day.
 */
data class UsageBucket(
    val start: LocalDate,
    val isWeek: Boolean,
    val scheduled: Int,
    val taken: Int,
    val skipped: Int,
    val missed: Int,
    val unanswered: Int,
)

/**
 * One medicine's record over one period, as the usage history screen shows it.
 *
 * @property firstDay the first local day of the window; [lastDay] is today.
 * @property scheduled every dose of the medicine whose moment fell inside the window and has
 *   passed. A dose due later than the present moment is not counted at all, in any category.
 *   This is always the sum of [taken], [skipped], [missed] and [unanswered].
 * @property taken doses the user confirmed they took.
 * @property skipped doses the user deliberately did not take. A skipped dose is a choice, never a
 *   failure, and is never counted as [missed].
 * @property missed doses that lapsed without an answer.
 * @property unanswered doses whose moment has passed but which have not lapsed yet. A real,
 *   short-lived state: neither a success nor a failure.
 * @property recordsStartOn the day the medicine's records begin, set only when that is later than
 *   [firstDay]; null when the records reach back at least as far as the window does.
 * @property buckets the chart's columns, in time order, empty ones included.
 */
data class UsageHistory(
    val period: UsagePeriod,
    val firstDay: LocalDate,
    val lastDay: LocalDate,
    val scheduled: Int,
    val taken: Int,
    val skipped: Int,
    val missed: Int,
    val unanswered: Int,
    val recordsStartOn: LocalDate?,
    val buckets: List<UsageBucket>,
) {
    /**
     * The proportion of scheduled doses that were taken, as a whole percentage rounded to the
     * nearest, or null when nothing was scheduled. Absent rather than zero: no doses is not the
     * same as none taken.
     */
    val adherencePercent: Int?
        get() = if (scheduled == 0) null else Math.round(taken * 100.0 / scheduled).toInt()

    /** True when the period holds nothing to show, which is what the empty state is for. */
    val isEmpty: Boolean get() = scheduled == 0
}
