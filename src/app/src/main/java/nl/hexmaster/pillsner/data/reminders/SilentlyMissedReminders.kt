package nl.hexmaster.pillsner.data.reminders

import java.time.Instant

/**
 * Where the coordinator records that a reminder went missing in silence (design D5).
 *
 * A port rather than the preferences themselves, so the wake cycle does not have to know about
 * DataStore and a test can state what was recorded without a file on disk. The wired implementation
 * is [ReminderPreferences.recordSilentlyMissedReminder].
 */
fun interface SilentlyMissedReminders {

    /** Records that a dose lapsed at [at] without the user ever having been told about it. */
    suspend fun record(at: Instant)
}
