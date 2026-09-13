package nl.hexmaster.pillsner.domain.model

import java.time.DayOfWeek
import nl.hexmaster.pillsner.domain.model.TestFixtures.medication
import nl.hexmaster.pillsner.domain.model.TestFixtures.mg40
import nl.hexmaster.pillsner.domain.model.TestFixtures.time
import org.junit.Assert.assertEquals
import org.junit.Test

/** The collapsing rules of [summarize] (spec: Schedule summary). */
class ScheduleSummaryTest {

    @Test
    fun twoDistinctTimesEveryDay_isTwiceADay() {
        val schedule = Schedule.EveryNDays(mg40, 1, listOf(time(8), time(20)))

        assertEquals(ScheduleSummary.TimesPerDay(2), schedule.summarize())
    }

    @Test
    fun duplicateTimes_countOnce() {
        val schedule = Schedule.EveryNDays(mg40, 1, listOf(time(8), time(8), time(20)))

        assertEquals(ScheduleSummary.TimesPerDay(2), schedule.summarize())
    }

    @Test
    fun oneTimeOnThreeDays_isOncePerDayOnThoseDays() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val schedule = Schedule.OnWeekdays(mg40, days, listOf(time(8)))

        assertEquals(ScheduleSummary.TimesPerDayOnDays(1, days), schedule.summarize())
    }

    @Test
    fun twoTimesEveryTwoDays_isTwiceEveryOtherDay() {
        val schedule = Schedule.EveryNDays(mg40, 2, listOf(time(8), time(20)))

        assertEquals(ScheduleSummary.TimesEveryOtherDay(2), schedule.summarize())
    }

    @Test
    fun oneTimeEveryThreeDays_keepsTheInterval() {
        val schedule = Schedule.EveryNDays(mg40, 3, listOf(time(8)))

        assertEquals(ScheduleSummary.TimesEveryNDays(1, 3), schedule.summarize())
    }

    @Test
    fun every24Hours_collapsesToOnceADay() {
        val schedule = Schedule.EveryNHours(mg40, 24, time(9))

        assertEquals(ScheduleSummary.TimesPerDay(1), schedule.summarize())
    }

    @Test
    fun every8Hours_keepsTheInterval() {
        val schedule = Schedule.EveryNHours(mg40, 8, time(7))

        assertEquals(ScheduleSummary.EveryNHours(8), schedule.summarize())
    }

    @Test
    fun aMedicationWithoutSchedules_summarizesAsNeeded() {
        assertEquals(listOf(ScheduleSummary.AsNeeded), medication().summarizeSchedules())
    }

    @Test
    fun aMedicationWithTwoSchedules_summarizesBothInOrder() {
        val subject = medication(
            schedules = listOf(
                Schedule.EveryNHours(mg40, 12, time(8)),
                Schedule.OnWeekdays(mg40, setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), listOf(time(10))),
            ),
        )

        assertEquals(
            listOf(
                ScheduleSummary.EveryNHours(12),
                ScheduleSummary.TimesPerDayOnDays(1, setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
            ),
            subject.summarizeSchedules(),
        )
    }
}
