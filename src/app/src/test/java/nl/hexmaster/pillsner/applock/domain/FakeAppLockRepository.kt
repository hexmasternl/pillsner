package nl.hexmaster.pillsner.applock.domain

import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** An in-memory [AppLockRepository] for tests, with no persistence across instances. */
class FakeAppLockRepository(
    initial: AppLockSettings = AppLockSettings(),
) : AppLockRepository {

    private val state = MutableStateFlow(initial)

    override val settings: StateFlow<AppLockSettings> = state

    override suspend fun storeCredential(credential: PinCredential) {
        state.value = state.value.copy(
            enabled = true,
            credential = credential,
            consecutiveFailures = 0,
            cooldownEndsAt = null,
        )
    }

    override suspend fun replaceCredential(credential: PinCredential) {
        state.value = state.value.copy(
            credential = credential,
            consecutiveFailures = 0,
            cooldownEndsAt = null,
        )
    }

    override suspend fun clearCredential() {
        state.value = AppLockSettings()
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        state.value = state.value.copy(biometricEnabled = enabled)
    }

    override suspend fun recordFailedAttempt(consecutiveFailures: Int, cooldownEndsAt: Instant?) {
        state.value = state.value.copy(consecutiveFailures = consecutiveFailures, cooldownEndsAt = cooldownEndsAt)
    }

    override suspend fun resetAttempts() {
        state.value = state.value.copy(consecutiveFailures = 0, cooldownEndsAt = null)
    }

    override suspend fun reconcileBiometricAvailability(isAvailable: Boolean) {
        if (!isAvailable && state.value.biometricEnabled) {
            state.value = state.value.copy(biometricEnabled = false)
        }
    }
}
