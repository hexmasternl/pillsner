package nl.hexmaster.pillsner.applock.domain

import java.time.Clock
import java.time.Instant

/** The outcome of confirming the current PIN to turn the lock off. */
sealed interface DisableLockResult {
    data object Success : DisableLockResult
    data object WrongPin : DisableLockResult
    data class CoolingDown(val endsAt: Instant) : DisableLockResult
}

/**
 * Turns the lock off after the current PIN is confirmed (spec "Disabling the lock requires the
 * current PIN"). Biometrics are never accepted here: the caller (the Security section's confirm
 * dialog) never offers a biometric prompt for this action.
 */
class DisablePinLock(
    private val repository: AppLockRepository,
    private val verifier: PinVerifier,
    private val registerFailedAttempt: RegisterFailedAttempt,
    private val clock: Clock,
) {
    suspend operator fun invoke(pin: Pin, settings: AppLockSettings): DisableLockResult {
        val credential = settings.credential ?: return DisableLockResult.WrongPin
        settings.cooldownEndsAt?.let { endsAt ->
            if (Instant.now(clock).isBefore(endsAt)) return DisableLockResult.CoolingDown(endsAt)
        }
        return if (verifier.verify(pin, credential)) {
            repository.clearCredential()
            DisableLockResult.Success
        } else {
            registerFailedAttempt(settings.consecutiveFailures)
            DisableLockResult.WrongPin
        }
    }
}
