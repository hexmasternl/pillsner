package nl.hexmaster.pillsner.applock.domain

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveInitialLockStateTest {

    @Test
    fun `lock never enabled resolves to disabled`() = runTest {
        val repository = FakeAppLockRepository(AppLockSettings(enabled = false))
        val verifier = FakePinVerifier(available = true)
        val resolve = ResolveInitialLockState(repository, verifier)

        assertEquals(LockState.Disabled, resolve())
    }

    @Test
    fun `enabled with the verifier key missing resolves to recovering`() = runTest {
        val pin = requireNotNull(Pin.of("1234"))
        val verifier = FakePinVerifier(available = true)
        val repository = FakeAppLockRepository()
        repository.storeCredential(verifier.create(pin))
        verifier.setAvailable(false)
        val resolve = ResolveInitialLockState(repository, verifier)

        assertEquals(LockState.Recovering, resolve())
    }

    @Test
    fun `enabled with the verifier available resolves to locked`() = runTest {
        val pin = requireNotNull(Pin.of("1234"))
        val verifier = FakePinVerifier(available = true)
        val repository = FakeAppLockRepository()
        repository.storeCredential(verifier.create(pin))
        val resolve = ResolveInitialLockState(repository, verifier)

        assertEquals(LockState.Locked(cooldownEndsAt = null), resolve())
    }

    @Test
    fun `locked state carries an active cooldown`() = runTest {
        val pin = requireNotNull(Pin.of("1234"))
        val verifier = FakePinVerifier(available = true)
        val repository = FakeAppLockRepository()
        repository.storeCredential(verifier.create(pin))
        val cooldownEndsAt = Instant.parse("2026-09-11T08:00:30Z")
        repository.recordFailedAttempt(5, cooldownEndsAt)
        val resolve = ResolveInitialLockState(repository, verifier)

        assertEquals(LockState.Locked(cooldownEndsAt), resolve())
    }
}
