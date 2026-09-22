package nl.hexmaster.pillsner.domain.history

import java.time.DayOfWeek
import java.time.Duration
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
 * The timing-accuracy arithmetic: which doses contribute, how a deviation is measured, and how it
 * is bucketed - pure so a leap day or a clock change never has to be re-checked by eye
 * (medicine-history-time-deviation design D2, D3).
 */
class SummariseTimeDeviationTest {

    private val amsterdam: ZoneId = ZoneId.of("Europe/Amsterdam")

    /** Monday 14 September 2026, lunchtime: doses later today are still to come. */
    private val noon: Instant =
        ZonedDateTime.of(LocalDate.of(2026, 9, 14), LocalTime.NOON, amsterdam).toInstant()

    private var nextId = 1L

    private fun summariser(now: Instant = noon, weekStart: DayOfWeek = DayOfWeek.MONDAY) =
        SummariseTimeDeviation(MutableTestClock(now, amsterdam)) { weekStart }

    private fun taken(at: Instant, recordedAt: Instant) = Dose(
        id = DoseId(nextId++),
        medicationId = MedicationId(1),
        medicationName = "Ibuprofen",
        amount = mg40,
        scheduledAt = at,
        intake = Intake(IntakeOutcome.TAKEN, recordedAt),
    )

    private fun unresolved(at: Instant, outcome: IntakeOutcome? = null) = Dose(
        id = DoseId(nextId++),
        medicationId = MedicationId(1),
        medicationName = "Ibuprofen",
        amount = mg40,
        scheduledAt = at,
        intake = outcome?.let { Intake(it, at) },
    )

    private fun at(date: LocalDate, hour: Int = 8): Instant =
        ZonedDateTime.of(date, LocalTime.of(hour, 0), amsterdam).toInstant()

    private fun day(month: Int, dayOfMonth: Int, year: Int = 2026): LocalDate = LocalDate.of(year, month, dayOfMonth)

    @Test
    fun `early and late contribute the same positive minutes`() {
        val early = at(day(9, 10))
        val late = at(day(9, 11))
        val doses = listOf(
            taken(early, early.minus(Duration.ofMinutes(5))),
            taken(late, late.plus(Duration.ofMinutes(5))),
        )

        val history = summariser()(UsagePeriod.WEEK, doses)

        assertEquals(5, history.averageMinutes)
    }

    @Test
    fun `skipped and missed doses are excluded entirely`() {
        val takenDose = taken(at(day(9, 10)), at(day(9, 10)).plus(Duration.ofMinutes(10)))
        val doses = listOf(
            takenDose,
            unresolved(at(day(9, 11)), IntakeOutcome.SKIPPED),
            unresolved(at(day(9, 12)), IntakeOutcome.MISSED),
        )

        val history = summariser()(UsagePeriod.WEEK, doses)

        assertEquals(10, history.averageMinutes)
        assertEquals(1, history.buckets.sumOf { it.takenCount })
    }

    @Test
    fun `a bucket with nothing taken is null not zero`() {
        val doses = listOf(unresolved(at(day(9, 10)), IntakeOutcome.MISSED))

        val history = summariser()(UsagePeriod.WEEK, doses)

        assertNull(history.buckets.first { it.start == day(9, 10) }.averageMinutes)
        assertEquals(0, history.buckets.first { it.start == day(9, 10) }.takenCount)
    }

    @Test
    fun `nothing taken anywhere in the period gives a null overall average`() {
        val doses = listOf(unresolved(at(day(9, 10)), IntakeOutcome.SKIPPED))

        val history = summariser()(UsagePeriod.WEEK, doses)

        assertNull(history.averageMinutes)
    }

    @Test
    fun `an entirely empty period also gives a null overall average`() {
        val history = summariser()(UsagePeriod.WEEK, emptyList())

        assertNull(history.averageMinutes)
        assertEquals(7, history.buckets.size)
    }

    @Test
    fun `a week and a month both bucket per day`() {
        val weekHistory = summariser()(UsagePeriod.WEEK, emptyList())
        val monthHistory = summariser()(UsagePeriod.MONTH, emptyList())

        assertEquals(true, weekHistory.buckets.none { it.isWeek })
        assertEquals(true, monthHistory.buckets.none { it.isWeek })
    }

    @Test
    fun `three months buckets per week`() {
        val history = summariser()(UsagePeriod.THREE_MONTHS, emptyList())

        assertEquals(true, history.buckets.all { it.isWeek })
        assertEquals(true, history.buckets.size in 13..14)
    }

    @Test
    fun `deviation rounds to the nearest minute rather than truncating`() {
        val scheduled = at(day(9, 10))
        // 90 seconds late rounds to the nearest minute (1.5 -> 2), not truncates to 1.
        val doses = listOf(taken(scheduled, scheduled.plus(Duration.ofSeconds(90))))

        val history = summariser()(UsagePeriod.WEEK, doses)

        assertEquals(2, history.averageMinutes)
    }

    @Test
    fun `a dose taken exactly on time deviates by zero minutes`() {
        val scheduled = at(day(9, 10))
        val doses = listOf(taken(scheduled, scheduled))

        val history = summariser()(UsagePeriod.WEEK, doses)

        assertEquals(0, history.averageMinutes)
    }

    @Test
    fun `a dose due later today does not contribute`() {
        val doses = listOf(taken(at(day(9, 14), hour = 20), at(day(9, 14), hour = 20)))

        val history = summariser()(UsagePeriod.WEEK, doses)

        assertNull(history.averageMinutes)
    }
}
