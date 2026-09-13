package nl.hexmaster.pillsner.applock.data

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.applock.domain.AppLockSettings
import nl.hexmaster.pillsner.applock.domain.AppLockStateHolder
import nl.hexmaster.pillsner.applock.domain.FakeAppLockRepository
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.domain.PinCredential
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** [LockOnBackgroundObserver.onStop] never reads [owner][LifecycleOwner], so a stub is enough. */
private object StubLifecycleOwner : LifecycleOwner {
    override val lifecycle: Lifecycle get() = throw UnsupportedOperationException("unused by the observer under test")
}

@OptIn(ExperimentalCoroutinesApi::class)
class LockOnBackgroundObserverTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onStop locks the app when the lock is enabled`() = runTest(dispatcher) {
        val repository = FakeAppLockRepository()
        repository.storeCredential(PinCredential(byteArrayOf(1), byteArrayOf(2)))
        val stateHolder = AppLockStateHolder().apply { set(LockState.Unlocked) }
        val observer = LockOnBackgroundObserver(repository, stateHolder, backgroundScope)

        observer.onStop(StubLifecycleOwner)

        assertEquals(LockState.Locked(), stateHolder.state.value)
    }

    @Test
    fun `onStop does nothing when the lock is disabled`() = runTest(dispatcher) {
        val repository = FakeAppLockRepository(AppLockSettings(enabled = false))
        val stateHolder = AppLockStateHolder().apply { set(LockState.Disabled) }
        val observer = LockOnBackgroundObserver(repository, stateHolder, backgroundScope)

        observer.onStop(StubLifecycleOwner)

        assertEquals(LockState.Disabled, stateHolder.state.value)
    }

    @Test
    fun `onStop preserves an active cooldown instead of resetting it`() = runTest(dispatcher) {
        val repository = FakeAppLockRepository()
        repository.storeCredential(PinCredential(byteArrayOf(1), byteArrayOf(2)))
        val cooldownEndsAt = java.time.Instant.parse("2026-09-11T08:00:30Z")
        val stateHolder = AppLockStateHolder().apply { set(LockState.Locked(cooldownEndsAt)) }
        val observer = LockOnBackgroundObserver(repository, stateHolder, backgroundScope)

        observer.onStop(StubLifecycleOwner)

        assertEquals(LockState.Locked(cooldownEndsAt), stateHolder.state.value)
    }
}
