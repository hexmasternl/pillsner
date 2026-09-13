package nl.hexmaster.pillsner.domain.model

import java.time.DayOfWeek

/**
 * A [Schedule] reduced to the shape a human-readable description needs (design D2).
 *
 * The domain owns the collapsing rules ("every 24 hours" is once a day); the UI layer owns the
 * wording, because wording lives in string resources and must be translatable.
 */
sealed interface ScheduleSummary {

    /** [count] doses on every day of the week. */
    data class TimesPerDay(val count: Int) : ScheduleSummary

    /** [count] doses on each of [days], which is always a strict subset of the week. */
    data class TimesPerDayOnDays(val count: Int, val days: Set<DayOfWeek>) : ScheduleSummary

    /** [count] doses every second day. */
    data class TimesEveryOtherDay(val count: Int) : ScheduleSummary

    /** [count] doses every [days] days, where [days] is three or more. */
    data class TimesEveryNDays(val count: Int, val days: Int) : ScheduleSummary

    /** One dose every [hours] hours, where [hours] is anything but 24. */
    data class EveryNHours(val hours: Int) : ScheduleSummary

    /** No schedule: the medication is taken when the user needs it. */
    data object AsNeeded : ScheduleSummary
}

/**
 * Reduces this schedule to a [ScheduleSummary], collapsing schedules that mean the same thing:
 * every one day is simply daily, every two days is every other day, every 24 hours is once a day.
 * Duplicate times count once.
 */
fun Schedule.summarize(): ScheduleSummary = when (this) {
    is Schedule.EveryNDays -> {
        val count = times.distinct().size
        when (intervalDays) {
            1 -> ScheduleSummary.TimesPerDay(count)
            2 -> ScheduleSummary.TimesEveryOtherDay(count)
            else -> ScheduleSummary.TimesEveryNDays(count, intervalDays)
        }
    }

    is Schedule.OnWeekdays -> ScheduleSummary.TimesPerDayOnDays(times.distinct().size, days)

    is Schedule.EveryNHours -> when (intervalHours) {
        Schedule.HOURS_IN_DAY -> ScheduleSummary.TimesPerDay(1)
        else -> ScheduleSummary.EveryNHours(intervalHours)
    }
}

/**
 * The summaries of this medication's schedules, in order. A medication with no schedules yields a
 * single [ScheduleSummary.AsNeeded], which the UI pairs with the default dose.
 */
fun Medication.summarizeSchedules(): List<ScheduleSummary> =
    if (schedules.isEmpty()) listOf(ScheduleSummary.AsNeeded) else schedules.map { it.summarize() }
