package nl.hexmaster.pillsner.domain.scheduling

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.TestFixtures.medication
import nl.hexmaster.pillsner.domain.model.TestFixtures.mg40
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec: dose-records generation, including the edge cases `CLAUDE.md` names — midnight, month
 * boundaries, leap day, both daylight-saving transitions and a time zone move.
 */
class DoseGeneratorTest {

    private val generator = DoseGenerator()
    private val amsterdam: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val tokyo: ZoneId = ZoneId.of("Asia/Tokyo")
    private val start = LocalDate.of(2026, 9, 14)

    @Test
    fun `twice a day produces two doses on each day of the window`() {
        val subject = medication(
            usedSince = start,
            schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(time(8), time(20)))),
        )

        val doses = generator.plan(subject, start..start.plusDays(1), amsterdam)

        assertEquals(
            listOf(at(start, 8), at(start, 20), at(start.plusDays(1), 8), at(start.plusDays(1), 20)),
            doses.map { it.scheduledAt },
        )
    }

    @Test
    fun `every other day is counted from the day the user started`() {
        val subject = medication(
            usedSince = start,
            schedules = listOf(Schedule.EveryNDays(mg40, 2, listOf(time(8)))),
        )

        val doses = generator.plan(subject, start..start.plusDays(3), amsterdam)

        assertEquals(listOf(at(start, 8), at(start.plusDays(2), 8)), doses.map { it.scheduledAt })
    }

    @Test
    fun `a weekdays schedule only produces doses on its own days`() {
        // 14 September 2026 is a Monday.
        val subject = medication(
            usedSince = start,
            schedules = listOf(
                Schedule.OnWeekdays(mg40, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), listOf(time(8))),
            ),
        )

        val doses = generator.plan(subject, start..start.plusDays(3), amsterdam)

        assertEquals(listOf(at(start, 8), at(start.plusDays(2), 8)), doses.map { it.scheduledAt })
    }

    @Test
    fun `every eight hours from seven produces three doses a day`() {
        val subject = medication(
            usedSince = start,
            schedules = listOf(Schedule.EveryNHours(mg40, 8, time(7))),
        )

        val doses = generator.plan(subject, start..start, amsterdam)

        assertEquals(listOf(at(start, 7), at(start, 15), at(start, 23)), doses.map { it.scheduledAt })
    }

    @Test
    fun `every twelve hours from twenty produces one dose that day`() {
        val subject = medication(
            usedSince = start,
            schedules = listOf(Schedule.EveryNHours(mg40, 12, time(20))),
        )

        val doses = generator.plan(subject, start..start, amsterdam)

        assertEquals(listOf(at(start, 20)), doses.map { it.scheduledAt })
    }

    @Test
    fun `an inactive medication produces nothing`() {
        val subject = medication(
            usedSince = start,
            isActive = false,
            schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(time(8)))),
        )

        assertEquals(emptyList<Instant>(), generator.plan(subject, start..start.plusDays(1), amsterdam))
    }

    @Test
    fun `a medication with no schedules produces nothing`() {
        val subject = medication(usedSince = start, schedules = emptyList())

        assertEquals(emptyList<Instant>(), generator.plan(subject, start..start.plusDays(1), amsterdam))
    }

    @Test
    fun `dates before used since produce nothing`() {
        val subject = medication(
            usedSince = start.plusDays(2),
            schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(time(8)))),
        )

        val doses = generator.plan(subject, start..start.plusDays(2), amsterdam)

        assertEquals(listOf(at(start.plusDays(2), 8)), doses.map { it.scheduledAt })
    }

    @Test
    fun `dates after use until produce nothing`() {
        val subject = medication(
            usedSince = start,
            useUntil = start,
            schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(time(8)))),
        )

        val doses = generator.plan(subject, start..start.plusDays(2), amsterdam)

        assertEquals(listOf(at(start, 8)), doses.map { it.scheduledAt })
    }

    @Test
    fun `two schedules on the same minute produce one dose, keeping the first amount`() {
        val twenty = Quantity.of("20", DoseUnit.MILLIGRAM)
        val subject = medication(
            usedSince = start,
            schedules = listOf(
                Schedule.EveryNDays(mg40, 1, listOf(time(8))),
                Schedule.OnWeekdays(twenty, setOf(DayOfWeek.MONDAY), listOf(time(8))),
            ),
        )

        val doses = generator.plan(subject, start..start, amsterdam)

        assertEquals(1, doses.size)
        assertEquals(mg40, doses.single().amount)
    }

    @Test
    fun `a window crossing a month boundary keeps counting days`() {
        val lastOfMonth = LocalDate.of(2026, 9, 30)
        val subject = medication(
            usedSince = lastOfMonth,
            schedules = listOf(Schedule.EveryNDays(mg40, 2, listOf(time(8)))),
        )

        val doses = generator.plan(subject, lastOfMonth..lastOfMonth.plusDays(2), amsterdam)

        assertEquals(
            listOf(at(lastOfMonth, 8), at(LocalDate.of(2026, 10, 2), 8)),
            doses.map { it.scheduledAt },
        )
    }

    @Test
    fun `a leap day is an ordinary day`() {
        val leapDay = LocalDate.of(2028, 2, 29)
        val subject = medication(
            usedSince = LocalDate.of(2028, 2, 28),
            schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(time(8)))),
        )

        val doses = generator.plan(subject, leapDay..leapDay, amsterdam)

        assertEquals(listOf(at(leapDay, 8)), doses.map { it.scheduledAt })
    }

    @Test
    fun `a dose at midnight belongs to the day that starts`() {
        val subject = medication(
            usedSince = start,
            schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.MIDNIGHT))),
        )

        val doses = generator.plan(subject, start..start, amsterdam)

        assertEquals(
            listOf(ZonedDateTime.of(start, LocalTime.MIDNIGHT, amsterdam).toInstant()),
            doses.map { it.scheduledAt },
        )
    }

    @Test
    fun `on the night the clock springs forward a dose inside the gap moves forward with it`() {
        // Europe/Amsterdam skips 02:00 to 03:00 on 29 March 2026.
        val springForward = LocalDate.of(2026, 3, 29)
        val subject = medication(
            usedSince = springForward,
            schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(2, 30)))),
        )

        val doses = generator.plan(subject, springForward..springForward, amsterdam)

        assertEquals(
            listOf(ZonedDateTime.of(springForward, LocalTime.of(3, 30), amsterdam).toInstant()),
            doses.map { it.scheduledAt },
        )
    }

    @Test
    fun `on the night the clock falls back a dose inside the overlap happens once, at the first offset`() {
        // Europe/Amsterdam repeats 02:00 to 03:00 on 25 October 2026.
        val fallBack = LocalDate.of(2026, 10, 25)
        val subject = medication(
            usedSince = fallBack,
            schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(2, 30)))),
        )

        val doses = generator.plan(subject, fallBack..fallBack, amsterdam)

        assertEquals(1, doses.size)
        assertEquals(
            ZonedDateTime.of(fallBack, LocalTime.of(2, 30), amsterdam).withEarlierOffsetAtOverlap().toInstant(),
            doses.single().scheduledAt,
        )
    }

    @Test
    fun `moving time zone keeps the wall-clock time and moves the instant`() {
        val subject = medication(
            usedSince = start,
            schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(time(8)))),
        )

        val here = generator.plan(subject, start..start, amsterdam).single().scheduledAt
        val there = generator.plan(subject, start..start, tokyo).single().scheduledAt

        assertEquals(ZonedDateTime.of(start, time(8), tokyo).toInstant(), there)
        assertEquals(true, there.isBefore(here))
    }

    @Test
    fun `the snapshot carries the medicine name and the schedule amount`() {
        val subject: Medication = medication(
            name = "Metoprolol",
            usedSince = start,
            schedules = listOf(Schedule.EveryNDays(Quantity.of("20", DoseUnit.MILLIGRAM), 1, listOf(time(8)))),
        )

        val dose = generator.plan(subject, start..start, amsterdam).single()

        assertEquals("Metoprolol", dose.medicationName)
        assertEquals(Quantity.of("20", DoseUnit.MILLIGRAM), dose.amount)
        assertEquals(subject.id, dose.medicationId)
    }

    private fun time(hour: Int, minute: Int = 0) = LocalTime.of(hour, minute)

    private fun at(date: LocalDate, hour: Int): Instant =
        ZonedDateTime.of(date, LocalTime.of(hour, 0), amsterdam).toInstant()
}
