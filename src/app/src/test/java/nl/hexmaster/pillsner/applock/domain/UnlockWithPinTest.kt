package nl.hexmaster.pillsner.applock.domain

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockWithPinTest {

    private val fixedInstant = Instant.parse("2026-09-11T08:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val verifier = FakePinVerifier()
    private val repository = FakeAppLockRepository()
    private val registerFailedAttempt = RegisterFailedAttempt(repository, clock)
    private val unlockWithPin = UnlockWithPin(repository, verifier, registerFailedAttempt, clock)

    @Test
    fun `correct pin unlocks and resets the failure count`() = runTest {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))
        repository.recordFailedAttempt(3, null)

        val result = unlockWithPin(pin, repository.settings.value)

        assertEquals(UnlockResult.Success, result)
        assertEquals(0, repository.settings.value.consecutiveFailures)
    }

    @Test
    fun `wrong pin clears nothing and increases the failure count`() = runTest {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))

        val result = unlockWithPin(requireNotNull(Pin.of("9999")), repository.settings.value)

        assertEquals(UnlockResult.WrongPin, result)
        assertEquals(1, repository.settings.value.consecutiveFailures)
    }

    @Test
    fun `fifth consecutive failure starts a cooldown`() = runTest {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))
        val wrongPin = requireNotNull(Pin.of("9999"))

        repeat(4) { unlockWithPin(wrongPin, repository.settings.value) }
        val result = unlockWithPin(wrongPin, repository.settings.value)

        assertEquals(5, repository.settings.value.consecutiveFailures)
        assertTrue(result is UnlockResult.WrongPin)
        assertEquals(fixedInstant.plusSeconds(30), repository.settings.value.cooldownEndsAt)
    }

    @Test
    fun `pin entry is refused while a cooldown is active`() = runTest {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))
        repository.recordFailedAttempt(5, fixedInstant.plusSeconds(30))

        val result = unlockWithPin(pin, repository.settings.value)

        assertEquals(UnlockResult.CoolingDown(fixedInstant.plusSeconds(30)), result)
    }
}
