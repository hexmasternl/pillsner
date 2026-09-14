package nl.hexmaster.pillsner.domain.history

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.model.Dose
import nl.hexmaster.pillsner.domain.model.DoseId
import nl.hexmaster.pillsner.domain.model.Intake
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.TestFixtures.mg40
import nl.hexmaster.pillsner.domain.model.UsagePeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The counting, the window arithmetic and the bucketing of the usage history, all of it pure so
 * the awkward dates - month ends, leap days, the clocks changing - are tested rather than hoped
 * for (app-medicine-usage-history design D2, D3, D4).
 */
class SummariseUsageHistoryTest {

    private val amsterdam: ZoneId = ZoneId.of("Europe/Amsterdam")

    /** Monday 14 September 2026, lunchtime: doses later today are still to come. */
    private val noon: Instant =
        ZonedDateTime.of(LocalDate.of(2026, 9, 14), LocalTime.NOON, amsterdam).toInstant()

    private var nextId = 1L

    private fun summariser(
        now: Instant = noon,
        zone: ZoneId = amsterdam,
        weekStart: DayOfWeek = DayOfWeek.MONDAY,
    ) = SummariseUsageHistory(MutableTestClock(now, zone)) { weekStart }

    private fun dose(at: Instant, outcome: IntakeOutcome? = null) = Dose(
        id = DoseId(nextId++),
        medicationId = MedicationId(1),
        medicationName = "Ibuprofen",
        amount = mg40,
        scheduledAt = at,
        intake = outcome?.let { Intake(it, at) },
    )

    private fun at(date: LocalDate, hour: Int = 8): Instant =
        ZonedDateTime.of(date, LocalTime.of(hour, 0), amsterdam).toInstant()

    private fun day(month: Int, dayOfMonth: Int, year: Int = 2026): LocalDate =
        LocalDate.of(year, month, dayOfMonth)

    // --- Window arithmetic ------------------------------------------------------------------

    @Test
    fun `a week is today and the six days before it`() {
        val history = summariser()(UsagePeriod.WEEK, emptyList(), null)

        assertEquals(day(9, 8), history.firstDay)
        assertEquals(day(9, 14), history.lastDay)
    }

    @Test
    fun `a month ending on the thirty-first starts on the first`() {
        val endOfMarch = ZonedDateTime.of(day(3, 31), LocalTime.NOON, amsterdam).toInstant()

        val history = summariser(now = endOfMarch)(UsagePeriod.MONTH, emptyList(), null)

        assertEquals(day(3, 1), history.firstDay)
        assertEquals(day(3, 31), history.lastDay)
    }

    @Test
    fun `three months back from the end of May in a leap year start on the first of March`() {
        val endOfMay = ZonedDateTime.of(day(5, 31, year = 2028), LocalTime.NOON, amsterdam).toInstant()

        val history = summariser(now = endOfMay)(UsagePeriod.THREE_MONTHS, emptyList(), null)

        // 2028 is a leap year; 29 February falls the day before the window, so it is outside it.
        assertEquals(LocalDate.of(2028, 3, 1), history.firstDay)
        assertEquals(LocalDate.of(2028, 5, 31), history.lastDay)
    }

    @Test
    fun `a week spanning the spring clock change still holds seven whole days`() {
        // Clocks go forward on Sunday 29 March 2026 in Amsterdam.
        val afterSpringForward = ZonedDateTime.of(day(3, 31), LocalTime.NOON, amsterdam).toInstant()
        val doses = (25..31).map { dose(at(day(3, it)), IntakeOutcome.TAKEN) }

        val history = summariser(now = afterSpringForward)(UsagePeriod.WEEK, doses, null)

        assertEquals(day(3, 25), history.firstDay)
        assertEquals(7, history.buckets.size)
        assertEquals(7, history.scheduled)
        assertEquals(List(7) { 1 }, history.buckets.map { it.scheduled })
    }

    @Test
    fun `a week spanning the autumn clock change still holds seven whole days`() {
        // Clocks go back on Sunday 25 October 2026 in Amsterdam.
        val afterFallBack = ZonedDateTime.of(day(10, 27), LocalTime.NOON, amsterdam).toInstant()
        val doses = (21..27).map { dose(at(day(10, it)), IntakeOutcome.TAKEN) }

        val history = summariser(now = afterFallBack)(UsagePeriod.WEEK, doses, null)

        assertEquals(day(10, 21), history.firstDay)
        assertEquals(7, history.buckets.size)
        assertEquals(7, history.scheduled)
        assertEquals(List(7) { 1 }, history.buckets.map { it.scheduled })
    }

    // --- Counting rules ---------------------------------------------------------------------

    @Test
    fun `the four outcomes are counted separately`() {
        val doses = listOf(
            dose(at(day(9, 9)), IntakeOutcome.TAKEN),
            dose(at(day(9, 10)), IntakeOutcome.TAKEN),
            dose(at(day(9, 11)), IntakeOutcome.TAKEN),
            dose(at(day(9, 12)), IntakeOutcome.SKIPPED),
            dose(at(day(9, 13)), IntakeOutcome.MISSED),
            dose(at(day(9, 14), hour = 8)),
        )

        val history = summariser()(UsagePeriod.WEEK, doses, null)

        assertEquals(6, history.scheduled)
        assertEquals(3, history.taken)
        assertEquals(1, history.skipped)
        assertEquals(1, history.missed)
        assertEquals(1, history.unanswered)
    }

    @Test
    fun `a dose later today is not counted at all`() {
        val doses = listOf(
            dose(at(day(9, 14), hour = 8), IntakeOutcome.TAKEN),
            dose(at(day(9, 14), hour = 20)),
        )

        val history = summariser()(UsagePeriod.WEEK, doses, null)

        assertEquals(1, history.scheduled)
        assertEquals(1, history.taken)
        assertEquals(0, history.unanswered)
    }

    @Test
    fun `tomorrow is not counted`() {
        val history = summariser()(UsagePeriod.WEEK, listOf(dose(at(day(9, 15)))), null)

        assertEquals(0, history.scheduled)
    }

    @Test
    fun `the day before the window is not counted`() {
        val doses = listOf(dose(at(day(9, 7)), IntakeOutcome.TAKEN))

        val history = summariser()(UsagePeriod.WEEK, doses, null)

        assertEquals(0, history.scheduled)
        assertEquals(0, history.taken)
    }

    @Test
    fun `a skipped dose is never counted as missed`() {
        val doses = listOf(dose(at(day(9, 10)), IntakeOutcome.SKIPPED))

        val history = summariser()(UsagePeriod.WEEK, doses, null)

        assertEquals(1, history.skipped)
        assertEquals(0, history.missed)
    }

    @Test
    fun `adherence rounds two of three to sixty-seven percent`() {
        val doses = listOf(
            dose(at(day(9, 10)), IntakeOutcome.TAKEN),
            dose(at(day(9, 11)), IntakeOutcome.TAKEN),
            dose(at(day(9, 12)), IntakeOutcome.MISSED),
        )

        assertEquals(67, summariser()(UsagePeriod.WEEK, doses, null).adherencePercent)
    }

    @Test
    fun `nothing scheduled gives no adherence at all`() {
        val history = summariser()(UsagePeriod.WEEK, emptyList(), null)

        assertNull(history.adherencePercent)
        assertEquals(true, history.isEmpty)
    }

    // --- Bucketing --------------------------------------------------------------------------

    @Test
    fun `a week gives seven day buckets ending today`() {
        val history = summariser()(UsagePeriod.WEEK, emptyList(), null)

        assertEquals(7, history.buckets.size)
        assertEquals(day(9, 8), history.buckets.first().start)
        assertEquals(day(9, 14), history.buckets.last().start)
        assertEquals(List(7) { false }, history.buckets.map { it.isWeek })
    }

    @Test
    fun `a month gives week buckets the first of which starts on the window first day`() {
        // 15 August 2026 is a Saturday, so the earliest bucket is a two-day stub.
        val history = summariser()(UsagePeriod.MONTH, emptyList(), null)

        assertEquals(day(8, 15), history.firstDay)
        assertEquals(day(8, 15), history.buckets.first().start)
        assertEquals(day(8, 17), history.buckets[1].start)
        assertEquals(DayOfWeek.MONDAY, history.buckets[1].start.dayOfWeek)
        assertEquals(day(9, 14), history.buckets.last().start)
        assertEquals(List(history.buckets.size) { true }, history.buckets.map { it.isWeek })
    }

    @Test
    fun `three months give thirteen or fourteen week buckets`() {
        val history = summariser()(UsagePeriod.THREE_MONTHS, emptyList(), null)

        assertEquals(day(6, 15), history.firstDay)
        assertEquals(true, history.buckets.size in 13..14)
        assertEquals(day(6, 15), history.buckets.first().start)
    }

    @Test
    fun `empty buckets stay in place and bucket totals sum to the period totals`() {
        val doses = listOf(
            dose(at(day(9, 8)), IntakeOutcome.TAKEN),
            dose(at(day(9, 8)), IntakeOutcome.MISSED),
            dose(at(day(9, 13)), IntakeOutcome.SKIPPED),
        )

        val history = summariser()(UsagePeriod.WEEK, doses, null)

        assertEquals(listOf(2, 0, 0, 0, 0, 1, 0), history.buckets.map { it.scheduled })
        assertEquals(history.scheduled, history.buckets.sumOf { it.scheduled })
        assertEquals(history.taken, history.buckets.sumOf { it.taken })
        assertEquals(history.skipped, history.buckets.sumOf { it.skipped })
        assertEquals(history.missed, history.buckets.sumOf { it.missed })
        assertEquals(history.unanswered, history.buckets.sumOf { it.unanswered })
    }

    @Test
    fun `a dose lands in the week bucket it belongs to`() {
        val doses = listOf(
            dose(at(day(8, 16)), IntakeOutcome.TAKEN), // the Sunday in the partial first bucket
            dose(at(day(8, 17)), IntakeOutcome.TAKEN), // the Monday that opens the second bucket
        )

        val history = summariser()(UsagePeriod.MONTH, doses, null)

        assertEquals(1, history.buckets[0].taken)
        assertEquals(1, history.buckets[1].taken)
    }

    // --- How far the records reach ----------------------------------------------------------

    @Test
    fun `records start is set when the oldest dose is later than the window first day`() {
        val earliest = at(day(9, 10))

        val history = summariser()(UsagePeriod.WEEK, listOf(dose(earliest, IntakeOutcome.TAKEN)), earliest)

        assertEquals(day(9, 10), history.recordsStartOn)
    }

    @Test
    fun `records start is absent when the records reach back further than the window`() {
        val earliest = at(day(1, 5))
        val doses = listOf(dose(at(day(9, 10)), IntakeOutcome.TAKEN))

        val history = summariser()(UsagePeriod.WEEK, doses, earliest)

        assertNull(history.recordsStartOn)
    }

    @Test
    fun `a medicine with gaps but older records shows no records-start note`() {
        val earliest = at(day(6, 1))
        val weekly = listOf(day(8, 24), day(8, 31), day(9, 7), day(9, 14))
            .map { dose(at(it), IntakeOutcome.TAKEN) }

        val history = summariser()(UsagePeriod.MONTH, weekly, earliest)

        assertNull(history.recordsStartOn)
        assertEquals(true, history.buckets.any { it.scheduled == 0 })
    }
}
