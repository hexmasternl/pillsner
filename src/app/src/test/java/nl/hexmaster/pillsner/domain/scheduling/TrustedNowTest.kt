package nl.hexmaster.pillsner.domain.scheduling

import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: dose-history-retention, "Trusted-now guards the cutoff" and "Trusted-now reseeds safely". */
class TrustedNowTest {

    private val wall = Instant.parse("2026-09-18T08:00:00Z")
    private val bootMillis = 10_000_000L

    @Test
    fun `ordinary elapsed time advances trusted-now normally`() {
        val previous = ClockSample(wall, bootMillis)
        val oneDay = Duration.ofDays(1)
        val now = wall.plus(oneDay)
        val bootNow = bootMillis + oneDay.toMillis()

        val result = TrustedNow(previous, now, bootNow)

        val advanced = result as TrustedNowResult.Advanced
        assertEquals(now, advanced.trustedNow)
        assertEquals(ClockSample(now, bootNow), advanced.newSample)
    }

    @Test
    fun `a wall-clock forward jump is capped to the real elapsed time the boot clock proves`() {
        val previous = ClockSample(wall, bootMillis)
        val realElapsed = Duration.ofHours(3)
        val now = wall.plus(Duration.ofDays(365)) // the wall clock claims a year has passed
        val bootNow = bootMillis + realElapsed.toMillis() // the boot clock says otherwise

        val result = TrustedNow(previous, now, bootNow) as TrustedNowResult.Advanced

        assertEquals(wall.plus(realElapsed), result.trustedNow)
        assertEquals(ClockSample(wall.plus(realElapsed), bootNow), result.newSample)
    }

    @Test
    fun `a wall-clock backward move does not move trusted-now backward and does not block anything`() {
        val previous = ClockSample(wall, bootMillis)
        val bootNow = bootMillis + Duration.ofHours(2).toMillis()
        val now = wall.minus(Duration.ofDays(30)) // the wall clock has been set back

        val result = TrustedNow(previous, now, bootNow) as TrustedNowResult.Advanced

        assertEquals("Trusted-now never regresses", wall, result.trustedNow)
        assertEquals(ClockSample(wall, bootNow), result.newSample)
    }

    @Test
    fun `a reboot, seen as the boot clock going backward, triggers a reseed instead of a nonsensical delta`() {
        val previous = ClockSample(wall, bootMillis)
        val now = wall.plus(Duration.ofMinutes(5))
        val bootAfterReboot = 500L // elapsedRealtime() reset to (almost) zero at boot

        val result = TrustedNow(previous, now, bootAfterReboot)

        assertTrue(result is TrustedNowResult.Reseed)
        assertEquals(ClockSample(now, bootAfterReboot), (result as TrustedNowResult.Reseed).newSample)
    }

    @Test
    fun `no prior sample triggers a reseed`() {
        val now = wall
        val bootNow = bootMillis

        val result = TrustedNow(previous = null, now = now, bootMillis = bootNow)

        assertTrue(result is TrustedNowResult.Reseed)
        assertEquals(ClockSample(now, bootNow), (result as TrustedNowResult.Reseed).newSample)
    }
}
