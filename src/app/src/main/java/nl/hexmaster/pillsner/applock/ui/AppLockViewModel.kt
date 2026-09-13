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
import nl.hexmaster.pillsner.applock.domain.DisableLockResult
import nl.hexmaster.pillsner.applock.domain.DisablePinLock
import nl.hexmaster.pillsner.applock.domain.EnablePinLock
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.domain.Pin
import nl.hexmaster.pillsner.applock.domain.ResetLockAfterRecovery
import nl.hexmaster.pillsner.applock.domain.SetBiometricUnlock
import nl.hexmaster.pillsner.applock.domain.UnlockResult
import nl.hexmaster.pillsner.applock.domain.UnlockWithPin

/**
 * Backs the root lock gate, the unlock screen, the PIN setup flow and the Security section
 * (design D1, D9): one activity-scoped view model so the failed-attempt cooldown and the
 * biometric preference are one shared thing, not several copies.
 */
class AppLockViewModel(
    private val stateHolder: AppLockStateHolder,
    private val repository: AppLockRepository,
    private val biometricAvailability: BiometricAvailability,
    private val enablePinLock: EnablePinLock,
    private val disablePinLock: DisablePinLock,
    private val unlockWithPin: UnlockWithPin,
    private val setBiometricUnlock: SetBiometricUnlock,
    private val resetLockAfterRecovery: ResetLockAfterRecovery,
    private val clock: Clock,
) : ViewModel() {

    private val biometricStatusFlow = MutableStateFlow(biometricAvailability.status())
    private val biometricPromptedForEpisode = MutableStateFlow(false)
    private val events = Channel<AppLockEvent>(Channel.BUFFERED)

    /** One-shot: a wrong PIN was just submitted, on either the unlock screen or the disable dialog. */
    val eventFlow: Flow<AppLockEvent> = events.receiveAsFlow()

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
            cooldownRemainingSeconds = remainingCooldownSeconds(lockState, settings, now),
            shouldPromptBiometricNow = lockState is LockState.Locked &&
                settings.biometricEnabled &&
                biometricStatus == BiometricStatus.Available &&
                !alreadyPrompted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppLockUiState())

    init {
        // Resets the "prompt once per lock episode" flag exactly when a new episode begins
        // (design D6): entering Locked from anything other than Locked itself.
        viewModelScope.launch {
            var previous: LockState = LockState.Loading
            stateHolder.state.collect { current ->
                if (current is LockState.Locked && previous !is LockState.Locked) {
                    biometricPromptedForEpisode.value = false
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

    /** Submits the current PIN in the disable-lock confirmation dialog. Never accepts biometrics. */
    fun onDisableLockPinSubmitted(rawPin: String) {
        val pin = Pin.of(rawPin) ?: return
        viewModelScope.launch {
            when (disablePinLock(pin, repository.settings.first())) {
                DisableLockResult.Success -> stateHolder.set(LockState.Disabled)
                DisableLockResult.WrongPin -> events.trySend(AppLockEvent.WrongPin)
                is DisableLockResult.CoolingDown -> Unit
            }
        }
    }

    /** Turns the biometric preference on (after one successful prompt) or off. */
    fun onBiometricToggle(enabled: Boolean) {
        viewModelScope.launch { setBiometricUnlock(enabled) }
    }

    /** After the recovery screen's device-credential prompt succeeds (design D5). */
    fun onRecoveryConfirmed() {
        viewModelScope.launch {
            resetLockAfterRecovery()
            stateHolder.set(LockState.Disabled)
        }
    }

    private fun remainingCooldownSeconds(state: LockState, settings: AppLockSettings, now: Instant): Long {
        if (state !is LockState.Locked) return 0
        val endsAt = settings.cooldownEndsAt ?: return 0
        val remaining = Duration.between(now, endsAt).seconds
        return remaining.coerceAtLeast(0)
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
