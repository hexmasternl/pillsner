package nl.hexmaster.pillsner.applock.ui

import androidx.lifecycle.ViewModelStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nl.hexmaster.pillsner.applock.domain.AppLockStateHolder
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.applock.domain.DisablePinLock
import nl.hexmaster.pillsner.applock.domain.EnablePinLock
import nl.hexmaster.pillsner.applock.domain.FakeAppLockRepository
import nl.hexmaster.pillsner.applock.domain.FakeBiometricAvailability
import nl.hexmaster.pillsner.applock.domain.FakePinVerifier
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.domain.Pin
import nl.hexmaster.pillsner.applock.domain.RegisterFailedAttempt
import nl.hexmaster.pillsner.applock.domain.ResetLockAfterRecovery
import nl.hexmaster.pillsner.applock.domain.SetBiometricUnlock
import nl.hexmaster.pillsner.applock.domain.UnlockWithPin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [AppLockViewModel.uiState] combines an infinite once-a-second ticker (for the cooldown
 * countdown) with `viewModelScope`, which is independent of `runTest`'s own coroutine hierarchy.
 * Every test therefore parks its view model in a [ViewModelStore] and clears it in [tearDown], so
 * the ticker is cancelled before the test ends rather than lingering on the shared test dispatcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppLockViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val fixedInstant = Instant.parse("2026-09-11T08:00:00Z")
    private val clock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)

    private val repository = FakeAppLockRepository()
    private val verifier = FakePinVerifier()
    private val biometricAvailability = FakeBiometricAvailability(BiometricStatus.Available)
    private val stateHolder = AppLockStateHolder()
    private val viewModelStore = ViewModelStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): AppLockViewModel {
        val registerFailedAttempt = RegisterFailedAttempt(repository, clock)
        val viewModel = AppLockViewModel(
            stateHolder = stateHolder,
            repository = repository,
            biometricAvailability = biometricAvailability,
            enablePinLock = EnablePinLock(repository, verifier),
            disablePinLock = DisablePinLock(repository, verifier, registerFailedAttempt, clock),
            unlockWithPin = UnlockWithPin(repository, verifier, registerFailedAttempt, clock),
            setBiometricUnlock = SetBiometricUnlock(repository),
            resetLockAfterRecovery = ResetLockAfterRecovery(repository),
            clock = clock,
        )
        viewModelStore.put("appLock", viewModel)
        return viewModel
    }

    @Test
    fun `biometric prompt is requested once entering a new lock episode`() = runTest(dispatcher) {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))
        repository.setBiometricEnabled(true)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        stateHolder.set(LockState.Locked())

        assertTrue(viewModel.uiState.value.shouldPromptBiometricNow)
    }

    @Test
    fun `the prompt is not requested again after it has been shown`() = runTest(dispatcher) {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))
        repository.setBiometricEnabled(true)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        stateHolder.set(LockState.Locked())

        viewModel.onBiometricPromptShown()
        viewModel.onBiometricResult(BiometricResult.Cancelled)

        assertFalse(viewModel.uiState.value.shouldPromptBiometricNow)
    }

    @Test
    fun `use biometrics retries the prompt after a cancel or lockout`() = runTest(dispatcher) {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))
        repository.setBiometricEnabled(true)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        stateHolder.set(LockState.Locked())
        viewModel.onBiometricPromptShown()
        viewModel.onBiometricResult(BiometricResult.LockedOut)
        assertFalse(viewModel.uiState.value.shouldPromptBiometricNow)

        viewModel.onRetryBiometricsClicked()

        assertTrue(viewModel.uiState.value.shouldPromptBiometricNow)
    }

    @Test
    fun `a successful biometric result unlocks the app`() = runTest(dispatcher) {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))
        repository.setBiometricEnabled(true)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        stateHolder.set(LockState.Locked())

        viewModel.onBiometricResult(BiometricResult.Success)

        assertEquals(LockState.Unlocked, viewModel.uiState.value.lockState)
    }

    @Test
    fun `cooldown remaining seconds reflects the persisted end time at the fixed clock`() = runTest(dispatcher) {
        val pin = requireNotNull(Pin.of("1234"))
        repository.storeCredential(verifier.create(pin))
        repository.recordFailedAttempt(5, fixedInstant.plusSeconds(30))
        stateHolder.set(LockState.Locked(fixedInstant.plusSeconds(30)))
        val viewModel = buildViewModel()

        backgroundScope.launch { viewModel.uiState.collect {} }

        assertEquals(30L, viewModel.uiState.value.cooldownRemainingSeconds)
    }

    @Test
    fun `recovering state is reported as-is and never prompts biometrics`() = runTest(dispatcher) {
        repository.setBiometricEnabled(true)
        stateHolder.set(LockState.Recovering)
        val viewModel = buildViewModel()

        backgroundScope.launch { viewModel.uiState.collect {} }

        assertEquals(LockState.Recovering, viewModel.uiState.value.lockState)
        assertFalse(viewModel.uiState.value.shouldPromptBiometricNow)
    }
}
