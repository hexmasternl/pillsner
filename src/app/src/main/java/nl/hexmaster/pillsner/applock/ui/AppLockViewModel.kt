package nl.hexmaster.pillsner.applock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.applock.domain.AppLockRepository
import nl.hexmaster.pillsner.applock.domain.AppLockSettings
import nl.hexmaster.pillsner.applock.domain.AppLockStateHolder
import nl.hexmaster.pillsner.applock.domain.BiometricAvailability
import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.applock.domain.ChangePin
import nl.hexmaster.pillsner.applock.domain.ChangePinResult
import nl.hexmaster.pillsner.applock.domain.DisableLock
import nl.hexmaster.pillsner.applock.domain.EnablePinLock
import nl.hexmaster.pillsner.applock.domain.IsCurrentPin
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.domain.Pin
import nl.hexmaster.pillsner.applock.domain.SecurityAction
import nl.hexmaster.pillsner.applock.domain.SetBiometricUnlock
import nl.hexmaster.pillsner.applock.domain.UnlockResult
import nl.hexmaster.pillsner.applock.domain.UnlockWithPin
import nl.hexmaster.pillsner.applock.domain.VerifyIdentity
import nl.hexmaster.pillsner.applock.domain.VerifyIdentityRequest
import nl.hexmaster.pillsner.applock.domain.VerifyIdentityState

/**
 * Backs the root lock gate, the unlock screen, the PIN setup flow and the Security section
 * (design D1, D9): one activity-scoped view model so the failed-attempt cooldown and the
 * biometric preference are one shared thing, not several copies. The identity check that guards
 * every Security change (app-settings-security D7) lives here for the same reason.
 */
class AppLockViewModel(
    private val stateHolder: AppLockStateHolder,
    private val repository: AppLockRepository,
    private val biometricAvailability: BiometricAvailability,
    private val enablePinLock: EnablePinLock,
    private val disableLock: DisableLock,
    private val unlockWithPin: UnlockWithPin,
    private val setBiometricUnlock: SetBiometricUnlock,
    private val verifyIdentity: VerifyIdentity,
    private val changePin: ChangePin,
    private val isCurrentPin: IsCurrentPin,
    private val clock: Clock,
) : ViewModel() {

    private val biometricStatusFlow = MutableStateFlow(biometricAvailability.status())
    private val biometricPromptedForEpisode = MutableStateFlow(false)
    private val verifyState = MutableStateFlow<VerifyIdentityState>(VerifyIdentityState.Idle)
    private val events = Channel<AppLockEvent>(Channel.BUFFERED)
    private val securityEffectChannel = Channel<SecurityEffect>(Channel.BUFFERED)

    /** One-shot: a wrong PIN was just submitted, on the unlock screen or in the identity check. */
    val eventFlow: Flow<AppLockEvent> = events.receiveAsFlow()

    /** One-shot: what the Settings screen should do or say after a Security change (design D7). */
    val securityEffects: Flow<SecurityEffect> = securityEffectChannel.receiveAsFlow()

    private val secondTicker = flow {
        while (true) {
            emit(Instant.now(clock))
            delay(TICK_MILLIS)
        }
    }

    val uiState: StateFlow<AppLockUiState> = combine(
        stateHolder.state,
        repository.settings,
        biometricStatusFlow,
        biometricPromptedForEpisode,
        secondTicker,
    ) { lockState, settings, biometricStatus, alreadyPrompted, now ->
        AppLockUiState(
            lockState = lockState,
            pinLockEnabled = settings.enabled,
            biometricEnabled = settings.biometricEnabled,
            biometricStatus = biometricStatus,
            cooldownRemainingSeconds = remainingCooldownSeconds(settings, now),
            shouldPromptBiometricNow = lockState is LockState.Locked &&
                settings.biometricEnabled &&
                biometricStatus == BiometricStatus.Available &&
                !alreadyPrompted,
        )
    }.combine(verifyState) { state, verify ->
        state.copy(verify = verify)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppLockUiState())

    init {
        // Resets the "prompt once per lock episode" flag exactly when a new episode begins
        // (design D6): entering Locked from anything other than Locked itself. The same moment
        // abandons an identity check that was open when the app went to the background
        // (app-settings-security D5): the Security section is gone and nothing was changed.
        viewModelScope.launch {
            var previous: LockState = LockState.Loading
            stateHolder.state.collect { current ->
                if (current is LockState.Locked && previous !is LockState.Locked) {
                    biometricPromptedForEpisode.value = false
                    verifyState.value = VerifyIdentityState.Idle
                }
                previous = current
            }
        }
    }

    /** Re-reads biometric availability; called when the unlock screen or Security section appears. */
    fun refreshBiometricAvailability() {
        val status = biometricAvailability.status()
        biometricStatusFlow.value = status
        viewModelScope.launch {
            repository.reconcileBiometricAvailability(status == BiometricStatus.Available)
        }
    }

    /** Marks the automatic prompt as shown for this lock episode so it is not re-presented. */
    fun onBiometricPromptShown() {
        biometricPromptedForEpisode.value = true
    }

    /** Lets "Use biometrics" trigger the prompt again after a cancel or lockout. */
    fun onRetryBiometricsClicked() {
        biometricPromptedForEpisode.value = false
    }

    fun onBiometricResult(result: BiometricResult) {
        if (result is BiometricResult.Success) {
            stateHolder.set(LockState.Unlocked)
        }
        // Cancelled, LockedOut, Unavailable and Failed all leave PIN entry available; nothing else to do.
    }

    /** Submits a PIN on the unlock screen (spec "Entering the correct PIN unlocks the app"). */
    fun onUnlockPinSubmitted(rawPin: String) {
        val pin = Pin.of(rawPin) ?: return
        viewModelScope.launch {
            when (unlockWithPin(pin, repository.settings.first())) {
                UnlockResult.Success -> stateHolder.set(LockState.Unlocked)
                UnlockResult.WrongPin -> events.trySend(AppLockEvent.WrongPin)
                is UnlockResult.CoolingDown -> Unit
            }
        }
    }

    /** Completes the PIN setup flow once both entries matched (spec "PIN confirmed"). */
    fun onPinLockEnabled(rawPin: String) {
        val pin = Pin.of(rawPin) ?: return
        viewModelScope.launch {
            enablePinLock(pin)
            stateHolder.set(LockState.Unlocked)
        }
    }

    /** Turns the biometric preference on, after the Security section's one successful prompt. */
    fun onBiometricEnabled() {
        viewModelScope.launch { setBiometricUnlock(true) }
    }

    /**
     * "Change PIN" was tapped: identify first, with a biometric where the user has one
     * (spec "Identity check before security changes").
     */
    fun onChangePinTapped() {
        startVerification(SecurityAction.CHANGE_PIN, allowBiometric = true)
    }

    /**
     * The biometric switch was turned off. The switch stays where it is until the check passes.
     * Turning it on is deliberately not routed through the check: its own prompt already proves
     * both the identity and a working enrolment (design D6).
     */
    fun onBiometricDisableRequested() {
        startVerification(SecurityAction.DISABLE_BIOMETRICS, allowBiometric = true)
    }

    /** "Protect with PIN" was turned off. Biometrics are refused: this one takes the PIN itself. */
    fun onLockDisableRequested() {
        startVerification(SecurityAction.DISABLE_LOCK, allowBiometric = false)
    }

    /** The check's biometric prompt came back. Anything but success falls back to the PIN. */
    fun onVerifyBiometricResult(result: BiometricResult) {
        viewModelScope.launch {
            advance(
                verifyIdentity.onBiometricResult(
                    state = verifyState.value,
                    succeeded = result is BiometricResult.Success,
                    settings = repository.settings.first(),
                ),
            )
        }
    }

    /** A PIN was submitted in the identity check. */
    fun onVerifyPinSubmitted(rawPin: String) {
        val pin = Pin.of(rawPin) ?: return
        viewModelScope.launch {
            val before = verifyState.value
            val after = verifyIdentity.onPinSubmitted(before, pin, repository.settings.first())
            if (after == before) events.trySend(AppLockEvent.WrongPin)
            advance(after)
        }
    }

    /** "Use PIN" in the identity check. */
    fun onVerifyUsePin() {
        viewModelScope.launch {
            verifyState.value = verifyIdentity.onUsePin(verifyState.value, repository.settings.first())
        }
    }

    /** "Use biometrics" in the identity check, after a cancelled prompt. */
    fun onVerifyUseBiometrics() {
        verifyState.value = verifyIdentity.onUseBiometrics(verifyState.value)
    }

    /** The identity check was cancelled or dismissed. Nothing changes (spec "Dismissed check"). */
    fun onVerifyDismissed() {
        verifyState.value = VerifyIdentityState.Idle
    }

    /** Whether [rawPin] is the PIN in force, so the change flow can refuse it at the first step. */
    suspend fun isPinInUse(rawPin: String): Boolean {
        val pin = Pin.of(rawPin) ?: return false
        return isCurrentPin(pin)
    }

    /** Both entries of the change-PIN flow matched (spec "Changing the PIN"). */
    fun onNewPinConfirmed(rawPin: String) {
        val pin = Pin.of(rawPin) ?: return
        viewModelScope.launch {
            if (changePin(pin) == ChangePinResult.Changed) {
                securityEffectChannel.trySend(SecurityEffect.PinChanged)
            }
        }
    }

    /** After the recovery screen's device-credential prompt succeeds (app-login design D5). */
    fun onRecoveryConfirmed() {
        viewModelScope.launch {
            disableLock()
            stateHolder.set(LockState.Disabled)
        }
    }

    private fun startVerification(purpose: SecurityAction, allowBiometric: Boolean) {
        viewModelScope.launch {
            verifyState.value = verifyIdentity.start(
                request = VerifyIdentityRequest(purpose, allowBiometric),
                settings = repository.settings.first(),
                biometricStatus = biometricStatusFlow.value,
            )
        }
    }

    /**
     * Takes the check to [next], and when that is [VerifyIdentityState.Verified] carries out the
     * one action it authorises and returns to [VerifyIdentityState.Idle]. Consumed here, exactly
     * once: a second sensitive change needs its own check (spec "One action per check").
     */
    private suspend fun advance(next: VerifyIdentityState) {
        if (next !is VerifyIdentityState.Verified) {
            verifyState.value = next
            return
        }
        verifyState.value = VerifyIdentityState.Idle
        when (next.request.purpose) {
            SecurityAction.CHANGE_PIN -> securityEffectChannel.trySend(SecurityEffect.StartPinChange)
            SecurityAction.DISABLE_BIOMETRICS -> {
                setBiometricUnlock(false)
                securityEffectChannel.trySend(SecurityEffect.BiometricsTurnedOff)
            }
            SecurityAction.DISABLE_LOCK -> {
                disableLock()
                stateHolder.set(LockState.Disabled)
                securityEffectChannel.trySend(SecurityEffect.LockDisabled)
            }
        }
    }

    private fun remainingCooldownSeconds(settings: AppLockSettings, now: Instant): Long {
        val endsAt = settings.cooldownEndsAt ?: return 0
        return Duration.between(now, endsAt).seconds.coerceAtLeast(0)
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
