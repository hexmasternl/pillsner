package nl.hexmaster.pillsner.domain.history

import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.TimeDeviationBucket
import nl.hexmaster.pillsner.domain.model.TimeDeviationHistory
import nl.hexmaster.pillsner.domain.model.UsagePeriod

/**
 * Turns a medicine's taken doses into the average minutes-off-schedule the timing accuracy chart
 * shows (medicine-history-time-deviation design D1, D2).
 *
 * Buckets per day for [UsagePeriod.WEEK] and [UsagePeriod.MONTH], per week for
 * [UsagePeriod.THREE_MONTHS] - a different rule from [SummariseUsageHistory], deliberately: a
 * month of daily timing detail is still readable and more useful than a month of weekly averages
 * (design D3).
 *
 * Pure: no Android dependency, every date decision made through the injected [Clock] and its zone.
 *
 * @param firstDayOfWeek which day a week bucket starts on; the device locale's by default.
 */
class SummariseTimeDeviation(
    private val clock: Clock,
    private val firstDayOfWeek: () -> DayOfWeek = { WeekFields.of(Locale.getDefault()).firstDayOfWeek },
) {

    /**
     * @param doses every stored dose of the medicine; anything outside the window, anything still
     *   in the future, and anything not taken, is excluded here - a skipped or missed dose has no
     *   recorded intake time and carries no timing information at all.
     */
    operator fun invoke(period: UsagePeriod, doses: List<Dose>): TimeDeviationHistory {
        val zone = clock.zone
        val now = clock.instant()
        val today = LocalDate.now(clock)
        val firstDay = period.firstDay(today)
        val windowStart = firstDay.atStartOfDay(zone).toInstant()
        val byWeek = period == UsagePeriod.THREE_MONTHS

        val starts = bucketStarts(firstDay, today, byWeek, firstDayOfWeek())
        val bucketDeviations = starts.map { mutableListOf<Int>() }
        val overallDeviations = mutableListOf<Int>()

        doses.forEach { dose ->
            val at = dose.scheduledAt
            // The upper bound is now, not the end of today, matching SummariseUsageHistory: a dose
            // due this evening has not happened yet.
            if (at.isBefore(windowStart) || at.isAfter(now)) return@forEach
            val intake = dose.intake ?: return@forEach
            if (intake.outcome != IntakeOutcome.TAKEN) return@forEach

            val minutes = deviationMinutes(at, intake.recordedAt)
            overallDeviations += minutes
            val day = at.atZone(zone).toLocalDate()
            val index = bucketIndex(starts, day)
            if (index >= 0) bucketDeviations[index] += minutes
        }

        return TimeDeviationHistory(
            period = period,
            firstDay = firstDay,
            lastDay = today,
            averageMinutes = overallDeviations.average(),
            buckets = starts.mapIndexed { i, start ->
                TimeDeviationBucket(
                    start = start,
                    isWeek = byWeek,
                    averageMinutes = bucketDeviations[i].average(),
                    takenCount = bucketDeviations[i].size,
                )
            },
        )
    }

    /**
     * Always positive, regardless of whether [recordedAt] fell before or after [scheduledAt], and
     * rounded to the nearest minute rather than truncated - truncating would silently make every
     * deviation look up to 59 seconds better than it was.
     */
    private fun deviationMinutes(scheduledAt: Instant, recordedAt: Instant): Int {
        val seconds = Duration.between(scheduledAt, recordedAt).seconds
        return abs(seconds / 60.0).roundToInt()
    }

    /** The mean, rounded to the nearest minute, or null when there is nothing to average. */
    private fun List<Int>.average(): Int? = if (isEmpty()) null else (sum().toDouble() / size).roundToInt()
}
