package nl.hexmaster.pillsner.applock.data

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import nl.hexmaster.pillsner.applock.domain.BiometricAvailability
import nl.hexmaster.pillsner.applock.domain.BiometricStatus

/**
 * Wraps `BiometricManager.canAuthenticate` for class 2 (weak) biometrics or stronger (design D6):
 * face unlock on many mid-range devices is class 2, and the prompt gates UI access rather than a
 * cryptographic key, so requiring class 3 would buy little.
 */
class AndroidBiometricAvailability(
    context: Context,
) : BiometricAvailability {

    private val biometricManager = BiometricManager.from(context.applicationContext)

    override fun status(): BiometricStatus = when (biometricManager.canAuthenticate(BIOMETRIC_WEAK)) {
        BIOMETRIC_SUCCESS -> BiometricStatus.Available
        BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NoneEnrolled
        BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
        BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.Unavailable
        else -> BiometricStatus.Unavailable
    }
}
