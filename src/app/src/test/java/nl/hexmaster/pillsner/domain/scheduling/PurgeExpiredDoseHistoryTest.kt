package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec: dose-history-retention, "Dose history purge" — the one-year cutoff. */
class PurgeExpiredDoseHistoryTest {

    private val amsterdam = ZoneId.of("Europe/Amsterdam")

    @Test
    fun `an ordinary one-year cutoff`() {
        val trustedNow = ZonedDateTime.of(2026, 9, 20, 12, 0, 0, 0, amsterdam).toInstant()
        val purge = PurgeExpiredDoseHistory(Clock.fixed(trustedNow, amsterdam))

        val cutoff = purge(trustedNow)

        assertEquals(ZonedDateTime.of(2025, 9, 20, 0, 0, 0, 0, amsterdam).toInstant(), cutoff)
    }

    @Test
    fun `leap day boundary`() {
        // One calendar year before 1 March of the year after a leap year is 1 March — after
        // 29 February of the leap year, so a dose from that leap day is out of the kept window.
        val trustedNow = ZonedDateTime.of(2029, 3, 1, 9, 0, 0, 0, amsterdam).toInstant()
        val purge = PurgeExpiredDoseHistory(Clock.fixed(trustedNow, amsterdam))

        val cutoff = purge(trustedNow)

        assertEquals(ZonedDateTime.of(2028, 3, 1, 0, 0, 0, 0, amsterdam).toInstant(), cutoff)
    }

    @Test
    fun `a cutoff spanning a daylight-saving transition still lands at local midnight`() {
        // 20 March 2026 is after Amsterdam's spring-forward; one year back, 20 March 2025, is too.
        // The cutoff must still resolve to a real local midnight, not an instant offset by a fixed
        // duration that could land inside a gap or an overlap.
        val trustedNow = ZonedDateTime.of(2026, 3, 20, 12, 0, 0, 0, amsterdam).toInstant()
        val purge = PurgeExpiredDoseHistory(Clock.fixed(trustedNow, amsterdam))

        val cutoff = purge(trustedNow)

        assertEquals(ZonedDateTime.of(2025, 3, 20, 0, 0, 0, 0, amsterdam).toInstant(), cutoff)
    }

    @Test
    fun `the current zone is read fresh on every call, never cached`() {
        // One PurgeExpiredDoseHistory instance, queried twice, with the clock's zone changed in
        // between: proves the instance itself holds no cached ZoneId field from construction time.
        // Two separate instances, each queried once, would pass even with such a field, since
        // neither would ever be asked twice.
        val trustedNow = ZonedDateTime.of(2026, 9, 20, 12, 0, 0, 0, ZoneOffset.UTC).toInstant()
        val clock = MutableZoneClock(trustedNow, ZoneOffset.UTC)
        val purge = PurgeExpiredDoseHistory(clock)

        val utcCutoff = purge(trustedNow)
        clock.setZone(amsterdam)
        val amsterdamCutoff = purge(trustedNow)

        assertEquals(
            "A different system zone must change the very next cutoff, never a stale one",
            ZonedDateTime.of(2025, 9, 20, 0, 0, 0, 0, ZoneOffset.UTC).toInstant(),
            utcCutoff,
        )
        assertEquals(ZonedDateTime.of(2025, 9, 20, 0, 0, 0, 0, amsterdam).toInstant(), amsterdamCutoff)
    }

    /** A fixed instant whose zone can be mutated in place, to prove the cutoff never caches it. */
    private class MutableZoneClock(private val instant: Instant, private var zoneId: ZoneId) : Clock() {
        override fun getZone(): ZoneId = zoneId
        override fun withZone(zone: ZoneId): Clock = MutableZoneClock(instant, zone)
        override fun instant(): Instant = instant
        fun setZone(zone: ZoneId) { zoneId = zone }
    }
}
