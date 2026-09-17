package nl.hexmaster.pillsner.ui.medicines.history

import java.time.LocalDate
import nl.hexmaster.pillsner.domain.model.TimeDeviationBucket
import nl.hexmaster.pillsner.domain.model.TimeDeviationHistory
import nl.hexmaster.pillsner.domain.model.UsagePeriod

/**
 * Fixed records for the previews of the timing accuracy chart. Fixed rather than generated from a
 * clock, so a preview looks the same tomorrow as it does today.
 */
object TimeDeviationPreviewData {

    private val today: LocalDate = LocalDate.of(2026, 9, 14)

    /** A week with a clear timing spread, including one day with no dose taken. */
    fun fullWeek(): TimeDeviationHistory {
        val buckets = listOf(
            bucket(today.minusDays(6), averageMinutes = 4, takenCount = 2),
            bucket(today.minusDays(5), averageMinutes = 9, takenCount = 2),
            bucket(today.minusDays(4), averageMinutes = null, takenCount = 0),
            bucket(today.minusDays(3), averageMinutes = 22, takenCount = 2),
            bucket(today.minusDays(2), averageMinutes = 6, takenCount = 1),
            bucket(today.minusDays(1), averageMinutes = 11, takenCount = 2),
            bucket(today, averageMinutes = 3, takenCount = 1),
        )
        return history(UsagePeriod.WEEK, today.minusDays(6), buckets)
    }

    /** Nothing taken anywhere in the period, which is when the whole card is omitted. */
    fun empty(): TimeDeviationHistory {
        val firstDay = today.minusDays(6)
        val buckets = (0..6).map { bucket(firstDay.plusDays(it.toLong()), averageMinutes = null, takenCount = 0) }
        return history(UsagePeriod.WEEK, firstDay, buckets)
    }

    private fun bucket(
        start: LocalDate,
        isWeek: Boolean = false,
        averageMinutes: Int?,
        takenCount: Int,
    ) = TimeDeviationBucket(
        start = start,
        isWeek = isWeek,
        averageMinutes = averageMinutes,
        takenCount = takenCount,
    )

    private fun history(
        period: UsagePeriod,
        firstDay: LocalDate,
        buckets: List<TimeDeviationBucket>,
    ): TimeDeviationHistory {
        val minutes = buckets.mapNotNull { it.averageMinutes }
        return TimeDeviationHistory(
            period = period,
            firstDay = firstDay,
            lastDay = today,
            averageMinutes = if (minutes.isEmpty()) null else minutes.average().toInt(),
            buckets = buckets,
        )
    }
}
