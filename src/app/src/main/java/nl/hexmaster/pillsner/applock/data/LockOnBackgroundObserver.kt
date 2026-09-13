package nl.hexmaster.pillsner.applock.data

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nl.hexmaster.pillsner.applock.domain.AppLockRepository
import nl.hexmaster.pillsner.applock.domain.AppLockStateHolder
import nl.hexmaster.pillsner.applock.domain.LockState

/**
 * Relocks the app the instant it leaves the foreground (design D2): registered on
 * `ProcessLifecycleOwner` from `PillsnerApplication`, `onStop` fires only when no activity of the
 * app is started, which is exactly "the app went to the background". `onPause` is deliberately not
 * used, because system dialogs such as the biometric prompt pause the activity without the app
 * leaving the foreground.
 */
class LockOnBackgroundObserver(
    private val repository: AppLockRepository,
    private val stateHolder: AppLockStateHolder,
    private val scope: CoroutineScope,
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        scope.launch {
            if (!repository.settings.first().enabled) return@launch
            stateHolder.update { current ->
                when (current) {
                    is LockState.Recovering, is LockState.Locked -> current
                    else -> LockState.Locked()
                }
            }
        }
    }
}
