package nl.hexmaster.pillsner.domain.scheduling

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Spec: dose-history-retention, "Trusted-now clock guard".
 *
 * The guard this holds to its word is what closed the earlier, abandoned attempt at this feature
 * (PR #41): a reboot must never let a tampered wall clock be trusted again.
 */
class TrustedNowTest {

    @Test
    fun `first run seeds trusted-now but reports nothing validated`() {
        val observation = TrustedNow.observe(
            previous = null,
            currentWallMillis = TAMPERED_FUTURE,
            currentElapsedRealtimeMillis = 1_000L,
        )

        assertNull(
            "Nothing has been validated yet, so a first run must not hand back a purge-ready instant",
            observation.validated,
        )
        assertEquals(TrustedNow.Sample(TAMPERED_FUTURE, 1_000L), observation.sample)
    }

    @Test
    fun `a later observation after the seed advances normally and validates`() {
        val seed = TrustedNow.observe(null, currentWallMillis = 1_000_000L, currentElapsedRealtimeMillis = 1_000L).sample

        val observation = TrustedNow.observe(
            previous = seed,
            currentWallMillis = 1_010_000L, // ten real seconds later
            currentElapsedRealtimeMillis = 11_000L,
        )

        assertEquals(Instant.ofEpochMilli(1_010_000L), observation.validated)
    }

    @Test
    fun `ordinary elapsed time advances trusted-now by exactly that much`() {
        val previous = TrustedNow.Sample(trustedNowMillis = 1_000_000L, anchorElapsedRealtimeMillis = 5_000L)
        val tenMinutes = 10 * 60_000L

        val observation = TrustedNow.observe(
            previous = previous,
            currentWallMillis = previous.trustedNowMillis + tenMinutes,
            currentElapsedRealtimeMillis = previous.anchorElapsedRealtimeMillis + tenMinutes,
        )

        assertEquals(Instant.ofEpochMilli(previous.trustedNowMillis + tenMinutes), observation.validated)
        assertEquals(
            previous.anchorElapsedRealtimeMillis + tenMinutes,
            observation.sample.anchorElapsedRealtimeMillis,
        )
    }

    @Test
    fun `a forward clock jump is capped at the real elapsed time`() {
        val previous = TrustedNow.Sample(trustedNowMillis = 1_000_000L, anchorElapsedRealtimeMillis = 5_000L)
        val tenMinutes = 10 * 60_000L
        val fourHundredDays = 400L * 24 * 60 * 60 * 1000

        val observation = TrustedNow.observe(
            previous = previous,
            currentWallMillis = previous.trustedNowMillis + fourHundredDays,
            currentElapsedRealtimeMillis = previous.anchorElapsedRealtimeMillis + tenMinutes,
        )

        assertEquals(
            "Trusted-now must advance by the real elapsed time, never by the tampered wall-clock jump",
            Instant.ofEpochMilli(previous.trustedNowMillis + tenMinutes),
            observation.validated,
        )
    }

    @Test
    fun `a backward clock move does not regress trusted-now`() {
        val previous = TrustedNow.Sample(trustedNowMillis = 1_000_000L, anchorElapsedRealtimeMillis = 5_000L)
        val oneMinute = 60_000L

        val observation = TrustedNow.observe(
            previous = previous,
            currentWallMillis = previous.trustedNowMillis - oneMinute, // wall clock moved backward
            currentElapsedRealtimeMillis = previous.anchorElapsedRealtimeMillis + oneMinute,
        )

        assertEquals(
            "The mark is a ratchet: a backward wall clock must not pull it back",
            Instant.ofEpochMilli(previous.trustedNowMillis),
            observation.validated,
        )
    }

    @Test
    fun `a reboot freezes trusted-now instead of reseeding from the raw wall clock`() {
        val previous = TrustedNow.Sample(trustedNowMillis = 1_000_000L, anchorElapsedRealtimeMillis = 50_000L)

        // elapsedRealtime() resets to zero on reboot, so a smaller reading than last time can only
        // mean the device rebooted since the last observation.
        val observation = TrustedNow.observe(
            previous = previous,
            currentWallMillis = TAMPERED_FUTURE,
            currentElapsedRealtimeMillis = 2_000L,
        )

        assertEquals(
            "A reboot must never let the raw (possibly tampered) wall clock become trusted-now",
            Instant.ofEpochMilli(previous.trustedNowMillis),
            observation.validated,
        )
        assertEquals(2_000L, observation.sample.anchorElapsedRealtimeMillis)
        assertEquals(previous.trustedNowMillis, observation.sample.trustedNowMillis)
    }

    @Test
    fun `after a reboot a later real observation resumes advancing normally`() {
        val previous = TrustedNow.Sample(trustedNowMillis = 1_000_000L, anchorElapsedRealtimeMillis = 50_000L)
        val afterReboot = TrustedNow.observe(previous, TAMPERED_FUTURE, currentElapsedRealtimeMillis = 2_000L).sample

        val fiveMinutes = 5 * 60_000L
        val observation = TrustedNow.observe(
            previous = afterReboot,
            currentWallMillis = afterReboot.trustedNowMillis + fiveMinutes,
            currentElapsedRealtimeMillis = afterReboot.anchorElapsedRealtimeMillis + fiveMinutes,
        )

        assertEquals(Instant.ofEpochMilli(afterReboot.trustedNowMillis + fiveMinutes), observation.validated)
    }

    private companion object {
        /** A wall-clock reading no legitimate elapsed-time delta could ever justify. */
        const val TAMPERED_FUTURE = 100_000_000_000L
    }
}
