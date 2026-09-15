package nl.hexmaster.pillsner.applock.domain

/**
 * A 4 to 6 decimal digit PIN (spec "Enabling the lock requires choosing and confirming a PIN").
 * Validation lives here so every entry point (setup, unlock, disable) shares one rule.
 */
@JvmInline
value class Pin private constructor(val digits: String) {

    companion object {
        /** The fewest digits a PIN may have. The UI refuses to submit anything shorter. */
        const val MIN_LENGTH = 4

        /** The most digits a PIN may have. The UI refuses to accept anything longer. */
        const val MAX_LENGTH = 6

        private val VALID = Regex("^[0-9]{$MIN_LENGTH,$MAX_LENGTH}$")

        /** Returns the [Pin] for [raw], or null when it is not 4 to 6 decimal digits. */
        fun of(raw: String): Pin? = if (VALID.matches(raw)) Pin(raw) else null
    }
}

/**
 * Derives and checks a device-bound verifier for a [Pin] (design D3). Free of Android imports so
 * it can be faked in tests; the production implementation,
 * [nl.hexmaster.pillsner.applock.data.KeystorePinVerifier], is the only place the Keystore is touched.
 */
interface PinVerifier {

    /** Whether the key this verifier depends on is currently usable. */
    fun isAvailable(): Boolean

    /** Creates a fresh [PinCredential] for [pin], with a new random salt. */
    fun create(pin: Pin): PinCredential

    /** Whether [pin] matches [credential]. Constant-time; never throws for a wrong PIN. */
    fun verify(pin: Pin, credential: PinCredential): Boolean
}
