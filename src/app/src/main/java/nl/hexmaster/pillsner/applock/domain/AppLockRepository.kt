package nl.hexmaster.pillsner.applock.domain

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * The app lock's persisted settings (design D4): a handful of scalars kept in their own storage
 * so a backup exclusion rule can target exactly that file. See
 * [nl.hexmaster.pillsner.applock.data.DataStoreAppLockRepository] for the production storage.
 */
data class AppLockSettings(
    val enabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val credential: PinCredential? = null,
    val consecutiveFailures: Int = 0,
    val cooldownEndsAt: Instant? = null,
)

/**
 * Reads and writes the app lock's persisted state. The production implementation stores it in a
 * dedicated Preferences DataStore excluded from backup (design D4); tests use an in-memory fake.
 */
interface AppLockRepository {

    /** The current settings, updated as they change. */
    val settings: Flow<AppLockSettings>

    /** Enables the lock with [credential], starting with no failures and no cooldown. */
    suspend fun storeCredential(credential: PinCredential)

    /** Disables the lock: clears the credential, the biometric preference and the failure count. */
    suspend fun clearCredential()

    /** Turns the biometric unlock preference on or off. */
    suspend fun setBiometricEnabled(enabled: Boolean)

    /**
     * Records one failed attempt: [consecutiveFailures] is the new total and [cooldownEndsAt] is
     * non-null only when this failure just completed a cooldown block (design D7).
     */
    suspend fun recordFailedAttempt(consecutiveFailures: Int, cooldownEndsAt: Instant?)

    /** Resets the failure count and clears any cooldown, after a successful unlock. */
    suspend fun resetAttempts()

    /**
     * Turns the biometric preference off when [isAvailable] is false and it is currently on
     * (design D6); a no-op otherwise. Called on every unlock screen and Security section appearance.
     */
    suspend fun reconcileBiometricAvailability(isAvailable: Boolean)
}
