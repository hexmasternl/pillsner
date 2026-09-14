package nl.hexmaster.pillsner.ui.medicines

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import nl.hexmaster.pillsner.R
import nl.hexmaster.pillsner.domain.model.Quantity
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.ScheduleSummary

/**
 * Turns a [ScheduleSummary] and the amount it applies to into the description shown on a medicine
 * tile: "40 mg twice a day", "2.5 ml every 8 hours", "500 mg as needed" (design D2).
 *
 * Every word comes from a string resource, so the description is translatable, and the amount and
 * the frequency are joined through a format string so a translator can reorder them.
 *
 * @param context resolves the string resources; an application or composition context both work.
 * @param locale decides the short weekday names and the day on which the week starts.
 */
class ScheduleDescriptionFormatter(
    private val context: Context,
    private val locale: Locale = Locale.getDefault(),
    private val quantityFormatter: QuantityFormatter = QuantityFormatter(context, locale),
) {

    /** The full line: the amount followed by the frequency. */
    fun describe(summary: ScheduleSummary, amount: Quantity): String = context.getString(
        R.string.schedule_with_amount,
        quantityFormatter.format(amount),
        frequency(summary),
    )

    /** The frequency alone, without an amount. */
    fun frequency(summary: ScheduleSummary): String = when (summary) {
        is ScheduleSummary.TimesPerDay -> countString(
            count = summary.count,
            one = R.string.schedule_once_a_day,
            two = R.string.schedule_twice_a_day,
            many = R.string.schedule_n_times_a_day,
        )

        is ScheduleSummary.TimesPerDayOnDays -> {
            val days = describeDays(summary.days)
            when (summary.count) {
                1 -> context.getString(R.string.schedule_once_a_day_on_days, days)
                2 -> context.getString(R.string.schedule_twice_a_day_on_days, days)
                else -> context.getString(R.string.schedule_n_times_a_day_on_days, summary.count, days)
            }
        }

        is ScheduleSummary.TimesEveryOtherDay -> countString(
            count = summary.count,
            one = R.string.schedule_once_every_other_day,
            two = R.string.schedule_twice_every_other_day,
            many = R.string.schedule_n_times_every_other_day,
        )

        is ScheduleSummary.TimesEveryNDays -> when (summary.count) {
            1 -> context.getString(R.string.schedule_once_every_n_days, summary.days)
            2 -> context.getString(R.string.schedule_twice_every_n_days, summary.days)
            else -> context.getString(R.string.schedule_n_times_every_n_days, summary.count, summary.days)
        }

        is ScheduleSummary.EveryNHours -> context.getString(R.string.schedule_every_n_hours, summary.hours)
        ScheduleSummary.AsNeeded -> context.getString(R.string.schedule_as_needed)
    }

    private fun countString(count: Int, one: Int, two: Int, many: Int): String = when (count) {
        1 -> context.getString(one)
        2 -> context.getString(two)
        else -> context.getString(many, count)
    }

    /**
     * Monday to Friday exactly reads as one word; anything else is the locale's short day names in
     * the locale's own week order, joined with the list separator.
     */
    private fun describeDays(days: Set<DayOfWeek>): String {
        if (days == WORKING_WEEK) return context.getString(R.string.schedule_days_weekdays)
        val separator = context.getString(R.string.list_separator)
        return orderedWeek(locale)
            .filter { it in days }
            .joinToString(separator) { it.getDisplayName(TextStyle.SHORT, locale) }
    }

    companion object {
        val WORKING_WEEK: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )

        /** The seven days starting at the locale's first day of the week. */
        fun orderedWeek(locale: Locale): List<DayOfWeek> {
            val first = WeekFields.of(locale).firstDayOfWeek
            return (0 until Schedule.DAYS_IN_WEEK).map { first.plus(it.toLong()) }
        }
    }
}

/** Remembers a formatter for the composition's context and locale. */
@Composable
fun rememberScheduleDescriptionFormatter(): ScheduleDescriptionFormatter {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(context, locale) { ScheduleDescriptionFormatter(context, locale) }
}

/** Remembers a quantity formatter for the composition's context and locale. */
@Composable
fun rememberQuantityFormatter(): QuantityFormatter {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(context, locale) { QuantityFormatter(context, locale) }
}
