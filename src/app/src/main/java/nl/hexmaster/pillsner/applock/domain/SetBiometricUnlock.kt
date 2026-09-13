package nl.hexmaster.pillsner.applock.domain

/**
 * Turns the biometric unlock preference on or off. The caller is responsible for the rule the
 * preference itself does not enforce: turning it on requires one successful biometric
 * authentication first (spec "Biometric unlock can be enabled as an addition to the PIN").
 */
class SetBiometricUnlock(
    private val repository: AppLockRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setBiometricEnabled(enabled)
    }
}
