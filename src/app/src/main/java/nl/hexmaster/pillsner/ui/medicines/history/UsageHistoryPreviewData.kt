package nl.hexmaster.pillsner.ui.medicines.history

import java.time.LocalDate
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
