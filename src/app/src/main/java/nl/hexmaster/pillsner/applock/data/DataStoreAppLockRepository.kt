package nl.hexmaster.pillsner.applock.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.hexmaster.pillsner.applock.domain.AppLockRepository
import nl.hexmaster.pillsner.applock.domain.AppLockSettings
import nl.hexmaster.pillsner.applock.domain.PinCredential

/**
 * The dedicated "applock" Preferences DataStore file (design D4). Kept separate from any other
 * settings so the backup exclusion rules (`res/xml/data_extraction_rules.xml`,
 * `res/xml/full_backup_content.xml`) can target exactly this file and nothing else.
 */
private val Context.appLockDataStore: DataStore<Preferences> by preferencesDataStore(name = "applock")

/**
 * Persists the app lock's settings in the [appLockDataStore] (design D4). Room is not used: these
 * are a handful of scalars, not domain records, and a table would drag a schema migration into
 * every tweak.
 */
class DataStoreAppLockRepository(
    context: Context,
) : AppLockRepository {

    private val dataStore = context.applicationContext.appLockDataStore

    override val settings: Flow<AppLockSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun storeCredential(credential: PinCredential) {
        dataStore.edit { prefs ->
            prefs[ENABLED] = true
            prefs[SALT] = credential.salt.toBase64()
            prefs[VERIFIER] = credential.verifier.toBase64()
            prefs[CONSECUTIVE_FAILURES] = 0
            prefs.remove(COOLDOWN_ENDS_AT_MILLIS)
            prefs[SETTINGS_VERSION] = CURRENT_SETTINGS_VERSION
        }
    }

    override suspend fun replaceCredential(credential: PinCredential) {
        dataStore.edit { prefs ->
            // One edit: the PIN is never half-replaced, whatever happens between here and the next
            // line (design D2). The enabled and biometric flags are deliberately left alone.
            prefs[SALT] = credential.salt.toBase64()
            prefs[VERIFIER] = credential.verifier.toBase64()
            prefs[CONSECUTIVE_FAILURES] = 0
            prefs.remove(COOLDOWN_ENDS_AT_MILLIS)
        }
    }

    override suspend fun clearCredential() {
        dataStore.edit { prefs ->
            prefs.clear()
            prefs[SETTINGS_VERSION] = CURRENT_SETTINGS_VERSION
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[BIOMETRIC_ENABLED] = enabled }
    }

    override suspend fun recordFailedAttempt(consecutiveFailures: Int, cooldownEndsAt: Instant?) {
        dataStore.edit { prefs ->
            prefs[CONSECUTIVE_FAILURES] = consecutiveFailures
            if (cooldownEndsAt != null) {
                prefs[COOLDOWN_ENDS_AT_MILLIS] = cooldownEndsAt.toEpochMilli()
            } else {
                prefs.remove(COOLDOWN_ENDS_AT_MILLIS)
            }
        }
    }

    override suspend fun resetAttempts() {
        dataStore.edit { prefs ->
            prefs[CONSECUTIVE_FAILURES] = 0
            prefs.remove(COOLDOWN_ENDS_AT_MILLIS)
        }
    }

    override suspend fun reconcileBiometricAvailability(isAvailable: Boolean) {
        dataStore.edit { prefs ->
            if (!isAvailable && prefs[BIOMETRIC_ENABLED] == true) {
                prefs[BIOMETRIC_ENABLED] = false
            }
        }
    }

    private fun Preferences.toSettings(): AppLockSettings {
        val salt = this[SALT]?.fromBase64()
        val verifier = this[VERIFIER]?.fromBase64()
        val credential = if (salt != null && verifier != null) PinCredential(salt, verifier) else null
        return AppLockSettings(
            enabled = this[ENABLED] ?: false,
            biometricEnabled = this[BIOMETRIC_ENABLED] ?: false,
            credential = credential,
            consecutiveFailures = this[CONSECUTIVE_FAILURES] ?: 0,
            cooldownEndsAt = this[COOLDOWN_ENDS_AT_MILLIS]?.let(Instant::ofEpochMilli),
        )
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val SALT = stringPreferencesKey("salt")
        val VERIFIER = stringPreferencesKey("verifier")
        val CONSECUTIVE_FAILURES = intPreferencesKey("consecutive_failures")
        val COOLDOWN_ENDS_AT_MILLIS = longPreferencesKey("cooldown_ends_at_millis")
        val SETTINGS_VERSION = intPreferencesKey("settings_version")
        const val CURRENT_SETTINGS_VERSION = 1
    }
}
