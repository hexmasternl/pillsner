package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The reminders' own Preferences file; nothing in it is secret, and nothing else writes to it. */
private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminders")

/**
 * What the reminders have to remember between runs.
 *
 * Two things. Whether the notification dialog has already been shown, because Android stops showing
 * it after two refusals and the Home banner takes over from there. And the moment a reminder was
 * last missed in silence — a dose that lapsed without ever being announced — which is the evidence
 * the Home banner is raised by (design D2, D5).
 *
 * The app asks for nothing else. It no longer asks to be left out of battery optimisation, so the
 * key that recorded having asked is abandoned in place; nothing reads it.
 */
class ReminderPreferences(context: Context) {

    private val dataStore = context.applicationContext.reminderDataStore

    val hasRequestedNotificationPermission: Flow<Boolean> =
        dataStore.data.map { it[REQUESTED] == true }

    /**
     * When a reminder was most recently missed in silence, or null when none has been since the
     * user last acknowledged one (design D5).
     *
     * Sticky on purpose. A phone that delivers three reminders in four would make a self-clearing
     * warning flicker, and a warning that comes and goes is worse than one that waits to be read.
     */
    val silentlyMissedReminderAt: Flow<Instant?> =
        dataStore.data.map { prefs -> prefs[SILENTLY_MISSED_AT]?.let(Instant::ofEpochMilli) }

    suspend fun markNotificationPermissionRequested() {
        dataStore.edit { it[REQUESTED] = true }
    }

    /** Records that a dose lapsed without the user ever having been told about it. */
    suspend fun recordSilentlyMissedReminder(at: Instant) {
        dataStore.edit { it[SILENTLY_MISSED_AT] = at.toEpochMilli() }
    }

    /** Forgets the most recent silent miss: the user has seen the banner and acted on it. */
    suspend fun clearSilentlyMissedReminder() {
        dataStore.edit { it.remove(SILENTLY_MISSED_AT) }
    }

    private companion object {
        val REQUESTED = booleanPreferencesKey("notification_permission_requested")
        val SILENTLY_MISSED_AT = longPreferencesKey("silently_missed_reminder_at")
    }
}
