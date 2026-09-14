package nl.hexmaster.pillsner.applock.domain

/**
 * An in-memory [PinVerifier] for tests: "verification" is just byte-array equality of the pin's
 * digits against the credential's verifier, with the salt unused beyond being non-empty.
 */
class FakePinVerifier(
    private var available: Boolean = true,
) : PinVerifier {

    fun setAvailable(isAvailable: Boolean) {
        available = isAvailable
    }

    override fun isAvailable(): Boolean = available

    override fun create(pin: Pin): PinCredential =
        PinCredential(salt = byteArrayOf(1), verifier = pin.digits.toByteArray())

    override fun verify(pin: Pin, credential: PinCredential): Boolean =
        pin.digits.toByteArray().contentEquals(credential.verifier)
}
