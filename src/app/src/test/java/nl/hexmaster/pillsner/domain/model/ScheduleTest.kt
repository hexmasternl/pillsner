package nl.hexmaster.pillsner.domain.model

import java.time.DayOfWeek
import java.time.LocalTime
import nl.hexmaster.pillsner.domain.model.TestFixtures.mg40
import nl.hexmaster.pillsner.domain.model.TestFixtures.time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Construction rules of [Schedule] (spec: Schedule shapes). */
class ScheduleTest {

    @Test
    fun everyNDays_withTwoTimesADay_isValid() {
        val schedule = Schedule.EveryNDays(mg40, intervalDays = 1, times = listOf(time(8), time(20)))

        assertEquals(2, schedule.times.size)
        assertEquals(mg40, schedule.amount)
    }

    @Test
    fun everyNDays_withZeroInterval_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Schedule.EveryNDays(mg40, intervalDays = 0, times = listOf(time(8)))
        }
    }

    @Test
    fun everyNDays_beyondTheLongestInterval_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Schedule.EveryNDays(mg40, intervalDays = Schedule.MAX_INTERVAL_DAYS + 1, times = listOf(time(8)))
        }
    }

    @Test
    fun everyNDays_withoutTimes_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Schedule.EveryNDays(mg40, intervalDays = 1, times = emptyList())
        }
    }

    @Test
    fun onWeekdays_withASubsetOfDays_isValid() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)

        val schedule = Schedule.OnWeekdays(mg40, days = days, times = listOf(time(8)))

        assertEquals(days, schedule.days)
    }

    @Test
    fun onWeekdays_withAllSevenDays_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Schedule.OnWeekdays(mg40, days = DayOfWeek.entries.toSet(), times = listOf(time(8)))
        }
    }

    @Test
    fun onWeekdays_withoutDays_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Schedule.OnWeekdays(mg40, days = emptySet(), times = listOf(time(8)))
        }
    }

    @Test
    fun onWeekdays_withoutTimes_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Schedule.OnWeekdays(mg40, days = setOf(DayOfWeek.MONDAY), times = emptyList())
        }
    }

    @Test
    fun everyNHours_withZeroInterval_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Schedule.EveryNHours(mg40, intervalHours = 0, firstDoseAt = time(8))
        }
    }

    @Test
    fun everyNHours_beyondADay_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Schedule.EveryNHours(mg40, intervalHours = 36, firstDoseAt = time(8))
        }
    }

    @Test
    fun everyTwelveHoursFromEight_hasTwoDailyDoseTimes() {
        val schedule = Schedule.EveryNHours(mg40, intervalHours = 12, firstDoseAt = time(8))

        assertEquals(listOf<LocalTime>(time(8), time(20)), schedule.dailyDoseTimes())
    }

    @Test
    fun everyEightHoursFromSeven_hasThreeDailyDoseTimes() {
        val schedule = Schedule.EveryNHours(mg40, intervalHours = 8, firstDoseAt = time(7))

        assertEquals(listOf<LocalTime>(time(7), time(15), time(23)), schedule.dailyDoseTimes())
    }

    @Test
    fun everyTwentyFourHours_hasOneDailyDoseTime() {
        val schedule = Schedule.EveryNHours(mg40, intervalHours = 24, firstDoseAt = time(9))

        assertEquals(listOf<LocalTime>(time(9)), schedule.dailyDoseTimes())
    }

    @Test
    fun aScheduleLateInTheDay_doesNotWrapPastMidnight() {
        val schedule = Schedule.EveryNHours(mg40, intervalHours = 8, firstDoseAt = time(17))

        assertEquals(listOf<LocalTime>(time(17)), schedule.dailyDoseTimes())
    }

    @Test
    fun aNonPositiveAmount_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Schedule.EveryNDays(Quantity.of("0", DoseUnit.MILLIGRAM), intervalDays = 1, times = listOf(time(8)))
        }
    }
}
