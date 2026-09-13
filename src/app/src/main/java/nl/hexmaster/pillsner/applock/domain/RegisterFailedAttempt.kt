package nl.hexmaster.pillsner.applock.domain

import java.time.Clock
import java.time.Instant
import kotlin.time.toJavaDuration

/**
 * Records one failed PIN attempt and applies the cooldown schedule (design D7). Shared by
 * [UnlockWithPin] and [DisablePinLock] so the failure counter and cooldown are one thing, not two.
 */
class RegisterFailedAttempt(
    private val repository: AppLockRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(consecutiveFailuresBeforeThisOne: Int) {
        val newCount = consecutiveFailuresBeforeThisOne + 1
        val cooldownEndsAt = if (newCount % LockPolicy.maxAttemptsPerWindow == 0) {
            Instant.now(clock).plus(LockPolicy.cooldownFor(newCount).toJavaDuration())
        } else {
            null
        }
        repository.recordFailedAttempt(newCount, cooldownEndsAt)
    }
}
