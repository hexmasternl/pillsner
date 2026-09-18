package nl.hexmaster.pillsner.domain.history

import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.TimeDeviationBucket
import nl.hexmaster.pillsner.domain.model.TimeDeviationHistory
import nl.hexmaster.pillsner.domain.model.UsagePeriod
import kotlin.math.roundToInt

/**
 * Turns a medicine's stored doses into the timing accuracy the usage history screen shows
 * alongside [SummariseUsageHistory]'s outcome counts (medicine-history-time-deviation design D1).
 *
 * Only taken doses carry a timing deviation - the minutes between when a dose was scheduled and
 * when it was recorded taken, always positive whether the dose was early or late. Skipped and
 * missed doses have no moment they were actually taken, so they contribute nothing here.
 *
 * Buckets this period by day for "1 week" and "1 month", and by week for "3 months" - a different
 * rule from the outcome chart's, chosen because a month of daily timing detail is still readable
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

    /** @param doses every stored dose of the medicine; the window and future-dose rules match [SummariseUsageHistory]. */
    operator fun invoke(period: UsagePeriod, doses: List<Dose>): TimeDeviationHistory {
        val zone = clock.zone
        val now = clock.instant()
        val today = LocalDate.now(clock)
        val firstDay = period.firstDay(today)
        val windowStart = firstDay.atStartOfDay(zone).toInstant()
        val byWeek = period == UsagePeriod.THREE_MONTHS

        val bucketStarts = UsageBucketing.bucketStarts(firstDay, today, byWeek, firstDayOfWeek())
        val bucketDeviations = bucketStarts.map { mutableListOf<Int>() }
        val periodDeviations = mutableListOf<Int>()

        doses.forEach { dose ->
            val at = dose.scheduledAt
            // Same window as the outcome chart's: a dose due later than now has not happened yet.
            if (at.isBefore(windowStart) || at.isAfter(now)) return@forEach
            val intake = dose.intake ?: return@forEach
            if (intake.outcome != IntakeOutcome.TAKEN) return@forEach

            val minutes = deviationMinutes(at, intake.recordedAt)
            periodDeviations += minutes
            val day = at.atZone(zone).toLocalDate()
            val index = UsageBucketing.bucketIndex(bucketStarts, day)
            if (index >= 0) bucketDeviations[index] += minutes
        }

        return TimeDeviationHistory(
            period = period,
            firstDay = firstDay,
            lastDay = today,
            averageMinutes = periodDeviations.averageMinutesOrNull(),
            buckets = bucketStarts.mapIndexed { i, start ->
                TimeDeviationBucket(
                    start = start,
                    isWeek = byWeek,
                    averageMinutes = bucketDeviations[i].averageMinutesOrNull(),
                    takenCount = bucketDeviations[i].size,
                )
            },
        )
    }

    /** The absolute gap between when a dose was due and when it was taken, rounded to the nearest minute. */
    private fun deviationMinutes(scheduledAt: Instant, recordedAt: Instant): Int =
        (Duration.between(scheduledAt, recordedAt).abs().toMillis() / 60_000.0).roundToInt()

    private fun List<Int>.averageMinutesOrNull(): Int? = if (isEmpty()) null else average().roundToInt()
}
