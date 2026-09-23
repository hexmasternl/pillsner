package nl.hexmaster.pillsner.domain.scheduling

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Today's date as a live stream (spec: medicine-stock-tracking, the live tile heads-up). */
@OptIn(ExperimentalCoroutinesApi::class)
class CurrentDateTest {

    private val amsterdam = ZoneId.of("Europe/Amsterdam")

    /** 23:00 on 13 September in Amsterdam, moving with the test's virtual time. */
    private fun TestScope.virtualClock(): Clock {
        val start = Instant.parse("2026-09-13T21:00:00Z")
        return object : Clock() {
            override fun getZone(): ZoneId = amsterdam
            override fun withZone(zone: ZoneId): Clock = this
            override fun instant(): Instant = start.plusMillis(testScheduler.currentTime)
        }
    }

    @Test
    fun `emits today at once and the next day once midnight passes`() = runTest {
        val seen = mutableListOf<LocalDate>()
        backgroundScope.launch { currentDates(virtualClock()).collect { seen += it } }
        runCurrent()

        assertEquals(listOf(LocalDate.of(2026, 9, 13)), seen)

        advanceTimeBy(HOUR_MILLIS - 1)
        runCurrent()
        assertEquals("Not yet midnight", 1, seen.size)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf(LocalDate.of(2026, 9, 13), LocalDate.of(2026, 9, 14)), seen)
    }

    @Test
    fun `keeps ticking over one day at a time`() = runTest {
        val seen = mutableListOf<LocalDate>()
        backgroundScope.launch { currentDates(virtualClock()).collect { seen += it } }

        advanceTimeBy(HOUR_MILLIS + 2 * DAY_MILLIS + 1)
        runCurrent()

        assertEquals(
            listOf(LocalDate.of(2026, 9, 13), LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 16)),
            seen,
        )
    }

    private companion object {
        const val HOUR_MILLIS = 60L * 60L * 1000L
        const val DAY_MILLIS = 24L * HOUR_MILLIS
    }
}
