package nl.hexmaster.pillsner.applock.ui

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import nl.hexmaster.pillsner.R

/** One outcome of a biometric or device-credential prompt (design D6). */
sealed interface BiometricResult {
    data object Success : BiometricResult
    data object Cancelled : BiometricResult
    data object LockedOut : BiometricResult
    data object Unavailable : BiometricResult
    data class Failed(val message: String) : BiometricResult
}

/**
 * Drives `BiometricPrompt` from the activity as a suspend function, keeping every view model free
 * of Android prompt callbacks (design D6). Requires a [FragmentActivity] host, which is why
 * `MainActivity` extends it for this change; the 1.4 line of `androidx.biometric` removes that
 * requirement once it reaches stable (a one-file follow-up).
 */
class BiometricAuthenticator(
    private val activity: FragmentActivity,
) {
    /** The unlock screen's automatic prompt: class 2 (weak) biometric, "Use PIN" as the fallback. */
    suspend fun authenticateWithBiometric(): BiometricResult =
        showPrompt(authenticators = BIOMETRIC_WEAK) { info ->
            info.setNegativeButtonText(activity.getString(R.string.applock_use_pin))
        }

    /** The recovery prompt (design D5): the class 2 biometric or the device screen lock. */
    suspend fun authenticateWithDeviceCredential(): BiometricResult =
        showPrompt(authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL) {
            // A negative button is not allowed together with DEVICE_CREDENTIAL: the system supplies
            // its own way out (a "Cancel" affordance on the credential screen).
        }

    private suspend fun showPrompt(
        authenticators: Int,
        configure: (BiometricPrompt.PromptInfo.Builder) -> Unit,
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                continuation.resumeIfActive(BiometricResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                continuation.resumeIfActive(errorCode.toBiometricResult(errString))
            }

            override fun onAuthenticationFailed() {
                // One rejected attempt; the prompt stays open for a retry, so there is no result yet.
            }
        }

        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.applock_biometric_prompt_title))
            .setAllowedAuthenticators(authenticators)
        configure(infoBuilder)

        prompt.authenticate(infoBuilder.build())
        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
    }

    private fun Int.toBiometricResult(errString: CharSequence): BiometricResult = when (this) {
        BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricResult.LockedOut
        BiometricPrompt.ERROR_HW_NOT_PRESENT, BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_HW_UNAVAILABLE ->
            BiometricResult.Unavailable
        BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_CANCELED ->
            BiometricResult.Cancelled
        else -> BiometricResult.Failed(errString.toString())
    }

    private fun CancellableContinuation<BiometricResult>.resumeIfActive(result: BiometricResult) {
        if (isActive) resume(result)
    }
}
