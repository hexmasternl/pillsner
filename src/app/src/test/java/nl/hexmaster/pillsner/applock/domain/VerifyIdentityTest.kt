package nl.hexmaster.pillsner.applock.domain

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The identity check that guards every Security change (spec "Identity check before security
 * changes"). It shares the unlock screen's failure count and cooldown, which is what stops the
 * Security section from becoming a place to guess a PIN without limit.
 */
class VerifyIdentityTest {

    private val fixedInstant = Instant.parse("2026-09-11T08:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val verifier = FakePinVerifier()
    private val repository = FakeAppLockRepository()
    private val verifyIdentity = VerifyIdentity(repository, verifier, RegisterFailedAttempt(repository, clock), clock)

    private val pin = requireNotNull(Pin.of("1234"))
    private val wrongPin = requireNotNull(Pin.of("9999"))

    private suspend fun lockedWithBiometrics(biometricEnabled: Boolean): AppLockSettings {
        repository.storeCredential(verifier.create(pin))
        repository.setBiometricEnabled(biometricEnabled)
        return repository.settings.value
    }

    private fun request(
        purpose: SecurityAction = SecurityAction.CHANGE_PIN,
        allowBiometric: Boolean = true,
    ) = VerifyIdentityRequest(purpose, allowBiometric)

    @Test
    fun `the biometric prompt comes first when the user has one and the action allows it`() = runTest {
        val settings = lockedWithBiometrics(biometricEnabled = true)

        val state = verifyIdentity.start(request(), settings, BiometricStatus.Available)

        assertEquals(VerifyIdentityState.AwaitingBiometric(request()), state)
    }

    @Test
    fun `with biometric unlock off the check asks for the pin`() = runTest {
        val settings = lockedWithBiometrics(biometricEnabled = false)

        val state = verifyIdentity.start(request(), settings, BiometricStatus.Available)

        assertEquals(VerifyIdentityState.AwaitingPin(request()), state)
    }

    @Test
    fun `turning the lock off asks for the pin even with biometrics on`() = runTest {
        val settings = lockedWithBiometrics(biometricEnabled = true)
        val disable = request(SecurityAction.DISABLE_LOCK, allowBiometric = false)

        val state = verifyIdentity.start(disable, settings, BiometricStatus.Available)

        assertEquals(VerifyIdentityState.AwaitingPin(disable), state)
    }

    @Test
    fun `a biometric that is enabled but unusable falls back to the pin`() = runTest {
        val settings = lockedWithBiometrics(biometricEnabled = true)

        val state = verifyIdentity.start(request(), settings, BiometricStatus.NoneEnrolled)

        assertEquals(VerifyIdentityState.AwaitingPin(request()), state)
    }

    @Test
    fun `passing the biometric prompt proves who the user is`() = runTest {
        val settings = lockedWithBiometrics(biometricEnabled = true)
        val awaiting = verifyIdentity.start(request(), settings, BiometricStatus.Available)

        val state = verifyIdentity.onBiometricResult(awaiting, succeeded = true, settings = settings)

        assertEquals(VerifyIdentityState.Verified(request(), IdentityMethod.BIOMETRIC), state)
    }

    @Test
    fun `a cancelled prompt falls back to the pin and leaves the preference alone`() = runTest {
        val settings = lockedWithBiometrics(biometricEnabled = true)
        val awaiting = verifyIdentity.start(request(), settings, BiometricStatus.Available)

        val state = verifyIdentity.onBiometricResult(awaiting, succeeded = false, settings = settings)

        assertEquals(VerifyIdentityState.AwaitingPin(request()), state)
        assertTrue("A cancelled prompt is no evidence that biometrics are gone", repository.settings.value.biometricEnabled)
    }

    @Test
    fun `the correct pin proves who the user is and clears the failure count`() = runTest {
        val settings = lockedWithBiometrics(biometricEnabled = false)
        repository.recordFailedAttempt(3, null)
        val awaiting = verifyIdentity.start(request(), repository.settings.value, BiometricStatus.NoHardware)

        val state = verifyIdentity.onPinSubmitted(awaiting, pin, repository.settings.value)

        assertEquals(VerifyIdentityState.Verified(request(), IdentityMethod.PIN), state)
        assertEquals(0, repository.settings.value.consecutiveFailures)
        assertEquals(settings.credential, repository.settings.value.credential)
    }

    @Test
    fun `a wrong pin keeps the keypad up and counts towards the shared cooldown`() = runTest {
        lockedWithBiometrics(biometricEnabled = false)
        val awaiting = verifyIdentity.start(request(), repository.settings.value, BiometricStatus.NoHardware)

        val state = verifyIdentity.onPinSubmitted(awaiting, wrongPin, repository.settings.value)

        assertEquals(awaiting, state)
        assertEquals(1, repository.settings.value.consecutiveFailures)
    }

    @Test
    fun `five wrong pins anywhere refuse entry until the cooldown is over`() = runTest {
        lockedWithBiometrics(biometricEnabled = false)
        // Four here and one on the unlock screen: the count is one count, not one per screen.
        repository.recordFailedAttempt(4, null)
        var state = verifyIdentity.start(request(), repository.settings.value, BiometricStatus.NoHardware)

        state = verifyIdentity.onPinSubmitted(state, wrongPin, repository.settings.value)
        val cooldownEndsAt = requireNotNull(repository.settings.value.cooldownEndsAt)

        // Even the right PIN is refused while the cooldown runs.
        state = verifyIdentity.onPinSubmitted(state, pin, repository.settings.value)

        assertEquals(VerifyIdentityState.AwaitingPin(request(), cooldownEndsAt), state)
        assertEquals(5, repository.settings.value.consecutiveFailures)
    }

    @Test
    fun `a check started while cooling down says so straight away`() = runTest {
        lockedWithBiometrics(biometricEnabled = false)
        repository.recordFailedAttempt(5, fixedInstant.plusSeconds(30))

        val state = verifyIdentity.start(request(), repository.settings.value, BiometricStatus.NoHardware)

        assertEquals(VerifyIdentityState.AwaitingPin(request(), fixedInstant.plusSeconds(30)), state)
    }

    @Test
    fun `an expired cooldown lets the pin through again`() = runTest {
        lockedWithBiometrics(biometricEnabled = false)
        repository.recordFailedAttempt(5, fixedInstant.minusSeconds(1))
        val awaiting = verifyIdentity.start(request(), repository.settings.value, BiometricStatus.NoHardware)

        val state = verifyIdentity.onPinSubmitted(awaiting, pin, repository.settings.value)

        assertEquals(VerifyIdentityState.Verified(request(), IdentityMethod.PIN), state)
    }

    @Test
    fun `use pin and use biometrics switch between the two ways in`() = runTest {
        val settings = lockedWithBiometrics(biometricEnabled = true)
        val biometric = verifyIdentity.start(request(), settings, BiometricStatus.Available)

        val keypad = verifyIdentity.onUsePin(biometric, settings)
        val backToPrompt = verifyIdentity.onUseBiometrics(keypad)

        assertEquals(VerifyIdentityState.AwaitingPin(request()), keypad)
        assertEquals(VerifyIdentityState.AwaitingBiometric(request()), backToPrompt)
    }

    @Test
    fun `an action that refuses biometrics has no way back to the prompt`() = runTest {
        val settings = lockedWithBiometrics(biometricEnabled = true)
        val disable = request(SecurityAction.DISABLE_LOCK, allowBiometric = false)
        val keypad = verifyIdentity.start(disable, settings, BiometricStatus.Available)

        assertEquals(keypad, verifyIdentity.onUseBiometrics(keypad))
    }

    @Test
    fun `a check that is already passed cannot be passed again`() = runTest {
        val settings = lockedWithBiometrics(biometricEnabled = false)
        val verified = VerifyIdentityState.Verified(request(), IdentityMethod.PIN)

        assertEquals(verified, verifyIdentity.onPinSubmitted(verified, pin, settings))
        assertEquals(verified, verifyIdentity.onBiometricResult(verified, succeeded = true, settings = settings))
    }
}
