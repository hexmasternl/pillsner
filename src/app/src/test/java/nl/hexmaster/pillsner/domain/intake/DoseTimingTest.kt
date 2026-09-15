package nl.hexmaster.pillsner.domain.intake

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec: dose-detail, the early and late warnings (design D4). */
class DoseTimingTest {

    private val due: Instant = Instant.parse("2026-09-14T08:00:00Z")

    @Test
    fun `hours before its moment a dose is early`() {
        assertEquals(DoseTiming.EARLY, doseTiming(due, due.minus(Duration.ofHours(6))))
    }

    @Test
    fun `exactly an hour before is still early`() {
        assertEquals(DoseTiming.EARLY, doseTiming(due, due.minus(WARNING_MARGIN)))
    }

    @Test
    fun `one second inside the hour is on time`() {
        assertEquals(DoseTiming.ON_TIME, doseTiming(due, due.minus(WARNING_MARGIN).plusSeconds(1)))
    }

    @Test
    fun `at its moment a dose is on time`() {
        assertEquals(DoseTiming.ON_TIME, doseTiming(due, due))
    }

    @Test
    fun `five minutes late is on time`() {
        assertEquals(DoseTiming.ON_TIME, doseTiming(due, due.plus(Duration.ofMinutes(5))))
    }

    @Test
    fun `one second short of the hour after is on time`() {
        assertEquals(DoseTiming.ON_TIME, doseTiming(due, due.plus(WARNING_MARGIN).minusSeconds(1)))
    }

    @Test
    fun `exactly an hour after is late`() {
        assertEquals(DoseTiming.LATE, doseTiming(due, due.plus(WARNING_MARGIN)))
    }

    @Test
    fun `hours after its moment a dose is late`() {
        assertEquals(DoseTiming.LATE, doseTiming(due, due.plus(Duration.ofHours(6))))
    }

    @Test
    fun `an hour is an hour across a daylight-saving change`() {
        // The night the Dutch clocks go back: 03:00 local happens twice, so an hour of wall clock
        // and an hour of elapsed time disagree. The rule counts elapsed time.
        val amsterdam = ZoneId.of("Europe/Amsterdam")
        val backNight = LocalDate.of(2026, 10, 25)
        val scheduled = ZonedDateTime.of(backNight, LocalTime.of(3, 30), amsterdam).toInstant()

        assertEquals(DoseTiming.EARLY, doseTiming(scheduled, scheduled.minus(WARNING_MARGIN)))
        assertEquals(DoseTiming.ON_TIME, doseTiming(scheduled, scheduled.minus(Duration.ofMinutes(30))))
        assertEquals(DoseTiming.LATE, doseTiming(scheduled, scheduled.plus(WARNING_MARGIN)))
    }
}
