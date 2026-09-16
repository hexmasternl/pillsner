package nl.hexmaster.pillsner.applock.domain

import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The outcome of submitting a PIN on the unlock screen. */
sealed interface UnlockResult {
    data object Success : UnlockResult
    data object WrongPin : UnlockResult
    data class CoolingDown(val endsAt: Instant) : UnlockResult
}

/**
 * Checks a PIN entered on the unlock screen against the stored verifier (spec "Entering the
 * correct PIN unlocks the app", "Repeated wrong PIN entries trigger a cooldown").
 */
class UnlockWithPin(
    private val repository: AppLockRepository,
    private val verifier: PinVerifier,
    private val registerFailedAttempt: RegisterFailedAttempt,
    private val clock: Clock,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(pin: Pin, settings: AppLockSettings): UnlockResult {
        val credential = settings.credential ?: return UnlockResult.WrongPin
        settings.cooldownEndsAt?.let { endsAt ->
            if (Instant.now(clock).isBefore(endsAt)) return UnlockResult.CoolingDown(endsAt)
        }
        return if (withContext(dispatcher) { verifier.verify(pin, credential) }) {
            repository.resetAttempts()
            UnlockResult.Success
        } else {
            registerFailedAttempt(settings.consecutiveFailures)
            UnlockResult.WrongPin
        }
    }
}
