package nl.hexmaster.pillsner.applock.domain

/**
 * Disables the app lock after the device credential recovery prompt succeeds (design D5, spec
 * "The lock can be recovered when the verifier cannot be evaluated"). Has the same effect as
 * disabling the lock normally: no PIN, no biometric preference, no failure count survive it.
 */
class ResetLockAfterRecovery(
    private val repository: AppLockRepository,
) {
    suspend operator fun invoke() {
        repository.clearCredential()
    }
}
