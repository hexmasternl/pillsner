package nl.hexmaster.pillsner.applock.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.KeyStore
import nl.hexmaster.pillsner.applock.domain.Pin
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real Android Keystore, so this runs on a device or emulator (task 3.6). */
@RunWith(AndroidJUnit4::class)
class KeystorePinVerifierTest {

    private val verifier = KeystorePinVerifier()

    @After
    fun tearDown() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    @Test
    fun createThenVerifyCorrectPinSucceeds() {
        val pin = requireNotNull(Pin.of("1234"))
        val credential = verifier.create(pin)

        assertTrue(verifier.verify(pin, credential))
    }

    @Test
    fun createThenVerifyWrongPinFails() {
        val pin = requireNotNull(Pin.of("1234"))
        val credential = verifier.create(pin)

        assertFalse(verifier.verify(requireNotNull(Pin.of("9999")), credential))
    }

    @Test
    fun isAvailableIsFalseAfterTheKeyAliasIsDeleted() {
        val pin = requireNotNull(Pin.of("1234"))
        verifier.create(pin)
        assertTrue(verifier.isAvailable())

        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.deleteEntry(KEY_ALIAS)

        assertFalse(verifier.isAvailable())
    }

    private companion object {
        const val KEY_ALIAS = "nl.hexmaster.pillsner.applock.pin"
    }
}
