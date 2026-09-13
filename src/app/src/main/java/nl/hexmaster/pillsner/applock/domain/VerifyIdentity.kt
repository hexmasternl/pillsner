package nl.hexmaster.pillsner.applock.domain

import java.time.Clock
import java.time.Instant

/** A change to the lock that has to be proved to be the owner's (design D1). */
enum class SecurityAction {
    CHANGE_PIN,
    DISABLE_BIOMETRICS,
    DISABLE_LOCK,
}

/** How the user proved who they are. */
enum class IdentityMethod { BIOMETRIC, PIN }

/**
 * One identity check.
 *
 * @property purpose what it is for; the check is valid for that one action and nothing else.
 * @property allowBiometric whether a biometric may stand in for the PIN. False for turning the
 *   lock off, where `app-login` requires the PIN itself.
 */
data class VerifyIdentityRequest(val purpose: SecurityAction, val allowBiometric: Boolean)

/** Where an identity check has got to. */
sealed interface VerifyIdentityState {
    /** No check is running. */
    data object Idle : VerifyIdentityState

    /** The biometric prompt is up. */
    data class AwaitingBiometric(val request: VerifyIdentityRequest) : VerifyIdentityState

    /** The keypad is up; [cooldownEndsAt] is set while entry is refused after repeated failures. */
    data class AwaitingPin(
        val request: VerifyIdentityRequest,
        val cooldownEndsAt: Instant? = null,
    ) : VerifyIdentityState

    /** The user proved who they are. Good for [request]'s purpose once, then gone. */
    data class Verified(val request: VerifyIdentityRequest, val method: IdentityMethod) :
        VerifyIdentityState
}

/**
 * Proves that the person changing the lock is the one who can open it (design D1).
 *
 * The threat is the same modest one the lock itself defends against: someone holding the phone
 * while Pillsner is already open. So the check accepts whatever unlocking accepts — a biometric
 * when the user has one and the action allows it, the PIN otherwise — and shares the unlock
 * screen's failed-attempt counter and cooldown, so a stranger cannot get unlimited guesses here
 * that they would not get there.
 *
 * A check is good for exactly one action. There is no window and no clock: prove it, do the one
 * thing, done. That is also why a failed biometric never turns the preference off — a prompt that
 * was cancelled says nothing about whether the fingerprint still works.
 */
class VerifyIdentity(
    private val repository: AppLockRepository,
    private val verifier: PinVerifier,
    private val registerFailedAttempt: RegisterFailedAttempt,
    private val clock: Clock,
) {

    /** Where a check for [request] begins, given the settings and whether a biometric works now. */
    fun start(
        request: VerifyIdentityRequest,
        settings: AppLockSettings,
        biometricStatus: BiometricStatus,
    ): VerifyIdentityState {
        val biometricPossible = request.allowBiometric &&
            settings.biometricEnabled &&
            biometricStatus == BiometricStatus.Available
        return if (biometricPossible) {
            VerifyIdentityState.AwaitingBiometric(request)
        } else {
            VerifyIdentityState.AwaitingPin(request, activeCooldown(settings))
        }
    }

    /**
     * The prompt came back. Success proves it; anything else — cancel, failure, lockout, no
     * hardware — falls back to the keypad rather than ending the check.
     */
    fun onBiometricResult(
        state: VerifyIdentityState,
        succeeded: Boolean,
        settings: AppLockSettings,
    ): VerifyIdentityState {
        val request = (state as? VerifyIdentityState.AwaitingBiometric)?.request ?: return state
        return if (succeeded) {
            VerifyIdentityState.Verified(request, IdentityMethod.BIOMETRIC)
        } else {
            VerifyIdentityState.AwaitingPin(request, activeCooldown(settings))
        }
    }

    /**
     * A PIN was entered. A wrong one counts towards the same cooldown the unlock screen uses; a
     * right one clears it, exactly as unlocking does.
     */
    suspend fun onPinSubmitted(
        state: VerifyIdentityState,
        pin: Pin,
        settings: AppLockSettings,
    ): VerifyIdentityState {
        val awaiting = state as? VerifyIdentityState.AwaitingPin ?: return state
        val credential = settings.credential ?: return awaiting

        activeCooldown(settings)?.let { return awaiting.copy(cooldownEndsAt = it) }

        return if (verifier.verify(pin, credential)) {
            repository.resetAttempts()
            VerifyIdentityState.Verified(awaiting.request, IdentityMethod.PIN)
        } else {
            registerFailedAttempt(settings.consecutiveFailures)
            awaiting
        }
    }

    /** The user asked for the keypad instead of the prompt. */
    fun onUsePin(state: VerifyIdentityState, settings: AppLockSettings): VerifyIdentityState =
        when (state) {
            is VerifyIdentityState.AwaitingBiometric ->
                VerifyIdentityState.AwaitingPin(state.request, activeCooldown(settings))

            else -> state
        }

    /** The user asked for the prompt again, after cancelling it or hitting a lockout. */
    fun onUseBiometrics(state: VerifyIdentityState): VerifyIdentityState = when (state) {
        is VerifyIdentityState.AwaitingPin ->
            if (state.request.allowBiometric) {
                VerifyIdentityState.AwaitingBiometric(state.request)
            } else {
                state
            }

        else -> state
    }

    private fun activeCooldown(settings: AppLockSettings): Instant? =
        settings.cooldownEndsAt?.takeIf { Instant.now(clock).isBefore(it) }
}
