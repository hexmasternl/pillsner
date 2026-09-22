package nl.hexmaster.pillsner.domain.scheduling

import java.time.Instant

/**
 * The trusted-now high-water mark that guards the dose-history purge against a tampered or reset
 * system clock (design D2).
 *
 * Two persisted longs are all this needs: [Sample.trustedNowMillis], a wall-clock reading already
 * validated as achievable within real elapsed time, and [Sample.anchorElapsedRealtimeMillis], the
 * monotonic boot-clock reading recorded alongside it. Pure and framework-free: the caller supplies
 * the current wall clock and boot clock readings, since reading them
 * (`System.currentTimeMillis()`, `android.os.SystemClock.elapsedRealtime()`) is the one
 * Android-touching part of this guard, kept out of the domain layer.
 *
 * The one promise this makes: [Sample.trustedNowMillis] can only ever advance by as much real
 * elapsed time as the boot clock has actually recorded since the last observation, no matter how
 * far the wall clock has moved forward in that interval. Moving the wall clock backward is never
 * treated as tampering — it is not guarded against, because it can only delay a purge further,
 * never remove data early.
 */
object TrustedNow {

    /** The two longs [nl.hexmaster.pillsner.data.reminders.TrustedClockStore] persists. */
    data class Sample(val trustedNowMillis: Long, val anchorElapsedRealtimeMillis: Long)

    /**
     * What one observation produced.
     *
     * @property sample the sample to persist for next time.
     * @property validated the trusted-now instant safe to purge with, or null the one time there
     *   is genuinely nothing safe to fall back on yet: the very first observation, before any
     *   sample has ever been recorded.
     */
    data class Observation(val sample: Sample, val validated: Instant?)

    /**
     * @param previous the last persisted sample, or null before the very first observation.
     * @param currentWallMillis the current wall clock (`System.currentTimeMillis()`) — untrusted,
     *   since this is exactly what a tampered clock would report.
     * @param currentElapsedRealtimeMillis the current monotonic boot clock
     *   (`SystemClock.elapsedRealtime()`): real time since boot, including deep sleep, and not
     *   settable by the user.
     */
    fun observe(
        previous: Sample?,
        currentWallMillis: Long,
        currentElapsedRealtimeMillis: Long,
    ): Observation {
        if (previous == null) {
            // No trusted anchor exists yet, so there is nothing to validate this first wall-clock
            // reading against. Seed it, but report no validated trusted-now: the one case a
            // reboot-after-tampering guard must not paper over by trusting the raw wall clock.
            val seed = Sample(currentWallMillis, currentElapsedRealtimeMillis)
            return Observation(seed, validated = null)
        }

        if (currentElapsedRealtimeMillis < previous.anchorElapsedRealtimeMillis) {
            // elapsedRealtime() only resets to zero on reboot, so this can only mean the device
            // rebooted since the last observation. The interval since then cannot be verified from
            // the wall clock — that is exactly the moment a reboot-plus-tampering attack would try
            // to exploit — so trusted-now does not advance and is never reseeded from the raw wall
            // clock. It is simply frozen at its last validated value, which is always safe to purge
            // with: at worst it is older than the true now, which only delays a purge.
            val frozen = previous.copy(anchorElapsedRealtimeMillis = currentElapsedRealtimeMillis)
            return Observation(frozen, validated = Instant.ofEpochMilli(previous.trustedNowMillis))
        }

        val elapsedDelta = currentElapsedRealtimeMillis - previous.anchorElapsedRealtimeMillis
        val wallDelta = currentWallMillis - previous.trustedNowMillis
        // A forward wall-clock jump is capped at the real elapsed time. A backward or stalled wall
        // clock advances the mark by zero rather than regressing it — the mark is a ratchet.
        val advance = maxOf(0L, minOf(wallDelta, elapsedDelta))
        val advanced = Sample(
            trustedNowMillis = previous.trustedNowMillis + advance,
            anchorElapsedRealtimeMillis = currentElapsedRealtimeMillis,
        )
        return Observation(advanced, validated = Instant.ofEpochMilli(advanced.trustedNowMillis))
    }
}
