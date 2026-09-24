package nl.hexmaster.pillsner.wear.ui

import java.time.Instant
import java.util.Locale
import nl.hexmaster.pillsner.wear.domain.AgendaDay

/**
 * One dose on the wrist, and everything the read-only details screen says about the medicine
 * behind it (`wear-day-overview` design D2).
 *
 * @property isOverdue the dose was due before now and has not been answered. It stays on the list,
 * at the top of today, because it is still a dose to take.
 * @property isTomorrow its time falls on the next day. The agenda already says so with its day
 * heading; the details screen, which shows one dose out of that context, says it again.
 * @property defaultDoseText the medicine's default dose, or null when the phone sent no details —
 * an older phone build, or a dose whose medicine no longer exists.
 * @property scheduleLines one line per schedule, as the phone words them.
 * @property stockText what is left in stock, or null for a medicine that records no stock.
 */
data class WatchDoseEntry(
    val doseId: Long,
    val name: String,
    val amountText: String,
    val scheduledAt: Instant,
    val isOverdue: Boolean,
    val isTomorrow: Boolean,
    val defaultDoseText: String? = null,
    val scheduleLines: List<String> = emptyList(),
    val stockText: String? = null,
) {
    /** Whether the phone told the watch anything about the medicine behind this dose. */
    val hasDetails: Boolean get() = defaultDoseText != null || scheduleLines.isNotEmpty()
}

/** Every dose due at one moment, under one time heading. */
data class WatchTimeGroup(val scheduledAt: Instant, val doses: List<WatchDoseEntry>)

/** One day of the agenda, under its own heading. */
data class WatchDaySection(val day: AgendaDay, val groups: List<WatchTimeGroup>)

/**
 * What the agenda screen shows (`wear-day-overview` design D2).
 *
 * @property hasData whether a readable list has ever arrived. Nothing yet and no phone in reach is
 * a different thing from two empty days, and the footer says which.
 * @property locale the phone app's language, so the watch reads the same way the phone does.
 */
data class WatchUiState(
    val sections: List<WatchDaySection> = emptyList(),
    val phoneConnected: Boolean = false,
    val hasData: Boolean = false,
    val locale: Locale = Locale.getDefault(),
) {
    val isEmpty: Boolean get() = sections.isEmpty()

    /** Every dose on the agenda, in the order it is shown. */
    val entries: List<WatchDoseEntry>
        get() = sections.flatMap { section -> section.groups.flatMap { it.doses } }

    /** The dose with [doseId], or null once it has left the agenda — answered on the phone. */
    fun entry(doseId: Long): WatchDoseEntry? = entries.firstOrNull { it.doseId == doseId }
}
