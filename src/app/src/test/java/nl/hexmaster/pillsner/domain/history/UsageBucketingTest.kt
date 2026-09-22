package nl.hexmaster.pillsner.domain.history

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bucket-boundary arithmetic shared by [SummariseUsageHistory] and [SummariseTimeDeviation]:
 * daily buckets, week-aligned buckets with a short first bucket, and the awkward dates - a leap
 * day, a daylight-saving transition - that must never split or duplicate a bucket.
 */
class UsageBucketingTest {

    private fun day(month: Int, dayOfMonth: Int, year: Int = 2026): LocalDate = LocalDate.of(year, month, dayOfMonth)

    @Test
    fun `daily buckets cover every day from first to today`() {
        val starts = bucketStarts(day(9, 8), day(9, 14), byWeek = false, firstDayOfWeek = DayOfWeek.MONDAY)

        assertEquals((8..14).map { day(9, it) }, starts)
    }

    @Test
    fun `weekly buckets align to the locale first day of week with a short first bucket`() {
        // 15 August 2026 is a Saturday, so the earliest bucket is a two-day stub.
        val starts = bucketStarts(day(8, 15), day(9, 14), byWeek = true, firstDayOfWeek = DayOfWeek.MONDAY)

        assertEquals(day(8, 15), starts.first())
        assertEquals(day(8, 17), starts[1])
        assertEquals(DayOfWeek.MONDAY, starts[1].dayOfWeek)
        assertEquals(day(9, 14), starts.last())
    }

    @Test
    fun `a weekly window that opens exactly on the week start has no short first bucket`() {
        val starts = bucketStarts(day(8, 17), day(9, 14), byWeek = true, firstDayOfWeek = DayOfWeek.MONDAY)

        assertEquals(day(8, 17), starts.first())
        assertEquals(day(8, 24), starts[1])
    }

    @Test
    fun `a leap day falls in its own daily bucket without shifting the rest`() {
        val starts = bucketStarts(
            firstDay = LocalDate.of(2028, 2, 26),
            today = LocalDate.of(2028, 3, 1),
            byWeek = false,
            firstDayOfWeek = DayOfWeek.MONDAY,
        )

        assertEquals(
            listOf(
                LocalDate.of(2028, 2, 26),
                LocalDate.of(2028, 2, 27),
                LocalDate.of(2028, 2, 28),
                LocalDate.of(2028, 2, 29),
                LocalDate.of(2028, 3, 1),
            ),
            starts,
        )
    }

    @Test
    fun `a daylight-saving transition day is still one bucket`() {
        // Clocks go forward on Sunday 29 March 2026 in Amsterdam; the bucketing is pure calendar
        // arithmetic and does not need to know that.
        val starts = bucketStarts(day(3, 25), day(3, 31), byWeek = false, firstDayOfWeek = DayOfWeek.MONDAY)

        assertEquals(7, starts.size)
        assertEquals(day(3, 29), starts[4])
    }

    @Test
    fun `bucketIndex finds the bucket a day falls in`() {
        val starts = listOf(day(9, 8), day(9, 10), day(9, 13))

        assertEquals(0, bucketIndex(starts, day(9, 9)))
        assertEquals(1, bucketIndex(starts, day(9, 12)))
        assertEquals(2, bucketIndex(starts, day(9, 14)))
    }

    @Test
    fun `bucketIndex is -1 before the first bucket`() {
        val starts = listOf(day(9, 8), day(9, 10))

        assertEquals(-1, bucketIndex(starts, day(9, 7)))
    }

    @Test
    fun `bucketIndex is -1 for an empty bucket list`() {
        assertEquals(-1, bucketIndex(emptyList(), day(9, 7)))
    }
}
