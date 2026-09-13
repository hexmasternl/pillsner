package nl.hexmaster.pillsner.domain.scheduling

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import nl.hexmaster.pillsner.domain.model.Medication
import nl.hexmaster.pillsner.domain.model.PlannedDose
import nl.hexmaster.pillsner.domain.model.Schedule

/**
 * Turns a medication's schedules into the moments its doses are due (design D2).
 *
 * Every schedule is a wall-clock rule, so the generator enumerates *dates* in the device's zone and
 * turns each date and time into an instant independently. That is what makes a time zone change or
 * a daylight-saving transition a matter of regenerating the window rather than of arithmetic on
 * instants.
 *
 * Daylight saving, which `java.time` decides and this class deliberately does not second-guess:
 * - **Spring forward.** On the night a zone skips an hour, a dose at a time inside the gap moves
 *   forward by the length of the gap: 02:30 becomes 03:30. The dose happens once, an hour later on
 *   the clock than usual.
 * - **Autumn overlap.** On the night a zone repeats an hour, a dose at a time inside the overlap
 *   takes the *earlier* of the two offsets, so it happens once, at the first occurrence of that
 *   wall-clock time.
 *
 * Both match what a bedside alarm clock does, which is what a person expects of a reminder.
 */
class DoseGenerator {

    /**
     * The doses [medication] should produce on every date in [window], as instants in [zone].
     *
     * An inactive medication produces nothing, and neither does a date before the medication's
     * used-since day or after its use-until day. Two schedules of one medicine that land on the
     * same minute produce one dose, keeping the first schedule's amount.
     *
     * @return the planned doses, ascending by moment.
     */
    fun plan(
        medication: Medication,
        window: ClosedRange<LocalDate>,
        zone: ZoneId,
    ): List<PlannedDose> {
        if (!medication.isActive || medication.schedules.isEmpty()) return emptyList()

        val planned = LinkedHashMap<java.time.Instant, PlannedDose>()

        var date = window.start
        while (!date.isAfter(window.endInclusive)) {
            if (medication.covers(date)) {
                medication.schedules.forEach { schedule ->
                    schedule.timesOn(date, medication.usedSince).forEach { time ->
                        val at = ZonedDateTime.of(date, time, zone).toInstant()
                        planned.putIfAbsent(
                            at,
                            PlannedDose(
                                medicationId = medication.id,
                                medicationName = medication.name,
                                amount = schedule.amount,
                                scheduledAt = at,
                            ),
                        )
                    }
                }
            }
            date = date.plusDays(1)
        }

        return planned.values.sortedBy { it.scheduledAt }
    }

    /** Whether [date] falls inside the days the user takes this medication. */
    private fun Medication.covers(date: LocalDate): Boolean =
        !date.isBefore(usedSince) && (useUntil == null || !date.isAfter(useUntil))

    /** The clock times this schedule produces on [date], counting intervals from [anchor]. */
    private fun Schedule.timesOn(date: LocalDate, anchor: LocalDate): List<LocalTime> = when (this) {
        is Schedule.EveryNDays ->
            if (ChronoUnit.DAYS.between(anchor, date) % intervalDays == 0L) times else emptyList()

        is Schedule.OnWeekdays -> if (date.dayOfWeek in days) times else emptyList()

        is Schedule.EveryNHours -> dailyDoseTimes()
    }
}
