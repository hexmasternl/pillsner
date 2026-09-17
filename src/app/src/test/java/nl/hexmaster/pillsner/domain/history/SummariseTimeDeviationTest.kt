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
 * The per-dose deviation, the bucket and period averages, and the timing accuracy chart's own
 * bucketing rule, all pure so the mid-week window starts and the different-from-the-outcome-chart
 * bucketing are tested rather than hoped for (medicine-history-time-deviation design D1-D3).
 */
class SummariseTimeDeviationTest {

    private val amsterdam: ZoneId = ZoneId.of("Europe/Amsterdam")

    /** Monday 14 September 2026, lunchtime: doses later today are still to come. */
    private val noon: Instant =
        ZonedDateTime.of(LocalDate.of(2026, 9, 14), LocalTime.NOON, amsterdam).toInstant()

    private var nextId = 1L

    private fun summariser(
        now: Instant = noon,
        weekStart: DayOfWeek = DayOfWeek.MONDAY,
    ) = SummariseTimeDeviation(MutableTestClock(now, amsterdam)) { weekStart }

    private fun at(date: LocalDate, hour: Int = 8, minute: Int = 0): Instant =
        ZonedDateTime.of(date, LocalTime.of(hour, minute), amsterdam).toInstant()

    private fun day(month: Int, dayOfMonth: Int, year: Int = 2026): LocalDate =
        LocalDate.of(year, month, dayOfMonth)

    private fun takenDose(scheduledAt: Instant, recordedAt: Instant) = Dose(
        id = DoseId(nextId++),
        medicationId = MedicationId(1),
        medicationName = "Ibuprofen",
        amount = mg40,
        scheduledAt = scheduledAt,
        intake = Intake(IntakeOutcome.TAKEN, recordedAt),
    )

    private fun unansweredDose(outcome: IntakeOutcome, scheduledAt: Instant) = Dose(
        id = DoseId(nextId++),
        medicationId = MedicationId(1),
        medicationName = "Ibuprofen",
        amount = mg40,
        scheduledAt = scheduledAt,
        intake = Intake(outcome, scheduledAt),
    )

    // --- Per-dose deviation -------------------------------------------------------------------

    @Test
    fun `taken late gives a positive number of minutes`() {
        val scheduled = at(day(9, 10), hour = 8, minute = 0)
        val dose = takenDose(scheduled, at(day(9, 10), hour = 8, minute = 13))

        val history = summariser()(UsagePeriod.WEEK, listOf(dose))

        assertEquals(13, history.averageMinutes)
    }

    @Test
    fun `taken early also gives a positive number of minutes`() {
        val scheduled = at(day(9, 10), hour = 8, minute = 0)
        val dose = takenDose(scheduled, at(day(9, 10), hour = 7, minute = 53))

        val history = summariser()(UsagePeriod.WEEK, listOf(dose))

        assertEquals(7, history.averageMinutes)
    }

    @Test
    fun `skipped and missed doses contribute no deviation`() {
        val doses = listOf(
            unansweredDose(IntakeOutcome.SKIPPED, at(day(9, 10))),
            unansweredDose(IntakeOutcome.MISSED, at(day(9, 11))),
        )

        val history = summariser()(UsagePeriod.WEEK, doses)

        assertNull(history.averageMinutes)
        assertEquals(true, history.buckets.all { it.takenCount == 0 })
    }

    // --- Bucket and period averages ------------------------------------------------------------

    @Test
    fun `a bucket average is the mean of its taken doses' deviations`() {
        val day = day(9, 10)
        val doses = listOf(
            takenDose(at(day, 8, 0), at(day, 8, 10)),
            takenDose(at(day, 12, 0), at(day, 12, 14)),
            takenDose(at(day, 20, 0), at(day, 20, 6)),
        )

        val history = summariser()(UsagePeriod.WEEK, doses)

        val bucket = history.buckets.first { it.start == day }
        assertEquals(10, bucket.averageMinutes)
        assertEquals(3, bucket.takenCount)
    }

    @Test
    fun `a bucket with only skipped or missed doses has no average`() {
        val doses = listOf(
            unansweredDose(IntakeOutcome.SKIPPED, at(day(9, 12))),
            unansweredDose(IntakeOutcome.MISSED, at(day(9, 12), hour = 20)),
        )

        val history = summariser()(UsagePeriod.WEEK, doses)

        val bucket = history.buckets.first { it.start == day(9, 12) }
        assertNull(bucket.averageMinutes)
        assertEquals(0, bucket.takenCount)
    }

    @Test
    fun `a period with no taken dose anywhere has no overall average`() {
        val doses = listOf(unansweredDose(IntakeOutcome.MISSED, at(day(9, 10))))

        val history = summariser()(UsagePeriod.WEEK, doses)

        assertNull(history.averageMinutes)
        assertEquals(true, history.isEmpty)
    }

    // --- Bucketing rule (different from the outcome chart's) ----------------------------------

    @Test
    fun `one week is bucketed by day`() {
        val history = summariser()(UsagePeriod.WEEK, emptyList())

        assertEquals(7, history.buckets.size)
        assertEquals(List(7) { false }, history.buckets.map { it.isWeek })
    }

    @Test
    fun `one month is bucketed by day, unlike the outcome chart's weekly buckets`() {
        val history = summariser()(UsagePeriod.MONTH, emptyList())

        // 15 August 2026 to 14 September 2026 is 31 days.
        assertEquals(31, history.buckets.size)
        assertEquals(List(31) { false }, history.buckets.map { it.isWeek })
    }

    @Test
    fun `three months is bucketed by week, the earliest starting on the window's first day`() {
        val history = summariser()(UsagePeriod.THREE_MONTHS, emptyList())

        assertEquals(day(6, 15), history.firstDay)
        assertEquals(day(6, 15), history.buckets.first().start)
        assertEquals(true, history.buckets.size in 13..14)
        assertEquals(true, history.buckets.all { it.isWeek })
    }

    @Test
    fun `a dose lands in the week bucket it belongs to for three months`() {
        val doses = listOf(
            // 15 June 2026 is a Monday, so the window opens on a week boundary and there is no stub.
            takenDose(at(day(6, 15)), at(day(6, 15), hour = 8, minute = 5)),
            takenDose(at(day(6, 22)), at(day(6, 22), hour = 8, minute = 20)),
        )

        val history = summariser()(UsagePeriod.THREE_MONTHS, doses)

        assertEquals(5, history.buckets[0].averageMinutes)
        assertEquals(20, history.buckets[1].averageMinutes)
    }
}
