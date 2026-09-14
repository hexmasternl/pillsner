package nl.hexmaster.pillsner.wear.ui

import java.time.Instant
import java.util.Locale

/**
 * One line on the wrist.
 *
 * @property isOverdue the dose was due before now and has not been answered. It stays on the list,
 * at the top, because it is still a dose to take.
 * @property isTomorrow its time falls on the next day. Within a six-hour window that is the only
 * other day possible, so "Tomorrow 02:00" is all the date anyone needs.
 */
data class WatchDoseEntry(
    val doseId: Long,
    val name: String,
    val amountText: String,
    val scheduledAt: Instant,
    val isOverdue: Boolean,
    val isTomorrow: Boolean,
)

/**
 * What the single screen shows (design D5).
 *
 * @property hasData whether a readable list has ever arrived. Nothing yet and no phone in reach is
 * a different thing from an empty six hours, and the footer says which.
 * @property locale the phone app's language, so the watch reads the same way the phone does.
 */
data class WatchUiState(
    val entries: List<WatchDoseEntry> = emptyList(),
    val phoneConnected: Boolean = false,
    val hasData: Boolean = false,
    val locale: Locale = Locale.getDefault(),
) {
    val isEmpty: Boolean get() = entries.isEmpty()
}
