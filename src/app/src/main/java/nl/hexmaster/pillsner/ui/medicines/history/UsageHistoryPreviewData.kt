package nl.hexmaster.pillsner.ui.medicines.history

import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.TimeDeviationBucket
import nl.hexmaster.pillsner.domain.model.TimeDeviationHistory
import nl.hexmaster.pillsner.domain.model.UsageBucket
import nl.hexmaster.pillsner.domain.model.UsageHistory
import nl.hexmaster.pillsner.domain.model.UsagePeriod

/**
 * Fixed records for the previews of the usage history screen and its two pieces. Fixed rather than
 * generated from a clock, so a preview looks the same tomorrow as it does today.
 */
object UsageHistoryPreviewData {

    private val today: LocalDate = LocalDate.of(2026, 9, 14)

    /** A week taken twice a day, with one skipped dose, one missed and one still unanswered. */
    fun fullWeek(): UsageHistory {
        val buckets = listOf(
            bucket(today.minusDays(6), taken = 2),
            bucket(today.minusDays(5), taken = 2),
            bucket(today.minusDays(4), taken = 1, skipped = 1),
            bucket(today.minusDays(3), taken = 2),
            bucket(today.minusDays(2), taken = 1, missed = 1),
            bucket(today.minusDays(1), taken = 2),
            bucket(today, taken = 1, unanswered = 1),
        )
        return history(UsagePeriod.WEEK, today.minusDays(6), buckets)
    }

    /** Three months of a weekly medicine, recorded from part way in, so gaps and a note both show. */
    fun sparseThreeMonths(): UsageHistory {
        val firstDay = today.minusMonths(3).plusDays(1)
        val buckets = List(14) { index ->
            val start = if (index == 0) firstDay else firstDay.plusWeeks(index.toLong())
            when {
                index < 4 -> bucket(start, isWeek = true)
                index % 3 == 0 -> bucket(start, isWeek = true, taken = 1, missed = 1)
                else -> bucket(start, isWeek = true, taken = 1)
            }
        }
        return history(
            period = UsagePeriod.THREE_MONTHS,
            firstDay = firstDay,
            buckets = buckets,
            recordsStartOn = firstDay.plusWeeks(4),
        )
    }

    /** A medicine that has not had a dose reach its moment yet. */
    fun empty(): UsageHistory {
        val firstDay = today.minusDays(6)
        val buckets = (0..6).map { bucket(firstDay.plusDays(it.toLong())) }
        return history(UsagePeriod.WEEK, firstDay, buckets)
    }

    /** The same week as [fullWeek], with a mix of populated and empty timing buckets. */
    fun fullWeekTiming(): TimeDeviationHistory {
        val buckets = listOf(
            timingBucket(today.minusDays(6), minutes = 4, taken = 2),
            timingBucket(today.minusDays(5), minutes = 12, taken = 2),
            timingBucket(today.minusDays(4), minutes = 6, taken = 1),
            timingBucket(today.minusDays(3), minutes = 22, taken = 2),
            timingBucket(today.minusDays(2)), // nothing taken: an empty column
            timingBucket(today.minusDays(1), minutes = 3, taken = 2),
            timingBucket(today, minutes = 9, taken = 1),
        )
        return timingHistory(UsagePeriod.WEEK, today.minusDays(6), buckets)
    }

    /** The same window as [sparseThreeMonths], bucketed weekly for the timing chart. */
    fun sparseThreeMonthsTiming(): TimeDeviationHistory {
        val firstDay = today.minusMonths(3).plusDays(1)
        val buckets = List(14) { index ->
            val start = if (index == 0) firstDay else firstDay.plusWeeks(index.toLong())
            if (index < 4) timingBucket(start, isWeek = true) else timingBucket(start, isWeek = true, minutes = 5 + index, taken = 1)
        }
        return timingHistory(UsagePeriod.THREE_MONTHS, firstDay, buckets)
    }

    /** A period in which nothing was taken: the timing accuracy chart is omitted for this. */
    fun nothingTakenTiming(): TimeDeviationHistory {
        val firstDay = today.minusDays(6)
        val buckets = (0..6).map { timingBucket(firstDay.plusDays(it.toLong())) }
        return timingHistory(UsagePeriod.WEEK, firstDay, buckets)
    }

    private fun bucket(
        start: LocalDate,
        isWeek: Boolean = false,
        taken: Int = 0,
        skipped: Int = 0,
        missed: Int = 0,
        unanswered: Int = 0,
    ) = UsageBucket(
        start = start,
        isWeek = isWeek,
        scheduled = taken + skipped + missed + unanswered,
        taken = taken,
        skipped = skipped,
        missed = missed,
        unanswered = unanswered,
    )

    private fun timingBucket(
        start: LocalDate,
        isWeek: Boolean = false,
        minutes: Int? = null,
        taken: Int = 0,
    ) = TimeDeviationBucket(start = start, isWeek = isWeek, averageMinutes = minutes, takenCount = taken)

    private fun timingHistory(
        period: UsagePeriod,
        firstDay: LocalDate,
        buckets: List<TimeDeviationBucket>,
    ): TimeDeviationHistory {
        val taken = buckets.filter { it.averageMinutes != null }
        val overall = if (taken.isEmpty()) null else taken.sumOf { it.averageMinutes!! * it.takenCount } / taken.sumOf { it.takenCount }
        return TimeDeviationHistory(
            period = period,
            firstDay = firstDay,
            lastDay = today,
            averageMinutes = overall,
            buckets = buckets,
        )
    }

    private fun history(
        period: UsagePeriod,
        firstDay: LocalDate,
        buckets: List<UsageBucket>,
        recordsStartOn: LocalDate? = null,
    ) = UsageHistory(
        period = period,
        firstDay = firstDay,
        lastDay = today,
        scheduled = buckets.sumOf { it.scheduled },
        taken = buckets.sumOf { it.taken },
        skipped = buckets.sumOf { it.skipped },
        missed = buckets.sumOf { it.missed },
        unanswered = buckets.sumOf { it.unanswered },
        recordsStartOn = recordsStartOn,
        buckets = buckets,
    )
}
