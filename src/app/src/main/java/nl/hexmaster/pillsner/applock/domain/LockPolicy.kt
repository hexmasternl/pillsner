package nl.hexmaster.pillsner.applock.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The failed-attempt cooldown schedule (design D7, spec "Repeated wrong PIN entries trigger a
 * cooldown"). Pure Kotlin so it is unit tested without any Android dependency.
 */
object LockPolicy {

    /** Consecutive failures that complete one cooldown block. */
    const val maxAttemptsPerWindow = 5

    /** Cooldown applied once the first block of failures completes. */
    val baseCooldown: Duration = 30.seconds

    /** Cooldown never grows past this. */
    val maxCooldown: Duration = 5.minutes

    /**
     * Time the app stays unlocked after going to the background before it relocks. Zero: relocking
     * is immediate (design D2). Kept as a constant so a later grace period is a one-line change.
     */
    val gracePeriod: Duration = Duration.ZERO

    /**
     * The cooldown for the block of failures that just completed at [consecutiveFailures], doubling
     * every [maxAttemptsPerWindow] failures and capped at [maxCooldown]. Only meaningful when
     * [consecutiveFailures] is a multiple of [maxAttemptsPerWindow]; callers only invoke it then.
     */
    fun cooldownFor(consecutiveFailures: Int): Duration {
        if (consecutiveFailures < maxAttemptsPerWindow) return Duration.ZERO
        val completedBlocks = consecutiveFailures / maxAttemptsPerWindow
        val doublings = (completedBlocks - 1).coerceAtLeast(0)
        val scaled = baseCooldown * (1 shl doublings)
        return if (scaled > maxCooldown) maxCooldown else scaled
    }
}
