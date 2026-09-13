package nl.hexmaster.pillsner.applock.domain

import kotlinx.coroutines.flow.first

/** What became of a request to change the PIN. */
sealed interface ChangePinResult {
    data object Changed : ChangePinResult

    /** The new PIN is the one already in use, so nothing changed. */
    data object SameAsCurrent : ChangePinResult
}

/**
 * Replaces the PIN in place (design D2).
 *
 * The lock stays on and the biometric preference is kept: the user asked to change their PIN, not
 * to start over. The new PIN gets a fresh salt and verifier, and the failure count and cooldown
 * are reset in the same write, so a new PIN starts with a clean slate.
 *
 * The caller has already proved who they are through [VerifyIdentity], and the setup flow has
 * already refused the current PIN through [IsCurrentPin]; the check here is the same one again, at
 * the point of writing, so no path can quietly store the PIN the user already had.
 */
class ChangePin(
    private val repository: AppLockRepository,
    private val verifier: PinVerifier,
) {
    suspend operator fun invoke(newPin: Pin): ChangePinResult {
        val current = repository.settings.first().credential
        if (current != null && verifier.verify(newPin, current)) return ChangePinResult.SameAsCurrent

        repository.replaceCredential(verifier.create(newPin))
        return ChangePinResult.Changed
    }
}
