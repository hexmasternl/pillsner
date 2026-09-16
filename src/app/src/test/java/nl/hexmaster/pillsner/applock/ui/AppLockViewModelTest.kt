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
import nl.hexmaster.pillsner.applock.domain.ChangePin
import nl.hexmaster.pillsner.applock.domain.DisableLock
import nl.hexmaster.pillsner.applock.domain.EnablePinLock
import nl.hexmaster.pillsner.applock.domain.FakeAppLockRepository
import nl.hexmaster.pillsner.applock.domain.FakeBiometricAvailability
import nl.hexmaster.pillsner.applock.domain.FakePinVerifier
import nl.hexmaster.pillsner.applock.domain.IsCurrentPin
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.domain.Pin
import nl.hexmaster.pillsner.applock.domain.RegisterFailedAttempt
import nl.hexmaster.pillsner.applock.domain.SecurityAction
import nl.hexmaster.pillsner.applock.domain.SetBiometricUnlock
import nl.hexmaster.pillsner.applock.domain.UnlockWithPin
import nl.hexmaster.pillsner.applock.domain.VerifyIdentity
import nl.hexmaster.pillsner.applock.domain.VerifyIdentityRequest
import nl.hexmaster.pillsner.applock.domain.VerifyIdentityState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
            enablePinLock = EnablePinLock(repository, verifier, dispatcher),
            disableLock = DisableLock(repository),
            unlockWithPin = UnlockWithPin(repository, verifier, registerFailedAttempt, clock, dispatcher),
            setBiometricUnlock = SetBiometricUnlock(repository),
            verifyIdentity = VerifyIdentity(repository, verifier, registerFailedAttempt, clock, dispatcher),
            changePin = ChangePin(repository, verifier, dispatcher),
            isCurrentPin = IsCurrentPin(repository, verifier, dispatcher),
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

    // --- The identity check before a Security change (app-settings-security D7) ----------------

    private val pin = requireNotNull(Pin.of("1234"))

    private suspend fun unlockedWithLock(biometricEnabled: Boolean = true): AppLockViewModel {
        repository.storeCredential(verifier.create(pin))
        repository.setBiometricEnabled(biometricEnabled)
        stateHolder.set(LockState.Unlocked)
        return buildViewModel()
    }

    @Test
    fun `changing the pin starts with the biometric prompt when the user has one`() = runTest(dispatcher) {
        val viewModel = unlockedWithLock()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val effects = mutableListOf<SecurityEffect>()
        backgroundScope.launch { viewModel.securityEffects.collect { effects += it } }

        viewModel.onChangePinTapped()

        assertEquals(
            VerifyIdentityState.AwaitingBiometric(VerifyIdentityRequest(SecurityAction.CHANGE_PIN, true)),
            viewModel.uiState.value.verify,
        )
        assertTrue("Nothing happens before the user identifies", effects.isEmpty())
    }

    @Test
    fun `turning biometrics off waits for the check and leaves the switch alone meanwhile`() = runTest(dispatcher) {
        val viewModel = unlockedWithLock()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.onBiometricDisableRequested()

        assertEquals(
            VerifyIdentityState.AwaitingBiometric(VerifyIdentityRequest(SecurityAction.DISABLE_BIOMETRICS, true)),
            viewModel.uiState.value.verify,
        )
        assertTrue(repository.settings.value.biometricEnabled)
    }

    @Test
    fun `turning biometrics on needs no check of its own`() = runTest(dispatcher) {
        val viewModel = unlockedWithLock(biometricEnabled = false)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.onBiometricEnabled()

        assertTrue(repository.settings.value.biometricEnabled)
        assertEquals(VerifyIdentityState.Idle, viewModel.uiState.value.verify)
    }

    @Test
    fun `turning the lock off asks for the pin even when a biometric is available`() = runTest(dispatcher) {
        val viewModel = unlockedWithLock()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.onLockDisableRequested()

        assertEquals(
            VerifyIdentityState.AwaitingPin(VerifyIdentityRequest(SecurityAction.DISABLE_LOCK, false)),
            viewModel.uiState.value.verify,
        )
    }

    @Test
    fun `the current pin turns the lock off and leaves nothing behind`() = runTest(dispatcher) {
        val viewModel = unlockedWithLock()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val effects = mutableListOf<SecurityEffect>()
        backgroundScope.launch { viewModel.securityEffects.collect { effects += it } }
        viewModel.onLockDisableRequested()

        viewModel.onVerifyPinSubmitted("1234")

        assertEquals(LockState.Disabled, viewModel.uiState.value.lockState)
        assertFalse(repository.settings.value.enabled)
        assertNull(repository.settings.value.credential)
        assertEquals(listOf(SecurityEffect.LockDisabled), effects)
    }

    @Test
    fun `a wrong pin in the check keeps it open and counts the failure`() = runTest(dispatcher) {
        val viewModel = unlockedWithLock()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onLockDisableRequested()

        viewModel.onVerifyPinSubmitted("9999")

        assertTrue(viewModel.uiState.value.verify is VerifyIdentityState.AwaitingPin)
        assertTrue(repository.settings.value.enabled)
        assertEquals(1, repository.settings.value.consecutiveFailures)
    }

    @Test
    fun `passing the check authorises one action and no more`() = runTest(dispatcher) {
        val viewModel = unlockedWithLock()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val effects = mutableListOf<SecurityEffect>()
        backgroundScope.launch { viewModel.securityEffects.collect { effects += it } }
        viewModel.onBiometricDisableRequested()

        viewModel.onVerifyBiometricResult(BiometricResult.Success)

        assertFalse(repository.settings.value.biometricEnabled)
        assertEquals(listOf(SecurityEffect.BiometricsTurnedOff), effects)
        assertEquals(
            "The next sensitive change needs its own check",
            VerifyIdentityState.Idle,
            viewModel.uiState.value.verify,
        )
    }

    @Test
    fun `passing the check for change pin only opens the flow`() = runTest(dispatcher) {
        val viewModel = unlockedWithLock()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val effects = mutableListOf<SecurityEffect>()
        backgroundScope.launch { viewModel.securityEffects.collect { effects += it } }
        viewModel.onChangePinTapped()

        viewModel.onVerifyBiometricResult(BiometricResult.Success)

        assertEquals(listOf(SecurityEffect.StartPinChange), effects)
        assertTrue("Nothing is changed until the new PIN is confirmed", verifier.verify(pin, requireNotNull(repository.settings.value.credential)))
    }

    @Test
    fun `confirming a new pin replaces it and says so`() = runTest(dispatcher) {
        val viewModel = unlockedWithLock()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val effects = mutableListOf<SecurityEffect>()
        backgroundScope.launch { viewModel.securityEffects.collect { effects += it } }

        viewModel.onNewPinConfirmed("5678")

        val credential = requireNotNull(repository.settings.value.credential)
        assertTrue(verifier.verify(requireNotNull(Pin.of("5678")), credential))
        assertTrue(repository.settings.value.enabled)
        assertEquals(listOf(SecurityEffect.PinChanged), effects)
    }

    @Test
    fun `a check open when the app relocks is abandoned`() = runTest(dispatcher) {
        val viewModel = unlockedWithLock()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onChangePinTapped()

        stateHolder.set(LockState.Locked())

        assertEquals(VerifyIdentityState.Idle, viewModel.uiState.value.verify)
        assertTrue(repository.settings.value.enabled)
    }

    @Test
    fun `dismissing the check changes nothing`() = runTest(dispatcher) {
        val viewModel = unlockedWithLock()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onBiometricDisableRequested()

        viewModel.onVerifyDismissed()

        assertEquals(VerifyIdentityState.Idle, viewModel.uiState.value.verify)
        assertTrue(repository.settings.value.biometricEnabled)
    }
}
