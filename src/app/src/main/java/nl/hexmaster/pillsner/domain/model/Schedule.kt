package nl.hexmaster.pillsner.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * The rule that says when doses of a medication are due, and how much each dose is.
 *
 * Every shape is a *wall-clock* rule: "08:00 every day" stays 08:00 when the device moves time
 * zone or crosses a daylight-saving boundary. Turning a rule into absolute instants is the dose
 * generator's job, not this model's.
 *
 * A medication holds a list of these. There is deliberately no as-needed shape: a medication with
 * an empty schedule list is the as-needed case, so the two can never contradict each other
 * (design D1).
 */
sealed interface Schedule {

    /** How much one dose of this schedule is. */
    val amount: Quantity

    /**
     * [times] on every day that is a whole multiple of [intervalDays] after the medication's
     * used-since date. Interval 1 with two times is "twice a day"; interval 2 with one time is
     * "once every other day".
     *
     * @throws IllegalArgumentException when [intervalDays] is outside 1..[MAX_INTERVAL_DAYS] or
     *   [times] is empty.
     */
    data class EveryNDays(
        override val amount: Quantity,
        val intervalDays: Int,
        val times: List<LocalTime>,
    ) : Schedule {
        init {
            require(intervalDays in 1..MAX_INTERVAL_DAYS) {
                "An every-N-days schedule needs an interval of 1 to $MAX_INTERVAL_DAYS days"
            }
            require(times.isNotEmpty()) { "An every-N-days schedule needs at least one time" }
        }
    }

    /**
     * [times] on each of [days].
     *
     * [days] must be a strict, non-empty subset of the week: all seven days is [EveryNDays] with
     * an interval of one, and allowing both would give the same rhythm two representations.
     *
     * @throws IllegalArgumentException when [days] is empty or holds all seven days, or [times] is
     *   empty.
     */
    data class OnWeekdays(
        override val amount: Quantity,
        val days: Set<DayOfWeek>,
        val times: List<LocalTime>,
    ) : Schedule {
        init {
            require(days.isNotEmpty()) { "A weekdays schedule needs at least one day" }
            require(days.size < DAYS_IN_WEEK) {
                "A weekdays schedule on all seven days is every-N-days with an interval of one"
            }
            require(times.isNotEmpty()) { "A weekdays schedule needs at least one time" }
        }
    }

    /**
     * One dose at [firstDoseAt] and then every [intervalHours] hours, restarting each day. Every 12
     * hours from 08:00 is 08:00 and 20:00; every 8 hours from 07:00 is 07:00, 15:00 and 23:00.
     *
     * Restarting daily keeps this a wall-clock rule like the others, which is what makes it survive
     * a daylight-saving change. It cannot express rhythms longer than a day, such as every 36
     * hours; [EveryNDays] covers those.
     *
     * @throws IllegalArgumentException when [intervalHours] is outside 1..24.
     */
    data class EveryNHours(
        override val amount: Quantity,
        val intervalHours: Int,
        val firstDoseAt: LocalTime,
    ) : Schedule {
        init {
            require(intervalHours in 1..HOURS_IN_DAY) {
                "An every-N-hours schedule needs an interval of 1 to $HOURS_IN_DAY hours"
            }
        }

        /** The clock times this schedule produces on one day, in ascending order. */
        fun dailyDoseTimes(): List<LocalTime> {
            val times = mutableListOf(firstDoseAt)
            var hoursFromFirst = intervalHours
            while (firstDoseAt.hour + hoursFromFirst < HOURS_IN_DAY) {
                times += firstDoseAt.plusHours(hoursFromFirst.toLong())
                hoursFromFirst += intervalHours
            }
            return times
        }
    }

    companion object {
        /** The longest interval the editor offers, and the model accepts, for every-N-days. */
        const val MAX_INTERVAL_DAYS = 30
        const val HOURS_IN_DAY = 24
        const val DAYS_IN_WEEK = 7
    }
}
