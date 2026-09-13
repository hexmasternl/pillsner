package nl.hexmaster.pillsner.applock.domain

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [DisablePinLock] has no biometric parameter at all: the Security section's confirm dialog never
 * offers a biometric prompt for this action (spec "Disabling the lock requires the current PIN"),
 * so there is nothing here to bypass with one.
 */
class DisablePinLockTest {

    private val fixedInstant = Instant.parse("2026-09-11T08:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val verifier = FakePinVerifier()
    private val repository = FakeAppLockRepository()
    private val registerFailedAttempt = RegisterFailedAttempt(repository, clock)
    private val disablePinLock = DisablePinLock(repository, verifier, registerFailedAttempt, clock)

    @Test
    fun `correct pin disables the lock and clears everything`() = runTest {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))

        val result = disablePinLock(pin, repository.settings.value)

        assertEquals(DisableLockResult.Success, result)
        assertEquals(AppLockSettings(), repository.settings.value)
    }

    @Test
    fun `wrong pin leaves the lock enabled and counts the failure`() = runTest {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))

        val result = disablePinLock(requireNotNull(Pin.of("9999")), repository.settings.value)

        assertEquals(DisableLockResult.WrongPin, result)
        assertEquals(true, repository.settings.value.enabled)
        assertEquals(1, repository.settings.value.consecutiveFailures)
    }

    @Test
    fun `disabling is refused while a cooldown is active`() = runTest {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))
        repository.recordFailedAttempt(5, fixedInstant.plusSeconds(30))

        val result = disablePinLock(pin, repository.settings.value)

        assertEquals(DisableLockResult.CoolingDown(fixedInstant.plusSeconds(30)), result)
        assertEquals(true, repository.settings.value.enabled)
        assertEquals(pin.digits.toByteArray().toList(), repository.settings.value.credential?.verifier?.toList())
    }
}
