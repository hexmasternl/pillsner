package nl.hexmaster.pillsner.applock.domain

/**
 * Enables the lock once the PIN setup flow's two entries match (spec "Enabling the lock requires
 * choosing and confirming a PIN"). The credential is only ever persisted here, after confirmation.
 */
class EnablePinLock(
    private val repository: AppLockRepository,
    private val verifier: PinVerifier,
) {
    suspend operator fun invoke(pin: Pin) {
        repository.storeCredential(verifier.create(pin))
    }
}
