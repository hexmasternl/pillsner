package nl.hexmaster.pillsner.applock.ui

import nl.hexmaster.pillsner.applock.domain.BiometricStatus
import nl.hexmaster.pillsner.applock.domain.LockState

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
) {
    /** Whether PIN entry is currently refused because a cooldown is active (design D7). */
    val pinEntryDisabled: Boolean get() = cooldownRemainingSeconds > 0
}

/** One-shot outcomes the unlock screen and the disable-lock dialog react to and then discard. */
sealed interface AppLockEvent {
    data object WrongPin : AppLockEvent
}
