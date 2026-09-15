package nl.hexmaster.pillsner.data.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import java.time.Instant
import kotlinx.coroutines.flow.first
import nl.hexmaster.pillsner.domain.scheduling.WakeKind
import nl.hexmaster.pillsner.domain.scheduling.WakeMoment
import nl.hexmaster.pillsner.domain.scheduling.WakeSchedule

/**
 * The alarms the app currently has set, kept where they can be read before the user has unlocked
 * the phone for the first time after a reboot (design D3, widened to a set by D8).
 *
 * Everything else the app owns — the medicines, the doses, the settings — lives in
 * credential-encrypted storage and is unreadable until then, so on a locked boot this record is all
 * there is to work from. It is also all that is needed: an alarm carries no payload, and the wake
 * works the rest out from the database once the phone is unlocked. It is what the watchdog compares
 * against too, so one record serves both.
 *
 * **Nothing identifying may ever be written here.** Device-protected storage is readable before the
 * user authenticates, so no medicine name, no amount and no dose id: moments and kinds and nothing
 * else. A reader learns that the phone will wake at some times, which the system's own next-alarm
 * slot already tells them.
 */
class ArmedAlarmStore(context: Context) {

    private val dataStore = store(context.applicationContext)

    /** The alarms the app last armed, or the empty set when it has none. */
    suspend fun armed(): WakeSchedule =
        dataStore.data.first()[ARMED].orEmpty().mapNotNullTo(mutableSetOf(), ::decode)

    /** Records [schedule] as the set that is now armed, replacing whatever was recorded before. */
    suspend fun replace(schedule: WakeSchedule) {
        dataStore.edit { it[ARMED] = schedule.mapTo(mutableSetOf(), ::encode) }
    }

    /** Forgets everything recorded; the app has no alarm set. */
    suspend fun clear() {
        dataStore.edit { it.remove(ARMED) }
    }

    /** Every key currently in the file, so a test can hold the rule above to its word. */
    internal suspend fun storedKeys(): Set<String> =
        dataStore.data.first().asMap().keys.mapTo(mutableSetOf()) { it.name }

    companion object {
        private const val FILE_NAME = "armed_alarm"
        private val ARMED = stringSetPreferencesKey("armed")

        private fun encode(moment: WakeMoment): String = "${moment.at.toEpochMilli()}|${moment.kind.name}"

        /**
         * A row written by a version that has since been changed is dropped rather than fought
         * over: the worst it costs is one alarm, which the next reconcile puts back.
         */
        private fun decode(row: String): WakeMoment? {
            val (millis, kind) = row.split('|').takeIf { it.size == 2 } ?: return null
            return WakeMoment(
                at = millis.toLongOrNull()?.let(Instant::ofEpochMilli) ?: return null,
                kind = WakeKind.entries.firstOrNull { it.name == kind } ?: return null,
            )
        }

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
