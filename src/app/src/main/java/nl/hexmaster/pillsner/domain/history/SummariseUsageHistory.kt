package nl.hexmaster.pillsner.domain.history

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import java.time.temporal.WeekFields
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.UsageBucket
import nl.hexmaster.pillsner.domain.model.UsageHistory
import nl.hexmaster.pillsner.domain.model.UsagePeriod

/**
 * Turns a medicine's stored doses into the counts and buckets the usage history screen shows
 * (app-medicine-usage-history design D3, D4).
 *
 * Pure: no Android dependency, every date decision made through the injected [Clock] and its zone,
 * so midnight, month ends, leap days and daylight-saving transitions are unit-tested rather than
 * hoped for.
 *
 * @param firstDayOfWeek which day a week bucket starts on; the device locale's by default.
 */
class SummariseUsageHistory(
    private val clock: Clock,
    private val firstDayOfWeek: () -> DayOfWeek = { WeekFields.of(Locale.getDefault()).firstDayOfWeek },
) {

    /**
     * @param doses every stored dose of the medicine; anything outside the window, and anything
     *   still in the future, is ignored here rather than assumed away by the caller.
     * @param earliestRecordedAt the moment of the medicine's oldest stored dose, or null when it
     *   has none.
     */
    operator fun invoke(
        period: UsagePeriod,
        doses: List<Dose>,
        earliestRecordedAt: Instant?,
    ): UsageHistory {
        val zone = clock.zone
        val now = clock.instant()
        val today = LocalDate.now(clock)
        val firstDay = period.firstDay(today)
        val windowStart = firstDay.atStartOfDay(zone).toInstant()

        val counts = MutableCounts()
        val bucketStarts = bucketStarts(period, firstDay, today)
        val buckets = bucketStarts.map { MutableCounts() }

        doses.forEach { dose ->
            val at = dose.scheduledAt
            // The upper bound is now, not the end of today: a dose due this evening has not
            // happened yet and must never read as one the user failed to take.
            if (at.isBefore(windowStart) || at.isAfter(now)) return@forEach

            counts.add(dose)
            val day = at.atZone(zone).toLocalDate()
            val index = bucketIndex(bucketStarts, day)
            if (index >= 0) buckets[index].add(dose)
        }

        return UsageHistory(
            period = period,
            firstDay = firstDay,
            lastDay = today,
            scheduled = counts.scheduled,
            taken = counts.taken,
            skipped = counts.skipped,
            missed = counts.missed,
            unanswered = counts.unanswered,
            recordsStartOn = earliestRecordedAt
                ?.atZone(zone)
                ?.toLocalDate()
                ?.takeIf { it.isAfter(firstDay) },
            buckets = bucketStarts.mapIndexed { i, start ->
                buckets[i].toBucket(start, isWeek = period.bucketsByWeek)
            },
        )
    }

    /**
     * The first day of every bucket, in time order. A week period gives one bucket per day; the
     * longer periods give one per week aligned to [firstDayOfWeek], except the earliest, which
     * starts on the window's own first day when that falls mid-week.
     */
    private fun bucketStarts(period: UsagePeriod, firstDay: LocalDate, today: LocalDate): List<LocalDate> {
        if (!period.bucketsByWeek) {
            return generateSequence(firstDay) { it.plusDays(1) }
                .takeWhile { !it.isAfter(today) }
                .toList()
        }
        val weekStart = firstDayOfWeek()
        // The first whole week that begins inside the window; the partial stretch before it is the
        // earliest bucket, starting on the window's first day.
        var secondStart = firstDay
        while (secondStart.dayOfWeek != weekStart) secondStart = secondStart.plusDays(1)
        if (secondStart == firstDay) secondStart = firstDay.plusWeeks(1)

        val starts = mutableListOf(firstDay)
        var start = secondStart
        while (!start.isAfter(today)) {
            starts += start
            start = start.plusWeeks(1)
        }
        return starts
    }

    /** The bucket [day] falls in, or -1 when it falls outside every bucket. */
    private fun bucketIndex(starts: List<LocalDate>, day: LocalDate): Int {
        if (starts.isEmpty() || day.isBefore(starts.first())) return -1
        val after = starts.indexOfFirst { it.isAfter(day) }
        return if (after < 0) starts.lastIndex else after - 1
    }

    /** The four outcomes of one bucket or of the whole period, tallied as the doses go by. */
    private class MutableCounts {
        var taken = 0
        var skipped = 0
        var missed = 0
        var unanswered = 0

        val scheduled: Int get() = taken + skipped + missed + unanswered

        fun add(dose: Dose) {
            when (dose.intake?.outcome) {
                IntakeOutcome.TAKEN -> taken++
                IntakeOutcome.SKIPPED -> skipped++
                IntakeOutcome.MISSED -> missed++
                null -> unanswered++
            }
        }

        fun toBucket(start: LocalDate, isWeek: Boolean) = UsageBucket(
            start = start,
            isWeek = isWeek,
            scheduled = scheduled,
            taken = taken,
            skipped = skipped,
            missed = missed,
            unanswered = unanswered,
        )
    }
}
