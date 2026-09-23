package nl.hexmaster.pillsner.domain.stock

import java.time.LocalDate
import java.time.ZoneId
import nl.hexmaster.pillsner.domain.model.DoseUnit
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.TestFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Spec: medicine-stock-tracking, "Weekly usage projection". */
class ProjectWeeklyUsageTest {

    private val zone: ZoneId = ZoneId.of("Europe/Amsterdam")
    private val from: LocalDate = LocalDate.of(2026, 9, 14)
    private val projectWeeklyUsage = ProjectWeeklyUsage()

    @Test
    fun `twice-daily medicine projects 14 tablets over 7 days`() {
        val medication = TestFixtures.medication(
            defaultDose = TestFixtures.oneTablet,
            usedSince = from,
            schedules = listOf(
                Schedule.EveryNDays(TestFixtures.oneTablet, intervalDays = 1, times = listOf(TestFixtures.time(8), TestFixtures.time(20))),
            ),
        )

        val usage = projectWeeklyUsage.forMedication(medication, from, zone)

        assertEquals(Quantity(java.math.BigDecimal("14"), DoseUnit.TABLET), usage)
    }

    @Test
    fun `weekday-only medicine counts only the weekdays inside the window`() {
        val medication = TestFixtures.medication(
            defaultDose = TestFixtures.oneTablet,
            usedSince = from,
            schedules = listOf(
                Schedule.OnWeekdays(
                    TestFixtures.oneTablet,
                    days = setOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY),
                    times = listOf(TestFixtures.time(8)),
                ),
            ),
        )

        // 14 September 2026 is a Monday; the 7-day window covers Mon-Sun, containing each of
        // Mon/Wed/Fri exactly once.
        val usage = projectWeeklyUsage.forMedication(medication, from, zone)

        assertEquals(Quantity(java.math.BigDecimal("3"), DoseUnit.TABLET), usage)
    }

    @Test
    fun `an as-needed medicine has no projection`() {
        val medication = TestFixtures.medication(schedules = emptyList())

        assertNull(projectWeeklyUsage.forMedication(medication, from, zone))
    }

    @Test
    fun `a schedule in a different unit than the default dose is not counted`() {
        val medication = TestFixtures.medication(
            defaultDose = TestFixtures.mg40,
            usedSince = from,
            schedules = listOf(
                Schedule.EveryNDays(TestFixtures.oneTablet, intervalDays = 1, times = listOf(TestFixtures.time(8))),
            ),
        )

        assertNull(
            "The schedule's tablet amount cannot usefully add to a stock tracked in milligrams",
            projectWeeklyUsage.forMedication(medication, from, zone),
        )
    }
}
