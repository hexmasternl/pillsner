package nl.hexmaster.pillsner.applock.domain

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: "Changing the PIN" — in place, atomically, with the rest of the lock left as it was. */
class ChangePinTest {

    private val verifier = FakePinVerifier()
    private val repository = FakeAppLockRepository()
    private val changePin = ChangePin(repository, verifier)
    private val isCurrentPin = IsCurrentPin(repository, verifier)

    private val currentPin = requireNotNull(Pin.of("1234"))
    private val newPin = requireNotNull(Pin.of("5678"))

    private suspend fun locked(biometricEnabled: Boolean = false) {
        repository.storeCredential(verifier.create(currentPin))
        repository.setBiometricEnabled(biometricEnabled)
    }

    @Test
    fun `the new pin replaces the old one`() = runTest {
        locked()

        assertEquals(ChangePinResult.Changed, changePin(newPin))

        val credential = requireNotNull(repository.settings.value.credential)
        assertTrue(verifier.verify(newPin, credential))
        assertFalse("The old PIN must stop working", verifier.verify(currentPin, credential))
    }

    @Test
    fun `the pin the user already has is refused`() = runTest {
        locked()
        val before = repository.settings.value.credential

        assertEquals(ChangePinResult.SameAsCurrent, changePin(currentPin))

        assertEquals(before, repository.settings.value.credential)
    }

    @Test
    fun `the lock stays on and keeps the biometric preference`() = runTest {
        locked(biometricEnabled = true)

        changePin(newPin)

        assertTrue(repository.settings.value.enabled)
        assertTrue("Changing the PIN is not starting over", repository.settings.value.biometricEnabled)
    }

    @Test
    fun `a new pin starts with a clean slate`() = runTest {
        locked()
        repository.recordFailedAttempt(5, Instant.parse("2026-09-11T08:00:30Z"))

        changePin(newPin)

        assertEquals(0, repository.settings.value.consecutiveFailures)
        assertNull(repository.settings.value.cooldownEndsAt)
    }

    @Test
    fun `the change flow can tell the user early that the pin is the one in force`() = runTest {
        locked()

        assertTrue(isCurrentPin(currentPin))
        assertFalse(isCurrentPin(newPin))
    }
}
