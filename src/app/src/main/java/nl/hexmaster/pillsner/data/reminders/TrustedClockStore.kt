package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import nl.hexmaster.pillsner.domain.scheduling.TrustedNow

/** The trusted clock guard's own Preferences file; nothing in it is secret. */
private val Context.trustedClockDataStore: DataStore<Preferences> by preferencesDataStore(name = "trusted_clock")

/**
 * Persists the two longs [TrustedNow] needs between wakes.
 *
 * Modelled on [ReminderPreferences]'s plain, ordinary DataStore, not [ArmedAlarmStore]'s
 * device-protected one: the purge only ever runs from inside [ReminderCoordinator.onWake]'s locked
 * body, which is reached only after the user has unlocked the device for the first time after a
 * reboot, so this store never needs to be read before that point (design D2's correction after the
 * first draft assumed otherwise).
 *
 * **Nothing identifying may ever be written here.** Two epoch-millisecond timestamps, nothing
 * about a dose, a medicine or an amount.
 */
class TrustedClockStore(context: Context) {

    private val dataStore = context.applicationContext.trustedClockDataStore

    /** The last persisted sample, or null before the very first observation. */
    suspend fun read(): TrustedNow.Sample? {
        val prefs = dataStore.data.first()
        val trustedNowMillis = prefs[TRUSTED_NOW_MILLIS] ?: return null
        val anchorElapsedRealtimeMillis = prefs[ANCHOR_ELAPSED_REALTIME_MILLIS] ?: return null
        return TrustedNow.Sample(trustedNowMillis, anchorElapsedRealtimeMillis)
    }

    /** Replaces whatever sample was recorded before. */
    suspend fun write(sample: TrustedNow.Sample) {
        dataStore.edit {
            it[TRUSTED_NOW_MILLIS] = sample.trustedNowMillis
            it[ANCHOR_ELAPSED_REALTIME_MILLIS] = sample.anchorElapsedRealtimeMillis
        }
    }

    /** Forgets the recorded sample; the next [read] is as though nothing had ever been observed. */
    suspend fun clear() {
        dataStore.edit {
            it.remove(TRUSTED_NOW_MILLIS)
            it.remove(ANCHOR_ELAPSED_REALTIME_MILLIS)
        }
    }

    private companion object {
        val TRUSTED_NOW_MILLIS = longPreferencesKey("trusted_now_millis")
        val ANCHOR_ELAPSED_REALTIME_MILLIS = longPreferencesKey("anchor_elapsed_realtime_millis")
    }
}
