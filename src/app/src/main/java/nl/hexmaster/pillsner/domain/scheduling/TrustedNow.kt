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
 *
 * **The seed itself needs its own guard.** A first observation with no prior sample has nothing to
 * validate the raw wall clock against, so seeding [Sample.trustedNowMillis] straight from it and
 * then, from the very next ordinary observation onward, advancing it by only a small real elapsed
 * delta, still leaves it wherever that first tampered reading was — advancing correctly from a
 * wrong starting point is still wrong. When independent evidence of how far real time has already
 * reached exists (the app's own dose history, which predates this observation and could not have
 * been written under a clock tampered *after* it was), the seed is clamped down to whichever of
 * the two readings is earlier, per [observe]'s `knownGoodFloorMillis` parameter, rather than
 * trusted outright.
 */
object TrustedNow {

    /** The two longs [nl.hexmaster.pillsner.data.reminders.TrustedClockStore] persists. */
    data class Sample(val trustedNowMillis: Long, val anchorElapsedRealtimeMillis: Long)

    /**
     * What one observation produced.
     *
     * @property sample the sample to persist for next time.
     * @property validated the trusted-now instant safe to purge with, or null the one time there
     *   is genuinely nothing safe to fall back on yet: the very first observation, when neither a
     *   prior sample nor any existing dose history exists to validate the wall clock against.
     */
    data class Observation(val sample: Sample, val validated: Instant?)

    /**
     * @param previous the last persisted sample, or null before the very first observation.
     * @param currentWallMillis the current wall clock (`System.currentTimeMillis()`) — untrusted,
     *   since this is exactly what a tampered clock would report.
     * @param currentElapsedRealtimeMillis the current monotonic boot clock
     *   (`SystemClock.elapsedRealtime()`): real time since boot, including deep sleep, and not
     *   settable by the user.
     * @param knownGoodFloorMillis the most recent moment the app independently knows really
     *   happened — in practice, the latest `plannedAt` already stored across every dose — or null
     *   when there is none yet. Only ever used to pull an untrusted seed *down* to a safer value,
     *   never to push it up, since a clock that reads too early is never the risk here.
     */
    fun observe(
        previous: Sample?,
        currentWallMillis: Long,
        currentElapsedRealtimeMillis: Long,
        knownGoodFloorMillis: Long? = null,
    ): Observation {
        if (previous == null) {
            if (knownGoodFloorMillis != null) {
                // Real, independent evidence already exists that "now" has reached at least this
                // point (a dose already written under some earlier, presumably honest, reading of
                // the same clock) — so a wall clock reading later than that is trusted no further
                // than this floor, and a wall clock reading earlier than the floor is trusted as
                // is (the floor only ever pulls a tampered-forward seed back down, per the design
                // note above). This is safe to validate immediately: it can be no more optimistic
                // than evidence the app already had before this observation began.
                val safe = minOf(currentWallMillis, knownGoodFloorMillis)
                val seed = Sample(safe, currentElapsedRealtimeMillis)
                return Observation(seed, validated = Instant.ofEpochMilli(safe))
            }
            // No trusted anchor and no dose history exists yet, so there is nothing to validate
            // this first wall-clock reading against - and nothing yet for a wrong seed to
            // endanger, since an empty history has nothing a purge could delete. Seed it, but
            // report no validated trusted-now: the one case a reboot-after-tampering guard must
            // not paper over by trusting the raw wall clock once real data does exist.
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
