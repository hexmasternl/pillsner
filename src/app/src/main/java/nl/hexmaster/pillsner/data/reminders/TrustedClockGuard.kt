package nl.hexmaster.pillsner.data.reminders

import android.os.SystemClock
import java.time.Instant
import nl.hexmaster.pillsner.domain.scheduling.TrustedNow

/**
 * The Android-touching half of the trusted-clock guard: reads the current wall clock and the
 * monotonic boot clock, hands them to the pure [TrustedNow.observe], and persists the result to
 * [store].
 *
 * @param wallClockMillis `System.currentTimeMillis()` by default; overridable so a test can supply
 *   a clock without waiting on real time.
 * @param elapsedRealtimeMillis `SystemClock.elapsedRealtime()` by default, for the same reason.
 */
class TrustedClockGuard(
    private val store: TrustedClockStore,
    private val wallClockMillis: () -> Long = System::currentTimeMillis,
    private val elapsedRealtimeMillis: () -> Long = SystemClock::elapsedRealtime,
) {

    /**
     * One observation: reads the last persisted sample, advances it, persists the result, and
     * returns the trusted-now instant safe to purge with — or null on the one occasion nothing
     * safe exists yet (no prior sample and no dose history for [knownGoodFloor] to draw on).
     *
     * @param knownGoodFloor the most recent moment the app independently knows really happened
     *   (in practice, [nl.hexmaster.pillsner.domain.repository.DoseRepository.latestKnownMoment]),
     *   or null when there is none. See [TrustedNow]'s class doc for why this is what closes the
     *   gap a first, unvalidated wall-clock reading would otherwise leave open.
     */
    suspend fun observe(knownGoodFloor: Instant?): Instant? {
        val previous = store.read()
        val observation = TrustedNow.observe(
            previous = previous,
            currentWallMillis = wallClockMillis(),
            currentElapsedRealtimeMillis = elapsedRealtimeMillis(),
            knownGoodFloorMillis = knownGoodFloor?.toEpochMilli(),
        )
        store.write(observation.sample)
        return observation.validated
    }
}
