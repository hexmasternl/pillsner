package nl.hexmaster.pillsner.domain.scheduling

import java.time.Instant
import java.time.LocalTime
import kotlinx.coroutines.runBlocking
import nl.hexmaster.pillsner.data.InMemoryDoseRepository
import nl.hexmaster.pillsner.data.InMemoryMedicationRepository
import nl.hexmaster.pillsner.domain.MutableTestClock
import nl.hexmaster.pillsner.domain.model.IntakeOutcome
import nl.hexmaster.pillsner.domain.model.MedicationId
import nl.hexmaster.pillsner.domain.model.Schedule
import nl.hexmaster.pillsner.domain.model.TestFixtures.medication
import nl.hexmaster.pillsner.domain.model.TestFixtures.mg40
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.amsterdam
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.at
import nl.hexmaster.pillsner.domain.scheduling.SchedulingTestSupport.today
import org.junit.Assert.assertEquals
import org.junit.Test

/** Spec: dose-records window refresh. */
class RefreshPlannedDosesTest {

    private val clock = MutableTestClock(at(hour = 6), amsterdam)
    private val medications = InMemoryMedicationRepository()
    private val doses = InMemoryDoseRepository()
    private val refresh = RefreshPlannedDoses(medications, doses, DoseGenerator(), clock)

    private val twiceADay = medication(
        usedSince = today,
        schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0), LocalTime.of(20, 0)))),
    )

    @Test
    fun `a new medicine gets two days of doses`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))

        refresh()

        assertEquals(
            listOf(at(hour = 8), at(hour = 20), at(today.plusDays(1), 8), at(today.plusDays(1), 20)),
            doses.all().map { it.scheduledAt },
        )
    }

    @Test
    fun `running twice changes nothing`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()
        val first = doses.all()

        refresh()

        assertEquals(first, doses.all())
    }

    @Test
    fun `removing a schedule withdraws its planned doses`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()

        medications.update(MedicationId(1)) {
            it.copy(schedules = listOf(Schedule.EveryNDays(mg40, 1, listOf(LocalTime.of(8, 0)))))
        }
        refresh()

        assertEquals(
            listOf(at(hour = 8), at(today.plusDays(1), 8)),
            doses.all().map { it.scheduledAt },
        )
    }

    @Test
    fun `deactivating a medicine withdraws its planned doses`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()

        medications.update(MedicationId(1)) { it.copy(isActive = false) }
        refresh()

        assertEquals(emptyList<Instant>(), doses.all().map { it.scheduledAt })
    }

    @Test
    fun `a dose the user has been reminded about is never withdrawn`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()
        val morning = doses.all().first()
        doses.setFirstReminded(morning.id, at(hour = 8))

        medications.update(MedicationId(1)) { it.copy(isActive = false) }
        refresh()

        assertEquals(listOf(morning.scheduledAt), doses.all().map { it.scheduledAt })
    }

    @Test
    fun `an answered dose is never withdrawn`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()
        val morning = doses.all().first()
        doses.recordIntake(morning.id, IntakeOutcome.TAKEN, at(hour = 8))

        medications.update(MedicationId(1)) { it.copy(isActive = false) }
        refresh()

        assertEquals(listOf(morning.scheduledAt), doses.all().map { it.scheduledAt })
    }

    @Test
    fun `the day rolling over plans the new tomorrow`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()

        clock.setTo(at(today.plusDays(1), 0, 5))
        refresh()

        assertEquals(
            listOf(
                at(today.plusDays(1), 8),
                at(today.plusDays(1), 20),
                at(today.plusDays(2), 8),
                at(today.plusDays(2), 20),
            ),
            doses.all().map { it.scheduledAt },
        )
    }

    @Test
    fun `moving time zone re-anchors the planned doses to the new wall clock`() = runBlocking {
        medications.replaceAll(listOf(twiceADay))
        refresh()
        val here = doses.all().map { it.scheduledAt }

        clock.moveTo(java.time.ZoneId.of("Asia/Tokyo"))
        refresh()

        val there = doses.all().map { it.scheduledAt }
        assertEquals(4, there.size)
        assertEquals(emptyList<Instant>(), there.filter { it in here })
        // Eight in the morning stays eight in the morning, wherever the user woke up.
        assertEquals(
            at(today, 8, zone = java.time.ZoneId.of("Asia/Tokyo")),
            there.first(),
        )
    }
}
