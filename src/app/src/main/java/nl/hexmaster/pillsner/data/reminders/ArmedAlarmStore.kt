package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import java.time.Instant
import kotlinx.coroutines.flow.first

/**
 * The moment of the alarm the app currently has set, kept where it can be read before the user has
 * unlocked the phone for the first time after a reboot (design D3).
 *
 * Everything else the app owns — the medicines, the doses, the settings — lives in
 * credential-encrypted storage and is unreadable until then, so on a locked boot this one
 * timestamp is all there is to work from. It is also all that is needed: an alarm carries no
 * payload, and the wake works the rest out from the database once the phone is unlocked.
 *
 * **Nothing identifying may ever be written here.** Device-protected storage is readable before the
 * user authenticates, so no medicine name, no amount and no dose id: a bare moment and nothing
 * else. A reader learns that the phone will wake at a time, which the system's own next-alarm slot
 * already tells them.
 */
class ArmedAlarmStore(context: Context) {

    private val dataStore = store(context.applicationContext)

    /** The moment of the alarm the app last armed, or null when it has none. */
    suspend fun armedAt(): Instant? = dataStore.data.first()[ARMED_AT]?.let(Instant::ofEpochMilli)

    /** Records that an alarm is now set for [at], replacing whatever was recorded before. */
    suspend fun set(at: Instant) {
        dataStore.edit { it[ARMED_AT] = at.toEpochMilli() }
    }

    /** Forgets the recorded moment; the app has no alarm set. */
    suspend fun clear() {
        dataStore.edit { it.remove(ARMED_AT) }
    }

    /** Every key currently in the file, so a test can hold the rule above to its word. */
    internal suspend fun storedKeys(): Set<String> =
        dataStore.data.first().asMap().keys.mapTo(mutableSetOf()) { it.name }

    companion object {
        private const val FILE_NAME = "armed_alarm"
        private val ARMED_AT = longPreferencesKey("armed_at")

        // DataStore allows one instance per file per process and throws on a second, so the
        // instance is shared here rather than held by whichever object happened to be built first.
        @Volatile
        private var instance: DataStore<Preferences>? = null

        private fun store(context: Context): DataStore<Preferences> =
            instance ?: synchronized(this) {
                instance ?: PreferenceDataStoreFactory.create {
                    context.createDeviceProtectedStorageContext().preferencesDataStoreFile(FILE_NAME)
                }.also { instance = it }
            }
    }
}
