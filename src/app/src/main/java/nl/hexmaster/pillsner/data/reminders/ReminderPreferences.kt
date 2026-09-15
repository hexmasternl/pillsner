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
 * What the reminders have to remember between runs: which of the two permission dialogs the user
 * has already been shown.
 *
 * Neither is asked for twice. Android stops showing the notification dialog after two refusals, and
 * the battery-exemption dialog is a system prompt that a reminder app has no business repeating.
 * Once each has been asked, the Home screen's banner is what tells the user reminders cannot be
 * delivered, and it points at the system setting that fixes it.
 */
class ReminderPreferences(context: Context) {

    private val dataStore = context.applicationContext.reminderDataStore

    val hasRequestedNotificationPermission: Flow<Boolean> =
        dataStore.data.map { it[REQUESTED] == true }

    /** Whether the battery-optimisation exemption has already been asked for once (design D6). */
    val hasRequestedBatteryExemption: Flow<Boolean> =
        dataStore.data.map { it[BATTERY_REQUESTED] == true }

    suspend fun markNotificationPermissionRequested() {
        dataStore.edit { it[REQUESTED] = true }
    }

    suspend fun markBatteryExemptionRequested() {
        dataStore.edit { it[BATTERY_REQUESTED] = true }
    }

    private companion object {
        val REQUESTED = booleanPreferencesKey("notification_permission_requested")
        val BATTERY_REQUESTED = booleanPreferencesKey("battery_exemption_requested")
    }
}
