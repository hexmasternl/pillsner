package nl.hexmaster.pillsner.domain.history

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The bucket boundaries [SummariseUsageHistory] and [SummariseTimeDeviation] both need: one bucket
 * per day, or one per week aligned to [firstDayOfWeek] with a short first bucket when the window
 * opens mid-week. Shared so the two summarisers agree on bucket edges - and on their leap-day and
 * daylight-saving correctness - without duplicating the arithmetic (medicine-history-time-deviation
 * design D3).
 */
internal fun bucketStarts(
    firstDay: LocalDate,
    today: LocalDate,
    byWeek: Boolean,
    firstDayOfWeek: DayOfWeek,
): List<LocalDate> {
    if (!byWeek) {
        return generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .toList()
    }
    // The first whole week that begins inside the window; the partial stretch before it is the
    // earliest bucket, starting on the window's first day.
    var secondStart = firstDay
    while (secondStart.dayOfWeek != firstDayOfWeek) secondStart = secondStart.plusDays(1)
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
internal fun bucketIndex(starts: List<LocalDate>, day: LocalDate): Int {
    if (starts.isEmpty() || day.isBefore(starts.first())) return -1
    val after = starts.indexOfFirst { it.isAfter(day) }
    return if (after < 0) starts.lastIndex else after - 1
}
