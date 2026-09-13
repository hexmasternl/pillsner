package nl.hexmaster.pillsner.applock.ui

import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.applock.domain.LockState
import nl.hexmaster.pillsner.applock.domain.VerifyIdentityState

/**
 * What [nl.hexmaster.pillsner.ui.PillsnerApp] and the Security section render (design D1). The
 * navigation host is composed only while [lockState] is [LockState.Unlocked] or
 * [LockState.Disabled]; every other state shows the unlock screen instead.
 */
data class AppLockUiState(
    val lockState: LockState = LockState.Loading,
    val pinLockEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricStatus: BiometricStatus = BiometricStatus.NoHardware,
    val cooldownRemainingSeconds: Long = 0,
    val shouldPromptBiometricNow: Boolean = false,
    val verify: VerifyIdentityState = VerifyIdentityState.Idle,
) {
    /** Whether PIN entry is currently refused because a cooldown is active (design D7). */
    val pinEntryDisabled: Boolean get() = cooldownRemainingSeconds > 0
}

/** One-shot outcomes the unlock screen and the identity check react to and then discard. */
sealed interface AppLockEvent {
    data object WrongPin : AppLockEvent
}

/**
 * One-shot outcomes of a Security change, consumed by the Settings screen: a confirmation to show,
 * or the go-ahead to start the change-PIN flow once the identity check has passed (design D7).
 *
 * A channel rather than a field of [AppLockUiState] because the change-PIN go-ahead has to survive
 * the Settings screen leaving the composition for the PIN setup screen and coming back.
 */
sealed interface SecurityEffect {
    /** The user proved who they are; the change-PIN flow may start. */
    data object StartPinChange : SecurityEffect

    data object PinChanged : SecurityEffect

    data object BiometricsTurnedOff : SecurityEffect

    data object LockDisabled : SecurityEffect
}
