package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The reminders' own Preferences file; nothing in it is secret, and nothing else writes to it. */
private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminders")

/**
 * The one thing the reminders have to remember between runs: whether the user has already been
 * asked for permission to show notifications.
 *
 * Android stops showing the dialog after two refusals, but the app should not put it in front of
 * someone on every launch either. Once it has been asked, the Home screen's banner is what tells
 * the user reminders cannot be delivered, and it points at the system setting.
 */
class ReminderPreferences(context: Context) {

    private val dataStore = context.applicationContext.reminderDataStore

    val hasRequestedNotificationPermission: Flow<Boolean> =
        dataStore.data.map { it[REQUESTED] == true }

    suspend fun markNotificationPermissionRequested() {
        dataStore.edit { it[REQUESTED] = true }
    }

    private companion object {
        val REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }
}
