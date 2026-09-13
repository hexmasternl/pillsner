package nl.hexmaster.pillsner.applock.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The single source of truth for [LockState], shared between the application-scoped
 * `LockOnBackgroundObserver` and the activity-scoped `AppLockViewModel` (design D1, D2). A
 * background switch relocks the app through this holder even before any activity view model
 * exists, and the same holder carries the state resolved at cold start.
 */
class AppLockStateHolder {

    private val _state = MutableStateFlow<LockState>(LockState.Loading)

    /** The current [LockState]. Starts at [LockState.Loading] until resolved once at process start. */
    val state: StateFlow<LockState> = _state

    /** Replaces the current state outright. */
    fun set(newState: LockState) {
        _state.value = newState
    }

    /** Replaces the current state based on what it was. */
    fun update(transform: (LockState) -> LockState) {
        _state.value = transform(_state.value)
    }
}
