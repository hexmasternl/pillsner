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
        val bucketStarts = bucketStarts(firstDay, today, period.bucketsByWeek, firstDayOfWeek())
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
