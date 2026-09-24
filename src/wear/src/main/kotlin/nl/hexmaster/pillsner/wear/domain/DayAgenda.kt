package nl.hexmaster.pillsner.wear.domain

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import nl.hexmaster.pillsner.shared.wear.SyncedDose

/** One of the two days the watch shows (`wear-day-overview` design D1). */
enum class AgendaDay {
    TODAY,
    TOMORROW,
}

/**
 * Every dose due at one moment, so a morning of three medicines reads as one time with three
 * medicines under it rather than the same time written three times.
 */
data class AgendaTimeGroup(val scheduledAt: Instant, val doses: List<SyncedDose>)

/** One day of the agenda, its times ascending. */
data class AgendaSection(val day: AgendaDay, val groups: List<AgendaTimeGroup>)

/**
 * Turns the list the phone sent into the agenda on the wrist (`wear-day-overview` design D1). Free
 * of Android imports, so the rule that decides what the user sees is a plain unit test.
 *
 * The phone sends every pending dose it holds — a rolling two-day window — and this keeps the ones
 * falling on today or tomorrow in the watch's own zone, grouped by the moment they are due.
 * Nothing is dropped on the past side: the phone never sends a dose that has lapsed into missed, so
 * a dose whose time has already gone is one the user still has to take, and it belongs at the top
 * of today.
 */
object DayAgenda {

    fun build(doses: List<SyncedDose>, now: Instant, zone: ZoneId): List<AgendaSection> {
        val today = now.atZone(zone).toLocalDate()
        val byDay = doses
            .sortedBy { it.scheduledAtEpochMillis }
            .groupBy { dose ->
                when (Instant.ofEpochMilli(dose.scheduledAtEpochMillis).atZone(zone).toLocalDate()) {
                    today -> AgendaDay.TODAY
                    today.plusDays(1) -> AgendaDay.TOMORROW
                    // Anything further out is beyond what this screen promises to show.
                    else -> null
                }
            }

        return AgendaDay.entries.mapNotNull { day ->
            byDay[day]?.takeIf { it.isNotEmpty() }?.let { AgendaSection(day, groupByTime(it)) }
        }
    }

    /** Doses due in the same minute are one group; the seconds a phone may carry are not a time. */
    private fun groupByTime(doses: List<SyncedDose>): List<AgendaTimeGroup> = doses
        .groupBy { Instant.ofEpochMilli(it.scheduledAtEpochMillis).truncatedTo(ChronoUnit.MINUTES) }
        .map { (time, grouped) -> AgendaTimeGroup(time, grouped) }
        .sortedBy { it.scheduledAt }
}
