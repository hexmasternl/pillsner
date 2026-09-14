package nl.hexmaster.pillsner.applock.domain

/** Whether biometric unlock can be offered right now (design D6). */
enum class BiometricStatus {
    /** A class 2 (weak) or stronger biometric is enrolled and usable. */
    Available,

    /** The device has biometric hardware but nothing is enrolled. */
    NoneEnrolled,

    /** The device has no biometric hardware at all. */
    NoHardware,

    /** Biometrics exist but are temporarily unusable (for example, hardware busy or updating). */
    Unavailable,
}

/**
 * Reports biometric availability. Free of Android imports; the production implementation,
 * [nl.hexmaster.pillsner.applock.data.AndroidBiometricAvailability], wraps `BiometricManager`.
 */
interface BiometricAvailability {
    fun status(): BiometricStatus
}
