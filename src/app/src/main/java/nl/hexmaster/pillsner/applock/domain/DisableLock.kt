package nl.hexmaster.pillsner.applock.domain

/**
 * Turns the app lock off: no credential, no biometric preference, no failure count survive it
 * (spec "Disabling the lock requires the current PIN").
 *
 * Two paths end here, and both prove who the user is before they do. From the Security section the
 * identity check has just taken the current PIN — biometrics are refused for this one action. From
 * the unlock screen's recovery prompt the device credential stood in, because the PIN could not be
 * checked at all once the Keystore key was gone (design D5 of `app-login`).
 */
class DisableLock(
    private val repository: AppLockRepository,
) {
    suspend operator fun invoke() {
        repository.clearCredential()
    }
}
