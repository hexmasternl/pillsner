package nl.hexmaster.pillsner.domain.scheduling

import java.time.Duration
import java.time.Instant

/**
 * One wall-clock instant, paired with the boot-clock reading taken at the same moment
 * (`SystemClock.elapsedRealtime()`, in milliseconds since the device last booted).
 *
 * [bootMillis] only ever advances by real elapsed time, even while the wall clock is moved
 * forward, backward, or not at all — which is what lets it keep [TrustedNow]'s wall-clock delta
 * honest (dose-history-retention design D3).
 */
data class ClockSample(val wall: Instant, val bootMillis: Long)

/** What one call to [TrustedNow] found. */
sealed class TrustedNowResult {

    /**
     * The trusted-now value to use for this wake, and the sample to persist in place of the one
     * just read, so the next wake compares against it in turn.
     */
    data class Advanced(val trustedNow: Instant, val newSample: ClockSample) : TrustedNowResult()

    /**
     * No trustworthy delta could be computed this wake: either there was no prior sample (first
     * run, or the store was cleared), or the boot clock has gone backward, meaning the device has
     * rebooted since the last sample was taken and `elapsedRealtime()` reset to zero.
     *
     * The caller records [newSample] as a fresh baseline and skips the purge for this one wake
     * (design D3, "Risks" — a full reboot resets the boot clock).
     */
    data class Reseed(val newSample: ClockSample) : TrustedNowResult()
}

/**
 * Computes the trusted-now high-water mark from the previous sample and the current clock
 * readings (dose-history-retention design D3).
 *
 * The wall clock is trusted to advance, but only by as much as the boot clock proves has actually
 * elapsed since [previous] was taken, so a wall clock moved forward can never make trusted-now
 * advance faster than real elapsed time allows. A wall clock moved backward never moves
 * trusted-now backward either: retention only ever needs a lower bound on how much time has
 * passed, and this deliberately never lets that bound decrease.
 *
 * No Android dependency: [bootMillis] is a plain `Long` the caller reads from
 * `SystemClock.elapsedRealtime()`, so this stays testable without a device.
 */
object TrustedNow {

    operator fun invoke(previous: ClockSample?, now: Instant, bootMillis: Long): TrustedNowResult {
        if (previous == null || bootMillis < previous.bootMillis) {
            return TrustedNowResult.Reseed(ClockSample(now, bootMillis))
        }

        val wallDelta = Duration.between(previous.wall, now)
        val bootDelta = Duration.ofMillis(bootMillis - previous.bootMillis)
        val trustedDelta = minOf(wallDelta, bootDelta).coerceAtLeast(Duration.ZERO)

        val trustedNow = previous.wall.plus(trustedDelta)
        return TrustedNowResult.Advanced(trustedNow, ClockSample(trustedNow, bootMillis))
    }
}
