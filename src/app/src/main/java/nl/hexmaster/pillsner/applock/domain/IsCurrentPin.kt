package nl.hexmaster.pillsner.applock.domain

import kotlinx.coroutines.flow.first

/**
 * Whether [Pin] is the one currently in force. Used by the change-PIN flow to refuse the PIN the
 * user already has at the first step, before they type it a second time (design D2).
 */
class IsCurrentPin(
    private val repository: AppLockRepository,
    private val verifier: PinVerifier,
) {
    suspend operator fun invoke(pin: Pin): Boolean {
        val credential = repository.settings.first().credential ?: return false
        return verifier.verify(pin, credential)
    }
}
