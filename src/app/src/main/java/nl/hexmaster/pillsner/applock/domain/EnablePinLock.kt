package nl.hexmaster.pillsner.applock.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enables the lock once the PIN setup flow's two entries match (spec "Enabling the lock requires
 * choosing and confirming a PIN"). The credential is only ever persisted here, after confirmation.
 */
class EnablePinLock(
    private val repository: AppLockRepository,
    private val verifier: PinVerifier,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend operator fun invoke(pin: Pin) {
        repository.storeCredential(withContext(dispatcher) { verifier.create(pin) })
    }
}
