package nl.hexmaster.pillsner.domain.model

import java.time.LocalDate

/**
 * One column of the timing accuracy chart: how many minutes, on average, a taken dose's actual
 * intake time differed from its scheduled time - always a positive amount regardless of whether
 * doses ran early or late (medicine-history-time-deviation design D1, D2).
 *
 * @property averageMinutes null exactly when [takenCount] is zero: no data, not a zero-minute
 *   average.
 */
data class TimeDeviationBucket(
    val start: LocalDate,
    val isWeek: Boolean,
    val averageMinutes: Int?,
    val takenCount: Int,
)

/**
 * One medicine's timing accuracy over one period: how close to on time its taken doses were
 * (medicine-history-time-deviation design D1).
 *
 * @property firstDay the first local day of the window; [lastDay] is today.
 * @property averageMinutes the overall average across every taken dose in the period, or null
 *   when none was taken - in which case the chart this backs is omitted entirely.
 * @property buckets the chart's columns, in time order, empty ones included.
 */
data class TimeDeviationHistory(
    val period: UsagePeriod,
    val firstDay: LocalDate,
    val lastDay: LocalDate,
    val averageMinutes: Int?,
    val buckets: List<TimeDeviationBucket>,
)
