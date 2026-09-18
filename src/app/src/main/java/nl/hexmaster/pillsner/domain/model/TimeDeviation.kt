package nl.hexmaster.pillsner.domain.model

import java.time.LocalDate

/**
 * One column of the timing accuracy chart: a single day, or a week for the "3 months" period
 * (medicine-history-time-deviation design D1, D3).
 *
 * @property start the first day this bucket covers. For a week bucket that may be later than the
 *   notional start of the week, when the window begins mid-week, so a label never claims days the
 *   window does not hold.
 * @property isWeek whether the bucket spans a week rather than a single day.
 * @property averageMinutes the mean timing deviation, in minutes, of every taken dose whose
 *   scheduled moment falls in this bucket, rounded to the nearest minute; null when the bucket
 *   holds no taken dose, distinct from an average of zero.
 * @property takenCount how many taken doses this average is over.
 */
data class TimeDeviationBucket(
    val start: LocalDate,
    val isWeek: Boolean,
    val averageMinutes: Int?,
    val takenCount: Int,
)

/**
 * One medicine's timing accuracy over one period, as the usage history screen shows it alongside
 * [UsageHistory].
 *
 * @property firstDay the first local day of the window; [lastDay] is today.
 * @property averageMinutes the mean timing deviation, in minutes, of every taken dose in the
 *   window, rounded to the nearest minute; null when no dose was taken anywhere in the period, in
 *   which case the whole chart is omitted rather than shown at zero.
 * @property buckets the chart's columns, in time order, empty ones included.
 */
data class TimeDeviationHistory(
    val period: UsagePeriod,
    val firstDay: LocalDate,
    val lastDay: LocalDate,
    val averageMinutes: Int?,
    val buckets: List<TimeDeviationBucket>,
) {
    /** True when no dose was taken anywhere in the period, which is when the chart is omitted. */
    val isEmpty: Boolean get() = averageMinutes == null
}
