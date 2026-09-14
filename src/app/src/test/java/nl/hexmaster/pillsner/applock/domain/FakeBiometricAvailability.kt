package nl.hexmaster.pillsner.applock.domain

/** A [BiometricAvailability] whose status is set directly by tests. */
class FakeBiometricAvailability(
    private var status: BiometricStatus = BiometricStatus.Available,
) : BiometricAvailability {

    fun setStatus(newStatus: BiometricStatus) {
        status = newStatus
    }

    override fun status(): BiometricStatus = status
}
