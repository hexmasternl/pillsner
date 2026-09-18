package nl.hexmaster.pillsner.domain.scheduling

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.amsterdam
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.dose
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.repositoryWith
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec: dose-history-retention, "What counts as expired dose history" and "Retention cutoff date arithmetic". */
class PurgeExpiredDoseHistoryTest {

    private fun startOfDay(date: LocalDate): Instant = ZonedDateTime.of(date, LocalTime.MIN, amsterdam).toInstant()

    private fun clockAt(trustedNow: Instant) = MutableTestClock(trustedNow, amsterdam)

    @Test
    fun `a dose scheduled before an ordinary one-year cutoff is deleted, one on or after is kept`() = runBlocking {
        val trustedNow = startOfDay(LocalDate.of(2026, 9, 18))
        val cutoff = startOfDay(LocalDate.of(2025, 9, 18))
        val justBefore = dose(1, cutoff.minusSeconds(1))
        val onCutoff = dose(2, cutoff)
        val doses = repositoryWith(justBefore, onCutoff)
        val purge = PurgeExpiredDoseHistory(doses, clockAt(trustedNow))

        val deleted = purge(trustedNow)

        assertEquals(1, deleted)
        assertEquals(listOf(2L), doses.all().map { it.id.value })
    }

    @Test
    fun `29 February resolves to 28 February one year back`() = runBlocking {
        // 2028 is a leap year; 2027 is not, so the cutoff must resolve onto 28 February 2027.
        val trustedNow = startOfDay(LocalDate.of(2028, 2, 29))
        val cutoff = startOfDay(LocalDate.of(2027, 2, 28))
        val justBefore = dose(1, cutoff.minusSeconds(1))
        val onCutoff = dose(2, cutoff)
        val doses = repositoryWith(justBefore, onCutoff)
        val purge = PurgeExpiredDoseHistory(doses, clockAt(trustedNow))

        purge(trustedNow)

        assertEquals(listOf(2L), doses.all().map { it.id.value })
    }

    @Test
    fun `a year spanning daylight-saving transitions is still exactly one calendar year`() = runBlocking {
        // Between these two dates Amsterdam both springs forward and falls back once.
        val trustedNow = startOfDay(LocalDate.of(2026, 3, 30))
        val cutoff = startOfDay(LocalDate.of(2025, 3, 30))
        val justBefore = dose(1, cutoff.minusSeconds(1))
        val onCutoff = dose(2, cutoff)
        val doses = repositoryWith(justBefore, onCutoff)
        val purge = PurgeExpiredDoseHistory(doses, clockAt(trustedNow))

        purge(trustedNow)

        assertEquals(listOf(2L), doses.all().map { it.id.value })
    }

    @Test
    fun `nothing eligible is a no-op`() = runBlocking {
        val trustedNow = startOfDay(LocalDate.of(2026, 9, 18))
        val doses = repositoryWith(dose(1, trustedNow))
        val purge = PurgeExpiredDoseHistory(doses, clockAt(trustedNow))

        val deleted = purge(trustedNow)

        assertEquals(0, deleted)
        assertEquals(listOf(1L), doses.all().map { it.id.value })
    }
}
