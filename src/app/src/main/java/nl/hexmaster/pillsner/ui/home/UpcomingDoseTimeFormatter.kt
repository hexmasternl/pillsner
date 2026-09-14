package nl.hexmaster.pillsner.ui.home

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** The scheduled time of a dose as the tile shows it. */
data class FormattedDoseTime(
    /** Locale-formatted time of day, for example "20:00" or "8:00 PM". */
    val time: String,
    /** Which day the dose falls on, or null when it is today. */
    val day: DayLabel?,
)

/** How a tile names a day other than today. */
sealed interface DayLabel {
    /** The next calendar day; the UI renders it from a string resource. */
    data object Tomorrow : DayLabel

    /** Any later day, already formatted for the locale: a weekday name within the week, a date beyond it. */
    data class Text(val value: String) : DayLabel
}

/**
 * Formats a dose's [Instant] for the device time zone and locale (docs/design-system.md section 3.3).
 *
 * "Today" is decided in [zoneId] from [clock]: a dose at 00:00 tomorrow gets the Tomorrow label even
 * when it is one minute away, and a dose at 23:59 today never does. Both are injectable so unit
 * tests can pin the boundary; production uses the defaults.
 */
class UpcomingDoseTimeFormatter(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val locale: Locale = Locale.getDefault(),
    private val clock: Clock = Clock.system(zoneId),
) {
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)

    fun format(scheduledAt: Instant): FormattedDoseTime {
        val scheduled = scheduledAt.atZone(zoneId)
        val today = clock.instant().atZone(zoneId).toLocalDate()
        val daysAhead = ChronoUnit.DAYS.between(today, scheduled.toLocalDate())

        val day = when {
            daysAhead <= 0L -> null
            daysAhead == 1L -> DayLabel.Tomorrow
            daysAhead < DAYS_IN_WEEK -> DayLabel.Text(scheduled.dayOfWeek.getDisplayName(TextStyle.FULL, locale))
            else -> DayLabel.Text(dateFormatter.format(scheduled))
        }
        return FormattedDoseTime(time = timeFormatter.format(scheduled), day = day)
    }

    private companion object {
        const val DAYS_IN_WEEK = 7L
    }
}
