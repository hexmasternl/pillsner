package nl.hexmaster.pillsner.applock.domain

import kotlinx.coroutines.flow.first

/**
 * Decides the lock state to show right after the settings are first read (design D2, D5):
 * disabled when the lock was never turned on; [LockState.Recovering] when it is on but the
 * verifier's key is gone; [LockState.Locked] otherwise. Run once at process start, before any
 * content is composed.
 */
class ResolveInitialLockState(
    private val repository: AppLockRepository,
    private val verifier: PinVerifier,
) {
    suspend operator fun invoke(): LockState {
        val settings = repository.settings.first()
        return when {
            !settings.enabled -> LockState.Disabled
            !verifier.isAvailable() -> LockState.Recovering
            else -> LockState.Locked(settings.cooldownEndsAt)
        }
    }
}
