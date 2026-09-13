package nl.hexmaster.pillsner.applock.domain

import java.time.Instant

/**
 * The app lock's state machine (design D1, state machine diagram). Drives what
 * [nl.hexmaster.pillsner.ui.PillsnerApp] composes: the navigation host only in [Unlocked] and
 * [Disabled]; the unlock screen (or a blank surface) otherwise.
 */
sealed interface LockState {

    /** The persisted settings have not been read yet. A blank surface is shown, never content. */
    data object Loading : LockState

    /** The user has never enabled the lock, or has disabled it. The app opens directly. */
    data object Disabled : LockState

    /** The lock is enabled and the user has proven their identity for this app session. */
    data object Unlocked : LockState

    /**
     * The lock is enabled and the app requires the PIN or biometric before showing content.
     *
     * @property cooldownEndsAt when PIN entry becomes available again after 5 consecutive
     * failures, or null when there is no active cooldown.
     */
    data class Locked(val cooldownEndsAt: Instant? = null) : LockState

    /**
     * The lock is enabled but the device-bound key needed to verify the PIN is gone (design D5).
     * The only way out is the device credential reset offered by the unlock screen.
     */
    data object Recovering : LockState
}

/**
 * A PIN's device-bound verifier (design D3): [salt] and [verifier] are the only things ever
 * persisted. Neither, alone or together, lets the PIN be recovered without the Keystore key that
 * produced [verifier].
 */
data class PinCredential(
    val salt: ByteArray,
    val verifier: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is PinCredential && salt.contentEquals(other.salt) && verifier.contentEquals(other.verifier))

    override fun hashCode(): Int = 31 * salt.contentHashCode() + verifier.contentHashCode()
}
