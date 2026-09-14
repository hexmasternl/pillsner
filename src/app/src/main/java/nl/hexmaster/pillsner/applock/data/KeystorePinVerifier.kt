package nl.hexmaster.pillsner.applock.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import nl.hexmaster.pillsner.applock.domain.Pin
import nl.hexmaster.pillsner.applock.domain.PinCredential
import nl.hexmaster.pillsner.applock.domain.PinVerifier

/**
 * Verifies a PIN with an HMAC keyed by a non-exportable Android Keystore key (design D3): a 4 to
 * 6 digit PIN has at most one million values, so any hash evaluable off the device is brute-forced
 * in minutes once the preferences file is copied. `HMAC-SHA256(key, salt || pin)`, with the key
 * generated in `AndroidKeyStore`, makes the persisted salt and verifier useless without it.
 */
class KeystorePinVerifier : PinVerifier {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    override fun isAvailable(): Boolean = runCatching {
        keyStore.containsAlias(KEY_ALIAS) && keyStore.getKey(KEY_ALIAS, null) != null
    }.getOrDefault(false)

    override fun create(pin: Pin): PinCredential {
        val salt = ByteArray(SALT_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        return PinCredential(salt = salt, verifier = hmac(salt, pin))
    }

    override fun verify(pin: Pin, credential: PinCredential): Boolean {
        if (!isAvailable()) return false
        val computed = runCatching { hmac(credential.salt, pin) }.getOrNull() ?: return false
        return MessageDigest.isEqual(computed, credential.verifier)
    }

    private fun hmac(salt: ByteArray, pin: Pin): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(loadOrCreateKey())
        mac.update(salt)
        return mac.doFinal(pin.digits.toByteArray(Charsets.UTF_8))
    }

    /** Reuses the existing key so verification of an old credential still works after a restart. */
    private fun loadOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN).build()
        generator.init(spec)
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "nl.hexmaster.pillsner.applock.pin"
        const val HMAC_ALGORITHM = "HmacSHA256"
        const val SALT_SIZE_BYTES = 16
    }
}
