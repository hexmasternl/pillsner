package nl.hexmaster.pillsner.wear.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import nl.hexmaster.pillsner.shared.wear.SyncedDose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: the watch shows today and tomorrow, grouped by the time each dose is due. */
class DayAgendaTest {

    private val zone = ZoneId.of("Europe/Amsterdam")

    // 10:00 UTC is midday in Amsterdam, so a few hours either way stays on the same day.
    private val now = Instant.parse("2026-09-13T10:00:00Z")

    private fun doseAt(id: Long, offset: Duration) =
        SyncedDose(id, "Medicine $id", "1 tablet", now.plus(offset).toEpochMilli())

    private fun build(vararg doses: SyncedDose) = DayAgenda.build(doses.toList(), now, zone)

    private fun idsOf(section: AgendaSection) = section.groups.flatMap { group -> group.doses.map { it.doseId } }

    @Test
    fun `the rest of today comes first, ordered by time`() {
        val agenda = build(doseAt(2, Duration.ofHours(5)), doseAt(1, Duration.ofHours(1)))

        assertEquals(listOf(AgendaDay.TODAY), agenda.map { it.day })
        assertEquals(listOf(1L, 2L), idsOf(agenda.single()))
    }

    @Test
    fun `a dose eight hours out is on the list too, where the six-hour window dropped it`() {
        val agenda = build(doseAt(1, Duration.ofHours(8)))

        assertEquals(listOf(1L), idsOf(agenda.single()))
    }

    @Test
    fun `tomorrow follows today, under its own heading`() {
        // 22:00 local today, then 10:00 and 22:00 local tomorrow.
        val agenda = build(
            doseAt(3, Duration.ofHours(34)),
            doseAt(1, Duration.ofHours(10)),
            doseAt(2, Duration.ofHours(22)),
        )

        assertEquals(listOf(AgendaDay.TODAY, AgendaDay.TOMORROW), agenda.map { it.day })
        assertEquals(listOf(1L), idsOf(agenda.first()))
        assertEquals(listOf(2L, 3L), idsOf(agenda.last()))
    }

    @Test
    fun `doses due at the same moment share one time heading`() {
        val agenda = build(
            doseAt(1, Duration.ofHours(2)),
            doseAt(2, Duration.ofHours(2)),
            doseAt(3, Duration.ofHours(4)),
        )

        val groups = agenda.single().groups
        assertEquals(2, groups.size)
        assertEquals(listOf(1L, 2L), groups.first().doses.map { it.doseId })
        assertEquals(listOf(3L), groups.last().doses.map { it.doseId })
    }

    @Test
    fun `a dose whose time has passed stays, at the top of today`() {
        // The phone never sends a dose that has lapsed into missed, so this one is still to be taken.
        val agenda = build(doseAt(1, Duration.ofHours(2)), doseAt(2, Duration.ofMinutes(-20)))

        assertEquals(listOf(2L, 1L), idsOf(agenda.single()))
    }

    @Test
    fun `the day after tomorrow is beyond what this screen promises`() {
        val agenda = build(doseAt(1, Duration.ofHours(60)))

        assertTrue(agenda.isEmpty())
    }

    @Test
    fun `days are told apart in the watch's own zone`() {
        // 23:00 UTC is already the next day in Amsterdam.
        val agenda = DayAgenda.build(
            listOf(SyncedDose(1, "Metformin", "1 tablet", Instant.parse("2026-09-13T23:00:00Z").toEpochMilli())),
            now,
            zone,
        )

        assertEquals(listOf(AgendaDay.TOMORROW), agenda.map { it.day })
    }

    @Test
    fun `nothing planned is an empty agenda`() {
        assertTrue(DayAgenda.build(emptyList(), now, zone).isEmpty())
    }
}
