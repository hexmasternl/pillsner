package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.first
import nl.hexmaster.pillsner.domain.scheduling.ClockSample

/** The trusted clock's own Preferences file; nothing else reads or writes it. */
private val Context.trustedClockDataStore: DataStore<Preferences> by preferencesDataStore(name = "trusted_clock")

/**
 * Persists the one sample `TrustedNow` needs between wakes (dose-history-retention design D3): the
 * wall clock the dose history purge last trusted, paired with the boot clock
 * (`SystemClock.elapsedRealtime()`) reading taken at the same moment.
 *
 * Modelled on [ArmedAlarmStore]'s shape — a small, single-purpose DataStore file behind a `read`/
 * `write` pair — but kept in ordinary (credential-encrypted) storage rather than device-protected
 * storage: unlike the armed alarm set, this is read only from [ReminderCoordinator]'s wake, which
 * never runs before the user has unlocked the phone once after a reboot.
 *
 * Holds nothing sensitive either way: two timestamps, no medicine name, no dose id.
 */
class TrustedClockStore(context: Context) {

    private val dataStore = context.applicationContext.trustedClockDataStore

    /** The last sample recorded, or null when there is none yet (first run, or storage cleared). */
    suspend fun read(): ClockSample? {
        val preferences = dataStore.data.first()
        val wall = preferences[LAST_WALL] ?: return null
        val boot = preferences[LAST_BOOT] ?: return null
        return ClockSample(Instant.ofEpochMilli(wall), boot)
    }

    /** Records [sample] as the one the next wake's readings are compared against. */
    suspend fun write(sample: ClockSample) {
        dataStore.edit {
            it[LAST_WALL] = sample.wall.toEpochMilli()
            it[LAST_BOOT] = sample.bootMillis
        }
    }

    /** Forgets the recorded sample, as if this were the first run. Only ever used by tests. */
    internal suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val LAST_WALL = longPreferencesKey("last_wall")
        val LAST_BOOT = longPreferencesKey("last_boot")
    }
}
