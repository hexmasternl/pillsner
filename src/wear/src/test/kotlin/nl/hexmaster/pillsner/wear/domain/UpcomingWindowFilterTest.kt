package nl.hexmaster.pillsner.wear.domain

import java.time.Duration
import java.time.Instant
import nl.hexmaster.pillsner.shared.wear.SyncedDose
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec: the watch shows the doses scheduled for the upcoming six hours, soonest first. */
class UpcomingWindowFilterTest {

    private val now = Instant.parse("2026-09-13T10:00:00Z")

    private fun doseAt(id: Long, offset: Duration) =
        SyncedDose(id, "Medicine $id", "1 tablet", now.plus(offset).toEpochMilli())

    private fun idsOf(doses: List<SyncedDose>) = UpcomingWindowFilter.filter(doses, now).map { it.doseId }

    @Test
    fun `a dose inside the window is shown`() {
        assertEquals(listOf(1L), idsOf(listOf(doseAt(1, Duration.ofHours(3)))))
    }

    @Test
    fun `a dose past the window is not`() {
        assertEquals(emptyList<Long>(), idsOf(listOf(doseAt(1, Duration.ofHours(6).plusMinutes(1)))))
    }

    @Test
    fun `exactly six hours away still counts`() {
        assertEquals(listOf(1L), idsOf(listOf(doseAt(1, Duration.ofHours(6)))))
    }

    @Test
    fun `a dose whose time has passed stays, at the top`() {
        // The phone never sends a dose that has lapsed into missed, so this one is still to be taken.
        val doses = listOf(doseAt(1, Duration.ofHours(2)), doseAt(2, Duration.ofMinutes(-20)))

        assertEquals(listOf(2L, 1L), idsOf(doses))
    }

    @Test
    fun `the list is ordered by time whatever order it arrives in`() {
        val doses = listOf(
            doseAt(3, Duration.ofHours(5)),
            doseAt(1, Duration.ofMinutes(30)),
            doseAt(2, Duration.ofHours(2)),
        )

        assertEquals(listOf(1L, 2L, 3L), idsOf(doses))
    }

    @Test
    fun `a two-day payload with nothing near is an empty screen`() {
        val doses = listOf(doseAt(1, Duration.ofHours(20)), doseAt(2, Duration.ofHours(30)))

        assertEquals(emptyList<Long>(), idsOf(doses))
    }
}
